package dev.instapro.patcher;
import java.io.*;
import java.util.*;
import org.jf.dexlib2.*;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.dexbacked.*;
import org.jf.dexlib2.immutable.*;
import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.dexlib2.writer.io.MemoryDataStore;
final class DexPatch {
 static final String TARGET="Lcom/OM7753/gold/BackUpFragment;";
 static String signature(Method m){return m.getName()+m.getParameterTypes().toString()+m.getReturnType();}
 static byte[] patch(byte[] original,byte[] payload)throws IOException {
  return patch(original,payload,2);
 }
 static byte[] patch(byte[] original,byte[] payload,int limit)throws IOException {
  if(limit<0||limit>2)throw new IOException("Hook stage");
  DexBackedDexFile old=new DexBackedDexFile(Opcodes.forApi(28),original), extra=new DexBackedDexFile(Opcodes.forApi(28),payload);
  Map<String,Method> replacements=new HashMap<>();List<ClassDef> classes=new ArrayList<>();Set<String> types=new HashSet<>();
  for(ClassDef c:extra.getClasses())if(c.getType().equals(TARGET)){for(Method m:c.getMethods())if(m.getName().equals("initPreference"))replacements.put(signature(m),m);}else if(c.getType().startsWith("Lcom/instagram/gpt/"))classes.add(c);else throw new IOException("Payload'da beklenmeyen sınıf: "+c.getType());
  if(replacements.size()!=2)throw new IOException("Payload yöntem sayısı");int changed=0;
  for(ClassDef c:old.getClasses()) {types.add(c.getType());if(!c.getType().equals(TARGET)){classes.add(c);continue;}
   List<Method> methods=new ArrayList<>();for(Method m:c.getMethods()){Method r=replacements.get(signature(m));if(r==null||changed>=limit){methods.add(m);continue;}if((m.getAccessFlags()&AccessFlags.NATIVE.getValue())==0||m.getImplementation()!=null)throw new IOException("Hook zaten değiştirilmiş");methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags()&~AccessFlags.NATIVE.getValue(),m.getAnnotations(),m.getHiddenApiRestrictions(),r.getImplementation()));changed++;}
   classes.add(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
  }
  if(changed!=limit)throw new IOException("Native hook eşleşmedi");for(ClassDef c:extra.getClasses())if(!c.getType().equals(TARGET)&&types.contains(c.getType()))throw new IOException("Çift payload sınıfı");
  DexPool pool=new DexPool(Opcodes.forApi(28));for(ClassDef c:classes)pool.internClass(c);MemoryDataStore out=new MemoryDataStore();pool.writeTo(out);byte[] result=Arrays.copyOf(out.getBuffer(),out.getSize());out.close();return result;
 }
}
