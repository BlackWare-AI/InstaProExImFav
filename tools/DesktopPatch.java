package dev.instapro.patcher;
import java.io.*;
import java.nio.file.*;
/** Keys stored outside the repository; baseline excludes Activity and DEX hooks. */
public class DesktopPatch {
 public static void main(String[] args)throws Exception{
  int stage=java.util.Arrays.asList("baseline","activity","hook1","favorites").indexOf(args[4]);if(stage<0)throw new IllegalArgumentException("stage");
  File dir=new File(System.getProperty("user.home"),".instapro-target-signing");dir.mkdirs();dir.setReadable(false,false);dir.setWritable(false,false);dir.setExecutable(false,false);dir.setReadable(true,true);dir.setWritable(true,true);dir.setExecutable(true,true);SigningKeys identity=new SigningKeys(dir);char[] password=identity.password();
  try{PatchEngine.build(new File(args[0]),new File(args[1]),Files.readAllBytes(Paths.get(args[5])),args[2],args[3],identity.load(),password,System.out::println,stage);}finally{java.util.Arrays.fill(password,'\0');}
 }
}
