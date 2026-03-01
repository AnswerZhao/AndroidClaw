# Phase 9: Upstream Gap Closure Design

**Date:** 2026-03-01
**Version:** v0.0.32 (baseline)
**Scope:** 13 items across 4 sub-phases closing gaps between the Android wrapper and a normal ZeroClaw install.

## Key Discovery

`ConfigTomlBuilder.kt` (lines 542-552) and `GlobalTomlConfig` (lines 178-291) **already implement** all config section emission for web_fetch, web_search, http_request, multimodal, transcription, security.resources, security.otp, and security.estop. The remaining work is:

1. **Settings UI screens** to let users edit these fields (values currently hardcoded to defaults)
2. **SettingsRepository persistence** to read/write these fields to Room/DataStore
3. **New FFI functions** for runtime features (E-Stop, traces, auth profiles, config read, cron variants, provider hot-swap)
4. **Doctor enhancements** (channels, traces)
5. **Restart banner cleanup** when provider hot-swap lands

---

## Phase 9A: Safety & Limits

### 9A-1: Emergency Stop (Kill-All)

**What upstream has:**
- `zeroclaw/src/security/estop.rs` — `EstopManager` with `EstopState` struct (lines 26-38): `kill_all: bool`, `network_kill: bool`, `blocked_domains: Vec<String>`, `frozen_tools: Vec<String>`, `updated_at: Option<String>`
- `EstopLevel` enum (lines 10-16): `KillAll`, `NetworkKill`, `DomainBlock(Vec<String>)`, `ToolFreeze(Vec<String>)`
- State persisted to `{config_dir}/estop-state.json` (line 254), atomic write with temp file + rename
- CLI handler in `zeroclaw/src/main.rs`: `Estop { level, domains, tools }` + subcommands `Status`, `Resume`

**What we build (kill-all only, per design decision):**

**New Rust FFI module: `estop.rs`**
- `engage_estop() -> Result<(), FfiError>` — sets `AtomicBool` in static `ESTOP_ENGAGED`, cancels any active session (`session_cancel_inner()`), writes `{data_dir}/estop-state.json` with `kill_all: true` and `updated_at` timestamp
- `get_estop_status() -> Result<FfiEstopStatus, FfiError>` — reads the `AtomicBool` and returns struct
- `resume_estop() -> Result<(), FfiError>` — clears the flag, writes `estop-state.json` with `kill_all: false`

**New UniFFI types:**
```rust
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiEstopStatus {
    pub engaged: bool,
    pub engaged_at_ms: Option<i64>,
}
```

**New FfiError variant:** `EstopEngaged { detail: String }` — returned by `send_message`, `session_send`, `send_message_streaming` when estop is active. Added to `error.rs` (lines 19-64).

**Guard integration in `lib.rs`:** Functions that execute agent work (`send_message` line 162, `session_send` line 947, `send_message_streaming` line 848) gain an estop check at entry:
```rust
if estop::is_engaged() {
    return Err(FfiError::EstopEngaged { detail: "Emergency stop is engaged".into() });
}
```

**State persistence:** `estop-state.json` in `data_dir` (same dir as config). On `start_daemon_inner()` (runtime.rs line 218), check for existing estop state file and restore `ESTOP_ENGAGED` flag if `kill_all: true`.

**REPL functions** (repl.rs, add after line 337):
- `estop()` -> "ok" (engage)
- `estop_status()` -> JSON
- `estop_resume()` -> "ok"

**Kotlin side:**

*New files:*
- `EstopRepository.kt` — calls FFI functions, exposes `estopEngaged: StateFlow<Boolean>` polled every 2 seconds while daemon is running
- Dashboard E-Stop button: red `FilledTonalButton` with `Icons.Filled.Warning`, always visible at top of `DashboardScreen.kt` (after service status indicator). Tapping shows confirmation dialog. When engaged, full-width `tertiaryContainer` banner replaces the button across all screens (similar pattern to `RestartRequiredBanner` but for estop state)

*Resume flow:*
- If device has lock screen (`KeyguardManager.isDeviceSecure`): launch `KeyguardManager.createConfirmDeviceCredentialIntent()` activity, resume on success
- If no lock screen: simple "Are you sure?" confirmation dialog
- No biometrics anywhere

**Files modified:**
| File | Change |
|------|--------|
| `zeroclaw-ffi/src/lib.rs` | Add 3 exported functions, estop guard in 3 messaging functions, `mod estop` declaration |
| `zeroclaw-ffi/src/error.rs` | Add `EstopEngaged` variant (line ~55) |
| `zeroclaw-ffi/src/estop.rs` | **New file** — estop logic, AtomicBool, JSON persistence |
| `zeroclaw-ffi/src/repl.rs` | Register 3 Rhai functions (after line 337) |
| `zeroclaw-ffi/src/runtime.rs` | Load estop state in `start_daemon_inner()` (line ~230) |
| `app/.../DashboardScreen.kt` | Add E-Stop button and engaged banner |
| `app/.../EstopRepository.kt` | **New file** — FFI wrapper with StateFlow |
| `app/.../ZeroClawApplication.kt` | Expose `estopRepository` |

---

### 9A-2: Resource Limits Settings UI

**Already implemented:**
- `GlobalTomlConfig` fields (lines 270-273): `securityResourcesMaxMemoryMb` (default 512), `securityResourcesMaxCpuTimeSecs` (default 60), `securityResourcesMaxSubprocesses` (default 10), `securityResourcesMemoryMonitoring` (default true)
- `ConfigTomlBuilder.appendSecurityResourcesSection()` (lines 1049-1067): emits `[security.resources]` with all 4 fields
- Upstream struct: `ResourceLimitsConfig` in `zeroclaw/src/config/schema.rs` (lines 3419-3464)

**What's missing: Settings UI + persistence**

*New file:* `ResourceLimitsScreen.kt` under `app/.../ui/screen/settings/security/`
- 3 number input fields: Max Memory (MB), Max CPU Time (seconds), Max Subprocesses
- 1 switch: Memory Monitoring enabled
- Integer inputs with `.coerceAtLeast(0)` validation
- Navigated from `SettingsScreen.kt` Security section — add new `SettingsListItem` after "Advanced Security" (line ~195)

*SettingsViewModel changes:*
- Add `updateResourceLimits(maxMemory, maxCpu, maxSubproc, monitoring)` method
- Calls `updateDaemonSetting{}` which marks restart required (resource limits can't be hot-swapped)

*SettingsRepository changes:*
- Add Room/DataStore fields for the 4 resource limit values
- Load into `GlobalTomlConfig` when building TOML

*Route changes:*
- `Route.kt`: add `ResourceLimitsRoute`
- `ZeroClawNavHost.kt`: add composable destination
- `SettingsNavAction.kt`: add navigation action

**Files modified:**
| File | Change |
|------|--------|
| `app/.../settings/security/ResourceLimitsScreen.kt` | **New file** |
| `app/.../settings/SettingsScreen.kt` | Add SettingsListItem in Security section (~line 195) |
| `app/.../settings/SettingsViewModel.kt` | Add update methods |
| `app/.../data/repository/SettingsRepository.kt` | Add 4 fields |
| `app/.../navigation/Route.kt` | Add route |
| `app/.../navigation/ZeroClawNavHost.kt` | Add destination |
| `app/.../navigation/SettingsNavAction.kt` | Add nav action |

---

### 9A-3: OTP Gating Settings UI

**Already implemented:**
- `GlobalTomlConfig` fields (lines 275-281): `securityOtpEnabled`, `securityOtpMethod`, `securityOtpTokenTtlSecs` (default 30), `securityOtpCacheValidSecs` (default 300), `securityOtpGatedActions` (default: shell, file_write, browser_open, browser, memory_forget), `securityOtpGatedDomains`, `securityOtpGatedDomainCategories`
- `ConfigTomlBuilder.appendSecurityOtpSection()` (lines 1090-1111): full emission
- Upstream: `OtpConfig` in `zeroclaw/src/config/schema.rs` (lines 3278-3341), `OtpValidator` in `zeroclaw/src/security/otp.rs`, `OtpMethod` enum (lines 3265-3276): `Totp`, `Pairing`, `CliPrompt`

**What's missing: Settings UI + persistence**

*New file:* `OtpGatingScreen.kt` under `app/.../ui/screen/settings/security/`
- Switch: OTP Enabled
- Dropdown: Method (only "totp" for now — Pairing and CliPrompt are future-reserved upstream)
- Number fields: Token TTL (seconds), Cache validity (seconds)
- Chip group: Gated Actions (multi-select from: shell, file_write, browser_open, browser, memory_forget, http_request, web_fetch, web_search)
- When OTP is enabled and device has PIN: note explaining that device credential will be required for gated actions
- When OTP is enabled and no device PIN: note explaining that a confirmation dialog will appear

*SettingsViewModel changes:*
- Add `updateOtpConfig(enabled, method, ttl, cacheValid, gatedActions)` method

*SettingsRepository changes:*
- Add Room/DataStore fields for the 7 OTP config values
- Load into `GlobalTomlConfig`

**Files modified:** Same pattern as 9A-2 (new screen, route, nav, settings VM, settings repo).

---

### 9A-4: Doctor Enhancements

**Existing Doctor system:**
- `DoctorValidator.kt` (lines 53-508): 5 categories — CONFIG, API_KEYS, CONNECTIVITY, DAEMON_HEALTH, SYSTEM
- `DiagnosticCategory` enum in `DiagnosticCheck.kt` (lines 29-44)
- `DoctorViewModel.kt` (lines 30-100): sequential `runAllChecks()` accumulating results
- `DoctorScreen.kt`: renders checks in collapsible sections
- Existing FFI: `doctor_channels(config_toml, data_dir)` in `lib.rs` line 202 — already exported but NOT called by DoctorValidator

**New category: CHANNELS**

Add `runChannelChecks()` to `DoctorValidator.kt`:
- Calls `doctorChannels(configToml, dataDir)` FFI function (already exists at lib.rs:202)
- Parses the JSON result into per-channel `DiagnosticCheck` entries
- Each channel gets PASS (connected), WARN (configured but not responding), or FAIL (error)
- actionRoute points to channel detail screen

**New category: RUNTIME_TRACES**

*New FFI function:* `query_runtime_traces(filter: Option<String>, event_type: Option<String>, limit: u32) -> Result<String, FfiError>`
- Reads upstream's JSONL trace file at `{workspace}/state/runtime-trace.jsonl` (upstream: `zeroclaw/src/observability/runtime_trace.rs`, `load_events()` at lines 233-293)
- Filters by `event_type` and/or content substring match
- Returns JSON array of `RuntimeTraceEvent` objects (upstream struct at lines 33-53: id, timestamp, event_type, channel, provider, model, turn_id, success, message, payload)

*New Rust FFI module:* `traces.rs`
- `query_traces_inner()` reads the JSONL file from `DaemonState.config.workspace_dir / state / runtime-trace.jsonl`
- Filters using upstream's same logic (event_type match, case-insensitive substring in message/payload)

*REPL function:* `traces(limit)` and `traces_filter(filter, limit)` registered in repl.rs

Add `runTraceChecks()` to `DoctorValidator.kt`:
- Calls `queryRuntimeTraces(filter="error", eventType=null, limit=5)` FFI
- If any error events found: WARN with count and most recent error message
- If no errors: PASS
- If trace file doesn't exist (mode=none): PASS with note "Runtime tracing disabled"

**DiagnosticCategory enum changes:**
```kotlin
enum class DiagnosticCategory {
    CONFIG, API_KEYS, CONNECTIVITY, DAEMON_HEALTH, CHANNELS, RUNTIME_TRACES, SYSTEM,
}
```

**DoctorViewModel.runAllChecks() additions** (after line 92, before systemChecks):
```kotlin
val channelChecks = validator.runChannelChecks(configToml, dataDir)
accumulated.addAll(channelChecks)
_checks.value = accumulated.toList()

val traceChecks = validator.runTraceChecks()
accumulated.addAll(traceChecks)
_checks.value = accumulated.toList()
```

**Files modified:**
| File | Change |
|------|--------|
| `zeroclaw-ffi/src/lib.rs` | Add `query_runtime_traces` export, `mod traces` |
| `zeroclaw-ffi/src/traces.rs` | **New file** — JSONL reader with filtering |
| `zeroclaw-ffi/src/repl.rs` | Register `traces()` and `traces_filter()` Rhai functions |
| `app/.../model/DiagnosticCheck.kt` | Add `CHANNELS`, `RUNTIME_TRACES` to enum (lines 29-44) |
| `app/.../service/DoctorValidator.kt` | Add `runChannelChecks()`, `runTraceChecks()` methods |
| `app/.../settings/doctor/DoctorViewModel.kt` | Call both new methods in `runAllChecks()` |
| `app/.../settings/doctor/DoctorScreen.kt` | Render new sections (no structural changes — existing category-based rendering handles it) |

---

## Phase 9B: Web & Config Access

### 9B-1: Web Fetch Settings UI

**Already implemented:** `GlobalTomlConfig` fields (lines 257-261), `ConfigTomlBuilder.appendWebFetchSection()` (lines 968-987). Upstream: `WebFetchConfig` at `zeroclaw/src/config/schema.rs` lines 1057-1093 — `enabled: bool`, `allowed_domains: Vec<String>`, `blocked_domains: Vec<String>`, `max_response_size: usize` (default 500000), `timeout_secs: u64` (default 30).

**What's missing: Settings UI + persistence**

*New file:* `WebAccessScreen.kt` — combined screen for web_fetch, web_search, and http_request (3 collapsible sections in one screen, since they're related)

*Web Fetch section:*
- Switch: Enabled
- Chip input: Allowed Domains (default: `["*"]` = all public hosts)
- Chip input: Blocked Domains
- Number: Max Response Size (bytes, default 500000)
- Number: Timeout (seconds, default 30)

*SettingsScreen.kt:* Add "Web Access" item in Network section (after Tunnel, ~line 225)

**Files modified:** New screen + route + nav + settings VM updates + settings repo persistence.

---

### 9B-2: Web Search Settings UI

**Already implemented:** `GlobalTomlConfig` fields (lines 262-266), `ConfigTomlBuilder.appendWebSearchSection()` (lines 997-1012). Upstream: `WebSearchConfig` at `zeroclaw/src/config/schema.rs` lines 1099-1140 — `enabled: bool`, `provider: String` (default "duckduckgo"), `brave_api_key: Option<String>`, `max_results: usize` (default 5), `timeout_secs: u64` (default 15).

**In the same `WebAccessScreen.kt`:**

*Web Search section:*
- Switch: Enabled
- Dropdown: Provider (duckduckgo, brave)
- Text field: Brave API Key (shown only when provider=brave)
- Number: Max Results (default 5)
- Number: Timeout (seconds, default 15)

---

### 9B-3: HTTP Request Settings UI

**Already implemented:** `GlobalTomlConfig` fields (lines 240-241), `ConfigTomlBuilder.appendHttpRequestSection()` (lines 874-883). Upstream: `HttpRequestConfig` at `zeroclaw/src/config/schema.rs` lines 1015-1047 — `enabled: bool`, `allowed_domains: Vec<String>` (default empty = deny all), `max_response_size: usize` (default 1000000), `timeout_secs: u64` (default 30). **Deny-by-default:** empty allowed_domains blocks all requests.

**In the same `WebAccessScreen.kt`:**

*HTTP Request section:*
- Switch: Enabled
- Chip input: Allowed Domains (required when enabled — deny-by-default)
- Note: "HTTP requests are blocked unless domains are explicitly allowed"

**Note:** `GlobalTomlConfig` currently only has `httpRequestEnabled` and `httpRequestAllowedDomains`. Add `httpRequestMaxResponseSize` (default 1000000) and `httpRequestTimeoutSecs` (default 30) fields, and update `appendHttpRequestSection()` to emit them.

---

### 9B-4: Multimodal Settings UI

**Already implemented:** `GlobalTomlConfig` fields (lines 247-249), `ConfigTomlBuilder.appendMultimodalSection()` (lines 912-923). Upstream: `MultimodalConfig` at `zeroclaw/src/config/schema.rs` lines 487-525 — `max_images: usize` (default 4, clamped 1-16), `max_image_size_mb: usize` (default 5, clamped 1-20), `allow_remote_fetch: bool` (default false).

**Settings UI:** Add a "Vision" card in Security section of SettingsScreen (or Advanced Configuration). Small inline form:
- Slider: Max Images (1-16, default 4)
- Slider: Max Image Size MB (1-20, default 5)
- Switch: Allow Remote Fetch (default off)

This is small enough to be an expandable card directly in an existing settings sub-screen (e.g., SecurityAdvancedScreen) rather than a new route.

---

### 9B-5: Config Read API

**New FFI function:** `get_running_config() -> Result<String, FfiError>`
- Returns the TOML string stored in `DaemonState.config` (runtime.rs line 37)
- Implementation: `with_daemon_config(|c| toml::to_string(c))` using the existing helper (runtime.rs line 99)

**REPL function:** `config()` -> TOML string

**Kotlin usage:** `DaemonServiceBridge` can call this to verify the running config matches what it expects. No UI needed — this is plumbing. Doctor can use it to add a "Config consistency" check (running config matches saved settings).

**Files modified:**
| File | Change |
|------|--------|
| `zeroclaw-ffi/src/lib.rs` | Add `get_running_config` export |
| `zeroclaw-ffi/src/runtime.rs` | Add `get_running_config_inner()` using `with_daemon_config` |
| `zeroclaw-ffi/src/repl.rs` | Register `config()` Rhai function |
| `app/.../ui/screen/settings/web/WebAccessScreen.kt` | **New file** — combined web_fetch/web_search/http_request settings |
| `app/.../settings/SettingsScreen.kt` | Add "Web Access" and "Vision" items |
| `app/.../settings/SettingsViewModel.kt` | Add update methods for all web access fields |
| `app/.../data/repository/SettingsRepository.kt` | Add persistence for web access fields |
| `app/.../service/ConfigTomlBuilder.kt` | Add missing `httpRequestMaxResponseSize`, `httpRequestTimeoutSecs` fields to emission (lines 874-883) |
| `app/.../navigation/Route.kt` | Add `WebAccessRoute` |
| `app/.../navigation/ZeroClawNavHost.kt` | Add destination |

---

## Phase 9C: Diagnostics & Auth

### 9C-1: Auth Profile Management

**Upstream auth system:** `zeroclaw/src/auth/profiles.rs`
- `AuthProfile` struct: `id`, `provider`, `profile_name`, `kind` (OAuth/Token), `account_id`, `workspace_id`, `token_set` (access_token, refresh_token, expires_at, token_type, scope), `token`, `metadata`, `created_at`, `updated_at`
- `AuthProfilesData`: `schema_version: u32`, `updated_at`, `active_profiles: BTreeMap<String, String>` (provider -> profile_id), `profiles: BTreeMap<String, AuthProfile>`
- File: `{state_dir}/auth-profiles.json`, schema version 1
- Encrypted at rest via `SecretStore`
- Profile ID format: `"{provider}:{profile_name}"` (e.g. `"openai-codex:default"`)
- `TokenSet.is_expiring_within(skew)` checks expiry

**New FFI functions (3):**

`list_auth_profiles() -> Result<Vec<FfiAuthProfile>, FfiError>`
- Reads `auth-profiles.json` from `DaemonState.config.workspace_dir`
- Parses `AuthProfilesData`, returns flattened list

`refresh_auth_token(provider: String, profile_name: String) -> Result<i64, FfiError>`
- Loads profile, uses refresh_token to get new access_token
- Calls provider's token endpoint (OpenAI: `auth.openai.com/oauth/token`)
- Returns new `expires_at` as epoch_ms

`remove_auth_profile(provider: String, profile_name: String) -> Result<(), FfiError>`
- Removes from `profiles` map and `active_profiles` if it was active
- Saves atomically

**New UniFFI types:**
```rust
#[derive(Debug, Clone, uniffi::Record)]
pub struct FfiAuthProfile {
    pub id: String,
    pub provider: String,
    pub profile_name: String,
    pub kind: String,  // "oauth" or "token"
    pub is_active: bool,
    pub expires_at_ms: Option<i64>,
    pub created_at_ms: i64,
    pub updated_at_ms: i64,
}
```

**New Rust FFI module:** `auth_profiles.rs`

**REPL functions:** `auth_list()`, `auth_refresh(provider, profile)`, `auth_remove(provider, profile)`

**Kotlin side:**

*New file:* `AuthProfilesScreen.kt` under `app/.../ui/screen/settings/apikeys/`
- LazyColumn of profile cards showing: provider icon, profile name, kind (OAuth/Token badge), status (active/expired/inactive), expiry countdown
- Per-card actions: Refresh (OAuth only), Delete, Set Active
- Navigate from Settings > API Keys section — add "Auth Profiles" item after existing "API Keys" entry (~line 200)

*New file:* `AuthProfilesViewModel.kt`
- `profiles: StateFlow<List<FfiAuthProfile>>` — loaded via FFI
- `refreshProfile(provider, name)`, `removeProfile(provider, name)`, `setActiveProfile(provider, name)` methods
- Snackbar feedback on success/failure

**Existing Doctor integration:**
- `DoctorValidator.runApiKeyChecks()` gains a sub-check: for each OAuth profile with `expires_at_ms` in the past, emit a WARN check with "Refresh" action button pointing to `AuthProfilesRoute`

**Files modified:**
| File | Change |
|------|--------|
| `zeroclaw-ffi/src/lib.rs` | Add 3 exported functions, `mod auth_profiles` |
| `zeroclaw-ffi/src/auth_profiles.rs` | **New file** — profile CRUD via auth-profiles.json |
| `zeroclaw-ffi/src/repl.rs` | Register 3 Rhai functions |
| `app/.../settings/apikeys/AuthProfilesScreen.kt` | **New file** |
| `app/.../settings/apikeys/AuthProfilesViewModel.kt` | **New file** |
| `app/.../settings/SettingsScreen.kt` | Add "Auth Profiles" item in Security section |
| `app/.../service/DoctorValidator.kt` | Add expired token sub-check in `runApiKeyChecks()` |
| `app/.../navigation/Route.kt` | Add `AuthProfilesRoute` |
| `app/.../navigation/ZeroClawNavHost.kt` | Add destination |

---

### 9C-2: Model Catalog Refresh

**Upstream finding:** There is NO `models_cache.json` or unified model catalog in upstream. Each provider has hardcoded model lists. Model discovery is provider-specific at runtime.

**Revised approach:** Since upstream doesn't have a cache API, we build a lightweight Android-side model catalog:

**New FFI function:** `discover_models(provider: String, api_key: String, base_url: Option<String>) -> Result<String, FfiError>`
- For OpenAI-compatible providers: `GET /v1/models` with the given API key
- For Anthropic: hardcoded known model list (upstream pattern)
- Returns JSON array of `{ id, name, context_window? }`
- Timeout: 10 seconds

**New Rust FFI module:** `models.rs`
- `discover_models_inner()` — builds reqwest client, calls provider's model listing endpoint
- Provider routing: OpenAI/OpenRouter/Compatible → `GET /v1/models`, Anthropic → static list, Ollama → `GET /api/tags`

**REPL function:** `models(provider, api_key)` -> JSON array

**Kotlin side:**

*No new screen.* Instead, enhance the existing agent detail model picker:
- `AgentDetailScreen.kt` model dropdown gains a refresh icon button
- Tapping it calls `discoverModels(provider, apiKey, baseUrl)` FFI
- Results cached in a `ModelCacheRepository` (in-memory `Map<String, List<ModelInfo>>` with 12-hour TTL)
- Dropdown populates from cache, falling back to a hardcoded default list if discovery fails

**Files modified:**
| File | Change |
|------|--------|
| `zeroclaw-ffi/src/lib.rs` | Add `discover_models` export, `mod models` |
| `zeroclaw-ffi/src/models.rs` | **New file** — provider model listing |
| `zeroclaw-ffi/src/repl.rs` | Register `models()` Rhai function |
| `app/.../agents/AgentDetailScreen.kt` | Add refresh icon to model dropdown |
| `app/.../agents/AgentDetailViewModel.kt` | Add `refreshModels()` method |
| `app/.../data/repository/ModelCacheRepository.kt` | **New file** — in-memory cache with TTL |

---

## Phase 9D: Scheduling & Media

### 9D-1: Cron `add-at` / `add-every`

**Upstream:**
- `CronCommands::AddAt { at: String, command: String }` in `zeroclaw/src/lib.rs` lines 212-222 — one-shot at RFC3339 timestamp
- `CronCommands::AddEvery { every_ms: u64, command: String }` in `zeroclaw/src/lib.rs` lines 224-232 — fixed-interval repeating
- `Schedule` enum in `zeroclaw/src/cron/types.rs` lines 61-75: `Cron { expr, tz }`, `At { at: DateTime<Utc> }`, `Every { every_ms: u64 }`
- Gateway API: `POST /api/cron` with `schedule` field in body

**New FFI functions (2):**

`add_cron_job_at(timestamp_rfc3339: String, command: String) -> Result<FfiCronJob, FfiError>`
- Posts to gateway with `schedule: { kind: "at", at: "<timestamp>" }`
- Returns `FfiCronJob` with `one_shot: true`

`add_cron_job_every(interval_ms: u64, command: String) -> Result<FfiCronJob, FfiError>`
- Posts to gateway with `schedule: { kind: "every", every_ms: <value> }`
- Returns `FfiCronJob` with `one_shot: false`

**REPL functions:** `cron_add_at(timestamp, command)`, `cron_add_every(ms, command)` registered in repl.rs

**Kotlin side:**

*Modified file:* `CronJobsScreen.kt` — the existing Add Job dialog expands:
- Current: only "Cron expression" and "One-shot delay" modes
- New: Add "At specific time" (date/time picker → RFC3339) and "Fixed interval" (number input → milliseconds, with helper showing "every X minutes/hours") modes
- Type selector: `SegmentedButton` or dropdown with 4 options

*Modified file:* `CronJobsViewModel.kt` — add `addJobAt(timestamp, command)` and `addJobEvery(intervalMs, command)` methods alongside existing `addJob()` and `addOneShot()`

**Files modified:**
| File | Change |
|------|--------|
| `zeroclaw-ffi/src/lib.rs` | Add 2 exported functions |
| `zeroclaw-ffi/src/cron.rs` | Add `add_cron_job_at_inner()`, `add_cron_job_every_inner()` |
| `zeroclaw-ffi/src/repl.rs` | Register 2 Rhai functions |
| `app/.../settings/cron/CronJobsScreen.kt` | Expand Add dialog with 2 new modes |
| `app/.../settings/cron/CronJobsViewModel.kt` | Add 2 new methods |

---

### 9D-2: Transcription Config Settings UI

**Already implemented:** `GlobalTomlConfig` fields (lines 242-246), `ConfigTomlBuilder.appendTranscriptionSection()` (lines 892-903). Upstream: `TranscriptionConfig` at `zeroclaw/src/config/schema.rs` lines 363-393 — `enabled: bool` (default false), `api_url: String` (default Groq Whisper endpoint), `model: String` (default "whisper-large-v3-turbo"), `language: Option<String>`, `max_duration_secs: u64` (default 120).

**What's missing: Settings UI + persistence**

*New file:* `TranscriptionScreen.kt` under `app/.../ui/screen/settings/`
- Switch: Enabled
- Text field: API URL (default: `https://api.groq.com/openai/v1/audio/transcriptions`)
- Dropdown: Model (whisper-large-v3-turbo, whisper-large-v3)
- Text field: Language hint (ISO-639-1, optional)
- Slider: Max Duration (30-300 seconds, default 120)

*SettingsScreen.kt:* Add "Voice Input" item in Advanced Configuration section (~line 265)

**Files modified:** New screen + route + nav + settings VM updates + settings repo persistence.

---

### 9D-3: Provider Hot-Swap + Restart Banner Cleanup

**New FFI function:** `swap_provider(provider: String, model: String, api_key: Option<String>) -> Result<(), FfiError>`
- Mutates `DaemonState.config` in-place (runtime.rs, via `lock_daemon()`)
- Updates `config.default_provider`, `config.default_model`, optionally `config.api_key`
- Invalidates any cached provider state in the daemon
- Does NOT require daemon restart

**REPL function:** `swap_provider(provider, model)` registered in repl.rs

**Kotlin side — DaemonServiceBridge changes:**

New method: `swapProvider(provider: String, model: String, apiKey: String?): Result<Unit>`
- Calls `swapProvider()` FFI function
- On success: clears `_restartRequired` if it was set
- On failure: falls back to setting `_restartRequired = true`

**Restart banner cleanup — what changes:**

The `RestartRequiredBanner` component is NOT deleted. Provider/model changes stop triggering it, but other config changes (gateway port, resource limits, channel config, security settings) still require restart. The banner becomes more targeted.

**Changes:**

| File | Line(s) | Change |
|------|---------|--------|
| `DaemonServiceBridge.kt` | 588-592 | `markRestartRequired()` stays but is no longer called for provider/model changes |
| `DaemonServiceBridge.kt` | new | Add `swapProvider(provider, model, apiKey)` method that calls FFI + clears restartRequired on success |
| `AgentDetailViewModel.kt` | save handler | On save: if only provider/model changed, call `swapProvider()` instead of `markRestartRequired()`. If other fields changed (channels, security), still mark restart required. |
| `SettingsViewModel.kt` | 52-56 | `updateDaemonSetting()` still marks restart required for non-hot-swappable settings |
| `SettingsViewModel.kt` | new | Add `updateProvider(provider, model, apiKey)` that calls bridge.swapProvider() |
| `RestartRequiredBanner.kt` | 63-64 | Change text to "Restart daemon to apply configuration changes" (more specific) |

**What does NOT change:**
- `RestartRequiredBanner.kt` — kept (still needed for non-hot-swappable changes)
- `_restartRequired` StateFlow in DaemonServiceBridge — kept
- `restartRequired` parameter threading in ZeroClawNavHost/SettingsScreen/AgentDetailScreen — kept
- Test assertions in SettingsScreenTest — kept (but may need updating for new text)

**What to remove:**
- Nothing is deleted. The behavioral change is: `AgentDetailViewModel` no longer calls `markRestartRequired()` for provider/model-only changes. Instead it calls `swapProvider()`.

**Files modified:**
| File | Change |
|------|--------|
| `zeroclaw-ffi/src/lib.rs` | Add `swap_provider` export |
| `zeroclaw-ffi/src/runtime.rs` | Add `swap_provider_inner()` — mutate DaemonState.config |
| `zeroclaw-ffi/src/repl.rs` | Register `swap_provider()` Rhai function |
| `app/.../service/DaemonServiceBridge.kt` | Add `swapProvider()` method |
| `app/.../agents/AgentDetailViewModel.kt` | Call swapProvider on provider/model save |
| `app/.../settings/SettingsViewModel.kt` | Add `updateProvider()` method |
| `app/.../ui/component/RestartRequiredBanner.kt` | Update banner text |
| `app/...androidTest/.../SettingsScreenTest.kt` | Update text assertion if changed |

---

## Summary: New Files Created

| Phase | New Rust Files | New Kotlin Files |
|-------|---------------|-----------------|
| 9A | `estop.rs`, `traces.rs` | `EstopRepository.kt`, `ResourceLimitsScreen.kt`, `OtpGatingScreen.kt` |
| 9B | (none) | `WebAccessScreen.kt` |
| 9C | `auth_profiles.rs`, `models.rs` | `AuthProfilesScreen.kt`, `AuthProfilesViewModel.kt`, `ModelCacheRepository.kt` |
| 9D | (none) | `TranscriptionScreen.kt` |

## Summary: New FFI Functions

| Phase | Function | Returns |
|-------|----------|---------|
| 9A | `engage_estop()` | `()` |
| 9A | `get_estop_status()` | `FfiEstopStatus` |
| 9A | `resume_estop()` | `()` |
| 9A | `query_runtime_traces(filter, event_type, limit)` | JSON string |
| 9B | `get_running_config()` | TOML string |
| 9C | `list_auth_profiles()` | `Vec<FfiAuthProfile>` |
| 9C | `refresh_auth_token(provider, profile)` | `i64` (epoch_ms) |
| 9C | `remove_auth_profile(provider, profile)` | `()` |
| 9C | `discover_models(provider, api_key, base_url)` | JSON string |
| 9D | `add_cron_job_at(timestamp, command)` | `FfiCronJob` |
| 9D | `add_cron_job_every(interval_ms, command)` | `FfiCronJob` |
| 9D | `swap_provider(provider, model, api_key)` | `()` |

**Total: 12 new FFI functions** (47 existing -> 59 total)

## Summary: New REPL Functions

`estop()`, `estop_status()`, `estop_resume()`, `traces(limit)`, `traces_filter(filter, limit)`, `config()`, `auth_list()`, `auth_refresh(provider, profile)`, `auth_remove(provider, profile)`, `models(provider, api_key)`, `cron_add_at(timestamp, command)`, `cron_add_every(ms, command)`, `swap_provider(provider, model)`

**Total: 13 new REPL functions** (31 existing -> 44 total)

## Summary: Removed/Zombie Code

| Item | Action | Reason |
|------|--------|--------|
| `markRestartRequired()` calls in AgentDetailViewModel for provider/model changes | **Remove calls** (method stays) | Replaced by `swapProvider()` |
| Banner text "Restart daemon to apply changes" | **Update** to "Restart daemon to apply configuration changes" | More specific now that not all changes require restart |

No files are deleted. No functions are removed. The restart banner system stays but triggers less often.

## Dependencies Between Phases

- **9A is independent** — can be built first
- **9B depends on nothing** — can be built in parallel with 9A
- **9C depends on nothing** — can be built in parallel
- **9D-3 (hot-swap)** should be last since it changes the restart banner behavior that 9A/9B settings screens rely on for non-hot-swappable changes
- Recommended order: 9A -> 9B -> 9C -> 9D

## Testing Strategy

Each phase includes:
1. **Rust unit tests** in `zeroclaw-ffi/tests/` — test new FFI functions with mock config
2. **Kotlin unit tests** — ViewModel tests with MockK
3. **Integration** — DoctorValidator tests for new categories
4. **Compose UI tests** — new screens with `composeTestRule`

Existing test count: 170 Rust tests. Expected addition: ~30 new tests across all phases.
