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

import static nom.tam.fits.header.Standard.BITPIX;
import static nom.tam.fits.header.Standard.GCOUNT;
import static nom.tam.fits.header.Standard.GROUPS;
import static nom.tam.fits.header.Standard.NAXIS;
import static nom.tam.fits.header.Standard.NAXISn;
import static nom.tam.fits.header.Standard.PCOUNT;
import static nom.tam.fits.header.Standard.SIMPLE;
import static nom.tam.fits.header.Standard.XTENSION;
import nom.tam.util.ArrayFuncs;

/**
 * Random groups HDUs. Note that the internal storage of random groups is a
 * Object[ngroup][2] array. The first element of each group is the parameter
 * data from that group. The second element is the data. The parameters should
 * be a one dimensional array of the primitive types byte, short, int, long,
 * float or double. The second element is a n-dimensional array of the same
 * type. When analyzing group data structure only the first group is examined,
 * but for a valid FITS file all groups must have the same structure.
 */
public class RandomGroupsHDU extends BasicHDU {

    Object dataArray;

    /** Create an HDU from the given header and data */
    public RandomGroupsHDU(Header h, Data d) {
        myHeader = h;
        myData = d;
    }

    /**
     * Indicate that a RandomGroupsHDU can come at the beginning of a FITS file.
     */
    @Override
    protected boolean canBePrimary() {
        return true;
    }

    /**
     * Move a RandomGroupsHDU to or from the beginning of a FITS file. Note that
     * the FITS standard only supports Random Groups data at the beginning of
     * the file, but we allow it within Image extensions.
     */
    @Override
    protected void setPrimaryHDU(boolean status) {
        try {
            super.setPrimaryHDU(status);
        } catch (FitsException e) {
            System.err.println("Unreachable catch in RandomGroupsHDU");
        }
        if (status) {
            myHeader.setSimple(true);
        } else {
            myHeader.setXtension("IMAGE");
        }
    }

    /**
     * Make a header point to the given object.
     * 
     * @param odata
     *            The random groups data the header should describe.
     */
    static Header manufactureHeader(Data d) throws FitsException {

        if (d == null) {
            throw new FitsException("Attempt to create null Random Groups data");
        }
        Header h = new Header();
        d.fillHeader(h);
        return h;

    }

    /**
     * Is this a random groups header?
     * 
     * @param hdr
     *            The header to be tested.
     */
    public static boolean isHeader(Header hdr) {

        if (hdr.getBooleanValue(SIMPLE)) {
            return hdr.getBooleanValue(GROUPS);
        }

        String s = hdr.getStringValue(XTENSION);
        if (s.trim().equals("IMAGE")) {
            return hdr.getBooleanValue(GROUPS);
        }

        return false;
    }

    /**
     * Check that this HDU has a valid header.
     * 
     * @return <CODE>true</CODE> if this HDU has a valid header.
     */
    public boolean isHeader() {
        return isHeader(myHeader);
    }

    /**
     * Check if this data is compatible with Random Groups structure. Must be an
     * Object[ngr][2] structure with both elements of each group having the same
     * base type and the first element being a simple primitive array. We do not
     * check anything but the first row.
     */
    public static boolean isData(Object oo) {
        if (oo instanceof Object[][]) {

            Object[][] o = (Object[][]) oo;

            if (o.length > 0) {
                if (o[0].length == 2) {
                    if (ArrayFuncs.getBaseClass(o[0][0]) == ArrayFuncs.getBaseClass(o[0][1])) {
                        String cn = o[0][0].getClass().getName();
                        if (cn.length() == 2 && cn.charAt(1) != 'Z' || cn.charAt(1) != 'C') {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Create a FITS Data object corresponding to this HDU header.
     */
    @Override
    public Data manufactureData() throws FitsException {
        return manufactureData(myHeader);
    }

    /**
     * Create FITS data object corresponding to a given header.
     */
    public static Data manufactureData(Header hdr) throws FitsException {

        int gcount = hdr.getIntValue(GCOUNT, -1);
        int pcount = hdr.getIntValue(PCOUNT, -1);

        if (!hdr.getBooleanValue(GROUPS) || hdr.getIntValue(NAXISn.n(1), -1) != 0 || gcount < 0 || pcount < 0 || hdr.getIntValue(NAXIS) < 2) {
            throw new FitsException("Invalid Random Groups Parameters");
        }

        // Allocate the object.
        Object[][] dataArray;

        if (gcount > 0) {
            dataArray = new Object[gcount][2];
        } else {
            dataArray = new Object[0][];
        }

        Object[] sampleRow = generateSampleRow(hdr);
        for (int i = 0; i < gcount; i += 1) {
            dataArray[i][0] = ((Object[]) nom.tam.util.ArrayFuncs.deepClone(sampleRow))[0];
            dataArray[i][1] = ((Object[]) nom.tam.util.ArrayFuncs.deepClone(sampleRow))[1];
        }
        return new RandomGroupsData(dataArray);

    }

    static Object[] generateSampleRow(Header h) throws FitsException {

        int ndim = h.getIntValue(NAXIS, 0) - 1;
        int[] dims = new int[ndim];

        int bitpix = h.getIntValue(BITPIX, 0);

        Class<?> baseClass;

        switch (bitpix) {
            case 8:
                baseClass = Byte.TYPE;
                break;
            case 16:
                baseClass = Short.TYPE;
                break;
            case 32:
                baseClass = Integer.TYPE;
                break;
            case 64:
                baseClass = Long.TYPE;
                break;
            case -32:
                baseClass = Float.TYPE;
                break;
            case -64:
                baseClass = Double.TYPE;
                break;
            default:
                throw new FitsException("Invalid BITPIX:" + bitpix);
        }

        // Note that we have to invert the order of the axes
        // for the FITS file to get the order in the array we
        // are generating. Also recall that NAXIS1=0, so that
        // we have an 'extra' dimension.

        for (int i = 0; i < ndim; i += 1) {
            long cdim = h.getIntValue(NAXISn.n(i + 2), 0);
            if (cdim < 0) {
                throw new FitsException("Invalid array dimension:" + cdim);
            }
            dims[ndim - i - 1] = (int) cdim;
        }

        Object[] sample = new Object[2];
        sample[0] = ArrayFuncs.newInstance(baseClass, h.getIntValue(PCOUNT));
        sample[1] = ArrayFuncs.newInstance(baseClass, dims);

        return sample;
    }

    public static Data encapsulate(Object o) throws FitsException {
        if (o instanceof Object[][]) {
            return new RandomGroupsData((Object[][]) o);
        } else {
            throw new FitsException("Attempt to encapsulate invalid data in Random Group");
        }
    }

    /**
     * Display structural information about the current HDU.
     */
    @Override
    public void info() {

        System.out.println("Random Groups HDU");
        if (myHeader != null) {
            System.out.println("   HeaderInformation:");
            System.out.println("     Ngroups:" + myHeader.getIntValue(GCOUNT));
            System.out.println("     Npar:   " + myHeader.getIntValue(PCOUNT));
            System.out.println("     BITPIX: " + myHeader.getIntValue(BITPIX));
            System.out.println("     NAXIS:  " + myHeader.getIntValue(NAXIS));
            for (int i = 0; i < myHeader.getIntValue(NAXIS); i += 1) {
                System.out.println("      " + NAXISn.n(i + 1).key() + "= " + myHeader.getIntValue(NAXISn.n(i + 1)));
            }
        } else {
            System.out.println("    No Header Information");
        }

        Object[][] data = null;
        if (myData != null) {
            try {
                data = (Object[][]) myData.getData();
            } catch (FitsException e) {
                data = null;
            }
        }

        if (data == null || data.length < 1 || data[0].length != 2) {
            System.out.println("    Invalid/unreadable data");
        } else {
            System.out.println("    Number of groups:" + data.length);
            System.out.println("    Parameters: " + nom.tam.util.ArrayFuncs.arrayDescription(data[0][0]));
            System.out.println("    Data:" + nom.tam.util.ArrayFuncs.arrayDescription(data[0][1]));
        }
    }
}
