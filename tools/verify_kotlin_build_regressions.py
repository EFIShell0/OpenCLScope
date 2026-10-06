from pathlib import Path

root = Path(__file__).resolve().parents[1]
code = root / 'app/src/main/java/com/efishell/openclscope'
files = {
    'ExtendedPages.kt': (code / 'ExtendedPages.kt').read_text(),
    'InputAccessibility.kt': (code / 'InputAccessibility.kt').read_text(),
    'MainActivity.kt': (code / 'MainActivity.kt').read_text(),
    'ParityFeatures.kt': (code / 'ParityFeatures.kt').read_text(),
}


def violations(source):
    errors = []
    extended = source['ExtendedPages.kt']
    accessibility = source['InputAccessibility.kt']
    main = source['MainActivity.kt']
    parity = source['ParityFeatures.kt']
    forbidden = [
        'import androidx.compose.foundation.layout.matchParentSize',
        'import androidx.compose.foundation.layout.weight',
        'import androidx.compose.ui.input.pointer.consume',
        'import androidx.compose.ui.input.key.nativeKeyEvent',
    ]
    all_text = '\n'.join(source.values())
    for item in forbidden:
        if item in all_text:
            errors.append('forbidden scoped/member import: ' + item)
    if 'import androidx.compose.ui.input.key.key' not in accessibility:
        errors.append('InputAccessibility key extension import missing')
    if 'import androidx.compose.ui.input.key.nativeKeyCode' not in accessibility:
        errors.append('InputAccessibility nativeKeyCode import missing')
    if 'event.key.nativeKeyCode' not in accessibility:
        errors.append('D-pad key dispatch missing')
    if 'event.nativeKeyEvent' in main:
        errors.append('unsupported nativeKeyEvent access restored')
    if 'icon = { Icon(painterResource(R.drawable.ic_shield)' in main:
        errors.append('AlertDialog wrapper received unsupported icon parameter')
    if 'OpenCLQrCode(publicUrl, Modifier.size(190.dp).align(Alignment.CenterHorizontally))' in main:
        errors.append('QR align called outside parent scope')
    if 'Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center)' not in main:
        errors.append('scope-safe centered QR container missing')
    if 'Modifier.matchParentSize()' not in extended or 'Modifier.matchParentSize()' not in parity:
        errors.append('scroll boundary overlay geometry unexpectedly removed')
    if 'onRefreshRuntime = { scope.launch { collect() } },' not in main:
        errors.append('runtime refresh must launch suspend collector from coroutine scope')
    if 'onRefreshRuntime = { collect() },' in main:
        errors.append('suspend collector called directly from non-suspend refresh callback')
    return errors


errors = violations(files)
if errors:
    raise SystemExit('\n'.join(errors))

mutations = []
mutated = dict(files)
mutated['ExtendedPages.kt'] = 'import androidx.compose.foundation.layout.matchParentSize\n' + mutated['ExtendedPages.kt']
mutations.append(('matchParentSize import', mutated))
mutated = dict(files)
mutated['InputAccessibility.kt'] = mutated['InputAccessibility.kt'].replace('import androidx.compose.ui.input.key.key\n', '')
mutations.append(('missing key import', mutated))
mutated = dict(files)
mutated['MainActivity.kt'] = 'import androidx.compose.ui.input.key.nativeKeyEvent\n' + mutated['MainActivity.kt']
mutations.append(('nativeKeyEvent import', mutated))
mutated = dict(files)
mutated['MainActivity.kt'] = mutated['MainActivity.kt'].replace(
    'OpenCLQrCode(publicUrl, Modifier.size(190.dp))',
    'OpenCLQrCode(publicUrl, Modifier.size(190.dp).align(Alignment.CenterHorizontally))',
)
mutations.append(('out-of-scope align', mutated))
mutated = dict(files)
mutated['MainActivity.kt'] = mutated['MainActivity.kt'].replace(
    'title = {\n                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {',
    'icon = { Icon(painterResource(R.drawable.ic_shield), null, tint = Accent) },\n            title = {\n                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {',
)
mutations.append(('unsupported dialog icon parameter', mutated))
mutated = dict(files)
mutated['MainActivity.kt'] = mutated['MainActivity.kt'].replace(
    'onRefreshRuntime = { scope.launch { collect() } },',
    'onRefreshRuntime = { collect() },',
)
mutations.append(('direct suspend collect callback', mutated))

for name, mutation in mutations:
    if not violations(mutation):
        raise SystemExit('NEGATIVE MUTATION NOT DETECTED: ' + name)

print('PASS: Kotlin build-regression gate covers scoped imports, key events, dialog signature, QR alignment and runtime-refresh coroutine boundary')
print(f'PASS: {len(mutations)}/{len(mutations)} deliberate compile-regression mutations rejected')
