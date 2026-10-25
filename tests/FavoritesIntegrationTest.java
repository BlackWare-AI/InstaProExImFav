package com.instagram.gpt;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class FavoritesIntegrationTest {
    static class JDBC implements FavoritesStore.Backend,AutoCloseable {
        final Connection c; int failAfter=-1;
        JDBC() throws Exception {
            c=DriverManager.getConnection("jdbc:sqlite::memory:");
            exec(FavoritesStore.CREATE_PAGES); exec(FavoritesStore.CREATE_CATEGORIES); exec(FavoritesStore.CREATE_LINKS);
        }
        void fail() throws Exception { if(failAfter==0) throw new java.io.IOException("simulated disk failure"); if(failAfter>0) failAfter--; }
        public void begin() throws Exception { c.setAutoCommit(false); }
        public void end(boolean ok) throws Exception { try { if(ok)c.commit(); else c.rollback(); } finally { c.setAutoCommit(true); } }
        PreparedStatement statement(String sql,Object[] args) throws Exception {
            PreparedStatement s=c.prepareStatement(sql); for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]); return s;
        }
        public void exec(String sql,Object... args) throws Exception { fail(); try(PreparedStatement s=statement(sql,args)){s.executeUpdate();} }
        public long insert(String sql,Object... args) throws Exception { exec(sql,args); try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT last_insert_rowid()")){return r.getLong(1);} }
        public List<Map<String,Object>> rows(String sql,Object... args) throws Exception {
            List<Map<String,Object>> out=new ArrayList<>();
            try(PreparedStatement s=statement(sql,args);ResultSet r=s.executeQuery()) {
                ResultSetMetaData md=r.getMetaData();
                while(r.next()) { Map<String,Object> row=new HashMap<>();for(int i=1;i<=md.getColumnCount();i++)row.put(md.getColumnName(i),r.getObject(i));out.add(row); }
            } return out;
        }
        public void close() throws Exception { c.close(); }
    }
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static void same(FavoritesDocument a,FavoritesDocument b) {
        check(a.categories.equals(b.categories),"categories/memberships changed");check(a.pages.keySet().equals(b.pages.keySet()),"usernames changed");
        for(String k:a.pages.keySet()) { FavoritesDocument.Page x=a.pages.get(k),y=b.pages.get(k);
            check(x.username.equals(y.username)&&x.name.equals(y.name)&&x.picture.equals(y.picture)&&x.bio.equals(y.bio)&&x.createdAt==y.createdAt,"metadata changed"); }
    }
    public static void main(String[] args) throws Exception {
        String text=new String(Files.readAllBytes(Paths.get(args[0])),StandardCharsets.UTF_8);
        FavoritesDocument doc=FavoritesDocument.parse(text);
        check(doc.categories.size()==8,"expected 8 categories");check(doc.pages.size()==16932,"expected 16932 users");check(doc.memberships()==17615,"expected 17615 links");
        same(doc,FavoritesDocument.parse(doc.toJson()));
        try(JDBC db=new JDBC()) {
            FavoritesStore store=new FavoritesStore(db);String[] backup={null};
            store.apply(doc,true,json->backup[0]=json); same(doc,store.read());
            check(backup[0]!=null,"no pre-import backup");
            store.apply(doc,false,json->backup[0]=json);same(doc,store.read());
            check(db.rows("SELECT * FROM favorite_page_categories").size()==17615,"reimport duplicated links");
            System.out.println("PASS SYNTHETIC workload → SQLite: 16932 users / 8 folders / 17615 memberships");
            System.out.println("PASS repeated merge: no duplicate users or memberships");
            FavoritesDocument round=FavoritesDocument.parse(store.snapshot());
            try(JDBC fresh=new JDBC()) { FavoritesStore other=new FavoritesStore(fresh);other.apply(round,true,json->{});same(doc,other.read()); }
            System.out.println("PASS SQLite → export JSON → parse → fresh SQLite, all fields and memberships preserved");
            String before=store.snapshot();
            try { store.apply(doc,true,json->{throw new java.io.IOException("backup denied");});throw new AssertionError("backup failure ignored"); } catch(java.io.IOException expected) {}
            check(before.equals(store.snapshot()),"backup failure modified DB");
            db.failAfter=50;
            try { store.apply(doc,true,json->{});throw new AssertionError("write failure ignored"); } catch(java.io.IOException expected) {}
            db.failAfter=-1;check(before.equals(store.snapshot()),"partial write not rolled back");
            System.out.println("PASS backup failure and mid-import write failure: original database preserved");
            String edge="[{\"name\":\"Boş\",\"items\":[]},{\"name\":\"Özel ' klasör\",\"items\":[{\"pageId\":\"@Test.User\",\"name\":\"Türkçe İıŞş ' \\\"\",\"profilePicUrl\":\"https://example.invalid/a\",\"bio\":\"satır\\nikinci\",\"createdAt\":123},{\"pageId\":\"test.user\",\"name\":\"duplicate\"}]}]";
            FavoritesDocument small=FavoritesDocument.parse(edge);check(small.pages.size()==1&&small.memberships()==1&&small.duplicates==1,"case dedup failed");
            store.apply(small,true,json->backup[0]=json);same(small,store.read());check(FavoritesDocument.parse(backup[0]).pages.size()==16932,"replacement snapshot incomplete");
            check(store.read().categories.containsKey("Boş"),"empty folder lost");
            store.apply(doc,false,json->{});check(store.read().pages.size()==16933,"merge lost existing user");
            System.out.println("PASS empty folders, Unicode/quotes, metadata, case deduplication, replace and merge");
            for(String bad:new String[]{"{}","[]x","{broken","{\"favorite_page_pref\":\"[]x\"}","[{\"name\":12,\"items\":[]}]","[{\"name\":\"X\",\"items\":[{\"pageId\":\"bad/user\"}]}]"}) {
                boolean rejected=false;try { FavoritesDocument.parse(bad); }catch(Exception expected){rejected=true;}check(rejected,"malformed input accepted");
            }
            System.out.println("PASS malformed and unrelated JSON rejected");
        }
        System.out.println("ALL TESTS PASSED (host JVM and SQLite; Android UI/device launch not tested)");
    }
}
