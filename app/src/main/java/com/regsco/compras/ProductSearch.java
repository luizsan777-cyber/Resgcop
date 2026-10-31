package com.regsco.compras;

import java.text.Normalizer;
import java.util.*;

/** Name-based alternatives. Never treats a barcode as a search term or a brand as a category. */
final class ProductSearch {
    static String normal(String s) { return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim(); }
    static final String[] TYPES={"extrato de tomate","farinha de mandioca","farinha de trigo","pimenta do reino","milho para pipoca","polpa","arroz","macarrao","espaguete","oleo","acucar","feijao","cafe","leite","flocao","cuscuz","pipoca","sardinha","sal","pimenta","jarra"};
    static final String[] DETAILS={"acerola","maracuja","manga","goiaba","caju","graviola","caja","cupuacu","acai","morango","uva","abacaxi","coco","tamarindo","umbu","bacuri","integral","desnatado","semidesnatado","branco","parboilizado","carioca","preto","soja","girassol","milho","azeite","refinado","cristal","demerara","mascavo","instantaneo","espaguete","parafuso","penne","sem lactose","zero lactose","sem acucar","sem gluten"};
    static boolean word(String text,String term){return (" "+normal(text)+" ").contains(" "+normal(term)+" ");}
    static String type(String text){String n=normal(text).replace("extrato tomate","extrato de tomate").replace("farinha mandioca","farinha de mandioca").replace("farinha trigo","farinha de trigo");for(String t:TYPES)if(word(n,t))return t;return "";}
    static String alternatives(String name){String t=type(name);if(t.isEmpty())return name.replaceAll("\\b\\d{8,14}\\b","").trim();LinkedHashSet<String> terms=new LinkedHashSet<>();terms.add(t);for(String d:DETAILS)if(word(name,d)&&!word(t,d))terms.add(d);return String.join(" ",terms);}
    static String fromOcr(String raw){ArrayList<String> lines=LabelParser.names(raw);String joined=String.join(" ",lines);String t=type(joined);if(!t.isEmpty())return alternatives(joined);return lines.isEmpty()?"":lines.get(0);}
    static boolean valid(String name){return name!=null&&name.length()<=120&&name.matches(".*\\p{L}{3}.*");}
    static boolean matches(String term,String name){String t=type(term);if(!t.isEmpty()&&!word(name,t))return false;for(String d:DETAILS)if(word(term,d)&&!word(name,d))return false;return true;}
}
