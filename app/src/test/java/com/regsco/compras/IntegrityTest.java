package com.regsco.compras;

import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import org.json.JSONObject;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class IntegrityTest {
    MainActivity app;
    @Before public void setup(){app=Robolectric.buildActivity(MainActivity.class).create().get();app.db.execSQL("DELETE FROM items");app.db.execSQL("DELETE FROM sessions");app.db.execSQL("DELETE FROM budgets");app.month=10;app.year=2026;}
    @After public void close(){app.db.close();}
    void purchase(int id,int month,int year,int finished,int qty,long price){app.db.execSQL("INSERT INTO sessions(id,name,store,month,year,finished,created) VALUES(?,'Feira','Loja',?,?,?,0)",new Object[]{id,month,year,finished});app.db.execSQL("INSERT INTO items(session_id,name,category,quantity,unit_cents) VALUES(?,'Arroz','Alimentação',?,?)",new Object[]{id,qty,price});}
    @Test public void budgetReopenAndMonthlySeparation(){
        app.db.execSQL("INSERT INTO budgets VALUES(10,2026,60000)");purchase(1,10,2026,1,2,9000);purchase(2,10,2026,1,1,12000);purchase(3,9,2026,1,1,40000);purchase(4,10,2025,1,1,60000);purchase(5,10,2026,0,1,10000);
        assertEquals(30000,app.spent());assertEquals(30000,app.budget()-app.spent());
        app.db.execSQL("UPDATE sessions SET finished=1 WHERE id=1");assertEquals(30000,app.spent());
        app.db.execSQL("UPDATE sessions SET finished=0 WHERE id=1");assertEquals(12000,app.spent());
        app.db.execSQL("UPDATE items SET unit_cents=13000 WHERE session_id=2");assertEquals(13000,app.spent());
        app.db.execSQL("DELETE FROM items WHERE session_id=2");assertEquals(0,app.spent());
    }
    @Test public void streamingBackupRoundTrip() throws Exception {
        purchase(1,10,2026,1,3,1234);app.db.execSQL("INSERT OR REPLACE INTO settings VALUES('theme','2')");
        ByteArrayOutputStream out=new ByteArrayOutputStream();BackupIO.write(app.db,out);
        File f=File.createTempFile("regsco-test", ".db");f.delete();
        SQLiteDatabase stage=BackupIO.stage(app.db,new ByteArrayInputStream(out.toByteArray()),f);
        app.db.execSQL("UPDATE items SET unit_cents=1");BackupIO.replace(app.db,stage);
        assertEquals(3702,app.spent());assertEquals(2,app.scalar("SELECT value FROM settings WHERE key='theme'",null));stage.close();f.delete();
    }
    @Test public void invalidBackupPreservesLiveData() throws Exception {
        purchase(1,10,2026,1,1,999);JSONObject json=app.exportData();json.getJSONArray("items").getJSONObject(0).put("quantity",-1);
        File f=File.createTempFile("regsco-test", ".db");f.delete();
        try{BackupIO.stage(app.db,new ByteArrayInputStream(json.toString().getBytes("UTF-8")),f);fail("Invalid backup accepted");}catch(IllegalArgumentException expected){}
        assertEquals(999,app.spent());
    }
    @Test public void orphanAndInvalidThemeRollback() throws Exception {
        purchase(1,10,2026,1,1,999);JSONObject json=app.exportData();json.getJSONArray("items").getJSONObject(0).put("session_id",999);
        try{app.restoreData(json);fail("Orphan accepted");}catch(IllegalArgumentException expected){}assertEquals(999,app.spent());
        json=app.exportData();json.getJSONArray("settings").put(new JSONObject().put("key","theme").put("value","broken"));
        try{app.restoreData(json);fail("Theme accepted");}catch(Exception expected){}assertEquals(999,app.spent());
    }
    @Test public void truncatedBackupIsRejected() throws Exception {
        purchase(1,10,2026,1,1,999);String json=app.exportData().toString();File f=File.createTempFile("regsco-test", ".db");f.delete();
        try{BackupIO.stage(app.db,new ByteArrayInputStream(json.substring(0,json.length()-2).getBytes("UTF-8")),f);fail("Truncated backup accepted");}catch(IOException expected){}assertEquals(999,app.spent());
    }
    @Test public void duplicatePurchaseIsOpenAndPreservesOriginal(){purchase(1,10,2026,1,2,500);app.duplicateSession(1);assertEquals(1000,app.spent());assertEquals(1,app.scalar("SELECT COUNT(*) FROM sessions WHERE finished=0",null));assertEquals(2,app.scalar("SELECT COUNT(*) FROM items",null));}

    @Test public void discountIncludedInBudgetAndCsv(){purchase(1,10,2026,1,3,1000);app.db.execSQL("UPDATE items SET discount_cents=125,store='Outra loja'");assertEquals(2875,app.spent());assertTrue(app.reportCsv().contains("28,75"));assertTrue(app.reportCsv().contains("Outra loja"));assertTrue(app.report().contains("28,75"));}
    @Test public void basketConvertsCompatibleUnitsAndIsIdempotent(){
        app.db.execSQL("DELETE FROM pantry");app.db.execSQL("DELETE FROM shopping_list");
        app.db.execSQL("INSERT INTO pantry(name,quantity,unit) VALUES('Arroz',500,'g'),('Arroz',2,'kg'),('Arroz',100,'un')");
        assertEquals(2500,app.missingStock("Arroz",5,"kg"));
        app.mergeMissing("Arroz",2500,"g","Alimentação");app.mergeMissing("Arroz",2500,"g","Alimentação");
        assertEquals(1,app.scalar("SELECT COUNT(*) FROM shopping_list",null));assertEquals(2500,app.scalar("SELECT quantity FROM shopping_list",null));
    }
    @Test public void legacyVersionThreeBackupRestoresWithDefaults() throws Exception {
        purchase(1,10,2026,1,2,999);JSONObject json=app.exportData();json.put("version",3);
        org.json.JSONObject item=json.getJSONArray("items").getJSONObject(0);
        for(String key:new String[]{"barcode","brand","unit","store","notes","retail_cents","wholesale_cents","wholesale_min","discount_cents"})item.remove(key);
        File f=File.createTempFile("regsco-test", ".db");f.delete();SQLiteDatabase stage=BackupIO.stage(app.db,new ByteArrayInputStream(json.toString().getBytes("UTF-8")),f);BackupIO.replace(app.db,stage);assertEquals(1998,app.spent());stage.close();f.delete();
    }
}
