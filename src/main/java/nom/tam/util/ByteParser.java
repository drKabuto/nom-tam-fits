package nom.tam.util;

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

/**
 * This class provides routines for efficient parsing of data stored in a byte
 * array. This routine is optimized (in theory at least!) for efficiency rather
 * than accuracy. The values read in for doubles or floats may differ in the
 * last bit or so from the standard input utilities, especially in the case
 * where a float is specified as a very long string of digits (substantially
 * longer than the precision of the type).
 * <p>
 * The get methods generally are available with or without a length parameter
 * specified. When a length parameter is specified only the bytes with the
 * specified range from the current offset will be search for the number. If no
 * length is specified, the entire buffer from the current offset will be
 * searched.
 * <p>
 * The getString method returns a string with leading and trailing white space
 * left intact. For all other get calls, leading white space is ignored. If
 * fillFields is set, then the get methods check that only white space follows
 * valid data and a FormatException is thrown if that is not the case. If
 * fillFields is not set and valid data is found, then the methods return having
 * read as much as possible. E.g., for the sequence "T123.258E13", a getBoolean,
 * getInteger and getFloat call would return true, 123, and 2.58e12 when called
 * in succession.
 */
public class ByteParser {

    /** Array being parsed */
    private byte[] input;

    /** Current offset into input. */
    private int offset;

    /** Length of last parsed value */
    private int numberLength;

    /** Did we find a sign last time we checked? */
    private boolean foundSign;

    /** Do we fill up fields? */
    private boolean fillFields = false;

    /**
     * Construct a parser.
     * 
     * @param input
     *            The byte array to be parsed. Note that the array can be
     *            re-used by refilling its contents and resetting the offset.
     */
    public ByteParser(byte[] input) {
        this.input = input;
        this.offset = 0;
    }

    /**
     * Find the sign for a number . This routine looks for a sign (+/-) at the
     * current location and return +1/-1 if one is found, or +1 if not. The
     * foundSign boolean is set if a sign is found and offset is incremented.
     */
    private int checkSign() {

        this.foundSign = false;

        if (this.input[this.offset] == '+') {
            this.foundSign = true;
            this.offset += 1;
            return 1;
        } else if (this.input[this.offset] == '-') {
            this.foundSign = true;
            this.offset += 1;
            return -1;
        }

        return 1;
    }

    /**
     * Get the integer value starting at the current position. This routine
     * returns a double rather than an int/long to enable it to read very long
     * integers (with reduced precision) such as
     * 111111111111111111111111111111111111111111. Note that this routine does
     * set numberLength.
     * 
     * @param length
     *            The maximum number of characters to use.
     */
    private double getBareInteger(int length) {

        int startOffset = this.offset;
        double number = 0;

        while (length > 0 && this.input[this.offset] >= '0' && this.input[this.offset] <= '9') {

            number *= 10;
            number += this.input[this.offset] - '0';
            this.offset += 1;
            length -= 1;
        }
        this.numberLength = this.offset - startOffset;
        return number;
    }

    /** Get a boolean value from the beginning of the buffer */
    public boolean getBoolean() throws FormatException {
        return getBoolean(this.input.length - this.offset);
    }

    /** Get a boolean value from a specified region of the buffer */
    public boolean getBoolean(int length) throws FormatException {

        int startOffset = this.offset;
        length -= skipWhite(length);
        if (length == 0) {
            throw new FormatException("Blank boolean field");
        }

        boolean value = false;
        if (this.input[this.offset] == 'T' || this.input[this.offset] == 't') {
            value = true;
        } else if (this.input[this.offset] != 'F' && this.input[this.offset] != 'f') {
            this.numberLength = 0;
            this.offset = startOffset;
            throw new FormatException("Invalid boolean value");
        }
        this.offset += 1;
        length -= 1;

        if (this.fillFields && length > 0) {
            if (isWhite(length)) {
                this.offset += length;
            } else {
                this.numberLength = 0;
                this.offset = startOffset;
                throw new FormatException("Non-white following boolean");
            }
        }
        this.numberLength = this.offset - startOffset;
        return value;
    }

    /** Get the buffer being used by the parser */
    public byte[] getBuffer() {
        return this.input;
    }

    /**
     * Read in the buffer until a double is read. This will read the entire
     * buffer if fillFields is set.
     * 
     * @return The value found.
     */
    public double getDouble() throws FormatException {
        return getDouble(this.input.length - this.offset);
    }

    /**
     * Look for a double in the buffer. Leading spaces are ignored.
     * 
     * @param length
     *            The maximum number of characters used to parse this number. If
     *            fillFields is specified then exactly only whitespace may
     *            follow a valid double value.
     */
    public double getDouble(int length) throws FormatException {

        int startOffset = this.offset;

        boolean error = true;

        double number = 0;
        // Skip initial blanks.
        length -= skipWhite(length);

        if (length == 0) {
            this.numberLength = this.offset - startOffset;
            return 0;
        }

        double mantissaSign = checkSign();
        if (this.foundSign) {
            length -= 1;
        }

        // Look for the special strings NaN, Inf,
        if (length >= 3 && (this.input[this.offset] == 'n' || this.input[this.offset] == 'N') && (this.input[this.offset + 1] == 'a' || this.input[this.offset + 1] == 'A')
                && (this.input[this.offset + 2] == 'n' || this.input[this.offset + 2] == 'N')) {

            number = Double.NaN;
            length -= 3;
            this.offset += 3;

            // Look for the longer string first then try the shorter.
        } else if (length >= 8 && (this.input[this.offset] == 'i' || this.input[this.offset] == 'I')
                && (this.input[this.offset + 1] == 'n' || this.input[this.offset + 1] == 'N') && (this.input[this.offset + 2] == 'f' || this.input[this.offset + 2] == 'F')
                && (this.input[this.offset + 3] == 'i' || this.input[this.offset + 3] == 'I') && (this.input[this.offset + 4] == 'n' || this.input[this.offset + 4] == 'N')
                && (this.input[this.offset + 5] == 'i' || this.input[this.offset + 5] == 'I') && (this.input[this.offset + 6] == 't' || this.input[this.offset + 6] == 'T')
                && (this.input[this.offset + 7] == 'y' || this.input[this.offset + 7] == 'Y')) {
            number = Double.POSITIVE_INFINITY;
            length -= 8;
            this.offset += 8;

        } else if (length >= 3 && (this.input[this.offset] == 'i' || this.input[this.offset] == 'I')
                && (this.input[this.offset + 1] == 'n' || this.input[this.offset + 1] == 'N') && (this.input[this.offset + 2] == 'f' || this.input[this.offset + 2] == 'F')) {
            number = Double.POSITIVE_INFINITY;
            length -= 3;
            this.offset += 3;

        } else {

            number = getBareInteger(length); // This will update offset
            length -= this.numberLength; // Set by getBareInteger

            if (this.numberLength > 0) {
                error = false;
            }

            // Check for fractional values after decimal
            if (length > 0 && this.input[this.offset] == '.') {

                this.offset += 1;
                length -= 1;

                double numerator = getBareInteger(length);
                if (numerator > 0) {
                    number += numerator / Math.pow(10., this.numberLength);
                }
                length -= this.numberLength;
                if (this.numberLength > 0) {
                    error = false;
                }
            }

            if (error) {
                this.offset = startOffset;
                this.numberLength = 0;
                throw new FormatException("Invalid real field");
            }

            // Look for an exponent
            if (length > 0) {

                // Our Fortran heritage means that we allow 'D' for the exponent
                // indicator.
                if (this.input[this.offset] == 'e' || this.input[this.offset] == 'E' || this.input[this.offset] == 'd' || this.input[this.offset] == 'D') {

                    this.offset += 1;
                    length -= 1;
                    if (length > 0) {
                        int sign = checkSign();
                        if (this.foundSign) {
                            length -= 1;
                        }

                        int exponent = (int) getBareInteger(length);

                        // For very small numbers we try to miminize
                        // effects of denormalization.
                        if (exponent * sign > -300) {
                            number *= Math.pow(10., exponent * sign);
                        } else {
                            number = 1.e-300 * (number * Math.pow(10., exponent * sign + 300));
                        }
                        length -= this.numberLength;
                    }
                }
            }
        }

        if (this.fillFields && length > 0) {

            if (isWhite(length)) {
                this.offset += length;
            } else {
                this.numberLength = 0;
                this.offset = startOffset;
                throw new FormatException("Non-blanks following real.");
            }
        }

        this.numberLength = this.offset - startOffset;
        return mantissaSign * number;
    }

    /**
     * Get a floating point value from the buffer. (see getDouble(int())
     */
    public float getFloat() throws FormatException {
        return (float) getDouble(this.input.length - this.offset);
    }

    /** Get a floating point value in a region of the buffer */
    public float getFloat(int length) throws FormatException {
        return (float) getDouble(length);
    }

    /** Look for an integer at the beginning of the buffer */
    public int getInt() throws FormatException {
        return getInt(this.input.length - this.offset);
    }

    /** Convert a region of the buffer to an integer */
    public int getInt(int length) throws FormatException {
        int startOffset = this.offset;

        length -= skipWhite(length);
        if (length == 0) {
            this.numberLength = this.offset - startOffset;
            return 0;
        }

        int number = 0;
        boolean error = true;

        int sign = checkSign();
        if (this.foundSign) {
            length -= 1;
        }

        while (length > 0 && this.input[this.offset] >= '0' && this.input[this.offset] <= '9') {
            number = number * 10 + this.input[this.offset] - '0';
            this.offset += 1;
            length -= 1;
            error = false;
        }

        if (error) {
            this.numberLength = 0;
            this.offset = startOffset;
            throw new FormatException("Invalid Integer");
        }

        if (length > 0 && this.fillFields) {
            if (isWhite(length)) {
                this.offset += length;
            } else {
                this.numberLength = 0;
                this.offset = startOffset;
                throw new FormatException("Non-white following integer");
            }
        }

        this.numberLength = this.offset - startOffset;
        return sign * number;
    }

    /** Look for a long in a specified region of the buffer */
    public long getLong(int length) throws FormatException {

        int startOffset = this.offset;

        // Skip white space.
        length -= skipWhite(length);
        if (length == 0) {
            this.numberLength = this.offset - startOffset;
            return 0;
        }

        long number = 0;
        boolean error = true;

        long sign = checkSign();
        if (this.foundSign) {
            length -= 1;
        }

        while (length > 0 && this.input[this.offset] >= '0' && this.input[this.offset] <= '9') {
            number = number * 10 + this.input[this.offset] - '0';
            error = false;
            this.offset += 1;
            length -= 1;
        }

        if (error) {
            this.numberLength = 0;
            this.offset = startOffset;
            throw new FormatException("Invalid long number");
        }

        if (length > 0 && this.fillFields) {
            if (isWhite(length)) {
                this.offset += length;
            } else {
                this.offset = startOffset;
                this.numberLength = 0;
                throw new FormatException("Non-white following long");
            }
        }
        this.numberLength = this.offset - startOffset;
        return sign * number;
    }

    /**
     * Get the number of characters used to parse the previous number (or the
     * length of the previous String returned).
     */
    public int getNumberLength() {
        return this.numberLength;
    }

    /**
     * Get the current offset
     * 
     * @return The current offset within the buffer.
     */
    public int getOffset() {
        return this.offset;
    }

    /**
     * Get a string
     * 
     * @param length
     *            The length of the string.
     */
    public String getString(int length) {

        String s = AsciiFuncs.asciiString(this.input, this.offset, length);
        this.offset += length;
        this.numberLength = length;
        return s;
    }

    /**
     * Is a region blank?
     * 
     * @param length
     *            The length of the region to be tested
     */
    private boolean isWhite(int length) {
        int oldOffset = this.offset;
        boolean value = skipWhite(length) == length;
        this.offset = oldOffset;
        return value;
    }

    /** Set the buffer for the parser */
    public void setBuffer(byte[] buf) {
        this.input = buf;
        this.offset = 0;
    }

    /**
     * Do we require a field to completely fill up the specified length (with
     * optional leading and trailing white space.
     * 
     * @param flag
     *            Is filling required?
     */
    public void setFillFields(boolean flag) {
        this.fillFields = flag;
    }

    /**
     * Set the offset into the array.
     * 
     * @param offset
     *            The desired offset from the beginning of the array.
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /** Skip bytes in the buffer */
    public void skip(int nBytes) {
        this.offset += nBytes;
    }

    /**
     * Skip white space. This routine skips with space in the input and returns
     * the number of character skipped. White space is defined as ' ', '\t',
     * '\n' or '\r'
     * 
     * @param length
     *            The maximum number of characters to skip.
     */
    public int skipWhite(int length) {

        int i;
        for (i = 0; i < length; i += 1) {
            if (this.input[this.offset + i] != ' ' && this.input[this.offset + i] != '\t' && this.input[this.offset + i] != '\n' && this.input[this.offset + i] != '\r') {
                break;
            }
        }

        this.offset += i;
        return i;

    }
}
