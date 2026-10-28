package com.regsco.compras;
import org.junit.Test;
import static org.junit.Assert.*;
public class MoneyTest {
    @Test public void brazilianAmounts(){assertEquals(135793,Money.parse("R$\u00a01.357,93"));assertEquals(10,Money.parse("0,10"));assertEquals(850,Money.parse("8.50"));assertEquals(0,Money.parse("0"));}
    @Test public void invalidAmounts(){for(String s:new String[]{"-1","NaN","1e3","1,234","", "9223372036854775808"})assertEquals(s,-1,Money.parse(s));}
    @Test public void wholesaleThreshold(){assertEquals(2000,Money.price(2,1000,850,3));assertEquals(2550,Money.price(3,1000,850,3));}
    @Test public void overflowRejected(){assertFalse(Money.validLine(3,Long.MAX_VALUE));assertFalse(Money.validLine(0,100));assertTrue(Money.validLine(3,850));}
    @Test public void labelCandidates(){assertEquals(java.util.Arrays.asList("14,99","13,49"),LabelParser.prices("CAFÉ 250G\nVAREJO R$ 14,99\nATACADO 13,49\nA PARTIR DE 3\nEAN 7891234567890"));assertTrue(LabelParser.prices("COD 7891234567890").isEmpty());}
}
