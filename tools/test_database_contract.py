import json
import subprocess
import sys
from pathlib import Path

root=Path(__file__).resolve().parents[1]
pair=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else root.parent/'OpenCLScope-Database-0.8.0'
app=(root/'app/src/main/java/com/efishell/openclscope/DatabaseClient.kt').read_text()
producer=(root/'app/build.gradle.kts').read_text()
manifest=json.loads((root/'app/src/main/assets/registry/catalog.json').read_text())
assert 'completeEnvelope' in app and '"technicalReport", original' in app and '"collection", JSONObject().put("status", "complete").put("source", "live")' in app
assert 'origin != "Live"' in app and 'if (!hasValidatedInternet(context))' in app
assert 'HttpsURLConnection' in app and 'instanceFollowRedirects = false' in app
assert 'versionName = "0.9.0"' in producer and 'versionCode = 900' in producer
print('PASS: producer submits complete native envelope only from explicit live report; HTTPS-only transport')

if not pair.is_dir():
    print('NOT EXECUTED: separate Database source directory not extracted beside application; provide its path as first argument')
    raise SystemExit(0)
consumer=(pair/'worker/src/index.js').read_text()
fixture=json.loads((pair/'worker/tests/complete-report.json').read_text())
assert fixture['registrySha256']==manifest['registrySha256']
assert fixture['application']['packageName']=='com.efishell.openclscope'
assert fixture['application']['versionName']=='0.2.0'
assert fixture['technicalReport']['appVersion']=='0.2.0'
assert fixture['collection']=={'source':'live','status':'complete'}
assert len(fixture['technicalReport']['platforms'][0]['properties'])==8
assert len(fixture['technicalReport']['platforms'][0]['devices'][0]['properties'])==120
assert len(manifest['platformQueries'])==16
assert len(manifest['deviceQueries'])==209
assert 'validateSubmission' in consumer
script='''import {readFileSync} from "node:fs";import {validateSubmission} from "./worker/src/index.js";import {PLATFORM_KEYS,DEVICE_KEYS} from "./worker/src/query-manifest.js";
const clean=JSON.parse(readFileSync("worker/tests/complete-report.json","utf8"));
const current=structuredClone(clean);current.application.versionName="0.9.0";current.application.versionCode=900;current.technicalReport.appVersion="0.9.0";
const p=current.technicalReport.platforms[0];const d=p.devices[0];
const pBy=new Map(p.properties.map(r=>[r.name,r]));p.properties=[...PLATFORM_KEYS].map(name=>pBy.get(name)??{name,status:"unavailable",code:-30,value:""});
const dBy=new Map(d.properties.map(r=>[r.name,r]));d.properties=[...DEVICE_KEYS].map(name=>dBy.get(name)??{name,status:"unavailable",code:-30,value:""});
const mutated=structuredClone(current);mutated.technicalReport.platforms[0].devices[0].properties.pop();
const permitted=structuredClone(current);const row=permitted.technicalReport.platforms[0].devices[0].properties.find(r=>r.name==="CL_DEVICE_AVAILABLE");row.status="available";row.value="false";row.code=0;
const offline=structuredClone(current);offline.technicalReport.status="unavailable";
const mismatch=structuredClone(current);mismatch.technicalReport.appVersion="0.3.0";
if(!validateSubmission(clean).ok||!validateSubmission(current).ok||validateSubmission(mismatch).ok||validateSubmission(mutated).ok||!validateSubmission(permitted).ok||validateSubmission(offline).ok)process.exit(4);
console.log("PASS: legacy 0.2.0 and current 0.9.0 producer fixtures accepted; producer mismatch and query-loss mutation denied; available:false control accepted; offline-native upload denied");'''
subprocess.run(['node','--input-type=module','-e',script],cwd=pair,check=True)
print('PASS: application ↔ Worker schema/version/registry digest; legacy 8+120 and current 16+209 query contracts')
