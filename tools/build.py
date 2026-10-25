#!/usr/bin/env python3
"""Linux release build. Does not require Gradle or desktop CLIs on the phone."""
import pathlib,subprocess,sys,zipfile,os,shutil
from deps import ROOT,CACHE
B=ROOT/'build'; B.mkdir(exist_ok=True)
def run(*args):
 subprocess.run([str(a) for a in args],cwd=ROOT,check=True)
def javac(dest,sources,cp):
 dest.mkdir(parents=True,exist_ok=True);run('java','com.sun.tools.javac.Main','-source','8','-target','8','-encoding','UTF-8','-cp',cp,'-d',dest,*sources)
def d8(dest,program,classpath=[]):
 dest.mkdir(parents=True,exist_ok=True);args=['java','-Xmx2g','-cp',str(CACHE/'r8.jar'),'com.android.tools.r8.D8','--release','--min-api','28','--lib',str(CACHE/'android.jar'),'--output',str(dest)]
 for c in classpath:args+=['--classpath',str(c)]
 run(*args,*program)
if __name__=='__main__':
 run(sys.executable,ROOT/'tools/deps.py')
 for d in ['stubclasses','payloadclasses','appclasses','payloaddex','appdex']:
  shutil.rmtree(B/d,ignore_errors=True)
 javac(B/'stubclasses',[ROOT/'payload/stub/com/OM7753/gold/GOLD.java'],str(CACHE/'android.jar'))
 javac(B/'payloadclasses',list((ROOT/'payload/src').rglob('*.java'))+[ROOT/'payload/stub/com/OM7753/gold/BackUpFragment.java'],os.pathsep.join(map(str,[CACHE/'android.jar',B/'stubclasses'])))
 d8(B/'payloaddex',list((B/'payloadclasses').rglob('*.class')),[B/'stubclasses'])
 libs=[CACHE/(s+'.jar') for s in ['apksig','dexlib2','guava','failureaccess','jsr305']]
 javac(B/'appclasses',list((ROOT/'app/src/main/java').rglob('*.java')),os.pathsep.join(map(str,[CACHE/'android.jar']+libs)))
 d8(B/'appdex',list((B/'appclasses').rglob('*.class'))+libs)
 unsigned=B/'patcher-unsigned.apk'
 run(CACHE/'aapt2','link','-I',CACHE/'android.jar','--manifest',ROOT/'app/src/main/AndroidManifest.xml','--min-sdk-version','28','--target-sdk-version','35','-o',unsigned)
 with zipfile.ZipFile(unsigned,'a',compression=zipfile.ZIP_DEFLATED) as z:
  for p in (B/'appdex').glob('*.dex'):z.write(p,p.name)
  z.write(B/'payloaddex/classes.dex','assets/favorites.dex')
  z.write(ROOT/'THIRD_PARTY_NOTICES.txt','assets/THIRD_PARTY_NOTICES.txt')
 print('Unsigned release:',unsigned)
