import pathlib,struct,sys,json,hashlib
root=pathlib.Path(sys.argv[1]);data=(root/'lib/arm64-v8a/libProMods.so').read_bytes()
assert data[:6]==b'\x7fELF\x02\x01' and struct.unpack_from('<H',data,18)[0]==183
phoff=struct.unpack_from('<Q',data,32)[0];entsize,count=struct.unpack_from('<HH',data,54);segments=[]
for i in range(count):
 p=phoff+i*entsize;t,flags,off,va,pa,filesz,memsz,align=struct.unpack_from('<IIQQQQQQ',data,p)
 if t==1:segments.append((off,va,filesz))
def offset(va):
 matches=[off+va-start for off,start,size in segments if start<=va<start+size];assert len(matches)==1;return matches[0]
p=offset(0xB8A0E);array=struct.unpack_from('<1831H',data,p)
for name,(start,length,key) in {'database':(143,12,3134),'pages':(155,164,1824),'categories':(319,89,1577),'memberships':(408,126,1313),'seed':(581,9,2087)}.items():
 text=''.join(chr(v^key) for v in array[start:start+length]);print(name+': '+text)
entries=json.loads((root/'APK_INVENTORY.json').read_text());print('Inventory:',len(entries),'entries,',sum(e['bytes'] for e in entries),'uncompressed bytes')
print('Evidence contains no runtime crash logs. Startup crash cause remains UNKNOWN.')
