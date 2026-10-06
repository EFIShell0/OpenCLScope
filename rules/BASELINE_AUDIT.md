# Immutable source inputs — OpenCLScope 0.3.0

| Source | Bytes | SHA-256 | ZIP entries |
|---|---:|---|---:|
| OpenCLScope-0.2.0.zip | 1227126 | `bc771e0faf1eb1fcc101c7b4278387441281e2cb73d60885b184baed19f0a3d6` | 154 |
| OpenCLScope-Database-0.1.0.zip | 406293 | `0fa985c4874321ec7bfdeceff8de4f3c2309ba704e75f2a1952cd655fb6676bc` | 34 |
| VulkanScope-3.0.12.zip | 4211536 | `8ce46a6d67824abd720fe11da3605b1693873f8de2b4be04fe58283b361d1ff2` | 856 |
| VulkanScope-Database-1.4.12.zip | 5286193 | `05050c880d89868e2dec0fd6c71b925591a3f9194be829ef9b1ad9f499e89c3c` | 840 |
| opencl.svg | 5713 | `7e99195dec3f96efb9cfd6aa137af390f9f30711b6e6a0573bfc47f50ab77f1e` | — |
| cl.xml | 533908 | `c24f63ad9aa5776fe67808926ae85613248ebbeefa7373fceb406a0bd12ce016` | — |

## Observed regressions in 0.2.0

- Launcher PNG omits the original VulkanScope two-tier SCOPE wordmark.
- Opening animation uses a fixed delay instead of the actual VulkanScope logo sequence.
- Preferences lacks Obtainium, per-release update preferences and runtime Update section.
- The compact navigation uses the wrong max width and differs from VulkanScope compact geometry.
- No persisted analysis history, watch comparison, minimum checks or local report format alternatives.
- Worker refuses any producer other than 0.2.0, so an application version bump would cause HTTP 400.

All changes are scoped to direct parity defects or protocol compatibility; Vulkan-specific Surface/Turnip hardware operations cannot be represented as OpenCL features.
