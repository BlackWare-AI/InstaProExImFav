"""Validate and join the ZIP parts produced by split_apk.py. No APK transformation occurs."""
import hashlib,json,sys,zipfile
from pathlib import Path
folder=Path(sys.argv[1]);target=Path(sys.argv[2]);parts={};expected=None
for path in folder.glob('apk-part-*.zip'):
 with zipfile.ZipFile(path) as z:
  meta=json.loads(z.read('PART.json'));chunk=z.read('chunk.bin')
 if hashlib.sha256(chunk).hexdigest()!=meta['part_sha256']:raise ValueError('Corrupt part '+path.name)
 key=(meta['full_sha256'],meta['count'],meta['bytes'])
 if expected is None:expected=key
 if key!=expected:raise ValueError('Parts from different APKs')
 if meta['index'] in parts:raise ValueError('Duplicate part index')
 parts[meta['index']]=(meta,path)
if expected is None or set(parts)!=set(range(expected[1])):raise ValueError('Missing parts')
target.parent.mkdir(parents=True,exist_ok=True);temp=target.with_name(target.name+'.joining');digest=hashlib.sha256()
try:
 with temp.open('wb') as out:
  for i in range(expected[1]):
   with zipfile.ZipFile(parts[i][1]) as z:chunk=z.read('chunk.bin')
   digest.update(chunk);out.write(chunk)
 if digest.hexdigest()!=expected[0] or temp.stat().st_size!=expected[2]:raise ValueError('Joined APK hash/size mismatch')
 temp.replace(target)
except BaseException:
 temp.unlink(missing_ok=True);raise
print('Verified:',target,'SHA256',expected[0])
