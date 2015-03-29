package nom.tam.fits.test;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 1996 - 2015 nom-tam-fits
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

import static nom.tam.fits.header.InstrumentDescription.FILTER;
import static nom.tam.fits.header.Standard.INSTRUME;
import static nom.tam.fits.header.Standard.NAXISn;
import static nom.tam.fits.header.extra.NOAOExt.WATn_nnn;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.header.Checksum;
import nom.tam.fits.header.FitsHeaderIndex;

import org.junit.Assert;
import org.junit.Test;

/**
 * Check out header manipulation.
 */
public class EnumHeaderTest {

    @Test
    public void exampleHeaderEnums() throws Exception {
        Header hdr = createHeader();

        // now some simple keywords
        hdr.addValue(INSTRUME.card().value("My very big telescope"));
        hdr.addValue(FILTER.card().value("meade #25A Red"));

        // and check if the simple keywords reached there destination.
        Assert.assertEquals("My very big telescope", hdr.getStringValue(INSTRUME));
        Assert.assertEquals("meade #25A Red", hdr.getStringValue(FILTER));
    }

    @Test
    public void simpleHeaderIndexes() throws Exception {
        Header hdr = createHeader();

        // ok the header NAXISn has a index, the 'n' in the keyword
        hdr.addValue(NAXISn.n(1).card().value(10));
        hdr.addValue(NAXISn.n(2).card().value(20));

        Assert.assertEquals("NAXIS1", NAXISn.n(1).key());

        // lets check if the right values where set when we ask for the keyword
        // by String
        Assert.assertEquals(10, hdr.getIntValue(NAXISn.n(1)));
        Assert.assertEquals(20, hdr.getIntValue(NAXISn.n(2)));
    }

    @Test
    public void multiyHeaderIndexes() throws Exception {
        Header hdr = createHeader();

        // now we take a header with multiple indexes
        hdr.addValue(WATn_nnn.n(9, 2, 3, 4).card().value("50"));
        Assert.assertEquals("WAT9_234", WATn_nnn.n(9, 2, 3, 4).key());

        // lets check is the keyword was correctly cearted
        Assert.assertEquals("50", hdr.getStringValue(WATn_nnn.n(9, 2, 3, 4)));
    }

    public Header createHeader() throws FitsException {
        byte[][] bimg = new byte[20][20];
        BasicHDU hdu = Fits.makeHDU(bimg);
        Header hdr = hdu.getHeader();
        return hdr;
    }

    @Test
    public void testFitsIndex() throws Exception {
        Assert.assertSame(INSTRUME, FitsHeaderIndex.find("INSTRUME"));
        Assert.assertSame(Checksum.CHECKSUM, FitsHeaderIndex.find("CHECKSUM"));
        Assert.assertSame(INSTRUME,INSTRUME.card().value(1).getKey());
    }
}
