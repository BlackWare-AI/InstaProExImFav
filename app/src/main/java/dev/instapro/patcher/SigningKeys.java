package dev.instapro.patcher;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.*;
import java.util.*;
/** Persistent, portable PKCS12 identity. No signing key is embedded in the APK. */
final class SigningKeys {
 final File file,secretFile;
 SigningKeys(File dir){file=new File(dir,"identity.p12");secretFile=new File(dir,"identity-password");}
 synchronized char[] password()throws Exception{
  if(secretFile.exists()){try(InputStream in=new FileInputStream(secretFile);ByteArrayOutputStream out=new ByteArrayOutputStream()){PatchEngine.copy(in,out,256);return new String(out.toByteArray(),StandardCharsets.US_ASCII).toCharArray();}}
  if(file.exists())throw new IOException("İmza anahtarının yerel parolası bulunamadı");
  byte[] random=new byte[32];new SecureRandom().nextBytes(random);StringBuilder text=new StringBuilder();for(byte b:random)text.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
  File tmp=new File(secretFile.getPath()+".tmp");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(text.toString().getBytes(StandardCharsets.US_ASCII));out.getFD().sync();}if(!tmp.renameTo(secretFile))throw new IOException("Anahtar parolası saklanamadı");return text.toString().toCharArray();
 }
 synchronized KeyStore load()throws Exception{if(file.exists())return read(new FileInputStream(file),password());KeyPairGenerator gen=KeyPairGenerator.getInstance("RSA");gen.initialize(3072);KeyPair pair=gen.generateKeyPair();KeyStore store=KeyStore.getInstance("PKCS12");store.load(null,null);store.setKeyEntry("patcher",pair.getPrivate(),password(),new java.security.cert.Certificate[]{certificate(pair)});save(store);return store;}
 void save(KeyStore store)throws Exception{File tmp=new File(file.getPath()+".tmp");try(FileOutputStream out=new FileOutputStream(tmp)){store.store(out,password());out.getFD().sync();}if(!tmp.renameTo(file))throw new IOException("Anahtar saklanamadı");}
 static KeyStore read(InputStream in,char[] pass)throws Exception{try(InputStream source=in;ByteArrayOutputStream bytes=new ByteArrayOutputStream()){PatchEngine.copy(source,bytes,1_000_000);KeyStore s=KeyStore.getInstance("PKCS12");s.load(new ByteArrayInputStream(bytes.toByteArray()),pass);return s;}}
 void exportTo(OutputStream out,char[] pass)throws Exception{KeyStore old=load(),s=KeyStore.getInstance("PKCS12");s.load(null,null);s.setKeyEntry("patcher",old.getKey("patcher",password()),pass,old.getCertificateChain("patcher"));s.store(out,pass);}
 void importFrom(InputStream in,char[] pass)throws Exception{KeyStore source=read(in,pass);List<String> keys=new ArrayList<>();for(Enumeration<String> a=source.aliases();a.hasMoreElements();){String n=a.nextElement();if(source.isKeyEntry(n))keys.add(n);}if(keys.size()!=1)throw new IOException("Tek imza anahtarı gerekli");String name=keys.get(0);PrivateKey k=(PrivateKey)source.getKey(name,pass);if(!"RSA".equals(k.getAlgorithm()))throw new IOException("RSA anahtar gerekli");KeyStore target=KeyStore.getInstance("PKCS12");target.load(null,null);target.setKeyEntry("patcher",k,password(),source.getCertificateChain(name));Signature proof=Signature.getInstance("SHA256withRSA");proof.initSign(k);proof.update(new byte[]{1});byte[] sig=proof.sign();proof.initVerify(source.getCertificate(name));proof.update(new byte[]{1});if(!proof.verify(sig))throw new IOException("Anahtar/sertifika uyuşmuyor");save(target);}
 static byte[] der(int tag,byte[]... values)throws IOException{ByteArrayOutputStream body=new ByteArrayOutputStream();for(byte[] b:values)body.write(b);byte[] b=body.toByteArray();ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(tag);if(b.length<128)out.write(b.length);else{int n=b.length,count=0;while(n>0){count++;n>>>=8;}out.write(128|count);for(int i=count-1;i>=0;i--)out.write(b.length>>>(i*8));}out.write(b);return out.toByteArray();}
 static X509Certificate certificate(KeyPair pair)throws Exception{
  byte[] algorithm=der(0x30,der(6,new byte[]{42,(byte)134,72,(byte)134,(byte)247,13,1,1,11}),der(5,new byte[0]));
  byte[] name=der(0x30,der(0x31,der(0x30,der(6,new byte[]{85,4,3}),der(12,"InstaPro Personal Signing".getBytes(StandardCharsets.UTF_8)))));
  byte[] validity=der(0x30,der(0x18,"20250101000000Z".getBytes(StandardCharsets.US_ASCII)),der(0x18,"20560101000000Z".getBytes(StandardCharsets.US_ASCII)));
  byte[] tbs=der(0x30,der(0xa0,der(2,new byte[]{2})),der(2,new BigInteger(120,new SecureRandom()).add(BigInteger.ONE).toByteArray()),algorithm,name,validity,name,pair.getPublic().getEncoded());
  Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(pair.getPrivate());signer.update(tbs);byte[] signature=signer.sign(),bits=new byte[signature.length+1];System.arraycopy(signature,0,bits,1,signature.length);
  return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der(0x30,tbs,algorithm,der(3,bits))));
 }
}
