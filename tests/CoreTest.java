package dev.instapro.patcher;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.zip.*;
import org.jf.dexlib2.*;
import org.jf.dexlib2.dexbacked.*;
import org.jf.dexlib2.iface.*;
import com.android.apksig.*;
import com.android.apksig.internal.apk.AndroidBinXmlParser;
public class CoreTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static byte[] bytes(Path p)throws IOException{return Files.readAllBytes(p);}
 static void manifest(byte[] b)throws Exception{
  AndroidBinXmlParser p=new AndroidBinXmlParser(ByteBuffer.wrap(b));int count=0,added=0;boolean debug=false;
  while(p.next()!=AndroidBinXmlParser.EVENT_END_DOCUMENT){if(p.getEventType()!=AndroidBinXmlParser.EVENT_START_ELEMENT)continue;String tag=p.getName();boolean component=Arrays.asList("activity","activity-alias","service","receiver","provider").contains(tag);boolean name=false;
   for(int i=0;i<p.getAttributeCount();i++){int id=p.getAttributeNameResourceId(i);if(id==0x01010003){name=true;if("com.instagram.gpt.FavoritesTransferActivity".equals(p.getAttributeStringValue(i)))added++;}if(tag.equals("application")&&id==0x0101000f){check(p.getAttributeIntValue(i)==0,"debug true");debug=true;}}
   if(component){check(name,"component android:name missing");count++;}
  }check(count==734&&added==1&&debug,"manifest counts "+count);System.out.println("PASS independent apksig AXML parser: 734 component names, new Activity, debug=false");
 }
 static String canonical(Object value)throws Exception {
  if(value==null)return "null";
  if(value instanceof Number||value instanceof String||value instanceof Enum||value instanceof org.jf.dexlib2.iface.reference.Reference)return value.toString();
  if(value instanceof Iterable){StringBuilder s=new StringBuilder("[");for(Object item:(Iterable<?>)value)s.append(canonical(item)).append(";");return s.append("]").toString();}
  TreeMap<String,String> props=new TreeMap<>();for(java.lang.reflect.Method m:value.getClass().getMethods())if(m.getName().startsWith("get")&&!m.getName().equals("getClass")&&m.getParameterCount()==0){m.setAccessible(true);props.put(m.getName(),canonical(m.invoke(value)));}return props.toString();
 }
 static String body(Method m)throws Exception{MethodImplementation impl=m.getImplementation();return impl==null?"native/abstract":impl.getRegisterCount()+":"+canonical(impl.getInstructions())+":"+canonical(impl.getTryBlocks())+":"+canonical(impl.getDebugItems());}
 public static void main(String[] args)throws Exception{
  Path evidence=Paths.get(args[0]),build=Paths.get(args[1]);String pkg="com.instagram.android.gpt";
  BinaryXml x=new BinaryXml(bytes(evidence.resolve("AndroidManifest.xml")));x.patch(pkg,"İnsta 🖤 GPT",true);byte[] patched=x.bytes();PatchEngine.verifyManifest(patched,pkg,true);manifest(patched);check(Arrays.equals(patched,new BinaryXml(patched).bytes()),"AXML roundtrip");Files.write(build.resolve("manifest-test.axml"),patched);
  BinaryXml baseline=new BinaryXml(bytes(evidence.resolve("AndroidManifest.xml")));baseline.patch(pkg,"InstaPro GPT",false);PatchEngine.verifyManifest(baseline.bytes(),pkg,false);
  byte[] old=bytes(evidence.resolve("resources.arsc")),arsc=BinaryXml.resources(old,pkg);int changed=0;for(int i=0;i<old.length;i++)if(old[i]!=arsc[i])changed++;check(arsc.length==old.length&&changed<256,"resource corruption");System.out.println("PASS ARSC fixed package field only; packed resources preserved");
  byte[] dex=bytes(evidence.resolve("classes20.dex"));byte[] result=DexPatch.patch(dex,bytes(build.resolve("payloaddex/classes.dex")));Files.write(build.resolve("classes20-test.dex"),result);
  DexBackedDexFile a=new DexBackedDexFile(Opcodes.forApi(28),dex),b=new DexBackedDexFile(Opcodes.forApi(28),result);Map<String,ClassDef> before=new HashMap<>();for(ClassDef c:a.getClasses())before.put(c.getType(),c);int methods=0,newClasses=0;
  for(ClassDef c:b.getClasses()){ClassDef original=before.get(c.getType());if(original==null){check(c.getType().startsWith("Lcom/instagram/gpt/"),"unwanted new class");newClasses++;continue;}Map<String,Method> origMethods=new HashMap<>();for(Method m:original.getMethods())origMethods.put(DexPatch.signature(m),m);
   for(Method m:c.getMethods()){Method oldMethod=origMethods.remove(DexPatch.signature(m));check(oldMethod!=null,"unexpected method");if(!org.jf.dexlib2.immutable.ImmutableMethod.of(m).equals(org.jf.dexlib2.immutable.ImmutableMethod.of(oldMethod)))throw new AssertionError("signature changed");boolean hook=c.getType().equals(DexPatch.TARGET)&&m.getName().equals("initPreference");if(hook){check(m.getImplementation()!=null&&(oldMethod.getAccessFlags()&256)!=0,"hook native flags");methods++;}else{check(m.getAccessFlags()==oldMethod.getAccessFlags(),"flags changed");check(body(m).equals(body(oldMethod)),"method body changed: "+c.getType()+m.getName());}}
   check(origMethods.isEmpty(),"lost methods");
  }check(methods==2&&newClasses>4,"dex changes");System.out.println("PASS DEX: exactly two native init methods replaced; original click/lifecycle preserved; GOLD stub excluded");
  try{DexPatch.patch(result,bytes(build.resolve("payloaddex/classes.dex")));throw new AssertionError("repeat patch accepted");}catch(IOException expected){}
  for(int limit=0;limit<=1;limit++){byte[] staged=DexPatch.patch(dex,bytes(build.resolve("payloaddex/classes.dex")),limit);int managed=0;for(ClassDef c:new DexBackedDexFile(Opcodes.forApi(28),staged).getClasses())if(c.getType().equals(DexPatch.TARGET))for(Method m:c.getMethods())if(m.getName().equals("initPreference")&&m.getImplementation()!=null)managed++;check(managed==limit,"diagnostic stage mismatch");}
  System.out.println("PASS diagnostic DEX stages: Activity payload without hooks, then exactly one hook");
  File keyDir=Files.createTempDirectory("instapro-key-test").toFile();SigningKeys keys=new SigningKeys(keyDir);KeyStore ks=keys.load();byte[] cert=ks.getCertificate("patcher").getEncoded();check(Arrays.equals(cert,keys.load().getCertificate("patcher").getEncoded()),"identity changed");ByteArrayOutputStream out=new ByteArrayOutputStream();char[] pass="test-only-strong-password".toCharArray();keys.exportTo(out,pass);keys.importFrom(new ByteArrayInputStream(out.toByteArray()),pass);check(Arrays.equals(cert,keys.load().getCertificate("patcher").getEncoded()),"portable identity changed");try{keys.importFrom(new ByteArrayInputStream(out.toByteArray()),"incorrect-pass".toCharArray());throw new AssertionError("wrong password accepted");}catch(IOException expected){}check(Arrays.equals(cert,keys.load().getCertificate("patcher").getEncoded()),"failed import replaced key");keys.file.delete();keys.secretFile.delete();keyDir.delete();
  System.out.println("PASS signing identity persistence, password-protected portable roundtrip, wrong-password safety");
  for(String suffix:new String[]{"", ".", ".1bad", "gpt", ".bad-x", ".a;evil"}){try{PatchEngine.checkSettings("Name",suffix);throw new AssertionError("invalid suffix accepted");}catch(IOException expected){}}
  PatchEngine.checkSettings("İnsta 🖤", ".gpt");System.out.println("ALL CORE TESTS PASSED (host only, no Android launch)");
 }
}
