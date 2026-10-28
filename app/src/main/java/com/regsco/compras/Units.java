package com.regsco.compras;
import java.util.Locale;
final class Units {
    static String canonical(String unit){String u=unit.trim().toLowerCase(Locale.ROOT);if(u.equals("kg")||u.equals("g"))return "g";if(u.equals("l")||u.equals("ml"))return "mL";return u;}
    static long base(long amount,String unit){String u=unit.trim().toLowerCase(Locale.ROOT);return u.equals("kg")||u.equals("l")?Math.multiplyExact(amount,1000):amount;}
    static boolean compatible(String a,String b){return canonical(a).equals(canonical(b));}
}
