package dev.instapro.patcher;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.*;
import com.android.apksig.*;
public final class PatchEngine {
 public static final String SOURCE_SHA="eeb7b8598b3cb2718bf3cf4c293e854e910e159e103ba9ccc0225dc4168e064e";
 public interface Progress {void show(String s);}
 public static final String ORIGINAL_PACKAGE="com.instagram.android";
 public static String normalizeSuffix(String input)throws IOException{
  if(input==null)throw new IOException("Paket eki okunamadı");
  String suffix=input.trim();
  if(suffix.isEmpty())return "";
  if(!suffix.startsWith("."))suffix="."+suffix;
  if(!suffix.matches("(?:\\.[A-Za-z][A-Za-z0-9_]{0,39})+")||suffix.length()>80)
   throw new IOException("Geçersiz paket eki. Örnek: gpt. Orijinal paketi korumak için boş bırakın.");
  return suffix;
 }
 public static String packageName(String suffix)throws IOException{return ORIGINAL_PACKAGE+normalizeSuffix(suffix);}
 public static void checkSettings(String label,String suffix)throws IOException{
  if(label==null||label.trim().isEmpty()||label.length()>80||label.chars().anyMatch(c->Character.isISOControl(c)))throw new IOException("Uygulama adı geçersiz");
  normalizeSuffix(suffix);
 }
 public static String sha(File f)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];for(int n;(n=in.read(b))!=-1;){cancelled();md.update(b,0,n);}}StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 static void cancelled()throws IOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("İptal edildi");}
 public static void copy(InputStream in,OutputStream out,long limit)throws IOException{byte[] b=new byte[65536];long total=0;for(int n;(n=in.read(b))!=-1;){cancelled();total+=n;if(total>limit)throw new IOException("Dosya boyutu sınırı aşıldı");out.write(b,0,n);}}
 static byte[] read(ZipFile zip,String name,int max)throws IOException{ZipEntry e=zip.getEntry(name);if(e==null||e.getSize()<0||e.getSize()>max)throw new IOException("Eksik/büyük giriş: "+name);ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=zip.getInputStream(e)){copy(in,out,max);}return out.toByteArray();}
 public static void build(File source,File output,byte[] payload,String label,String suffix,KeyStore keys,char[] keyPassword,Progress progress,int stage)throws Exception{
  if(stage<0||stage>3)throw new IOException("Geçersiz aşama");checkSettings(label,suffix);String pkg=packageName(suffix);
  if(source.length()!=180564084L||!sha(source).equals(SOURCE_SHA))throw new IOException("Desteklenmeyen APK. Yalnız orijinal InstaPro 15.85 profili kabul edilir; zaten modlanmış dosya da reddedilir.");
  File unsigned=new File(output.getParentFile(),"unsigned.tmp.apk");boolean ok=false;
  try(ZipFile zip=new ZipFile(source)){
   Set<String> names=new HashSet<>();long size=0;for(Enumeration<? extends ZipEntry> es=zip.entries();es.hasMoreElements();){ZipEntry e=es.nextElement();String n=e.getName();if(!names.add(n)||n.startsWith("/")||n.contains("\\")||Arrays.asList(n.split("/")).contains("..")||e.getSize()<0||e.getSize()>300_000_000L)throw new IOException("Güvensiz ZIP girişi");size+=e.getSize();if(names.size()>100000||size>1_200_000_000L)throw new IOException("ZIP sınırı");}
   progress.show("Manifest ve kaynak paketi düzenleniyor…");Map<String,byte[]> changed=new HashMap<>();BinaryXml xml=new BinaryXml(read(zip,"AndroidManifest.xml",2_000_000));xml.patch(pkg,label,stage>0);changed.put("AndroidManifest.xml",xml.bytes());
   if(!pkg.equals(ORIGINAL_PACKAGE))changed.put("resources.arsc",BinaryXml.resources(read(zip,"resources.arsc",20_000_000),pkg));
   if(stage>0){progress.show("Favori menüsü ekleniyor…");changed.put("classes20.dex",DexPatch.patch(read(zip,"classes20.dex",4_000_000),payload,Math.max(0,stage-1)));}
   progress.show("Paketleniyor ve hizalanıyor…");
   try(CountOutput counter=new CountOutput(new FileOutputStream(unsigned));ZipOutputStream out=new ZipOutputStream(counter)){
    for(Enumeration<? extends ZipEntry> es=zip.entries();es.hasMoreElements();){cancelled();ZipEntry e=es.nextElement();String name=e.getName();if(signature(name))continue;byte[] replacement=changed.get(name);ZipEntry ne=new ZipEntry(name);ne.setMethod(e.getMethod());ne.setTime(e.getTime());if(e.getComment()!=null)ne.setComment(e.getComment());
     if(e.getMethod()==ZipEntry.STORED){long len=replacement==null?e.getSize():replacement.length;long crc=e.getCrc();if(replacement!=null){CRC32 c=new CRC32();c.update(replacement);crc=c.getValue();}ne.setSize(len);ne.setCompressedSize(len);ne.setCrc(crc);int align=name.endsWith(".so")?16384:4;int pad=(int)((align-(counter.count+30+name.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)%align)%align);if(pad>0){if(pad<4)pad+=align;byte[] extra=new byte[pad];extra[0]=0x35;extra[1]=(byte)0xd9;extra[2]=(byte)(pad-4);extra[3]=(byte)((pad-4)>>>8);ne.setExtra(extra);}}
     out.putNextEntry(ne);if(replacement!=null)out.write(replacement);else try(InputStream in=zip.getInputStream(e)){copy(in,out,300_000_000L);}out.closeEntry();
    }
   }
   cancelled();progress.show("Release imzası uygulanıyor…");PrivateKey key=(PrivateKey)keys.getKey("patcher",keyPassword);X509Certificate cert=(X509Certificate)keys.getCertificate("patcher");
   ApkSigner.SignerConfig signer=new ApkSigner.SignerConfig.Builder("instapro",key,Collections.singletonList(cert)).build();new ApkSigner.Builder(Collections.singletonList(signer)).setInputApk(unsigned).setOutputApk(output).setMinSdkVersion(28).setV1SigningEnabled(false).setV2SigningEnabled(true).setV3SigningEnabled(true).setV4SigningEnabled(false).build().sign();
   cancelled();progress.show("İmza ve release manifest doğrulanıyor…");ApkVerifier.Result result=new ApkVerifier.Builder(output).setMinCheckedPlatformVersion(28).setMaxCheckedPlatformVersion(36).build().verify();if(!result.isVerified())throw new IOException("İmza doğrulanamadı: "+result.getErrors());
   try(ZipFile signed=new ZipFile(output)){verifyManifest(read(signed,"AndroidManifest.xml",2_000_000),pkg,stage>0);for(String name:changed.keySet())if(!Arrays.equals(changed.get(name),read(signed,name,20_000_000)))throw new IOException("Çıktı uyuşmuyor: "+name);}
   cancelled();ok=true;
  }finally{unsigned.delete();if(!ok)output.delete();}
 }
 static boolean signature(String name){String n=name.toUpperCase(Locale.ROOT);return n.equals("STAMP-CERT-SHA256")||n.startsWith("META-INF/")&&(n.endsWith(".SF")||n.endsWith(".RSA")||n.endsWith(".DSA")||n.endsWith(".EC")||n.equals("META-INF/MANIFEST.MF"));}
 public static void verifyManifest(byte[] data,String pkg,boolean activity)throws IOException{
  BinaryXml x=new BinaryXml(data);boolean debug=false,packageOk=false,added=false;
  for(byte[] c:x.chunks)if(BinaryXml.u16(c,0)==0x102){String tag=x.tag(c);boolean component=Arrays.asList("activity","activity-alias","service","receiver","provider").contains(tag),name=false;
   for(int p:x.attrs(c)){int id=x.id(c,p);String s=x.text(c,p);if(tag.equals("manifest")&&x.strings.get(BinaryXml.i32(c,p+4)).equals("package"))packageOk=pkg.equals(s);
    if(tag.equals("application")&&id==0x0101000f){if((c[p+15]&255)!=0x12||BinaryXml.i32(c,p+16)!=0)throw new IOException("debuggable false değil");debug=true;}
    if(id==0x01010003&&s!=null){name=true;if(s.equals("com.instagram.gpt.FavoritesTransferActivity"))added=true;}
   }if(component&&!name)throw new IOException("Resource-mapped android:name eksik: "+tag);
  }if(!debug||!packageOk||activity!=added)throw new IOException("Release manifest uyuşmuyor");
 }
 static final class CountOutput extends FilterOutputStream{long count;CountOutput(OutputStream o){super(o);}public void write(int b)throws IOException{out.write(b);count++;}public void write(byte[] b,int p,int n)throws IOException{out.write(b,p,n);count+=n;}}
}
