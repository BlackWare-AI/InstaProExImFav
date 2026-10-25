#!/usr/bin/env python3
"""Capture evidence on a connected authorized Android device. No debuggable flag change.
Raw logs stay local: review/redact before sharing. Exit 2 means launch is unverified.
"""
import argparse,pathlib,subprocess,json,hashlib,time,re,sys
p=argparse.ArgumentParser();p.add_argument('apk',type=pathlib.Path);p.add_argument('package');p.add_argument('--serial',required=True);p.add_argument('--out',type=pathlib.Path,required=True);p.add_argument('--install',action='store_true');a=p.parse_args()
assert re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+',a.package)
a.out.mkdir(parents=True,exist_ok=True);base=['adb','-s',a.serial]
def adb(*cmd):
 r=subprocess.run(base+list(cmd),capture_output=True,text=True);return {'command':cmd,'returncode':r.returncode,'stdout':r.stdout,'stderr':r.stderr}
record={'apk_sha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'package':a.package,'serial':a.serial,'device':{}}
for key in ['ro.product.model','ro.build.version.release','ro.build.version.sdk','ro.product.cpu.abilist']:record['device'][key]=adb('shell','getprop',key)
if a.install:
 record['install']=adb('install','-r',str(a.apk));
 if record['install']['returncode']!=0:(a.out/'install.json').write_text(json.dumps(record,indent=2));sys.exit(2)
# Do not clear global logs; collect a timestamped window instead.
stamp=adb('shell','date','+%m-%d %H:%M:%S.000')['stdout'].strip()
record['force_stop']=adb('shell','am','force-stop',a.package);record['launch']=adb('shell','monkey','-p',a.package,'-c','android.intent.category.LAUNCHER','1')
record['samples']=[]
for delay in [2,5,8]:
 time.sleep(delay);record['samples'].append({'pid':adb('shell','pidof',a.package),'activities':adb('shell','dumpsys','activity','activities')})
for name,cmd in {'startup-logcat.txt':['logcat','-d','-b','all','-v','threadtime','-T',stamp],'crash-buffer.txt':['logcat','-d','-b','crash','-v','threadtime','-T',stamp],'installed-package.txt':['shell','dumpsys','package',a.package]}.items():
 r=adb(*cmd);(a.out/name).write_text(r['stdout']+'\n'+r['stderr'])
record['verdict']='UNVERIFIED: inspect process/resumed Activity/crash evidence and test Backup/SAF/import/export manually.'
(a.out/'run.json').write_text(json.dumps(record,ensure_ascii=False,indent=2));print(record['verdict']);sys.exit(2)
