# OpenCLScope 0.2.0 / Database 0.1.0 — Pre-patch comparison audit

## Immutable baselines
| Source | Bytes | Entries | SHA-256 |
|---|---:|---:|---|
| VulkanScope-3.0.12.zip | 4211536 | 856 | `8ce46a6d67824abd720fe11da3605b1693873f8de2b4be04fe58283b361d1ff2` |
| VulkanScope-Database-1.4.12.zip | 5286193 | 840 | `05050c880d89868e2dec0fd6c71b925591a3f9194be829ef9b1ad9f499e89c3c` |
| OpenCLScope-0.1.0.zip | 1208650 | 149 | `402151f5faaa9bf748cf316e6ff9e68dd8007ecbcab9c41f6c94932118b9ddce` |
| cl.xml | 533908 | 1 | `c24f63ad9aa5776fe67808926ae85613248ebbeefa7373fceb406a0bd12ce016` |
| opencl.svg | 5713 | 1 | `7e99195dec3f96efb9cfd6aa137af390f9f30711b6e6a0573bfc47f50ab77f1e` |

## Behavioral/visual parity matrix
| Capability | VulkanScope 3.0.12 | OpenCLScope 0.1.0 | New scope classification |
|---|---|---|---|
| Compose dark expressive UI / brand / navigation | Included | Initial analogue, fewer sections | USABILITY |
| Overview and full runtime collection | Vulkan native probe | OpenCL native probe | API-specific by design |
| Properties, feature states, limits, memory, formats, extensions | Vulkan registry probes | OpenCL cl.xml probes | API-specific by design |
| Profile/capability checklist | Included | Basic checklist | USABILITY |
| Query diagnostics, tests, graphs, encyclopedia | Included | Basic paths | USABILITY |
| Full local report export/import | Multiple formats & history | Local JSON only | SPEC-MISSING |
| Database POST, list, ID lookup, permalink, comparison | HTTPS explicit submission | Absent | SPEC-MISSING |
| Production schema + Cloudflare Worker/D1 | Vulkan schema v2/v3 | No database | SPEC-MISSING |
| GitHub Pages presentation/cache snapshot | Included | No database | SPEC-MISSING |
| Surface/HDR/Turnip driver replacement | Vulkan-only | Not applicable | NOT APPLICABLE; never fabricate OpenCL equivalent |
| Android native multi-ABI | 3 ABIs | 3 ABIs | PRESERVE |
| No speculative hardware inference | Required | Required | PRESERVE |

## Pre-fix regression signature
`OpenCLScope 0.1.0` lacks both `DatabaseClient.kt` and the OpenCL Worker submission contract; therefore public user-initiated submission and remote report lookup cannot run, unlike the reference. Failing-before-fix oracle: `test_database_contract.py` rejects the predecessor for absent database transport and versioned envelope. New checks must pass on the successor and fail on a negative mutation, with no-loader as the permitted false-positive control.

## Exact parity caveat
One-to-one API parity is technically impossible: OpenCL platforms/devices/image queries are not Vulkan Surface/WSI/Turnip instances. UI hierarchy, navigation patterns, privacy, collection semantics, report usability, database/API endpoints and publication architecture can be made analogous while retaining canonical OpenCL fields.
