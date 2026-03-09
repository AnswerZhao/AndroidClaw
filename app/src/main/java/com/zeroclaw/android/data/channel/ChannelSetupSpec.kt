/*
 * Copyright 2026 ZeroClaw Community
 *
 * Licensed under the MIT License. See LICENSE in the project root.
 */

package com.zeroclaw.android.data.channel

import com.zeroclaw.android.R
import com.zeroclaw.android.model.ChannelFieldSpec
import com.zeroclaw.android.model.ChannelType
import com.zeroclaw.android.util.DeepLinkTarget
import com.zeroclaw.android.util.ExternalAppLauncher

/**
 * Specification for an interactive channel setup sub-flow.
 *
 * Each channel type defines a sequence of guided [steps] that walk the
 * user through credentials, configuration, and optional advanced settings.
 *
 * @property channelType The channel platform this spec describes.
 * @property steps Ordered list of sub-step specifications for the setup flow.
 */
data class ChannelSetupSpec(
    val channelType: ChannelType,
    val steps: List<ChannelSetupStepSpec>,
)

/**
 * One sub-step in a channel setup flow.
 *
 * Each step groups related fields together with contextual instructions,
 * an optional deep link to an external service, and an optional live
 * validator to verify credentials before proceeding.
 *
 * @property title Short heading displayed at the top of the step.
 * @property instructions Ordered content items rendered as guidance text.
 * @property deepLink Optional external link target for the step (e.g. BotFather).
 * @property fields Configuration fields collected in this step.
 * @property validatorType Optional live validation to run on this step's fields.
 * @property optional Whether the user may skip this step entirely.
 */
data class ChannelSetupStepSpec(
    val title: String = "",
    val titleResId: Int? = null,
    val instructions: List<InstructionItem>,
    val deepLink: DeepLinkTarget? = null,
    val fields: List<ChannelFieldSpec>,
    val validatorType: ValidatorType? = null,
    val optional: Boolean = false,
)

/**
 * Instruction items rendered in the setup flow.
 *
 * Each variant maps to a distinct visual treatment in the UI:
 * plain text, numbered steps, warnings, or expandable hints.
 */
sealed interface InstructionItem {
    /**
     * Plain descriptive text.
     *
     * @property content The text content to display.
     */
    data class Text(
        val content: String = "",
        val contentResId: Int? = null,
    ) : InstructionItem

    /**
     * A numbered step in a procedure.
     *
     * @property number The step number (1-based).
     * @property content The instruction text for this step.
     */
    data class NumberedStep(
        val number: Int,
        val content: String = "",
        val contentResId: Int? = null,
    ) : InstructionItem

    /**
     * A warning callout emphasising important information.
     *
     * @property content The warning message to display.
     */
    data class Warning(
        val content: String = "",
        val contentResId: Int? = null,
    ) : InstructionItem

    /**
     * A hint that may be shown in a collapsible section.
     *
     * @property content The hint text.
     * @property expandable Whether the hint is initially collapsed.
     */
    data class Hint(
        val content: String = "",
        val contentResId: Int? = null,
        val expandable: Boolean = false,
    ) : InstructionItem
}

/**
 * Types of live validation available for channel setup.
 *
 * Each value corresponds to a platform-specific API call that
 * verifies the user's credentials before proceeding to the next step.
 */
enum class ValidatorType {
    /** Validates a Telegram Bot API token via the `getMe` endpoint. */
    TELEGRAM_BOT_TOKEN,

    /** Validates a Discord bot token via the `users/@me` endpoint. */
    DISCORD_BOT_TOKEN,

    /** Validates a Slack bot token via the `auth.test` endpoint. */
    SLACK_BOT_TOKEN,

    /** Validates a Matrix access token via the `whoami` endpoint. */
    MATRIX_ACCESS_TOKEN,
}

/**
 * Registry of [ChannelSetupSpec] definitions for all supported channel types.
 *
 * Provides a data-driven lookup from [ChannelType] to its guided setup
 * specification. Channel types without a dedicated spec (currently only
 * [ChannelType.WEBHOOK]) return null from [forType].
 */
@Suppress("DEPRECATION", "LongMethod", "MagicNumber")
object ChannelSetupSpecs {
    /**
     * Returns the [ChannelSetupSpec] for the given [type], or null if the
     * channel type does not support guided setup.
     *
     * @param type The channel type to look up.
     * @return The setup specification, or null for unsupported types.
     */
    fun forType(type: ChannelType): ChannelSetupSpec? = specs[type]

    /**
     * Retrieves fields from a [ChannelType] by their key names.
     *
     * Returns only those [ChannelFieldSpec] entries whose [ChannelFieldSpec.key]
     * appears in [keys], preserving the order of [keys].
     */
    private fun ChannelType.fieldsByKey(
        vararg keys: String,
    ): List<ChannelFieldSpec> {
        val indexed = fields.associateBy { it.key }
        return keys.mapNotNull { indexed[it] }
    }

    /**
     * Retrieves fields from a [ChannelType] excluding the given key names.
     *
     * Returns [ChannelFieldSpec] entries whose [ChannelFieldSpec.key] does
     * not appear in [keys], preserving the original field order.
     */
    private fun ChannelType.fieldsExcluding(
        vararg keys: String,
    ): List<ChannelFieldSpec> {
        val excluded = keys.toSet()
        return fields.filter { it.key !in excluded }
    }

    /**
     * Builds a generic two-step spec for channels without a dedicated flow.
     *
     * Step 1 ("Credentials") contains all required or secret fields.
     * Step 2 ("Configuration") contains the remaining fields and is
     * marked optional. If all fields are credentials, only one step
     * is produced.
     */
    private fun genericSpec(type: ChannelType): ChannelSetupSpec {
        val credentials = type.fields.filter { it.isRequired || it.isSecret }
        val configuration =
            type.fields.filter { !it.isRequired && !it.isSecret }

        val steps =
            buildList {
                add(
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_credentials,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_generic_credentials,
                                ),
                            ),
                        fields = credentials,
                    ),
                )
                if (configuration.isNotEmpty()) {
                    add(
                        ChannelSetupStepSpec(
                            titleResId = R.string.channel_setup_title_configuration,
                            instructions =
                                listOf(
                                    InstructionItem.Text(
                                        contentResId = R.string.channel_setup_instruction_generic_configuration,
                                    ),
                                ),
                            fields = configuration,
                            optional = true,
                        ),
                    )
                }
            }

        return ChannelSetupSpec(channelType = type, steps = steps)
    }

    /** Pre-built spec map keyed by [ChannelType]. */
    private val specs: Map<ChannelType, ChannelSetupSpec> =
        buildMap {
            put(ChannelType.TELEGRAM, telegramSpec())
            put(ChannelType.DISCORD, discordSpec())
            put(ChannelType.SLACK, slackSpec())
            put(ChannelType.MATRIX, matrixSpec())

            ChannelType.entries
                .filter { it != ChannelType.WEBHOOK }
                .filter { it !in this }
                .forEach { put(it, genericSpec(it)) }
        }

    /** Builds the Telegram setup spec with 3 sub-steps. */
    private fun telegramSpec(): ChannelSetupSpec =
        ChannelSetupSpec(
            channelType = ChannelType.TELEGRAM,
            steps =
                listOf(
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_create_telegram_bot,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_telegram_create_bot,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 1,
                                    contentResId = R.string.channel_setup_instruction_telegram_open_botfather,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 2,
                                    contentResId = R.string.channel_setup_instruction_telegram_send_newbot,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 3,
                                    contentResId = R.string.channel_setup_instruction_telegram_copy_token,
                                ),
                                InstructionItem.Warning(
                                    contentResId = R.string.channel_setup_instruction_telegram_keep_token_secret,
                                ),
                            ),
                        deepLink = ExternalAppLauncher.TELEGRAM_BOTFATHER,
                        fields = ChannelType.TELEGRAM.fieldsByKey("bot_token"),
                        validatorType = ValidatorType.TELEGRAM_BOT_TOKEN,
                    ),
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_allow_your_account,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_telegram_restrict_users,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 1,
                                    contentResId = R.string.channel_setup_instruction_telegram_open_userinfobot,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 2,
                                    contentResId = R.string.channel_setup_instruction_telegram_get_user_id,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 3,
                                    contentResId = R.string.channel_setup_instruction_telegram_paste_user_id,
                                ),
                                InstructionItem.Hint(
                                    contentResId = R.string.channel_setup_instruction_telegram_multiple_user_ids,
                                    expandable = true,
                                ),
                            ),
                        deepLink = ExternalAppLauncher.TELEGRAM_USERINFOBOT,
                        fields = ChannelType.TELEGRAM.fieldsByKey("allowed_users"),
                    ),
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_advanced_settings,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_telegram_advanced,
                                ),
                            ),
                        fields =
                            ChannelType.TELEGRAM.fieldsExcluding(
                                "bot_token",
                                "allowed_users",
                            ),
                        optional = true,
                    ),
                ),
        )

    /** Builds the Discord setup spec with 3 sub-steps. */
    private fun discordSpec(): ChannelSetupSpec =
        ChannelSetupSpec(
            channelType = ChannelType.DISCORD,
            steps =
                listOf(
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_create_discord_bot,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_discord_create_application,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 1,
                                    contentResId = R.string.channel_setup_instruction_discord_open_portal,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 2,
                                    contentResId = R.string.channel_setup_instruction_discord_new_application,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 3,
                                    contentResId = R.string.channel_setup_instruction_discord_reset_token,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 4,
                                    contentResId = R.string.channel_setup_instruction_discord_copy_token,
                                ),
                                InstructionItem.Warning(
                                    contentResId = R.string.channel_setup_instruction_discord_enable_intent,
                                ),
                            ),
                        deepLink = ExternalAppLauncher.DISCORD_DEV_PORTAL,
                        fields = ChannelType.DISCORD.fieldsByKey("bot_token"),
                        validatorType = ValidatorType.DISCORD_BOT_TOKEN,
                    ),
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_configure_server,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_discord_configure_server,
                                ),
                                InstructionItem.Hint(
                                    contentResId = R.string.channel_setup_instruction_discord_enable_dev_mode,
                                    expandable = true,
                                ),
                            ),
                        fields =
                            ChannelType.DISCORD.fieldsByKey(
                                "guild_id",
                                "allowed_users",
                            ),
                    ),
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_advanced_settings,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_discord_advanced,
                                ),
                            ),
                        fields = ChannelType.DISCORD.fieldsByKey("listen_to_bots"),
                        optional = true,
                    ),
                ),
        )

    /** Builds the Slack setup spec with 3 sub-steps. */
    private fun slackSpec(): ChannelSetupSpec =
        ChannelSetupSpec(
            channelType = ChannelType.SLACK,
            steps =
                listOf(
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_create_slack_app,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_slack_create_app,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 1,
                                    contentResId = R.string.channel_setup_instruction_slack_open_console,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 2,
                                    contentResId = R.string.channel_setup_instruction_slack_create_new_app,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 3,
                                    contentResId = R.string.channel_setup_instruction_slack_add_scopes,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 4,
                                    contentResId = R.string.channel_setup_instruction_slack_install_copy_bot_token,
                                ),
                                InstructionItem.NumberedStep(
                                    number = 5,
                                    contentResId = R.string.channel_setup_instruction_slack_generate_app_token,
                                ),
                                InstructionItem.Warning(
                                    contentResId = R.string.channel_setup_instruction_slack_need_both_tokens,
                                ),
                            ),
                        deepLink = ExternalAppLauncher.SLACK_APP_CONSOLE,
                        fields =
                            ChannelType.SLACK.fieldsByKey(
                                "bot_token",
                                "app_token",
                            ),
                        validatorType = ValidatorType.SLACK_BOT_TOKEN,
                    ),
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_configure_channel,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_slack_configure_channel,
                                ),
                                InstructionItem.Hint(
                                    contentResId = R.string.channel_setup_instruction_slack_find_channel_id,
                                    expandable = true,
                                ),
                            ),
                        fields =
                            ChannelType.SLACK.fieldsByKey(
                                "channel_id",
                                "allowed_users",
                            ),
                    ),
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_advanced_settings,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_slack_advanced,
                                ),
                            ),
                        fields =
                            ChannelType.SLACK.fieldsExcluding(
                                "bot_token",
                                "app_token",
                                "channel_id",
                                "allowed_users",
                            ),
                        optional = true,
                    ),
                ),
        )

    /** Builds the Matrix setup spec with 2 sub-steps. */
    private fun matrixSpec(): ChannelSetupSpec =
        ChannelSetupSpec(
            channelType = ChannelType.MATRIX,
            steps =
                listOf(
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_connect_to_matrix,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_matrix_enter_homeserver_token,
                                ),
                                InstructionItem.Hint(
                                    contentResId = R.string.channel_setup_instruction_matrix_generate_access_token,
                                    expandable = true,
                                ),
                                InstructionItem.Warning(
                                    contentResId = R.string.channel_setup_instruction_matrix_keep_token_secret,
                                ),
                            ),
                        fields =
                            ChannelType.MATRIX.fieldsByKey(
                                "homeserver",
                                "access_token",
                            ),
                        validatorType = ValidatorType.MATRIX_ACCESS_TOKEN,
                    ),
                    ChannelSetupStepSpec(
                        titleResId = R.string.channel_setup_title_configure_room,
                        instructions =
                            listOf(
                                InstructionItem.Text(
                                    contentResId = R.string.channel_setup_instruction_matrix_configure_room_users,
                                ),
                                InstructionItem.Hint(
                                    contentResId = R.string.channel_setup_instruction_matrix_find_room_id,
                                    expandable = true,
                                ),
                            ),
                        fields =
                            ChannelType.MATRIX.fieldsByKey(
                                "room_id",
                                "allowed_users",
                            ),
                    ),
                ),
        )
}
