import hashlib
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
resource = root / 'app/src/main/res'
asset = root / 'app/src/main/assets/registry'
code = root / 'app/src/main/java/com/efishell/openclscope'


def ensure(condition, message):
    if not condition:
        raise AssertionError(message)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


ensure(sha(asset / 'cl.xml') == 'c24f63ad9aa5776fe67808926ae85613248ebbeefa7373fceb406a0bd12ce016', 'Registry must be the original uploaded cl.xml')
ensure(sha(asset / 'opencl.svg') == '7e99195dec3f96efb9cfd6aa137af390f9f30711b6e6a0573bfc47f50ab77f1e', 'SVG must be the original uploaded OpenCL mark')
registry = ET.parse(asset / 'cl.xml').getroot()
ensure(registry.tag == 'registry', 'The normative OpenCL reference XML is missing')
catalog = json.loads((asset / 'catalog.json').read_text())
ensure(catalog['registrySha256'] == sha(asset / 'cl.xml'), 'Catalog not tied to embedded reference XML')
ensure(len(catalog['platformQueries']) == 16 and len(catalog['deviceQueries']) == 209, 'Normative OpenCL query catalog unexpectedly changed')
ensure(len(catalog['extensions']) == 156, 'OpenCL extension reference count unexpectedly changed')
ensure(catalog.get('enums', {}).get('cl_device_type', {}).get('CL_DEVICE_TYPE_ALL') == '0xFFFFFFFF', 'CL_DEVICE_TYPE_ALL registry token changed')

ensure(catalog.get('maxCoreFeature') == 'CL_VERSION_3_1', 'Embedded cl.xml core baseline is not OpenCL 3.1')
required_platform_names = {e.get('name') for req in registry.findall('.//require') if req.get('comment') == 'cl_platform_info' for e in req.findall('enum') if e.get('name')}
required_device_names = {e.get('name') for req in registry.findall('.//require') if req.get('comment') == 'cl_device_info' for e in req.findall('enum') if e.get('name')}
all_enum_values = {}
for group in registry.findall('enums'):
    for entry in group.findall('enum'):
        name, value = entry.get('name'), entry.get('value', '')
        if name and re.fullmatch(r'0x[0-9a-fA-F]+', value): all_enum_values[name] = int(value, 0)
required_platform_values = {all_enum_values[name] for name in required_platform_names if name in all_enum_values}
required_device_values = {all_enum_values[name] for name in required_device_names if name in all_enum_values}
ensure({row['value'] for row in catalog['platformQueries']} == required_platform_values, 'Platform query set is not derived exactly from normative cl_platform_info requirements')
ensure({row['value'] for row in catalog['deviceQueries']} == required_device_values, 'Device query set is not derived exactly from normative cl_device_info requirements')
ensure(len({e['name'] for e in catalog['deviceQueries']}) == len(catalog['deviceQueries']), 'Duplicate device query')
ensure(len({e['name'] for e in catalog['platformQueries']}) == len(catalog['platformQueries']), 'Duplicate platform query')

for path in resource.rglob('*.xml'):
    ET.parse(path)
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
ns = '{http://schemas.android.com/apk/res/android}'
permissions = {x.get(ns + 'name') for x in manifest.findall('uses-permission')}
ensure(permissions == {'android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE', 'android.permission.REQUEST_INSTALL_PACKAGES', 'android.permission.MANAGE_EXTERNAL_STORAGE'}, 'Manifest permissions diverge from VulkanScope 3.0.12 parity')
ensure(manifest.find('application').get(ns + 'usesCleartextTraffic') == 'false', 'Cleartext networking must be disabled')
application = manifest.find('application')
ensure(application is not None and application.get(ns + 'label') == 'OpenCLScope', 'Application label changed')
native_libraries = {x.get(ns + 'name'): x.get(ns + 'required') for x in application.findall('uses-native-library')}
ensure(native_libraries.get('libOpenCL.so') == 'false', 'Android 12+ vendor OpenCL visibility request missing')
ensure(native_libraries.get('libGLES_mali.so') == 'false', 'Optional Mali OpenCL carrier visibility request missing')
for optional_library in ('libOpenCL-pixel.so','libOpenCL-car.so','libOpenCL_adreno.so','libmali.so','libPVROCL.so'):
    ensure(native_libraries.get(optional_library) == 'false', f'Optional Android OpenCL carrier visibility request missing: {optional_library}')
ensure(manifest.find('uses-feature') is not None, 'Platform compatibility metadata missing')
build = (root / 'app/build.gradle.kts').read_text()
project_build = (root / 'build.gradle.kts').read_text()
ensure('applicationId = "com.efishell.openclscope"' in build, 'Application ID incorrect')
ensure('namespace = "com.efishell.openclscope"' in build, 'Namespace incorrect')
ensure('versionName = "0.9.0"' in build and 'versionCode = 900' in build, 'Version metadata incorrect')
ensure('version "9.4.1"' in project_build, 'AGP baseline incorrect')
ensure('compileSdk = 37' in build and 'minorApiLevel' not in build, 'compileSdk must match VulkanScope 3.0.12 exactly')
ensure('minSdk = 31' in build and 'targetSdk = 37' in build, 'Android minimum/target SDK diverged from reference')
ensure('id("com.android.application") version "9.4.1"' in project_build, 'AGP must exactly match VulkanScope')
ensure('id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"' in project_build, 'Kotlin Compose plugin must exactly match VulkanScope')
exact_dependencies = [
    'androidx.core:core-ktx:1.19.0',
    'androidx.core:core-splashscreen:1.2.0',
    'androidx.activity:activity-compose:1.13.0',
    'androidx.compose.ui:ui:1.12.0',
    'androidx.compose.foundation:foundation:1.12.0',
    'androidx.compose.animation:animation:1.12.0',
    'androidx.compose.material3:material3:1.5.0-alpha27',
    'androidx.lifecycle:lifecycle-runtime-compose:2.11.0',
    'com.squareup.okhttp3:okhttp:5.5.0',
    'com.google.zxing:core:3.5.4',
]
for dep in exact_dependencies:
    ensure(dep in build, f'Build dependency differs from VulkanScope reference: {dep}')
ensure('ndkVersion = "29.0.14206865"' in build, 'NDK version differs from VulkanScope reference')
wrapper=(root/'gradle/wrapper/gradle-wrapper.properties').read_text()
ensure('gradle-9.7.1-bin.zip' in wrapper, 'Gradle version differs from VulkanScope reference')
styles=(resource/'values/styles.xml').read_text()
ensure('<item name="android:windowActionModeOverlay">true</item>' in styles, 'AppTheme action mode overlay differs from VulkanScope')
ensure(application.get(ns+'allowBackup') == 'false' and application.get(ns+'supportsRtl') == 'true', 'Privacy backup/RTL must be explicit')
ensure(all(x in build for x in ('"armeabi-v7a"', '"arm64-v8a"', '"x86_64"')), 'An ABI is missing')
ensure('isMinifyEnabled = true' in build and 'isShrinkResources = true' in build, 'Release optimizations disabled')
ensure((root / 'gradle/wrapper/gradle-wrapper.jar').exists(), 'Gradle wrapper JAR missing')

referenced = set()
for path in code.rglob('*.kt'):
    text = path.read_text()
    ensure('com.efishell.vulkanscope' not in text, 'Stale app ID present')
    ensure('com.efishell.vulkanscope' not in text, 'Vulkan application identity leaked')
    referenced.update(re.findall(r'R\.drawable\.([a-zA-Z][a-zA-Z_0-9]*)', text))
existing = {x.stem for x in resource.rglob('*') if x.is_file()}
ensure(not referenced - existing, f'R.drawable does not exist: {sorted(referenced-existing)}')
res_text = '\n'.join(x.read_text() for x in resource.rglob('*.xml'))
referenced_xml = set(re.findall(r'@(?:drawable|mipmap)/([a-zA-Z][a-zA-Z_0-9]*)', res_text))
ensure(not referenced_xml - existing, f'Resource XML contains a dangling image reference: {sorted(referenced_xml-existing)}')
native_source=(root / 'app/src/main/cpp/openclscope.cpp').read_text()
ensure('dlopen' in native_source, 'Live OpenCL loader missing')
for candidate in ('libOpenCL.so','libOpenCL-pixel.so','libOpenCL-car.so','libOpenCL_adreno.so','libGLES_mali.so','libmali.so','libPVROCL.so','/vendor/lib64/libOpenCL.so','/vendor/lib/libOpenCL.so'):
    ensure(candidate in native_source, f'OpenCL runtime discovery candidate missing: {candidate}')
ensure('tryIcdDirectory' in native_source and '/system/vendor/Khronos/OpenCL/vendors' in native_source and '/vendor/etc/OpenCL/vendors' in native_source, 'Android ICD vendor-file discovery path missing')
ensure('clIcdGetPlatformIDsKHR' in native_source and 'icdPlatforms' in native_source, 'OpenCL ICD platform enumeration fallback missing')
ensure('\\"appVersion\\":\\"0.9.0\\"' in (root/'app/src/main/cpp/openclscope.cpp').read_text(), 'Native report version missing')
ensure('"0.9.0"' in (code/'DatabaseClient.kt').read_text(), 'Producer version mismatch: HTTP 400 risk')
ensure('clGetSupportedImageFormats' in native_source, 'Live image-format query missing')
ensure('constexpr CLULong kAllDevices = 0xFFFFFFFFull;' in native_source, 'Device enumeration must use the exact CL_DEVICE_TYPE_ALL registry value')
ensure('application/json' in (code / 'MainActivity.kt').read_text(), 'Local JSON file action missing')
ensure((root / 'rules/PROJECT_RULES.md').exists() and (root / 'rules/UPSTREAM_REFERENCE_RULES.md').exists(), 'Rule provenance incomplete')
print('PASS: embedded registry/logo SHA-256, OpenCL 3.1 registry baseline, exact 16+209 normative query derivation and XML parse')
print('PASS: Android ID/version, three ABIs, release optimizations and HTTPS-only explicit Database permission')
print(f'PASS: all {len(referenced)} Kotlin drawable references and XML image references resolve')
print('PASS: source provenance, runtime OpenCL data paths and local report actions')

api = (code / 'DatabaseClient.kt').read_text()
activity = (code / 'MainActivity.kt').read_text()
ensure('completeEnvelope' in api and '"schemaVersion", 1' in api and '"technicalReport", original' in api, 'Full report producer contract missing')
ensure('instanceFollowRedirects = false' in api and 'HttpsURLConnection' in api and 'hasValidatedInternet(context)' in api, 'Fixed HTTPS client transport missing')
ensure('Report ID' in activity and 'Submit complete report' in activity, 'User-initiated database navigation missing')
ensure('DATABASE_API_BASE_URL' in build and 'DATABASE_WEB_BASE_URL' in build, 'Database endpoints must be bound through build configuration')
ensure('fun eligible' in api and 'origin != "Live"' in api, 'Imported technical reports must not become fresh evidence')
ensure('page = Screen.Database' in activity and 'CompactBottomNavigation' in activity, 'Database or compact navigation missing')
print('PASS: explicit user-initiated complete report production, lookup, comparison and native vs imported origin gate')

ensure((code / 'OpeningAnimation.kt').exists() and 'onFinished' in (code / 'OpeningAnimation.kt').read_text(), 'Real gated opening animation missing')
ensure((code / 'ReleaseChannel.kt').exists() and 'obtainium://app/' in (code / 'ReleaseChannel.kt').read_text(), 'Obtainium handoff missing')
ensure('RELEASE_GITHUB_REPOSITORY' in build and 'openclscopeGitHubRepository' in build, 'Repository must use a vetted build-time URL')
ensure('HistoryPage' in activity and 'WatchPage' in activity and 'MinimumPage' in activity, 'Core VulkanScope-equivalent analysis destinations missing')
ensure('ReportExport.encode' in activity and all(x in activity for x in ('ExportFormat.JSON', 'ExportFormat.TXT', 'ExportFormat.HTML')) if 'ExportFormat.JSON' in activity else 'ReportExport.encode' in activity and 'ExportFormat.TXT' in activity and 'ExportFormat.HTML' in activity, 'Complete report exports missing')
ensure((root / 'branding/openclscope_launcher.svg').exists(), 'Full brand launcher SVG missing')
ET.parse(root / 'branding/openclscope_launcher.svg')
ensure((resource / 'drawable-nodpi/openclscope_scope_wordmark.png').exists(), 'Unaltered inherited SCOPE wordmark missing')
ensure((resource / 'mipmap-xxxhdpi/ic_launcher.png').exists(), 'Launcher must include OpenCL and SCOPE')
print('PASS: VulkanScope two-tier logo source, animated opening gate, Obtainium, History, Watch, Minimum and full export')

original_scope = root / 'branding/upstream_scope_wordmark_unmodified.png'
ensure(sha(original_scope) == '9167a36fe6f31e21d796bc01d42276d1e0f2397193ab6e814133b7bf0dc72ed9', 'Inherited exact SCOPE source changed')
ns_svg = '{http://www.w3.org/2000/svg}'
svg_root = ET.parse(root / 'branding/openclscope_launcher.svg').getroot()
def brand_layers(item):
    original_mark = item.find(ns_svg + 'g')
    official_paths = len(list(original_mark.iter(ns_svg + 'path'))) if original_mark is not None else 0
    converted_scope = item.findall(ns_svg + 'path')
    return original_mark is not None and official_paths >= 6 and len(converted_scope) == 1 and len(converted_scope[0].attrib.get('d','')) > 200
ensure(brand_layers(svg_root), 'SVG missing original OpenCL mark or actual SCOPE path layer')
import copy
without_scope = copy.deepcopy(svg_root)
without_scope.remove(without_scope.findall(ns_svg + 'path')[-1])
ensure(not brand_layers(without_scope), 'NEGATIVE MUTATION: loss of SCOPE must fail')
print('PASS: launcher source layer parity and deliberate missing-SCOPE mutation are correctly gated')

from PIL import Image
baseline=root.parent/'base/app/src/main/res'
if baseline.exists():
    for folder, name in [('mipmap-xxxhdpi','ic_launcher.png'), ('drawable-nodpi','openclscope_logo_foreground.png'), ('drawable-nodpi','openclscope_logo_horizontal_aligned.png')]:
        original_name = name.replace('openclscope_', 'vulkanscope_')
        original=Image.open(baseline/folder/original_name).convert('RGBA')
        actual=Image.open(resource/folder/name).convert('RGBA')
        ensure(original.size == actual.size, 'Brand dimensions diverge from reference')
        if folder.startswith('mipmap'):
            y=round(original.height*108/192)
            expected=original.crop((0,y,original.width,original.height))
            given=actual.crop((0,y,actual.width,actual.height))
        elif name.endswith('foreground.png'):
            expected=original.crop((0,59,108,108));given=actual.crop((0,59,108,108))
        else:
            expected=original.crop((250,0,546,84));given=actual.crop((250,0,546,84))
        ensure(expected.tobytes()==given.tobytes(), 'Exact original SCOPE pixel layer has changed: '+name)
    print('PASS: immutable upstream SCOPE area bitwise identical in launcher / adaptive / horizontal brands')
exact_scope_reference = {
    'ic_launcher.png': '873fde314c3e20d5f37b269ab09e3118dc0d100c3f9787bc27b5f45e7d1f1760',
    'openclscope_logo_foreground.png': 'da2db0a54db19ceb2139d39bb48199dd3991522f22417ba370b565a635801515',
    'openclscope_logo_horizontal_aligned.png': 'ddd7db5c4aca403b8e5e172454fb4a4fadc8189bba14d7d6856cc641b220c18a',
}
for folder, name, rect in [
    ('mipmap-xxxhdpi', 'ic_launcher.png', (0,108,192,192)),
    ('drawable-nodpi', 'openclscope_logo_foreground.png', (0,59,108,108)),
    ('drawable-nodpi', 'openclscope_logo_horizontal_aligned.png', (250,0,546,84)),
]:
    actual=Image.open(resource/folder/name).convert('RGBA')
    ensure(hashlib.sha256(actual.crop(rect).tobytes()).hexdigest()==exact_scope_reference[name], 'SCOPE unmodified reference integrity changed: '+name)
print('PASS: cropped SCOPE layers checked against embedded immutable reference digests')
parity=json.loads((root/'branding/immutable_scope_region_contract.json').read_text())
ensure(parity['referenceArchiveSha256']=='8ce46a6d67824abd720fe11da3605b1693873f8de2b4be04fe58283b361d1ff2', 'Wrong upstream reference logo')
for item in parity['assets']:
    img=Image.open(resource/item['path']).convert('RGBA')
    ensure(list(img.size)==item['dimensions'], 'Unexpected DPI image size: '+item['path'])
    digest=hashlib.sha256(img.crop(tuple(item['region'])).tobytes()).hexdigest()
    ensure(digest == item['scopeSha256'], 'Source SCOPE letter pixels diverged at '+item['path'])
negative=parity['assets'][0].copy();negative['scopeSha256']='0'*64
ensure(hashlib.sha256(Image.open(resource/negative['path']).convert('RGBA').crop(tuple(negative['region'])).tobytes()).hexdigest()!=negative['scopeSha256'], 'NEGATIVE MUTATION: altered SCOPE digest accepted')
print('PASS: all ten launcher-density SCOPE regions, adaptive foreground, horizontal brand + altered SHA mutation')


from PIL import Image
monochrome_assets = [
    resource/'mipmap-mdpi/ic_launcher.png', resource/'mipmap-hdpi/ic_launcher.png', resource/'mipmap-xhdpi/ic_launcher.png',
    resource/'mipmap-xxhdpi/ic_launcher.png', resource/'mipmap-xxxhdpi/ic_launcher.png',
    resource/'drawable-nodpi/openclscope_logo_foreground.png', resource/'drawable-nodpi/openclscope_logo_horizontal_aligned.png',
    resource/'drawable-nodpi/openclscope_logo_master.png', resource/'drawable-nodpi/tv_banner.png', resource/'drawable-xhdpi/tv_banner.png'
]
for image_path in monochrome_assets:
    image = Image.open(image_path).convert('RGBA')
    ensure(all(a == 0 or (max(r,g,b)-min(r,g,b) <= 3) for r,g,b,a in image.getdata()), f'Colored OpenCL logo pixel remains in {image_path.relative_to(root)}')
print('PASS: every visible OpenCL brand raster is monochrome white/gray on transparent or black surface; no colored OpenCL arc remains')

main_source=(code/'MainActivity.kt').read_text()
chrome_source=(code/'UiParityChrome.kt').read_text()
overview_source=(code/'VulkanParityOverview.kt').read_text()
ensure('Overview("Overview", R.drawable.ic_home)' in main_source, 'Overview page identity changed')
ensure('OpenCL("OpenCL", R.drawable.ic_opencl)' in main_source, 'OpenCL second tab identity changed')
ensure('Devices("Devices", R.drawable.ic_surface)' in main_source, 'OpenCL device tab must occupy VulkanScope Surface slot/icon')
ensure('Extensions("Extensions", R.drawable.ic_extensions)' in main_source, 'Extensions tab identity changed')
ensure('Info("Info", R.drawable.ic_info)' in main_source, 'Info page/icon parity missing')
ensure('vulkanParityOpenCLOverview' in main_source and 'openclscope_logo_master' not in main_source, 'Overview still uses the obsolete giant body logo path')
ensure('OpenCLHeroCard' in overview_source and 'OpenCLExploreCard' in overview_source and 'Capability snapshot' in overview_source and 'Analysis workspace' in overview_source, 'VulkanScope Overview structure parity missing')
ensure('Screen.Features, Screen.Memory, Screen.Queues' in chrome_source and '-> Screen.Overview' in chrome_source, 'Hidden detail pages must select Overview like VulkanScope')
gpu_refs=json.loads((root/'rules/VULKANSCOPE_3_0_12_GPU_REFERENCE_SHA256.json').read_text())
for name,digest in gpu_refs.items():
    current=resource/'drawable-nodpi'/name
    ensure(current.exists() and sha(current)==digest, f'GPU vendor logo differs from VulkanScope 3.0.12: {name}')
print(f'PASS: four-tab VulkanScope navigation structure, Overview composition, Info page and {len(gpu_refs)}/{len(gpu_refs)} GPU vendor logos')

parity_features=(code/'ParityFeatures.kt').read_text()
extended=(code/'ExtendedPages.kt').read_text()
for label in ('Compare','Search','Drivers','Diagnostics','Requirements','Minimums','Graph','Presentation','Raw JSON','Database','History','Watched','Quality','Share','Tests'):
    ensure(f'"{label}"' in parity_features, f'Analysis workspace destination missing: {label}')
ensure('OpenCLEncyclopediaPage' in parity_features and (root/'app/src/main/assets/registry/encyclopedia.json').exists(), 'Generated OpenCL encyclopedia missing')
encyclopedia=json.loads((root/'app/src/main/assets/registry/encyclopedia.json').read_text())
ensure(isinstance(encyclopedia, dict) and len(encyclopedia.get('entries', [])) >= 2000, 'Encyclopedia coverage unexpectedly incomplete')
ensure('OpenCLInfoPage' in parity_features and 'LibraryLicenseDialog' in parity_features, 'Info/licenses parity pages missing')
ensure('ScrollBoundaryIndicators(listState' in (code/'MainActivity.kt').read_text() and 'ScrollBoundaryIndicators(scrollState' in (code/'MainActivity.kt').read_text(), 'Scroll boundary arrows missing')
file_manager=(code/'SharedStorageFileManager.kt').read_text()
selectors=(code/'ExpressiveSelectors.kt').read_text()
all_kotlin='\n'.join(path.read_text() for path in code.glob('*.kt'))
for forbidden in ('rememberLauncherForActivityResult','ActivityResultContracts.OpenDocument','ActivityResultContracts.CreateDocument','ACTION_OPEN_DOCUMENT','ACTION_CREATE_DOCUMENT','contentResolver.openInputStream','contentResolver.openOutputStream'):
    ensure(forbidden not in all_kotlin, f'SAF path remains in source: {forbidden}')
ensure('OpenCLSharedStorageFileManager' in file_manager and 'MANAGE_APP_ALL_FILES_ACCESS_PERMISSION' in file_manager, 'VulkanScope-style shared File Manager missing')
ensure('writeSharedStorageFile' in file_manager and 'ATOMIC_MOVE' in file_manager and 'FileManagerViewMode' in file_manager and 'FileManagerSortMode' in file_manager, 'File Manager validation/view/sort/atomic export parity missing')
ensure('OpenCLExpressiveFilterBar' in selectors and 'Search filters' in selectors and 'Previous filter page' in selectors and 'Next filter page' in selectors, 'Expressive selection dialog parity missing')
ensure('OpenCLExpressiveFilterBar(categories, category, "Category")' in parity_features and 'OpenCLCollectionPager(result.size' in parity_features, 'Encyclopedia selector/pager parity missing')
ensure('OpenCLExpressiveFilterBar(' in main_source and 'Screen.Devices -> PageList("Devices"' in main_source, 'Devices/collection selector parity missing')
ensure('if (!openingGate) {' in main_source and 'return@MaterialExpressiveTheme' in main_source and main_source.index('return@MaterialExpressiveTheme') < main_source.index('val networkUiState'), 'Opening animation does not exclusively gate app chrome')
print('PASS: 15-tool Analysis workspace, generated Encyclopedia, SAF-free full-screen File Manager, expressive selectors and exclusive opening gate')

ui=(code/'MainActivity.kt').read_text()
chrome=(code/'UiParityChrome.kt').read_text()
accessibility=(code/'InputAccessibility.kt').read_text()
opening=(code/'OpeningAnimation.kt').read_text()
responsive=ui + chrome + (code/'ParityFeatures.kt').read_text() + opening
checks={
 'D-pad up/down and page navigation': all(v in accessibility for v in ('KEYCODE_DPAD_DOWN','KEYCODE_DPAD_UP','KEYCODE_DPAD_LEFT','KEYCODE_DPAD_RIGHT','KEYCODE_PAGE_DOWN','KEYCODE_PAGE_UP')),
 'D-pad Enter long press copy dialog': all(v in ui for v in ('Key.DirectionCenter','Key.Enter','Key.NumPadEnter','KEYCODE_BUTTON_SELECT','delay(550L)','Copy row','Copy value')),
 'Accessibility headings and status': all(v in responsive for v in ('heading()','LiveRegionMode.Polite','onClickLabel')),
 'RTL and layouts': all(v in responsive for v in ('LocalLayoutDirection.current','LayoutDirection.Ltr','heightIn')),
 'Focus indicators': 'onFocusChanged' in chrome and 'if (focused) AccentSoft' in chrome,
 'Reduced motion opening': 'ValueAnimator.areAnimatorsEnabled()' in opening,
 'Obtainium settings': 'obtainium://app/' in (code/'ReleaseChannel.kt').read_text(),
 'No auto upload': 'Submit complete report' in ui and 'origin != "Live"' in api,
 'Validated network gates': 'LocalValidatedInternetAvailable' in chrome and 'networkAvailable' in ui,
 'Measured popup inset': 'LocalTransientOverlayContentInset' in chrome and 'transientOverlayHeightPx' in ui,
}
for test,result in checks.items():ensure(result,test)
invalid=checks.copy();invalid['D-pad Enter long press copy dialog']=False
ensure(not all(invalid.values()), 'NEGATIVE MUTATION: absent long-press behavior must fail')
print('PASS: D-pad/TalkBack/RTL/scaled text/reduced motion & deliberate removal negative mutation structural gates')
subprocess.run(['python','-B',str(root/'tools/test_ui_parity.py')],check=True)
print('PASS: VulkanScope UI geometry/icon/banner parity gate')
subprocess.run(['python','-B',str(root/'tools/verify_kotlin_build_regressions.py')],check=True)
print('PASS: Kotlin release build regression gate')
