package com.regsco.compras;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.widget.*;
import java.util.*;
import java.time.*;
final class ReceiptReview {
 final MainActivity app;final String raw,hash;final List<ReceiptParser.Row> rows;final List<EditText[]> fields=new ArrayList<>();EditText shop,date,total;String shopValue="",dateValue=LocalDate.now().toString(),totalValue="";
 ReceiptReview(MainActivity a,String text){app=a;raw=text;hash=ReceiptParser.fingerprint(text);rows=ReceiptParser.parse(text);if(rows.isEmpty())rows.add(new ReceiptParser.Row("","1","un",""));}
 void sync(){for(int i=0;i<fields.size();i++){EditText[] f=fields.get(i);ReceiptParser.Row r=rows.get(i);r.name=f[0].getText().toString();r.quantity=f[1].getText().toString();r.unit=f[2].getText().toString();r.price=f[3].getText().toString();r.discount=f[4].getText().toString();}if(shop!=null){shopValue=shop.getText().toString();dateValue=date.getText().toString();totalValue=total.getText().toString();}}
 void show(){app.page("Revisar nota fiscal");app.draftType="receipt";fields.clear();app.screen.addView(app.label("Confira todos os produtos, quantidades e valores. A nota só entra no orçamento ao confirmar. Para produtos pesados, um lote representa a quantidade descrita.",14));
  app.button("Ver texto reconhecido",()->new AlertDialog.Builder(app).setTitle("Texto da nota — confira o original").setMessage(raw).setPositiveButton("OK",null).show());
  shop=app.field("Estabelecimento",shopValue,false);date=app.field("Data AAAA-MM-DD",dateValue,false);total=app.field("Total dos produtos revisados (R$)",totalValue,true);
  for(int i=0;i<rows.size();i++){final int index=i;ReceiptParser.Row r=rows.get(i);app.section("Produto "+(i+1));EditText n=app.field("Nome — use o mesmo do cadastro",r.name,false),q=app.field("Quantidade inteira (ou 1 lote pesado)",r.quantity,true),u=app.field("Unidade / lote",r.unit,false),p=app.field("Preço unitário (R$)",r.price,true),discount=app.field("Desconto da linha (R$)",r.discount,true);fields.add(new EditText[]{n,q,u,p,discount});app.button("Usar nome de produto já comprado",()->{
   ArrayList<String> names=new ArrayList<>();try(Cursor c=app.db.rawQuery("SELECT DISTINCT name FROM items ORDER BY name COLLATE NOCASE",null)){while(c.moveToNext())names.add(c.getString(0));}
   new AlertDialog.Builder(app).setTitle("Associar ao histórico").setItems(names.toArray(new String[0]),(d,w)->n.setText(names.get(w))).setNegativeButton("Cancelar",null).show();});app.button("Remover este item da revisão",()->{sync();rows.remove(index);show();});}
  app.button("Adicionar linha que faltou",()->{sync();rows.add(new ReceiptParser.Row("","1","un",""));show();});
  app.primaryButton("Conferir e registrar nota",()->save());app.button("Cancelar importação",()->new AlertDialog.Builder(app).setMessage("Descartar a revisão desta nota?").setPositiveButton("Descartar",(d,w)->app.home()).setNegativeButton("Continuar revisando",null).show());
 }
 void save(){sync();try{
  LocalDate day=LocalDate.parse(dateValue.trim());if(day.getYear()<2000||day.getYear()>2200||shopValue.trim().isEmpty()||rows.isEmpty())throw new IllegalArgumentException();long sum=0;
  for(var r:rows){long q=Long.parseLong(r.quantity.trim()),p=Money.parse(r.price);if(r.name.trim().isEmpty()||r.unit.trim().isEmpty()||q<1||!Money.validLine(q,p)||p<0)throw new IllegalArgumentException();long discount=Money.parse(r.discount);if(discount<0||discount>q*p)throw new IllegalArgumentException();sum=Math.addExact(sum,Math.multiplyExact(q,p)-discount);}
  long expected=Money.parse(totalValue);if(expected!=sum){app.message("Os itens somam "+app.money(sum)+". Confira descontos, linhas e o total informado antes de salvar.");return;}
  final long totalCents=sum;
  new AlertDialog.Builder(app).setTitle("Registrar compra da nota?").setMessage(rows.size()+" produtos • "+app.money(sum)+"\n"+shopValue+" • "+dateValue+"\nSerá uma compra finalizada no orçamento desse mês. Se a compra já está no histórico, cancele para não duplicá-la.")
   .setNegativeButton("Revisar",null).setPositiveButton("Registrar",(d,w)->{try{long id=insert(app,rows,shopValue,day,totalCents,hash,raw);app.editSession(id);}catch(IllegalStateException e){app.message(e.getMessage());}catch(Exception e){app.message("Não foi possível salvar. Nenhum item da nota foi registrado.");}}).show();
 }catch(Exception e){app.message("Confira estabelecimento, data, nomes, unidades, quantidades e preços. Linhas incertas precisam de correção manual.");}}
 static long insert(MainActivity a,List<ReceiptParser.Row> rows,String shop,LocalDate day,long total,String hash,String raw){
  a.db.beginTransaction();try{
   if(a.scalar("SELECT COUNT(*) FROM sessions WHERE receipt_hash=?",new String[]{hash})>0)throw new IllegalStateException("Esta nota já foi importada. Abra a compra existente no histórico.");
   long sum=0;for(var r:rows){long q=Long.parseLong(r.quantity),p=Money.parse(r.price);if(q<=0||p<0||r.name.trim().isEmpty()||r.unit.trim().isEmpty()||!Money.validLine(q,p))throw new IllegalArgumentException();long discount=Money.parse(r.discount);if(discount<0||discount>q*p)throw new IllegalArgumentException();sum=Math.addExact(sum,Math.multiplyExact(q,p)-discount);}if(rows.isEmpty()||sum!=total)throw new IllegalArgumentException();
   ContentValues s=new ContentValues();s.put("name","Nota fiscal — "+shop);s.put("store",shop);s.put("month",day.getMonthValue());s.put("year",day.getYear());s.put("created",day.atTime(12,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());s.put("finished",1);s.put("receipt_hash",hash);s.put("receipt_text",raw);long id=a.db.insertOrThrow("sessions",null,s);
   for(var r:rows){ContentValues v=new ContentValues();v.put("session_id",id);v.put("name",r.name.trim());v.put("quantity",Long.parseLong(r.quantity));v.put("unit",r.unit.trim());v.put("unit_cents",Money.parse(r.price));v.put("retail_cents",Money.parse(r.price));v.put("discount_cents",Money.parse(r.discount));v.put("category","Outros");v.put("notes","Importado de nota fiscal; valores revisados pelo usuário.");a.db.insertOrThrow("items",null,v);}
   a.db.setTransactionSuccessful();return id;
  }finally{a.db.endTransaction();}
 }
}
