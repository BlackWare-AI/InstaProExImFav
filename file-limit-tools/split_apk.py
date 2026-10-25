"""Split any APK into independently uploadable ZIP parts smaller than 20,000,000 bytes."""
import hashlib,json,sys,zipfile
from pathlib import Path
source=Path(sys.argv[1]);output=Path(sys.argv[2]);output.mkdir(parents=True,exist_ok=True)
chunk_size=19_000_000
full_hash=hashlib.sha256(source.read_bytes()).hexdigest();count=(source.stat().st_size+chunk_size-1)//chunk_size
with source.open('rb') as stream:
 for i in range(count):
  chunk=stream.read(chunk_size)
  meta={'name':source.name,'index':i,'count':count,'bytes':source.stat().st_size,'full_sha256':full_hash,'part_sha256':hashlib.sha256(chunk).hexdigest()}
  target=output/f'apk-part-{i+1:03d}-of-{count:03d}.zip'
  with zipfile.ZipFile(target,'w',zipfile.ZIP_STORED) as z:
   z.writestr('PART.json',json.dumps(meta));z.writestr('chunk.bin',chunk)
  assert target.stat().st_size<20_000_000
  print(target)
