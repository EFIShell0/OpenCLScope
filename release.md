# OpenCLScope™ 0.9.0

## Added
- Added the VulkanScope 3.0.12 full-screen direct shared-storage File Manager with breadcrumb navigation, expandable search, six real view modes, six sort modes, bounded canonical-path validation, atomic export replacement and D-pad/desktop navigation.
- Added VulkanScope-style searchable, paged Expressive selectors across device, extension, evidence-state, Database and Encyclopedia choices.

## Changed
- Removed Android SAF document-picker import/export paths; report import and JSON/TXT/HTML export now use the in-app File Manager after explicit shared-storage access.
- Moved multi-device selection to the common page level like VulkanScope instead of limiting it to the Devices page.
- Reworked Encyclopedia category/page selection and shared Analysis filters to the same VulkanScope selector language.
- Application version is 0.9.0 / versionCode 900; paired Database is OpenCLScope™ Database 0.8.0.

## Fixed
- Opening animation now exclusively gates composition of the application chrome, so header, navigation and page buttons cannot appear behind the animation.
- Preserved the 0.8.1 coroutine-boundary build fix and added release gates for SAF removal, File Manager parity, selector parity and exclusive startup composition.
