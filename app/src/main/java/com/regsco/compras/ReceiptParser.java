package com.regsco.compras;
import java.util.*;
import java.util.regex.*;
import java.math.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
/** Conservative suggestions only. The user reviews every line before import. */
final class ReceiptParser {
 static class Row {String name,unit,quantity,price,discount="0";Row(String n,String q,String u,String p){name=n;quantity=q;unit=u;price=p;}}
 static final Pattern DETAIL=Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(UN|UND|KG|G|L|ML|PC|PCT|CX)\\s*[xX×*]?\\s*(?:R\\$\\s*)?(\\d+[,.]\\d{2})(?:\\s+[xX×=]?\\s*(\\d+[,.]\\d{2}))?");
 static boolean summary(String s){return s.toUpperCase(Locale.ROOT).matches(".*(TOTAL|SUBTOTAL|TROCO|DINHEIRO|CART[ÃA]O|DESCONTO|TRIBUTO|CNPJ|CPF|CHAVE|CONSUMIDOR|EMISS[ÃA]O|VALOR PAGO|QTD\\.? TOTAL).* ".trim());}
 static String clean(String name){return name.trim().replaceFirst("^\\d{1,3}\\s+","").replaceFirst("^\\d{8,14}\\s+","").trim();}
 static List<Row> parse(String text){List<Row> rows=new ArrayList<>();String previous="";
  for(String line:text.split("\\r?\\n")){String s=line.trim();if(s.isEmpty())continue;if(summary(s)){previous="";continue;}Matcher m=DETAIL.matcher(s);if(m.find()){
   String name=clean(s.substring(0,m.start()));if(name.isEmpty())name=clean(previous);
   if(!name.matches(".*\\p{L}{2}.*")){previous=s;continue;}
   BigDecimal q=new BigDecimal(m.group(1).replace(',','.'));if(q.signum()<=0){previous="";continue;}
   String unit=m.group(2).toLowerCase(Locale.ROOT),price=m.group(3),qty;
   try{qty=""+q.intValueExact();}catch(Exception e){if(m.group(4)==null){previous="";continue;}qty="1";unit="lote "+m.group(1)+" "+unit;price=m.group(4);}
   if(qty.equals("0")){previous="";continue;}
   Row row=new Row(name,qty,unit.equals("und")?"un":unit,price);if(m.group(4)!=null){long full=Long.parseLong(qty)*Money.parse(price),net=Money.parse(m.group(4));if(net>=0&&full>=net)row.discount=java.math.BigDecimal.valueOf(full-net,2).toPlainString();}rows.add(row);previous="";
  }else previous=s;
  }return rows;
 }
 static String fingerprint(String text){String compact=text.replaceAll("\\s+","");Matcher m=Pattern.compile("(?<!\\d)\\d{44}(?!\\d)").matcher(compact);String key=m.find()?m.group():text.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+"," ");try{byte[] b=MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte v:b)out.append(String.format("%02x",v&255));return out.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
