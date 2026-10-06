import json
import os
import subprocess
import tempfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = root / 'app/src/main/cpp/openclscope.cpp'
mock = root / 'tools/native_mock.cpp'
runner = root / 'tools/native_runner.cpp'
java = Path('/usr/lib/jvm/java-21-openjdk-amd64/include')
if not java.exists():
    java = next(Path('/usr/lib/jvm').glob('*/include'))
with tempfile.TemporaryDirectory() as directory:
    tmp = Path(directory)
    output = tmp / 'native_runner'
    flags = ['-std=c++20','-DOPENCLSCOPE_HOST_TEST','-Wall','-Wextra','-Werror','-I'+str(java),'-I'+str(java/'linux')]
    subprocess.run(['g++',*flags,str(source),str(runner),'-ldl','-o',str(output)], check=True)
    def execute(path=None, fallback=None):
        env = dict(os.environ)
        env.pop('OPENCLSCOPE_TEST_LIBRARY',None)
        env.pop('OPENCLSCOPE_TEST_LIBRARY_FALLBACK',None)
        env.pop('OPENCLSCOPE_TEST_ICD_DIR',None)
        if path: env['OPENCLSCOPE_TEST_LIBRARY']=str(path)
        if fallback: env['OPENCLSCOPE_TEST_LIBRARY_FALLBACK']=str(fallback)
        return json.loads(subprocess.check_output([str(output)],env=env,text=True))
    absent=execute()
    assert absent['status']=='unavailable' and absent['platforms']==[]
    print('PASS: no loader is unavailable, not falsely unsupported')
    lib=tmp/'libOpenCL_mock.so'
    subprocess.run(['g++','-std=c++20','-shared','-fPIC','-Wall','-Wextra','-Werror',str(mock),'-o',str(lib)],check=True)
    data=execute(lib)
    assert data['status']=='ok' and len(data['platforms'])==1
    device=data['platforms'][0]['devices'][0]
    assert device['name']=='Mock OpenCL GPU' and device['type']=='GPU'
    assert len(data['platforms'][0]['properties'])==16 and len(device['properties'])==209
    assert device['driverVersion']=='Mock Driver 0.1 – Ünïcøde 😀'
    assert device['extensions']==['cl_khr_fp16','cl_khr_int64_base_atomics']
    assert next(p for p in device['properties'] if p['name']=='CL_DEVICE_MAX_COMPUTE_UNITS')['value']=='6'
    assert next(p for p in device['properties'] if p['name']=='CL_DEVICE_NUMERIC_VERSION')['status']=='unavailable'
    assert next(p for p in device['properties'] if p['name']=='CL_DEVICE_UUID')['status']=='not_applicable'
    assert next(p for p in device['properties'] if p['name']=='CL_DEVICE_FEATURE_CAPABILITIES_INTEL')['status']=='not_applicable'
    assert next(p for p in data['platforms'][0]['properties'] if p['name']=='CL_PLATFORM_SEMAPHORE_TYPES_KHR')['status']=='not_applicable'
    assert len(device['imageFormats'])==18
    image=next(p for p in device['imageFormats'] if p['imageType']=='CL_MEM_OBJECT_IMAGE2D' and p['flags']=='CL_MEM_READ_ONLY')
    assert [(f['order'],f['dataType']) for f in image['formats']]==[('CL_RGBA','CL_UNORM_INT8'),('CL_BGRA','CL_UNORM_INT8')]
    assert data['durationMs']>=0
    print('PASS: live 16 platform/209 device registry queries, UTF-8 roundtrip, extensions, explicit unknown states and 18 image queries')
    no_platform=tmp/'libOpenCL_none.so'
    subprocess.run(['g++','-std=c++20','-DNO_PLATFORM','-shared','-fPIC','-Wall','-Wextra','-Werror',str(mock),'-o',str(no_platform)],check=True)
    data=execute(no_platform)
    assert data['status']=='unavailable' and data['errorCode']==-1001
    print('PASS: CL_PLATFORM_NOT_FOUND_KHR handled without fabricating data')
    no_symbol=tmp/'libOpenCL_missing.so'
    subprocess.run(['g++','-std=c++20','-DNO_DEVICEINFO','-shared','-fPIC','-Wall','-Wextra','-Werror',str(mock),'-o',str(no_symbol)],check=True)
    data=execute(no_symbol)
    assert data['status']=='unavailable' and data['errorCode']==-2
    print('PASS: missing required OpenCL entry point is unavailable')
    fallback_data=execute(no_symbol, lib)
    assert fallback_data['status']=='ok' and fallback_data['loader']==str(lib)
    print('PASS: loader discovery skips a visible non-OpenCL carrier and continues to a valid runtime')
    icd_dir=tmp/'icd-vendors'
    icd_dir.mkdir()
    (icd_dir/'mock.icd').write_text(str(lib)+'\n')
    env=dict(os.environ)
    env.pop('OPENCLSCOPE_TEST_LIBRARY',None)
    env.pop('OPENCLSCOPE_TEST_LIBRARY_FALLBACK',None)
    env['OPENCLSCOPE_TEST_ICD_DIR']=str(icd_dir)
    icd_data=json.loads(subprocess.check_output([str(output)],env=env,text=True))
    assert icd_data['status']=='ok' and icd_data['loader']==str(lib)
    unsafe_dir=tmp/'icd-unsafe'
    unsafe_dir.mkdir()
    (unsafe_dir/'unsafe.icd').write_text('../'+lib.name+'\n')
    env['OPENCLSCOPE_TEST_ICD_DIR']=str(unsafe_dir)
    unsafe_data=json.loads(subprocess.check_output([str(output)],env=env,text=True))
    assert unsafe_data['status']=='unavailable'
    print('PASS: bounded Android ICD vendor-file discovery accepts a valid runtime and rejects traversal paths')
    original_for_loader=source.read_text()
    fallback_signature='if (bind(candidate, path)) return true;\n        dlclose(candidate);\n        return false;'
    assert original_for_loader.count(fallback_signature)==1
    loader_mutation=tmp/'mutated_loader.cpp'
    loader_mutation.write_text(original_for_loader.replace(fallback_signature,'if (bind(candidate, path)) return true;\n        handle = candidate;\n        return true;'))
    loader_mutant=tmp/'mutated_loader_runner'
    subprocess.run(['g++',*flags,'-I'+str(source.parent),str(loader_mutation),str(runner),'-ldl','-o',str(loader_mutant)],check=True)
    env=dict(os.environ)
    env['OPENCLSCOPE_TEST_LIBRARY']=str(no_symbol)
    env['OPENCLSCOPE_TEST_LIBRARY_FALLBACK']=str(lib)
    loader_bad=json.loads(subprocess.check_output([str(loader_mutant)],env=env,text=True))
    assert loader_bad['status']!='ok'
    env['OPENCLSCOPE_TEST_LIBRARY']=str(lib)
    env.pop('OPENCLSCOPE_TEST_LIBRARY_FALLBACK',None)
    loader_control=json.loads(subprocess.check_output([str(loader_mutant)],env=env,text=True))
    assert loader_control['status']=='ok'
    print('PASS: negative loader mutation reproduces first-opened-library trap; valid first library remains false-positive control')
    exact_device_type_signature='constexpr CLULong kAllDevices = 0xFFFFFFFFull;'
    assert original_for_loader.count(exact_device_type_signature)==1
    device_type_mutation=tmp/'mutated_device_type.cpp'
    device_type_mutation.write_text(original_for_loader.replace(exact_device_type_signature,'constexpr CLULong kAllDevices = ~CLULong{0};'))
    device_type_mutant=tmp/'mutated_device_type_runner'
    subprocess.run(['g++',*flags,'-I'+str(source.parent),str(device_type_mutation),str(runner),'-ldl','-o',str(device_type_mutant)],check=True)
    env=dict(os.environ);env['OPENCLSCOPE_TEST_LIBRARY']=str(lib);env.pop('OPENCLSCOPE_TEST_LIBRARY_FALLBACK',None)
    wrong_type=json.loads(subprocess.check_output([str(device_type_mutant)],env=env,text=True))
    assert wrong_type['status']=='ok' and wrong_type['platforms'][0]['deviceEnumerationStatus']=='error' and wrong_type['platforms'][0]['deviceEnumerationCode']==-31
    exact_type_control=execute(lib)
    assert exact_type_control['platforms'][0]['deviceEnumerationStatus']=='available' and len(exact_type_control['platforms'][0]['devices'])==1
    print('PASS: negative CL_DEVICE_TYPE_ALL mutation reproduces CL_INVALID_DEVICE_TYPE; exact 0xFFFFFFFF registry token enumerates the device')
    applicability_signature='if (queryApplicable(definition, coreVersion, runtimeExtensions)) output += property(definition, query(getter, handle, definition.id));'
    assert original_for_loader.count(applicability_signature)==1
    applicability_source=tmp/'mutated_applicability.cpp'
    applicability_source.write_text(original_for_loader.replace(applicability_signature,'if (true) output += property(definition, query(getter, handle, definition.id));'))
    applicability_runner=tmp/'mutated_applicability_runner'
    subprocess.run(['g++',*[f for f in flags if f != '-Werror'],'-I'+str(source.parent),str(applicability_source),str(runner),'-ldl','-o',str(applicability_runner)],check=True)
    env=dict(os.environ);env['OPENCLSCOPE_TEST_LIBRARY']=str(lib);env.pop('OPENCLSCOPE_TEST_LIBRARY_FALLBACK',None)
    applicability_bad=json.loads(subprocess.check_output([str(applicability_runner)],env=env,text=True))
    bad_device=applicability_bad['platforms'][0]['devices'][0]
    assert next(p for p in bad_device['properties'] if p['name']=='CL_DEVICE_FEATURE_CAPABILITIES_INTEL')['status']=='unavailable'
    control_device=execute(lib)['platforms'][0]['devices'][0]
    assert next(p for p in control_device['properties'] if p['name']=='CL_DEVICE_FEATURE_CAPABILITIES_INTEL')['status']=='not_applicable'
    print('PASS: extension/core applicability gate avoids invalid queries; deliberate unconditional-query mutation is detected')
    oversized=tmp/'libOpenCL_excess.so'
    subprocess.run(['g++','-std=c++20','-DOVERSIZE_PLATFORM_COUNT','-shared','-fPIC','-Wall','-Wextra','-Werror',str(mock),'-o',str(oversized)],check=True)
    data=execute(oversized)
    assert data['status']=='error' and data['errorCode']==-100002
    oversized_devices=tmp/'libOpenCL_excess_devices.so'
    subprocess.run(['g++','-std=c++20','-DOVERSIZE_DEVICE_COUNT','-shared','-fPIC','-Wall','-Wextra','-Werror',str(mock),'-o',str(oversized_devices)],check=True)
    data=execute(oversized_devices)
    assert data['status']=='ok' and data['platforms'][0]['deviceEnumerationStatus']=='error' and data['platforms'][0]['deviceEnumerationCode']==-100009
    oversized_info=tmp/'libOpenCL_excess_info.so'
    subprocess.run(['g++','-std=c++20','-DOVERSIZE_INFO','-shared','-fPIC','-Wall','-Wextra','-Werror',str(mock),'-o',str(oversized_info)],check=True)
    data=execute(oversized_info)
    name=next(p for p in data['platforms'][0]['devices'][0]['properties'] if p['name']=='CL_DEVICE_NAME')
    assert name['status']=='error' and name['code']==-100000
    print('PASS: platform/device/field safety bounds fail explicitly, never silently truncate')
    mutated_source=tmp/'mutated.cpp'
    original=source.read_text()
    signature='if (err == -1001 || (err == 0 && platformCount == 0))'
    assert original.count(signature)==1
    mutated_source.write_text(original.replace(signature,'if (err == -1002 || (err == 0 && platformCount == 0))'))
    mutated_exe=tmp/'mutated_runner'
    subprocess.run(['g++',*flags,'-I'+str(source.parent),str(mutated_source),str(runner),'-ldl','-o',str(mutated_exe)],check=True)
    env=dict(os.environ)
    env['OPENCLSCOPE_TEST_LIBRARY']=str(no_platform)
    mutant=json.loads(subprocess.check_output([str(mutated_exe)],env=env,text=True))
    assert mutant['status']!='unavailable'
    env['OPENCLSCOPE_TEST_LIBRARY']=str(lib)
    control=json.loads(subprocess.check_output([str(mutated_exe)],env=env,text=True))
    assert control['status']=='ok'
    print('PASS: negative mutation fails no-platform state gate; valid loader is false-positive control')
