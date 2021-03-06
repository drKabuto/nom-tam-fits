package nom.tam.util.test;

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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

/** This class tests the ByteFormatter and ByteParser classes.
 */
import nom.tam.util.ByteFormatter;
import nom.tam.util.ByteParser;
import nom.tam.util.TruncationException;

import org.junit.Test;

public class ByteFormatParseTest {

    byte[] buffer = new byte[100000];

    ByteFormatter bf = new ByteFormatter();

    ByteParser bp = new ByteParser(this.buffer);

    int offset = 0;

    int cnt = 0;

    @Test
    public void testBoolean() throws Exception {

        boolean[] btst = new boolean[100];
        for (int i = 0; i < btst.length; i += 1) {
            btst[i] = Math.random() > 0.5;
        }
        this.offset = 0;
        for (boolean element : btst) {
            this.offset = this.bf.format(element, this.buffer, this.offset, 1);
            this.offset = this.bf.format(" ", this.buffer, this.offset, 1);
        }

        this.bp.setOffset(0);
        for (int i = 0; i < btst.length; i += 1) {
            assertEquals("Boolean:" + i, btst[i], this.bp.getBoolean());
        }
    }

    @Test
    public void testDouble() throws Exception {

        for (int i = 0; i < 10; i += 1) {
            this.buffer[i] = (byte) ' ';
        }
        this.bp.setOffset(0);
        assertEquals("DoubBlank", 0., this.bp.getDouble(10), 0.);

        double[] dbl = new double[100];
        for (int i = 6; i < dbl.length; i += 1) {
            dbl[i] = 2 * (Math.random() - 0.5) * Math.pow(10, 60 * (Math.random() - 0.5));
        }

        dbl[0] = Double.MAX_VALUE;
        dbl[1] = Double.MIN_VALUE;
        dbl[2] = 0;
        dbl[3] = Double.NaN;
        dbl[4] = Double.POSITIVE_INFINITY;
        dbl[5] = Double.NEGATIVE_INFINITY;

        this.bf.setTruncationThrow(false);
        this.bf.setAlign(true);
        this.offset = 0;
        this.cnt = 0;
        while (this.cnt < dbl.length) {
            this.offset = this.bf.format(dbl[this.cnt], this.buffer, this.offset, 25);
            this.cnt += 1;
            if (this.cnt % 4 == 0) {
                this.offset = this.bf.format("\n", this.buffer, this.offset, 1);
            }
        }

        this.bp.setOffset(0);
        for (int i = 0; i < dbl.length; i += 1) {

            double chk = this.bp.getDouble(25);

            double dx = Math.abs(chk - dbl[i]);
            if (dbl[i] != 0) {
                dx = dx / Math.abs(dbl[i]);
            }
            if (Double.isNaN(dbl[i])) {
                assertEquals("Double check:" + i, true, Double.isNaN(chk));
            } else if (Double.isInfinite(dbl[i])) {
                assertEquals("Double check:" + i, dbl[i], chk, 0);
            } else {
                assertEquals("Double check:" + i, 0., dx, 1.e-14);
            }

            if ((i + 1) % 4 == 0) {
                this.bp.skip(1);
            }
        }
    }

    @Test
    public void testFloat() throws Exception {

        for (int i = 0; i < 10; i += 1) {
            this.buffer[i] = (byte) ' ';
        }
        this.bp.setOffset(0);
        assertEquals("FloatBlank", 0.f, this.bp.getFloat(10), 0.);

        float[] flt = new float[100];
        for (int i = 6; i < flt.length; i += 1) {
            flt[i] = (float) (2 * (Math.random() - 0.5) * Math.pow(10, 60 * (Math.random() - 0.5)));
        }

        flt[0] = Float.MAX_VALUE;
        flt[1] = Float.MIN_VALUE;
        flt[2] = 0;
        flt[3] = Float.NaN;
        flt[4] = Float.POSITIVE_INFINITY;
        flt[5] = Float.NEGATIVE_INFINITY;

        this.bf.setTruncationThrow(false);
        this.bf.setAlign(true);

        this.offset = 0;
        this.cnt = 0;

        while (this.cnt < flt.length) {
            this.offset = this.bf.format(flt[this.cnt], this.buffer, this.offset, 24);
            this.cnt += 1;
            if (this.cnt % 4 == 0) {
                this.offset = this.bf.format("\n", this.buffer, this.offset, 1);
            }
        }

        this.bp.setOffset(0);

        for (int i = 0; i < flt.length; i += 1) {

            float chk = this.bp.getFloat(24);

            float dx = Math.abs(chk - flt[i]);
            if (flt[i] != 0) {
                dx = dx / Math.abs(flt[i]);
            }
            if (Float.isNaN(flt[i])) {
                assertEquals("Float check:" + i, true, Float.isNaN(chk));
            } else if (Float.isInfinite(flt[i])) {
                assertEquals("Float check:" + i, flt[i], chk, 0);
            } else {
                assertEquals("Float check:" + i, 0., dx, 1.e-6);
            }
            if ((i + 1) % 4 == 0) {
                this.bp.skip(1);
            }
        }
    }

    @Test
    public void testInt() throws Exception {

        for (int i = 0; i < 10; i += 1) {
            this.buffer[i] = (byte) ' ';
        }
        this.bp.setOffset(0);
        assertEquals("IntBlank", 0, this.bp.getInt(10));

        this.bf.setAlign(true);
        this.bf.setTruncationThrow(false);

        int[] tint = new int[100];

        tint[0] = Integer.MIN_VALUE;
        tint[1] = Integer.MAX_VALUE;
        tint[2] = 0;

        for (int i = 0; i < tint.length; i += 1) {
            tint[i] = (int) (Integer.MAX_VALUE * (2 * (Math.random() - .5)));
        }

        // Write 100 numbers
        int colSize = 12;
        while (this.cnt < tint.length) {
            this.offset = this.bf.format(tint[this.cnt], this.buffer, this.offset, colSize);
            this.cnt += 1;
            if (this.cnt % 8 == 0) {
                this.offset = this.bf.format("\n", this.buffer, this.offset, 1);
            }
        }

        // Now see if we can get them back
        this.bp.setOffset(0);
        for (int i = 0; i < tint.length; i += 1) {

            int chk = this.bp.getInt(colSize);

            assertEquals("IntegersRA", chk, tint[i]);
            if ((i + 1) % 8 == 0) {
                this.bp.skip(1);
            }
        }

        // Now do it with left-aligned numbers.
        this.bf.setAlign(false);
        this.bp.setFillFields(true);
        this.offset = 0;
        colSize = 12;
        this.cnt = 0;
        this.offset = 0;
        while (this.cnt < tint.length) {
            int oldOffset = this.offset;
            this.offset = this.bf.format(tint[this.cnt], this.buffer, this.offset, colSize);
            int nb = colSize - (this.offset - oldOffset);
            if (nb > 0) {
                this.offset = this.bf.alignFill(this.buffer, this.offset, nb);
            }
            this.cnt += 1;
            if (this.cnt % 8 == 0) {
                this.offset = this.bf.format("\n", this.buffer, this.offset, 1);
            }
        }

        // Now see if we can get them back
        this.bp.setOffset(0);
        for (int i = 0; i < tint.length; i += 1) {

            int chk = this.bp.getInt(colSize);

            assertEquals("IntegersLA", chk, tint[i]);
            if ((i + 1) % 8 == 0) {
                this.bp.skip(1);
            }
        }

        this.offset = 0;
        colSize = 12;
        this.cnt = 0;
        this.offset = 0;
        while (this.cnt < tint.length) {
            this.offset = this.bf.format(tint[this.cnt], this.buffer, this.offset, colSize);
            this.cnt += 1;
            if (this.cnt % 8 == 0) {
                this.offset = this.bf.format("\n", this.buffer, this.offset, 1);
            }
        }

        String myStr = new String(this.buffer, 0, this.offset);
        assertEquals("No spaces", -1, myStr.indexOf(" "));

        this.bf.setAlign(false);

        this.offset = 0;
        colSize = 12;
        this.cnt = 0;
        this.offset = 0;
        while (this.cnt < tint.length) {
            this.offset = this.bf.format(tint[this.cnt], this.buffer, this.offset, colSize);
            this.offset = this.bf.format(" ", this.buffer, this.offset, 1);
            this.cnt += 1;
        }
        myStr = new String(this.buffer, 0, this.offset);
        String[] array = myStr.split(" ");

        assertEquals("Split size", 100, array.length);

        for (int i = 0; i < array.length; i += 1) {
            assertEquals("Parse token", tint[i], Integer.parseInt(array[i]));
        }

        this.bf.setTruncationThrow(false);

        int val = 1;
        Arrays.fill(this.buffer, (byte) ' ');

        for (int i = 0; i < 10; i += 1) {
            this.offset = this.bf.format(val, this.buffer, 0, 6);
            String test = (val + "      ").substring(0, 6);
            if (i < 6) {
                assertEquals("TestTrunc" + i, test, new String(this.buffer, 0, 6));
            } else {
                assertEquals("TestTrunc" + i, "******", new String(this.buffer, 0, 6));
            }
            val *= 10;
        }

        this.bf.setTruncationThrow(true);
        val = 1;
        for (int i = 0; i < 10; i += 1) {
            boolean thrown = false;
            try {
                this.offset = this.bf.format(val, this.buffer, 0, 6);
            } catch (TruncationException e) {
                thrown = true;
            }
            if (i < 6) {
                assertEquals("TestTruncThrow" + i, false, thrown);
            } else {
                assertEquals("TestTruncThrow" + i, true, thrown);
            }
            val *= 10;
        }
    }

    @Test
    public void testLong() throws Exception {

        for (int i = 0; i < 10; i += 1) {
            this.buffer[i] = (byte) ' ';
        }
        this.bp.setOffset(0);
        assertEquals("LongBlank", 0L, this.bp.getLong(10));

        long[] lng = new long[100];
        for (int i = 0; i < lng.length; i += 1) {
            lng[i] = (long) (Long.MAX_VALUE * (2 * (Math.random() - 0.5)));
        }

        lng[0] = Long.MAX_VALUE;
        lng[1] = Long.MIN_VALUE;
        lng[2] = 0;

        this.bf.setTruncationThrow(false);
        this.bp.setFillFields(true);
        this.bf.setAlign(true);
        this.offset = 0;
        for (int i = 0; i < lng.length; i += 1) {
            this.offset = this.bf.format(lng[i], this.buffer, this.offset, 20);
            if ((i + 1) % 4 == 0) {
                this.offset = this.bf.format("\n", this.buffer, this.offset, 1);
            }
        }

        this.bp.setOffset(0);

        for (int i = 0; i < lng.length; i += 1) {
            assertEquals("Long check", lng[i], this.bp.getLong(20));
            if ((i + 1) % 4 == 0) {
                this.bp.skip(1);
            }
        }
    }

    @Test
    public void testString() throws Exception {

        this.offset = 0;
        String bigStr = "abcdefghijklmnopqrstuvwxyz";

        for (int i = 0; i < 100; i += 1) {
            this.offset = this.bf.format(bigStr.substring(i % 27), this.buffer, this.offset, 13);
            this.offset = this.bf.format(" ", this.buffer, this.offset, 1);
        }

        this.bp.setOffset(0);
        for (int i = 0; i < 100; i += 1) {
            int ind = i % 27;
            if (ind > 13) {
                ind = 13;
            }
            String want = bigStr.substring(i % 27);
            if (want.length() > 13) {
                want = want.substring(0, 13);
            }
            String s = this.bp.getString(want.length());
            assertEquals("String:" + i, want, s);
            this.bp.skip(1);
        }
    }
}
