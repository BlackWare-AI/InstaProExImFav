import sys,pathlib,os,json,subprocess,hashlib
from build import ROOT,CACHE,B,run,javac
if '--host-only' not in sys.argv:
 E=pathlib.Path(sys.argv[1]) if len(sys.argv)>1 else ROOT.parent/'instapro/original-evidence'
 for item in json.loads((E/'FILES.json').read_text()):
  p=E/item['path'];assert p.stat().st_size==item['bytes'] and hashlib.sha256(p.read_bytes()).hexdigest()==item['sha256'],p
 print('PASS evidence hash/size checks',flush=True)
 cp=os.pathsep.join(map(str,[B/'appclasses',CACHE/'apksig.jar',CACHE/'dexlib2.jar',CACHE/'guava.jar',CACHE/'failureaccess.jar',CACHE/'jsr305.jar']))
 javac(B/'testclasses',list((ROOT/'tests').glob('CoreTest.java')),cp)
 run('java','-Xmx2g','-cp',str(B/'testclasses')+os.pathsep+cp,'dev.instapro.patcher.CoreTest',E,B)
# A generated workload matches the handoff counts; it is NOT the user's absent backup.
groups=[{'name':'Klasör '+str(i),'items':[]} for i in range(8)]
for i in range(16932):groups[i%8]['items'].append({'pageId':'fixture_'+str(i),'name':'Sentetik '+str(i)})
for i in range(683):groups[(i+1)%8]['items'].append({'pageId':'fixture_'+str(i),'name':'Sentetik '+str(i)})
fixture=B/'synthetic-favorites.json';fixture.write_text(json.dumps({'favorite_page_pref':json.dumps(groups,ensure_ascii=False)},ensure_ascii=False))
cp=os.pathsep.join(map(str,[CACHE/'json.jar',CACHE/'sqlite.jar',CACHE/'slf4j.jar']))
javac(B/'storetests',[ROOT/'payload/src/com/instagram/gpt/FavoritesDocument.java',ROOT/'payload/src/com/instagram/gpt/FavoritesStore.java',ROOT/'tests/FavoritesIntegrationTest.java'],cp)
run('java','-cp',str(B/'storetests')+os.pathsep+cp,'com.instagram.gpt.FavoritesIntegrationTest',fixture)
