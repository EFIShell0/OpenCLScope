# OpenCLScope Engineering Rules

## Immutable upstream parity reference
- VulkanScope 3.0.12 is the UI, navigation, build-chain, accessibility, update, report, Analysis, licensing, file-management and security reference for OpenCLScope 0.9.0.
- Common behavior is ported without simplifying, replacing or version-downgrading the upstream behavior.
- Allowed intentional differences are OpenCL API semantics, package/application identity, OpenCL branding and the OpenCL green accent palette.
- Vulkan-only capability concepts must not be fabricated in OpenCLScope. A Vulkan Surface/WSI, Turnip replacement-driver path or Vulkan Video capability is replaced only by an actual OpenCL counterpart where one exists; otherwise the UI explicitly identifies the API-specific difference.

## Non-negotiable
- Source-code comments are forbidden.
- Security, correctness, memory safety, performance, accessibility and usability are never traded away for convenience.
- No guessed hardware capabilities.
- Unknown, unsupported, unavailable and not-applicable are distinct states.
- Registry/reference presence is not runtime support evidence.
- Runtime OpenCL extension names are displayed exactly as returned by the implementation.
- A missing query result is unavailable or unknown unless an actual OpenCL result proves a stronger state.
- Device/report data stay on-device unless the user explicitly exports, shares, fetches or submits.
- Database upload is explicit, complete-report-only and never automatic or background.
- Report upload excludes IMEI, Android ID, serial numbers, MAC addresses, accounts, authentication tokens and private file paths.
- Network access is limited to explicit OpenCLScope Database traffic and the configured official OpenCLScope release/update path over HTTPS.
- Release APK installation always requires an explicit user action after package identity, signing-certificate and version verification.
- Large collections are lazy, searchable and bounded.
- UI-thread blocking native collection and network work are forbidden.
- Visible icons preserve semantic identity and may not reuse the exact same treatment for unrelated simultaneously visible actions.

## Build parity
- Android Gradle Plugin 9.4.1 exactly.
- Kotlin Compose plugin 2.4.10 exactly.
- Gradle 9.7.1 exactly.
- compileSdk 37 exactly.
- targetSdk 37 exactly.
- minSdk 31 exactly.
- NDK 29.0.14206865 exactly.
- C++20.
- armeabi-v7a, arm64-v8a and x86_64 required; x86 excluded.
- Release builds are non-debuggable, minified and resource-shrunk.
- Dependency versions match VulkanScope 3.0.12 exactly: Core KTX 1.19.0, Core SplashScreen 1.2.0, Activity Compose 1.13.0, Compose UI/Foundation/Animation 1.12.0, Material 3 1.5.0-alpha27, Lifecycle Runtime Compose 2.11.0, OkHttp 5.5.0 and ZXing Core 3.5.4.

## Branding and UI parity
- Launcher, adaptive foreground, horizontal logo, opening-animation logo, About/Info logo and TV banner use the official OpenCL geometry combined with the unmodified VulkanScope SCOPE wordmark/layout.
- All visible OpenCL logo geometry is white, including the four regions that are colored in the official source artwork. Geometry may not be redrawn or approximated.
- AMOLED black, surface hierarchy, shape radii, typography direction policy, compact bottom-navigation dimensions, selected-indicator motion, scroll-boundary arrows, focus behavior and page spacing follow VulkanScope 3.0.12.
- The intentional color difference is the OpenCL green accent family instead of Vulkan red.
- Opening animation gates collection and optional release checking until it completes unless disabled in Settings. Disabled system animations bypass it.

## Accessibility and form factors
- TalkBack headings, labels, state announcements and click/long-click descriptions are required.
- RTL layout and directional chevrons are required.
- D-pad Up/Down/Left/Right, Page Up/Page Down and long OK/Enter copy behavior are required on applicable screens.
- Android TV, ChromeOS/desktop pointer behavior and keyboard focus must not depend on touch input.
- High font scale and high DPI must not clip primary navigation or critical actions.
- Every scrollable page exposes the same transient top/bottom scroll-boundary indicators used by the reference UI.

## OpenCL collection
- `cl.xml` is bundled as the canonical naming/reference source and is never fetched at runtime.
- Platform/device properties are queried from the live OpenCL runtime and retain their exact query state/result code.
- Platform and device extension strings are runtime evidence and are kept distinct.
- `clGetSupportedImageFormats` evidence is scoped to the exact queried context, flags and image-object type.
- Missing OpenCL loader/platform/device/function paths never become inferred unsupported hardware.
- Imported reports are historical evidence and cannot be resubmitted as a fresh live report.

## Reports, Analysis and Database
- Complete JSON/TXT/HTML export retains unavailable, unknown and error states.
- Analysis presentation filters never alter collection/export/Database payload semantics.
- Analysis workspace includes Compare, Search, Runtime/Drivers, Diagnostics, Requirements, Minimums, Graph, API-specific Presentation counterpart, Raw JSON, Database, History, Watched, Quality, Share and Tests destinations matching the VulkanScope workspace structure.
- Local history and watched evidence are bounded.
- Quality/integrity scoring describes collection evidence only and is never presented as conformance, performance, certification, vendor or GPU quality ranking.
- Database submission uses the complete live technical report or does not submit.

## Settings, licenses, updates and files
- Settings exposes Info, Reports & Database, runtime/update preferences, Obtainium, opening-animation control and the report File Manager.
- Info lists the actual packaged build chain and library versions and exposes packaged license text.
- Obtainium uses only the configured official release repository; no fabricated repository URL is shipped.
- File Manager access to shared storage begins only after explicit user action and remains bounded to visible file operations.

## Mandatory release evidence
- Every release starts from immutable predecessor and VulkanScope reference archives.
- Audit precedes patching and parity differences are classified and documented.
- A gate added for a regression includes a deliberate negative mutation proving the defect would fail.
- Static verification, native tests, Database contract tests, resource-reference checks and archive integrity checks run before packaging.
- Device-only claims remain NOT EXECUTED unless actually observed on Android hardware/emulator/TV/TalkBack/RTL/high-font/high-DPI configurations.
## 0.9.0 parity locks
- Android SAF document contracts and content-URI report streams are forbidden for report import/export; the VulkanScope 3.0.12 direct shared-storage File Manager is the only report file-selection surface.
- File Manager access is user-initiated, requires explicit shared-storage permission, rejects paths outside canonical shared storage, bounds scans to 4096 entries and uses atomic replacement for export.
- List, compact, details, grid, dense-grid and large-grid modes plus A–Z, Z–A, modified-newest/oldest and created-newest/oldest sorting must remain functional, not decorative labels.
- Device, extension, evidence-state, Database-mode and Encyclopedia category selectors use the common searchable/paged Expressive selection surface; ad-hoc FilterChip/LazyRow selection bars are forbidden.
- When multiple OpenCL devices exist, the device selector is a common page-level surface like VulkanScope and not duplicated inside the Devices destination.
- While opening animation is active, application header, navigation, transient banners and page content must not be composed behind it.

