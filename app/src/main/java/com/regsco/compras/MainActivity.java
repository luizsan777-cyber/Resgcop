package com.regsco.compras;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    SQLiteDatabase db;
    LinearLayout screen;
    int theme=0;
    int month=Calendar.getInstance().get(Calendar.MONTH)+1, year=Calendar.getInstance().get(Calendar.YEAR);
    final String[] themes={"Verde Natureza","Claro Rosa","Escuro Nebulosa"};
    int bg(){return theme==0?0xfffff8e8:theme==1?0xffffedf5:0xff1c1127;}
    int fg(){return theme==2?Color.WHITE:theme==1?0xff59274c:0xff1e5037;}
    String money(long cents){return NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(cents/100.0);}
    long parse(String value){try {String v=value.trim().replace("R$","").replace(" ",""); if(v.contains(","))v=v.replace(".","").replace(",",".");return Math.round(Double.parseDouble(v)*100);}catch(Exception e){return -1;}}
    @Override public void onCreate(Bundle state){super.onCreate(state);
        db=openOrCreateDatabase("regsco.db",MODE_PRIVATE,null);
        db.execSQL("CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY,value TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,store TEXT NOT NULL,month INTEGER NOT NULL,year INTEGER NOT NULL,finished INTEGER NOT NULL DEFAULT 0,created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS items(id INTEGER PRIMARY KEY AUTOINCREMENT,session_id INTEGER NOT NULL,name TEXT NOT NULL,category TEXT NOT NULL,quantity INTEGER NOT NULL,unit_cents INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS budgets(month INTEGER NOT NULL,year INTEGER NOT NULL,cents INTEGER NOT NULL,PRIMARY KEY(month,year))");
        try(Cursor c=db.rawQuery("SELECT value FROM settings WHERE key='theme'",null)){if(c.moveToFirst())theme=Integer.parseInt(c.getString(0));}
        home();
    }
    int accent(){return theme==0?0xff43785c:theme==1?0xffc04c86:0xffad79e8;}
    int panel(){return theme==0?0xffffffff:theme==1?0xfffffafc:0xff30203f;}
    int muted(){return theme==2?0xffd5c9e1:theme==1?0xff815d73:0xff65806f;}
    int dp(int v){return (int)(getResources().getDisplayMetrics().density*v+0.5f);}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    void margin(View v,int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(top),0,dp(bottom));v.setLayoutParams(p);}
    TextView label(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(fg());v.setPadding(dp(12),dp(12),dp(12),dp(12));return v;}
    void button(String title,Runnable click){
        TextView b=label(title+"   ›",16);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setGravity(Gravity.CENTER_VERTICAL);b.setMinHeight(dp(56));b.setPadding(dp(20),dp(14),dp(20),dp(14));
        b.setBackground(shape(panel(),18));b.setElevation(dp(2));margin(b,6,6);
        screen.addView(b);b.setOnClickListener(v->click.run());
    }
    void section(String title){TextView v=label(title,18);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);margin(v,18,4);screen.addView(v);}
    void card(String heading,String value,String detail){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(18),dp(15),dp(18),dp(15));wrap.setBackground(shape(panel(),22));wrap.setElevation(dp(3));margin(wrap,8,8);
        TextView h=label(heading,14);h.setTextColor(muted());wrap.addView(h);
        TextView amount=label(value,27);amount.setTypeface(Typeface.DEFAULT,Typeface.BOLD);amount.setTextColor(accent());wrap.addView(amount);
        TextView sub=label(detail,13);sub.setTextColor(muted());wrap.addView(sub);screen.addView(wrap);
    }
    void page(String title){
        getWindow().setStatusBarColor(theme==2?0xff1c1127:accent());
        getWindow().setNavigationBarColor(bg());
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(20),dp(24),dp(20),dp(36));screen.setBackgroundColor(bg());
        scroll.addView(screen);setContentView(scroll);
        TextView brand=label("R E G S C O",13);brand.setTextColor(accent());brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);screen.addView(brand);
        TextView h=label(title,29);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);screen.addView(h);
    }
    EditText field(String hint,String value,boolean numeric){
        EditText e=new EditText(this);e.setHint(hint);e.setText(value);
        e.setTextColor(fg());e.setHintTextColor(muted());
        e.setPadding(dp(18),dp(14),dp(18),dp(14));e.setBackground(shape(panel(),16));
        if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        margin(e,7,7);screen.addView(e);return e;
    }
    void message(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    long scalar(String sql,String[] args){try(Cursor c=db.rawQuery(sql,args)){return c.moveToFirst()?c.getLong(0):0;}}
    String period(){return String.format(new Locale("pt","BR"),"%02d/%d",month,year);}
    long spent(){return scalar("SELECT COALESCE(SUM(i.quantity*i.unit_cents),0) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=?",new String[]{""+month,""+year});}
    long budget(){return scalar("SELECT COALESCE(cents,0) FROM budgets WHERE month=? AND year=?",new String[]{""+month,""+year});}
    void home(){
        page("Suas compras");
        screen.addView(label("Organize o mês com tranquilidade • "+period(),15));
        long b=budget(),spentAmount=spent();
        card("SALDO DISPONÍVEL",money(b-spentAmount),"Orçamento: "+money(b)+"   •   Gasto: "+money(spentAmount));
        if(b>0){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress((int)Math.min(100,spentAmount*100/b));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent()));margin(bar,8,8);screen.addView(bar);}
        if(b>0&&spentAmount>b)screen.addView(label("Atenção: orçamento ultrapassado!",16));
        section("Vamos às compras?");
        button("＋  Iniciar nova compra",()->newSession());
        button("▤  Minhas compras e histórico",()->sessions());
        section("Planejamento");
        button("◉  Ajustar orçamento mensal",()->editBudget());
        button("▦  Escolher mês de referência",()->choosePeriod());
        section("Mais opções");
        button("↗  Compartilhar relatório",()->share());
        button("✦  Personalizar aparência",()->themePage());
        screen.addView(label("Seus dados ficam armazenados neste aparelho.",12));
    }
    void choosePeriod(){page("Mês de referência");EditText m=field("Mês (1-12)",""+month,true),y=field("Ano",""+year,true);button("Aplicar",()->{try{int mm=Integer.parseInt(m.getText().toString()),yy=Integer.parseInt(y.getText().toString());if(mm<1||mm>12||yy<2000||yy>2200)throw new Exception();month=mm;year=yy;home();}catch(Exception e){message("Informe mês e ano válidos");}});button("Voltar",()->home());}
    void editBudget(){page("Orçamento de "+period());EditText e=field("Orçamento em R$",money(budget()).replace("R$","").trim(),true);button("Salvar",()->{long c=parse(e.getText().toString());if(c<0){message("Valor inválido");return;}db.execSQL("INSERT OR REPLACE INTO budgets(month,year,cents) VALUES(?,?,?)",new Object[]{month,year,c});home();});button("Voltar",()->home());}
    void newSession(){page("Nova compra");EditText name=field("Nome (ex.: Feira do mês)","",false),store=field("Supermercado ou loja","",false);button("Criar compra",()->{String n=name.getText().toString().trim(),st=store.getText().toString().trim();if(n.isEmpty()){message("Informe o nome da compra");return;}android.content.ContentValues v=new android.content.ContentValues();v.put("name",n);v.put("store",st);v.put("month",month);v.put("year",year);v.put("created",System.currentTimeMillis());v.put("finished",0);long id=db.insert("sessions",null,v);if(id>0)editSession(id);else message("Erro ao criar compra");});button("Cancelar",()->home());}
    void sessions(){page("Compras");try(Cursor c=db.rawQuery("SELECT s.id,s.name,s.store,s.month,s.year,s.finished,COALESCE(SUM(i.quantity*i.unit_cents),0) FROM sessions s LEFT JOIN items i ON i.session_id=s.id GROUP BY s.id ORDER BY s.id DESC",null)){while(c.moveToNext()){long id=c.getLong(0);String title=c.getString(1)+" • "+c.getString(2)+"\n"+c.getInt(3)+"/"+c.getInt(4)+" • "+(c.getInt(5)==1?"Finalizada":"Em andamento")+" • "+money(c.getLong(6));button(title,()->editSession(id));}}button("Voltar",()->home());}
    void editSession(long id){try(Cursor c=db.rawQuery("SELECT name,store,month,year,finished FROM sessions WHERE id=?",new String[]{""+id})){if(!c.moveToFirst()){sessions();return;}String name=c.getString(0),store=c.getString(1);int m=c.getInt(2),y=c.getInt(3),done=c.getInt(4);page(name);screen.addView(label(store+" • "+m+"/"+y+" • "+(done==1?"Finalizada":"Em andamento"),17));long sum=scalar("SELECT COALESCE(SUM(quantity*unit_cents),0) FROM items WHERE session_id=?",new String[]{""+id});screen.addView(label("Total da compra: "+money(sum),21));
        try(Cursor items=db.rawQuery("SELECT id,name,category,quantity,unit_cents FROM items WHERE session_id=? ORDER BY id DESC",new String[]{""+id})){while(items.moveToNext()){long itemId=items.getLong(0);String itemName=items.getString(1);int qty=items.getInt(3);long unit=items.getLong(4);screen.addView(label(itemName+" • "+items.getString(2)+"\n"+qty+" × "+money(unit)+" = "+money(qty*unit),16));button("Excluir "+itemName,()->new AlertDialog.Builder(this).setMessage("Excluir este item?").setPositiveButton("Excluir",(d,w)->{db.execSQL("DELETE FROM items WHERE id=?",new Object[]{itemId});editSession(id);}).setNegativeButton("Cancelar",null).show());}}
        if(done==0){button("Adicionar produto",()->addItem(id));button("Finalizar compra",()->new AlertDialog.Builder(this).setMessage("Finalizar compra e contabilizar no orçamento de "+m+"/"+y+"?").setPositiveButton("Finalizar",(d,w)->{db.execSQL("UPDATE sessions SET finished=1 WHERE id=?",new Object[]{id});editSession(id);}).setNegativeButton("Cancelar",null).show());}
        else button("Reabrir para editar",()->{db.execSQL("UPDATE sessions SET finished=0 WHERE id=?",new Object[]{id});editSession(id);});
        button("Excluir compra",()->new AlertDialog.Builder(this).setMessage("Excluir a compra e todos os seus produtos?").setPositiveButton("Excluir",(d,w)->{db.beginTransaction();try{db.execSQL("DELETE FROM items WHERE session_id=?",new Object[]{id});db.execSQL("DELETE FROM sessions WHERE id=?",new Object[]{id});db.setTransactionSuccessful();}finally{db.endTransaction();}sessions();}).setNegativeButton("Cancelar",null).show());button("Voltar ao histórico",()->sessions());}}
    void addItem(long id){page("Adicionar produto");EditText name=field("Produto","",false),category=field("Categoria (ex.: Alimentos)","",false),quantity=field("Quantidade","1",true),price=field("Preço unitário (R$)","",true);button("Salvar produto",()->{String n=name.getText().toString().trim();long cents=parse(price.getText().toString());int q;try{q=Integer.parseInt(quantity.getText().toString());}catch(Exception e){q=0;}if(n.isEmpty()||q<=0||cents<0){message("Preencha produto, quantidade e preço válidos");return;}db.execSQL("INSERT INTO items(session_id,name,category,quantity,unit_cents) VALUES(?,?,?,?,?)",new Object[]{id,n,category.getText().toString().trim(),q,cents});editSession(id);});button("Cancelar",()->editSession(id));}
    String report(){StringBuilder b=new StringBuilder("REGSCO - RELATÓRIO "+period()+"\n");b.append("Orçamento: ").append(money(budget())).append("\nGasto finalizado: ").append(money(spent())).append("\nSaldo: ").append(money(budget()-spent())).append("\n\n");try(Cursor c=db.rawQuery("SELECT s.name,s.store,i.name,i.quantity,i.unit_cents FROM sessions s JOIN items i ON i.session_id=s.id WHERE s.finished=1 AND s.month=? AND s.year=? ORDER BY s.id,i.id",new String[]{""+month,""+year})){while(c.moveToNext())b.append(c.getString(0)).append(" / ").append(c.getString(1)).append(": ").append(c.getString(2)).append(" (").append(c.getInt(3)).append(" × ").append(money(c.getLong(4))).append(")\n");}return b.toString();}
    void share(){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,report());startActivity(Intent.createChooser(i,"Compartilhar relatório"));}
    void themePage(){page("Temas");for(int i=0;i<themes.length;i++){final int n=i;button(themes[i],()->{theme=n;db.execSQL("INSERT OR REPLACE INTO settings(key,value) VALUES('theme',?)",new Object[]{""+n});themePage();});}button("Voltar",()->home());}
    @Override protected void onDestroy(){if(db!=null&&db.isOpen())db.close();super.onDestroy();}
}
