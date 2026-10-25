import pathlib, urllib.request, zipfile, os, concurrent.futures, hashlib, json
ROOT=pathlib.Path(__file__).resolve().parents[1]
CACHE=ROOT/'.cache'; CACHE.mkdir(exist_ok=True)
DEPS={
'platform.zip':'https://dl.google.com/android/repository/platform-35_r02.zip',
'aapt2.jar':'https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/8.6.1-11315950/aapt2-8.6.1-11315950-linux.jar',
'r8.jar':'https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.6.27/r8-8.6.27.jar',
'apksig.jar':'https://dl.google.com/dl/android/maven2/com/android/tools/build/apksig/8.6.1/apksig-8.6.1.jar',
'dexlib2.jar':'https://repo.maven.apache.org/maven2/org/smali/dexlib2/2.5.2/dexlib2-2.5.2.jar',
'guava.jar':'https://repo.maven.apache.org/maven2/com/google/guava/guava/27.1-android/guava-27.1-android.jar',
'failureaccess.jar':'https://repo.maven.apache.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar',
'jsr305.jar':'https://repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar',
'json.jar':'https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar',
'sqlite.jar':'https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.45.3.0/sqlite-jdbc-3.45.3.0.jar',
'slf4j.jar':'https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar',
}
def get(item):
 name,url=item; p=CACHE/name
 if not p.exists():
  print('Download',name,flush=True); tmp=p.with_suffix('.tmp'); urllib.request.urlretrieve(url,tmp); tmp.rename(p)
 return name,hashlib.sha256(p.read_bytes()).hexdigest()
if __name__=='__main__':
 with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool: hashes=dict(pool.map(get,DEPS.items()))
 lock=ROOT/'tools/dependencies.sha256.json'
 if lock.exists():
  assert json.loads(lock.read_text())==hashes,'Dependency checksum mismatch'
 else: lock.write_text(json.dumps(hashes,indent=2)+'\n')
 with zipfile.ZipFile(CACHE/'platform.zip') as z:
  name=next(n for n in z.namelist() if n.endswith('/android.jar')); (CACHE/'android.jar').write_bytes(z.read(name))
 with zipfile.ZipFile(CACHE/'aapt2.jar') as z: (CACHE/'aapt2').write_bytes(z.read('aapt2'))
 (CACHE/'aapt2').chmod(0o755)
