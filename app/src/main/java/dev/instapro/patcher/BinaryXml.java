package dev.instapro.patcher;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Bounded AXML editor for the pinned profile. Resource mapped attribute IDs are authoritative. */
public final class BinaryXml {
 static final int NONE=-1, STR=3, ANDROID=0x01010000;
 final List<String> strings=new ArrayList<>(); final List<byte[]> chunks=new ArrayList<>(); int[] ids; int ns;
 static int u16(byte[] b,int p){ return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getShort(p)&65535; }
 static int i32(byte[] b,int p){ return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt(p); }
 static void put(byte[] b,int p,int v){ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).putInt(p,v);}
 static void shortAt(byte[] b,int p,int v){ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).putShort(p,(short)v);}
 public BinaryXml(byte[] data) throws IOException {
  if(data.length<8||u16(data,0)!=3||i32(data,4)!=data.length)throw new IOException("Bozuk AXML");
  for(int p=8;p<data.length;){int n=i32(data,p+4);if(n<8||n>data.length-p)throw new IOException("AXML chunk");byte[] c=Arrays.copyOfRange(data,p,p+n);chunks.add(c);p+=n;

  }
  byte[] pool=chunks.stream().filter(c->u16(c,0)==1).findFirst().orElseThrow(()->new IOException("String pool yok"));
  if(u16(pool,2)!=28||i32(pool,12)!=0||i32(pool,16)!=0)throw new IOException("Profil string pool uyumsuz");
  int count=i32(pool,8),start=i32(pool,20); if(count<0||count>100000)throw new IOException("String sınırı");
  for(int i=0;i<count;i++){int p=start+i32(pool,28+i*4),n=u16(pool,p);p+=2;if((n&32768)!=0){n=((n&32767)<<16)|u16(pool,p);p+=2;}strings.add(new String(pool,p,n*2,StandardCharsets.UTF_16LE));}
  byte[] map=chunks.stream().filter(c->u16(c,0)==0x180).findFirst().orElseThrow(()->new IOException("Resource map yok"));ids=new int[(map.length-8)/4];for(int i=0;i<ids.length;i++)ids[i]=i32(map,8+4*i);
  ns=strings.indexOf("http://schemas.android.com/apk/res/android");
 }
 int sid(String s){int i=strings.indexOf(s);if(i<0){i=strings.size();strings.add(s);}return i;}
 int attrIndex(int id) throws IOException {for(int i=0;i<ids.length;i++)if(ids[i]==id)return i;throw new IOException("Attribute ID yok: "+Integer.toHexString(id));}
 String text(byte[] c,int p){return (c[p+15]&255)==3?strings.get(i32(c,p+16)):null;}
 void value(byte[] c,int p,String s){int i=sid(s);put(c,p+8,i);c[p+15]=3;put(c,p+16,i);}
 String tag(byte[] c){return strings.get(i32(c,20));}
 public void patch(String pkg,String label, boolean activity) throws IOException {
  Set<String> permissions=new HashSet<>();
  for(byte[] c:chunks)if(u16(c,0)==0x102&&tag(c).equals("permission"))for(int p:attrs(c))if(id(c,p)==0x01010003){String s=text(c,p);if(s!=null)permissions.add(s);}
  boolean app=false,debug=false;
  for(byte[] c:chunks){if(u16(c,0)!=0x102)continue;String tag=tag(c);
   for(int p:attrs(c)){int id=id(c,p);String s=text(c,p);String key=strings.get(i32(c,p+4));
    if(tag.equals("manifest")&&key.equals("package")){if(!"com.instagram.android".equals(s))throw new IOException("Kaynak paket uyumsuz");value(c,p,pkg);}
    if(tag.equals("application")){app=true;if(id==0x0101000f){put(c,p+8,NONE);c[p+15]=0x12;put(c,p+16,0);debug=true;}}
    if(id==0x01010001&&(tag.equals("application")||tag.equals("activity")||tag.equals("activity-alias")))value(c,p,label);
    if(s==null)continue;
    if(id==0x01010018)value(c,p,joinAuthorities(s,pkg.substring("com.instagram.android".length())));
    else if((id==0x01010003&&(tag.equals("permission")||tag.startsWith("uses-permission")))||id==0x01010006||id==0x01010007||id==0x01010008){if(s.startsWith("com.instagram.android."))value(c,p,pkg+s.substring(21));else if(s.startsWith(".")&&permissions.contains(s))value(c,p,pkg+s);}
    else if((id==0x01010012||id==0x01010011)&&s.startsWith("com.instagram.android"))value(c,p,pkg+s.substring(21));
   }
  }
  if(!app||!debug)throw new IOException("Profil application/debuggable alanı eksik");
  if(activity){byte[] node=new byte[36+5*20];shortAt(node,0,0x102);shortAt(node,2,16);put(node,4,node.length);put(node,8,1);put(node,12,NONE);put(node,16,NONE);put(node,20,sid("activity"));shortAt(node,24,20);shortAt(node,26,20);shortAt(node,28,5);
   attr(node,36,0x01010000,1,16974382);attr(node,56,0x01010001,3,sid("Favorites"));attr(node,76,0x01010003,3,sid("com.instagram.gpt.FavoritesTransferActivity"));attr(node,96,0x01010010,0x12,0);attr(node,116,0x0101001f,0x11,0x4a0);
   byte[] end=new byte[24];shortAt(end,0,0x103);shortAt(end,2,16);put(end,4,24);put(end,8,1);put(end,12,NONE);put(end,16,NONE);put(end,20,sid("activity"));
   for(int i=0;i<chunks.size();i++)if(u16(chunks.get(i),0)==0x103&&tag(chunks.get(i)).equals("application")){chunks.add(i,node);chunks.add(i+1,end);break;}
  }
 }
 static String joinAuthorities(String s,String suffix){String[] a=s.split(";",-1);for(int i=0;i<a.length;i++)a[i]+=suffix;return String.join(";",a);}
 int id(byte[] c,int p){int n=i32(c,p+4);return n<ids.length?ids[n]:0;}
 List<Integer> attrs(byte[] c)throws IOException{int start=16+u16(c,24),size=u16(c,26),n=u16(c,28);if(size!=20||start+n*size>c.length)throw new IOException("AXML attributes");List<Integer> out=new ArrayList<>();for(int i=0;i<n;i++)out.add(start+i*size);return out;}
 void attr(byte[] c,int p,int id,int type,int data)throws IOException{put(c,p,ns);put(c,p+4,attrIndex(id));put(c,p+8,type==3?data:NONE);shortAt(c,p+12,8);c[p+15]=(byte)type;put(c,p+16,data);}
 public byte[] bytes()throws IOException {
  ByteArrayOutputStream raw=new ByteArrayOutputStream();int[] offsets=new int[strings.size()];for(int i=0;i<strings.size();i++){offsets[i]=raw.size();String s=strings.get(i);int n=s.length();if(n>=32768){raw.write((n>>>16)&255);raw.write(128|((n>>>24)&127));}raw.write(n&255);raw.write((n>>>8)&255);raw.write(s.getBytes(StandardCharsets.UTF_16LE));raw.write(0);raw.write(0);}while(raw.size()%4!=0)raw.write(0);
  byte[] pool=new byte[28+offsets.length*4+raw.size()];shortAt(pool,0,1);shortAt(pool,2,28);put(pool,4,pool.length);put(pool,8,offsets.length);put(pool,20,28+offsets.length*4);for(int i=0;i<offsets.length;i++)put(pool,28+i*4,offsets[i]);System.arraycopy(raw.toByteArray(),0,pool,28+offsets.length*4,raw.size());
  ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(new byte[8]);for(byte[] c:chunks)out.write(u16(c,0)==1?pool:c);byte[] b=out.toByteArray();shortAt(b,0,3);shortAt(b,2,8);put(b,4,b.length);return b;
 }
 public static byte[] resources(byte[] b,String pkg)throws IOException{b=b.clone();int found=0;for(int p=u16(b,2);p<b.length;){int n=i32(b,p+4);if(n<8||n>b.length-p)throw new IOException("ARSC chunk");if(u16(b,p)==0x200){if(u16(b,p+2)<268)throw new IOException("ARSC header");String old=new String(b,p+12,256,StandardCharsets.UTF_16LE).split("\u0000",2)[0];if(old.equals("com.instagram.android")){byte[] v=pkg.getBytes(StandardCharsets.UTF_16LE);if(v.length>254)throw new IOException("Paket çok uzun");Arrays.fill(b,p+12,p+268,(byte)0);System.arraycopy(v,0,b,p+12,v.length);found++;}}p+=n;}if(found!=1)throw new IOException("ARSC paket eşleşmedi");return b;}
}
