package nom.tam.fits.test;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 2004 - 2015 nom-tam-fits
 * %%
 * This is free and unencumbered software released into the public domain.
 * 
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * #L%
 */

import static nom.tam.fits.header.Standard.COMMENT;
import static nom.tam.fits.header.Standard.SIMPLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.header.FitsHeaderIndex;
import nom.tam.fits.header.IFitsHeader;

import org.junit.Test;

public class HeaderCardTest {

    private static IFitsHeader TTTT = FitsHeaderIndex.findOrCreateKey("TTTT");

    private static IFitsHeader KEY = FitsHeaderIndex.findOrCreateKey("KEY");

    private static IFitsHeader LONGKEYWORD = FitsHeaderIndex.findOrCreateKey("LONGKEYWORD");

    private static IFitsHeader STRING = FitsHeaderIndex.findOrCreateKey("STRING");

    private static IFitsHeader QUOTES = FitsHeaderIndex.findOrCreateKey("QUOTES");

    private static IFitsHeader HIERARCH_TEST1_TEST2_INT = FitsHeaderIndex.findOrCreateKey("HIERARCH.TEST1.TEST2.INT");

    private static IFitsHeader TEST = FitsHeaderIndex.findOrCreateKey("TEST");

    @Test
    public void test1() throws Exception {

        HeaderCard p;
        p = new HeaderCard("SIMPLE  =                     T");

        assertEquals("t1", SIMPLE, p.getKey());
        assertEquals("t2", "T", p.getValue());
        assertNull("t3", p.getComment());

        p = new HeaderCard("VALUE   =                   123");
        assertEquals("t4", "VALUE", p.getKey().key());
        assertEquals("t5", "123", p.getValue());
        assertNull("t3", p.getComment());

        p = new HeaderCard("VALUE   =    1.23698789798798E23 / Comment ");
        assertEquals("t6", "VALUE", p.getKey().key());
        assertEquals("t7", "1.23698789798798E23", p.getValue());
        assertEquals("t8", "Comment", p.getComment());

        String lng = "111111111111111111111111111111111111111111111111111111111111111111111111";
        p = new HeaderCard("COMMENT " + lng);
        assertEquals("t9", COMMENT, p.getKey());
        assertNull("t10", p.getValue());
        assertEquals("t11", lng, p.getComment());

        boolean thrown = false;
        try {
            //
            p = new HeaderCard("VALUE   = '   ");
        } catch (Exception e) {
            thrown = true;
        }
        assertEquals("t12", true, thrown);

        p = new HeaderCard("COMMENT " + lng + lng);
        assertEquals("t13", lng, p.getComment());

        HeaderCard z = TTTT.card().value(1.234567891234567891234567e101).comment("a comment");
        assertTrue("t14", z.toString().indexOf("E") > 0);
    }

    @Test
    public void test3() throws Exception {

        HeaderCard p = KEY.card().value("VALUE").comment("COMMENT");
        assertEquals("x1", "KEY     = 'VALUE   '           / COMMENT                                        ", p.toString());

        p = KEY.card().value(123).comment("COMMENT");
        assertEquals("x2", "KEY     =                  123 / COMMENT                                        ", p.toString());
        p = KEY.card().value(1.23).comment("COMMENT");
        assertEquals("x3", "KEY     =                 1.23 / COMMENT                                        ", p.toString());
        p = KEY.card().value(true).comment("COMMENT");
        assertEquals("x4", "KEY     =                    T / COMMENT                                        ", p.toString());

        boolean thrown = false;
        try {
            p = LONGKEYWORD.card().value(123).comment("COMMENT");
        } catch (Exception e) {
            thrown = true;
        }
        assertEquals("x5", true, thrown);

        thrown = false;
        String lng = "00000000001111111111222222222233333333334444444444555555555566666666667777777777";
        try {
            p = KEY.card().value(lng).comment("COMMENT");
        } catch (Exception e) {
            thrown = true;
        }
        assertEquals("x6", true, thrown);

        // Only trailing spaces are stripped.
        p = STRING.card().value("VALUE");
        assertEquals("x6", "VALUE", p.getValue());

        p = STRING.card().value("VALUE ");
        assertEquals("x7", "VALUE", p.getValue());

        p = STRING.card().value(" VALUE");
        assertEquals("x8", " VALUE", p.getValue());

        p = STRING.card().value(" VALUE ");
        assertEquals("x9", " VALUE", p.getValue());

        p = QUOTES.card().value("ABC'DEF");
        assertEquals("x10", "ABC'DEF", p.getValue());
        assertEquals("x10b", p.toString().indexOf("''") > 0, true);

        p = QUOTES.card().value("ABC''DEF");
        assertEquals("x11", "ABC''DEF", p.getValue());
        assertEquals("x10b", p.toString().indexOf("''''") > 0, true);
    }

    @Test
    public void testHierarch() throws Exception {

        HeaderCard hc;
        boolean thrown = false;
        try {
            hc = HIERARCH_TEST1_TEST2_INT.card().value(123).comment("Comment");
        } catch (Exception e) {
            thrown = true;
        }
        assertEquals("h1", true, thrown);

        String card = "HIERARCH TEST1 TEST2 INT=           123 / Comment                               ";
        hc = new HeaderCard(card);
        assertEquals("h2", "HIERARCH", hc.getKey().key());
        assertNull("h3", hc.getValue());
        assertEquals("h4", "TEST1 TEST2 INT=           123 / Comment", hc.getComment());

        FitsFactory.setUseHierarch(true);

        hc = HIERARCH_TEST1_TEST2_INT.card().value(123).comment("Comment");

        assertEquals("h5", HIERARCH_TEST1_TEST2_INT.key(), hc.getKey().key());
        assertEquals("h6", "123", hc.getValue());
        assertEquals("h7", "Comment", hc.getComment());

        hc = new HeaderCard(card);
        assertEquals("h8", HIERARCH_TEST1_TEST2_INT.key(), hc.getKey().key());
        assertEquals("h9", "123", hc.getValue());
        assertEquals("h10", "Comment", hc.getComment());
    }

    @Test
    public void testLongDoubles() throws Exception {
        // Check to see if we make long double values
        // fit in the recommended space.
        HeaderCard hc = TEST.card().value(-1.234567890123456789e-123).comment("dummy");
        String val = hc.getValue();
        assertEquals("tld1", val.length(), 20);
        assertEquals(Double.class, hc.valueType());
    }

    @Test
    public void testLong() throws Exception {
        // Check to see if we make long double values
        // fit in the recommended space.
        HeaderCard hc = TEST.card().value(999999999999999999L).comment("dummy");
        assertEquals(Long.class, hc.valueType());
    }

    @Test
    public void testInt() throws Exception {
        // Check to see if we make long double values
        // fit in the recommended space.
        HeaderCard hc = TEST.card().value(9999).comment("dummy");
        assertEquals(Integer.class, hc.valueType());
    }

    @Test
    public void testBoolean() throws Exception {
        // Check to see if we make long double values
        // fit in the recommended space.
        HeaderCard hc = TEST.card().value(true).comment("dummy");
        assertEquals(Boolean.class, hc.valueType());
        hc = TEST.card().value(false).comment("dummy");
        assertEquals(Boolean.class, hc.valueType());
    }

    @Test
    public void testString() throws Exception {
        // Check to see if we make long double values
        // fit in the recommended space.
        HeaderCard hc = TEST.card().value("bla bla").comment("dummy");
        assertEquals(String.class, hc.valueType());
    }
}
