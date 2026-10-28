package com.regsco.compras;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Financial parsing uses decimal input and integer cents throughout. */
public final class Money {
    private Money() {}
    public static long parse(String value) {
        try {
            String v=value.trim().replace("R$", "").replaceAll("[\\s\\u00a0\\u202f]", "");
            if(v.contains(",")) v=v.replace(".", "").replace(',', '.');
            if(!v.matches("[0-9]+(?:\\.[0-9]{1,2})?")) return -1;
            return new BigDecimal(v).movePointRight(2).setScale(0,RoundingMode.UNNECESSARY).longValueExact();
        } catch(Exception e) { return -1; }
    }
    public static boolean validLine(long quantity,long cents) {
        if(quantity<=0||cents<0)return false;
        try {Math.multiplyExact(quantity,cents);return true;}catch(ArithmeticException e){return false;}
    }
    public static long price(long quantity,long retail,long wholesale,long minimum) {
        if(quantity<=0||retail<0||wholesale<0||minimum<=0)throw new IllegalArgumentException("Condição inválida");
        return Math.multiplyExact(quantity,quantity>=minimum?wholesale:retail);
    }
}
