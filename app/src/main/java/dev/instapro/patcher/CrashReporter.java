package dev.instapro.patcher;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.AtomicFile;
import android.util.Log;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Local-only diagnostics. The Android default crash handler still receives uncaught errors. */
public final class CrashReporter extends Application {
 private static File file(Context context){return new File(context.getNoBackupFilesDir(),"patcher-crash.txt");}
 @Override public void onCreate(){
  super.onCreate();
  final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
  Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
   try{record(this,error);}catch(Throwable loggingFailure){Log.e("InstaProPatcher","Error reporting failed",loggingFailure);}
   if(previous!=null)previous.uncaughtException(thread,error);
   else {android.os.Process.killProcess(android.os.Process.myPid());System.exit(10);}
  });
 }
 static String record(Context context,Throwable error){
  StringWriter text=new StringWriter();PrintWriter writer=new PrintWriter(text);
  writer.println("InstaPro Patcher — local error report");
  try{PackageInfo p=context.getPackageManager().getPackageInfo(context.getPackageName(),0);writer.println("Version: "+p.versionName+" ("+p.getLongVersionCode()+")");}catch(Exception ignored){}
  writer.println("Device: "+Build.MANUFACTURER+" "+Build.MODEL);
  writer.println("Android: "+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT);
  writer.println("ABI: "+Arrays.toString(Build.SUPPORTED_ABIS));
  writer.println("Time: "+System.currentTimeMillis());writer.println();error.printStackTrace(writer);writer.flush();
  String report=text.toString();if(report.length()>60000)report=report.substring(0,60000)+"\n[truncated]";
  AtomicFile target=new AtomicFile(file(context));FileOutputStream out=null;
  try{out=target.startWrite();out.write(report.getBytes(StandardCharsets.UTF_8));target.finishWrite(out);}
  catch(Exception failed){if(out!=null)target.failWrite(out);Log.e("InstaProPatcher","Cannot persist error report",failed);}
  Log.e("InstaProPatcher","Patcher error",error);
  return report;
 }
 static String read(Context context){
  AtomicFile target=new AtomicFile(file(context));
  try(InputStream in=target.openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] buffer=new byte[4096];int n;
   while((n=in.read(buffer))!=-1){if(out.size()+n>250000)throw new IOException("Report too large");out.write(buffer,0,n);}
   return new String(out.toByteArray(),StandardCharsets.UTF_8);
  }catch(IOException missing){return null;}
 }
 static void clear(Context context){new AtomicFile(file(context)).delete();}
}
