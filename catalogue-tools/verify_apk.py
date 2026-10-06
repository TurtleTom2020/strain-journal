import hashlib,json,os,pathlib,subprocess,sys,tempfile
project=pathlib.Path(sys.argv[1]);sdk=pathlib.Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')));tools=sdk/'build-tools/35.0.0';apk=project/'app/build/outputs/apk/debug/app-debug.apk'
badging=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(apk)],text=True).splitlines()[0]
assert "name='com.medleaf.journal'" in badging and "versionCode='60'" in badging and "versionName='0.55.2'" in badging,badging
signing=subprocess.check_output([str(tools/'apksigner'),'verify','--print-certs',str(apk)],text=True)
with tempfile.TemporaryDirectory() as tmp:
 cert=pathlib.Path(tmp)/'cert.der'
 subprocess.run(['keytool','-exportcert','-keystore',str(project/'app/stable-debug.keystore'),'-storepass','medleaf-testing','-alias','medleaf-debug','-file',str(cert)],check=True,capture_output=True)
 digest=hashlib.sha256(cert.read_bytes()).hexdigest();assert digest.lower() in signing.lower(),signing
report={'package':'com.medleaf.journal','versionCode':60,'versionName':'0.55.2','certificateSha256':digest,'sameStableSigningKey':True,'apkSha256':hashlib.sha256(apk.read_bytes()).hexdigest()}
(project/'apk-upgrade-verification.json').write_text(json.dumps(report,indent=2));print(json.dumps(report))
