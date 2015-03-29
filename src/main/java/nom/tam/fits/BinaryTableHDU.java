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

import static nom.tam.fits.header.Standard.NAXISn;
import static nom.tam.fits.header.Standard.PCOUNT;
import static nom.tam.fits.header.Standard.TDIMn;
import static nom.tam.fits.header.Standard.TDISPn;
import static nom.tam.fits.header.Standard.TFIELDS;
import static nom.tam.fits.header.Standard.TFORMn;
import static nom.tam.fits.header.Standard.THEAP;
import static nom.tam.fits.header.Standard.TNULLn;
import static nom.tam.fits.header.Standard.TSCALn;
import static nom.tam.fits.header.Standard.TTYPEn;
import static nom.tam.fits.header.Standard.TUNITn;
import static nom.tam.fits.header.Standard.TZEROn;
import static nom.tam.fits.header.Standard.XTENSION;
import nom.tam.fits.header.IFitsHeader;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.ArrayFuncs;

/** FITS binary table header/data unit */
public class BinaryTableHDU extends TableHDU {

    private BinaryTable table;

    /** The standard column keywords for a binary table. */
    private IFitsHeader[] keyStems = {
        TTYPEn,
        TFORMn,
        TUNITn,
        TNULLn,
        TSCALn,
        TZEROn,
        TDISPn,
        TDIMn
    };

    public BinaryTableHDU(Header hdr, Data datum) {

        super((TableData) datum);
        myHeader = hdr;
        myData = datum;
        table = (BinaryTable) datum;

    }

    /**
     * Create data from a binary table header.
     * 
     * @param header
     *            the template specifying the binary table.
     * @exception FitsException
     *                if there was a problem with the header.
     */
    public static Data manufactureData(Header header) throws FitsException {
        return new BinaryTable(header);
    }

    @Override
    public Data manufactureData() throws FitsException {
        return manufactureData(myHeader);
    }

    /**
     * Build a binary table HDU from the supplied data.
     * 
     * @param data
     *            the data used to build the binary table. This is typically
     *            some kind of array of objects.
     * @exception FitsException
     *                if there was a problem with the data.
     */
    public static Header manufactureHeader(Data data) throws FitsException {
        Header hdr = new Header();
        data.fillHeader(hdr);
        return hdr;
    }

    /** Encapsulate data in a BinaryTable data type */
    public static Data encapsulate(Object o) throws FitsException {

        if (o instanceof nom.tam.util.ColumnTable) {
            return new BinaryTable((nom.tam.util.ColumnTable) o);
        } else if (o instanceof Object[][]) {
            return new BinaryTable((Object[][]) o);
        } else if (o instanceof Object[]) {
            return new BinaryTable((Object[]) o);
        } else {
            throw new FitsException("Unable to encapsulate object of type:" + o.getClass().getName() + " as BinaryTable");
        }
    }

    /**
     * Check that this is a valid binary table header.
     * 
     * @param header
     *            to validate.
     * @return <CODE>true</CODE> if this is a binary table header.
     */
    public static boolean isHeader(Header header) {
        String xten = header.getStringValue(XTENSION);
        if (xten == null) {
            return false;
        }
        xten = xten.trim();
        if (xten.equals("BINTABLE") || xten.equals("A3DTABLE")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check that this HDU has a valid header.
     * 
     * @return <CODE>true</CODE> if this HDU has a valid header.
     */
    public boolean isHeader() {
        return isHeader(myHeader);
    }

    /*
     * Check if this data object is consistent with a binary table. There are
     * three options: a column table object, an Object[][], or an Object[]. This
     * routine doesn't check that the dimensions of arrays are properly
     * consistent.
     */
    public static boolean isData(Object o) {

        if (o instanceof nom.tam.util.ColumnTable || o instanceof Object[][] || o instanceof Object[]) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add a column without any associated header information.
     * 
     * @param data
     *            The column data to be added. Data should be an Object[] where
     *            type of all of the constituents is identical. The length of
     *            data should match the other columns. <b> Note:</b> It is valid
     *            for data to be a 2 or higher dimensionality primitive array.
     *            In this case the column index is the first (in Java speak)
     *            index of the array. E.g., if called with int[30][20][10], the
     *            number of rows in the table should be 30 and this column will
     *            have elements which are 2-d integer arrays with TDIM =
     *            (10,20).
     * @exception FitsException
     *                the column could not be added.
     */
    @Override
    public int addColumn(Object data) throws FitsException {

        int col = table.addColumn(data);
        table.pointToColumn(getNCols() - 1, myHeader);
        return col;
    }

    // Need to tell header about the Heap before writing.
    @Override
    public void write(ArrayDataOutput ado) throws FitsException {

        int oldSize = myHeader.getIntValue(PCOUNT);
        if (oldSize != table.getHeapSize()) {
            myHeader.addLine(PCOUNT.card().value(table.getHeapSize()).comment("ntf::binarytablehdu:pcount:1"));
        }

        if (myHeader.getIntValue(PCOUNT) == 0) {
            myHeader.deleteKey(THEAP);
        } else {
            myHeader.getIntValue(TFIELDS);
            int offset = myHeader.getIntValue(NAXISn.n(1)) * myHeader.getIntValue(NAXISn.n(2)) + table.getHeapOffset();
            myHeader.addLine(THEAP.card().value(offset).comment("ntf::binarytablehdu:theap:1"));
        }

        super.write(ado);
    }

    /**
     * Convert a column in the table to complex. Only tables with appropriate
     * types and dimensionalities can be converted. It is legal to call this on
     * a column that is already complex.
     * 
     * @param index
     *            The 0-based index of the column to be converted.
     * @return Whether the column can be converted
     * @throws FitsException
     */
    public boolean setComplexColumn(int index) throws FitsException {
        boolean status = false;
        if (table.setComplexColumn(index)) {

            // No problem with the data. Make sure the header
            // is right.

            int[] dimens = table.getDimens()[index];
            Class<?> base = table.getBases()[index];

            int dim = 1;
            String tdim = "";
            String sep = "";
            // Don't loop over all values.
            // The last is the [2] for the complex data.
            for (int i = 0; i < dimens.length - 1; i += 1) {
                dim *= dimens[i];
                tdim = dimens[i] + sep + tdim;
                sep = ",";
            }
            String suffix = "C"; // For complex
            // Update the TFORMn keyword.
            if (base == double.class) {
                suffix = "M";
            }

            // Worry about variable length columns.
            String prefix = "";
            if (table.isVarCol(index)) {
                prefix = "P";
                dim = 1;
                if (table.isLongVary(index)) {
                    prefix = "Q";
                }
            }

            // Now update the header.
            myHeader.findCard(TFORMn.n(index + 1));
            HeaderCard hc = myHeader.nextCard();
            String oldComment = hc.getComment();
            if (oldComment == null) {
                oldComment = "Column converted to complex";
            }
            myHeader.addLine(TFORMn.n(index + 1).card().value(dim + prefix + suffix).comment(oldComment));
            if (tdim.length() > 0) {
                myHeader.addLine(TDIMn.n(index + 1).card().value("(" + tdim + ")").comment("ntf::binarytablehdu:tdimN:1"));
            } else {
                // Just in case there used to be a TDIM card that's no longer
                // needed.
                myHeader.removeCard(TDIMn.n(index + 1));
            }
            status = true;
        }
        return status;
    }

    private void prtField(String type, IFitsHeader field) {
        String val = myHeader.getStringValue(field);
        if (val != null) {
            System.out.print(type + '=' + val + "; ");
        }
    }

    /**
     * Print out some information about this HDU.
     */
    @Override
    public void info() {

        BinaryTable myData = (BinaryTable) this.myData;

        System.out.println("  Binary Table");
        System.out.println("      Header Information:");

        int nhcol = myHeader.getIntValue(TFIELDS, -1);
        int nrow = myHeader.getIntValue(NAXISn.n(2), -1);
        int rowsize = myHeader.getIntValue(NAXISn.n(1), -1);

        System.out.print("          " + nhcol + " fields");
        System.out.println(", " + nrow + " rows of length " + rowsize);

        for (int i = 1; i <= nhcol; i += 1) {
            System.out.print("           " + i + ":");
            prtField("Name", TTYPEn.n(i));
            prtField("Format", TFORMn.n(i));
            prtField("Dimens", TDIMn.n(i));
            System.out.println("");
        }

        System.out.println("      Data Information:");
        if (myData == null || table.getNRows() == 0 || table.getNCols() == 0) {
            System.out.println("         No data present");
            if (table.getHeapSize() > 0) {
                System.out.println("         Heap size is: " + table.getHeapSize() + " bytes");
            }
        } else {

            System.out.println("          Number of rows=" + table.getNRows());
            System.out.println("          Number of columns=" + table.getNCols());
            if (table.getHeapSize() > 0) {
                System.out.println("          Heap size is: " + table.getHeapSize() + " bytes");
            }
            Object[] cols = table.getFlatColumns();
            for (int i = 0; i < cols.length; i += 1) {
                System.out.println("           " + i + ":" + ArrayFuncs.arrayDescription(cols[i]));
            }
        }
    }

    /**
     * What are the standard column stems for a binary table?
     */
    @Override
    public IFitsHeader[] columnKeyStems() {
        return keyStems;
    }
}
