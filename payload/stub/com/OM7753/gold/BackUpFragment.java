package com.OM7753.gold;
import android.preference.*;
import com.instagram.gpt.FavoritesMenu;
/** Only a source of replacement method bodies. Never merged as a class. */
public class BackUpFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
 private void initPreference(CharSequence key,int title) { FavoritesMenu.init(this,key,title,0); }
 private void initPreference(CharSequence key,int title,int summary) { FavoritesMenu.init(this,key,title,summary); }
 public native boolean onPreferenceClick(Preference p);
}
