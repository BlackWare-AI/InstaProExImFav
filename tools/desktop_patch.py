"""Build diagnostic baseline / full stage on desktop using the SAME engine as phone.
Full APK and original SHA required. This does not execute Android.
"""
import sys,os,pathlib
from build import ROOT,CACHE,B,run,javac
if len(sys.argv)!=6:raise SystemExit('desktop_patch.py original.apk output.apk name .suffix baseline|activity|hook1|favorites')
cp=os.pathsep.join(map(str,[B/'appclasses']+[CACHE/(s+'.jar') for s in ['apksig','dexlib2','guava','failureaccess','jsr305']]))
javac(B/'desktop',[ROOT/'tools/DesktopPatch.java'],cp)
run('java','-Xmx2g','-cp',str(B/'desktop')+os.pathsep+cp,'dev.instapro.patcher.DesktopPatch',*sys.argv[1:],B/'payloaddex/classes.dex')
