package com.instagram.gpt;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;

public final class AndroidFavoritesBackend implements FavoritesStore.Backend, java.io.Closeable {
    private final SQLiteOpenHelper helper;
    private final SQLiteDatabase db;
    private final Map<String,SQLiteStatement> statements=new HashMap<>();
    public AndroidFavoritesBackend(Context context) {
        helper=new SQLiteOpenHelper(context.getApplicationContext(),"favorites.db",null,1) {
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(FavoritesStore.CREATE_PAGES); db.execSQL(FavoritesStore.CREATE_CATEGORIES); db.execSQL(FavoritesStore.CREATE_LINKS);
                db.execSQL("INSERT INTO favorite_categories (name) VALUES (?)",new Object[]{"Favorites"});
            }
            public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { throw new IllegalStateException("Desteklenmeyen favori veritabanı sürümü."); }
        };
        db=helper.getWritableDatabase();
    }
    public void begin() { db.beginTransaction(); }
    public void end(boolean ok) { try { if(ok) db.setTransactionSuccessful(); } finally { db.endTransaction(); } }
    private SQLiteStatement statement(String sql,Object[] args) {
        SQLiteStatement s=statements.get(sql);
        if(s==null) { s=db.compileStatement(sql); statements.put(sql,s); }
        s.clearBindings();
        for(int i=0;i<args.length;i++) {
            Object v=args[i]; if(v==null) s.bindNull(i+1); else if(v instanceof Number) s.bindLong(i+1,((Number)v).longValue()); else s.bindString(i+1,v.toString());
        }
        return s;
    }
    public void exec(String sql,Object... args) { statement(sql,args).execute(); }
    public long insert(String sql,Object... args) {
        long id=statement(sql,args).executeInsert(); if(id<0) throw new IllegalStateException("Favori kaydı yazılamadı."); return id;
    }
    public List<Map<String,Object>> rows(String sql,Object... args) {
        String[] strings=new String[args.length]; for(int i=0;i<args.length;i++) strings[i]=String.valueOf(args[i]);
        ArrayList<Map<String,Object>> out=new ArrayList<>();
        try(Cursor cursor=db.rawQuery(sql,strings)) {
            while(cursor.moveToNext()) {
                Map<String,Object> row=new HashMap<>();
                for(int i=0;i<cursor.getColumnCount();i++) row.put(cursor.getColumnName(i),cursor.isNull(i)?null:cursor.getType(i)==Cursor.FIELD_TYPE_INTEGER?Long.valueOf(cursor.getLong(i)):cursor.getString(i));
                out.add(row);
            }
        }
        return out;
    }
    public void close() { for(SQLiteStatement s:statements.values()) s.close(); helper.close(); }
}
