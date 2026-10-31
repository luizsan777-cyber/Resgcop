package com.regsco.compras;
import java.util.ArrayList;
import java.util.regex.*;
/** Conservative monetary candidates; no prices or product names are silently accepted. */
final class LabelParser {
    static ArrayList<String> names(String text){ArrayList<String> out=new ArrayList<>();if(text==null)return out;for(String s:text.split("\\r?\\n")){s=s.trim();if(s.length()<3||s.length()>120||!s.matches(".*\\p{L}{3}.*")||!prices(s).isEmpty()||s.toLowerCase(java.util.Locale.ROOT).matches(".*(varejo|atacado|a partir|mix mateus|cnpj|cpf|preço).*"))continue;if(!out.contains(s))out.add(s);if(out.size()==20)break;}return out;}
    static ArrayList<String> prices(String text){
        ArrayList<String> values=new ArrayList<>();
        Matcher m=Pattern.compile("(?<![\\d.,])(?:R\\$\\s*)?(\\d{1,5}[,.]\\d{2})(?![\\d.,])").matcher(text);
        while(m.find()){String v=m.group(1).replace('.',',');if(!values.contains(v))values.add(v);}
        return values;
    }
}
