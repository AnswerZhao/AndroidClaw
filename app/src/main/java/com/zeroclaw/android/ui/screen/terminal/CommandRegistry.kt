/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.ui.screen.terminal

import com.zeroclaw.android.R

/**
 * Result of parsing a terminal input line.
 *
 * The [TerminalViewModel] uses this sealed hierarchy to decide whether to
 * evaluate a Rhai expression against the daemon, execute a local UI action,
 * or route plain text as a chat message.
 */
sealed interface CommandResult {
    /**
     * A Rhai expression to be evaluated by the FFI engine.
     *
     * @property expression Valid Rhai source text.
     */
    data class RhaiExpression(
        val expression: String,
    ) : CommandResult

    /**
     * A local action handled entirely by the ViewModel.
     *
     * @property action Action identifier such as "help" or "clear".
     */
    data class LocalAction(
        val action: String,
    ) : CommandResult

    /**
     * Plain text routed as a chat message through `send()`.
     *
     * @property text The user message with Rhai-unsafe characters escaped.
     */
    data class ChatMessage(
        val text: String,
    ) : CommandResult
}

/**
 * Definition of a slash command available in the terminal REPL.
 *
 * Each command knows how to translate its argument list into a Rhai
 * expression string that the FFI engine can evaluate.
 *
 * @property name The command name without the leading slash (e.g. "status").
 * @property descriptionResId String resource id of the command description.
 * @property usageResId String resource id of the usage hint, or null if none.
 * @property toExpression Translates a split argument list into a Rhai expression string,
 *     or `null` for commands handled locally by the ViewModel.
 */
data class SlashCommand(
    val name: String,
    val descriptionResId: Int,
    val usageResId: Int? = null,
    val toExpression: (args: List<String>) -> String?,
)

/**
 * Registry of all slash commands available in the terminal REPL.
 *
 * Commands are registered declaratively and looked up by exact name or
 * prefix for autocomplete. The [parseAndTranslate] entry point handles
 * the full lifecycle: slash-command dispatch, local actions, and
 * fall-through to plain chat messages.
 */
object CommandRegistry {
    /** Default limit for event queries when the user omits the argument. */
    private const val DEFAULT_EVENT_LIMIT = 20

    /** Default limit for memory listing and recall queries. */
    private const val DEFAULT_MEMORY_LIMIT = 50

    /** Default limit for memory recall results. */
    private const val DEFAULT_RECALL_LIMIT = 20

    /** Default limit for trace queries when the user omits the argument. */
    private const val DEFAULT_TRACE_LIMIT = 20

    /** All registered slash commands, in display order. */
    val commands: List<SlashCommand> = buildCommandList()

    /**
     * Finds a command by exact name match.
     *
     * @param name Command name without the leading slash.
     * @return The matching [SlashCommand], or `null` if none exists.
     */
    fun find(name: String): SlashCommand? = commands.find { it.name == name }

    /**
     * Returns all commands whose name starts with the given prefix.
     *
     * Used by the autocomplete overlay to filter suggestions as the
     * user types.
     *
     * @param prefix Partial command name without the leading slash.
     * @return Commands matching the prefix, in registration order.
     */
    fun matches(prefix: String): List<SlashCommand> = commands.filter { it.name.startsWith(prefix, ignoreCase = true) }

    /**
     * Parses a raw terminal input line and translates it into a [CommandResult].
     *
     * Slash commands are dispatched to their registered [SlashCommand.toExpression]
     * lambda. Local commands (`/help`, `/clear`) produce [CommandResult.LocalAction].
     * Any other input is treated as a plain chat message routed through `send()`.
     *
     * @param input The raw text entered by the user.
     * @return A [CommandResult] ready for the ViewModel to act on.
     */
    fun parseAndTranslate(input: String): CommandResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return CommandResult.ChatMessage("")
        }

        if (!trimmed.startsWith("/")) {
            return CommandResult.ChatMessage(escapeForRhai(trimmed))
        }

        val withoutSlash = trimmed.removePrefix("/")
        val match = findLongestMatch(withoutSlash)

        if (match != null) {
            val (command, remainingArgs) = match
            val expression = command.toExpression(remainingArgs)
            if (expression == null) {
                return CommandResult.LocalAction(command.name)
            }
            return CommandResult.RhaiExpression(expression)
        }

        return CommandResult.ChatMessage(escapeForRhai(trimmed))
    }

    /**
     * Finds the command with the longest matching name prefix from the input.
     *
     * Multi-word commands like "cost daily" must match before single-word
     * commands like "cost". The input after the matched name is split into
     * the argument list.
     *
     * @param input Text after the leading slash.
     * @return A pair of the matched command and its argument list, or `null`.
     */
    private fun findLongestMatch(input: String): Pair<SlashCommand, List<String>>? {
        val sorted = commands.sortedByDescending { it.name.length }
        for (command in sorted) {
            if (input == command.name ||
                input.startsWith(command.name + " ")
            ) {
                val argsText = input.removePrefix(command.name).trim()
                val args =
                    if (argsText.isEmpty()) {
                        emptyList()
                    } else {
                        argsText.split(" ").filter { it.isNotEmpty() }
                    }
                return command to args
            }
        }
        return null
    }

    /**
     * Escapes a string for use inside a Rhai double-quoted string literal.
     *
     * Backslashes are doubled and double quotes are escaped so that the
     * resulting string can be safely embedded between `"` delimiters.
     *
     * @param text Raw user text.
     * @return Escaped text safe for Rhai string literals.
     */
    private fun escapeForRhai(text: String): String = text.replace("\\", "\\\\").replace("\"", "\\\"")

    /**
     * Wraps a value in Rhai double quotes after escaping.
     *
     * @param value Raw string value.
     * @return A quoted Rhai string literal, e.g. `"hello"`.
     */
    private fun rhaiString(value: String): String = "\"${escapeForRhai(value)}\""

    /**
     * Builds the complete list of registered slash commands.
     *
     * @return All commands in display order.
     */
    @Suppress("LongMethod", "CognitiveComplexMethod", "CyclomaticComplexMethod")
    private fun buildCommandList(): List<SlashCommand> =
        listOf(
            SlashCommand(
                name = "status",
                descriptionResId = R.string.terminal_command_desc_status,
                toExpression = { "status()" },
            ),
            SlashCommand(
                name = "version",
                descriptionResId = R.string.terminal_command_desc_version,
                toExpression = { "version()" },
            ),
            SlashCommand(
                name = "health",
                descriptionResId = R.string.terminal_command_desc_health,
                usageResId = R.string.terminal_command_usage_health,
                toExpression = { args ->
                    if (args.isEmpty()) {
                        "health()"
                    } else {
                        "health_component(${rhaiString(args.first())})"
                    }
                },
            ),
            SlashCommand(
                name = "doctor",
                descriptionResId = R.string.terminal_command_desc_doctor,
                usageResId = R.string.terminal_command_usage_doctor,
                toExpression = { args ->
                    if (args.size >= 2) {
                        "doctor(${rhaiString(args[0])}, ${rhaiString(args[1])})"
                    } else {
                        "doctor()"
                    }
                },
            ),
            SlashCommand(
                name = "cost daily",
                descriptionResId = R.string.terminal_command_desc_cost_daily,
                usageResId = R.string.terminal_command_usage_cost_daily,
                toExpression = { args ->
                    if (args.size >= 3) {
                        "cost_daily(${args[0]}, ${args[1]}, ${args[2]})"
                    } else {
                        "cost_daily()"
                    }
                },
            ),
            SlashCommand(
                name = "cost monthly",
                descriptionResId = R.string.terminal_command_desc_cost_monthly,
                usageResId = R.string.terminal_command_usage_cost_monthly,
                toExpression = { args ->
                    if (args.size >= 2) {
                        "cost_monthly(${args[0]}, ${args[1]})"
                    } else {
                        "cost_monthly()"
                    }
                },
            ),
            SlashCommand(
                name = "cost",
                descriptionResId = R.string.terminal_command_desc_cost,
                toExpression = { "cost()" },
            ),
            SlashCommand(
                name = "budget",
                descriptionResId = R.string.terminal_command_desc_budget,
                usageResId = R.string.terminal_command_usage_budget,
                toExpression = { args ->
                    val amount = args.firstOrNull() ?: "0.0"
                    "budget($amount)"
                },
            ),
            SlashCommand(
                name = "events",
                descriptionResId = R.string.terminal_command_desc_events,
                usageResId = R.string.terminal_command_usage_events,
                toExpression = { args ->
                    val limit = args.firstOrNull() ?: DEFAULT_EVENT_LIMIT.toString()
                    "events($limit)"
                },
            ),
            SlashCommand(
                name = "cron get",
                descriptionResId = R.string.terminal_command_desc_cron_get,
                usageResId = R.string.terminal_command_usage_cron_get,
                toExpression = { args ->
                    "cron_get(${rhaiString(args.firstOrNull().orEmpty())})"
                },
            ),
            SlashCommand(
                name = "cron add",
                descriptionResId = R.string.terminal_command_desc_cron_add,
                usageResId = R.string.terminal_command_usage_cron_add,
                toExpression = { args ->
                    if (args.size >= 2) {
                        val expression = args.first()
                        val command = args.drop(1).joinToString(" ")
                        "cron_add(${rhaiString(expression)}, ${rhaiString(command)})"
                    } else {
                        "cron_add(\"\", \"\")"
                    }
                },
            ),
            SlashCommand(
                name = "cron oneshot",
                descriptionResId = R.string.terminal_command_desc_cron_oneshot,
                usageResId = R.string.terminal_command_usage_cron_oneshot,
                toExpression = { args ->
                    if (args.size >= 2) {
                        val delay = args.first()
                        val command = args.drop(1).joinToString(" ")
                        "cron_oneshot(${rhaiString(delay)}, ${rhaiString(command)})"
                    } else {
                        "cron_oneshot(\"\", \"\")"
                    }
                },
            ),
            SlashCommand(
                name = "cron remove",
                descriptionResId = R.string.terminal_command_desc_cron_remove,
                usageResId = R.string.terminal_command_usage_cron_remove,
                toExpression = { args ->
                    "cron_remove(${rhaiString(args.firstOrNull().orEmpty())})"
                },
            ),
            SlashCommand(
                name = "cron pause",
                descriptionResId = R.string.terminal_command_desc_cron_pause,
                usageResId = R.string.terminal_command_usage_cron_pause,
                toExpression = { args ->
                    "cron_pause(${rhaiString(args.firstOrNull().orEmpty())})"
                },
            ),
            SlashCommand(
                name = "cron resume",
                descriptionResId = R.string.terminal_command_desc_cron_resume,
                usageResId = R.string.terminal_command_usage_cron_resume,
                toExpression = { args ->
                    "cron_resume(${rhaiString(args.firstOrNull().orEmpty())})"
                },
            ),
            SlashCommand(
                name = "cron",
                descriptionResId = R.string.terminal_command_desc_cron,
                toExpression = { "cron_list()" },
            ),
            SlashCommand(
                name = "skills tools",
                descriptionResId = R.string.terminal_command_desc_skills_tools,
                usageResId = R.string.terminal_command_usage_skills_tools,
                toExpression = { args ->
                    "skill_tools(${rhaiString(args.firstOrNull().orEmpty())})"
                },
            ),
            SlashCommand(
                name = "skills install",
                descriptionResId = R.string.terminal_command_desc_skills_install,
                usageResId = R.string.terminal_command_usage_skills_install,
                toExpression = { args ->
                    "skill_install(${rhaiString(args.firstOrNull().orEmpty())})"
                },
            ),
            SlashCommand(
                name = "skills remove",
                descriptionResId = R.string.terminal_command_desc_skills_remove,
                usageResId = R.string.terminal_command_usage_skills_remove,
                toExpression = { args ->
                    "skill_remove(${rhaiString(args.firstOrNull().orEmpty())})"
                },
            ),
            SlashCommand(
                name = "skills",
                descriptionResId = R.string.terminal_command_desc_skills,
                toExpression = { "skills()" },
            ),
            SlashCommand(
                name = "tools",
                descriptionResId = R.string.terminal_command_desc_tools,
                toExpression = { "tools()" },
            ),
            SlashCommand(
                name = "memories",
                descriptionResId = R.string.terminal_command_desc_memories,
                usageResId = R.string.terminal_command_usage_memories,
                toExpression = { args ->
                    if (args.isEmpty()) {
                        "memories($DEFAULT_MEMORY_LIMIT)"
                    } else {
                        "memories_by_category(${rhaiString(args.first())}, $DEFAULT_MEMORY_LIMIT)"
                    }
                },
            ),
            SlashCommand(
                name = "memory recall",
                descriptionResId = R.string.terminal_command_desc_memory_recall,
                usageResId = R.string.terminal_command_usage_memory_recall,
                toExpression = { args ->
                    val query = args.joinToString(" ")
                    "memory_recall(${rhaiString(query)}, $DEFAULT_RECALL_LIMIT)"
                },
            ),
            SlashCommand(
                name = "memory forget",
                descriptionResId = R.string.terminal_command_desc_memory_forget,
                usageResId = R.string.terminal_command_usage_memory_forget,
                toExpression = { args ->
                    "memory_forget(${rhaiString(args.firstOrNull().orEmpty())})"
                },
            ),
            SlashCommand(
                name = "memory count",
                descriptionResId = R.string.terminal_command_desc_memory_count,
                toExpression = { "memory_count()" },
            ),
            SlashCommand(
                name = "config",
                descriptionResId = R.string.terminal_command_desc_config,
                toExpression = { "config()" },
            ),
            SlashCommand(
                name = "validate",
                descriptionResId = R.string.terminal_command_desc_validate,
                usageResId = R.string.terminal_command_usage_validate,
                toExpression = { args ->
                    val toml = args.joinToString(" ")
                    "validate_config(${rhaiString(toml)})"
                },
            ),
            SlashCommand(
                name = "traces",
                descriptionResId = R.string.terminal_command_desc_traces,
                usageResId = R.string.terminal_command_usage_traces,
                toExpression = { args ->
                    if (args.isEmpty()) {
                        "traces($DEFAULT_TRACE_LIMIT)"
                    } else {
                        val filter = args.joinToString(" ")
                        "traces_filter(${rhaiString(filter)}, $DEFAULT_TRACE_LIMIT)"
                    }
                },
            ),
            SlashCommand(
                name = "bind",
                descriptionResId = R.string.terminal_command_desc_bind,
                usageResId = R.string.terminal_command_usage_bind,
                toExpression = { args ->
                    if (args.size >= 2) {
                        val channel = args[0]
                        val userId = args.drop(1).joinToString(" ")
                        "bind(${rhaiString(channel)}, ${rhaiString(userId)})"
                    } else {
                        "bind(\"\", \"\")"
                    }
                },
            ),
            SlashCommand(
                name = "allowlist",
                descriptionResId = R.string.terminal_command_desc_allowlist,
                usageResId = R.string.terminal_command_usage_allowlist,
                toExpression = { args ->
                    "allowlist(${rhaiString(args.firstOrNull().orEmpty())})"
                },
            ),
            SlashCommand(
                name = "swap",
                descriptionResId = R.string.terminal_command_desc_swap,
                usageResId = R.string.terminal_command_usage_swap,
                toExpression = { args ->
                    if (args.size >= 2) {
                        "swap_provider(${rhaiString(args[0])}, ${rhaiString(args[1])})"
                    } else {
                        "swap_provider(\"\", \"\")"
                    }
                },
            ),
            SlashCommand(
                name = "models",
                descriptionResId = R.string.terminal_command_desc_models,
                usageResId = R.string.terminal_command_usage_models,
                toExpression = { args ->
                    "models(${rhaiString(args.firstOrNull().orEmpty())})"
                },
            ),
            SlashCommand(
                name = "auth remove",
                descriptionResId = R.string.terminal_command_desc_auth_remove,
                usageResId = R.string.terminal_command_usage_auth_remove,
                toExpression = { args ->
                    if (args.size >= 2) {
                        "auth_remove(${rhaiString(args[0])}, ${rhaiString(args[1])})"
                    } else {
                        "auth_remove(\"\", \"\")"
                    }
                },
            ),
            SlashCommand(
                name = "auth",
                descriptionResId = R.string.terminal_command_desc_auth,
                toExpression = { "auth_list()" },
            ),
            SlashCommand(
                name = "cron at",
                descriptionResId = R.string.terminal_command_desc_cron_at,
                usageResId = R.string.terminal_command_usage_cron_at,
                toExpression = { args ->
                    if (args.size >= 2) {
                        val timestamp = args.first()
                        val command = args.drop(1).joinToString(" ")
                        "cron_add_at(${rhaiString(timestamp)}, ${rhaiString(command)})"
                    } else {
                        "cron_add_at(\"\", \"\")"
                    }
                },
            ),
            SlashCommand(
                name = "cron every",
                descriptionResId = R.string.terminal_command_desc_cron_every,
                usageResId = R.string.terminal_command_usage_cron_every,
                toExpression = { args ->
                    if (args.size >= 2) {
                        val ms = args.first()
                        val command = args.drop(1).joinToString(" ")
                        "cron_add_every($ms, ${rhaiString(command)})"
                    } else {
                        "cron_add_every(0, \"\")"
                    }
                },
            ),
            SlashCommand(
                name = "help",
                descriptionResId = R.string.terminal_command_desc_help,
                toExpression = { null },
            ),
            SlashCommand(
                name = "clear",
                descriptionResId = R.string.terminal_command_desc_clear,
                toExpression = { null },
            ),
        )
}
