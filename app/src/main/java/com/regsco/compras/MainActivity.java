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
    LinearLayout screen;
    int theme=0;
    int month=Calendar.getInstance().get(Calendar.MONTH)+1, year=Calendar.getInstance().get(Calendar.YEAR);
    final String[] themes={"Verde Natureza","Claro Rosa","Escuro Nebulosa"};
    int bg(){return theme==0?0xfffff8e8:theme==1?0xffffedf5:0xff1c1127;}
    int fg(){return theme==2?Color.WHITE:theme==1?0xff59274c:0xff1e5037;}
    String money(long cents){return NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(cents/100.0);}
    long parse(String value){try {String v=value.trim().replace("R$","").replace(" ",""); if(v.contains(","))v=v.replace(".","").replace(",",".");java.math.BigDecimal amount=new java.math.BigDecimal(v);if(amount.signum()<0)return -1;return amount.movePointRight(2).setScale(0,java.math.RoundingMode.HALF_UP).longValueExact();}catch(Exception e){return -1;}}
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
        if(b>0){long pending=scalar("SELECT COALESCE(SUM(i.quantity*i.unit_cents),0) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=0 AND s.month=? AND s.year=?",new String[]{""+month,""+year});if(pending>0)screen.addView(label("Compras em andamento: "+money(pending)+" • projeção do saldo: "+money(b-spentAmount-pending),14));}
        section("Vamos às compras?");
        button("＋  Iniciar nova compra",()->newSession());
        button("▤  Minhas compras e histórico",()->sessions());
        button("⌕  Buscar compras",()->searchSessions());
        section("Planejamento");
        button("☑  Lista de compras",()->shoppingList());
        button("▣  Estoque da casa",()->pantry());
        button("⚠  Produtos para repor",()->lowStock());
        button("▣  Resumo do planejamento",()->planningSummary());
        button("◉  Ajustar orçamento mensal",()->editBudget());
        button("▦  Escolher mês de referência",()->choosePeriod());
        button("▧  Compras em andamento",()->activeSessions());
        section("Mais opções");
        button("▤  Cesta básica recorrente",()->basketPage());
        button("Limpar itens concluídos",()->confirmClearChecked());
        button("Reativar itens concluídos",()->reopenChecked());
        button("▣  Planejar compra pelo estoque",()->planFromStock());
        button("▥  Relatórios por categoria",()->categoryReport());
        button("▤  Gastos dos últimos 6 meses",()->sixMonthReport());
        button("Resumo por loja",()->storeSummary());
        button("Pesquisar produtos na lista",()->searchList());
        button("Compartilhar lista de compras",()->shareShoppingList());
        button("Resumo de itens pendentes",()->pendingListSummary());
        button("▥  Comparar meses",()->compareMonths());
        button("⌕  Histórico de preços",()->priceHistory());
        button("▦  Comparar preços registrados",()->comparePrices());
        button("▣  Produtos mais comprados",()->topProducts());
        button("⚙  Gerenciar lojas",()->storesPage());
        button("↗  Compartilhar relatório",()->share());
        button("⇩  Exportar relatório CSV",()->exportCsv());
        button("✦  Personalizar aparência",()->themePage());
        button("⇩  Backup e restauração",()->backupPage());
        screen.addView(label("Seus dados ficam armazenados neste aparelho.",12));
    }
    void shoppingList(){
        page("Lista de compras");
        screen.addView(label("Planeje antes de ir ao mercado. Os itens marcados permanecem salvos.",14));
        try(Cursor c=db.rawQuery("SELECT id,name,quantity,checked FROM shopping_list ORDER BY checked,id DESC",null)){
            while(c.moveToNext()){
                long id=c.getLong(0);String name=c.getString(1);int qty=c.getInt(2),checked=c.getInt(3);
                button((checked==1?"✓  ":"○  ")+name+"  × "+qty,()->{db.execSQL("UPDATE shopping_list SET checked=? WHERE id=?",new Object[]{checked==1?0:1,id});shoppingList();});
                button("Editar "+name,()->editListItem(id,name,qty));
                button("Remover "+name,()->{db.execSQL("DELETE FROM shopping_list WHERE id=?",new Object[]{id});shoppingList();});
            }
        }
        section("Adicionar à lista");
        EditText name=field("Nome do produto","",false),quantity=field("Quantidade","1",true);
        button("Adicionar item",()->{int q=positive(quantity.getText().toString());String n=name.getText().toString().trim();if(n.isEmpty()||q<1){message("Informe produto e quantidade");return;}db.execSQL("INSERT INTO shopping_list(name,quantity) VALUES(?,?)",new Object[]{n,q});shoppingList();});
        button("Voltar",()->home());
    }
    int positive(String value){try{int n=Integer.parseInt(value.trim());return n>0?n:0;}catch(Exception e){return 0;}}
    void pantry(){
        page("Estoque da casa");
        screen.addView(label("Controle os produtos disponíveis e suas quantidades.",14));
        try(Cursor c=db.rawQuery("SELECT id,name,quantity FROM pantry ORDER BY name",null)){
            while(c.moveToNext()){
                long id=c.getLong(0);String name=c.getString(1);int q=c.getInt(2);
                screen.addView(label(name+"  •  "+q+" unidade(s)",17));
                button("−  Consumir 1",()->{db.execSQL("UPDATE pantry SET quantity=quantity-1 WHERE id=? AND quantity>0",new Object[]{id});pantry();});
                button("＋  Adicionar 1",()->{db.execSQL("UPDATE pantry SET quantity=quantity+1 WHERE id=?",new Object[]{id});pantry();});
                button("Excluir "+name,()->new AlertDialog.Builder(this).setMessage("Excluir do estoque?").setPositiveButton("Excluir",(d,w)->{db.execSQL("DELETE FROM pantry WHERE id=?",new Object[]{id});pantry();}).setNegativeButton("Cancelar",null).show());
            }
        }
        button("Repor estoque usando lista de compras",()->restockFromList());
        section("Cadastrar no estoque");
        EditText name=field("Produto","",false),quantity=field("Quantidade","1",true);
        button("Salvar produto",()->{String n=name.getText().toString().trim();int q=positive(quantity.getText().toString());if(n.isEmpty()||q==0){message("Informe produto e quantidade");return;}db.execSQL("INSERT INTO pantry(name,quantity) VALUES(?,?)",new Object[]{n,q});pantry();});
        button("Voltar",()->home());
    }
    static final int EXPORT_BACKUP=701, IMPORT_BACKUP=702, EXPORT_CSV=703;
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
        JSONObject root=new JSONObject();root.put("format","regsco-backup");root.put("version",3);
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
        if(!"regsco-backup".equals(root.optString("format"))||(root.optInt("version")<1||root.optInt("version")>3))throw new Exception("Formato de backup incompatível");
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
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        try(Cursor c=db.rawQuery("SELECT value FROM settings WHERE key='theme'",null)){theme=c.moveToFirst()?Integer.parseInt(c.getString(0)):0;}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK)return;
        if(request==CAMERA_SCAN){handleCameraResult(data);return;}
        if(data==null||data.getData()==null)return;
        try{
            if(request==EXPORT_CSV){
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                    if(out==null)throw new IOException("Não foi possível abrir o arquivo");
                    out.write(("\uFEFF"+reportCsv()).getBytes("UTF-8"));
                }
                message("Relatório CSV exportado");
            }else if(request==EXPORT_BACKUP){
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                    if(out==null)throw new IOException("Não foi possível abrir o arquivo");
                    out.write(exportData().toString(2).getBytes("UTF-8"));
                }
                message("Backup exportado com sucesso");
            }else if(request==IMPORT_BACKUP){
                ByteArrayOutputStream buffer=new ByteArrayOutputStream();
                try(InputStream in=getContentResolver().openInputStream(data.getData())){
                    if(in==null)throw new IOException("Não foi possível ler o arquivo");
                    byte[] bytes=new byte[8192];int n;
                    while((n=in.read(bytes))!=-1){buffer.write(bytes,0,n);}
                }
                restoreData(new JSONObject(buffer.toString("UTF-8")));
                message("Backup restaurado");home();
            }
        }catch(Exception e){message("Erro: "+e.getMessage());}
    }
    void categoryReport(){
        page("Relatório mensal");
        screen.addView(label("Gastos finalizados em "+period(),16));
        card("TOTAL DO MÊS",money(spent()),"Saldo do orçamento: "+money(budget()-spent()));
        section("Gastos por categoria");
        long total=spent();
        try(Cursor c=db.rawQuery("SELECT CASE WHEN TRIM(i.category)='' THEN 'Sem categoria' ELSE i.category END AS category, SUM(i.quantity*i.unit_cents) AS value FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=? GROUP BY category ORDER BY value DESC",new String[]{""+month,""+year})){
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
        try(Cursor c=db.rawQuery("SELECT CASE WHEN TRIM(s.store)='' THEN 'Não informado' ELSE s.store END, SUM(i.quantity*i.unit_cents) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=? GROUP BY s.store ORDER BY 2 DESC",new String[]{""+month,""+year})){
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
        try(Cursor c=db.rawQuery("SELECT i.name,s.store,i.unit_cents,s.month,s.year FROM items i JOIN sessions s ON s.id=i.session_id WHERE i.name LIKE ? COLLATE NOCASE ORDER BY s.created DESC,i.id DESC LIMIT 100",new String[]{"%"+term+"%"})){
            if(c.getCount()==0)screen.addView(label("Nenhum preço encontrado.",16));
            while(c.moveToNext()){
                String store=c.getString(1);if(store==null||store.trim().isEmpty())store="Loja não informada";
                screen.addView(label(c.getString(0)+"  •  "+money(c.getLong(2))+" / un.\n"+store+"  •  "+c.getInt(3)+"/"+c.getInt(4),16));
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
        page("Cesta básica recorrente");
        screen.addView(label("Monte uma cesta padrão e envie seus produtos à lista de compras quando precisar repor.",14));
        try(Cursor c=db.rawQuery("SELECT id,name,quantity FROM basket ORDER BY name",null)){
            if(c.getCount()==0)screen.addView(label("Sua cesta ainda está vazia.",15));
            while(c.moveToNext()){
                long id=c.getLong(0);String name=c.getString(1);int qty=c.getInt(2);
                screen.addView(label(name+"  × "+qty,16));
                button("Editar "+name,()->editBasketItem(id,name,qty));
                button("Excluir "+name,()->{db.execSQL("DELETE FROM basket WHERE id=?",new Object[]{id});basketPage();});
            }
        }
        section("Adicionar produto recorrente");
        EditText name=field("Produto","",false),quantity=field("Quantidade","1",true);
        button("Salvar na cesta",()->{String n=name.getText().toString().trim();int q=positive(quantity.getText().toString());if(n.isEmpty()||q<1){message("Informe produto e quantidade");return;}db.execSQL("INSERT INTO basket(name,quantity) VALUES(?,?)",new Object[]{n,q});basketPage();});
        button("Adicionar cesta à lista de compras",()->{
            db.beginTransaction();int count=0;
            try(Cursor c=db.rawQuery("SELECT name,quantity FROM basket",null)){
                while(c.moveToNext()){
                    try(Cursor existing=db.rawQuery("SELECT id,quantity FROM shopping_list WHERE LOWER(TRIM(name))=LOWER(TRIM(?)) AND checked=0 LIMIT 1",new String[]{c.getString(0)})){
                        if(existing.moveToFirst())db.execSQL("UPDATE shopping_list SET quantity=? WHERE id=?",new Object[]{Math.max(existing.getInt(1),c.getInt(1)),existing.getLong(0)});
                        else db.execSQL("INSERT INTO shopping_list(name,quantity,checked) VALUES(?,?,0)",new Object[]{c.getString(0),c.getInt(1)});
                    }
                    count++;
                }
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            message(count+" produto(s) enviados à lista");shoppingList();
        });
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
        if(product.isEmpty()){message("Informe um produto");return;}
        page("Comparação de preços");
        screen.addView(label("Pesquisa: "+product,16));
        long min=Long.MAX_VALUE;String cheapest="";
        try(Cursor c=db.rawQuery("SELECT s.store,i.name,i.unit_cents,s.month,s.year FROM items i JOIN sessions s ON s.id=i.session_id WHERE LOWER(TRIM(i.name))=LOWER(TRIM(?)) AND TRIM(s.store)<>'' ORDER BY i.unit_cents ASC LIMIT 50",new String[]{product})){
            if(c.getCount()==0)screen.addView(label("Nenhum registro exato com loja informada.",15));
            while(c.moveToNext()){
                String store=c.getString(0);long cents=c.getLong(2);
                if(cents<min){min=cents;cheapest=store;}
                screen.addView(label(store+"  •  "+money(cents)+" / unidade\n"+c.getInt(3)+"/"+c.getInt(4),16));
            }
        }
        if(min!=Long.MAX_VALUE)card("MENOR PREÇO REGISTRADO",money(min),cheapest+" • histórico, não oferta atual");
        button("Nova comparação",()->comparePrices());button("Voltar",()->home());
    }
    void lowStock(){
        page("Produtos para repor");
        screen.addView(label("Produtos da cesta básica cujo estoque está abaixo da quantidade planejada.",14));
        try(Cursor c=db.rawQuery("SELECT b.name,b.quantity,COALESCE((SELECT SUM(p.quantity) FROM pantry p WHERE LOWER(TRIM(p.name))=LOWER(TRIM(b.name))),0) FROM basket b ORDER BY b.name",null)){
            int count=0;
            while(c.moveToNext()){
                String name=c.getString(0);int required=c.getInt(1),available=c.getInt(2),missing=required-available;
                if(missing<=0)continue;
                count++;
                screen.addView(label(name+"  •  faltam "+missing+" (estoque: "+available+")",16));
                button("Adicionar "+missing+" à lista",()->{
                    try(Cursor existing=db.rawQuery("SELECT id,quantity FROM shopping_list WHERE LOWER(TRIM(name))=LOWER(TRIM(?)) AND checked=0 LIMIT 1",new String[]{name})){
                        if(existing.moveToFirst())db.execSQL("UPDATE shopping_list SET quantity=? WHERE id=?",new Object[]{Math.max(missing,existing.getInt(1)),existing.getLong(0)});
                        else db.execSQL("INSERT INTO shopping_list(name,quantity,checked) VALUES(?,?,0)",new Object[]{name,missing});
                    }
                    message("Lista atualizada");lowStock();
                });
            }
            if(count==0)screen.addView(label("Nenhum item da cesta precisa de reposição.",16));
        }
        button("Voltar",()->home());
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
        try(Cursor c=db.rawQuery("SELECT s.id,s.name,s.store,s.month,s.year,COALESCE(SUM(i.quantity*i.unit_cents),0) FROM sessions s LEFT JOIN items i ON i.session_id=s.id WHERE s.finished=0 GROUP BY s.id ORDER BY s.created DESC",null)){
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
        try(Cursor c=db.rawQuery("SELECT s.id,s.name,s.store,s.month,s.year,s.finished,COALESCE(SUM(i.quantity*i.unit_cents),0) FROM sessions s LEFT JOIN items i ON i.session_id=s.id WHERE s.name LIKE ? OR s.store LIKE ? GROUP BY s.id ORDER BY s.created DESC LIMIT 200",new String[]{"%"+term+"%","%"+term+"%"})){
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
        StringBuilder b=new StringBuilder("Compra;Loja;Mês;Ano;Status;Produto;Categoria;Quantidade;Preço unitário (R$);Total (R$)\r\n");
        try(Cursor c=db.rawQuery("SELECT s.name,s.store,s.month,s.year,s.finished,i.name,i.category,i.quantity,i.unit_cents FROM sessions s JOIN items i ON i.session_id=s.id WHERE s.month=? AND s.year=? ORDER BY s.id,i.id",new String[]{""+month,""+year})){
            while(c.moveToNext()){
                long cents=c.getLong(8);int qty=c.getInt(7);
                b.append(csvCell(c.getString(0))).append(';').append(csvCell(c.getString(1))).append(';')
                .append(c.getInt(2)).append(';').append(c.getInt(3)).append(';')
                .append(csvCell(c.getInt(4)==1?"Finalizada":"Em andamento")).append(';')
                .append(csvCell(c.getString(5))).append(';').append(csvCell(c.getString(6))).append(';')
                .append(qty).append(';').append(csvCell(String.format(Locale.US,"%.2f",cents/100.0).replace('.',','))).append(';')
                .append(csvCell(String.format(Locale.US,"%.2f",(cents*qty)/100.0).replace('.',','))).append("\r\n");
            }
        }
        return b.toString();
    }
    void exportCsv(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/csv");i.putExtra(Intent.EXTRA_TITLE,"RegsCo-relatorio-"+period().replace('/','-')+".csv");
        startActivityForResult(i,EXPORT_CSV);
    }
    void chooseCategory(EditText target){
        String[] choices={"Alimentação","Carnes e frios","Hortifruti","Bebidas","Limpeza","Higiene pessoal","Farmácia","Casa","Outros"};
        new AlertDialog.Builder(this).setTitle("Categoria do produto").setItems(choices,(d,w)->target.setText(choices[w])).setNegativeButton("Cancelar",null).show();
    }
    void planFromStock(){
        page("Planejar pelo estoque");
        screen.addView(label("Confira a cesta básica, compare com o estoque e inclua na lista apenas as quantidades que faltam.",14));
        button("Adicionar todos os itens em falta",()->{
            int added=0;
            db.beginTransaction();
            try(Cursor c=db.rawQuery("SELECT b.name,b.quantity,COALESCE((SELECT SUM(p.quantity) FROM pantry p WHERE LOWER(TRIM(p.name))=LOWER(TRIM(b.name))),0) FROM basket b",null)){
                while(c.moveToNext()){
                    String name=c.getString(0);int missing=c.getInt(1)-c.getInt(2);
                    if(missing<=0)continue;
                    try(Cursor existing=db.rawQuery("SELECT id,quantity FROM shopping_list WHERE LOWER(TRIM(name))=LOWER(TRIM(?)) AND checked=0 LIMIT 1",new String[]{name})){
                        if(existing.moveToFirst())db.execSQL("UPDATE shopping_list SET quantity=? WHERE id=?",new Object[]{Math.max(missing,existing.getInt(1)),existing.getLong(0)});
                        else db.execSQL("INSERT INTO shopping_list(name,quantity,checked) VALUES(?,?,0)",new Object[]{name,missing});
                    }
                    added++;
                }
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            message(added+" produto(s) verificados e incluídos/atualizados");shoppingList();
        });
        button("Ver detalhes da reposição",()->lowStock());
        button("Voltar",()->home());
    }
    void sixMonthReport(){
        page("Gastos dos últimos 6 meses");
        screen.addView(label("Somente compras finalizadas são contabilizadas.",14));
        Calendar cursor=Calendar.getInstance();
        cursor.set(year,month-1,1);
        long sum=0;
        for(int i=0;i<6;i++){
            int m=cursor.get(Calendar.MONTH)+1,y=cursor.get(Calendar.YEAR);
            long value=scalar("SELECT COALESCE(SUM(i.quantity*i.unit_cents),0) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=?",new String[]{""+m,""+y});
            sum+=value;
            screen.addView(label(String.format(Locale.forLanguageTag("pt-BR"),"%02d/%d",m,y)+"  •  "+money(value),17));
            cursor.add(Calendar.MONTH,-1);
        }
        card("TOTAL DE 6 MESES",money(sum),"Média mensal: "+money(sum/6));
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
        long last=scalar("SELECT COALESCE(SUM(i.quantity*i.unit_cents),0) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=?",new String[]{""+pm,""+py});
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
        try(Cursor c=db.rawQuery("SELECT i.name,SUM(i.quantity),SUM(i.quantity*i.unit_cents) FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.month=? AND s.year=? GROUP BY LOWER(TRIM(i.name)) ORDER BY SUM(i.quantity*i.unit_cents) DESC LIMIT 30",new String[]{""+month,""+year})){
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
        try(Cursor c=db.rawQuery("SELECT s.store,COUNT(DISTINCT s.id),COALESCE(SUM(i.quantity*i.unit_cents),0) FROM sessions s LEFT JOIN items i ON i.session_id=s.id WHERE s.finished=1 AND s.month=? AND s.year=? GROUP BY s.store ORDER BY 3 DESC",new String[]{""+month,""+year})){
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
        try(Cursor c=db.rawQuery("SELECT id,name,quantity,checked FROM shopping_list WHERE name LIKE ? ORDER BY checked,id DESC LIMIT 200",new String[]{"%"+term+"%"})){
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
        try(Cursor c=db.rawQuery("SELECT name,quantity FROM shopping_list WHERE checked=0 ORDER BY name COLLATE NOCASE",null)){
            while(c.moveToNext()){b.append("• ").append(c.getString(0)).append(" × ").append(c.getInt(1)).append("\n");count++;}
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
        Intent intent=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager())==null){message("Nenhum aplicativo de câmera disponível");return;}
        startActivityForResult(intent,CAMERA_SCAN);
    }
    void handleCameraResult(Intent data){
        android.graphics.Bitmap bitmap=null;
        if(data!=null&&data.getExtras()!=null){
            Object photo=data.getExtras().get("data");
            if(photo instanceof android.graphics.Bitmap)bitmap=(android.graphics.Bitmap)photo;
        }
        if(bitmap==null){message("A câmera não retornou uma imagem");return;}
        com.google.mlkit.vision.common.InputImage image=com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap,0);
        com.google.mlkit.vision.text.TextRecognizer recognizer=com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image).addOnSuccessListener(result->{
            String detected=result.getText();
            java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("(?:R\\$\\s*)?(\\d{1,5}[,.]\\d{2})").matcher(detected);
            String price="";
            while(matcher.find()){price=matcher.group(1);break;}
            final String foundPrice=price;
            new AlertDialog.Builder(this).setTitle("Texto reconhecido")
                .setMessage((detected.isEmpty()?"Nenhum texto reconhecido. Tente uma foto mais próxima.":detected)+"\\n\\nConfira o preço antes de salvar.")
                .setPositiveButton("Usar preço",(d,w)->{if(scanPriceTarget!=null&&!foundPrice.isEmpty())scanPriceTarget.setText(foundPrice);})
                .setNegativeButton("Cancelar",null).show();
        }).addOnFailureListener(e->message("Não foi possível reconhecer a etiqueta: "+e.getMessage()))
          .addOnCompleteListener(task->recognizer.close());
        com.google.mlkit.vision.barcode.BarcodeScanner scanner=com.google.mlkit.vision.barcode.BarcodeScanning.getClient();
        scanner.process(image).addOnSuccessListener(codes->{
            if(codes.isEmpty())return;
            String value=codes.get(0).getRawValue();
            if(value==null||value.isEmpty())return;
            if(scanNameTarget!=null&&scanNameTarget.getText().toString().trim().isEmpty()){
                new AlertDialog.Builder(this).setTitle("Código de barras detectado")
                    .setMessage(value+"\\nO código não identifica automaticamente o nome do produto.")
                    .setPositiveButton("Preencher código",(d,w)->scanNameTarget.setText(value))
                    .setNegativeButton("Ignorar",null).show();
            }
        }).addOnCompleteListener(task->scanner.close());
    }
    void choosePeriod(){page("Mês de referência");EditText m=field("Mês (1-12)",""+month,true),y=field("Ano",""+year,true);button("Aplicar",()->{try{int mm=Integer.parseInt(m.getText().toString()),yy=Integer.parseInt(y.getText().toString());if(mm<1||mm>12||yy<2000||yy>2200)throw new Exception();month=mm;year=yy;home();}catch(Exception e){message("Informe mês e ano válidos");}});button("Voltar",()->home());}
    void editBudget(){page("Orçamento de "+period());EditText e=field("Orçamento em R$",money(budget()).replace("R$","").trim(),true);button("Salvar",()->{long c=parse(e.getText().toString());if(c<0){message("Valor inválido");return;}db.execSQL("INSERT OR REPLACE INTO budgets(month,year,cents) VALUES(?,?,?)",new Object[]{month,year,c});home();});button("Voltar",()->home());}
    void newSession(){page("Nova compra");EditText name=field("Nome (ex.: Feira do mês)","",false),store=field("Supermercado ou loja","",false);button("Criar compra",()->{String n=name.getText().toString().trim(),st=store.getText().toString().trim();if(n.isEmpty()){message("Informe o nome da compra");return;}android.content.ContentValues v=new android.content.ContentValues();v.put("name",n);v.put("store",st);v.put("month",month);v.put("year",year);v.put("created",System.currentTimeMillis());v.put("finished",0);long id=db.insert("sessions",null,v);if(id>0)editSession(id);else message("Erro ao criar compra");});button("Cancelar",()->home());}
    void sessions(){page("Compras");try(Cursor c=db.rawQuery("SELECT s.id,s.name,s.store,s.month,s.year,s.finished,COALESCE(SUM(i.quantity*i.unit_cents),0) FROM sessions s LEFT JOIN items i ON i.session_id=s.id GROUP BY s.id ORDER BY s.id DESC",null)){while(c.moveToNext()){long id=c.getLong(0);String title=c.getString(1)+" • "+c.getString(2)+"\n"+c.getInt(3)+"/"+c.getInt(4)+" • "+(c.getInt(5)==1?"Finalizada":"Em andamento")+" • "+money(c.getLong(6));button(title,()->editSession(id));}}button("Voltar",()->home());}
    void editSession(long id){try(Cursor c=db.rawQuery("SELECT name,store,month,year,finished FROM sessions WHERE id=?",new String[]{""+id})){if(!c.moveToFirst()){sessions();return;}String name=c.getString(0),store=c.getString(1);int m=c.getInt(2),y=c.getInt(3),done=c.getInt(4);page(name);screen.addView(label(store+" • "+m+"/"+y+" • "+(done==1?"Finalizada":"Em andamento"),17));long sum=scalar("SELECT COALESCE(SUM(quantity*unit_cents),0) FROM items WHERE session_id=?",new String[]{""+id});screen.addView(label("Total da compra: "+money(sum),21));
        try(Cursor items=db.rawQuery("SELECT id,name,category,quantity,unit_cents FROM items WHERE session_id=? ORDER BY id DESC",new String[]{""+id})){while(items.moveToNext()){long itemId=items.getLong(0);String itemName=items.getString(1);int qty=items.getInt(3);long unit=items.getLong(4);screen.addView(label(itemName+" • "+items.getString(2)+"\n"+qty+" × "+money(unit)+" = "+money(qty*unit),16));button("Editar "+itemName,()->editItem(id,itemId));button("Excluir "+itemName,()->new AlertDialog.Builder(this).setMessage("Excluir este item?").setPositiveButton("Excluir",(d,w)->{db.execSQL("DELETE FROM items WHERE id=?",new Object[]{itemId});editSession(id);}).setNegativeButton("Cancelar",null).show());}}
        if(done==0){button("Adicionar produto",()->addItem(id));button("Importar produto da lista",()->importListItem(id));button("Finalizar compra",()->new AlertDialog.Builder(this).setMessage("Finalizar compra e contabilizar no orçamento de "+m+"/"+y+"?").setPositiveButton("Finalizar",(d,w)->{db.execSQL("UPDATE sessions SET finished=1 WHERE id=?",new Object[]{id});editSession(id);}).setNegativeButton("Cancelar",null).show());}
        else button("Reabrir para editar",()->{db.execSQL("UPDATE sessions SET finished=0 WHERE id=?",new Object[]{id});editSession(id);});
        button("Excluir compra",()->new AlertDialog.Builder(this).setMessage("Excluir a compra e todos os seus produtos?").setPositiveButton("Excluir",(d,w)->{db.beginTransaction();try{db.execSQL("DELETE FROM items WHERE session_id=?",new Object[]{id});db.execSQL("DELETE FROM sessions WHERE id=?",new Object[]{id});db.setTransactionSuccessful();}finally{db.endTransaction();}sessions();}).setNegativeButton("Cancelar",null).show());button("Voltar ao histórico",()->sessions());}}
    void addItem(long id){page("Adicionar produto");EditText name=field("Produto","",false),category=field("Categoria (ex.: Alimentos)","",false),quantity=field("Quantidade","1",true),price=field("Preço unitário (R$)","",true);button("Escolher categoria",()->chooseCategory(category));button("Ler etiqueta ou código de barras pela câmera",()->startScan(name,price));button("Salvar produto",()->{String n=name.getText().toString().trim();long cents=parse(price.getText().toString());int q;try{q=Integer.parseInt(quantity.getText().toString());}catch(Exception e){q=0;}if(n.isEmpty()||q<=0||cents<0){message("Preencha produto, quantidade e preço válidos");return;}db.execSQL("INSERT INTO items(session_id,name,category,quantity,unit_cents) VALUES(?,?,?,?,?)",new Object[]{id,n,category.getText().toString().trim(),q,cents});editSession(id);});button("Cancelar",()->editSession(id));}
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
        EditText category=field("Categoria (ex.: Alimentação)","",false);
        EditText price=field("Preço unitário em R$","",true);
        button("Escolher categoria",()->chooseCategory(category));
        button("Ler etiqueta pela câmera",()->startScan(null,price));
        button("Adicionar à compra",()->{
            long cents=parse(price.getText().toString());
            if(cents<0){message("Informe um preço válido");return;}
            db.beginTransaction();
            try{
                db.execSQL("INSERT INTO items(session_id,name,category,quantity,unit_cents) VALUES(?,?,?,?,?)",
                    new Object[]{sessionId,name,category.getText().toString().trim(),qty,cents});
                db.execSQL("UPDATE shopping_list SET checked=1 WHERE id=?",new Object[]{listId});
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            editSession(sessionId);
        });
        button("Voltar à lista",()->importListItem(sessionId));
    }
    void editItem(long sessionId,long itemId){
        try(Cursor c=db.rawQuery("SELECT name,category,quantity,unit_cents FROM items WHERE id=? AND session_id=?",new String[]{""+itemId,""+sessionId})){
            if(!c.moveToFirst()){message("Produto não encontrado");editSession(sessionId);return;}
            page("Editar produto");
            EditText name=field("Produto",c.getString(0),false);
            EditText category=field("Categoria",c.getString(1)==null?"":c.getString(1),false);
            EditText quantity=field("Quantidade",""+c.getInt(2),true);
            EditText price=field("Preço unitário (R$)",money(c.getLong(3)).replace("R$","").trim(),true);
            button("Escolher categoria",()->chooseCategory(category));
            button("Salvar alterações",()->{
                String n=name.getText().toString().trim();
                int q=positive(quantity.getText().toString());
                long cents=parse(price.getText().toString());
                if(n.isEmpty()||q<1||cents<0){message("Preencha produto, quantidade e preço válidos");return;}
                db.execSQL("UPDATE items SET name=?,category=?,quantity=?,unit_cents=? WHERE id=? AND session_id=?",
                    new Object[]{n,category.getText().toString().trim(),q,cents,itemId,sessionId});
                editSession(sessionId);
            });
            button("Cancelar",()->editSession(sessionId));
        }
    }
    String report(){StringBuilder b=new StringBuilder("REGSCO - RELATÓRIO "+period()+"\n");b.append("Orçamento: ").append(money(budget())).append("\nGasto finalizado: ").append(money(spent())).append("\nSaldo: ").append(money(budget()-spent())).append("\n\n");try(Cursor c=db.rawQuery("SELECT s.name,s.store,i.name,i.quantity,i.unit_cents FROM sessions s JOIN items i ON i.session_id=s.id WHERE s.finished=1 AND s.month=? AND s.year=? ORDER BY s.id,i.id",new String[]{""+month,""+year})){while(c.moveToNext())b.append(c.getString(0)).append(" / ").append(c.getString(1)).append(": ").append(c.getString(2)).append(" (").append(c.getInt(3)).append(" × ").append(money(c.getLong(4))).append(")\n");}return b.toString();}
    void share(){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,report());startActivity(Intent.createChooser(i,"Compartilhar relatório"));}
    void themePage(){page("Temas");for(int i=0;i<themes.length;i++){final int n=i;button(themes[i],()->{theme=n;db.execSQL("INSERT OR REPLACE INTO settings(key,value) VALUES('theme',?)",new Object[]{""+n});themePage();});}button("Voltar",()->home());}
    @Override protected void onDestroy(){if(db!=null&&db.isOpen())db.close();super.onDestroy();}
}
