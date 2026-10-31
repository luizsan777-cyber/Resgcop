package com.regsco.compras;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.math.*;
import java.text.*;
import java.util.*;
final class PriceHistory {
 static String describe(SQLiteDatabase db,String name,String barcode,String unit,String brand,long current,long exclude){
  if(name.trim().isEmpty()&&barcode.trim().isEmpty())return "Informe o produto para comparar com a última compra.";
  String sql="SELECT i.name,i.quantity,i.unit_cents,i.discount_cents,i.brand,COALESCE(NULLIF(i.store,''),s.store),s.created FROM items i JOIN sessions s ON s.id=i.session_id WHERE s.finished=1 AND s.id<>? AND i.unit=? AND ((?<>'' AND i.barcode=?) OR (?='' AND LOWER(TRIM(i.name))=LOWER(TRIM(?)) AND (?='' OR LOWER(i.brand)=LOWER(?)))) ORDER BY s.created DESC,s.id DESC,i.id DESC LIMIT 1";
  try(Cursor c=db.rawQuery(sql,new String[]{""+exclude,unit,barcode,barcode,barcode,name,brand,brand})){
   if(!c.moveToFirst())return "Sem compra anterior compatível. Confira nome, marca e unidade.";
   BigDecimal old=BigDecimal.valueOf(c.getLong(2)).subtract(BigDecimal.valueOf(c.getLong(3)).divide(BigDecimal.valueOf(c.getLong(1)),6,RoundingMode.HALF_UP));
   NumberFormat f=NumberFormat.getCurrencyInstance(new Locale("pt","BR"));String base="Última compra: "+f.format(old.movePointLeft(2))+" / "+unit+"\n"+c.getString(5)+" • "+new SimpleDateFormat("dd/MM/yyyy",new Locale("pt","BR")).format(new Date(c.getLong(6)))+" • "+c.getString(4);
   if(current<0)return base+"\nInforme o preço atual para comparar.";
   BigDecimal delta=BigDecimal.valueOf(current).subtract(old);return base+"\n"+(delta.signum()==0?"Mesmo preço":f.format(delta.abs().movePointLeft(2))+(delta.signum()>0?" mais caro":" mais barato")+" por "+unit)+". Histórico local, após descontos da compra anterior.";
  }
 }
}
