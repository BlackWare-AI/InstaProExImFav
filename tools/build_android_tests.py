import os,zipfile
from build import ROOT,CACHE,B,run,javac,d8
javac(B/'androidtestclasses',[ROOT/'android-tests/SmokeRunner.java'],str(CACHE/'android.jar'))
d8(B/'androidtestdex',list((B/'androidtestclasses').rglob('*.class')))
unsigned=B/'android-tests-unsigned.apk'
run(CACHE/'aapt2','link','-I',CACHE/'android.jar','--manifest',ROOT/'android-tests/AndroidManifest.xml','--min-sdk-version','28','--target-sdk-version','35','-o',unsigned)
with zipfile.ZipFile(unsigned,'a',compression=zipfile.ZIP_DEFLATED) as z:
 for p in (B/'androidtestdex').glob('*.dex'):z.write(p,p.name)
# The test runner must share the target's certificate; no private material is packaged.
key=os.environ.get('PATCHER_RELEASE_KEYSTORE','/workspace/shared/instapro-release/identity.p12')
password=os.environ.get('PATCHER_RELEASE_PASSWORD_FILE','/workspace/shared/instapro-release/password')
alias=os.environ.get('PATCHER_RELEASE_ALIAS','patcher-release')
javac(B/'signtool',[ROOT/'tools/SignRelease.java'],str(CACHE/'apksig.jar'))
run('java','-cp',str(B/'signtool')+os.pathsep+str(CACHE/'apksig.jar'),'SignRelease',key,password,alias,unsigned,B/'Patcher-smoke-tests.apk')
