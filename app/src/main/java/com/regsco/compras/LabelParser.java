package com.regsco.compras;
import java.util.ArrayList;
import java.util.regex.*;
/** Conservative monetary candidates; no prices or product names are silently accepted. */
final class LabelParser {
    static ArrayList<String> prices(String text){
        ArrayList<String> values=new ArrayList<>();
        Matcher m=Pattern.compile("(?<![\\d.,])(?:R\\$\\s*)?(\\d{1,5}[,.]\\d{2})(?![\\d.,])").matcher(text);
        while(m.find()){String v=m.group(1).replace('.',',');if(!values.contains(v))values.add(v);}
        return values;
    }
}
