package nom.tam.fits;

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

import nom.tam.image.StandardImageTiler;
import nom.tam.util.ArrayFuncs;
import static nom.tam.fits.header.Standard.*;
/** FITS image header/data unit */
public class ImageHDU extends BasicHDU {

    /**
     * Build an image HDU using the supplied data.
     * 
     * @param h
     *            the header for the image.
     * @param d
     *            the data used in the image.
     * @exception FitsException
     *                if there was a problem with the data.
     */
    public ImageHDU(Header h, Data d) throws FitsException {
        myData = d;
        myHeader = h;

    }

    /** Indicate that Images can appear at the beginning of a FITS dataset */
    @Override
    protected boolean canBePrimary() {
        return true;
    }

    /** Change the Image from/to primary */
    @Override
    protected void setPrimaryHDU(boolean status) {

        try {
            super.setPrimaryHDU(status);
        } catch (FitsException e) {
            System.err.println("Impossible exception in ImageData");
        }

        if (status) {
            myHeader.setSimple(true);
        } else {
            myHeader.setXtension("IMAGE");
        }
    }

    /**
     * Check that this HDU has a valid header for this type.
     * 
     * @return <CODE>true</CODE> if this HDU has a valid header.
     */
    public static boolean isHeader(Header hdr) {
        boolean found = false;
        found = hdr.getBooleanValue(SIMPLE);
        if (!found) {
            String s = hdr.getStringValue(XTENSION);
            if (s != null) {
                if (s.trim().equals("IMAGE") || s.trim().equals("IUEIMAGE")) {
                    found = true;
                }
            }
        }
        if (!found) {
            return false;
        }
        return !hdr.getBooleanValue(GROUPS);
    }

    /**
     * Check if this object can be described as a FITS image.
     * 
     * @param o
     *            The Object being tested.
     */
    public static boolean isData(Object o) {
        String s = o.getClass().getName();

        int i;
        for (i = 0; i < s.length(); i += 1) {
            if (s.charAt(i) != '[') {
                break;
            }
        }

        // Allow all non-boolean/Object arrays.
        // This does not check the rectangularity of the array though.
        if (i <= 0 || s.charAt(i) == 'L' || s.charAt(i) == 'Z') {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Create a Data object to correspond to the header description.
     * 
     * @return An unfilled Data object which can be used to read in the data for
     *         this HDU.
     * @exception FitsException
     *                if the image extension could not be created.
     */
    @Override
    public Data manufactureData() throws FitsException {
        return manufactureData(myHeader);
    }

    public static Data manufactureData(Header hdr) throws FitsException {
        return new ImageData(hdr);
    }

    /**
     * Create a header that describes the given image data.
     * 
     * @param d
     *            The image to be described.
     * @exception FitsException
     *                if the object does not contain valid image data.
     */
    public static Header manufactureHeader(Data d) throws FitsException {

        if (d == null) {
            return null;
        }

        Header h = new Header();
        d.fillHeader(h);

        return h;
    }

    /** Encapsulate an object as an ImageHDU. */
    public static Data encapsulate(Object o) throws FitsException {
        return new ImageData(o);
    }

    public StandardImageTiler getTiler() {
        return ((ImageData) myData).getTiler();
    }

    /**
     * Print out some information about this HDU.
     */
    @Override
    public void info() {
        if (isHeader(myHeader)) {
            System.out.println("  Image");
        } else {
            System.out.println("  Image (bad header)");
        }

        System.out.println("      Header Information:");
        System.out.println("         BITPIX=" + myHeader.getIntValue(BITPIX, -1));
        int naxis = myHeader.getIntValue(NAXIS, -1);
        System.out.println("         NAXIS=" + naxis);
        for (int i = 1; i <= naxis; i += 1) {
            System.out.println("         " + NAXISn.n(i).key() + "=" + myHeader.getIntValue(NAXISn.n(i), -1));
        }

        System.out.println("      Data information:");
        try {
            if (myData.getData() == null) {
                System.out.println("        No Data");
            } else {
                System.out.println("         " + ArrayFuncs.arrayDescription(myData.getData()));
            }
        } catch (Exception e) {
            System.out.println("      Unable to get data");
        }
    }
}
