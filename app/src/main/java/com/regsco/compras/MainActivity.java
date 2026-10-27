package com.regsco.compras;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    SQLiteDatabase db;
    LinearLayout screen;
    int theme=0;
    final String[] names={"Verde Natureza","Claro Rosa","Escuro Nebulosa"};
    int bg(){return theme==0?0xfffff8e8:theme==1?0xffffedf5:0xff1c1127;}
    int fg(){return theme==2?Color.WHITE:theme==1?0xff59274c:0xff1e5037;}
    String money(long cents){return NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(cents/100.0);}
    long parse(String value){try{return Math.round(Double.parseDouble(value.replace(".","").replace(",","."))*100);}catch(Exception e){return -1;}}
    @Override public void onCreate(Bundle state){super.onCreate(state);
        db=openOrCreateDatabase("regsco.db",MODE_PRIVATE,null);
        db.execSQL("CREATE TABLE IF NOT EXISTS purchases(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,category TEXT NOT NULL,store TEXT NOT NULL,amount INTEGER NOT NULL,created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY,value TEXT)");
        try(Cursor c=db.rawQuery("SELECT value FROM settings WHERE key='theme'",null)){if(c.moveToFirst())theme=Integer.parseInt(c.getString(0));}
        home();
    }
    TextView label(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(fg());v.setPadding(12,14,12,14);return v;}
    void button(String title,Runnable click){Button b=new Button(this);b.setText(title);b.setAllCaps(false);screen.addView(b);b.setOnClickListener(v->click.run());}
    void page(String title){ScrollView scroll=new ScrollView(this);screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);screen.setPadding(20,28,20,20);screen.setBackgroundColor(bg());scroll.addView(screen);setContentView(scroll);screen.addView(label("RegsCo  •  "+title,25));}
    void home(){page("Início");long sum=total();screen.addView(label("Total registrado: "+money(sum),23));button("Nova compra",()->newPurchase());button("Histórico e relatório",()->history());button("Temas",()->themes());button("Voltar ao início",()->home());screen.addView(label("Versão inicial de teste. Funcionalidades avançadas ainda em desenvolvimento.",14));}
    long total(){try(Cursor c=db.rawQuery("SELECT COALESCE(SUM(amount),0) FROM purchases",null)){c.moveToFirst();return c.getLong(0);}}
    void newPurchase(){page("Nova compra");EditText name=new EditText(this);name.setHint("Nome da compra");screen.addView(name);EditText category=new EditText(this);category.setHint("Categoria (ex.: Feira, Farmácia)");screen.addView(category);EditText store=new EditText(this);store.setHint("Estabelecimento");screen.addView(store);EditText amount=new EditText(this);amount.setHint("Valor total em R$ (ex.: 125,90)");amount.setInputType(8194);screen.addView(amount);
        button("Salvar compra",()->{long cents=parse(amount.getText().toString());if(name.getText().toString().trim().isEmpty()||cents<0){Toast.makeText(this,"Informe nome e valor válido",Toast.LENGTH_LONG).show();return;}
            android.content.ContentValues values=new android.content.ContentValues();values.put("name",name.getText().toString().trim());values.put("category",category.getText().toString().trim());values.put("store",store.getText().toString().trim());values.put("amount",cents);values.put("created",System.currentTimeMillis());db.insert("purchases",null,values);home();});
        button("Cancelar",()->home());
    }
    String report(){StringBuilder b=new StringBuilder("RELATÓRIO DE GASTOS - RegsCo\n\n");try(Cursor c=db.rawQuery("SELECT name,category,store,amount FROM purchases ORDER BY id DESC",null)){while(c.moveToNext())b.append("• ").append(c.getString(0)).append(" (").append(c.getString(1)).append(") - ").append(c.getString(2)).append(": ").append(money(c.getLong(3))).append("\n");}return b.append("\nTOTAL: ").append(money(total())).toString();}
    void history(){page("Histórico");screen.addView(label(report(),16));button("Compartilhar relatório pelo WhatsApp ou outros apps",()->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,report());startActivity(Intent.createChooser(i,"Compartilhar gastos"));});button("Voltar",()->home());}
    void themes(){page("Temas");for(int i=0;i<names.length;i++){final int index=i;button(names[i],()->{theme=index;db.execSQL("INSERT OR REPLACE INTO settings(key,value) VALUES('theme',?)",new Object[]{""+index});themes();});}button("Voltar",()->home());}
}
