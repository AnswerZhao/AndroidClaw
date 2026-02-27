# Terminal REPL Design

**Date**: 2026-02-27
**Status**: Approved
**Replaces**: Console screen (full deprecation)

## Summary

Replace the existing chat-style Console with a terminal-aesthetic REPL that talks to the ZeroClaw gateway. Slash commands map to gateway operations via a Rhai scripting engine in Rust. Natural language input routes to the agent as before. The visual style mimics Claude Code CLI: monospace font, dark surface, braille spinner, markdown rendering, colored output.

---

## Architecture

Three layers:

```
┌─────────────────────────────────┐
│  Terminal UI (Compose)          │  Monospace LazyColumn, prompt input,
│  - TerminalScreen               │  markdown rendering, colored output,
│  - TerminalViewModel            │  command autocomplete popup
├─────────────────────────────────┤
│  Command Router (Kotlin)        │  Parses input, routes:
│  - starts with "/" → translate  │    slash → Rhai expression
│    to Rhai expression           │    plain text → send("...") call
│  - plain text → send("text")   │
├─────────────────────────────────┤
│  Rhai Engine (Rust/FFI)         │  Single engine instance in OnceLock,
│  - eval_repl(expr) → String     │  33+ registered fns mapping to
│  - All ZeroClaw APIs registered │  existing FFI functions,
│    as native Rhai functions     │  results serialized as JSON strings
└─────────────────────────────────┘
```

### Key decisions

- **One new FFI function**: `eval_repl(expression: String) -> Result<String, FfiError>`. Everything goes through this single entry point.
- **Results as JSON strings**: Rhai functions return JSON. Kotlin parses and renders rich terminal output (status blocks, tables, lists, markdown).
- **Slash-to-expression translation** happens Kotlin-side. Simple string mapping: `/cost daily` becomes `cost_daily()`, `/memory recall "topic"` becomes `memory_recall("topic")`.
- **Every mapping verified against source**: At every step, function signatures are verified against the actual `zeroclaw-ffi/src/lib.rs`, not from memory or cached docs.

---

## Terminal UI

### Visual layout

```
┌─────────────────────────────────────┐
│ ZeroClaw v0.0.29 • ● Connected      │  ← sticky header
│─────────────────────────────────────│
│                                     │
│ > /status                           │  ← user input (primary color)
│ ┌ System Status ─────────────────┐  │  ← structured output block
│ │ Router   ● running             │  │
│ │ Memory   ● healthy (42 items)  │  │
│ │ Budget   $0.23 / $5.00 daily   │  │
│ └────────────────────────────────┘  │
│                                     │
│ > what's the weather in tokyo?      │  ← natural language input
│ ⠹ Thinking...                       │  ← braille spinner while waiting
│                                     │
│ Based on current data, Tokyo is     │  ← agent response (markdown)
│ experiencing **partly cloudy**      │
│ conditions at 18C...                │
│                                     │
├─────────────────────────────────────┤
│ > █                              ⏎  │  ← input bar (monospace)
│ /cost  /status  /skills  /help      │  ← autocomplete popup
└─────────────────────────────────────┘
```

### Components

- **LazyColumn** with `reverseLayout = true`: each entry is a `TerminalBlock` (input, response, structured output, error, or spinner)
- **JetBrains Mono** bundled as font resource (free, excellent mobile legibility)
- **Dark surface**: terminal area uses `surfaceContainerLowest` for contrast, even in light theme
- **Braille spinner**: `⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏` character rotation next to "Thinking..." label while awaiting response. Falls back to static `...` when `isPowerSaveMode` is true.
- **Markdown rendering** in agent responses: bold, italic, inline code, code blocks, lists (CommonMark subset)
- **Long-press to copy** on any output block (carried over from Console)
- **Sticky header**: version string + colored status dot, always visible

### Input bar

- Monospace `OutlinedTextField` with `>` prompt prefix
- Send button (paper plane icon) on the right
- Image attach button (paperclip icon) on the left
- Staged images appear as compact text strip above input: `[filename.jpg 1.2MB]` (not thumbnails, fits terminal aesthetic)
- Sent images render in scrollback as `[image: filename.jpg (1.2MB)]` text

### Command autocomplete

- Popup appears above input bar when user types `/`
- Filters as user types: `/co` shows `/cost`, `/cron`
- Shows subcommands after space: `/cost ` shows `daily`, `monthly`, `budget`
- Tap to accept (primary interaction on mobile)
- Hardware keyboard tab also accepts
- Commands with required args show placeholder: `/memory recall <query>`

### Command history

- Swipe up on input field cycles through previous inputs
- Stored in Room alongside terminal entries
- History persists across app restarts

---

## Rhai Engine

### Setup

One `Engine` instance in a `OnceLock<Mutex<Engine>>` in the FFI crate. Built with `Engine::new_raw()` + `CorePackage` for minimal footprint. All existing FFI functions registered as native Rhai functions.

### Function mapping

Every mapping below is verified against the actual `zeroclaw-ffi/src/lib.rs` function signatures during implementation.

```
FFI function                → Rhai function
────────────────────────────────────────────────
start_daemon(config)        → start(config)
stop_daemon()               → stop()
get_status()                → status()
get_version()               → version()
send_message(msg)           → send(msg)
send_vision_message(...)    → send_vision(msg, images, mimes)
validate_config(toml)       → validate_config(toml)
get_health_detail()         → health()
get_component_health(name)  → health_component(name)
doctor_channels()           → doctor()
get_cost_summary()          → cost()
get_daily_cost()            → cost_daily()
get_monthly_cost()          → cost_monthly()
check_budget()              → budget()
list_skills()               → skills()
get_skill_tools(name)       → skill_tools(name)
install_skill(url)          → skill_install(url)
remove_skill(name)          → skill_remove(name)
list_tools()                → tools()
list_memories()             → memories()
recall_memory(query)        → memory_recall(query)
forget_memory(id)           → memory_forget(id)
memory_count()              → memory_count()
list_cron_jobs()            → cron_list()
get_cron_job(id)            → cron_get(id)
add_cron_job(...)           → cron_add(name, schedule, action)
add_one_shot_job(...)       → cron_oneshot(name, delay, action)
remove_cron_job(id)         → cron_remove(id)
pause_cron_job(id)          → cron_pause(id)
resume_cron_job(id)         → cron_resume(id)
register_event_listener     → events_listen(callback)
unregister_event_listener   → events_unlisten(id)
get_recent_events(n)        → events(n)
scaffold_workspace(path)    → scaffold(path)
```

### Single entry point

```rust
#[uniffi::export]
fn eval_repl(expression: String) -> Result<String, FfiError> {
    catch_unwind(|| {
        let engine = REPL_ENGINE.get_or_init(|| Mutex::new(build_engine()));
        let mut engine = engine.lock().unwrap();
        let result = engine.eval::<Dynamic>(&expression)?;
        serde_json::to_string(&result_to_json(result))
    })
}
```

Each registered function returns a `Dynamic` wrapping a JSON-serializable structure. The `eval_repl` wrapper serializes to a JSON string, which Kotlin parses for rendering.

### Slash command translation (Kotlin-side)

```
/status             → eval_repl("status()")
/cost daily         → eval_repl("cost_daily()")
/cost               → eval_repl("cost()")
/memory recall X    → eval_repl("memory_recall(\"X\")")
/cron pause 5       → eval_repl("cron_pause(\"5\")")
/help               → handled locally (no FFI call)
/clear              → handled locally (clear scrollback)
plain text          → eval_repl("send(\"escaped text\")")
```

### Power user scripting

Because the engine is Rhai, power users can type raw expressions directly:

```
let x = cost_daily(); print(x.total)
```

This is available for free without additional implementation.

---

## Output Rendering

Kotlin parses the JSON from `eval_repl` and renders typed terminal blocks:

| JSON shape | Renders as |
|---|---|
| `{"type": "status", "components": [...]}` | Colored status table with dot indicators |
| `{"type": "list", "items": [...]}` | Numbered or bulleted list |
| `{"type": "table", "headers": [...], "rows": [...]}` | Monospace-aligned table |
| `{"type": "text", "content": "..."}` | Markdown-rendered text block |
| `{"type": "cost", ...}` | Cost summary with currency formatting |
| `{"type": "error", "message": "..."}` | Red-tinted error block |
| Plain string (from `send()`) | Markdown-rendered agent response |

---

## Font Scaling & Responsive Layout

### Font scaling

- All text uses `MaterialTheme.typography.*` named styles exclusively. No raw `.sp` or `.dp` literals for text sizing.
- JetBrains Mono is loaded as a custom `FontFamily` and applied through a terminal-specific typography variant that still uses M3's sp-based sizing.
- The terminal content area has no fixed-height containers. All blocks use `wrapContentHeight()` so text can expand freely at 200% system font scale.
- Line height specified in `sp` (not `dp`) to scale proportionally under Android 14+ nonlinear font scaling.
- Minimum touch targets remain 48x48dp regardless of font scale.

### Responsive width

- Terminal content area uses 16dp edge margins on compact screens (<600dp), 24dp on medium/expanded.
- Structured output blocks (status tables, lists) use full available width minus margins. No fixed-width boxes that could clip on narrow screens at large font sizes.
- Autocomplete popup width matches the input bar width.
- Long command output wraps naturally. No horizontal scrolling except inside code blocks (which get a horizontal scroll modifier).

### Testing requirements

- Verify at 100%, 150%, and 200% system font scale that no text is clipped and all interactive elements remain tappable.
- Verify on compact (360dp), medium (600dp), and expanded (840dp+) widths.

---

## Accessibility

### Screen reader support

Following the established codebase patterns:

**Terminal blocks** (each entry in the scrollback):
- Each `TerminalBlock` composable uses `semantics(mergeDescendants = true)` with a computed `contentDescription` that linearizes the content:
  - User input blocks: `"Command: /status"` or `"Message: what's the weather"`
  - Agent response blocks: the full text content (markdown stripped to plain text for TalkBack)
  - Structured output blocks: linearized summary, e.g. `"Status: Router running, Memory healthy 42 items, Budget 0.23 of 5.00 daily"`
  - Error blocks: `"Error: connection timed out"`

**Spinner (Thinking... indicator)**:
- `LiveRegionMode.Polite` on the spinner container so TalkBack announces "Thinking" when it appears, without requiring focus.
- When the response arrives and the spinner disappears, TalkBack does NOT auto-announce the response (same pattern as ConsoleScreen's message list — announcing every new message would be disruptive). The user navigates to it manually.

**Sticky header**:
- `semantics(mergeDescendants = true)` with description: `"ZeroClaw version 0.0.29, status: connected"`
- `LiveRegionMode.Polite` so status changes (connected/disconnected) are announced.

**Input bar**:
- OutlinedTextField relies on M3's built-in label announcement.
- Send button: `semantics { contentDescription = "Send" }`, icon gets `null`.
- Attach images button: `semantics { contentDescription = "Attach images" }`, icon gets `null`.

**Autocomplete popup**:
- Each suggestion item: `semantics { contentDescription = "/cost: Cost summary" }` (command name + description).
- Popup container: `semantics { liveRegion = LiveRegionMode.Polite }` so appearing/changing suggestions are announced.

**Image attachments**:
- Staged images strip: `semantics(mergeDescendants = true) { contentDescription = "2 images attached: photo1.jpg, photo2.jpg" }`
- Remove image button: `semantics { contentDescription = "Remove photo1.jpg" }`, icon gets `null`.

### Color is never the sole differentiator

- Status dots always accompanied by text labels: `● running`, `● healthy`, `○ warning`
- Error blocks use red tint AND a text prefix: `Error:`
- Minimum 4.5:1 contrast ratio for all terminal text against `surfaceContainerLowest` background

### No TalkBack runtime detection

Following existing codebase convention, accessibility is implemented via static semantic annotations. No `AccessibilityManager` checks at runtime.

---

## Files Changed

| File | Action |
|---|---|
| `ConsoleScreen.kt` | **Delete** |
| `ConsoleViewModel.kt` | **Delete** |
| `TerminalScreen.kt` | **Create** |
| `TerminalViewModel.kt` | **Create** |
| `CommandRegistry.kt` | **Create** — slash command definitions + autocomplete data |
| `TerminalBlock.kt` | **Create** — sealed interface for terminal entry types |
| `TerminalOutputRenderer.kt` | **Create** — JSON parsing + Compose rendering for each block type |
| `BrailleSpinner.kt` | **Create** — spinner composable with power-save fallback |
| `MarkdownText.kt` | **Create** — CommonMark subset renderer in monospace |
| `ChatMessage` entity | **Rename** to `TerminalEntry`, Room migration |
| `ChatMessageDao` | **Rename** to `TerminalEntryDao` |
| `ChatMessageRepository` | **Rename** to `TerminalEntryRepository` |
| Navigation route | **Change** `ConsoleRoute` to `TerminalRoute` |
| Bottom nav | **Change** label "Console" to "Terminal" |
| `DaemonServiceBridge` | **Keep** — used by Rhai `send()` |
| `VisionBridge` | **Keep** — used by Rhai `send_vision()` |
| `zeroclaw-ffi/Cargo.toml` | **Edit** — add `rhai` and `serde_json` dependencies |
| `zeroclaw-ffi/src/repl.rs` | **Create** — Rhai engine setup, function registration, `eval_repl` |
| `zeroclaw-ffi/src/lib.rs` | **Edit** — add `mod repl` and export `eval_repl` |
| `README.md` | **Update** — document Terminal REPL feature |
| Console unit tests | **Delete + rewrite** for Terminal |
| `res/font/jetbrains_mono*.ttf` | **Add** — bundled font files |
| `Type.kt` | **Edit** — add terminal font family variant |

---

## Verification Strategy

1. **Source verification**: Read `zeroclaw-ffi/src/lib.rs` at each mapping step. Every Rhai registered function must match the FFI function's exact signature (parameter types, return type, error handling).
2. **Cross-reference bindings**: Check the UniFFI-generated Kotlin bindings to confirm type mapping (e.g. `u16` to `UShort`).
3. **Rust unit tests**: `eval_repl("status()")` returns valid JSON. One test per registered function.
4. **Kotlin parse tests**: Each JSON response type can be parsed into the corresponding `TerminalBlock`.
5. **Accessibility audit**: TalkBack walkthrough at 100% and 200% font scale on a compact device.
6. **Font scale testing**: Visual verification at 100%, 150%, 200% on 360dp, 600dp, and 840dp widths.
