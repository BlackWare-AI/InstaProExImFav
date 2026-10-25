import os,pathlib,secrets,sys,zipfile,struct
from build import ROOT,CACHE,B,run,javac
# Outside the repository. Never package, print or commit private material.
key=pathlib.Path(os.environ.get('PATCHER_RELEASE_KEYSTORE','/workspace/shared/instapro-release/identity.p12'))
password=pathlib.Path(os.environ.get('PATCHER_RELEASE_PASSWORD_FILE','/workspace/shared/instapro-release/password'))
alias=os.environ.get('PATCHER_RELEASE_ALIAS','patcher-release')
if not key.exists():
 if '--init-local-identity' not in sys.argv:raise SystemExit('Set PATCHER_RELEASE_KEYSTORE/PASSWORD_FILE/ALIAS, or explicitly use --init-local-identity.')
 key.parent.mkdir(parents=True,exist_ok=True);key.parent.chmod(0o700);password.write_text(secrets.token_urlsafe(36));password.chmod(0o600)
 run('keytool','-genkeypair','-keystore',key,'-storetype','PKCS12','-storepass:file',password,'-keypass:file',password,'-alias',alias,'-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=InstaPro Patcher Release, O=BlackWare-AI, C=TR')
 key.chmod(0o600)
# zipalign all STORED entries before signing, preserving compression methods.
aligned=B/'patcher-aligned.apk'
with zipfile.ZipFile(B/'patcher-unsigned.apk') as source,zipfile.ZipFile(aligned,'w') as target:
 for old in source.infolist():
  info=zipfile.ZipInfo(old.filename,old.date_time);info.compress_type=old.compress_type
  if info.compress_type==zipfile.ZIP_STORED:
   pad=(-(target.fp.tell()+30+len(info.filename.encode())))%4
   if pad:
    if pad<4:pad+=4
    info.extra=struct.pack('<HH',0xd935,pad-4)+bytes(pad-4)
  target.writestr(info,source.read(old))
javac(B/'signtool',[ROOT/'tools/SignRelease.java'],str(CACHE/'apksig.jar'))
run('java','-cp',str(B/'signtool')+os.pathsep+str(CACHE/'apksig.jar'),'SignRelease',key,password,alias,aligned,B/'InstaPro-Patcher-release.apk')
