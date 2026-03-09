# i18n Localization TODO (Incremental)

**Date:** 2026-03-05  
**Owner:** Codex + User  
**Goal:** Migrate Android UI text to resource-based localization with `values/strings.xml` + `values-zh-rCN/strings.xml`.

## Baseline

- Hardcoded Compose text count (`Text("...")`): **267**
- Current remaining hardcoded Compose text after Batch 5: **131**
- Current remaining hardcoded Compose text after Batch 6: **1** (internal non-UI probe literal)
- Stage 2 baseline (`text=` / `title=` / `subtitle=` / `contentDescription=`): **66 / 21 / 0 / 48**
- Stage 2 current after completion: **0 / 0 / 0 / 0**
- Existing string resources: notifications + app name only
- Existing localization directories: `values`, empty `values-zh-rCN`

## Work Plan

1. `done` Add a tracking TODO and define phased migration scope.
2. `done` Batch 1: Migrate `ServiceConfigScreen.kt` hardcoded text to `stringResource`.
3. `done` Batch 2: Migrate `OfficialPluginConfigSection.kt` hardcoded text to `stringResource`.
4. `done` Batch 3: Migrate `ApiKeysScreen.kt` hardcoded text to `stringResource`.
5. `done` Batch 4: Migrate `AddCronJobDialog.kt` + `SecurityAdvancedScreen.kt`.
6. `done` Batch 5: Migrate onboarding 9-step flow and dependent setup components.
7. `done` Format localization: replace fixed `Locale.US` display paths where appropriate.
8. `done` Add CI guardrail to prevent new hardcoded `Text("...")` in UI.
9. `done` Stage 2 scan: include non-`Text("...")` UI literals (e.g. `text = "..."`, semantics labels, dialog body strings).
10. `in_progress` Stage 3 scan: migrate user-facing ViewModel/runtime message literals to resource-backed i18n.

## Batch Tracking

### Batch 1 - ServiceConfigScreen

- Status: `done`
- Files:
  - `app/src/main/java/com/zeroclaw/android/ui/screen/settings/ServiceConfigScreen.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-zh-rCN/strings.xml`
- Scope:
  - Section titles, labels, button/toggle text, helper text, error text.
  - Dynamic strings with placeholders for bounds/values.
- Verification:
  - Screen builds with `R.string.*` usage and no hardcoded English UI labels.
  - `ServiceConfigScreen.kt` hardcoded `Text("...")` count: `31 -> 0`.
  - UI-wide hardcoded `Text("...")` count: `267 -> 236`.

### Batch 2 - OfficialPluginConfigSection

- Status: `done`
- Files:
  - `app/src/main/java/com/zeroclaw/android/ui/screen/plugins/OfficialPluginConfigSection.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-zh-rCN/strings.xml`
- Verification:
  - `OfficialPluginConfigSection.kt` hardcoded `Text("...")` count: `27 -> 0`.
  - UI-wide hardcoded `Text("...")` count: `236 -> 209`.

### Batch 3 - ApiKeysScreen

- Status: `done`
- Files:
  - `app/src/main/java/com/zeroclaw/android/ui/screen/settings/apikeys/ApiKeysScreen.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-zh-rCN/strings.xml`
- Verification:
  - `ApiKeysScreen.kt` hardcoded `Text("...")` count: `19 -> 0`.
  - UI-wide hardcoded `Text("...")` count: `209 -> 190`.

### Batch 4 - AddCronJobDialog + SecurityAdvancedScreen

- Status: `done`
- Files:
  - `app/src/main/java/com/zeroclaw/android/ui/screen/settings/cron/AddCronJobDialog.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/screen/settings/SecurityAdvancedScreen.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-zh-rCN/strings.xml`
- Verification:
  - `AddCronJobDialog.kt` hardcoded `Text("...")` count: `18 -> 0`.
  - `SecurityAdvancedScreen.kt` hardcoded `Text("...")` count: `16 -> 0`.
  - UI-wide hardcoded `Text("...")` count: `190 -> 156`.

### Batch 5 - Onboarding 9-step + Setup Flow Components

- Status: `done`
- Files:
  - `app/src/main/java/com/zeroclaw/android/ui/screen/onboarding/OnboardingScreen.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/screen/onboarding/steps/WelcomeStep.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/screen/onboarding/steps/ProviderStep.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/screen/onboarding/steps/ActivationStep.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/setup/AutonomyPicker.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/setup/ChannelSelectionGrid.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/setup/ChannelSetupFlow.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/setup/ProviderSetupFlow.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/setup/TunnelConfigFlow.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/setup/MemoryConfigFlow.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/setup/IdentityConfigFlow.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/setup/ValidationIndicator.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/setup/ConfigSummaryCard.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/setup/DeepLinkButton.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/setup/InstructionsList.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/ProviderCredentialForm.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/ProviderDropdown.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/ModelSuggestionField.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/NetworkScanSheet.kt`
  - `app/src/main/java/com/zeroclaw/android/ui/component/SecretTextField.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-zh-rCN/strings.xml`
- Verification:
  - Onboarding + setup flow hardcoded `Text("...")` in active path reduced to near-zero (remaining entries are legacy/preview-only).
  - UI-wide hardcoded `Text("...")` count: `156 -> 131`.

## Progress Notes

- 2026-03-05: TODO created. Batch 1 started.
- 2026-03-05: Batch 1 completed (`ServiceConfigScreen.kt` + `values/strings.xml` + `values-zh-rCN/strings.xml`).
- 2026-03-05: Batch 2 completed (`OfficialPluginConfigSection.kt` + bilingual resource entries).
- 2026-03-05: Batch 3 completed (`ApiKeysScreen.kt` + bilingual resource entries).
- 2026-03-05: Batch 4 completed (`AddCronJobDialog.kt` + `SecurityAdvancedScreen.kt` + bilingual resource entries).
- 2026-03-06: Batch 5 completed (onboarding 9-step flow + dependent setup/components + bilingual resource entries).
- 2026-03-07: Re-scan complete. Remaining hardcoded Compose text (`Text("...")`) in `app/src/main/java`: **132**.
- 2026-03-07: Batch 6 completed. Remaining hardcoded Compose text (`Text("...")`) in `app/src/main/java`: **1** (`DaemonServiceBridge.kt` internal probe `"ok"`; non-UI).
- 2026-03-07: Added 100+ bilingual resource keys to `values/strings.xml` and `values-zh-rCN/strings.xml` for migrated screens/components.
- 2026-03-07: Build verification passed: `./gradlew :app:compileDebugKotlin`.
- 2026-03-07: Stage 2 baseline scan (`text=`/`title=`/`subtitle=`/`contentDescription=`) before this wave: `66 / 21 / 0 / 48`.
- 2026-03-07: Stage 2 wave completed for:
  - `DashboardScreen.kt`
  - `CostSummaryCard.kt`
  - `CronSummaryCard.kt`
  - `ActivityFeedSection.kt`
  - `QrScannerScreen.kt`
  - `TerminalScreen.kt`
  - `values/strings.xml` + `values-zh-rCN/strings.xml` additions
- 2026-03-07: Stage 2 scan after this wave: `49 / 19 / 0 / 36`.
- 2026-03-07: Build verification passed after this wave: `./gradlew :app:compileDebugKotlin`.
- 2026-03-07: Stage 2 second wave completed for:
  - `TunnelScreen.kt`
  - `SetupScreen.kt`
  - `ApiKeyDetailScreen.kt`
  - `values/strings.xml` + `values-zh-rCN/strings.xml` additions
- 2026-03-07: Stage 2 scan after second wave: `41 / 14 / 0 / 32`.
- 2026-03-07: Build verification passed after second wave: `./gradlew :app:compileDebugKotlin`.
- 2026-03-07: Stage 2 third wave completed for:
  - `PluginDetailScreen.kt`
  - `MemoryBrowserScreen.kt`
  - `CronJobsScreen.kt`
  - `AuthProfilesScreen.kt`
  - `values/strings.xml` + `values-zh-rCN/strings.xml` additions
- 2026-03-07: Stage 2 scan after third wave: `29 / 12 / 0 / 25`.
- 2026-03-07: Build verification passed after third wave: `./gradlew :app:compileDebugKotlin`.
- 2026-03-07: Stage 2 fourth wave completed for:
  - `ThinkingCard.kt`
  - `DoctorScreen.kt`
  - `SkillsTab.kt`
  - `PluginsScreen.kt`
  - `values/strings.xml` + `values-zh-rCN/strings.xml` additions
- 2026-03-07: Stage 2 scan after fourth wave: `21 / 10 / 0 / 19`.
- 2026-03-07: Build verification passed after fourth wave: `./gradlew :app:compileDebugKotlin`.
- 2026-03-07: Stage 2 fifth wave completed for:
  - `RestartRequiredBanner.kt`
  - `ConfirmDeleteDialog.kt`
  - `LoadingIndicator.kt`
  - `ModelSuggestionField.kt`
  - `SettingsToggleRow.kt`
  - `PinKeypad.kt`
  - `ProviderIcon.kt`
  - `OfficialPluginBadge.kt`
  - `PluginSectionHeader.kt`
  - `StatusDot.kt`
  - `MaskedText.kt`
  - `SetupBottomSheet.kt`
  - `AgentDetailScreen.kt`
  - `ToolsTab.kt`
  - `NetworkScanSheet.kt`
  - `LockGateScreen.kt`
  - `PinEntrySheet.kt`
  - `ConnectedChannelsScreen.kt`
  - `ChannelDetailScreen.kt`
  - `TerminalScreen.kt`
  - `TerminalOutputRenderer.kt`
  - `BatterySettingsScreen.kt`
  - `LogViewerScreen.kt`
  - `values/strings.xml` + `values-zh-rCN/strings.xml` additions
- 2026-03-07: Stage 2 sixth wave completed for preview/demo text cleanup:
  - `ChannelSetupFlow.kt`
  - `InstructionsList.kt`
  - `values/strings.xml` + `values-zh-rCN/strings.xml` additions
- 2026-03-07: Stage 2 final scan result (`text=`/`title=`/`subtitle=`/`contentDescription=`): `0 / 0 / 0 / 0`.
- 2026-03-07: Build verification passed after Stage 2 completion: `./gradlew :app:compileDebugKotlin`.
- 2026-03-07: Stage 3 first wave completed for ViewModel runtime/snackbar/system messages:
  - `ChannelsViewModel.kt`
  - `CronJobsViewModel.kt`
  - `SkillsViewModel.kt`
  - `PluginsViewModel.kt`
  - `ApiKeysViewModel.kt`
  - `TerminalViewModel.kt`
  - `AuthProfilesViewModel.kt`
  - `MemoryBrowserViewModel.kt`
  - `values/strings.xml` + `values-zh-rCN/strings.xml` additions
- 2026-03-07: Stage 3 first-wave verification:
  - Pattern scan (`runMutation("...")`, snackbar assignments, save/error literal constructors) in `ui/screen/*ViewModel.kt`: **0 remaining matches**
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
- 2026-03-07: Stage 3 second wave completed for onboarding runtime defaults/errors:
  - `OnboardingViewModel.kt`
  - `values/strings.xml` + `values-zh-rCN/strings.xml` additions
- 2026-03-07: Build verification passed after Stage 3 second wave: `./gradlew :app:compileDebugKotlin`
- 2026-03-07: Stage 3 third wave completed for memory category localization:
  - `MemoryBrowserScreen.kt`
  - `values/strings.xml` + `values-zh-rCN/strings.xml` additions
- 2026-03-07: Build verification passed after Stage 3 third wave: `./gradlew :app:compileDebugKotlin`
- 2026-03-07: Stage 3 fourth wave completed for terminal command metadata and error-classification cleanup:
  - `CommandRegistry.kt` (`description`/`usage` switched to resource ids)
  - `TerminalScreen.kt` (autocomplete description from localized resources)
  - `TerminalViewModel.kt` (help text reads localized command metadata)
  - `ApiKeysViewModel.kt` (`mapConnectionError` refactored to non-UI `ConnectionErrorKind`)
  - `ConnectionErrorMessageTest.kt` updated for classification assertions
  - `values/strings.xml` + `values-zh-rCN/strings.xml` additions
- 2026-03-07: Stage 3 fourth-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
  - Targeted unit tests passed: `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.ui.screen.settings.apikeys.ConnectionErrorMessageTest`
- 2026-03-07: Stage 3 fifth wave completed for validation/runtime i18n infrastructure:
  - `ValidationResult.kt` now supports resource-backed payloads (`*ResId` + `*Args`) while preserving string compatibility.
  - `ValidationIndicator.kt` now resolves resource-backed validation texts in UI.
  - `ChannelValidator.kt` + `ProviderValidator.kt` migrated from hardcoded literals to `R.string`-backed results.
  - `OnboardingCoordinator.kt` OAuth/runtime validation messages migrated to `R.string` resources.
  - `DaemonNotificationManager.kt` error-prefix notification text migrated to `R.string`.
  - `DoctorValidator.kt` diagnostic title/detail/action strings migrated to `R.string`.
  - `SetupOrchestrator.kt` setup pipeline failure/timeout user-facing errors migrated to `R.string`.
  - `SetupViewModel.kt` + `ApiKeysViewModel.kt` updated for new `SetupOrchestrator` constructor context.
  - `ChannelSetupSpec.kt` step titles/instructions migrated to resource-backed fields (`titleResId` / `contentResId`).
  - `ChannelSetupFlow.kt` + `InstructionsList.kt` updated to resolve resource-backed step/instruction content.
  - `values/strings.xml` + `values-zh-rCN/strings.xml` appended with 100+ new i18n keys.
- 2026-03-07: Stage 3 fifth-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
  - Targeted tests passed:
    - `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.data.channel.ChannelSetupSpecTest`
    - `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.data.validation.ChannelValidatorTest`
    - `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.data.validation.ProviderValidatorTest`
- 2026-03-07: Stage 3 sixth wave completed for channel metadata i18n:
  - Added full `ChannelType` localized name mapping in `ui/i18n/ChannelTypeDisplayName.kt` (all entries, no fallback-only languages).
  - Added new `ui/i18n/ChannelFieldLabel.kt` for TOML field-key to localized label mapping.
  - Updated channel field rendering call-sites to use localized labels:
    - `ChannelSetupFlow.kt`
    - `ChannelDetailScreen.kt`
    - `ChannelSetupStep.kt`
  - Added extensive EN/zh-CN resources for:
    - channel type names (`channel_type_name_*`)
    - channel field labels (`channel_field_label_*`)
- 2026-03-07: Stage 3 sixth-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
  - Targeted tests passed:
    - `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.data.channel.ChannelSetupSpecTest`
- 2026-03-07: Stage 3 seventh wave completed for provider metadata/runtime feed/tile labels:
  - Added provider metadata i18n mapping:
    - `ui/i18n/ProviderMetadataText.kt` (`helpText` + `keyPrefixHint` by provider ID)
  - Updated provider credential UI/runtime usage:
    - `ProviderCredentialForm.kt` now renders localized provider help text and localized key-prefix warning
    - `ProviderKeyValidator.kt` added `hasKeyFormatWarning(...)` boolean API for locale-safe UI rendering
    - `ApiKeyDetailScreen.kt` now uses localized screen titles (`Add/Edit API Key`) and boolean prefix check
  - Migrated daemon runtime event feed messages to resources:
    - `EventBridge.kt` now uses localized `R.string` templates for activity feed messages
    - `ZeroClawApplication.kt` updated with new `EventBridge(context, ...)` constructor
    - `EventBridgeTest.kt` updated for context-backed string resolution
  - Migrated Quick Settings tile labels to resources:
    - `DaemonTileService.kt`
  - Added EN/zh-CN resources for:
    - `api_key_detail_*` titles
    - `provider_help_text_*`
    - `provider_key_prefix_hint_*`
    - `event_bridge_*`
    - `daemon_tile_label_*`
- 2026-03-07: Stage 3 seventh-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
  - Targeted tests passed:
    - `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.data.ProviderKeyValidatorTest --tests com.zeroclaw.android.data.validation.ProviderValidatorTest`
    - `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.service.EventBridgeTest`
- 2026-03-07: Stage 3 eighth wave completed for network-scan runtime error text:
  - `NetworkScanner.kt` local-network unavailable error migrated to `R.string.network_scanner_not_connected_local_network`
  - Added EN/zh-CN string resources for the above key
- 2026-03-07: Stage 3 eighth-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
- 2026-03-07: Stage 3 ninth wave completed for shared runtime error sanitizer localization:
  - `ErrorSanitizer.kt` added context-aware localization APIs:
    - `sanitizeForUi(context, e)`
    - `sanitizeMessage(context, raw)`
  - Migrated ViewModel call sites to localized sanitizer output:
    - `PluginsViewModel.kt`
    - `SkillsViewModel.kt`
    - `TerminalViewModel.kt`
    - `CronJobsViewModel.kt`
    - `CostDetailViewModel.kt`
    - `MemoryBrowserViewModel.kt`
    - `ToolsBrowserViewModel.kt`
    - `AuthProfilesViewModel.kt`
  - Added EN/zh-CN resources for sanitizer messages:
    - `error_sanitizer_*`
- 2026-03-07: Stage 3 ninth-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
  - Targeted tests passed:
    - `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.service.EventBridgeTest --tests com.zeroclaw.android.data.validation.ProviderValidatorTest`
- 2026-03-07: Stage 3 tenth wave cleanup:
  - Removed legacy non-localized `ErrorSanitizer` fallback API paths:
    - dropped `sanitizeForUi(e)` and `sanitizeMessage(raw)`
    - removed unused `STATUS_PARSE_ERROR` English constant
  - Kept only context-aware localized sanitizer API usage across UI/ViewModels
- 2026-03-07: Stage 3 tenth-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
  - Targeted tests passed:
    - `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.service.EventBridgeTest --tests com.zeroclaw.android.data.validation.ProviderValidatorTest`
- 2026-03-07: Stage 3 eleventh wave completed for fallback hardcoded cleanup:
  - Removed provider-level hardcoded fallback text in `ProviderRegistry.kt`:
    - deleted all `helpText = "..."`
    - deleted all `keyPrefixHint = "..."`
  - Runtime text now resolves from i18n mappings (`ProviderMetadataText.kt`) rather than data-layer English literals.
  - Expanded `ErrorSanitizer` known-pattern localization coverage for registry/storage/status failure messages:
    - registry HTTPS required
    - registry fetch failure
    - registry empty response
    - registry response too large
    - invalid/malformed native status JSON
    - storage unavailable/readback failure
  - Added EN/zh-CN resources for the above sanitizer patterns.
- 2026-03-07: Stage 3 eleventh-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
  - Targeted tests passed:
    - `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.data.ProviderKeyValidatorTest --tests com.zeroclaw.android.service.EventBridgeTest`
- 2026-03-08: Stage 3 twelfth wave completed for plugin registry sync error-path localization:
  - Refactored `OkHttpPluginRegistryClient.kt` to throw typed exceptions instead of hardcoded English message strings:
    - `PluginRegistryFetchException.HttpsRequired`
    - `PluginRegistryFetchException.HttpFailure`
    - `PluginRegistryFetchException.EmptyResponseBody`
    - `PluginRegistryFetchException.ResponseTooLarge`
  - Updated `PluginsViewModel.syncNow()` to map typed exceptions to localized UI messages.
  - Added bilingual string resource:
    - `plugins_sync_registry_http_failed`
- 2026-03-08: Stage 3 twelfth-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
  - Targeted tests passed:
    - `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.ui.screen.plugins.PluginsViewModelTest --tests com.zeroclaw.android.service.EventBridgeTest`
- 2026-03-08: Stage 3 thirteenth wave completed for locale-aware number formatting:
  - `CostFormatting.kt`:
    - migrated USD formatting from fixed `Locale.US` to locale-aware currency formatter with USD currency unit
  - `CostDetailScreen.kt`:
    - migrated token compact number decimal formatting from fixed `Locale.US` to locale-aware number formatter
- 2026-03-08: Stage 3 thirteenth-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
- 2026-03-08: Stage 3 fourteenth wave completed for CI guardrail:
  - Added script: `scripts/check_i18n_hardcoded_ui.sh`
    - Fails on hardcoded UI literal patterns in `app/src/main/java/com/zeroclaw/android/ui`
    - Supports one-line opt-out via `i18n-ignore`
  - Integrated script into CI Kotlin lint job:
    - `.github/workflows/ci.yml` (`lint-kotlin` job)
- 2026-03-08: Stage 3 fourteenth-wave verification:
  - Local guardrail check passed: `./scripts/check_i18n_hardcoded_ui.sh`
- 2026-03-08: Stage 3 fifteenth wave completed for user-visible unknown/fallback copy:
  - `DaemonViewModel.kt`:
    - `"Unknown daemon error"` fallback migrated to `R.string.daemon_unknown_error_fallback`
  - `CostDetailViewModel.kt`:
    - model-name fallback no longer emits hardcoded `"unknown"` directly
    - uses internal sentinel and maps to localized `R.string.common_unknown` for UI output
  - `AboutScreen.kt`:
    - crate version fallback migrated from hardcoded `"unknown"` to localized `R.string.common_unknown`
  - Added EN/zh-CN resources:
    - `daemon_unknown_error_fallback`
    - `common_unknown`
  - Updated `DaemonViewModelTest.kt` mocks for resource-backed fallback lookup.
- 2026-03-08: Stage 3 fifteenth-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
  - Targeted unit tests passed:
    - `./gradlew :app:testDebugUnitTest --tests com.zeroclaw.android.viewmodel.DaemonViewModelTest --tests com.zeroclaw.android.service.EventBridgeTest`
- 2026-03-08: Stage 3 sixteenth wave completed for home bottom navigation labels:
  - `TopLevelDestination.kt`:
    - migrated top-level destination labels from hardcoded `String` to `@StringRes` IDs
  - `ZeroClawAppShell.kt`:
    - navigation suite now resolves destination labels via `context.getString(...)` for both text and icon content descriptions
  - Added EN/zh-CN resources:
    - `top_level_destination_dashboard`
    - `top_level_destination_connections`
    - `top_level_destination_plugins`
    - `top_level_destination_terminal`
    - `top_level_destination_settings`
- 2026-03-08: Stage 3 seventeenth wave completed for provider metadata regression fix:
  - `ProviderRegistry.kt`:
    - restored legacy fallback population for `ProviderInfo.keyPrefixHint/helpText`
    - added centralized fallback maps for provider help text and key-prefix hints
    - ensured `allProviders` applies fallback injection after registry assembly
  - Purpose:
    - keep existing validation/test paths stable while resource-mapped i18n migration is in progress
- 2026-03-08: Stage 3 eighteenth wave completed for navigation/deep-link visible copy:
  - `ZeroClawAppShell.kt`:
    - migrated top-bar back button content description and sub-screen titles to string resources
  - `ZeroClawNavHost.kt`:
    - migrated API key export share subject/chooser titles to string resources
  - `ExternalAppLauncher.kt` + `DeepLinkButton.kt`:
    - migrated deep-link button labels from hardcoded strings to `@StringRes` labels
  - Added EN/zh-CN resources:
    - `navigation_back_content_description`
    - `nav_screen_title_*` (sub-screen top-bar titles)
    - `api_keys_share_subject`
    - `api_keys_share_chooser_title`
    - `external_app_*` deep-link labels
- 2026-03-08: Stage 3 eighteenth-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`
  - UI hardcoded literal check passed: `./scripts/check_i18n_hardcoded_ui.sh`
  - Device install blocked: no connected device (`adb devices -l` returned empty)
- 2026-03-08: Stage 3 nineteenth wave completed for daemon activity feed copy:
  - `ZeroClawDaemonService.kt`:
    - migrated activity-feed and startup-blocking daemon messages to string resources
      - `daemon_start_blocked_no_api_key`
      - `daemon_activity_stopped_by_user`
      - `daemon_activity_stop_failed`
      - `daemon_activity_restored_after_process_death`
      - `daemon_activity_started_on_host_port`
      - `daemon_activity_start_failed`
      - `daemon_activity_network_connectivity_lost`
      - `daemon_activity_network_connectivity_restored`
      - `daemon_activity_config_validation_failed`
      - `daemon_activity_config_validation_error`
  - Added EN/zh-CN resources for the above messages.
- 2026-03-08: Stage 3 nineteenth-wave verification:
  - Build verification passed: `./gradlew :app:compileDebugKotlin`

## 2026-03-07 Stage 2 Completion

- All tracked Stage 2 literal patterns in `app/src/main/java/com/zeroclaw/android/ui` are now migrated to resources:
  - `text = "..."`
  - `title = "..."`
  - `subtitle = "..."`
  - `contentDescription = "..."`

## 2026-03-07 Stage 3 Backlog (Runtime Messages)

- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/terminal/TerminalViewModel.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/settings/apikeys/ApiKeysViewModel.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/settings/channels/ChannelsViewModel.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/plugins/PluginsViewModel.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/settings/cron/CronJobsViewModel.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/plugins/SkillsViewModel.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/settings/apikeys/AuthProfilesViewModel.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/settings/memory/MemoryBrowserViewModel.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/onboarding/OnboardingViewModel.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/settings/memory/MemoryBrowserScreen.kt` (category labels now resource-backed)
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/terminal/CommandRegistry.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/settings/apikeys/ApiKeysViewModel.kt` helper cleanup (`ConnectionErrorKind`)
- `done` `app/src/main/java/com/zeroclaw/android/data/validation/ValidationResult.kt` (resource-backed payload support)
- `done` `app/src/main/java/com/zeroclaw/android/data/validation/ChannelValidator.kt`
- `done` `app/src/main/java/com/zeroclaw/android/data/validation/ProviderValidator.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/onboarding/OnboardingCoordinator.kt` (OAuth validation/runtime messages)
- `done` `app/src/main/java/com/zeroclaw/android/service/DoctorValidator.kt`
- `done` `app/src/main/java/com/zeroclaw/android/service/DaemonNotificationManager.kt`
- `done` `app/src/main/java/com/zeroclaw/android/service/SetupOrchestrator.kt`
- `done` `app/src/main/java/com/zeroclaw/android/data/channel/ChannelSetupSpec.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/component/setup/ChannelSetupFlow.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/component/setup/InstructionsList.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/i18n/ChannelTypeDisplayName.kt` (all channel types mapped)
- `done` `app/src/main/java/com/zeroclaw/android/ui/i18n/ChannelFieldLabel.kt`
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/settings/channels/ChannelDetailScreen.kt` (localized field labels)
- `done` `app/src/main/java/com/zeroclaw/android/ui/screen/onboarding/steps/ChannelSetupStep.kt` (localized required field labels)

### Out of Scope (confirmed non-UI literal)

- `app/src/main/java/com/zeroclaw/android/service/DaemonServiceBridge.kt`: internal probe text `"ok"` for local IPC check.
