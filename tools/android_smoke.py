#!/usr/bin/env python3
"""Run the actual signed release on Android; do not infer success from am start alone."""
import argparse,hashlib,json,pathlib,subprocess,time,re,shutil
p=argparse.ArgumentParser();p.add_argument('--apk',type=pathlib.Path,required=True);p.add_argument('--serial',required=True);p.add_argument('--adb',default=shutil.which('adb'));p.add_argument('--out',type=pathlib.Path,required=True);a=p.parse_args()
if not a.adb:raise SystemExit('adb is required')
a.out.mkdir(parents=True,exist_ok=True);package='dev.instapro.patcher';report={'sha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'package':package,'device':{},'launches':[]};known_pids=set()
def adb(*args,timeout=120,binary=False):
 r=subprocess.run([a.adb,'-s',a.serial,*args],capture_output=True,text=not binary,timeout=timeout)
 if r.returncode:raise RuntimeError(str(args)+': '+str(r.stderr))
 return r.stdout
try:
 for key in ['ro.product.model','ro.build.version.release','ro.build.version.sdk','ro.product.cpu.abilist']:report['device'][key]=adb('shell','getprop',key).strip()
 adb('install','-r',str(a.apk),timeout=240)
 installed=adb('shell','dumpsys','package',package);report['installed_flags']=[s.strip() for s in installed.splitlines() if re.search(r'(?:pkgFlags|flags)=\[',s)]
 assert report['installed_flags'] and all('DEBUGGABLE' not in s for s in report['installed_flags']),'Installed target is debuggable'
 for index in range(3):
  adb('shell','am','force-stop',package)
  start=adb('shell','am','start','-W','-n',package+'/.StartupActivity');entry={'start':start,'samples':[]};report['launches'].append(entry)
  assert 'Error' not in start and 'Exception' not in start,start
  for delay in [2,5]:
   time.sleep(delay)
   # pidof returns 1 when the process has already crashed.
   r=subprocess.run([a.adb,'-s',a.serial,'shell','pidof',package],capture_output=True,text=True,timeout=30)
   pid=r.stdout.strip();entry['samples'].append({'pid':pid});known_pids.update(pid.split())
   assert r.returncode==0 and pid,'Process died after launch '+str(index+1)
   activities=adb('shell','dumpsys','activity','activities')
   resumed=[s.strip() for s in activities.splitlines() if 'ResumedActivity' in s and package+'/' in s]
   entry['samples'][-1]['resumed']=resumed
   assert resumed,'Patcher is not the resumed Activity'
 adb('shell','uiautomator','dump','/sdcard/patcher-smoke-ui.xml')
 ui=adb('shell','cat','/sdcard/patcher-smoke-ui.xml');(a.out/'ui.xml').write_text(ui)
 assert 'InstaPro Patcher' in ui and 'Paket: com.instagram.android.gpt' in ui,'Initial form/preview not visible'
 (a.out/'screen.png').write_bytes(adb('exec-out','screencap','-p',binary=True))
 report['status']='PASS: three release cold starts, process survival and resumed Activity; default form/preview; debuggable=false'
 print(report['status'])
except BaseException as error:
 report['status']='FAIL: '+repr(error);print(report['status']);raise
finally:
 try:
  crash=adb('logcat','-d','-b','crash','-v','threadtime')
  lines=crash.splitlines()
  for line in lines:
   if 'Process: '+package+',' in line and len(line.split())>3:known_pids.add(line.split()[2])
  own=[line for line in lines if len(line.split())>3 and line.split()[2] in known_pids]
  (a.out/'patcher-crash.txt').write_text('\n'.join(own)+'\n')
 except Exception as error:report['log_error']=repr(error)
 (a.out/'result.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n')
