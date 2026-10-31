package com.regsco.compras;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.ByteArrayOutputStream;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class ScreensTest {
    @Test public void android15ScreensAcceptPopulatedDatabase() throws Exception {
        MainActivity a=Robolectric.buildActivity(MainActivity.class).create().get();
        try{
            a.db.execSQL("INSERT INTO sessions(id,name,store,month,year,finished,created) VALUES(1,'Feira','Loja principal',10,2026,1,1)");
            a.db.execSQL("INSERT INTO items(session_id,name,category,quantity,unit_cents,discount_cents,unit,brand,store) VALUES(1,'Café','Farmácia',2,1000,100,'un','250 g','Loja do produto')");
            a.month=10;a.year=2026;
            a.home();a.categoryReport();a.storeSummary();a.showPriceResults("Café");a.showComparison("Café");a.topProducts();a.sixMonthReport();a.compareMonths();a.pharmacyPage();a.sessions();a.historyPage("Café","Loja do produto","Farmácia","10","2026",0);a.editSession(1);a.editItem(1,1);a.addItem(1);a.shoppingList();a.basketPage();a.pantry();a.pantryForm(0);a.lowStock();a.settingsPage();a.reportsPage();a.backupPage();
            assertEquals(1900,a.spent());assertTrue(a.reportCsv().contains("19,00"));
            ByteArrayOutputStream out=new ByteArrayOutputStream();a.writePdf(a.report(),out);assertTrue(new String(out.toByteArray(),0,4,"US-ASCII").equals("%PDF"));
        }finally{a.db.close();}
    }
    @Test public void allThemesRenderDashboard() throws Exception {
        MainActivity a=Robolectric.buildActivity(MainActivity.class).create().get();
        try{a.month=10;a.year=2026;a.db.execSQL("INSERT OR REPLACE INTO budgets VALUES(10,2026,60000)");a.db.execSQL("INSERT INTO sessions(id,name,store,month,year,finished,created) VALUES(123,'Compra de teste','Loja de teste',10,2026,1,1)");a.db.execSQL("INSERT INTO items(session_id,name,category,quantity,unit_cents) VALUES(123,'Produto de teste','Alimentação',1,32540)");for(int theme=0;theme<3;theme++){a.theme=theme;a.home();assertNotNull(a.screen);assertTrue(a.screen.getChildCount()>10);capture(a,"theme-"+theme);((android.widget.ScrollView)a.screen.getParent()).scrollTo(0,a.dp(300));capture(a,"scrolled-"+theme);a.themePage();capture(a,"settings-"+theme);a.db.execSQL("INSERT INTO pantry(name,quantity,unit) VALUES('Arroz',3,'kg')");a.pantry();capture(a,"pantry-"+theme);a.db.execSQL("DELETE FROM pantry");}}finally{a.db.close();}
    }
    static void capture(MainActivity a,String name)throws Exception{
        android.view.View root=a.getWindow().getDecorView();int width=a.dp(393),height=a.dp(851);root.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(height,android.view.View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(width,height,android.graphics.Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(image));java.io.File folder=new java.io.File("build/theme-previews");folder.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,name+".png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}image.recycle();
    }
}
