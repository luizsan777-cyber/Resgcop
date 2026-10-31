package com.regsco.compras;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.app.AlertDialog;
import android.widget.*;
import java.util.*;

final class HouseholdStock {
    static final String[][] ITEMS={
        {"Arroz","3","saco","1"},{"Macarrão","3","pacote","0"},{"Extrato de tomate","1","un","0"},
        {"Café","3","pacote","0"},{"Óleo","4","garrafa","0"},{"Leite","1","caixa","0"},
        {"Massa de cuscuz","2","pacote","0"},{"Açúcar","5","pacote","0"},{"Feijão","5","pacote","0"},
        {"Milho para pipoca","1","pacote","1"},{"Farinha de mandioca","2","pacote","0"},
        {"Farinha branca","1","pacote","1"},{"Sal","1","pacote","1"},{"Pimenta","0","un","0"},{"Sardinha","2","lata","0"}
    };
    static String identity(String name){String key=ProductPhotos.key(name);return key.isEmpty()?ProductSearch.normal(name):key;}
    static Set<String> existing(SQLiteDatabase db){Set<String> names=new HashSet<>();try(Cursor c=db.rawQuery("SELECT name FROM pantry",null)){while(c.moveToNext())names.add(identity(c.getString(0)));}return names;}
    static String summary(long q,String unit,long opened,String notes){
        if(q==0&&"Quantidade não informada".equals(notes))return "Quantidade a informar";
        String suffix=q==1?unit:unit.equals("un")?"un":unit.equals("kg")||unit.equals("g")||unit.equals("L")||unit.equals("mL")?unit:unit.endsWith("s")?unit:unit+"s";
        return q+" "+suffix+(opened>0?" · "+opened+" em uso":"");
    }
    static int importMissing(SQLiteDatabase db,boolean[] selected){int added=0;db.beginTransaction();try{Set<String> existing=existing(db);for(int i=0;i<ITEMS.length;i++){String[] row=ITEMS[i];if(!selected[i]||!existing.add(identity(row[0])))continue;db.execSQL("INSERT INTO pantry(name,quantity,unit,in_use,notes) VALUES(?,?,?,?,?)",new Object[]{row[0],Long.parseLong(row[1]),row[2],Long.parseLong(row[3]),row[0].equals("Pimenta")?"Quantidade não informada":""});added++;}DatabaseRules.validate(db);db.setTransactionSuccessful();return added;}finally{db.endTransaction();}}
    static void review(MainActivity a){a.page("Cadastrar lista no estoque");a.stockPage=4;a.screen.addView(a.label("Confira sua lista de casa. Produtos já cadastrados serão preservados, inclusive quantidades e fotos. As quantidades incluem as embalagens em uso.",14));
        Set<String> existing=existing(a.db);boolean[] selected=new boolean[ITEMS.length];
        for(int i=0;i<ITEMS.length;i++){final int index=i;String[] row=ITEMS[i];boolean exists=existing.contains(identity(row[0]));CheckBox box=new CheckBox(a);box.setTextColor(a.fg());box.setText(row[0]+" — "+summary(Long.parseLong(row[1]),row[2],Long.parseLong(row[3]),row[0].equals("Pimenta")?"Quantidade não informada":"")+(exists?"\nJá cadastrado • será mantido":""));box.setChecked(!exists);box.setEnabled(!exists);box.setMinHeight(a.dp(52));selected[i]=!exists;box.setOnCheckedChangeListener((b,v)->selected[index]=v);a.screen.addView(box);}
        a.primaryButton("Adicionar os selecionados",()->{int count=importMissing(a.db,selected);a.message(count+" produtos adicionados. Toque em cada um para ajustar.");a.pantryResults("",false);});
        a.button("Voltar ao estoque",()->a.pantry());
    }
}
