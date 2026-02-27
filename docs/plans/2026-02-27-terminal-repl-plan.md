# Terminal REPL Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the chat-style Console with a terminal-aesthetic REPL powered by a Rhai scripting engine, exposing all 33 FFI functions as slash commands.

**Architecture:** Rhai engine in the Rust FFI crate registers all gateway functions as native calls. A single `eval_repl` FFI export evaluates expressions and returns JSON. Kotlin-side translates slash commands to Rhai expressions and renders JSON results as styled terminal blocks in Compose.

**Tech Stack:** Rhai (Rust scripting), serde_json (serialization), Jetpack Compose (terminal UI), JetBrains Mono (font), Room (persistence)

---

### Task 1: Add Rhai dependency and Serialize derives

**Files:**
- Modify: `zeroclaw-android/zeroclaw-ffi/Cargo.toml`
- Modify: `zeroclaw-android/zeroclaw-ffi/src/cost.rs`
- Modify: `zeroclaw-android/zeroclaw-ffi/src/health.rs`
- Modify: `zeroclaw-android/zeroclaw-ffi/src/cron.rs`
- Modify: `zeroclaw-android/zeroclaw-ffi/src/skills.rs`
- Modify: `zeroclaw-android/zeroclaw-ffi/src/tools_browse.rs`
- Modify: `zeroclaw-android/zeroclaw-ffi/src/memory_browse.rs`

**Step 1: Add rhai to Cargo.toml**

Add to `[dependencies]` section after `chrono`:
```toml
rhai = { version = "1.21", features = ["sync"] }
```

The `sync` feature is required because the engine lives in a `Mutex` shared across FFI calls from the Kotlin IO dispatcher.

**Step 2: Add `serde::Serialize` to all FFI record types**

Each type currently derives `Debug, Clone, uniffi::Record`. Add `serde::Serialize`:

- `cost.rs:18` — `FfiCostSummary`: add `serde::Serialize`
- `cost.rs:35` — `FfiBudgetStatus`: add `serde::Serialize` (enum — need to derive on each variant's data too, but serde handles this automatically with the derive macro on the enum)
- `health.rs:15` — `FfiComponentHealth`: add `serde::Serialize`
- `health.rs:28` — `FfiHealthDetail`: add `serde::Serialize`
- `cron.rs:20` — `FfiCronJob`: add `serde::Serialize`
- `skills.rs:21` — `FfiSkill`: add `serde::Serialize`
- `skills.rs:40` — `FfiSkillTool`: add `serde::Serialize`
- `tools_browse.rs:19` — `FfiToolSpec`: add `serde::Serialize`
- `memory_browse.rs:19` — `FfiMemoryEntry`: add `serde::Serialize`

Example change for each:
```rust
// Before:
#[derive(Debug, Clone, uniffi::Record)]
// After:
#[derive(Debug, Clone, serde::Serialize, uniffi::Record)]
```

**Step 3: Run existing tests to verify no breakage**

Run: `/c/Users/Natal/.cargo/bin/cargo.exe test -p zeroclaw-ffi`
Expected: All 65 existing tests pass. Serialize derive is additive and cannot break existing functionality.

**Step 4: Commit**

```bash
git add zeroclaw-android/zeroclaw-ffi/Cargo.toml zeroclaw-android/zeroclaw-ffi/src/cost.rs zeroclaw-android/zeroclaw-ffi/src/health.rs zeroclaw-android/zeroclaw-ffi/src/cron.rs zeroclaw-android/zeroclaw-ffi/src/skills.rs zeroclaw-android/zeroclaw-ffi/src/tools_browse.rs zeroclaw-android/zeroclaw-ffi/src/memory_browse.rs
git commit -m "build(ffi): add rhai dependency and Serialize derives for REPL"
```

---

### Task 2: Create Rhai engine module with function registration

**Files:**
- Create: `zeroclaw-android/zeroclaw-ffi/src/repl.rs`
- Modify: `zeroclaw-android/zeroclaw-ffi/src/lib.rs`

**Step 1: Write the repl.rs module**

Create `zeroclaw-android/zeroclaw-ffi/src/repl.rs`.

This module:
1. Builds a Rhai `Engine` with `Engine::new_raw()` + `CorePackage` (minimal footprint)
2. Registers all gateway functions as native Rhai functions
3. Calls `_inner` functions directly (avoids double `catch_unwind`)
4. Each registered function serializes its result to JSON via `serde_json`
5. Functions that return `()` return `"ok"` as a string
6. Functions that return primitive types (String, f64, bool, u32) serialize directly

**Critical: Verified function signatures from source**

```
start_daemon(config_toml: String, data_dir: String, host: String, port: u16) -> Result<(), FfiError>
stop_daemon() -> Result<(), FfiError>
get_status() -> Result<String, FfiError>                    // already returns JSON
get_health_detail() -> Result<FfiHealthDetail, FfiError>
get_component_health(name: String) -> Result<Option<FfiComponentHealth>, FfiError>
send_message(message: String) -> Result<String, FfiError>
validate_config(config_toml: String) -> Result<String, FfiError>
doctor_channels(config_toml: String, data_dir: String) -> Result<String, FfiError>
get_version() -> Result<String, FfiError>
scaffold_workspace(workspace_path: String, agent_name: String, user_name: String, timezone: String, communication_style: String) -> Result<(), FfiError>
get_cost_summary() -> Result<FfiCostSummary, FfiError>
get_daily_cost(year: i32, month: u32, day: u32) -> Result<f64, FfiError>
get_monthly_cost(year: i32, month: u32) -> Result<f64, FfiError>
check_budget(estimated_cost_usd: f64) -> Result<FfiBudgetStatus, FfiError>
register_event_listener(listener: Box<dyn FfiEventListener>) -> Result<(), FfiError>  // SKIP: callback
unregister_event_listener() -> Result<(), FfiError>                                    // SKIP: paired with above
get_recent_events(limit: u32) -> Result<String, FfiError>
list_cron_jobs() -> Result<Vec<FfiCronJob>, FfiError>
get_cron_job(id: String) -> Result<Option<FfiCronJob>, FfiError>
add_cron_job(expression: String, command: String) -> Result<FfiCronJob, FfiError>
add_one_shot_job(delay: String, command: String) -> Result<FfiCronJob, FfiError>
remove_cron_job(id: String) -> Result<(), FfiError>
pause_cron_job(id: String) -> Result<(), FfiError>
resume_cron_job(id: String) -> Result<(), FfiError>
list_skills() -> Result<Vec<FfiSkill>, FfiError>
get_skill_tools(skill_name: String) -> Result<Vec<FfiSkillTool>, FfiError>
install_skill(source: String) -> Result<(), FfiError>
remove_skill(name: String) -> Result<(), FfiError>
list_tools() -> Result<Vec<FfiToolSpec>, FfiError>
list_memories(category: Option<String>, limit: u32, session_id: Option<String>) -> Result<Vec<FfiMemoryEntry>, FfiError>
recall_memory(query: String, limit: u32, session_id: Option<String>) -> Result<Vec<FfiMemoryEntry>, FfiError>
forget_memory(key: String) -> Result<bool, FfiError>
send_vision_message(text: String, image_data: Vec<String>, mime_types: Vec<String>) -> Result<String, FfiError>
memory_count() -> Result<u32, FfiError>
```

**Rhai function name mapping (31 functions, skipping 2 callback functions):**

```rust
//! REPL scripting engine backed by Rhai.
//!
//! Registers all gateway functions as native Rhai calls. The engine
//! lives in a `Mutex` inside a `OnceLock`, reused across `eval_repl`
//! invocations from the Kotlin IO dispatcher.

use std::sync::{Mutex, OnceLock};

use rhai::{packages::{CorePackage, Package}, Dynamic, Engine, EvalAltResult};

use crate::{cost, cron, events, health, memory_browse, runtime, skills, tools_browse, vision, workspace};
use crate::error::FfiError;

/// Global REPL engine, lazily initialised on first `eval_repl` call.
static REPL_ENGINE: OnceLock<Mutex<Engine>> = OnceLock::new();

/// Converts an [`FfiError`] into a Rhai runtime error.
fn ffi_err(e: FfiError) -> Box<EvalAltResult> {
    e.to_string().into()
}

/// Builds the Rhai engine and registers all gateway functions.
fn build_engine() -> Engine {
    let mut engine = Engine::new_raw();
    let package = CorePackage::new();
    package.register_into_engine(&mut engine);

    // -- Lifecycle --
    // start/stop/scaffold omitted from REPL: they require config state
    // managed by the Android service layer, not interactive use.

    engine.register_fn("status", || -> Result<String, Box<EvalAltResult>> {
        runtime::get_status_inner().map_err(ffi_err)
    });

    engine.register_fn("version", || -> Result<String, Box<EvalAltResult>> {
        crate::get_version().map_err(ffi_err)
    });

    // -- Messaging --
    engine.register_fn("send", |msg: String| -> Result<String, Box<EvalAltResult>> {
        runtime::send_message_inner(msg).map_err(ffi_err)
    });

    engine.register_fn("send_vision", |text: String, images: rhai::Array, mimes: rhai::Array| -> Result<String, Box<EvalAltResult>> {
        let image_data: Vec<String> = images.into_iter().map(|v| v.into_string().unwrap_or_default()).collect();
        let mime_types: Vec<String> = mimes.into_iter().map(|v| v.into_string().unwrap_or_default()).collect();
        vision::send_vision_message_inner(text, image_data, mime_types).map_err(ffi_err)
    });

    engine.register_fn("validate_config", |toml: String| -> Result<String, Box<EvalAltResult>> {
        runtime::validate_config_inner(toml).map_err(ffi_err)
    });

    // -- Health --
    engine.register_fn("health", || -> Result<String, Box<EvalAltResult>> {
        let detail = health::get_health_detail_inner().map_err(ffi_err)?;
        serde_json::to_string(&detail).map_err(|e| e.to_string().into())
    });

    engine.register_fn("health_component", |name: String| -> Result<String, Box<EvalAltResult>> {
        let comp = health::get_component_health_inner(name);
        serde_json::to_string(&comp).map_err(|e| e.to_string().into())
    });

    engine.register_fn("doctor", |config_toml: String, data_dir: String| -> Result<String, Box<EvalAltResult>> {
        runtime::doctor_channels_inner(config_toml, data_dir).map_err(ffi_err)
    });

    // -- Cost --
    engine.register_fn("cost", || -> Result<String, Box<EvalAltResult>> {
        let summary = cost::get_cost_summary_inner().map_err(ffi_err)?;
        serde_json::to_string(&summary).map_err(|e| e.to_string().into())
    });

    engine.register_fn("cost_daily", |year: i64, month: i64, day: i64| -> Result<Dynamic, Box<EvalAltResult>> {
        let result = cost::get_daily_cost_inner(year as i32, month as u32, day as u32).map_err(ffi_err)?;
        Ok(Dynamic::from_float(result))
    });

    engine.register_fn("cost_monthly", |year: i64, month: i64| -> Result<Dynamic, Box<EvalAltResult>> {
        let result = cost::get_monthly_cost_inner(year as i32, month as u32).map_err(ffi_err)?;
        Ok(Dynamic::from_float(result))
    });

    engine.register_fn("budget", |estimated: f64| -> Result<String, Box<EvalAltResult>> {
        let status = cost::check_budget_inner(estimated).map_err(ffi_err)?;
        serde_json::to_string(&status).map_err(|e| e.to_string().into())
    });

    // -- Events --
    engine.register_fn("events", |limit: i64| -> Result<String, Box<EvalAltResult>> {
        events::get_recent_events_inner(limit as u32).map_err(ffi_err)
    });

    // -- Cron --
    engine.register_fn("cron_list", || -> Result<String, Box<EvalAltResult>> {
        let jobs = cron::list_cron_jobs_inner().map_err(ffi_err)?;
        serde_json::to_string(&jobs).map_err(|e| e.to_string().into())
    });

    engine.register_fn("cron_get", |id: String| -> Result<String, Box<EvalAltResult>> {
        let job = cron::get_cron_job_inner(id).map_err(ffi_err)?;
        serde_json::to_string(&job).map_err(|e| e.to_string().into())
    });

    engine.register_fn("cron_add", |expression: String, command: String| -> Result<String, Box<EvalAltResult>> {
        let job = cron::add_cron_job_inner(expression, command).map_err(ffi_err)?;
        serde_json::to_string(&job).map_err(|e| e.to_string().into())
    });

    engine.register_fn("cron_oneshot", |delay: String, command: String| -> Result<String, Box<EvalAltResult>> {
        let job = cron::add_one_shot_job_inner(delay, command).map_err(ffi_err)?;
        serde_json::to_string(&job).map_err(|e| e.to_string().into())
    });

    engine.register_fn("cron_remove", |id: String| -> Result<String, Box<EvalAltResult>> {
        cron::remove_cron_job_inner(id).map_err(ffi_err)?;
        Ok("ok".into())
    });

    engine.register_fn("cron_pause", |id: String| -> Result<String, Box<EvalAltResult>> {
        cron::pause_cron_job_inner(id).map_err(ffi_err)?;
        Ok("ok".into())
    });

    engine.register_fn("cron_resume", |id: String| -> Result<String, Box<EvalAltResult>> {
        cron::resume_cron_job_inner(id).map_err(ffi_err)?;
        Ok("ok".into())
    });

    // -- Skills --
    engine.register_fn("skills", || -> Result<String, Box<EvalAltResult>> {
        let list = skills::list_skills_inner().map_err(ffi_err)?;
        serde_json::to_string(&list).map_err(|e| e.to_string().into())
    });

    engine.register_fn("skill_tools", |name: String| -> Result<String, Box<EvalAltResult>> {
        let tools = skills::get_skill_tools_inner(name).map_err(ffi_err)?;
        serde_json::to_string(&tools).map_err(|e| e.to_string().into())
    });

    engine.register_fn("skill_install", |source: String| -> Result<String, Box<EvalAltResult>> {
        skills::install_skill_inner(source).map_err(ffi_err)?;
        Ok("ok".into())
    });

    engine.register_fn("skill_remove", |name: String| -> Result<String, Box<EvalAltResult>> {
        skills::remove_skill_inner(name).map_err(ffi_err)?;
        Ok("ok".into())
    });

    // -- Tools --
    engine.register_fn("tools", || -> Result<String, Box<EvalAltResult>> {
        let list = tools_browse::list_tools_inner().map_err(ffi_err)?;
        serde_json::to_string(&list).map_err(|e| e.to_string().into())
    });

    // -- Memory --
    engine.register_fn("memories", |limit: i64| -> Result<String, Box<EvalAltResult>> {
        let entries = memory_browse::list_memories_inner(None, limit as u32, None).map_err(ffi_err)?;
        serde_json::to_string(&entries).map_err(|e| e.to_string().into())
    });

    engine.register_fn("memories_by_category", |category: String, limit: i64| -> Result<String, Box<EvalAltResult>> {
        let entries = memory_browse::list_memories_inner(Some(category), limit as u32, None).map_err(ffi_err)?;
        serde_json::to_string(&entries).map_err(|e| e.to_string().into())
    });

    engine.register_fn("memory_recall", |query: String, limit: i64| -> Result<String, Box<EvalAltResult>> {
        let entries = memory_browse::recall_memory_inner(query, limit as u32, None).map_err(ffi_err)?;
        serde_json::to_string(&entries).map_err(|e| e.to_string().into())
    });

    engine.register_fn("memory_forget", |key: String| -> Result<bool, Box<EvalAltResult>> {
        memory_browse::forget_memory_inner(key).map_err(ffi_err)
    });

    engine.register_fn("memory_count", || -> Result<i64, Box<EvalAltResult>> {
        let count = memory_browse::memory_count_inner().map_err(ffi_err)?;
        Ok(i64::from(count))
    });

    engine
}

/// Evaluates a Rhai expression against the gateway and returns the result as a string.
///
/// This is the single FFI entry point for the terminal REPL. All slash
/// commands are translated to Rhai expressions Kotlin-side before calling
/// this function.
///
/// Results are JSON strings for structured data, plain strings for text
/// responses, or `"ok"` for void operations.
pub(crate) fn eval_repl_inner(expression: String) -> Result<String, FfiError> {
    let engine_mutex = REPL_ENGINE.get_or_init(|| Mutex::new(build_engine()));
    let engine = engine_mutex.lock().map_err(|_| FfiError::StateCorrupted {
        detail: "REPL engine mutex poisoned".into(),
    })?;

    let result = engine.eval::<Dynamic>(&expression).map_err(|e| FfiError::SpawnError {
        detail: e.to_string(),
    })?;

    // Convert Dynamic to string — if it's already a string, return as-is.
    // Otherwise serialize to JSON.
    if result.is_string() {
        Ok(result.into_string().unwrap_or_default())
    } else if result.is_bool() {
        Ok(result.as_bool().map_or("false".into(), |b| b.to_string()))
    } else if result.is_int() {
        Ok(result.as_int().map_or("0".into(), |i| i.to_string()))
    } else if result.is_float() {
        Ok(result.as_float().map_or("0.0".into(), |f| f.to_string()))
    } else if result.is_unit() {
        Ok("ok".into())
    } else {
        Ok(format!("{result}"))
    }
}
```

**Step 2: Wire into lib.rs**

Add `mod repl;` after `mod workspace;` (line 30 of lib.rs).

Add the exported function after the `memory_count` export (after line 742):

```rust
/// Evaluates a Rhai expression in the REPL engine.
///
/// Slash commands from the Android terminal are translated to Rhai
/// expressions Kotlin-side, then evaluated here. Returns JSON strings
/// for structured data, plain strings for text, or `"ok"` for void ops.
///
/// # Errors
///
/// Returns [`FfiError::SpawnError`] for Rhai evaluation errors,
/// [`FfiError::StateCorrupted`] if the engine mutex is poisoned, or
/// [`FfiError::InternalPanic`] if native code panics.
#[uniffi::export]
pub fn eval_repl(expression: String) -> Result<String, FfiError> {
    catch_unwind(AssertUnwindSafe(|| repl::eval_repl_inner(expression))).unwrap_or_else(|e| {
        Err(FfiError::InternalPanic {
            detail: panic_detail(&e),
        })
    })
}
```

**Step 3: Run tests**

Run: `/c/Users/Natal/.cargo/bin/cargo.exe test -p zeroclaw-ffi`
Expected: All existing tests pass + new module compiles.

**Step 4: Commit**

```bash
git add zeroclaw-android/zeroclaw-ffi/src/repl.rs zeroclaw-android/zeroclaw-ffi/src/lib.rs
git commit -m "feat(ffi): add Rhai REPL engine with all gateway functions registered"
```

---

### Task 3: Write Rust tests for eval_repl

**Files:**
- Modify: `zeroclaw-android/zeroclaw-ffi/src/repl.rs` (add tests module)

**Step 1: Write tests**

Add at the bottom of `repl.rs`:

```rust
#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;

    #[test]
    fn test_eval_version() {
        let result = eval_repl_inner("version()".into()).unwrap();
        assert_eq!(result, env!("CARGO_PKG_VERSION"));
    }

    #[test]
    fn test_eval_status_returns_json() {
        let result = eval_repl_inner("status()".into()).unwrap();
        let parsed: serde_json::Value = serde_json::from_str(&result).unwrap();
        assert!(parsed.get("daemon_running").is_some());
    }

    #[test]
    fn test_eval_health_returns_json() {
        let result = eval_repl_inner("health()".into()).unwrap();
        let parsed: serde_json::Value = serde_json::from_str(&result).unwrap();
        assert!(parsed.get("daemon_running").is_some());
    }

    #[test]
    fn test_eval_send_not_running() {
        let result = eval_repl_inner("send(\"hello\")".into());
        assert!(result.is_err());
    }

    #[test]
    fn test_eval_memory_count_not_running() {
        let result = eval_repl_inner("memory_count()".into());
        assert!(result.is_err());
    }

    #[test]
    fn test_eval_invalid_expression() {
        let result = eval_repl_inner("this is not valid rhai {{{{".into());
        assert!(result.is_err());
    }

    #[test]
    fn test_eval_arithmetic() {
        let result = eval_repl_inner("40 + 2".into()).unwrap();
        assert_eq!(result, "42");
    }

    #[test]
    fn test_eval_string_literal() {
        let result = eval_repl_inner("\"hello world\"".into()).unwrap();
        assert_eq!(result, "hello world");
    }
}
```

**Step 2: Run tests**

Run: `/c/Users/Natal/.cargo/bin/cargo.exe test -p zeroclaw-ffi`
Expected: All tests pass (version, status, health succeed; send/memory_count fail with StateError as expected).

**Step 3: Commit**

```bash
git add zeroclaw-android/zeroclaw-ffi/src/repl.rs
git commit -m "test(ffi): add eval_repl unit tests for REPL engine"
```

---

### Task 4: Bundle JetBrains Mono font and create terminal typography

**Files:**
- Create: `app/src/main/res/font/jetbrains_mono_regular.ttf`
- Create: `app/src/main/res/font/jetbrains_mono_bold.ttf`
- Modify: `app/src/main/java/com/zeroclaw/android/ui/theme/Type.kt`

**Step 1: Download JetBrains Mono font files**

Download from the JetBrains Mono GitHub releases (OFL license). Place:
- `JetBrainsMono-Regular.ttf` → `app/src/main/res/font/jetbrains_mono_regular.ttf`
- `JetBrainsMono-Bold.ttf` → `app/src/main/res/font/jetbrains_mono_bold.ttf`

Filenames must be lowercase with underscores per Android resource naming rules.

**Step 2: Create terminal FontFamily and Typography in Type.kt**

Read the current `Type.kt` first. Add:

```kotlin
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** JetBrains Mono font family for the terminal REPL screen. */
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/**
 * Terminal-specific typography using JetBrains Mono.
 *
 * Inherits M3 default sizes and line heights (all in sp) for correct
 * behaviour under Android 14+ nonlinear font scaling. Only overrides
 * the font family.
 */
val TerminalTypography = Typography(
    bodyLarge = Typography().bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
    bodySmall = Typography().bodySmall.copy(fontFamily = JetBrainsMonoFamily),
    labelLarge = Typography().labelLarge.copy(fontFamily = JetBrainsMonoFamily),
    labelMedium = Typography().labelMedium.copy(fontFamily = JetBrainsMonoFamily),
    labelSmall = Typography().labelSmall.copy(fontFamily = JetBrainsMonoFamily),
    titleLarge = Typography().titleLarge.copy(fontFamily = JetBrainsMonoFamily),
    titleMedium = Typography().titleMedium.copy(fontFamily = JetBrainsMonoFamily),
    titleSmall = Typography().titleSmall.copy(fontFamily = JetBrainsMonoFamily),
)
```

**Step 3: Verify build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: Compiles without errors.

**Step 4: Commit**

```bash
git add app/src/main/res/font/ app/src/main/java/com/zeroclaw/android/ui/theme/Type.kt
git commit -m "feat(ui): bundle JetBrains Mono and add terminal typography"
```

---

### Task 5: Create TerminalEntry model, Room entity, DAO, and repository

**Files:**
- Create: `app/src/main/java/com/zeroclaw/android/model/TerminalEntry.kt`
- Create: `app/src/main/java/com/zeroclaw/android/data/local/entity/TerminalEntryEntity.kt`
- Create: `app/src/main/java/com/zeroclaw/android/data/local/dao/TerminalEntryDao.kt`
- Create: `app/src/main/java/com/zeroclaw/android/data/repository/TerminalEntryRepository.kt`
- Create: `app/src/main/java/com/zeroclaw/android/data/repository/RoomTerminalEntryRepository.kt`
- Modify: `app/src/main/java/com/zeroclaw/android/data/local/ZeroClawDatabase.kt` (add entity + DAO, bump version)

**Step 1: Create the domain model**

`TerminalEntry.kt`:
```kotlin
package com.zeroclaw.android.model

/**
 * A single entry in the terminal REPL scrollback.
 *
 * @property id Auto-generated primary key from Room (0 for unsaved entries).
 * @property content The text content (user input, response JSON, or error message).
 * @property entryType The type of entry: "input", "response", "error", or "system".
 * @property timestamp Epoch milliseconds when the entry was created.
 * @property imageUris Content URIs of images attached to this entry (empty for text-only).
 */
data class TerminalEntry(
    val id: Long = 0,
    val content: String,
    val entryType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUris: List<String> = emptyList(),
)
```

**Step 2: Create Room entity, DAO, repository interface, and Room implementation**

Follow the exact patterns from the existing `ChatMessageEntity`, `ChatMessageDao`, `ChatMessageRepository`, and `RoomChatMessageRepository`. Use the same column names where possible to enable Room auto-migration from the old `chat_messages` table.

Key differences from ChatMessage:
- Replace `isFromUser: Boolean` with `entryType: String` (supports "input", "response", "error", "system")
- Keep `imageUris` as a JSON column with TypeConverter (same pattern as existing)

**Step 3: Add entity to database, bump version to 8, add migration**

In `ZeroClawDatabase.kt`:
- Add `TerminalEntryEntity::class` to the `entities` array
- Add `abstract fun terminalEntryDao(): TerminalEntryDao`
- Bump `version = 8`
- Add migration from 7 to 8 that creates the new table

Keep the old `ChatMessageEntity` and table temporarily for the Room migration. It will be removed in the cleanup task.

**Step 4: Wire into ZeroClawApplication.kt**

Add `val terminalEntryRepository: TerminalEntryRepository` alongside the existing `chatMessageRepository`.

**Step 5: Verify build**

Run: `./gradlew :app:compileDebugKotlin`

**Step 6: Commit**

```bash
git add app/src/main/java/com/zeroclaw/android/model/TerminalEntry.kt \
  app/src/main/java/com/zeroclaw/android/data/local/entity/TerminalEntryEntity.kt \
  app/src/main/java/com/zeroclaw/android/data/local/dao/TerminalEntryDao.kt \
  app/src/main/java/com/zeroclaw/android/data/repository/TerminalEntryRepository.kt \
  app/src/main/java/com/zeroclaw/android/data/repository/RoomTerminalEntryRepository.kt \
  app/src/main/java/com/zeroclaw/android/data/local/ZeroClawDatabase.kt \
  app/src/main/java/com/zeroclaw/android/ZeroClawApplication.kt
git commit -m "feat(data): add TerminalEntry entity, DAO, and repository for REPL persistence"
```

---

### Task 6: Create CommandRegistry and slash command definitions

**Files:**
- Create: `app/src/main/java/com/zeroclaw/android/ui/screen/terminal/CommandRegistry.kt`

**Step 1: Create the command registry**

This file defines all slash commands and their translation to Rhai expressions. Must match the exact Rhai function names from Task 2.

```kotlin
package com.zeroclaw.android.ui.screen.terminal

/**
 * Definition of a slash command available in the terminal REPL.
 *
 * @property name The command name without the leading slash (e.g. "status").
 * @property description Brief description shown in autocomplete.
 * @property subcommands Available subcommands (empty if none).
 * @property usage Usage hint shown when args are required.
 * @property toExpression Translates the command + args into a Rhai expression string.
 */
data class SlashCommand(
    val name: String,
    val description: String,
    val subcommands: List<String> = emptyList(),
    val usage: String = "",
    val toExpression: (args: List<String>) -> String,
)
```

Register all commands mapping to the Rhai functions. Use the verified signatures:

- `/status` → `status()`
- `/version` → `version()`
- `/health` → `health()` ; `/health <component>` → `health_component("component")`
- `/doctor <config_path> <data_dir>` → `doctor("...", "...")`
- `/cost` → `cost()` ; `/cost daily <y> <m> <d>` → `cost_daily(y, m, d)` ; `/cost monthly <y> <m>` → `cost_monthly(y, m)`
- `/budget <amount>` → `budget(amount)`
- `/events <limit>` → `events(limit)` (default 20)
- `/cron` → `cron_list()` ; `/cron get <id>` → `cron_get("id")` ; `/cron add <expr> <cmd>` → `cron_add("expr", "cmd")` ; etc.
- `/skills` → `skills()` ; `/skills tools <name>` → `skill_tools("name")` ; etc.
- `/tools` → `tools()`
- `/memories` → `memories(50)` ; `/memories <category>` → `memories_by_category("cat", 50)`
- `/memory recall <query>` → `memory_recall("query", 20)` ; `/memory forget <key>` → `memory_forget("key")` ; `/memory count` → `memory_count()`
- `/help` → handled locally
- `/clear` → handled locally

**Step 2: Verify build**

Run: `./gradlew :app:compileDebugKotlin`

**Step 3: Commit**

```bash
git add app/src/main/java/com/zeroclaw/android/ui/screen/terminal/CommandRegistry.kt
git commit -m "feat(terminal): add CommandRegistry with all 33 slash command definitions"
```

---

### Task 7: Create TerminalBlock sealed interface and output renderer

**Files:**
- Create: `app/src/main/java/com/zeroclaw/android/ui/screen/terminal/TerminalBlock.kt`
- Create: `app/src/main/java/com/zeroclaw/android/ui/screen/terminal/TerminalOutputRenderer.kt`

**Step 1: Create TerminalBlock**

Sealed interface representing each type of entry in the terminal scrollback:

```kotlin
sealed interface TerminalBlock {
    val id: Long
    val timestamp: Long

    /** User-typed input (command or message). */
    data class Input(override val id: Long, override val timestamp: Long, val text: String, val imageNames: List<String> = emptyList()) : TerminalBlock

    /** Agent or gateway response text (may contain markdown). */
    data class Response(override val id: Long, override val timestamp: Long, val content: String) : TerminalBlock

    /** Structured JSON output from a slash command. */
    data class Structured(override val id: Long, override val timestamp: Long, val json: String) : TerminalBlock

    /** Error message. */
    data class Error(override val id: Long, override val timestamp: Long, val message: String) : TerminalBlock

    /** System message (welcome, clear confirmation, etc.). */
    data class System(override val id: Long, override val timestamp: Long, val text: String) : TerminalBlock
}
```

**Step 2: Create TerminalOutputRenderer**

Composable functions that render each `TerminalBlock` variant. Uses `TerminalTypography` throughout.

Key rendering rules:
- `Input` → monospace, primary color, prefixed with `> `
- `Response` → monospace, `onSurface` color, markdown rendered (bold, italic, code blocks, lists)
- `Structured` → parse JSON, detect `type` field, render appropriate layout (status table, list, cost summary, etc.)
- `Error` → monospace, `error` color, prefixed with `Error: `
- `System` → monospace, `outline` color (dimmed)

For the `Structured` variant, parse JSON and detect:
- Objects with fields like `daemon_running`, `components` → status table
- Arrays of objects with `name`, `description` → numbered list
- Objects with `session_cost_usd` → cost summary
- Objects with `id`, `expression`, `command` → cron job card(s)
- Fall back to pretty-printed JSON for unrecognized structures

**Step 3: Verify build**

Run: `./gradlew :app:compileDebugKotlin`

**Step 4: Commit**

```bash
git add app/src/main/java/com/zeroclaw/android/ui/screen/terminal/TerminalBlock.kt \
  app/src/main/java/com/zeroclaw/android/ui/screen/terminal/TerminalOutputRenderer.kt
git commit -m "feat(terminal): add TerminalBlock sealed interface and output renderers"
```

---

### Task 8: Create BrailleSpinner composable

**Files:**
- Create: `app/src/main/java/com/zeroclaw/android/ui/screen/terminal/BrailleSpinner.kt`

**Step 1: Create the spinner**

```kotlin
/**
 * Braille character spinner matching Claude Code CLI's thinking indicator.
 *
 * Cycles through `⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏` at 80ms intervals. Falls back to
 * static `...` when [LocalPowerSaveMode] is true (battery saver active).
 */
@Composable
fun BrailleSpinner(label: String, modifier: Modifier = Modifier) { ... }
```

Frames: `charArrayOf('⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏')`
Interval: 80ms via `LaunchedEffect` + `delay`
Power save: check `LocalPowerSaveMode.current`, show static "..." instead
Accessibility: `LiveRegionMode.Polite` on the container, `contentDescription = label`

**Step 2: Commit**

```bash
git add app/src/main/java/com/zeroclaw/android/ui/screen/terminal/BrailleSpinner.kt
git commit -m "feat(terminal): add BrailleSpinner composable with power-save fallback"
```

---

### Task 9: Create TerminalViewModel

**Files:**
- Create: `app/src/main/java/com/zeroclaw/android/ui/screen/terminal/TerminalViewModel.kt`

**Step 1: Create the ViewModel**

Follows the same patterns as `ConsoleViewModel` but routes through `eval_repl` FFI. Key responsibilities:

1. Parse input: if starts with `/`, look up in `CommandRegistry`, translate to Rhai expression
2. If plain text, wrap as `send("escaped text")` Rhai call
3. If `/help` or `/clear`, handle locally
4. Call `evalRepl(expression)` via the UniFFI-generated binding on `Dispatchers.IO`
5. Persist input and response as `TerminalEntry` in Room
6. Expose `StateFlow<List<TerminalBlock>>` for the UI
7. Maintain command history list for up-swipe navigation
8. Handle image attachment via the same base64 pipeline as ConsoleViewModel

State:
```kotlin
data class TerminalState(
    val blocks: List<TerminalBlock>,
    val isLoading: Boolean,
    val pendingImages: List<ProcessedImage>,
    val isProcessingImages: Boolean,
    val commandHistory: List<String>,
    val historyIndex: Int,
)
```

**Step 2: Verify build**

Run: `./gradlew :app:compileDebugKotlin`

**Step 3: Commit**

```bash
git add app/src/main/java/com/zeroclaw/android/ui/screen/terminal/TerminalViewModel.kt
git commit -m "feat(terminal): add TerminalViewModel with eval_repl routing and command history"
```

---

### Task 10: Create TerminalScreen composable

**Files:**
- Create: `app/src/main/java/com/zeroclaw/android/ui/screen/terminal/TerminalScreen.kt`

**Step 1: Create the screen**

Main composable with:
1. **Sticky header**: version + status dot (reuse `StatusDot` from existing code)
2. **Scrollback**: `LazyColumn(reverseLayout = true)` rendering `TerminalBlock` items via `TerminalOutputRenderer`
3. **Input bar**: monospace `OutlinedTextField` with `>` prompt, send button, attach button
4. **Autocomplete popup**: shown when input starts with `/`, filters `CommandRegistry.commands`
5. **Spinner**: `BrailleSpinner("Thinking...")` shown when `isLoading`
6. **Image strip**: compact text format above input when images staged

Background: `MaterialTheme.colorScheme.surfaceContainerLowest`

Accessibility (following codebase patterns):
- Each `TerminalBlock` item: `semantics(mergeDescendants = true) { contentDescription = ... }`
- Input blocks: `"Command: /status"` or `"Message: what's the weather"`
- Response blocks: plain-text version of content
- Structured blocks: linearized summary
- Error blocks: `"Error: ..."`
- Spinner container: `liveRegion = LiveRegionMode.Polite`
- Header: `semantics(mergeDescendants = true)`, `liveRegion = LiveRegionMode.Polite`
- Send button: `semantics { contentDescription = "Send" }`, icon `null`
- Attach button: `semantics { contentDescription = "Attach images" }`, icon `null`
- Autocomplete items: `semantics { contentDescription = "/cost: Cost summary" }`
- Long-press to copy on output blocks

Split into `TerminalScreen` (wired to ViewModel) and `TerminalContent` (stateless, testable).

**Step 2: Verify build**

Run: `./gradlew :app:compileDebugKotlin`

**Step 3: Commit**

```bash
git add app/src/main/java/com/zeroclaw/android/ui/screen/terminal/TerminalScreen.kt
git commit -m "feat(terminal): add TerminalScreen with terminal aesthetic and accessibility"
```

---

### Task 11: Wire navigation and update bottom nav

**Files:**
- Modify: `app/src/main/java/com/zeroclaw/android/navigation/Route.kt`
- Modify: `app/src/main/java/com/zeroclaw/android/navigation/ZeroClawNavHost.kt`
- Modify: `app/src/main/java/com/zeroclaw/android/navigation/TopLevelDestination.kt`

**Step 1: Add TerminalRoute**

In `Route.kt`, add after the existing `ConsoleRoute`:
```kotlin
@Serializable
data object TerminalRoute
```

**Step 2: Register in NavHost**

In `ZeroClawNavHost.kt`, replace the `ConsoleRoute` composable with:
```kotlin
composable<TerminalRoute> {
    TerminalScreen(edgeMargin = edgeMargin)
}
```

**Step 3: Update TopLevelDestination**

In `TopLevelDestination.kt`, change the `CONSOLE` entry:
- Label: `"Console"` → `"Terminal"`
- Route: `ConsoleRoute` → `TerminalRoute`
- Icon: keep `Icons.Filled.Terminal` / `Icons.Outlined.Terminal` (or `DataObject` if Terminal isn't available in the current M3 icon set — verify availability)

**Step 4: Verify build and navigation**

Run: `./gradlew :app:assembleDebug`

**Step 5: Commit**

```bash
git add app/src/main/java/com/zeroclaw/android/navigation/Route.kt \
  app/src/main/java/com/zeroclaw/android/navigation/ZeroClawNavHost.kt \
  app/src/main/java/com/zeroclaw/android/navigation/TopLevelDestination.kt
git commit -m "feat(nav): replace Console route with Terminal in navigation and bottom bar"
```

---

### Task 12: Delete Console files and old tests

**Files:**
- Delete: `app/src/main/java/com/zeroclaw/android/ui/screen/console/ConsoleScreen.kt`
- Delete: `app/src/main/java/com/zeroclaw/android/ui/screen/console/ConsoleViewModel.kt`
- Delete: `app/src/androidTest/java/com/zeroclaw/android/screen/ConsoleScreenTest.kt`
- Modify: `app/src/androidTest/java/com/zeroclaw/android/screen/helpers/FakeData.kt` (remove `fakeConsoleState`)
- Remove `ConsoleRoute` from `Route.kt` if not already done

**Step 1: Delete files**

Remove the 3 Console files and the ConsoleRoute definition. Update FakeData to remove the console helper.

**Step 2: Clean up any remaining Console imports**

Search for `import.*Console` across the codebase and remove dead imports.

**Step 3: Verify build**

Run: `./gradlew :app:assembleDebug`
Expected: Compiles. No references to Console remain.

**Step 4: Commit**

```bash
git add -A
git commit -m "refactor: remove deprecated Console screen, ViewModel, and tests"
```

---

### Task 13: Write Terminal unit and UI tests

**Files:**
- Create: `app/src/test/java/com/zeroclaw/android/ui/screen/terminal/CommandRegistryTest.kt`
- Create: `app/src/test/java/com/zeroclaw/android/ui/screen/terminal/TerminalViewModelTest.kt`
- Create: `app/src/androidTest/java/com/zeroclaw/android/screen/TerminalScreenTest.kt`

**Step 1: CommandRegistry tests**

Test that each slash command generates the expected Rhai expression:
```kotlin
@Test
fun `status command generates correct expression`() {
    val cmd = CommandRegistry.find("status")
    assertNotNull(cmd)
    assertEquals("status()", cmd.toExpression(emptyList()))
}

@Test
fun `cost daily command with args generates correct expression`() {
    val cmd = CommandRegistry.find("cost")
    assertNotNull(cmd)
    assertEquals("cost_daily(2026, 2, 27)", cmd.toExpression(listOf("daily", "2026", "2", "27")))
}
```

One test per command family.

**Step 2: TerminalViewModel tests**

Mock `evalRepl` using MockK (same pattern as existing ViewModel tests). Test:
- Plain text routes through `send("text")`
- Slash commands translate correctly
- `/help` handled locally
- `/clear` clears the block list
- Error responses create Error blocks
- Command history tracks inputs

**Step 3: TerminalScreen UI tests**

Compose test rule with `TerminalContent` (stateless composable). Test:
- Input blocks render with `>` prefix
- Spinner shows when loading
- Autocomplete popup appears on `/` input
- Long-press triggers copy

**Step 4: Run tests**

Run: `./gradlew :app:testDebugUnitTest`
Run: `./gradlew :app:connectedDebugAndroidTest` (if emulator available)

**Step 5: Commit**

```bash
git add app/src/test/ app/src/androidTest/
git commit -m "test(terminal): add CommandRegistry, ViewModel, and UI tests for Terminal REPL"
```

---

### Task 14: Update README.md

**Files:**
- Modify: `README.md`

**Step 1: Update the README**

Add a "Terminal REPL" section documenting:
- The terminal replaces the old Console screen
- Slash commands list (all commands with brief descriptions)
- Power user: raw Rhai expressions supported
- Image support via `/send_vision` or attach button
- Keyboard: command history via swipe-up

Update the architecture overview to mention the Rhai engine layer.

Update the phase list to include "Phase 9: Terminal REPL" as complete.

**Step 2: Commit**

```bash
git add README.md
git commit -m "docs: document Terminal REPL feature in README"
```

---

### Task 15: Run full lint and test suite

**Files:** None (validation only)

**Step 1: Rust checks**

```bash
/c/Users/Natal/.cargo/bin/cargo.exe clippy -p zeroclaw-ffi --all-targets -- -D warnings
/c/Users/Natal/.cargo/bin/cargo.exe fmt -p zeroclaw-ffi --check
/c/Users/Natal/.cargo/bin/cargo.exe test -p zeroclaw-ffi
```

**Step 2: Kotlin checks**

```bash
./gradlew spotlessCheck
./gradlew detekt
./gradlew :app:testDebugUnitTest
```

**Step 3: Full build**

```bash
./gradlew :app:assembleDebug
```

**Step 4: Fix any issues and commit fixes**

If lint/test failures occur, fix and commit with appropriate conventional commit message.

---

### Task 16: Trigger UI state refresh after mutating REPL commands

**Files:**
- Modify: `app/src/main/java/com/zeroclaw/android/ui/screen/terminal/TerminalViewModel.kt`

**Context:** The app uses `DaemonServiceBridge` (StateFlow-based) as the single source of truth. Other bridges (Health, Cost, Cron, Skills, Tools, Memory) poll at 15-30 second intervals. When the REPL executes a mutating command (cron add, skill install, memory forget, etc.), the Dashboard and other screens won't reflect the change until the next poll cycle. We need to trigger an immediate refresh.

**Step 1: Identify mutating commands**

Commands that change daemon state:
- `cron_add`, `cron_oneshot`, `cron_remove`, `cron_pause`, `cron_resume` → refresh CronBridge
- `skill_install`, `skill_remove` → refresh SkillsBridge
- `memory_forget` → refresh MemoryBridge
- `send`, `send_vision` → refresh CostBridge (new cost incurred)

Read-only commands (no refresh needed): `status`, `version`, `health`, `cost`, `events`, `skills` (list), `tools`, `memories`, `memory_recall`, `memory_count`, `budget`, `doctor`, `validate_config`

**Step 2: Add refresh dispatch in TerminalViewModel**

After a successful `eval_repl` call for a mutating command, call the corresponding bridge's refresh method. The bridges are accessible via `ZeroClawApplication`:

```kotlin
private fun refreshAfterCommand(commandName: String) {
    when (commandName) {
        "cron_add", "cron_oneshot", "cron_remove", "cron_pause", "cron_resume" ->
            app.cronBridge.refresh()
        "skill_install", "skill_remove" ->
            app.skillsBridge.refresh()
        "memory_forget" ->
            app.memoryBridge.refresh()
        "send", "send_vision" ->
            app.costBridge.refresh()
    }
}
```

The exact refresh method names need to be verified against the bridge classes. Look for `refresh()`, `poll()`, `fetchLatest()`, or similar methods. If none exist, add a `refresh()` method that re-triggers the polling coroutine immediately.

**Step 3: Wire into the eval flow**

In the `submitInput()` method, after `eval_repl` returns successfully, determine which Rhai function was called (from the command name or expression) and call `refreshAfterCommand()`.

**Step 4: Verify cross-screen updates**

Manual test: type `/cron add "0 * * * *" "echo test"` in the terminal, switch to Dashboard, verify the cron job appears immediately without waiting for the poll cycle.

**Step 5: Commit**

```bash
git add app/src/main/java/com/zeroclaw/android/ui/screen/terminal/TerminalViewModel.kt
git commit -m "feat(terminal): trigger immediate bridge refresh after mutating REPL commands"
```

---

### Task 17: Remove old ChatMessage infrastructure (cleanup)

**Files:**
- Delete: `app/src/main/java/com/zeroclaw/android/model/ChatMessage.kt`
- Delete: `app/src/main/java/com/zeroclaw/android/data/local/dao/ChatMessageDao.kt`
- Delete: `app/src/main/java/com/zeroclaw/android/data/local/entity/ChatMessageEntity.kt`
- Delete: `app/src/main/java/com/zeroclaw/android/data/repository/ChatMessageRepository.kt`
- Delete: `app/src/main/java/com/zeroclaw/android/data/repository/RoomChatMessageRepository.kt`
- Modify: `app/src/main/java/com/zeroclaw/android/data/local/ZeroClawDatabase.kt` (remove ChatMessageEntity from entities, remove chatMessageDao method)
- Modify: `app/src/main/java/com/zeroclaw/android/ZeroClawApplication.kt` (remove chatMessageRepository)

**Step 1: Remove all ChatMessage files**

Delete the 5 ChatMessage-related files.

**Step 2: Update database**

Remove `ChatMessageEntity` from the `@Database` entities array. Remove the `chatMessageDao()` abstract function. Add a Room migration that drops the `chat_messages` table.

**Step 3: Remove from Application**

Remove `chatMessageRepository` field and initialization from `ZeroClawApplication.kt`.

**Step 4: Search for any remaining references**

Grep for `ChatMessage`, `chatMessage`, `chat_message` across the codebase. Fix any remaining references.

**Step 5: Verify build and tests**

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

**Step 6: Commit**

```bash
git add -A
git commit -m "refactor: remove deprecated ChatMessage infrastructure"
```
