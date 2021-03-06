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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.RandomAccess;

/**
 * This class supports the FITS heap. This is currently used for variable length
 * columns in binary tables.
 */
public class FitsHeap implements FitsElement {

    /**
     * The storage buffer
     */
    private byte[] heap;

    /**
     * The current used size of the buffer <= heap.length
     */
    private int heapSize;

    /**
     * The offset within a file where the heap begins
     */
    private long fileOffset = -1;

    /**
     * Has the heap ever been expanded?
     */
    private boolean expanded = false;

    /**
     * The stream the last read used
     */
    private ArrayDataInput input;

    /**
     * Our current offset into the heap. When we read from the heap we use a
     * byte array input stream. So long as we continue to read further into the
     * heap, we can continue to use the same stream, but we need to recreate the
     * stream whenever we skip backwards.
     */
    private int heapOffset = 0;

    /**
     * A stream used to read the heap data
     */
    private BufferedDataInputStream bstr;

    /**
     * Create a heap of a given size.
     */
    FitsHeap(int size) {
        this.heapSize = size;
        if (size < 0) {
            throw new IllegalArgumentException("Illegal size for FITS heap:" + size);
        }
    }

    private void allocate() {
        if (this.heap == null) {
            this.heap = new byte[this.heapSize];
        }
    }

    /**
     * Check if the Heap can accommodate a given requirement. If not expand the
     * heap.
     */
    void expandHeap(int need) {

        // Invalidate any existing input stream to the heap.
        this.bstr = null;
        allocate();

        if (this.heapSize + need > this.heap.length) {
            this.expanded = true;
            int newlen = (this.heapSize + need) * 2;
            if (newlen < 16384) {
                newlen = 16384;
            }
            byte[] newHeap = new byte[newlen];
            System.arraycopy(this.heap, 0, newHeap, 0, this.heapSize);
            this.heap = newHeap;
        }
    }

    /**
     * Get data from the heap.
     * 
     * @param offset
     *            The offset at which the data begins.
     * @param array
     *            The array to be extracted.
     */
    public void getData(int offset, Object array) throws FitsException {

        allocate();
        try {
            // Can we reuse the existing byte stream?
            if (this.bstr == null || this.heapOffset > offset) {
                this.heapOffset = 0;
                this.bstr = new BufferedDataInputStream(new ByteArrayInputStream(this.heap));
            }

            this.bstr.skipBytes(offset - this.heapOffset);
            this.heapOffset = offset;
            this.heapOffset += this.bstr.readLArray(array);

        } catch (IOException e) {
            throw new FitsException("Error decoding heap area at offset=" + offset + ".  Exception: Exception " + e);
        }
    }

    /**
     * Get the file offset of the heap
     */
    @Override
    public long getFileOffset() {
        return this.fileOffset;
    }

    /**
     * Return the size of the heap using the more bean compatible format
     */
    @Override
    public long getSize() {
        return size();
    }

    /**
     * Add some data to the heap.
     */
    int putData(Object data) throws FitsException {

        long lsize = ArrayFuncs.computeLSize(data);
        if (lsize > Integer.MAX_VALUE) {
            throw new FitsException("FITS Heap > 2 G");
        }
        int size = (int) lsize;
        expandHeap(size);
        ByteArrayOutputStream bo = new ByteArrayOutputStream(size);

        try {
            BufferedDataOutputStream o = new BufferedDataOutputStream(bo);
            o.writeArray(data);
            o.flush();
            o.close();
        } catch (IOException e) {
            throw new FitsException("Unable to write variable column length data");
        }

        System.arraycopy(bo.toByteArray(), 0, this.heap, this.heapSize, size);
        int oldOffset = this.heapSize;
        this.heapSize += size;

        return oldOffset;
    }

    /**
     * Read the heap
     */
    @Override
    public void read(ArrayDataInput str) throws FitsException {

        if (str instanceof RandomAccess) {
            this.fileOffset = FitsUtil.findOffset(str);
            this.input = str;
        }

        if (this.heapSize > 0) {
            allocate();
            try {
                str.read(this.heap, 0, this.heapSize);
            } catch (IOException e) {
                throw new FitsException("Error reading heap:" + e);
            }
        }

        this.bstr = null;
    }

    @Override
    public boolean reset() {
        try {
            FitsUtil.reposition(this.input, this.fileOffset);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Attempt to rewrite the heap with the current contents. Note that no
     * checking is done to make sure that the heap does not extend past its
     * prior boundaries.
     */
    @Override
    public void rewrite() throws IOException, FitsException {
        allocate();
        if (rewriteable()) {
            ArrayDataOutput str = (ArrayDataOutput) this.input;
            FitsUtil.reposition(str, this.fileOffset);
            write(str);
        } else {
            throw new FitsException("Invalid attempt to rewrite FitsHeap");
        }

    }

    @Override
    public boolean rewriteable() {
        return this.fileOffset >= 0 && this.input instanceof ArrayDataOutput && !this.expanded;
    }

    /**
     * Return the size of the Heap
     */
    public int size() {
        return this.heapSize;
    }

    /**
     * Write the heap
     */
    @Override
    public void write(ArrayDataOutput str) throws FitsException {
        allocate();
        try {
            str.write(this.heap, 0, this.heapSize);
        } catch (IOException e) {
            throw new FitsException("Error writing heap:" + e);
        }
    }
}
