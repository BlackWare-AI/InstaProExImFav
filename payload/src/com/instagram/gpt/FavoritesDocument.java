package com.instagram.gpt;

import org.json.*;
import java.util.*;

/** Only favorites are accepted/exported; unrelated backup settings are never copied. */
public final class FavoritesDocument {
    public static final class Page {
        public String username, name, picture, bio;
        public long createdAt;
        Page(String u, String n, String p, String b, long t) {
            username=u; name=n; picture=p; bio=b; createdAt=t;
        }
    }
    public final LinkedHashMap<String,Page> pages = new LinkedHashMap<>();
    public final LinkedHashMap<String,LinkedHashSet<String>> categories = new LinkedHashMap<>();
    public int duplicates;
    public int memberships() {
        int n=0; for(Set<String> s: categories.values()) n+=s.size(); return n;
    }
    public static FavoritesDocument parse(String input) throws Exception {
        String text=input.trim();
        if (text.startsWith("\uFEFF")) text=text.substring(1).trim();
        JSONTokener tokener=new JSONTokener(text);
        Object root=tokener.nextValue();
        if(tokener.nextClean()!=0) throw new IllegalArgumentException("JSON dosyasının sonunda geçersiz veri var.");
        JSONArray groups;
        if(root instanceof JSONObject) {
            JSONObject obj=(JSONObject)root;
            Object value=obj.opt("favorite_page_pref");
            if(value instanceof String) { JSONTokener inner=new JSONTokener((String)value); Object parsed=inner.nextValue(); if(!(parsed instanceof JSONArray)||inner.nextClean()!=0) throw new IllegalArgumentException("favorite_page_pref geçerli bir dizi değil."); groups=(JSONArray)parsed; }
            else if(value instanceof JSONArray) groups=(JSONArray)value;
            else throw new IllegalArgumentException("Bu dosyada favorite_page_pref bulunamadı.");
        } else if(root instanceof JSONArray) groups=(JSONArray)root;
        else throw new IllegalArgumentException("Geçersiz favori JSON dosyası.");
        FavoritesDocument doc=new FavoritesDocument();
        long now=System.currentTimeMillis(); int total=0;
        if(groups.length()>10000) throw new IllegalArgumentException("Çok fazla klasör var.");
        for(int g=0;g<groups.length();g++) {
            JSONObject group=groups.getJSONObject(g);
            String category=text(group,"name").trim();
            if(category.isEmpty()) throw new IllegalArgumentException("Klasör adı eksik.");
            if(category.length()>1000) throw new IllegalArgumentException("Klasör adı çok uzun.");
            LinkedHashSet<String> links=doc.categories.get(category);
            if(links==null) { links=new LinkedHashSet<>(); doc.categories.put(category,links); }
            JSONArray items=group.getJSONArray("items");
            for(int i=0;i<items.length();i++) {
                if(++total>100000) throw new IllegalArgumentException("100.000 kayıt sınırı aşıldı.");
                JSONObject item=items.getJSONObject(i);
                String username=text(item,"pageId").trim();
                if(username.isEmpty()) username=text(item,"username").trim();
                if(username.startsWith("@")) username=username.substring(1);
                if(!username.matches("[A-Za-z0-9._]{1,30}"))
                    throw new IllegalArgumentException("Geçersiz kullanıcı adı: klasör "+(g+1)+", kayıt "+(i+1));
                String key=username.toLowerCase(Locale.ROOT);
                String name=text(item,"name");
                if(name.isEmpty()) name=text(item,"pageName");
                Object date=item.opt("createdAt");
                if(date!=null && !(date instanceof Integer) && !(date instanceof Long)) throw new IllegalArgumentException("Kayıt tarihi tam sayı olmalı.");
                long time=date==null?now-total:((Number)date).longValue();
                if(time<0) throw new IllegalArgumentException("Geçersiz kayıt tarihi.");
                Page page=new Page(username,name,text(item,"profilePicUrl"),text(item,"bio"),time);
                Page existing=doc.pages.get(key);
                if(existing==null) doc.pages.put(key,page);
                else { if(existing.name.isEmpty()) existing.name=page.name;
                       if(existing.picture.isEmpty()) existing.picture=page.picture;
                       if(existing.bio.isEmpty()) existing.bio=page.bio; }
                if(!links.add(key)) doc.duplicates++;
            }
        }
        return doc;
    }
    static String text(JSONObject o,String k) {
        Object v=o.opt(k); if(v==null) return ""; if(!(v instanceof String)) throw new IllegalArgumentException("Metin alanı geçersiz: "+k); String s=(String)v; if(s.length()>100000) throw new IllegalArgumentException("Metin alanı çok uzun: "+k); return s;
    }
    public String toJson() throws Exception {
        JSONArray groups=new JSONArray();
        for(Map.Entry<String,LinkedHashSet<String>> entry: categories.entrySet()) {
            JSONArray items=new JSONArray();
            for(String key:entry.getValue()) {
                Page p=pages.get(key);
                if(p==null) throw new IllegalStateException("Klasör bağlantısı eksik.");
                JSONObject item=new JSONObject();
                item.put("pageId",p.username); item.put("name",p.name);
                item.put("profilePicUrl",p.picture); item.put("bio",p.bio); item.put("createdAt",p.createdAt);
                items.put(item);
            }
            JSONObject group=new JSONObject(); group.put("name",entry.getKey()); group.put("items",items); groups.put(group);
        }
        JSONObject root=new JSONObject();
        root.put("gpt_favorites_format","instapro-gpt-favorites-v1");
        root.put("favorite_page_pref",groups.toString());
        return root.toString();
    }
}
