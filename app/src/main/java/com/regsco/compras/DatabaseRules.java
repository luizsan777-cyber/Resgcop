package com.regsco.compras;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

final class DatabaseRules {
    static void upgrade(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            for(String column:new String[]{"barcode TEXT NOT NULL DEFAULT ''","brand TEXT NOT NULL DEFAULT ''","unit TEXT NOT NULL DEFAULT 'un'","store TEXT NOT NULL DEFAULT ''","notes TEXT NOT NULL DEFAULT ''","retail_cents INTEGER NOT NULL DEFAULT 0","wholesale_cents INTEGER NOT NULL DEFAULT 0","wholesale_min INTEGER NOT NULL DEFAULT 0","discount_cents INTEGER NOT NULL DEFAULT 0"}) addColumn(db,"items",column);
            addColumn(db,"sessions","planned INTEGER NOT NULL DEFAULT 0");
            addColumn(db,"sessions","stock_added INTEGER NOT NULL DEFAULT 0");
            for(String table:new String[]{"pantry","basket","shopping_list"}){
                addColumn(db,table,"unit TEXT NOT NULL DEFAULT 'un'");addColumn(db,table,"category TEXT NOT NULL DEFAULT ''");
            }
            addColumn(db,"pantry","minimum INTEGER NOT NULL DEFAULT 0");addColumn(db,"pantry","expiry TEXT NOT NULL DEFAULT ''");addColumn(db,"pantry","notes TEXT NOT NULL DEFAULT ''");
            db.execSQL("CREATE INDEX IF NOT EXISTS items_barcode ON items(barcode)");
            db.execSQL("CREATE INDEX IF NOT EXISTS items_session ON items(session_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS sessions_period ON sessions(year,month,finished)");
            db.execSQL("CREATE INDEX IF NOT EXISTS items_name ON items(name COLLATE NOCASE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS list_pending ON shopping_list(checked,name COLLATE NOCASE)");
            if(!exists(db,"SELECT 1 FROM settings WHERE key='suggested_stores'")) {
                for(String name:new String[]{"Mix Mateus Parque Alvorada","Mix Mateus Avenida Piauí","Carvalho Super","Branquinha","Drogasil","Pague Menos","Drogaria Globo","Extrafarma"})
                    db.execSQL("INSERT OR IGNORE INTO stores(name) VALUES(?)",new Object[]{name});
                db.execSQL("INSERT INTO settings(key,value) VALUES('suggested_stores','1')");
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    static void addColumn(SQLiteDatabase db,String table,String declaration){String name=declaration.split(" ")[0];try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())if(c.getString(1).equals(name))return;}db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+declaration);}
    static boolean exists(SQLiteDatabase db,String query){try(Cursor c=db.rawQuery(query,null)){return c.moveToFirst();}}
    static void validate(SQLiteDatabase db) {
        String[] invalid={
            "SELECT 1 FROM sessions WHERE id<=0 OR length(trim(name))=0 OR month NOT BETWEEN 1 AND 12 OR year NOT BETWEEN 2000 AND 2200 OR finished NOT IN (0,1)",
            "SELECT 1 FROM items WHERE id<=0 OR quantity<=0 OR unit_cents<0 OR typeof(quantity)<>'integer' OR typeof(unit_cents)<>'integer' OR length(trim(name))=0 OR session_id NOT IN (SELECT id FROM sessions) OR typeof(quantity*unit_cents)<>'integer'",
            "SELECT 1 FROM budgets WHERE cents<0 OR typeof(cents)<>'integer' OR month NOT BETWEEN 1 AND 12 OR year NOT BETWEEN 2000 AND 2200",
            "SELECT 1 FROM shopping_list WHERE quantity<=0 OR typeof(quantity)<>'integer' OR checked NOT IN(0,1) OR length(trim(name))=0",
            "SELECT 1 FROM pantry WHERE quantity<0 OR typeof(quantity)<>'integer' OR length(trim(name))=0",
            "SELECT 1 FROM basket WHERE quantity<=0 OR typeof(quantity)<>'integer' OR length(trim(name))=0",
            "SELECT 1 FROM settings WHERE key='theme' AND value NOT IN('0','1','2')"
        };
        for(String q:invalid)if(exists(db,q))throw new IllegalArgumentException("Backup contém dados inválidos. Nada foi substituído.");
        if(exists(db,"SELECT 1 FROM items WHERE discount_cents<0 OR discount_cents>quantity*unit_cents OR wholesale_cents<0 OR wholesale_min<0 OR retail_cents<0"))throw new IllegalArgumentException("Condição de preço inválida no backup");
        if(exists(db,"SELECT 1 FROM pantry WHERE minimum<0")||exists(db,"SELECT 1 FROM sessions WHERE stock_added NOT IN (0,1)"))throw new IllegalArgumentException("Estoque inválido no backup");
        // SQLite SUM raises rather than silently converting an overflowing integer aggregate.
        try(Cursor c=db.rawQuery("SELECT SUM(quantity*unit_cents) FROM items",null)){c.moveToFirst();c.getLong(0);}
    }
}
