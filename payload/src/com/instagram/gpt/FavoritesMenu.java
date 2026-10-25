package com.instagram.gpt;
import android.content.Intent;
import android.preference.*;
import com.OM7753.gold.GOLD;
public final class FavoritesMenu {
 public static void init(PreferenceFragment fragment,CharSequence key,int title,int summary) {
  Preference p=fragment.findPreference(key);
  if(p==null) throw new IllegalStateException("Desteklenmeyen Backup ekranı: "+key);
  boolean export="save_backup".contentEquals(key), imp="restore_backup".contentEquals(key);
  if(export||imp) {
   p.setTitle(export?"Export favorites":"Import favorites");
   p.setSummary(export?"Hesapları ve klasörleri JSON dosyasına kaydet":"Favori JSON dosyasından birleştir veya listeyi değiştir");
   p.setOnPreferenceClickListener(pref->{Intent intent=new Intent(fragment.getActivity(),FavoritesTransferActivity.class);intent.putExtra("mode",export?"export":"import");fragment.startActivity(intent);return true;});
  } else {
   p.setTitle(GOLD.EnStrById(title));if(summary!=0)p.setSummary(GOLD.EnStrById(summary));
   p.setOnPreferenceClickListener((Preference.OnPreferenceClickListener)fragment);
  }
 }
}
