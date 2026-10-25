import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.*;
import com.android.apksig.*;
import com.android.apksig.internal.apk.AndroidBinXmlParser;
public class SignRelease {
 public static void main(String[] args)throws Exception{
  char[] password=new String(Files.readAllBytes(Paths.get(args[1])),java.nio.charset.StandardCharsets.UTF_8).trim().toCharArray();KeyStore ks=KeyStore.getInstance("PKCS12");try(InputStream in=new FileInputStream(args[0])){ks.load(in,password);}PrivateKey key=(PrivateKey)ks.getKey(args[2],password);X509Certificate cert=(X509Certificate)ks.getCertificate(args[2]);Arrays.fill(password,'\0');
  ApkSigner.SignerConfig signer=new ApkSigner.SignerConfig.Builder("release",key,Collections.singletonList(cert)).build();new ApkSigner.Builder(Collections.singletonList(signer)).setInputApk(new File(args[3])).setOutputApk(new File(args[4])).setMinSdkVersion(28).setV1SigningEnabled(false).setV2SigningEnabled(true).setV3SigningEnabled(true).setV4SigningEnabled(false).build().sign();
  ApkVerifier.Result result=new ApkVerifier.Builder(new File(args[4])).setMinCheckedPlatformVersion(28).setMaxCheckedPlatformVersion(36).build().verify();if(!result.isVerified())throw new AssertionError(result.getErrors());
  boolean debug=false;try(ZipFile z=new ZipFile(args[4])){AndroidBinXmlParser p=new AndroidBinXmlParser(ByteBuffer.wrap(z.getInputStream(z.getEntry("AndroidManifest.xml")).readAllBytes()));while(p.next()!=AndroidBinXmlParser.EVENT_END_DOCUMENT){if(p.getEventType()!=AndroidBinXmlParser.EVENT_START_ELEMENT||!p.getName().equals("application"))continue;for(int i=0;i<p.getAttributeCount();i++)if(p.getAttributeNameResourceId(i)==0x0101000f){if(p.getAttributeIntValue(i)!=0)throw new AssertionError("debuggable=true");debug=true;}}}if(!debug)throw new AssertionError("explicit debug=false missing");
  byte[] digest=MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format("%02x",b&255));System.out.println("PASS release signature v2="+result.isVerifiedUsingV2Scheme()+" v3="+result.isVerifiedUsingV3Scheme()+"; android:debuggable=false");System.out.println("Certificate SHA-256: "+hex);
 }
}
