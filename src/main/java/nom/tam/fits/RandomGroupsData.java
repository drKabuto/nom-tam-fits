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

import java.io.EOFException;
import java.io.IOException;

import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.ArrayFuncs;

/**
 * This class instantiates FITS Random Groups data. Random groups are
 * instantiated as a two-dimensional array of objects. The first dimension of
 * the array is the number of groups. The second dimension is 2. The first
 * object in every row is a one dimensional parameter array. The second element
 * is the n-dimensional data array.
 */
public class RandomGroupsData extends Data {

    private final Object[][] dataArray;

    /**
     * Create the equivalent of a null data element.
     */
    public RandomGroupsData() {
        this.dataArray = new Object[0][];
    }

    /**
     * Create a RandomGroupsData object using the specified object to initialize
     * the data array.
     * 
     * @param x
     *            The initial data array. This should a two-d array of objects
     *            as described above.
     */
    public RandomGroupsData(Object[][] x) {
        this.dataArray = x;
    }

    @Override
    protected void fillHeader(Header h) throws FitsException {

        if (this.dataArray.length <= 0 || this.dataArray[0].length != 2) {
            throw new FitsException("Data not conformable to Random Groups");
        }

        Object paraSamp = this.dataArray[0][0];
        Object dataSamp = this.dataArray[0][1];

        Class pbase = nom.tam.util.ArrayFuncs.getBaseClass(paraSamp);
        Class dbase = nom.tam.util.ArrayFuncs.getBaseClass(dataSamp);

        if (pbase != dbase) {
            throw new FitsException("Data and parameters do not agree in type for random group");
        }

        int[] pdims = nom.tam.util.ArrayFuncs.getDimensions(paraSamp);
        int[] ddims = nom.tam.util.ArrayFuncs.getDimensions(dataSamp);

        if (pdims.length != 1) {
            throw new FitsException("Parameters are not 1 d array for random groups");
        }

        // Got the information we need to build the header.

        h.setSimple(true);
        if (dbase == byte.class) {
            h.setBitpix(8);
        } else if (dbase == short.class) {
            h.setBitpix(16);
        } else if (dbase == int.class) {
            h.setBitpix(32);
        } else if (dbase == long.class) { // Non-standard
            h.setBitpix(64);
        } else if (dbase == float.class) {
            h.setBitpix(-32);
        } else if (dbase == double.class) {
            h.setBitpix(-64);
        } else {
            throw new FitsException("Data type:" + dbase + " not supported for random groups");
        }

        h.setNaxes(ddims.length + 1);
        h.addValue("NAXIS1", 0, "ntf::randomgroupsdata:naxis1:1");
        for (int i = 2; i <= ddims.length + 1; i += 1) {
            h.addValue("NAXIS" + i, ddims[i - 2], "ntf::randomgroupsdata:naxisN:1");
        }

        h.addValue("GROUPS", true, "ntf::randomgroupsdata:groups:1");
        h.addValue("GCOUNT", this.dataArray.length, "ntf::randomgroupsdata:gcount:1");
        h.addValue("PCOUNT", pdims[0], "ntf::randomgroupsdata:pcount:1");
    }

    @Override
    public Object getData() {
        return this.dataArray;
    }

    /** Get the size of the actual data element. */
    @Override
    protected long getTrueSize() {

        if (this.dataArray != null && this.dataArray.length > 0) {
            return (ArrayFuncs.computeLSize(this.dataArray[0][0]) + ArrayFuncs.computeLSize(this.dataArray[0][1])) * this.dataArray.length;
        } else {
            return 0;
        }
    }

    /** Read the RandomGroupsData */
    @Override
    public void read(ArrayDataInput str) throws FitsException {

        setFileOffset(str);

        try {
            str.readLArray(this.dataArray);
        } catch (IOException e) {
            throw new FitsException("IO error reading Random Groups data " + e);
        }
        int pad = FitsUtil.padding(getTrueSize());
        try {
            str.skipBytes(pad);
        } catch (EOFException e) {
            throw new PaddingException("EOF reading padding after random groups", this);
        } catch (IOException e) {
            throw new FitsException("IO error reading padding after random groups");
        }
    }

    /** Write the RandomGroupsData */
    @Override
    public void write(ArrayDataOutput str) throws FitsException {
        try {
            str.writeArray(this.dataArray);
            FitsUtil.pad(str, getTrueSize());
        } catch (IOException e) {
            throw new FitsException("IO error writing random groups data " + e);
        }
    }

}
