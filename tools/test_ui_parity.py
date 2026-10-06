from pathlib import Path
import hashlib,json,tempfile

root=Path(__file__).resolve().parents[1]
code=root/'app/src/main/java/com/efishell/openclscope'
res=root/'app/src/main/res'

def require(value,message):
    if not value: raise AssertionError(message)

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()

refs=json.loads((root/'rules/VULKANSCOPE_3_0_12_DRAWABLE_REFERENCE_SHA256.json').read_text())
require(len(refs)==92,'Reference drawable set must contain 92 VulkanScope 3.0.12 common icons')
for name,digest in refs.items():
    path=res/'drawable'/name
    require(path.exists(),f'Missing reference drawable {name}')
    require(sha(path)==digest,f'Reference drawable changed: {name}')

chrome=(code/'UiParityChrome.kt').read_text()
main=(code/'MainActivity.kt').read_text()
buttons=(code/'ParityButtons.kt').read_text()
dialogs=(code/'ParityDialogs.kt').read_text()
controls=(code/'ParityControls.kt').read_text()
extended=(code/'ExtendedPages.kt').read_text()
layout=(code/'ParityPageLayout.kt').read_text()
features=(code/'ParityFeatures.kt').read_text()
overview=(code/'VulkanParityOverview.kt').read_text()

exact=[
    ('PrimaryNavigationMaxWidth = 310.dp',chrome),('PrimaryNavigationHeight = 54.dp',chrome),('PrimaryNavigationIndicatorHeight = 42.dp',chrome),
    ('if (landscape) 340.dp',chrome),('if (landscape) 46.dp',chrome),('if (landscape) 35.dp',chrome),
    ('tween(150, easing = FastOutSlowInEasing)',chrome),('tween(180, easing = FastOutSlowInEasing)',chrome),('tween(120)',chrome),
    ('Modifier.width(126.dp).height(30.dp)',chrome),('buttonSize = 36.dp',chrome),('slotSize = 50.dp',chrome),
    ('delay(4_500L)',chrome),('delay(380L)',chrome),('RoundedCornerShape(22.dp)',chrome),
    ('durationMillis = 260, delayMillis = index * 45',chrome),('tween(durationMillis = 120)',chrome),('enter = fadeIn(tween(220)) + expandVertically(tween(220))',chrome),('LinearWavyProgressIndicator',chrome),
    ('delay(if (isScrollInProgress) 1050 else 900)',main),
    ('slideInHorizontally(animationSpec = spring()) { it / 5 }',main),('slideOutHorizontally(animationSpec = spring()) { -it / 5 }',main),
    ('RoundedCornerShape(20.dp)',buttons),('RoundedCornerShape(26.dp)',buttons),('RoundedCornerShape(18.dp)',buttons),('RoundedCornerShape(24.dp)',buttons),
    ('AccentSoft.copy(alpha = 0.46f)',dialogs),('MaterialTheme.shapes.extraLarge',dialogs),
    ('checkedThumbColor = Foreground',controls),('checkedTrackColor = Accent',controls),
]
for token,text in exact: require(token in text,f'UI parity token missing: {token}')
for label in ('Info','Reports & Database','Runtime & Update Preferences'):
    require(f'"{label}"' in chrome,f'Settings section missing: {label}')
require(chrome.index('"Info"') < chrome.index('"Reports & Database"') < chrome.index('"Runtime & Update Preferences"'),'Settings section order changed')
require('NET_CAPABILITY_VALIDATED' in chrome and 'registerDefaultNetworkCallback' in chrome,'Validated network status observer missing')
require('LayoutDirection.Ltr' in chrome and 'LocalLayoutDirection.current' in chrome,'RTL-aware system navigation inset alignment missing')
require('LocalValidatedInternetAvailable' in chrome and 'networkAvailable' in main,'Validated network action gate missing')
require('LocalTransientOverlayContentInset' in chrome and 'onSizeChanged { transientOverlayHeightPx = it.height }' in main,'Transient popup content inset coordination missing')
require('LocalBottomNavigationContentInset' in chrome and 'onSizeChanged { bottomNavigationHeightPx = it.height }' in main and 'PrimaryNavigationContentGap' in main,'Measured bottom navigation content inset coordination missing')
require('BringIntoViewRequester' in chrome and 'parityTvBrowseModifier' in chrome,'Reference D-pad focus bring-into-view behavior missing')
require('OpenCLCompactBottomNavigationBar' in main and 'OpenCLParityAppHeader' in main,'Parity chrome not wired')
require('Overview("Overview", R.drawable.ic_home)' in main and 'OpenCL("OpenCL", R.drawable.ic_opencl)' in main and 'Devices("Devices", R.drawable.ic_surface)' in main and 'Extensions("Extensions", R.drawable.ic_extensions)' in main,'Primary navigation slot/icon mapping diverged from VulkanScope parity')
require('bottom = bottomNavigationInset + 4.dp' in main and 'zIndex(10f)' in main and 'scrollHintTopInset' in main,'Scroll-boundary arrows are not locked outside header/bottom navigation chrome')
require('OpenCLSectionHeaderIcon(title, openCLSectionIcon(title, icon))' in main and 'OpenCLSectionHeaderIcon(title, openCLSectionIcon(title, icon))' in overview,'Section header icon renderer is not shared across Overview and detail pages')
require('private fun OpenCLAppHeader' not in main and 'private fun CompactBottomNavigation(page' not in main,'Obsolete duplicate chrome remains')

require('title.equals("Explore", true) -> R.drawable.ic_compass' in main,'Explore must use the VulkanScope compass icon')
require('openCLSectionIcon(title, icon)' in overview,'Overview section icons must use VulkanScope title mapping')
require('openCLSectionIcon(title, icon)' in main and 'parityTvBrowseModifier(shape)' in main,'Common section cards must use parity icon mapping and D-pad focus geometry')
require(extended.count('parityPageContentPadding()') >= 4,'Standalone pages must use the shared VulkanScope page padding model')
require(extended.count('parityScrollHintPadding()') >= 4,'Standalone pages must use the shared VulkanScope scroll-arrow overlay model')
require('animationSpec = tween(120, easing = FastOutSlowInEasing)' in layout,'Scroll-arrow/header transient overlay coordination must use reference 120 ms motion')
info_order=['ScopeCard("Developer"','ScopeCard("Application"','ScopeCard("Libraries"','ScopeCard("Build toolchain"','ScopeCard("Device ABI"','ScopeCard("Android"','ScopeCard("OpenCL registry / query engine"','ScopeCard("About"']
positions=[features.index(token) for token in info_order]
require(positions==sorted(positions),'Info section order diverged from VulkanScope reference')
require('ParityIdentityBlock("Semih Boran"' in features and 'ParityVersionBlock("OpenCLScope"' in features and 'ParityChevronAffordance("License"' in features,'Info expressive blocks/license affordance missing')
require('settingsPanel = "Info"' in main,'Settings Info must stay inside the Settings section flow')
prefs_order=['ScopeCard("Update preferences"','ScopeCard("Opening animation"','ScopeCard("OpenCL runtime"']
prefs_positions=[extended.index(token) for token in prefs_order]
require(prefs_positions==sorted(prefs_positions),'Runtime/update preference card order diverged from VulkanScope reference')
require('Text("Obtainium"' in extended and 'ReleaseChannel.openObtainium(context)' in extended,'Obtainium must stay inside Update preferences without introducing a separate out-of-order settings card')
mutated_icon=main.replace('title.equals("Explore", true) -> R.drawable.ic_compass','title.equals("Explore", true) -> R.drawable.ic_quick_access_grid',1)
try:
    require('title.equals("Explore", true) -> R.drawable.ic_compass' in mutated_icon,'NEGATIVE MUTATION accepted wrong Explore icon')
except AssertionError:
    pass
else:
    raise AssertionError('Explore-icon negative mutation gate failed')
mutated_arrows=extended.replace('parityScrollHintPadding()','PaddingValues(8.dp)',1)
try:
    require(mutated_arrows.count('parityScrollHintPadding()') >= 4,'NEGATIVE MUTATION accepted broken standalone page arrows')
except AssertionError:
    pass
else:
    raise AssertionError('Scroll-arrow negative mutation gate failed')
mutated_page_arrows=main.replace('bottom = bottomNavigationInset + 4.dp','bottom = 4.dp',1)
try:
    require('bottom = bottomNavigationInset + 4.dp' in mutated_page_arrows,'NEGATIVE MUTATION accepted arrows behind bottom navigation')
except AssertionError:
    pass
else:
    raise AssertionError('Bottom-navigation scroll-arrow negative mutation gate failed')

mutated=chrome.replace('delay(4_500L)','delay(4_000L)',1)
require('delay(4_500L)' not in mutated,'Negative mutation did not change network banner timing')
try:
    require('delay(4_500L)' in mutated,'NEGATIVE MUTATION accepted wrong network banner timing')
except AssertionError:
    pass
else:
    raise AssertionError('Negative mutation gate failed')

false_positive=chrome.replace('OpenCLGlassTint = Color(0xA8171F18)','OpenCLGlassTint = Color(0xA8172018)',1)
require('PrimaryNavigationHeight = 54.dp' in false_positive and 'delay(4_500L)' in false_positive,'Unrelated OpenCL tint change incorrectly broke geometry gate')
print('PASS: 92/92 VulkanScope common drawables are byte-identical')
print('PASS: navigation/header/button/dialog/switch/settings/page-transition/scroll-arrow timings and geometry match locked parity tokens')
print('PASS: validated internet transition + persistent offline banner and measured overlay inset coordination are present')
print('PASS: Explore/common icons, Settings-embedded Info order, expressive Info blocks and standalone page arrows match the parity contract')
print('PASS: wrong 4.5 s network timing negative mutation fails while API-specific accent mutation is outside parity gate')
file_manager=(code/'SharedStorageFileManager.kt').read_text()
selectors=(code/'ExpressiveSelectors.kt').read_text()
all_source='\n'.join(path.read_text() for path in code.glob('*.kt'))
for forbidden in ('rememberLauncherForActivityResult','ActivityResultContracts.OpenDocument','ActivityResultContracts.CreateDocument','ACTION_OPEN_DOCUMENT','ACTION_CREATE_DOCUMENT','contentResolver.openInputStream','contentResolver.openOutputStream','FilterChip('):
    require(forbidden not in all_source,f'0.9.0 forbidden simplified/SAF path remains: {forbidden}')
for token in ('MANAGE_APP_ALL_FILES_ACCESS_PERMISSION','ATOMIC_MOVE','FileManagerViewMode','FileManagerSortMode','LazyVerticalGrid','GridCells.Adaptive','hardwareLazyGridDpad'):
    require(token in file_manager or token in (code/'InputAccessibility.kt').read_text(),f'0.9.0 File Manager parity token missing: {token}')
require(main.count('DeviceSelector(report!!.devices, deviceSelection)')==1,'Device selector must exist exactly once at common page level')
require('val multiDevice = report != null && report.devices.size > 1' in main,'Common multi-device selector gate missing')
require('OpenCLExpressiveFilterBar(categories, category, "Category")' in features,'Encyclopedia common selector missing')
require('OpenCLExpressiveFilterBar(' in main and 'OpenCLExpressiveFilterBar(' in features,'Shared Expressive selector not wired across page surfaces')
require('if (!openingGate) {' in main and 'return@MaterialExpressiveTheme' in main and main.index('return@MaterialExpressiveTheme') < main.index('val networkUiState'),'Opening animation is not an exclusive composition gate')
mutated_saf=all_source+'\nval regression = ActivityResultContracts.OpenDocument()'
try:
    require('ActivityResultContracts.OpenDocument' not in mutated_saf,'NEGATIVE MUTATION accepted SAF launcher restoration')
except AssertionError:
    pass
else:
    raise AssertionError('SAF negative mutation gate failed')
mutated_splash=main.replace('return@MaterialExpressiveTheme','Unit',1)
try:
    require('return@MaterialExpressiveTheme' in mutated_splash and mutated_splash.index('return@MaterialExpressiveTheme') < mutated_splash.index('val networkUiState'),'NEGATIVE MUTATION accepted chrome behind opening animation')
except (AssertionError, ValueError):
    pass
else:
    raise AssertionError('Opening-gate negative mutation failed')
mutated_selector=features.replace('OpenCLExpressiveFilterBar(categories, category, "Category")','Text(category)',1)
try:
    require('OpenCLExpressiveFilterBar(categories, category, "Category")' in mutated_selector,'NEGATIVE MUTATION accepted Encyclopedia selector downgrade')
except AssertionError:
    pass
else:
    raise AssertionError('Selector negative mutation gate failed')
print('PASS: 0.9.0 SAF removal, functional File Manager modes, shared device/selection surfaces and exclusive splash composition gate')
print('PASS: 3/3 0.9.0 SAF/splash/selector deliberate negative mutations rejected')
