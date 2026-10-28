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
import java.io.*;
import org.json.*;

public class MainActivity extends Activity {
    SQLiteDatabase db;
    static final int CAMERA_SCAN=704;
    EditText scanNameTarget,scanPriceTarget;
    android.net.Uri scanPhotoUri;
    LinearLayout screen;
    int theme=0;
    int month=Calendar.getInstance().get(Calendar.MONTH)+1, year=Calendar.getInstance().get(Calendar.YEAR);
    final String[] themes={"Verde Natureza","Rosa Claro","Roxo Nebulosa"};
    int bg(){return ThemeSkin.background(theme);}
    int fg(){return ThemeSkin.text(theme);}
    String money(long cents){return NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(java.math.BigDecimal.valueOf(cents,2));}
    long parse(String value){return Money.parse(value);}
    @Override public void onCreate(Bundle state){super.onCreate(state);
        db=openOrCreateDatabase("regsco.db",MODE_PRIVATE,null);
        db.execSQL("CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY,value TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,store TEXT NOT NULL,month INTEGER NOT NULL,year INTEGER NOT NULL,finished INTEGER NOT NULL DEFAULT 0,created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS items(id INTEGER PRIMARY KEY AUTOINCREMENT,session_id INTEGER NOT NULL,name TEXT NOT NULL,category TEXT NOT NULL,quantity INTEGER NOT NULL,unit_cents INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS budgets(month INTEGER NOT NULL,year INTEGER NOT NULL,cents INTEGER NOT NULL,PRIMARY KEY(month,year))");
        db.execSQL("CREATE TABLE IF NOT EXISTS shopping_list(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,quantity INTEGER NOT NULL DEFAULT 1,checked INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS pantry(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,quantity INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS basket(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,quantity INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS stores(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE)");
        try(Cursor c=db.rawQuery("SELECT value FROM settings WHERE key='theme'",null)){if(c.moveToFirst())theme=Integer.parseInt(c.getString(0));}
        DatabaseRules.upgrade(db);
        home();
    }
    int accent(){return ThemeSkin.accent(theme);}
    int panel(){return ThemeSkin.surface(theme);}
    int muted(){return ThemeSkin.secondary(theme);}
    int dp(int v){return (int)(getResources().getDisplayMetrics().density*v+0.5f);}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    void margin(View v,int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(top),0,dp(bottom));v.setLayoutParams(p);}
    TextView label(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));v.setTextColor(fg());v.setPadding(dp(12),dp(12),dp(12),dp(12));return v;}
    void button(String title,Runnable click){
        TextView b=label(title+"   ›",16);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setGravity(Gravity.CENTER_VERTICAL);b.setMinHeight(dp(56));b.setPadding(dp(20),dp(14),dp(20),dp(14));
        b.setBackground(ThemeSkin.card(theme,dp(18)));b.setElevation(dp(1));b.setClickable(true);b.setFocusable(true);margin(b,6,6);
        screen.addView(b);b.setOnClickListener(v->click.run());
    }
    void section(String title){TextView v=label(title,18);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);margin(v,18,4);screen.addView(v);}
    void card(String heading,String value,String detail){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(18),dp(15),dp(18),dp(15));wrap.setBackground(ThemeSkin.card(theme,dp(22)));wrap.setElevation(dp(3));margin(wrap,8,8);
        TextView h=label(heading,14);h.setTextColor(muted());wrap.addView(h);
        TextView amount=label(value,27);amount.setTypeface(Typeface.DEFAULT,Typeface.BOLD);amount.setTextColor(accent());wrap.addView(amount);
        TextView sub=label(detail,13);sub.setTextColor(muted());wrap.addView(sub);screen.addView(wrap);
    }
    void page(String title){
        atHome=false;boolean home=title.equals("Suas compras");
        getWindow().setStatusBarColor(bg());getWindow().setNavigationBarColor(bg());
        getWindow().getDecorView().setSystemUiVisibility(theme==2?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackground(new ThemeSkin.Backdrop(theme));
        shell.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets;});
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);screen.setPadding(dp(18),dp(10),dp(18),dp(20));
        scroll.addView(screen);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView symbol=label("",22);ThemeSkin.Icon icon=new ThemeSkin.Icon(home?7:8,accent());icon.setBounds(0,0,dp(30),dp(30));symbol.setCompoundDrawables(icon,null,null,null);symbol.setContentDescription(home?"RegsCo":"Voltar ao início");symbol.setOnClickListener(v->home());heading.addView(symbol);
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);TextView h=label(home?"RegsCo":title,home?26:21);h.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));h.setPadding(0,dp(8),0,dp(2));names.addView(h);
        if(home){TextView subtitle=label("Suas compras em um só lugar",12);subtitle.setTextColor(muted());subtitle.setPadding(0,0,0,dp(12));names.addView(subtitle);}heading.addView(names,new LinearLayout.LayoutParams(0,-2,1));screen.addView(heading);
        int selected=home?0:title.toLowerCase(new Locale("pt","BR")).contains("farmácia")?2:title.toLowerCase(new Locale("pt","BR")).contains("estoque")?3:title.toLowerCase(new Locale("pt","BR")).contains("compra")?1:4;
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(6),dp(6),dp(6),dp(4));nav.setBackground(ThemeSkin.card(theme,dp(20)));
        String[] labels={"Início","Compras","Farmácia","Estoque","Mais"};Runnable[] actions={()->home(),()->sessions(),()->pharmacyPage(),()->pantry(),()->settingsPage()};
        for(int i=0;i<labels.length;i++){final int index=i;TextView item=label(labels[i],11);item.setPadding(dp(1),dp(6),dp(1),dp(6));item.setGravity(Gravity.CENTER);item.setMinHeight(dp(55));item.setTextColor(i==selected?accent():muted());ThemeSkin.Icon navIcon=new ThemeSkin.Icon(i, i==selected?accent():muted());navIcon.setBounds(0,0,dp(23),dp(23));item.setCompoundDrawables(null,navIcon,null,null);item.setCompoundDrawablePadding(dp(4));if(i==selected)item.setTypeface(Typeface.DEFAULT,Typeface.BOLD);item.setOnClickListener(v->actions[index].run());item.setFocusable(true);item.setContentDescription(labels[i]+(i==selected?", selecionado":""));nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));}
        shell.addView(nav,new LinearLayout.LayoutParams(-1,-2));setContentView(shell);
    }
    void primaryButton(String text,Runnable action){TextView b=label(text,16);b.setGravity(Gravity.CENTER);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(theme==2?0xff321337:Color.WHITE);b.setMinHeight(dp(52));b.setBackground(ThemeSkin.primary(theme,dp(16)));margin(b,8,8);b.setOnClickListener(v->action.run());b.setFocusable(true);screen.addView(b);}
    void monthSelector(){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setBackground(ThemeSkin.card(theme,dp(15)));margin(row,8,10);
        TextView previous=label("‹",28),next=label("›",28);previous.setContentDescription("Mês anterior");next.setContentDescription("Próximo mês");previous.setOnClickListener(v->shiftPeriod(-1));next.setOnClickListener(v->shiftPeriod(1));
        String monthName=java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.FULL,new Locale("pt","BR"));TextView center=label(Character.toUpperCase(monthName.charAt(0))+monthName.substring(1)+" de "+year,15);center.setGravity(Gravity.CENTER);center.setTypeface(Typeface.DEFAULT,Typeface.BOLD);center.setOnClickListener(v->choosePeriod());row.addView(previous);row.addView(center,new LinearLayout.LayoutParams(0,-2,1));row.addView(next);screen.addView(row);}
    void shiftPeriod(int amount){java.time.YearMonth next=java.time.YearMonth.of(year,month).plusMonths(amount);if(next.getYear()<2000||next.getYear()>2200)return;month=next.getMonthValue();year=next.getYear();home();}
    void tileRow(String first,String firstDetail,int firstIcon,Runnable firstAction,String second,String secondDetail,int secondIcon,Runnable secondAction){LinearLayout row=new LinearLayout(this);margin(row,5,5);row.addView(tile(first,firstDetail,firstIcon,firstAction),tileParams(true));row.addView(tile(second,secondDetail,secondIcon,secondAction),tileParams(false));screen.addView(row);}
    LinearLayout.LayoutParams tileParams(boolean left){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(left?0:dp(5),0,left?dp(5):0,0);return p;}
    View tile(String title,String detail,int icon,Runnable action){LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setPadding(dp(14),dp(14),dp(12),dp(14));wrap.setBackground(ThemeSkin.card(theme,dp(18)));wrap.setMinimumHeight(dp(132));
        TextView mark=label("",20);mark.setPadding(dp(6),dp(5),dp(6),dp(5));ThemeSkin.Icon drawable=new ThemeSkin.Icon(icon,accent());drawable.setBounds(0,0,dp(27),dp(27));mark.setCompoundDrawables(drawable,null,null,null);wrap.addView(mark);TextView name=label(title,16);name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);name.setPadding(0,dp(6),0,dp(4));wrap.addView(name);TextView description=label(detail,13);description.setTextColor(muted());description.setPadding(0,0,0,0);wrap.addView(description);wrap.setOnClickListener(v->action.run());wrap.setFocusable(true);wrap.setContentDescription(title+". "+detail);return wrap;}
    EditText field(String hint,String value,boolean numeric){
        EditText e=new EditText(this);e.setHint(hint);e.setText(value);
        e.setTextColor(fg());e.setHintTextColor(muted());
        e.setPadding(dp(18),dp(14),dp(18),dp(14));e.setBackground(ThemeSkin.card(theme,dp(14)));e.setMinHeight(dp(52));
        e.setContentDescription(hint);
        if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        margin(e,7,7);screen.addView(e);return e;
    }
    void message(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    long scalar(String sql,String[] args){try(Cursor c=db.rawQuery(sql,args)){return c.moveToFirst()?c.getLong(0):0;}}
    String period(){return String.format(new Locale("pt","BR"),"%02d/%d",month,year);}
    long spent(){return scalar("SELECT COALESCE(SUM((i.quantity*i.unit_cents-i.discount_cents)),0) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=?",new String[]{""+month,""+year});}
    long budget(){return scalar("SELECT COALESCE(cents,0) FROM budgets WHERE month=? AND year=?",new String[]{""+month,""+year});}
    boolean atHome=false;
    void home(){
        page("Suas compras");atHome=true;monthSelector();
        long budget=budget(),actual=spent();String[] args={""+month,""+year};
        FrameLayout gauge=new FrameLayout(this);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(210));gauge.setLayoutParams(gp);
        ThemeSkin.Ring ring=new ThemeSkin.Ring(this,theme,budget==0?0:(double)actual/budget);FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(dp(210),dp(210),Gravity.CENTER);gauge.addView(ring,rp);
        LinearLayout caption=new LinearLayout(this);caption.setOrientation(LinearLayout.VERTICAL);caption.setGravity(Gravity.CENTER);TextView amount=label(money(budget),25);amount.setTypeface(Typeface.DEFAULT,Typeface.BOLD);amount.setGravity(Gravity.CENTER);amount.setPadding(0,0,0,dp(6));caption.addView(amount);TextView sub=label("Orçamento do mês",13);sub.setTextColor(muted());sub.setPadding(0,0,0,0);caption.addView(sub);gauge.addView(caption,new FrameLayout.LayoutParams(-1,-1));gauge.setOnClickListener(v->editBudget());gauge.setContentDescription("Orçamento mensal "+money(budget)+". Toque para editar.");screen.addView(gauge);
        LinearLayout stats=new LinearLayout(this);stats.setPadding(dp(5),dp(10),dp(5),dp(10));stats.setBackground(ThemeSkin.card(theme,dp(18)));margin(stats,-7,10);
        String[] values={money(actual),money(budget-actual),budget>0?String.format(new Locale("pt","BR"),"%.0f%%",actual*100.0/budget):"—"};String[] descriptions={"Gasto até agora","Saldo restante","Utilizado"};
        for(int i=0;i<3;i++){LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setGravity(Gravity.CENTER);TextView value=label(values[i],16);value.setTypeface(Typeface.DEFAULT,Typeface.BOLD);value.setTextColor(i==1?accent():fg());value.setPadding(dp(2),dp(6),dp(2),dp(4));value.setGravity(Gravity.CENTER);col.addView(value);TextView desc=label(descriptions[i],11);desc.setTextColor(muted());desc.setPadding(dp(1),dp(2),dp(1),dp(4));desc.setGravity(Gravity.CENTER);col.addView(desc);stats.addView(col,new LinearLayout.LayoutParams(0,-2,1));}screen.addView(stats);
        if(budget>0&&actual>=budget*.8)screen.addView(label(actual>budget?"Orçamento ultrapassado":"Atenção: orçamento próximo do limite",14));
        tileRow("Supermercado","Planeje e registre suas compras",1,()->newSession(),"Farmácia","Organize suas compras e valores",2,()->pharmacyPage());
        tileRow("Estoque","Controle o que tem em casa",3,()->pantry(),"Histórico","Veja todas as suas compras",5,()->sessions());
        primaryButton("＋  Iniciar nova compra",()->newSession());
        long projected=scalar("SELECT COALESCE(SUM(i.quantity*i.unit_cents-i.discount_cents),0) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=0 AND s.month=? AND s.year=?",args);
        if(projected>0)card("COMPRAS EM ABERTO",money(projected),"Saldo previsto: "+money(budget-actual-projected));
        long completed=scalar("SELECT COUNT(*) FROM sessions WHERE finished=1 AND month=? AND year=?",args);screen.addView(label(completed+" compras finalizadas • "+scalar("SELECT COUNT(*) FROM shopping_list WHERE checked=0",null)+" itens pendentes na lista",13));
        button("Lista de compras",()->shoppingList());button("Cesta básica recorrente",()->basketPage());button("Produtos para repor",()->lowStock());button("Comparar preços registrados",()->comparePrices());button("Relatórios",()->reportsPage());
    }
    void settingsPage(){page("Configurações");button("Orçamento mensal",()->editBudget());button("Mês e ano de referência",()->choosePeriod());button("Tema e aparência",()->themePage());button("Gerenciar lojas",()->storesPage());button("Backup e restauração",()->backupPage());button("Voltar",()->home());}
    void reportsPage(){page("Relatórios");button("Gastos por categoria e loja",()->categoryReport());button("Últimos seis meses",()->sixMonthReport());button("Comparar meses",()->compareMonths());button("Produtos mais comprados",()->topProducts());button("Histórico de preços",()->priceHistory());button("Compartilhar resumo",()->share());button("Exportar CSV para Excel",()->exportCsv());button("Exportar PDF",()->exportPdf());button("Voltar",()->home());}
    void shoppingList(){listResults("","",-1);}
    void listResults(String term,String category,int status){
        page("Lista de compras");EditText search=field("Pesquisar produto",term,false),cat=field("Filtrar categoria (vazio: todas)",category,false);
        button("Pesquisar / filtrar",()->listResults(search.getText().toString().trim(),cat.getText().toString().trim(),status));
        button("Todos",()->listResults(term,category,-1));button("Pendentes",()->listResults(term,category,0));button("Concluídos",()->listResults(term,category,1));
        button("Adicionar produto",()->planningForm("shopping_list",0));button("Compartilhar lista",()->shareShoppingList());button("Limpar concluídos",()->confirmClearChecked());button("Reativar concluídos",()->reopenChecked());
        try(Cursor c=db.rawQuery("SELECT id,name,quantity,checked,unit,category FROM shopping_list WHERE name LIKE ? AND (?='' OR category=?) AND (?=-1 OR checked=?) ORDER BY checked,name COLLATE NOCASE",new String[]{"%"+term+"%",category,category,""+status,""+status})){
            if(c.getCount()==0)screen.addView(label("Nenhum item encontrado.",15));
            while(c.moveToNext()){long id=c.getLong(0);int checked=c.getInt(3);button((checked==1?"✓ ":"○ ")+c.getString(1)+" • "+c.getLong(2)+" "+c.getString(4)+" • "+c.getString(5),()->{db.execSQL("UPDATE shopping_list SET checked=? WHERE id=?",new Object[]{checked==1?0:1,id});listResults(term,category,status);});button("Editar ou excluir item",()->planningForm("shopping_list",id));}
        }
        button("Voltar",()->home());
    }
    void planningForm(String table,long id){
        String n="",q="1",u="un",cat="";if(id!=0)try(Cursor c=db.rawQuery("SELECT name,quantity,unit,category FROM "+table+" WHERE id=?",new String[]{""+id})){if(!c.moveToFirst())return;n=c.getString(0);q=c.getString(1);u=c.getString(2);cat=c.getString(3);}
        page("Produto do planejamento");EditText name=field("Produto",n,false),qty=field("Quantidade inteira",q,true),unit=field("Unidade (un, pacote, kg, g, L, mL)",u,false),category=field("Categoria",cat,false);button("Escolher categoria",()->chooseCategory(category));
        Runnable back=()->{if(table.equals("basket"))basketPage();else shoppingList();};
        button("Salvar",()->{int amount=positive(qty.getText().toString());String text=name.getText().toString().trim(),un=unit.getText().toString().trim();if(text.isEmpty()||un.isEmpty()||amount<1){message("Confira produto, unidade e quantidade");return;}android.content.ContentValues v=new android.content.ContentValues();v.put("name",text);v.put("quantity",amount);v.put("unit",un);v.put("category",category.getText().toString().trim());if(id==0)db.insertOrThrow(table,null,v);else db.update(table,v,"id=?",new String[]{""+id});back.run();});
        if(id!=0)button("Excluir",()->confirmDelete("Excluir este produto?",()->{db.delete(table,"id=?",new String[]{""+id});back.run();}));button("Cancelar",back);
    }
    int positive(String value){try{int n=Integer.parseInt(value.trim());return n>0?n:0;}catch(Exception e){return 0;}}
    void pantry(){pantryResults("",false);}
    void pantryResults(String term,boolean low){
        page("Estoque da casa");
        EditText query=field("Pesquisar produto",term,false);
        button("Pesquisar",()->pantryResults(query.getText().toString().trim(),low));
        button(low?"Ver todos":"Somente estoque baixo",()->pantryResults(term,!low));
        button("Cadastrar produto",()->pantryForm(0));
        String today=java.time.LocalDate.now().toString(),soon=java.time.LocalDate.now().plusDays(30).toString();
        try(Cursor c=db.rawQuery("SELECT id,name,quantity,unit,minimum,expiry,category FROM pantry WHERE name LIKE ? "+(low?"AND quantity<minimum ":"")+"ORDER BY name COLLATE NOCASE",new String[]{"%"+term+"%"})){
            if(c.getCount()==0)screen.addView(label("Nenhum produto encontrado.",15));
            while(c.moveToNext()){
                long id=c.getLong(0);String expiry=c.getString(5);String status=expiry.isEmpty()?"Sem validade informada":expiry.compareTo(today)<0?"VENCIDO • "+expiry:expiry.compareTo(soon)<=0?"Vence em breve • "+expiry:"Validade: "+expiry;
                button(c.getString(1)+" • "+c.getLong(2)+" "+c.getString(3)+"\nMínimo: "+c.getLong(4)+" • "+status+"\n"+c.getString(6),()->pantryForm(id));
            }
        }
        button("Voltar",()->home());
    }
    void pantryForm(long id){
        String name="",q="0",unit="un",minimum="0",expiry="",category="",notes="";
        if(id!=0)try(Cursor c=db.rawQuery("SELECT name,quantity,unit,minimum,expiry,category,notes FROM pantry WHERE id=?",new String[]{""+id})){if(!c.moveToFirst())return;name=c.getString(0);q=c.getString(1);unit=c.getString(2);minimum=c.getString(3);expiry=c.getString(4);category=c.getString(5);notes=c.getString(6);}
        page(id==0?"Cadastrar estoque":"Ajustar estoque");
        EditText n=field("Produto",name,false),quantity=field("Quantidade disponível (inteira)",q,true),u=field("Unidade (un, kg, g, L, mL)",unit,false),min=field("Quantidade mínima",minimum,true),exp=field("Validade AAAA-MM-DD (opcional)",expiry,false),cat=field("Categoria",category,false),obs=field("Observações",notes,false);
        button("Salvar",()->{try{
            long qty=Long.parseLong(quantity.getText().toString()),mi=Long.parseLong(min.getText().toString());String nm=n.getText().toString().trim(),un=u.getText().toString().trim(),ex=exp.getText().toString().trim();
            if(qty<0||mi<0||nm.isEmpty()||un.isEmpty())throw new IllegalArgumentException();if(!ex.isEmpty())java.time.LocalDate.parse(ex);
            android.content.ContentValues v=new android.content.ContentValues();v.put("name",nm);v.put("quantity",qty);v.put("unit",un);v.put("minimum",mi);v.put("expiry",ex);v.put("category",cat.getText().toString().trim());v.put("notes",obs.getText().toString().trim());
            if(id==0)db.insertOrThrow("pantry",null,v);else db.update("pantry",v,"id=?",new String[]{""+id});pantry();
        }catch(Exception e){message("Confira nome, unidade, quantidades não negativas e validade AAAA-MM-DD");}});
        if(id!=0)button("Excluir produto",()->confirmDelete("Excluir este produto do estoque?",()->{db.delete("pantry","id=?",new String[]{""+id});pantry();}));
        button("Cancelar",()->pantry());
    }
    void stockPurchase(long session){
        if(scalar("SELECT stock_added FROM sessions WHERE id=?",new String[]{""+session})==1){message("Esta compra já foi adicionada ao estoque. Ajuste o estoque manualmente se necessário.");return;}
        new AlertDialog.Builder(this).setTitle("Adicionar ao estoque")
        .setMessage("Adicionar todos os produtos desta compra ao estoque? Faça isso somente se todos forem estocáveis. Reabrir a compra não desfaz o movimento físico do estoque.")
        .setNegativeButton("Cancelar",null).setPositiveButton("Adicionar",(dialog,which)->{
            db.beginTransaction();try{
                if(scalar("SELECT stock_added FROM sessions WHERE id=?",new String[]{""+session})==1)return;
                try(Cursor c=db.rawQuery("SELECT name,quantity,unit,category FROM items WHERE session_id=?",new String[]{""+session})){
                    while(c.moveToNext()){
                        try(Cursor p=db.rawQuery("SELECT id,quantity FROM pantry WHERE LOWER(TRIM(name))=LOWER(TRIM(?)) AND unit=? AND expiry='' LIMIT 1",new String[]{c.getString(0),c.getString(2)})){
                            if(p.moveToFirst())db.execSQL("UPDATE pantry SET quantity=? WHERE id=?",new Object[]{Math.addExact(p.getLong(1),c.getLong(1)),p.getLong(0)});
                            else db.execSQL("INSERT INTO pantry(name,quantity,unit,category) VALUES(?,?,?,?)",new Object[]{c.getString(0),c.getLong(1),c.getString(2),c.getString(3)});
                        }
                    }
                }
                db.execSQL("UPDATE sessions SET stock_added=1 WHERE id=?",new Object[]{session});db.setTransactionSuccessful();
            }catch(Exception e){message("Não foi possível atualizar o estoque");}finally{db.endTransaction();}editSession(session);
        }).show();
    }
    static final int EXPORT_BACKUP=701, IMPORT_BACKUP=702, EXPORT_CSV=703, EXPORT_PDF=705;
    final String[] backupTables={"settings","sessions","items","budgets","shopping_list","pantry","basket","stores"};
    void backupPage(){
        page("Backup e restauração");
        screen.addView(label("Salve seus registros em um arquivo JSON no armazenamento do celular. A restauração substitui os registros atuais.",15));
        button("Exportar backup completo",()->{
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"RegsCo-backup-"+System.currentTimeMillis()+".json");
            startActivityForResult(i,EXPORT_BACKUP);
        });
        button("Restaurar de arquivo",()->new AlertDialog.Builder(this).setTitle("Restaurar backup")
            .setMessage("A restauração substituirá todas as compras, listas, estoque e configurações atuais. Continue somente se o arquivo for confiável.")
            .setNegativeButton("Cancelar",null).setPositiveButton("Escolher arquivo",(d,w)->{
                Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");startActivityForResult(i,IMPORT_BACKUP);
            }).show());
        button("Voltar",()->home());
    }
    JSONObject exportData() throws Exception{
        JSONObject root=new JSONObject();root.put("format","regsco-backup");root.put("version",4);
        for(String table:backupTables){
            JSONArray rows=new JSONArray();
            try(Cursor c=db.rawQuery("SELECT * FROM "+table,null)){
                while(c.moveToNext()){
                    JSONObject row=new JSONObject();
                    for(int i=0;i<c.getColumnCount();i++){
                        if(c.isNull(i))row.put(c.getColumnName(i),JSONObject.NULL);
                        else if(c.getType(i)==Cursor.FIELD_TYPE_INTEGER)row.put(c.getColumnName(i),c.getLong(i));
                        else row.put(c.getColumnName(i),c.getString(i));
                    }
                    rows.put(row);
                }
            }
            root.put(table,rows);
        }
        return root;
    }
    void restoreData(JSONObject root) throws Exception{
        if(!"regsco-backup".equals(root.optString("format"))||(root.optInt("version")<1||root.optInt("version")>4))throw new Exception("Formato de backup incompatível");
        for(String table:backupTables)if(root.optJSONArray(table)==null&&!(root.optInt("version")<2&&table.equals("basket"))&&!(root.optInt("version")<3&&table.equals("stores")))throw new Exception("Backup incompleto: "+table);
        db.beginTransaction();
        try{
            for(int t=backupTables.length-1;t>=0;t--)db.delete(backupTables[t],null,null);
            for(String table:backupTables){
                JSONArray rows=root.optJSONArray(table);if(rows==null)continue;
                for(int i=0;i<rows.length();i++){
                    JSONObject row=rows.getJSONObject(i);
                    android.content.ContentValues values=new android.content.ContentValues();
                    Iterator<String> keys=row.keys();
                    while(keys.hasNext()){
                        String key=keys.next();Object value=row.get(key);
                        if(value==JSONObject.NULL)values.putNull(key);
                        else if(value instanceof Number)values.put(key,((Number)value).longValue());
                        else values.put(key,String.valueOf(value));
                    }
                    if(db.insertOrThrow(table,null,values)<0)throw new Exception("Falha ao restaurar "+table);
                }
            }
            DatabaseRules.validate(db);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        try(Cursor c=db.rawQuery("SELECT value FROM settings WHERE key='theme'",null)){theme=c.moveToFirst()?Integer.parseInt(c.getString(0)):0;}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK){if(request==CAMERA_SCAN)discardScanPhoto();return;}
        if(request==CAMERA_SCAN){handleCameraResult(data);return;}
        if(data==null||data.getData()==null)return;
        try{
            if(request==EXPORT_PDF){
                android.net.Uri uri=data.getData();String text=report();
                background("Gerando PDF…",()->{writePdf(text,getContentResolver().openOutputStream(uri,"wt"));},()->message("PDF exportado"));
            }else if(request==EXPORT_CSV){
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                    if(out==null)throw new IOException("Não foi possível abrir o arquivo");
                    out.write(("\uFEFF"+reportCsv()).getBytes("UTF-8"));
                }
                message("Relatório CSV exportado");
            }else if(request==EXPORT_BACKUP){
                android.net.Uri uri=data.getData();
                background("Exportando backup…",()->{BackupIO.write(db,getContentResolver().openOutputStream(uri,"wt"));},()->message("Backup exportado"));
            }else if(request==IMPORT_BACKUP){
                android.net.Uri uri=data.getData();
                final SQLiteDatabase[] staged={null};final File file=new File(getCacheDir(),"restore-"+System.nanoTime()+".db");
                background("Verificando backup…",()->{staged[0]=BackupIO.stage(db,getContentResolver().openInputStream(uri),file);},()->{
                    new AlertDialog.Builder(this).setTitle("Backup validado")
                    .setMessage("Substituir todos os dados deste aparelho pelos dados do arquivo? Esta operação não pode ser desfeita. Exporte um backup antes, se necessário.")
                    .setNegativeButton("Cancelar",(d,w)->{staged[0].close();file.delete();})
                    .setOnCancelListener(d->{staged[0].close();file.delete();})
                    .setPositiveButton("Restaurar",(d,w)->background("Restaurando…",()->{try{BackupIO.replace(db,staged[0]);}finally{staged[0].close();file.delete();}},()->{
                        try(Cursor c=db.rawQuery("SELECT value FROM settings WHERE key='theme'",null)){theme=c.moveToFirst()?Integer.parseInt(c.getString(0)):0;}
                        message("Backup restaurado");home();
                    })).show();
                });
            }
        }catch(Exception e){message("Erro: "+e.getMessage());}
    }
    void categoryReport(){
        page("Relatório mensal");
        screen.addView(label("Gastos finalizados em "+period(),16));
        card("TOTAL DO MÊS",money(spent()),"Saldo do orçamento: "+money(budget()-spent()));
        section("Gastos por categoria");
        long total=spent();
        try(Cursor c=db.rawQuery("SELECT CASE WHEN TRIM(i.category)='' THEN 'Sem categoria' ELSE i.category END AS category, SUM((i.quantity*i.unit_cents-i.discount_cents)) AS value FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=? GROUP BY category ORDER BY value DESC",new String[]{""+month,""+year})){
            if(c.getCount()==0)screen.addView(label("Nenhuma compra finalizada neste mês.",15));
            while(c.moveToNext()){
                long value=c.getLong(1);String category=c.getString(0);
                int percent=total>0?(int)Math.round(value*100.0/total):0;
                screen.addView(label(category+"  •  "+money(value)+"  ("+percent+"%)",16));
                ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
                bar.setMax(100);bar.setProgress(percent);bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent()));
                margin(bar,2,12);screen.addView(bar);
            }
        }
        section("Por estabelecimento");
        try(Cursor c=db.rawQuery("SELECT CASE WHEN TRIM(COALESCE(NULLIF(i.store,''),s.store))='' THEN 'Não informado' ELSE COALESCE(NULLIF(i.store,''),s.store) END, SUM((i.quantity*i.unit_cents-i.discount_cents)) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=? GROUP BY COALESCE(NULLIF(i.store,''),s.store) ORDER BY 2 DESC",new String[]{""+month,""+year})){
            while(c.moveToNext())screen.addView(label(c.getString(0)+"  •  "+money(c.getLong(1)),16));
        }
        button("Compartilhar relatório",()->share());
        button("Voltar",()->home());
    }
    void priceHistory(){
        page("Histórico de preços");
        screen.addView(label("Preços registrados nas suas compras, organizados do mais recente para o mais antigo. Não são ofertas em tempo real.",14));
        EditText search=field("Buscar produto (ex.: arroz)","",false);
        button("Pesquisar",()->showPriceResults(search.getText().toString().trim()));
        button("Voltar",()->home());
    }
    void showPriceResults(String term){
        page("Preços registrados");
        screen.addView(label("Busca: "+(term.isEmpty()?"Todos os produtos":term),15));
        try(Cursor c=db.rawQuery("SELECT i.name,COALESCE(NULLIF(i.store,''),s.store),i.unit_cents,s.month,s.year,i.unit,i.brand FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND i.name LIKE ? COLLATE NOCASE ORDER BY s.created DESC,i.id DESC LIMIT 100",new String[]{"%"+term+"%"})){
            if(c.getCount()==0)screen.addView(label("Nenhum preço encontrado.",16));
            while(c.moveToNext()){
                String store=c.getString(1);if(store==null||store.trim().isEmpty())store="Loja não informada";
                screen.addView(label(c.getString(0)+"  •  "+money(c.getLong(2))+" / "+c.getString(5)+" • "+c.getString(6)+"\n"+store+"  •  "+c.getInt(3)+"/"+c.getInt(4),16));
            }
        }
        button("Nova pesquisa",()->priceHistory());button("Voltar",()->home());
    }
    void restockFromList(){
        page("Repor estoque pela lista");
        screen.addView(label("Selecione produtos comprados para atualizar o estoque. Os itens serão marcados como concluídos na lista.",14));
        try(Cursor c=db.rawQuery("SELECT id,name,quantity FROM shopping_list WHERE checked=0 ORDER BY id DESC",null)){
            if(c.getCount()==0)screen.addView(label("Não há itens pendentes na lista.",15));
            while(c.moveToNext()){
                long id=c.getLong(0);String name=c.getString(1);int qty=c.getInt(2);
                button("Repor "+name+"  × "+qty,()->{
                    db.beginTransaction();
                    try{
                        try(Cursor p=db.rawQuery("SELECT id FROM pantry WHERE LOWER(TRIM(name))=LOWER(TRIM(?)) LIMIT 1",new String[]{name})){
                            if(p.moveToFirst())db.execSQL("UPDATE pantry SET quantity=quantity+? WHERE id=?",new Object[]{qty,p.getLong(0)});
                            else db.execSQL("INSERT INTO pantry(name,quantity) VALUES(?,?)",new Object[]{name,qty});
                        }
                        db.execSQL("UPDATE shopping_list SET checked=1 WHERE id=?",new Object[]{id});
                        db.setTransactionSuccessful();
                    }finally{db.endTransaction();}
                    restockFromList();
                });
            }
        }
        button("Voltar ao estoque",()->pantry());
    }
    void basketPage(){
        page("Cesta básica recorrente");button("Adicionar produto",()->planningForm("basket",0));button("Ver faltantes no estoque",()->lowStock());button("Enviar faltantes à lista",()->planFromStock());
        try(Cursor c=db.rawQuery("SELECT id,name,quantity,unit FROM basket ORDER BY name COLLATE NOCASE",null)){if(c.getCount()==0)screen.addView(label("Cadastre sua cesta mensal.",15));while(c.moveToNext()){long id=c.getLong(0);button(c.getString(1)+" • "+c.getLong(2)+" "+c.getString(3),()->planningForm("basket",id));}}
        button("Voltar",()->home());
    }
    void storesPage(){
        page("Minhas lojas");
        screen.addView(label("Cadastre seus estabelecimentos favoritos para preencher o nome nas próximas compras.",14));
        try(Cursor c=db.rawQuery("SELECT id,name FROM stores ORDER BY name",null)){
            while(c.moveToNext()){
                long id=c.getLong(0);String name=c.getString(1);
                screen.addView(label(name,16));
                button("Excluir "+name,()->new AlertDialog.Builder(this).setMessage("Remover loja favorita? As compras anteriores serão preservadas.").setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(d,w)->{db.execSQL("DELETE FROM stores WHERE id=?",new Object[]{id});storesPage();}).show());
            }
        }
        EditText name=field("Nome da loja","",false);
        button("Salvar loja",()->{
            String n=name.getText().toString().trim();if(n.isEmpty()){message("Informe o nome da loja");return;}
            android.content.ContentValues v=new android.content.ContentValues();v.put("name",n);
            if(db.insertWithOnConflict("stores",null,v,SQLiteDatabase.CONFLICT_IGNORE)==-1)message("Esta loja já está cadastrada");
            storesPage();
        });
        button("Voltar",()->home());
    }
    void chooseStore(EditText field){
        ArrayList<String> names=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT name FROM stores ORDER BY name",null)){
            while(c.moveToNext())names.add(c.getString(0));
        }
        if(names.isEmpty()){message("Cadastre uma loja em Gerenciar lojas");return;}
        new AlertDialog.Builder(this).setTitle("Selecionar loja")
            .setItems(names.toArray(new String[0]),(d,w)->field.setText(names.get(w)))
            .setNegativeButton("Cancelar",null).show();
    }
    void comparePrices(){
        page("Comparar preços registrados");
        screen.addView(label("Compare preços unitários de produtos cadastrados em compras anteriores. Não são preços atuais das lojas.",14));
        EditText search=field("Nome do produto","",false);
        button("Comparar",()->showComparison(search.getText().toString().trim()));
        button("Voltar",()->home());
    }
    void showComparison(String product){
        if(product.isEmpty()){message("Informe um produto");return;}page("Comparação local");
        screen.addView(label("Preços históricos de compras finalizadas. As estatísticas são separadas por marca/apresentação e unidade.",15));
        java.util.LinkedHashMap<String,long[]> stats=new java.util.LinkedHashMap<>();
        try(Cursor c=db.rawQuery("SELECT COALESCE(NULLIF(i.store,''),s.store),i.unit_cents,s.month,s.year,i.unit,i.brand,i.discount_cents,i.quantity FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND LOWER(TRIM(i.name))=LOWER(TRIM(?)) ORDER BY s.created DESC,i.id DESC",new String[]{product})){
            if(c.getCount()==0)screen.addView(label("Nenhum registro finalizado encontrado.",15));
            while(c.moveToNext()){
                String identity=(c.getString(5).isEmpty()?"Marca/apresentação não informada":c.getString(5))+" • "+c.getString(4);long price=c.getLong(1);
                long[] values=stats.get(identity);if(values==null){values=new long[]{price,price,price};stats.put(identity,values);}else{values[0]=Math.min(values[0],price);values[1]=Math.max(values[1],price);}
                screen.addView(label(c.getString(0)+" • "+money(price)+" / "+c.getString(4)+"\n"+identity+" • "+c.getInt(2)+"/"+c.getInt(3)+(c.getLong(6)>0?"\nDesconto adicional no lote: "+money(c.getLong(6)):""),16));
            }
        }
        for(java.util.Map.Entry<String,long[]> entry:stats.entrySet()){long[] v=entry.getValue();card(entry.getKey(),"Menor: "+money(v[0]),"Maior: "+money(v[1])+" • Último: "+money(v[2])+"\nPreços por unidade, antes de desconto adicional no lote.");}
        screen.addView(label("Produtos sem marca ou tamanho informado exigem conferência manual. Não há consulta automática de preços online nesta versão.",14));
        button("Nova comparação",()->comparePrices());button("Voltar",()->home());
    }
    long missingStock(String name,long wanted,String unit){
        long available=0;try(Cursor c=db.rawQuery("SELECT quantity,unit FROM pantry WHERE LOWER(TRIM(name))=LOWER(TRIM(?))",new String[]{name})){while(c.moveToNext())if(Units.compatible(unit,c.getString(1)))available=Math.addExact(available,Units.base(c.getLong(0),c.getString(1)));}
        return Math.max(0,Math.subtractExact(Units.base(wanted,unit),available));
    }
    void mergeMissing(String name,long missing,String baseUnit,String category){
        long existing=0,first=0;ArrayList<Long> others=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT id,quantity,unit FROM shopping_list WHERE LOWER(TRIM(name))=LOWER(TRIM(?)) AND checked=0",new String[]{name})){while(c.moveToNext())if(Units.compatible(baseUnit,c.getString(2))){existing=Math.addExact(existing,Units.base(c.getLong(1),c.getString(2)));if(first==0)first=c.getLong(0);else others.add(c.getLong(0));}}
        if(first==0)db.execSQL("INSERT INTO shopping_list(name,quantity,unit,category) VALUES(?,?,?,?)",new Object[]{name,missing,baseUnit,category});
        else{db.execSQL("UPDATE shopping_list SET quantity=?,unit=? WHERE id=?",new Object[]{Math.max(existing,missing),baseUnit,first});for(long id:others)db.delete("shopping_list","id=?",new String[]{""+id});}
    }
    void lowStock(){
        page("Reposição da cesta");int count=0;
        try(Cursor c=db.rawQuery("SELECT name,quantity,unit,category FROM basket ORDER BY name",null)){
            while(c.moveToNext()){String name=c.getString(0),unit=Units.canonical(c.getString(2)),category=c.getString(3);long missing=missingStock(name,c.getLong(1),c.getString(2));if(missing<=0)continue;count++;button(name+" • faltam "+missing+" "+unit+" — adicionar à lista",()->{db.beginTransaction();try{mergeMissing(name,missing,unit,category);db.setTransactionSuccessful();}finally{db.endTransaction();}message("Lista atualizada");});}
        }
        if(count==0)screen.addView(label("Nenhum produto da cesta em falta.",15));button("Voltar",()->home());
    }
    void planningSummary(){
        page("Resumo do planejamento");
        long pending=scalar("SELECT COUNT(*) FROM shopping_list WHERE checked=0",null);
        long checked=scalar("SELECT COUNT(*) FROM shopping_list WHERE checked=1",null);
        long stock=scalar("SELECT COUNT(*) FROM pantry WHERE quantity>0",null);
        long open=scalar("SELECT COUNT(*) FROM sessions WHERE finished=0",null);
        card("ITENS PENDENTES",""+pending,"Produtos que ainda precisam ser comprados");
        card("ITENS CONCLUÍDOS",""+checked,"Produtos marcados na lista");
        card("PRODUTOS EM ESTOQUE",""+stock,"Tipos de produtos com quantidade positiva");
        card("COMPRAS EM ANDAMENTO",""+open,"Sessões ainda não finalizadas");
        button("Abrir lista",()->shoppingList());
        button("Ver itens para repor",()->lowStock());
        button("Ver compras abertas",()->activeSessions());
        button("Voltar",()->home());
    }
    void activeSessions(){
        page("Compras em andamento");
        try(Cursor c=db.rawQuery("SELECT s.id,s.name,s.store,s.month,s.year,COALESCE(SUM((i.quantity*i.unit_cents-i.discount_cents)),0) FROM sessions s LEFT JOIN items i ON i.session_id=s.id WHERE s.finished=0 GROUP BY s.id ORDER BY s.created DESC",null)){
            if(c.getCount()==0)screen.addView(label("Nenhuma compra em andamento.",16));
            while(c.moveToNext()){
                long id=c.getLong(0);
                button(c.getString(1)+"  •  "+money(c.getLong(5))+"\n"+c.getString(2)+"  •  "+c.getInt(3)+"/"+c.getInt(4),()->editSession(id));
            }
        }
        button("Voltar",()->home());
    }
    void searchSessions(){
        page("Buscar compras");
        EditText term=field("Nome da compra ou loja","",false);
        button("Pesquisar",()->showSessionResults(term.getText().toString().trim()));
        button("Voltar",()->home());
    }
    void showSessionResults(String term){
        page("Resultados de compras");
        try(Cursor c=db.rawQuery("SELECT s.id,s.name,s.store,s.month,s.year,s.finished,COALESCE(SUM((i.quantity*i.unit_cents-i.discount_cents)),0) FROM sessions s LEFT JOIN items i ON i.session_id=s.id WHERE s.name LIKE ? OR s.store LIKE ? GROUP BY s.id ORDER BY s.created DESC LIMIT 200",new String[]{"%"+term+"%","%"+term+"%"})){
            if(c.getCount()==0)screen.addView(label("Nenhuma compra encontrada.",16));
            while(c.moveToNext()){
                long id=c.getLong(0);
                button(c.getString(1)+" • "+c.getString(2)+"\n"+c.getInt(3)+"/"+c.getInt(4)+" • "+(c.getInt(5)==1?"Finalizada":"Em andamento")+" • "+money(c.getLong(6)),()->editSession(id));
            }
        }
        button("Nova busca",()->searchSessions());button("Voltar",()->home());
    }
    String csvCell(String value){
        if(value==null)value="";
        if(!value.isEmpty() && "=+-@".indexOf(value.charAt(0))>=0)value="\u0027"+value;
        return "\"" + value.replace("\"","\"\"").replace("\r"," ").replace("\n"," ") + "\"";
    }
    String reportCsv(){
        StringBuilder b=new StringBuilder("Compra;Loja;Mês;Ano;Status;Produto;Categoria;Quantidade;Preço unitário (R$);Desconto (R$);Total (R$)\r\n");
        try(Cursor c=db.rawQuery("SELECT s.name,COALESCE(NULLIF(i.store,''),s.store),s.month,s.year,s.finished,i.name,i.category,i.quantity,i.unit_cents,i.discount_cents FROM sessions s JOIN items i ON i.session_id=s.id WHERE s.month=? AND s.year=? ORDER BY s.id,i.id",new String[]{""+month,""+year})){
            while(c.moveToNext()){
                long cents=c.getLong(8);int qty=c.getInt(7);
                b.append(csvCell(c.getString(0))).append(';').append(csvCell(c.getString(1))).append(';')
                .append(c.getInt(2)).append(';').append(c.getInt(3)).append(';')
                .append(csvCell(c.getInt(4)==1?"Finalizada":"Em andamento")).append(';')
                .append(csvCell(c.getString(5))).append(';').append(csvCell(c.getString(6))).append(';')
                .append(qty).append(';').append(csvCell(amountInput(cents))).append(';')
                .append(csvCell(amountInput(c.getLong(9)))).append(';').append(csvCell(amountInput(cents*qty-c.getLong(9)))).append("\r\n");
            }
        }
        return b.toString();
    }
    void exportCsv(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/csv");i.putExtra(Intent.EXTRA_TITLE,"RegsCo-relatorio-"+period().replace('/','-')+".csv");
        startActivityForResult(i,EXPORT_CSV);
    }
    void exportPdf(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,"RegsCo-"+period().replace('/','-')+".pdf");startActivityForResult(i,EXPORT_PDF);}
    void writePdf(String text,OutputStream output) throws Exception{PdfReport.write(text,output);}
    void chooseCategory(EditText target){
        java.util.LinkedHashSet<String> categories=new java.util.LinkedHashSet<>(java.util.Arrays.asList("Alimentação","Carnes e frios","Hortifruti","Bebidas","Limpeza","Higiene pessoal","Farmácia","Casa","Outros"));
        try(Cursor c=db.rawQuery("SELECT DISTINCT category FROM items WHERE TRIM(category)<>'' UNION SELECT DISTINCT category FROM shopping_list WHERE TRIM(category)<>''",null)){while(c.moveToNext())categories.add(c.getString(0));}
        String[] choices=categories.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Categoria do produto").setItems(choices,(d,w)->target.setText(choices[w])).setNegativeButton("Cancelar",null).show();
    }
    void planFromStock(){
        db.beginTransaction();try(Cursor c=db.rawQuery("SELECT name,quantity,unit,category FROM basket",null)){
            while(c.moveToNext()){long missing=missingStock(c.getString(0),c.getLong(1),c.getString(2));if(missing>0)mergeMissing(c.getString(0),missing,Units.canonical(c.getString(2)),c.getString(3));}
            db.setTransactionSuccessful();
        }catch(Exception e){message("Não foi possível atualizar a lista");}finally{db.endTransaction();}shoppingList();
    }
    void sixMonthReport(){
        page("Gastos dos últimos 6 meses");
        screen.addView(label("Somente compras finalizadas são contabilizadas.",14));
        Calendar cursor=Calendar.getInstance();
        cursor.set(year,month-1,1);
        long sum=0;
        for(int i=0;i<6;i++){
            int m=cursor.get(Calendar.MONTH)+1,y=cursor.get(Calendar.YEAR);
            long value=scalar("SELECT COALESCE(SUM((i.quantity*i.unit_cents-i.discount_cents)),0) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=?",new String[]{""+m,""+y});
            sum+=value;
            screen.addView(label(String.format(Locale.forLanguageTag("pt-BR"),"%02d/%d",m,y)+"  •  "+money(value),17));
            cursor.add(Calendar.MONTH,-1);
        }
        card("TOTAL DE 6 MESES",money(sum),"Média mensal: "+money(java.math.BigDecimal.valueOf(sum).divide(java.math.BigDecimal.valueOf(6),0,java.math.RoundingMode.HALF_UP).longValueExact()));
        button("Voltar",()->home());
    }
    void editListItem(long id,String oldName,int oldQty){
        page("Editar item da lista");
        EditText name=field("Produto",oldName,false),quantity=field("Quantidade",""+oldQty,true);
        button("Salvar",()->{
            String n=name.getText().toString().trim();int q=positive(quantity.getText().toString());
            if(n.isEmpty()||q<1){message("Informe produto e quantidade");return;}
            db.execSQL("UPDATE shopping_list SET name=?,quantity=? WHERE id=?",new Object[]{n,q,id});
            shoppingList();
        });
        button("Cancelar",()->shoppingList());
    }
    void editBasketItem(long id,String oldName,int oldQty){
        page("Editar cesta recorrente");
        EditText name=field("Produto",oldName,false),quantity=field("Quantidade",""+oldQty,true);
        button("Salvar",()->{
            String n=name.getText().toString().trim();int q=positive(quantity.getText().toString());
            if(n.isEmpty()||q<1){message("Informe produto e quantidade");return;}
            db.execSQL("UPDATE basket SET name=?,quantity=? WHERE id=?",new Object[]{n,q,id});
            basketPage();
        });
        button("Cancelar",()->basketPage());
    }
    void compareMonths(){
        page("Comparar meses");
        Calendar previous=Calendar.getInstance();previous.set(year,month-1,1);previous.add(Calendar.MONTH,-1);
        int pm=previous.get(Calendar.MONTH)+1,py=previous.get(Calendar.YEAR);
        long current=spent();
        long last=scalar("SELECT COALESCE(SUM((i.quantity*i.unit_cents-i.discount_cents)),0) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=?",new String[]{""+pm,""+py});
        card("MÊS ATUAL • "+period(),money(current),"Somente compras finalizadas");
        card("MÊS ANTERIOR • "+String.format(Locale.forLanguageTag("pt-BR"),"%02d/%d",pm,py),money(last),"Somente compras finalizadas");
        long difference=current-last;
        screen.addView(label((difference>0?"Aumento de ":difference<0?"Redução de ":"Sem diferença: ")+money(Math.abs(difference)),18));
        if(last>0)screen.addView(label("Variação: "+String.format(Locale.forLanguageTag("pt-BR"),"%.1f%%",(difference*100.0/last)),16));
        else screen.addView(label("Sem base de gastos no mês anterior para calcular a variação percentual.",14));
        button("Escolher outro mês",()->choosePeriod());button("Voltar",()->home());
    }
    void topProducts(){
        page("Produtos mais comprados");
        screen.addView(label("Quantidade e valor acumulados nas compras finalizadas do mês "+period()+".",14));
        try(Cursor c=db.rawQuery("SELECT i.name,SUM(i.quantity),SUM((i.quantity*i.unit_cents-i.discount_cents)) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=? GROUP BY LOWER(TRIM(i.name)) ORDER BY SUM((i.quantity*i.unit_cents-i.discount_cents)) DESC LIMIT 30",new String[]{""+month,""+year})){
            if(c.getCount()==0)screen.addView(label("Nenhuma compra finalizada neste mês.",15));
            while(c.moveToNext())screen.addView(label(c.getString(0)+"  •  "+c.getLong(1)+" un.\nTotal gasto: "+money(c.getLong(2)),16));
        }
        button("Voltar",()->home());
    }
    void confirmClearChecked(){
        long count=scalar("SELECT COUNT(*) FROM shopping_list WHERE checked=1",null);
        if(count==0){message("Não há itens concluídos");return;}
        new AlertDialog.Builder(this).setTitle("Limpar lista")
            .setMessage("Remover "+count+" itens concluídos? Os pendentes serão preservados.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Remover",(d,w)->{db.execSQL("DELETE FROM shopping_list WHERE checked=1");shoppingList();}).show();
    }
    void storeSummary(){
        page("Gastos por loja");
        try(Cursor c=db.rawQuery("SELECT COALESCE(NULLIF(i.store,''),s.store),COUNT(DISTINCT s.id),COALESCE(SUM((i.quantity*i.unit_cents-i.discount_cents)),0) FROM sessions s LEFT JOIN items i ON i.session_id=s.id WHERE s.finished=1 AND s.month=? AND s.year=? GROUP BY COALESCE(NULLIF(i.store,''),s.store) ORDER BY 3 DESC",new String[]{""+month,""+year})){
            if(c.getCount()==0)screen.addView(label("Nenhuma compra finalizada no período.",16));
            while(c.moveToNext())card(c.getString(0),money(c.getLong(2)),c.getLong(1)+" compra(s)");
        }
        button("Voltar",()->home());
    }
    void reopenChecked(){
        long count=scalar("SELECT COUNT(*) FROM shopping_list WHERE checked=1",null);
        if(count==0){message("Não há itens concluídos");return;}
        new AlertDialog.Builder(this).setTitle("Reativar itens")
            .setMessage("Retornar "+count+" itens concluídos para a lista pendente?")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Reativar",(d,w)->{db.execSQL("UPDATE shopping_list SET checked=0 WHERE checked=1");shoppingList();}).show();
    }
    void searchList(){
        page("Pesquisar na lista");
        EditText query=field("Digite o produto","",false);
        button("Buscar",()->searchListResults(query.getText().toString().trim()));
        button("Voltar",()->shoppingList());
    }
    void searchListResults(String term){
        page("Resultados da lista");
        try(Cursor c=db.rawQuery("SELECT id,name,quantity,checked FROM shopping_list WHERE name LIKE ? ORDER BY checked,name COLLATE NOCASE LIMIT 200",new String[]{"%"+term+"%"})){
            if(c.getCount()==0)screen.addView(label("Nenhum produto encontrado.",16));
            while(c.moveToNext()){
                long id=c.getLong(0);String name=c.getString(1);int qty=c.getInt(2),checked=c.getInt(3);
                button((checked==1?"✓ ":"○ ")+name+" × "+qty,()->{db.execSQL("UPDATE shopping_list SET checked=? WHERE id=?",new Object[]{checked==1?0:1,id});searchListResults(term);});
                button("Editar "+name,()->editListItem(id,name,qty));
            }
        }
        button("Nova pesquisa",()->searchList());
        button("Voltar",()->shoppingList());
    }
    void shareShoppingList(){
        StringBuilder b=new StringBuilder("RegsCo - Lista de compras\n");
        int count=0;
        try(Cursor c=db.rawQuery("SELECT name,quantity,unit FROM shopping_list WHERE checked=0 ORDER BY name COLLATE NOCASE",null)){
            while(c.moveToNext()){b.append("• ").append(c.getString(0)).append(" × ").append(c.getLong(1)).append(" ").append(c.getString(2)).append("\n");count++;}
        }
        if(count==0){message("Sua lista não possui produtos pendentes");return;}
        Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT,b.toString());
        startActivity(Intent.createChooser(send,"Compartilhar lista"));
    }
    void pendingListSummary(){
        page("Resumo da lista");
        long products=scalar("SELECT COUNT(*) FROM shopping_list WHERE checked=0",null);
        long units=scalar("SELECT COALESCE(SUM(quantity),0) FROM shopping_list WHERE checked=0",null);
        long completed=scalar("SELECT COUNT(*) FROM shopping_list WHERE checked=1",null);
        card("PRODUTOS PENDENTES",""+products,"Total de unidades: "+units);
        card("PRODUTOS CONCLUÍDOS",""+completed,"Itens já marcados como comprados");
        button("Compartilhar lista",()->shareShoppingList());
        button("Abrir lista",()->shoppingList());
        button("Voltar",()->home());
    }
    void startScan(EditText name,EditText price){
        scanNameTarget=name;scanPriceTarget=price;
        if(name==null){scanBarcodeTarget=null;scanBrandTarget=null;scanUnitTarget=null;}
        startActivityForResult(new Intent(this,ScanActivity.class).putExtra("theme",theme),CAMERA_SCAN);
    }
    void discardScanPhoto(){
        if(scanPhotoUri==null)return;
        try{getContentResolver().delete(scanPhotoUri,null,null);}catch(Exception ignored){}
        scanPhotoUri=null;
    }
    void handleCameraResult(Intent data){
        if(data==null)return;
        String detected=data.getStringExtra("text");if(detected==null)detected="";
        String barcode=data.getStringExtra("barcode");
        if(scanBarcodeTarget!=null&&barcode!=null&&!barcode.isEmpty()){scanBarcodeTarget.setText(barcode);if(scanNameTarget!=null&&scanBrandTarget!=null&&scanUnitTarget!=null)lookupBarcode(barcode,scanNameTarget,scanBrandTarget,scanUnitTarget);}
        final String result=detected+(barcode==null||barcode.isEmpty()?"":"\nCódigo: "+barcode);
        ArrayList<String> prices=LabelParser.prices(detected);
        if(prices.isEmpty()){new AlertDialog.Builder(this).setTitle("Texto reconhecido").setMessage(result+"\nConfira e preencha os campos manualmente.").setPositiveButton("OK",null).show();return;}
        new AlertDialog.Builder(this).setTitle("Confira a etiqueta")
            .setMessage(result+"\n\nVerifique varejo, atacado e quantidade mínima antes de selecionar o preço.")
            .setPositiveButton("Escolher preço",(d,w)->new AlertDialog.Builder(this).setTitle("Preço unitário")
                .setItems(prices.toArray(new String[0]),(dialog,which)->{if(scanPriceTarget!=null)scanPriceTarget.setText(prices.get(which));})
                .setNegativeButton("Cancelar",null).show())
            .setNegativeButton("Cancelar",null).show();
    }
    void choosePeriod(){page("Mês de referência");EditText m=field("Mês (1-12)",""+month,true),y=field("Ano",""+year,true);button("Aplicar",()->{try{int mm=Integer.parseInt(m.getText().toString()),yy=Integer.parseInt(y.getText().toString());if(mm<1||mm>12||yy<2000||yy>2200)throw new Exception();month=mm;year=yy;home();}catch(Exception e){message("Informe mês e ano válidos");}});button("Voltar",()->home());}
    void editBudget(){page("Orçamento de "+period());EditText e=field("Orçamento em R$",money(budget()).replace("R$","").trim(),true);button("Salvar",()->{long c=parse(e.getText().toString());if(c<0){message("Valor inválido");return;}db.execSQL("INSERT OR REPLACE INTO budgets(month,year,cents) VALUES(?,?,?)",new Object[]{month,year,c});home();});button("Voltar",()->home());}
    void newSession(){page("Nova compra");EditText name=field("Nome (ex.: Feira do mês)","",false),store=field("Supermercado ou loja","",false);button("Escolher loja",()->chooseStore(store));button("Criar compra",()->{String n=name.getText().toString().trim(),st=store.getText().toString().trim();if(n.isEmpty()){message("Informe o nome da compra");return;}android.content.ContentValues v=new android.content.ContentValues();v.put("name",n);v.put("store",st);v.put("month",month);v.put("year",year);v.put("created",System.currentTimeMillis());v.put("finished",0);long id=db.insert("sessions",null,v);if(id>0)editSession(id);else message("Erro ao criar compra");});button("Cancelar",()->home());}
    void sessions(){historyPage("","","","","",0);}
    void historyPage(String term,String store,String category,String mm,String yy,int offset){
        page("Histórico de compras");
        EditText query=field("Nome da compra ou produto",term,false),shop=field("Loja (vazio: todas)",store,false),cat=field("Categoria (vazio: todas)",category,false),m=field("Mês (vazio: todos)",mm,true),y=field("Ano (vazio: todos)",yy,true);
        button("Aplicar filtros",()->{String monthFilter=m.getText().toString().trim(),yearFilter=y.getText().toString().trim();if((!monthFilter.isEmpty()&&(positive(monthFilter)<1||positive(monthFilter)>12))||(!yearFilter.isEmpty()&&(positive(yearFilter)<2000||positive(yearFilter)>2200))){message("Mês ou ano inválido");return;}historyPage(query.getText().toString().trim(),shop.getText().toString().trim(),cat.getText().toString().trim(),monthFilter,yearFilter,0);});
        button("Compras abertas",()->activeSessions());
        String sql="SELECT s.id,s.name,s.store,s.month,s.year,s.finished,COALESCE(SUM(i.quantity*i.unit_cents-i.discount_cents),0) FROM sessions s LEFT JOIN items i ON i.session_id=s.id WHERE (s.name LIKE ? OR EXISTS(SELECT 1 FROM items n WHERE n.session_id=s.id AND n.name LIKE ?)) AND (?='' OR s.month=?) AND (?='' OR s.year=?) AND (?='' OR s.store LIKE ? OR EXISTS(SELECT 1 FROM items t WHERE t.session_id=s.id AND t.store LIKE ?)) AND (?='' OR EXISTS(SELECT 1 FROM items k WHERE k.session_id=s.id AND k.category=?)) GROUP BY s.id ORDER BY s.year DESC,s.month DESC,s.created DESC,s.id DESC LIMIT 101 OFFSET "+offset;
        boolean more=false;try(Cursor c=db.rawQuery(sql,new String[]{"%"+term+"%","%"+term+"%",mm,mm,yy,yy,store,"%"+store+"%","%"+store+"%",category,category})){
            if(c.getCount()==0)screen.addView(label("Nenhuma compra encontrada.",16));int count=0;
            while(c.moveToNext()){if(count++==100){more=true;break;}long id=c.getLong(0);button(c.getString(1)+" • "+c.getString(2)+"\n"+c.getInt(3)+"/"+c.getInt(4)+" • "+(c.getInt(5)==1?"Finalizada":"Em andamento")+" • "+money(c.getLong(6)),()->editSession(id));}
        }
        if(offset>0)button("Página anterior",()->historyPage(term,store,category,mm,yy,Math.max(0,offset-100)));
        if(more)button("Próxima página",()->historyPage(term,store,category,mm,yy,offset+100));button("Voltar",()->home());
    }
    void editSession(long id){try(Cursor c=db.rawQuery("SELECT name,store,month,year,finished FROM sessions WHERE id=?",new String[]{""+id})){if(!c.moveToFirst()){sessions();return;}String name=c.getString(0),store=c.getString(1);int m=c.getInt(2),y=c.getInt(3),done=c.getInt(4);page(name);if(scalar("SELECT planned FROM sessions WHERE id=?",new String[]{""+id})==1)screen.addView(label("Situação: Planejada",17));screen.addView(label(store+" • "+m+"/"+y+" • "+(done==1?"Finalizada":"Em andamento"),17));long sum=scalar("SELECT COALESCE(SUM(quantity*unit_cents-discount_cents),0) FROM items WHERE session_id=?",new String[]{""+id});screen.addView(label("Total da compra: "+money(sum),21));
        try(Cursor items=db.rawQuery("SELECT id,name,category,quantity,unit_cents,discount_cents,unit,brand,store FROM items WHERE session_id=? ORDER BY id DESC",new String[]{""+id})){while(items.moveToNext()){long itemId=items.getLong(0);String itemName=items.getString(1);int qty=items.getInt(3);long unit=items.getLong(4);screen.addView(label(itemName+" • "+items.getString(2)+"\n"+qty+" × "+money(unit)+" = "+money(qty*unit-items.getLong(5))+" • "+items.getString(6)+"\n"+items.getString(7)+" • "+items.getString(8),16));button("Editar "+itemName,()->editItem(id,itemId));button("Excluir "+itemName,()->new AlertDialog.Builder(this).setMessage("Excluir este item?").setPositiveButton("Excluir",(d,w)->{db.execSQL("DELETE FROM items WHERE id=?",new Object[]{itemId});editSession(id);}).setNegativeButton("Cancelar",null).show());}}
        if(done==0){button("Adicionar produto",()->addItem(id));button("Importar produto da lista",()->importListItem(id));button("Finalizar compra",()->new AlertDialog.Builder(this).setMessage("Finalizar compra e contabilizar no orçamento de "+m+"/"+y+"?").setPositiveButton("Finalizar",(d,w)->{db.execSQL("UPDATE sessions SET finished=1,planned=0 WHERE id=?",new Object[]{id});editSession(id);}).setNegativeButton("Cancelar",null).show());}
        else button("Reabrir para editar",()->{db.execSQL("UPDATE sessions SET finished=0 WHERE id=?",new Object[]{id});editSession(id);});
        if(done==0)button(scalar("SELECT planned FROM sessions WHERE id=?",new String[]{""+id})==1?"Iniciar compra planejada":"Marcar como planejada",()->{db.execSQL("UPDATE sessions SET planned=1-planned WHERE id=?",new Object[]{id});editSession(id);});
        if(done==1)button("Adicionar produtos ao estoque",()->stockPurchase(id));
        button("Editar dados da compra",()->editSessionDetails(id));
        button("Duplicar como compra aberta",()->duplicateSession(id));
        button("Excluir compra",()->new AlertDialog.Builder(this).setMessage("Excluir a compra e todos os seus produtos?").setPositiveButton("Excluir",(d,w)->{db.beginTransaction();try{db.execSQL("DELETE FROM items WHERE session_id=?",new Object[]{id});db.execSQL("DELETE FROM sessions WHERE id=?",new Object[]{id});db.setTransactionSuccessful();}finally{db.endTransaction();}sessions();}).setNegativeButton("Cancelar",null).show());button("Voltar ao histórico",()->sessions());}}
    void addItem(long id){itemForm(id,0);}
    void importListItem(long sessionId){
        page("Importar da lista");
        screen.addView(label("Escolha um produto pendente e informe o preço pago.",15));
        try(Cursor c=db.rawQuery("SELECT id,name,quantity FROM shopping_list WHERE checked=0 ORDER BY id DESC",null)){
            if(c.getCount()==0)screen.addView(label("Não há produtos pendentes na lista.",15));
            while(c.moveToNext()){
                long listId=c.getLong(0);String name=c.getString(1);int qty=c.getInt(2);
                button(name+" × "+qty,()->priceImportedItem(sessionId,listId,name,qty));
            }
        }
        button("Voltar",()->editSession(sessionId));
    }
    void priceImportedItem(long sessionId,long listId,String name,int qty){
        page("Preço de "+name);
        try(Cursor c=db.rawQuery("SELECT unit FROM shopping_list WHERE id=?",new String[]{""+listId})){if(c.moveToFirst())screen.addView(label("Informe o preço por "+c.getString(0)+". Quantidade: "+qty,16));}
        EditText category=field("Categoria (ex.: Alimentação)","",false);
        EditText price=field("Preço unitário em R$","",true);
        button("Escolher categoria",()->chooseCategory(category));
        button("Ler etiqueta pela câmera",()->startScan(null,price));
        button("Adicionar à compra",()->{
            long cents=parse(price.getText().toString());
            if(!Money.validLine(qty,cents)){message("Informe um preço válido");return;}
            if(scalar("SELECT checked FROM shopping_list WHERE id=?",new String[]{""+listId})!=0){message("Este item já foi importado");return;}
            db.beginTransaction();
            try{
                db.execSQL("INSERT INTO items(session_id,name,category,quantity,unit_cents) VALUES(?,?,?,?,?)",
                    new Object[]{sessionId,name,category.getText().toString().trim(),qty,cents});
                db.execSQL("UPDATE items SET unit=(SELECT unit FROM shopping_list WHERE id=?) WHERE id=last_insert_rowid()",new Object[]{listId});
                db.execSQL("UPDATE shopping_list SET checked=1 WHERE id=?",new Object[]{listId});
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            editSession(sessionId);
        });
        button("Voltar à lista",()->importListItem(sessionId));
    }
    void editItem(long sessionId,long itemId){itemForm(sessionId,itemId);}
    EditText scanBarcodeTarget,scanBrandTarget,scanUnitTarget;
    String itemText(long id,String column,String fallback){if(id==0)return fallback;try(Cursor c=db.rawQuery("SELECT "+column+" FROM items WHERE id=?",new String[]{""+id})){return c.moveToFirst()?c.getString(0):fallback;}}
    String amountInput(long cents){return java.math.BigDecimal.valueOf(cents,2).toPlainString().replace('.',',');}
    void itemForm(long session,long id){
        page(id==0?"Adicionar produto":"Editar produto");
        EditText name=field("Produto",itemText(id,"name",""),false),brand=field("Marca / apresentação / dosagem",itemText(id,"brand",""),false),barcode=field("Código de barras",itemText(id,"barcode",""),false);
        EditText category=field("Categoria",itemText(id,"category","Alimentação"),false);
        button("Escolher categoria",()->chooseCategory(category));
        EditText qty=field("Quantidade inteira na unidade escolhida",itemText(id,"quantity","1"),true),unit=field("Unidade (un, pacote, kg, g, L, mL)",itemText(id,"unit","un"),false);
        long retail=Long.parseLong(itemText(id,"retail_cents","0"));if(retail==0)retail=Long.parseLong(itemText(id,"unit_cents","0"));
        EditText price=field("Preço de varejo por unidade (R$)",id==0?"":amountInput(retail),true);
        scanBarcodeTarget=barcode;scanBrandTarget=brand;scanUnitTarget=unit;
        button("Fotografar etiqueta ou código",()->{scanBarcodeTarget=barcode;startScan(name,price);});
        button("Buscar código no histórico local",()->lookupBarcode(barcode.getText().toString().trim(),name,brand,unit));
        EditText wholesale=field("Preço atacado (R$, opcional)",amountInput(Long.parseLong(itemText(id,"wholesale_cents","0"))),true),minimum=field("Quantidade mínima atacado (0 desativa)",itemText(id,"wholesale_min","0"),true);
        EditText discount=field("Desconto total deste item (R$)",amountInput(Long.parseLong(itemText(id,"discount_cents","0"))),true),store=field("Loja deste produto (vazio usa a compra)",itemText(id,"store",""),false),notes=field("Observações",itemText(id,"notes",""),false);
        button("Escolher loja do produto",()->chooseStore(store));
        button("Conferir total e salvar",()->{
            String n=name.getText().toString().trim(),u=unit.getText().toString().trim();int q=positive(qty.getText().toString());long r=parse(price.getText().toString()),w=parse(wholesale.getText().toString()),d=parse(discount.getText().toString());int min;
            try{min=Integer.parseInt(minimum.getText().toString());}catch(Exception e){min=-1;}
            if(n.isEmpty()||u.isEmpty()||q<1||r<0||w<0||d<0||min<0){message("Confira produto, unidade, quantidade e preços");return;}
            long effective=min>0&&q>=min?w:r;if(!Money.validLine(q,effective)||d>q*effective){message("Desconto ou total inválido");return;}
            android.content.ContentValues values=new android.content.ContentValues();values.put("session_id",session);values.put("name",n);values.put("brand",brand.getText().toString().trim());values.put("barcode",barcode.getText().toString().trim());values.put("category",category.getText().toString().trim());values.put("quantity",q);values.put("unit",u);values.put("unit_cents",effective);values.put("retail_cents",r);values.put("wholesale_cents",w);values.put("wholesale_min",min);values.put("discount_cents",d);values.put("store",store.getText().toString().trim());values.put("notes",notes.getText().toString().trim());
            new AlertDialog.Builder(this).setTitle("Confirmar produto").setMessage(q+" "+u+" × "+money(effective)+" − "+money(d)+" = "+money(q*effective-d)+(min>0&&q>=min?"\nPreço de atacado aplicado.":"\nPreço de varejo aplicado."))
                .setNegativeButton("Revisar",null).setPositiveButton("Salvar",(dialog,which)->{if(id==0)db.insertOrThrow("items",null,values);else db.update("items",values,"id=? AND session_id=?",new String[]{""+id,""+session});editSession(session);}).show();
        });
        button("Cancelar",()->editSession(session));
    }
    void lookupBarcode(String code,EditText name,EditText brand,EditText unit){
        if(code.isEmpty()){message("Informe ou fotografe o código");return;}
        try(Cursor c=db.rawQuery("SELECT name,brand,unit FROM items WHERE barcode=? ORDER BY id DESC LIMIT 1",new String[]{code})){
            if(!c.moveToFirst()){message("Código não encontrado localmente. Cadastre os dados manualmente.");return;}
            name.setText(c.getString(0));brand.setText(c.getString(1));unit.setText(c.getString(2));message("Produto recuperado do histórico. Confira os dados e o preço atual.");
        }
    }
    String report(){
        StringBuilder b=new StringBuilder("RELATÓRIO REGSCO — "+period()+"\n");
        b.append("Orçamento: ").append(money(budget())).append("\nGasto finalizado: ").append(money(spent())).append("\nSaldo: ").append(money(budget()-spent())).append("\n");
        String[] args={""+month,""+year};
        b.append("Compras finalizadas: ").append(scalar("SELECT COUNT(*) FROM sessions WHERE finished=1 AND month=? AND year=?",args)).append("\nCompras em andamento: ").append(scalar("SELECT COUNT(*) FROM sessions WHERE finished=0 AND month=? AND year=?",args)).append("\n\n");
        try(Cursor c=db.rawQuery("SELECT CASE WHEN TRIM(i.category)='' THEN 'Sem categoria' ELSE i.category END,SUM(i.quantity*i.unit_cents-i.discount_cents) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=? GROUP BY 1 ORDER BY 2 DESC",args)){while(c.moveToNext())b.append(c.getString(0)).append(": ").append(money(c.getLong(1))).append("\n");}
        return b.toString();
    }
    void share(){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,report());startActivity(Intent.createChooser(i,"Compartilhar relatório"));}
    void themePage(){
        page("Escolha seu tema");screen.addView(label("Três formas de organizar seu dia. A escolha fica salva no aparelho.",14));
        for(int i=0;i<themes.length;i++){final int choice=i;LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setPadding(dp(18),dp(20),dp(18),dp(20));tile.setMinimumHeight(dp(155));tile.setBackground(new ThemeSkin.Backdrop(i));margin(tile,8,8);TextView name=label(themes[i]+(theme==i?"  ✓":""),21);name.setTextColor(ThemeSkin.text(i));name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);tile.addView(name);TextView detail=label(i==0?"Verde, bege suave e folhagens":i==1?"Rosa suave, lavanda e luz delicada":"Roxo profundo, estrelas e brilho rosa",14);detail.setTextColor(ThemeSkin.secondary(i));tile.addView(detail);tile.setOnClickListener(v->{theme=choice;db.execSQL("INSERT OR REPLACE INTO settings(key,value) VALUES('theme',?)",new Object[]{""+choice});themePage();});tile.setFocusable(true);screen.addView(tile);}
        primaryButton("Aplicar e voltar ao início",()->home());
    }
    void pharmacyPage(){
        page("Farmácia");
        screen.addView(label("Organize compras de medicamentos e produtos de farmácia. Informe apresentação e dosagem no cadastro; nenhuma substituição é recomendada pelo aplicativo.",14));
        String[] args={""+month,""+year};
        long total=scalar("SELECT COALESCE(SUM(i.quantity*i.unit_cents-i.discount_cents),0) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=? AND i.category='Farmácia'",args);
        card("GASTOS EM "+period(),money(total),"Incluídos no orçamento doméstico geral");
        button("Nova compra de farmácia",()->newSession());
        button("Lista de produtos",()->shoppingList());
        button("Comparar preços registrados",()->comparePrices());
        section("Histórico de farmácia");
        try(Cursor c=db.rawQuery("SELECT s.id,i.name,i.brand,i.quantity,i.unit_cents,i.discount_cents,COALESCE(NULLIF(i.store,''),s.store),s.month,s.year,s.finished FROM items i JOIN sessions s ON s.id=i.session_id WHERE i.category='Farmácia' ORDER BY s.created DESC,i.id DESC",null)){
            if(c.getCount()==0)screen.addView(label("Nenhum produto cadastrado na categoria Farmácia. Selecione essa categoria ao adicionar um produto.",15));
            while(c.moveToNext()){long id=c.getLong(0);button(c.getString(1)+" • "+c.getString(2)+"\n"+money(c.getLong(3)*c.getLong(4)-c.getLong(5))+" • "+c.getString(6)+" • "+c.getInt(7)+"/"+c.getInt(8)+" • "+(c.getInt(9)==1?"Finalizada":"Em andamento"),()->editSession(id));}
        }
        button("Voltar",()->home());
    }
    void confirmDelete(String text,Runnable action){new AlertDialog.Builder(this).setMessage(text).setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(d,w)->action.run()).show();}
    @Override public void onBackPressed(){if(busy)return;if(atHome)super.onBackPressed();else home();}
    void editSessionDetails(long id){
        try(Cursor c=db.rawQuery("SELECT name,store,month,year FROM sessions WHERE id=?",new String[]{""+id})){
            if(!c.moveToFirst())return;
            page("Editar compra");
            EditText n=field("Nome",c.getString(0),false),s=field("Estabelecimento",c.getString(1),false),m=field("Mês",""+c.getInt(2),true),y=field("Ano",""+c.getInt(3),true);
            button("Escolher loja",()->chooseStore(s));
            button("Salvar",()->{int mm=positive(m.getText().toString()),yy=positive(y.getText().toString());String name=n.getText().toString().trim();if(name.isEmpty()||mm<1||mm>12||yy<2000||yy>2200){message("Confira nome, mês e ano");return;}db.execSQL("UPDATE sessions SET name=?,store=?,month=?,year=? WHERE id=?",new Object[]{name,s.getText().toString().trim(),mm,yy,id});editSession(id);});
            button("Cancelar",()->editSession(id));
        }
    }
    void duplicateSession(long id){
        db.beginTransaction();long copy;
        try{
            db.execSQL("INSERT INTO sessions(name,store,month,year,finished,created) SELECT name||' (cópia)',store,?,?,0,? FROM sessions WHERE id=?",new Object[]{month,year,System.currentTimeMillis(),id});
            copy=scalar("SELECT last_insert_rowid()",null);
            db.execSQL("INSERT INTO items(session_id,name,category,quantity,unit_cents,barcode,brand,unit,store,notes,retail_cents,wholesale_cents,wholesale_min,discount_cents) SELECT ?,name,category,quantity,unit_cents,barcode,brand,unit,store,notes,retail_cents,wholesale_cents,wholesale_min,discount_cents FROM items WHERE session_id=?",new Object[]{copy,id});
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        editSession(copy);
    }
    boolean busy=false;
    interface Job {void run() throws Exception;}
    void background(String title,Job job,Runnable success){
        if(busy)return;
        busy=true;ProgressDialog progress=new ProgressDialog(this);progress.setMessage(title);progress.setCancelable(false);progress.show();
        new Thread(()->{String error=null;try{job.run();}catch(Exception e){error=e.getMessage();}final String result=error;
            runOnUiThread(()->{busy=false;if(isDestroyed()){if(db.isOpen())db.close();return;}progress.dismiss();if(result==null)success.run();else message("Operação não concluída: "+result);});
        },"regsco-storage").start();
    }
    @Override protected void onDestroy(){if(!busy&&db!=null&&db.isOpen())db.close();super.onDestroy();}
}
