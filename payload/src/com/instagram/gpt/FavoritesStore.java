package com.instagram.gpt;
import java.util.*;

/** Shared transaction and mapping code, used unchanged by Android and host integration tests. */
public final class FavoritesStore {
    public static final String CREATE_PAGES="CREATE TABLE favorite_pages (id INTEGER PRIMARY KEY AUTOINCREMENT, pageName TEXT, username TEXT, profilePicUrl TEXT, bio TEXT, createdAt INTEGER NOT NULL DEFAULT 0)";
    public static final String CREATE_CATEGORIES="CREATE TABLE favorite_categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE)";
    public static final String CREATE_LINKS="CREATE TABLE favorite_page_categories (pageId INTEGER NOT NULL, categoryId INTEGER NOT NULL, PRIMARY KEY (pageId, categoryId))";
    public interface Backend {
        void begin() throws Exception;
        void end(boolean success) throws Exception;
        void exec(String sql,Object... args) throws Exception;
        long insert(String sql,Object... args) throws Exception;
        List<Map<String,Object>> rows(String sql,Object... args) throws Exception;
    }
    public interface Backup { void save(String json) throws Exception; }
    private final Backend db;
    public FavoritesStore(Backend db) { this.db=db; }
    static String text(Map<String,Object> row,String key) { Object v=row.get(key); return v==null?"":v.toString(); }
    static long number(Map<String,Object> row,String key) { Object v=row.get(key); return v instanceof Number?((Number)v).longValue():0; }
    public FavoritesDocument read() throws Exception {
        FavoritesDocument doc=new FavoritesDocument();
        Map<Long,String> pageKeys=new HashMap<>(); Map<Long,String> categoryNames=new HashMap<>();
        for(Map<String,Object> row:db.rows("SELECT id,pageName,username,profilePicUrl,bio,createdAt FROM favorite_pages ORDER BY createdAt DESC,id ASC")) {
            String u=text(row,"username"),key=u.toLowerCase(Locale.ROOT);
            if(u.isEmpty()) continue;
            pageKeys.put(number(row,"id"),key);
            if(!doc.pages.containsKey(key)) doc.pages.put(key,new FavoritesDocument.Page(u,text(row,"pageName"),text(row,"profilePicUrl"),text(row,"bio"),number(row,"createdAt")));
        }
        for(Map<String,Object> row:db.rows("SELECT id,name FROM favorite_categories ORDER BY id ASC")) {
            String name=text(row,"name"); categoryNames.put(number(row,"id"),name);
            doc.categories.put(name,new LinkedHashSet<String>());
        }
        Set<String> linked=new HashSet<>();
        for(Map<String,Object> row:db.rows("SELECT pageId,categoryId FROM favorite_page_categories ORDER BY categoryId,pageId")) {
            String p=pageKeys.get(number(row,"pageId")),c=categoryNames.get(number(row,"categoryId"));
            if(p!=null&&c!=null) { doc.categories.get(c).add(p); linked.add(p); }
        }
        for(String key:doc.pages.keySet()) if(!linked.contains(key)) {
            if(!doc.categories.containsKey("Favorites")) doc.categories.put("Favorites",new LinkedHashSet<String>());
            doc.categories.get("Favorites").add(key);
        }
        return doc;
    }
    public String snapshot() throws Exception {
        db.begin(); boolean ok=false;
        try { String s=read().toJson(); ok=true; return s; } finally { db.end(ok); }
    }
    public String apply(FavoritesDocument doc,boolean replace,Backup backup) throws Exception {
        db.begin(); boolean ok=false; int added=0;
        try {
            backup.save(read().toJson());
            if(replace) {
                db.exec("DELETE FROM favorite_page_categories"); db.exec("DELETE FROM favorite_pages"); db.exec("DELETE FROM favorite_categories");
            }
            Map<String,Long> pageIds=new HashMap<>(),categoryIds=new HashMap<>();
            Map<String,Map<String,Object>> oldPages=new HashMap<>();
            for(Map<String,Object> row:db.rows("SELECT id,pageName,username,profilePicUrl,bio,createdAt FROM favorite_pages")) {
                String key=text(row,"username").toLowerCase(Locale.ROOT);
                if(!pageIds.containsKey(key)) { pageIds.put(key,number(row,"id")); oldPages.put(key,row); }
            }
            for(Map<String,Object> row:db.rows("SELECT id,name FROM favorite_categories")) categoryIds.put(text(row,"name"),number(row,"id"));
            for(Map.Entry<String,FavoritesDocument.Page> e:doc.pages.entrySet()) {
                String key=e.getKey(); FavoritesDocument.Page p=e.getValue(); Long id=pageIds.get(key);
                if(id==null) {
                    id=db.insert("INSERT INTO favorite_pages (pageName,username,profilePicUrl,bio,createdAt) VALUES (?,?,?,?,?)",p.name,p.username,p.picture,p.bio,p.createdAt);
                    pageIds.put(key,id); added++;
                } else {
                    Map<String,Object> old=oldPages.get(key);
                    db.exec("UPDATE favorite_pages SET pageName=?,profilePicUrl=?,bio=? WHERE id=?",
                        p.name.isEmpty()?text(old,"pageName"):p.name,
                        p.picture.isEmpty()?text(old,"profilePicUrl"):p.picture,
                        p.bio.isEmpty()?text(old,"bio"):p.bio,id);
                }
            }
            for(Map.Entry<String,LinkedHashSet<String>> e:doc.categories.entrySet()) {
                Long category=categoryIds.get(e.getKey());
                if(category==null) { category=db.insert("INSERT INTO favorite_categories (name) VALUES (?)",e.getKey()); categoryIds.put(e.getKey(),category); }
                for(String key:e.getValue()) db.exec("INSERT OR IGNORE INTO favorite_page_categories (pageId,categoryId) VALUES (?,?)",pageIds.get(key),category);
            }
            ok=true;
            return doc.pages.size()+" hesap, "+doc.categories.size()+" klasör, "+doc.memberships()+" klasör üyeliği aktarıldı.\nYeni eklenen hesap: "+added+".\nÖnceki listenin otomatik yedeği saklandı.";
        } finally { db.end(ok); }
    }
}
