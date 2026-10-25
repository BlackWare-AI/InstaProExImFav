package dev.instapro.patcher;
import java.io.IOException;
/** Regression for the user's gpt input and for patching without cloning. */
public final class SettingsTest {
 static void expect(String input,String expected)throws Exception{
  PatchEngine.checkSettings("InstaPro",input);
  String actual=PatchEngine.packageName(input);
  if(!expected.equals(actual))throw new AssertionError(input+" -> "+actual);
 }
 public static void main(String[] args)throws Exception{
  String original="com.instagram.android";
  expect("",original);expect("   ",original);
  expect("gpt",original+".gpt");expect(".gpt",original+".gpt");
  expect("  gpt  ",original+".gpt");expect("personal.copy",original+".personal.copy");
  for(String bad:new String[]{null,".","..gpt","1gpt","gpt.","gp t","gpt/evil","gpt;evil"}){
   try{PatchEngine.checkSettings("InstaPro",bad);throw new AssertionError("Accepted invalid suffix: "+bad);}catch(IOException expected){}
  }
  System.out.println("PASS suffix regression: gpt/.gpt equivalent; blank/whitespace keep original package; malformed suffixes rejected");
 }
}
