package com.regsco.compras;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import android.database.Cursor;
import java.io.*;
import java.util.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StockSearchTest {
 @Test public void nameSearchRemovesBrandCodeAndPreservesFlavor(){
  assertEquals("polpa acerola",ProductSearch.alternatives("Polpa de Acerola Mirador 400g 7891234567890"));
  assertEquals("polpa acerola",ProductSearch.fromOcr("MIRADOR\nPOLPA DE ACEROLA\n400g\n7891234567890\nR$ 9,00"));
  assertFalse(ProductSearch.valid("7891234567890"));
  assertTrue(ProductSearch.matches("polpa acerola","Polpa de Acerola Fruta Polpa 400g"));
  assertFalse(ProductSearch.matches("polpa acerola","Polpa de Goiaba 400g"));
  assertFalse(ProductSearch.matches("polpa acerola","Jarra Acerola 1L"));
  assertFalse(ProductSearch.matches("leite integral","Leite Desnatado 1L"));
 }
 @Test public void presetNeverDuplicatesOrOverwritesExistingStock(){MainActivity a=Robolectric.buildActivity(MainActivity.class).create().get();try{
  a.db.execSQL("INSERT INTO pantry(name,quantity,unit,image_data) VALUES('arroz',15,'kg','own photo')");
  boolean[] all=new boolean[HouseholdStock.ITEMS.length];Arrays.fill(all,true);assertEquals(14,HouseholdStock.importMissing(a.db,all));assertEquals(0,HouseholdStock.importMissing(a.db,all));
  assertEquals(15,a.scalar("SELECT quantity FROM pantry WHERE name='arroz'",null));assertEquals(15,a.scalar("SELECT COUNT(*) FROM pantry",null));
  assertEquals(1,a.scalar("SELECT in_use FROM pantry WHERE name='Milho para pipoca'",null));
  assertEquals("3 sacos · 1 em uso",HouseholdStock.summary(3,"saco",1,""));
  assertEquals("Quantidade a informar",HouseholdStock.summary(0,"un",0,"Quantidade não informada"));
 }finally{a.db.close();}}
 @Test public void migrationIsAdditiveAndRejectsInvalidOpenedCount(){MainActivity a=Robolectric.buildActivity(MainActivity.class).create().get();try{
  a.db.execSQL("INSERT INTO pantry(name,quantity,unit) VALUES('Café',3,'pacote')");DatabaseRules.upgrade(a.db);assertEquals(3,a.scalar("SELECT quantity FROM pantry",null));assertEquals(0,a.scalar("SELECT in_use FROM pantry",null));
  a.db.execSQL("UPDATE pantry SET in_use=4");try{DatabaseRules.validate(a.db);fail();}catch(IllegalArgumentException expected){}
 }finally{a.db.close();}}
 @Test public void backupRetainsOpenedCountAndOldBackupDefaults()throws Exception{MainActivity a=Robolectric.buildActivity(MainActivity.class).create().get();try{
  a.db.execSQL("INSERT INTO pantry(name,quantity,in_use) VALUES('Arroz',3,1)");ByteArrayOutputStream out=new ByteArrayOutputStream();BackupIO.write(a.db,out);File f=File.createTempFile("stock-backup",".db");f.delete();var staged=BackupIO.stage(a.db,new ByteArrayInputStream(out.toByteArray()),f);try(Cursor c=staged.rawQuery("SELECT in_use FROM pantry",null)){assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));}staged.close();f.delete();
  String legacy=out.toString("UTF-8").replace(",\"in_use\":1","");staged=BackupIO.stage(a.db,new ByteArrayInputStream(legacy.getBytes("UTF-8")),f);try(Cursor c=staged.rawQuery("SELECT in_use FROM pantry",null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}staged.close();f.delete();
 }finally{a.db.close();}}
 @Test public void everyHouseholdProductHasPackPhotoAndNavigationWorks()throws Exception{MainActivity a=Robolectric.buildActivity(MainActivity.class).create().get();try{
  boolean[] all=new boolean[HouseholdStock.ITEMS.length];Arrays.fill(all,true);HouseholdStock.importMissing(a.db,all);
  for(String[] row:HouseholdStock.ITEMS)assertTrue(row[0],a.getResources().getIdentifier("pack_"+ProductPhotos.key(row[0]),"drawable",a.getPackageName())!=0);
  for(int t=0;t<3;t++){a.theme=t;a.pantry();assertEquals(1,a.stockPage);ScreensTest.capture(a,"stock-hub-"+t);a.pantryResults("",false);assertEquals(2,a.stockPage);ScreensTest.capture(a,"stock-list-"+t);a.pantryForm(1);assertEquals(3,a.stockPage);a.onBackPressed();assertEquals(2,a.stockPage);a.onBackPressed();assertEquals(1,a.stockPage);}
 }finally{a.db.close();}}
 @Test public void liveOfferCarriesItsOwnImageAndRejectsUnsafeImages()throws Exception{
  var hit=new org.json.JSONObject("{\"name\":\"Polpa Acerola 400g\",\"objectID\":\"p\",\"market_id\":\"a\",\"for_sale\":true,\"sale_price\":4.99,\"image\":\"https://ik.imagekit.io/catalog/acerola.png\"}");
  var offers=OnlinePricesActivity.parseMix(new org.json.JSONObject().put("hits",new org.json.JSONArray().put(hit)),"a","Mix");assertEquals("https://ik.imagekit.io/catalog/acerola.png",offers.get(0).image);
  assertEquals("",RemotePhotos.safe("http://ik.imagekit.io/x"));assertEquals("",RemotePhotos.safe("https://ik.imagekit.io.evil.example/x"));assertEquals("",RemotePhotos.safe("https://127.0.0.1/x"));
 }
}
