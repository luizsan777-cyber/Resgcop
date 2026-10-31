package com.regsco.compras;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Streaming, transactional backups; reads versions 1–5 without loading the file in memory. */
final class BackupIO {
    static final String[] TABLES={"settings","sessions","items","budgets","shopping_list","pantry","basket","stores"};
    static void write(SQLiteDatabase db,OutputStream stream) throws Exception {
        if(stream==null)throw new IOException("Não foi possível abrir o arquivo");
        db.beginTransaction();
        try(JsonWriter out=new JsonWriter(new OutputStreamWriter(stream,StandardCharsets.UTF_8))){
            out.beginObject().name("format").value("regsco-backup").name("version").value(5);
            for(String table:TABLES){
                out.name(table).beginArray();
                try(Cursor c=db.rawQuery("SELECT * FROM "+table,null)){
                    while(c.moveToNext()){
                        out.beginObject();
                        for(int n=0;n<c.getColumnCount();n++){
                            out.name(c.getColumnName(n));
                            if(c.isNull(n))out.nullValue();
                            else if(c.getType(n)==Cursor.FIELD_TYPE_INTEGER)out.value(c.getLong(n));
                            else out.value(c.getString(n));
                        }
                        out.endObject();
                    }
                }
                out.endArray();
            }
            out.endObject();out.flush();db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    static SQLiteDatabase stage(SQLiteDatabase live,InputStream input,File file) throws Exception {
        if(input==null)throw new IOException("Não foi possível ler o arquivo");
        SQLiteDatabase staged=SQLiteDatabase.openOrCreateDatabase(file,null);
        try {
            for(String table:TABLES)try(Cursor c=live.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){if(!c.moveToFirst())throw new IOException("Tabela ausente");staged.execSQL(c.getString(0));}
            HashSet<String> found=new HashSet<>();String format="";int version=0;
            staged.beginTransaction();
            try(JsonReader in=new JsonReader(new InputStreamReader(input,StandardCharsets.UTF_8))){
                in.beginObject();
                while(in.hasNext()){
                    String key=in.nextName();
                    if(!found.add(key))throw new IOException("Campo duplicado no backup");
                    if(key.equals("format")){format=in.nextString();continue;}
                    if(key.equals("version")){version=in.nextInt();continue;}
                    if(!Arrays.asList(TABLES).contains(key))throw new IOException("Campo desconhecido: "+key);
                    Map<String,String> columns=new HashMap<>();
                    try(Cursor c=staged.rawQuery("PRAGMA table_info("+key+")",null)){while(c.moveToNext())columns.put(c.getString(1),c.getString(2));}
                    in.beginArray();
                    while(in.hasNext()){
                        ContentValues row=new ContentValues();HashSet<String> rowKeys=new HashSet<>();in.beginObject();
                        while(in.hasNext()){
                            String col=in.nextName();
                            if(!columns.containsKey(col)||!rowKeys.add(col))throw new IOException("Coluna inválida: "+col);
                            if(in.peek()==JsonToken.NULL){in.nextNull();row.putNull(col);}
                            else if(columns.get(col).equalsIgnoreCase("INTEGER"))row.put(col,Long.parseLong(in.nextString()));
                            else row.put(col,in.nextString());
                        }
                        in.endObject();
                        // Missing NOT NULL columns or duplicate identifiers abort staging.
                        staged.insertOrThrow(key,null,row);
                    }
                    in.endArray();
                }
                in.endObject();
                if(in.peek()!=JsonToken.END_DOCUMENT)throw new IOException("Conteúdo inesperado após o backup");
                if(!format.equals("regsco-backup")||version<1||version>5)throw new IOException("Versão de backup incompatível");
                for(String table:TABLES)if(!found.contains(table)&&!(version<2&&table.equals("basket"))&&!(version<3&&table.equals("stores")))throw new IOException("Backup incompleto: "+table);
                DatabaseRules.validate(staged);staged.setTransactionSuccessful();
            }finally{staged.endTransaction();}
            return staged;
        }catch(Exception e){staged.close();file.delete();throw e;}
    }
    static void replace(SQLiteDatabase live,SQLiteDatabase staged){
        live.beginTransaction();
        try{
            for(int t=TABLES.length-1;t>=0;t--)live.delete(TABLES[t],null,null);
            for(String table:TABLES)try(Cursor c=staged.rawQuery("SELECT * FROM "+table,null)){
                while(c.moveToNext()){
                    ContentValues row=new ContentValues();
                    for(int n=0;n<c.getColumnCount();n++)if(c.isNull(n))row.putNull(c.getColumnName(n));else if(c.getType(n)==Cursor.FIELD_TYPE_INTEGER)row.put(c.getColumnName(n),c.getLong(n));else row.put(c.getColumnName(n),c.getString(n));
                    live.insertOrThrow(table,null,row);
                }
            }
            DatabaseRules.validate(live);live.setTransactionSuccessful();
        }finally{live.endTransaction();}
    }
}
