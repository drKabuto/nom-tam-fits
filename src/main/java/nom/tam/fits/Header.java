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

import static nom.tam.fits.header.NonStandard.CONTINUE;
import static nom.tam.fits.header.NonStandard.LONGSTRN;
import static nom.tam.fits.header.Standard.BITPIX;
import static nom.tam.fits.header.Standard.BLANK;
import static nom.tam.fits.header.Standard.COMMENT;
import static nom.tam.fits.header.Standard.END;
import static nom.tam.fits.header.Standard.EXTEND;
import static nom.tam.fits.header.Standard.GCOUNT;
import static nom.tam.fits.header.Standard.GROUPS;
import static nom.tam.fits.header.Standard.HISTORY;
import static nom.tam.fits.header.Standard.NAXIS;
import static nom.tam.fits.header.Standard.NAXISn;
import static nom.tam.fits.header.Standard.PCOUNT;
import static nom.tam.fits.header.Standard.SIMPLE;
import static nom.tam.fits.header.Standard.TFIELDS;
import static nom.tam.fits.header.Standard.XTENSION;

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import nom.tam.fits.header.IFitsHeader;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.AsciiFuncs;
import nom.tam.util.Cursor;
import nom.tam.util.HashedList;
import nom.tam.util.RandomAccess;

/**
 * This class describes methods to access and manipulate the header for a FITS
 * HDU. This class does not include code specific to particular types of HDU. As
 * of version 1.1 this class supports the long keyword convention which allows
 * long string keyword values to be split among multiple keywords
 * 
 * <pre>
 *    KEY        = 'ABC&'   /A comment
 *    CONTINUE      'DEF&'  / Another comment
 *    CONTINUE      'GHIJKL '
 * </pre>
 * 
 * The methods getStringValue(key), addValue(key,value,comment) and
 * deleteCard(key) will get, create/update and delete long string values if the
 * longStringsEnabled flag is set. This flag is set automatically when a FITS
 * header with a LONGSTRN card is found. The value is not checked. It may also
 * be set/unset using the static method setLongStringsEnabled(boolean). [So if a
 * user wishes to ensure that it is not set, it should be unset after any header
 * is read] When long strings are found in the FITS header users should be
 * careful not to interpose new header cards within a long value sequence. When
 * writing long strings, the comment is included in the last card. If a user is
 * writing long strings, a the keyword LONGSTRN = 'OGIP 1.0' should be added to
 * the FITS header, but this is not done automatically for the user.
 */
public class Header implements FitsElement {

    /**
     * The actual header data stored as a HashedList of HeaderCard's.
     */
    private HashedList<String, HeaderCard> cards = new HashedList<String, HeaderCard>() {

        @Override
        protected String intToKey(int i) {
            return Integer.toString(i);
        }

        @Override
        protected String valueToKey(HeaderCard value) {
            return value.getKey().key();
        }

    };

    /**
     * This iterator allows one to run through the list.
     */
    private Cursor<String, HeaderCard> iter = cards.iterator(0);

    /** Offset of this Header in the FITS file */
    private long fileOffset = -1;

    private List<HeaderCard> duplicates;

    /** Input descriptor last time header was read */
    private ArrayDataInput input;

    /**
     * Number of cards in header before duplicates were removed. A user may want
     * to know how large the actual FITS header was on input. Since the keyword
     * hash removes duplicate keys the internal size may be smaller. Added by
     * Booth Hartley (IPAC/Caltech).
     */
    private int originalCardCount = 0; // RBH ADDED

    /** Create an empty header */
    public Header() {
    }

    /** Do we support long strings when reading/writing keywords */
    private static boolean longStringsEnabled = false;

    public static void setLongStringsEnabled(boolean flag) {
        longStringsEnabled = flag;
    }

    public static boolean getLongStringsEnabled() {
        return longStringsEnabled;
    }

    /**
     * Create a header and populate it from the input stream
     * 
     * @param is
     *            The input stream where header information is expected.
     */
    public Header(ArrayDataInput is) throws TruncatedFileException, IOException {
        read(is);
    }

    /**
     * Create a header and initialize it with a vector of strings.
     * 
     * @param newCards
     *            Card images to be placed in the header.
     */
    public Header(String[] newCards) {

        for (String newCard : newCards) {
            HeaderCard card = new HeaderCard(newCard);
            if (card.getValue() == null) {
                cards.add(card);
            } else {
                cards.add(card.getKey().key(), card);
            }

        }
    }

    /**
     * Create a header which points to the given data object.
     * 
     * @param o
     *            The data object to be described.
     * @exception FitsException
     *                if the data was not valid for this header.
     */
    public Header(Data o) throws FitsException {
        o.fillHeader(this);
    }

    /** Create the data element corresponding to the current header */
    public Data makeData() throws FitsException {
        return FitsFactory.dataFactory(this);
    }

    /**
     * Get the size of the original header in bytes.
     */
    public long getOriginalSize() {
        return FitsUtil.addPadding(originalCardCount * 80);
    }

    /**
     * Indicate that we can use the current internal size of the Header as the
     * 'original' size (e.g., perhaps we've rewritten the header to disk). Note
     * that affects the results of rewriteable(), so users should not call this
     * method unless the underlying data has actually been updated.
     */
    public void resetOriginalSize() {
        originalCardCount = cards.size();
    }

    /**
     * Update a line in the header
     * 
     * @param key
     *            The key of the card to be replaced.
     * @param card
     *            A new card
     */
    public void updateLine(IFitsHeader key, HeaderCard card) throws HeaderCardException {
        removeCard(key);
        iter.addKeyed(card);
    }

    /**
     * Overwrite the lines in the header. Add the new PHDU header to the current
     * one. If keywords appear twice, the new value and comment overwrite the
     * current contents. By Richard J Mathar.
     * 
     * @param newHdr
     *            the list of new header data lines to replace the current ones.
     * @throws nom.tam.fits.HeaderCardException
     * @since 2005-10-24
     */
    public void updateLines(final Header newHdr) throws nom.tam.fits.HeaderCardException {
        Cursor<String, HeaderCard> j = newHdr.iterator();

        while (j.hasNext()) {
            HeaderCard nextHCard = (HeaderCard) j.next();
            // updateLine() doesn't work with COMMENTs because
            // this would allow only one COMMENT in total in each header
            // TODO: check if a normal equals does not do the trick
            if (nextHCard.getKey().key().startsWith(COMMENT.key())) {
                insertComment(nextHCard.getComment());
            } else {
                updateLine(nextHCard.getKey(), nextHCard);
            }
        }
    }

    /** Find the number of cards in the header */
    public int getNumberOfCards() {
        return cards.size();
    }

    /**
     * Get an iterator over the header cards
     */
    public Cursor<String, HeaderCard> iterator() {
        return cards.iterator(0);
    }

    /**
     * Get an iterator over the header cards, and procedeed to the specified
     * start index.
     */
    public Cursor<String, HeaderCard> iterator(int startIndex) {
        return cards.iterator(startIndex);
    }

    /** Get the offset of this header */
    @Override
    public long getFileOffset() {
        return fileOffset;
    }

    /**
     * Calculate the unpadded size of the data segment from the header
     * information.
     * 
     * @return the unpadded data segment size.
     */
    int trueDataSize() {

        if (!isValidHeader()) {
            return 0;
        }

        int naxis = getIntValue(NAXIS, 0);

        // If there are no axes then there is no data.
        if (naxis == 0) {
            return 0;
        }

        getIntValue(BITPIX);

        int[] axes = new int[naxis];

        for (int axis = 1; axis <= naxis; axis += 1) {
            axes[axis - 1] = getIntValue(NAXIS.n(axis), 0);
        }

        boolean isGroup = getBooleanValue(GROUPS, false);

        int pcount = getIntValue(PCOUNT, 0);
        int gcount = getIntValue(GCOUNT, 1);

        int startAxis = 0;

        if (isGroup && naxis > 1 && axes[0] == 0) {
            startAxis = 1;
        }

        int size = 1;
        for (int i = startAxis; i < naxis; i += 1) {
            size *= axes[i];
        }

        size += pcount;
        size *= gcount;

        // Now multiply by the number of bits per pixel and
        // convert to bytes.
        size *= Math.abs(getIntValue(BITPIX, 0)) / 8;

        return size;
    }

    /**
     * Return the size of the data including any needed padding.
     * 
     * @return the data segment size including any needed padding.
     */
    public long getDataSize() {
        return FitsUtil.addPadding(trueDataSize());
    }

    /** Get the size of the header in bytes */
    @Override
    public long getSize() {
        return headerSize();
    }

    /**
     * Return the size of the header data including padding.
     * 
     * @return the header size including any needed padding.
     */
    int headerSize() {

        if (!isValidHeader()) {
            return 0;
        }

        return FitsUtil.addPadding(cards.size() * 80);
    }

    /**
     * Is this a valid header.
     * 
     * @return <CODE>true</CODE> for a valid header, <CODE>false</CODE>
     *         otherwise.
     */
    boolean isValidHeader() {

        if (getNumberOfCards() < 4) {
            return false;
        }
        iter = iterator();
        IFitsHeader key = iter.next().getKey();
        if (key != SIMPLE && key != XTENSION) {
            return false;
        }
        key = iter.next().getKey();
        if (key != BITPIX) {
            return false;
        }
        key = iter.next().getKey();
        if (key != NAXIS) {
            return false;
        }
        while (iter.hasNext()) {
            key = iter.next().getKey();
        }
        if (key != END) {
            return false;
        }
        return true;

    }

    /**
     * Find the card associated with a given key. If found this sets the mark to
     * the card, otherwise it unsets the mark.
     * 
     * @param key
     *            The header key.
     * @return <CODE>null</CODE> if the keyword could not be found; return the
     *         HeaderCard object otherwise.
     */
    public HeaderCard findCard(IFitsHeader key) {

        HeaderCard card = (HeaderCard) cards.get(key.key());
        if (card != null) {
            iter.setKey(key.key());
        }
        return card;
    }

    /**
     * Get the value associated with the key as an int.
     * 
     * @param key
     *            The header key.
     * @param dft
     *            The value to be returned if the key is not found.
     */
    public int getIntValue(IFitsHeader key, int dft) {
        return (int) getLongValue(key, dft);
    }

    /**
     * Get the <CODE>int</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @return The associated value or 0 if not found.
     */
    public int getIntValue(IFitsHeader key) {
        return (int) getLongValue(key);
    }

    /**
     * Get the <CODE>long</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @return The associated value or 0 if not found.
     */
    public long getLongValue(IFitsHeader key) {
        return getLongValue(key, 0L);
    }

    /**
     * Get the <CODE>long</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @param dft
     *            The default value to be returned if the key cannot be found.
     * @return the associated value.
     */
    public long getLongValue(IFitsHeader key, long dft) {
        HeaderCard fcard = findCard(key);
        if (fcard == null) {
            return dft;
        }

        try {
            String v = fcard.getValue();
            if (v != null) {
                return Long.parseLong(v);
            }
        } catch (NumberFormatException e) {
        }

        return dft;
    }

    /**
     * Get the <CODE>float</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @param dft
     *            The value to be returned if the key is not found.
     */
    public float getFloatValue(IFitsHeader key, float dft) {
        return (float) getDoubleValue(key, dft);
    }

    /**
     * Get the <CODE>float</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @return The associated value or 0.0 if not found.
     */
    public float getFloatValue(IFitsHeader key) {
        return (float) getDoubleValue(key);

    }

    /**
     * Get the <CODE>double</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @return The associated value or 0.0 if not found.
     */
    public double getDoubleValue(IFitsHeader key) {
        return getDoubleValue(key, 0d);
    }

    /**
     * Get the <CODE>double</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @param dft
     *            The default value to return if the key cannot be found.
     * @return the associated value.
     */
    public double getDoubleValue(IFitsHeader key, double dft) {
        HeaderCard fcard = findCard(key);
        if (fcard == null) {
            return dft;
        }

        try {
            String v = fcard.getValue();
            if (v != null) {
                return new Double(v).doubleValue();
            }
        } catch (NumberFormatException e) {
        }

        return dft;
    }

    /**
     * Get the <CODE>boolean</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @return The value found, or false if not found or if the keyword is not a
     *         logical keyword.
     */
    public boolean getBooleanValue(IFitsHeader key) {
        return getBooleanValue(key, false);
    }

    /**
     * Get the <CODE>boolean</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @param dft
     *            The value to be returned if the key cannot be found or if the
     *            parameter does not seem to be a boolean.
     * @return the associated value.
     */
    public boolean getBooleanValue(IFitsHeader key, boolean dft) {

        HeaderCard fcard = findCard(key);
        if (fcard == null) {
            return dft;
        }

        String val = fcard.getValue();
        if (val == null) {
            return dft;
        }

        if (val.equals("T")) {
            return true;
        } else if (val.equals("F")) {
            return false;
        } else {
            return dft;
        }
    }

    /**
     * Get the <CODE>String</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @return The associated value or null if not found or if the value is not
     *         a string.
     */
    public String getStringValue(IFitsHeader key) {
        HeaderCard fcard = findCard(key);
        if (fcard == null || !fcard.isStringValue()) {
            return null;
        }
        String val = fcard.getValue();
        boolean append = longStringsEnabled && val != null && val.endsWith("&");
        iter.next(); // skip the primary card.
        while (append) {
            HeaderCard nxt = (HeaderCard) iter.next();
            if (nxt == null) {
                append = false;
            } else {
                key = nxt.getKey();
                String comm = nxt.getComment();
                if (key == null || comm == null || key != CONTINUE) {
                    append = false;
                } else {
                    comm = continueString(comm);
                    if (comm != null) {
                        comm = comm.substring(1, comm.length() - 1);
                        val = val.substring(0, val.length() - 1) + comm;
                        append = comm.endsWith("&");
                    }
                }
            }
        }

        return val;
    }

    /**
     * add a list with headers to the header
     * 
     * @param cards
     *            the list with cards to add .
     */
    public void addValue(List<HeaderCard> cards) {
        for (HeaderCard headerCard : cards) {
            addValue(headerCard);
        }
    }

    /**
     * Add a card image to the header.
     * 
     * @param fcard
     *            The card to be added.
     */
    public void addValue(HeaderCard fcard) {
        if (fcard.getKey() != COMMENT && fcard.getKey() != HISTORY && fcard.getKey() != BLANK) {
            try {
                removeCard(fcard.getKey());
            } catch (HeaderCardException e) {
                // we can ignore this
            }
        }
        if (fcard != null) {
            if (fcard.isKeyValuePair()) {
                iter.addKeyed(fcard);
            } else {
                iter.addUnKeyed(fcard);
            }
        }
    }

    /**
     * Add a card image to the header.
     * 
     * @param card
     *            The card to be added.
     * @exception HeaderCardException
     *                If the card is not valid.
     */
    public void addLine(String card) throws HeaderCardException {
        addValue(new HeaderCard(card));
    }

    /**
     * Create a header by reading the information from the input stream.
     * 
     * @param dis
     *            The input stream to read the data from.
     * @return <CODE>null</CODE> if there was a problem with the header;
     *         otherwise return the header read from the input stream.
     */
    public static Header readHeader(ArrayDataInput dis) throws TruncatedFileException, IOException {
        Header myHeader = new Header();
        try {
            myHeader.read(dis);
        } catch (EOFException e) {
            // An EOF exception is thrown only if the EOF was detected
            // when reading the first card. In this case we want
            // to return a null.
            return null;
        }
        return myHeader;
    }

    /**
     * Read a stream for header data.
     * 
     * @param dis
     *            The input stream to read the data from.
     */
    @Override
    public void read(ArrayDataInput dis) throws TruncatedFileException, IOException {
        if (dis instanceof RandomAccess) {
            fileOffset = FitsUtil.findOffset(dis);
        } else {
            fileOffset = -1;
        }

        byte[] buffer = new byte[80];

        boolean firstCard = true;
        int count = 0;

        try {
            while (true) {

                int len;

                int need = 80;

                try {

                    while (need > 0) {
                        len = dis.read(buffer, 80 - need, need);
                        count += 1;
                        if (len == 0) {
                            throw new TruncatedFileException();
                        }
                        need -= len;
                    }
                } catch (EOFException e) {

                    // Rethrow the EOF if we are at the beginning of the header,
                    // otherwise we have a FITS error.
                    // Added by Booth Hartley:
                    // If this is an extension HDU, then we may allow
                    // junk at the end and simply ignore it
                    //
                    if (firstCard && (need == 80 || fileOffset > 0 && FitsFactory.getAllowTerminalJunk())) {
                        throw e;
                    }
                    throw new TruncatedFileException(e.getMessage());
                }

                String cbuf = AsciiFuncs.asciiString(buffer);
                HeaderCard fcard = new HeaderCard(cbuf);

                IFitsHeader key = fcard.getKey();
                if (firstCard) {

                    if (key == null || key != SIMPLE && key != XTENSION) {
                        if (fileOffset > 0 && FitsFactory.getAllowTerminalJunk()) {
                            throw new EOFException("Not FITS format at " + fileOffset + ":" + cbuf);
                        } else {
                            throw new IOException("Not FITS format at " + fileOffset + ":" + cbuf);
                        }
                    }
                    firstCard = false;
                }

                if (key != null && cards.containsKey(key.key())) {
                    System.err.println("Warning: multiple occurrences of key:" + key);
                    addDuplicate((HeaderCard) cards.get(key));
                }

                // We don't check the value here. If the user
                // wants to be sure that long strings are disabled,
                // they can call setLongStringsEnabled(false) after
                // reading the header.
                // (Missing null check here fixed thanks to Kevin McAbee).
                if (key != null && key == LONGSTRN) {
                    longStringsEnabled = true;
                }
                // save card

                originalCardCount++; // RBH ADDED
                addValue(fcard);
                if (key == END) {
                    break; // Out of reading the header.
                }
            }

        } catch (EOFException e) {
            throw e;

        } catch (Exception e) {
            if (!(e instanceof EOFException)) {
                // For compatibility with Java V5 we just add in the error
                // message
                // rather than using using the cause mechanism.
                // Probably should update this when we can ignore Java 5.
                throw new IOException("Invalid FITS Header:" + e);
            }
        }
        if (fileOffset >= 0) {
            input = dis;
        }

        // Read to the end of the current FITS block.
        //
        try {
            dis.skipBytes(FitsUtil.padding(count * 80));
        } catch (IOException e) {
            throw new TruncatedFileException(e.getMessage());
        }
    }

    private void addDuplicate(HeaderCard dup) {
        if (duplicates == null) {
            duplicates = new ArrayList<HeaderCard>();
        }
        duplicates.add(dup);
    }

    /** Were duplicate header keys found when this record was read in? */
    public boolean hadDuplicates() {
        return duplicates != null;
    }

    /**
     * Return the list of duplicate cards. Note that when the header is read in,
     * only the last entry for a given keyword is retained in the active header.
     * This method returns earlier cards that have been discarded in the order
     * in which they were encountered in the header. It is possible for there to
     * be many cards with the same keyword in this list.
     */
    public List<HeaderCard> getDuplicates() {
        return duplicates;
    }

    /**
     * Find the card associated with a given key.
     * 
     * @param key
     *            The header key.
     * @return <CODE>null</CODE> if the keyword could not be found; return the
     *         card image otherwise.
     */
    public String findKey(IFitsHeader key) {
        HeaderCard card = findCard(key);
        if (card == null) {
            return null;
        } else {
            return card.toString();
        }
    }

    /**
     * Replace the key with a new key. Typically this is used when deleting or
     * inserting columns so that TFORMx -> TFORMx-1
     * 
     * @param oldKey
     *            The old header keyword.
     * @param newKey
     *            the new header keyword.
     * @return <CODE>true</CODE> if the card was replaced.
     * @exception HeaderCardException
     *                If <CODE>newKey</CODE> is not a valid FITS keyword.
     */
    boolean replaceKey(IFitsHeader oldKey, IFitsHeader newKey) throws HeaderCardException {

        HeaderCard oldCard = findCard(oldKey);
        if (oldCard == null) {
            return false;
        }
        if (!cards.replaceKey(oldKey.key(), newKey.key())) {
            throw new HeaderCardException("Duplicate key in replace");
        }

        oldCard.setKey(newKey);

        return true;
    }

    /**
     * Write the current header (including any needed padding) to the output
     * stream.
     * 
     * @param dos
     *            The output stream to which the data is to be written.
     * @exception FitsException
     *                if the header could not be written.
     */
    @Override
    public void write(ArrayDataOutput dos) throws FitsException {

        fileOffset = FitsUtil.findOffset(dos);

        // Ensure that all cards are in the proper order.
        cards.sort(new HeaderOrder());
        checkBeginning();
        checkEnd();
        if (cards.size() <= 0) {
            return;
        }

        Cursor<String, HeaderCard> iter = cards.iterator(0);

        try {
            while (iter.hasNext()) {
                HeaderCard card = (HeaderCard) iter.next();

                byte[] b = AsciiFuncs.getBytes(card.toString());
                dos.write(b);
            }

            FitsUtil.pad(dos, getNumberOfCards() * 80, (byte) ' ');
        } catch (IOException e) {
            throw new FitsException("IO Error writing header: " + e);
        }
        try {
            dos.flush();
        } catch (IOException e) {
        }

    }

    /** Rewrite the header. */
    @Override
    public void rewrite() throws FitsException, IOException {

        ArrayDataOutput dos = (ArrayDataOutput) input;

        if (rewriteable()) {
            FitsUtil.reposition(dos, fileOffset);
            write(dos);
            dos.flush();
        } else {
            throw new FitsException("Invalid attempt to rewrite Header.");
        }
    }

    /** Reset the file pointer to the beginning of the header */
    @Override
    public boolean reset() {
        try {
            FitsUtil.reposition(input, fileOffset);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Can the header be rewritten without rewriting the entire file? */
    @Override
    public boolean rewriteable() {

        if (fileOffset >= 0 && input instanceof ArrayDataOutput && (cards.size() + 35) / 36 == (originalCardCount + 35) / 36) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Delete a key.
     * 
     * @param key
     *            The header key.
     */
    public void removeCard(IFitsHeader key) throws HeaderCardException {
        if (cards.containsKey(key.key())) {
            iter.setKey(key.key());
            if (iter.hasNext()) {
                HeaderCard hc = (HeaderCard) iter.next();
                String val = hc.getValue();
                boolean delExtensions = longStringsEnabled && val != null && val.endsWith("&");
                iter.remove();
                while (delExtensions) {
                    hc = (HeaderCard) iter.next();
                    if (hc == null) {
                        delExtensions = false;
                    } else {
                        if (hc.getKey() == CONTINUE) {
                            String more = hc.getComment();
                            more = continueString(more);
                            if (more != null) {
                                iter.remove();
                                delExtensions = more.endsWith("&'");
                            } else {
                                delExtensions = false;
                            }
                        } else {
                            delExtensions = false;
                        }
                    }
                }
            }
        }
    }

    /**
     * Look for the continuation part of a COMMENT. The comment may also include
     * a 'real' comment, e.g.,
     * 
     * <pre>
     *  X = 'AB&'
     *  CONTINUE 'CDEF' / ABC
     * </pre>
     * 
     * Here we are looking for just the 'CDEF' part of the CONTINUE card.
     */
    private String continueString(String input) {
        if (input == null) {
            return null;
        }

        input = input.trim();
        if (input.length() < 2 || input.charAt(0) != '\'') {
            return null;
        }

        for (int i = 1; i < input.length(); i += 1) {
            char c = input.charAt(i);
            if (c == '\'') {
                if (i < input.length() - 1 && input.charAt(i + 1) == c) {
                    // consecutive quotes -> escaped single quote
                    // Get rid of the extra quote.
                    input = input.substring(0, i) + input.substring(i + 1);
                    continue; // Check the next character.
                } else {
                    // Found closing apostrophe
                    return input.substring(0, i + 1);
                }
            }
        }
        // Never found a closing apostrophe.
        return null;
    }

    /**
     * Add a line to the header using the COMMENT style, i.e., no '=' in column
     * 9.
     * 
     * @param header
     *            The comment style header.
     * @param value
     *            A string to follow the header.
     * @exception HeaderCardException
     *                If the parameters cannot build a valid FITS card.
     */
    public void insertCommentStyle(IFitsHeader header, String value) {
        // Should just truncate strings, so we should never get
        // an exception...

        try {
            iter.addUnKeyed(header.card().comment(value));
        } catch (HeaderCardException e) {
            System.err.println("Impossible Exception for comment style:" + header + ":" + value);
        }
    }

    /**
     * Add a COMMENT line.
     * 
     * @param value
     *            The comment.
     * @exception HeaderCardException
     *                If the parameter is not a valid FITS comment.
     */
    public void insertComment(String value) throws HeaderCardException {
        insertCommentStyle(COMMENT, value);
    }

    /**
     * Add a HISTORY line.
     * 
     * @param value
     *            The history record.
     * @exception HeaderCardException
     *                If the parameter is not a valid FITS comment.
     */
    public void insertHistory(String value) throws HeaderCardException {
        insertCommentStyle(HISTORY, value);
    }

    /**
     * Delete the card associated with the given key. Nothing occurs if the key
     * is not found.
     * 
     * @param key
     *            The header key.
     */
    public void deleteKey(IFitsHeader key) {
        iter.setKey(key.key());
        if (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
    }

    /**
     * Tests if the specified keyword is present in this table.
     * 
     * @param key
     *            the keyword to be found.
     * @return <CODE>true<CODE> if the specified keyword is present in this
     * 		table; <CODE>false<CODE> otherwise.
     */
    public final boolean containsKey(IFitsHeader key) {
        return cards.containsKey(key.key());
    }

    /**
     * Create a header for a null image.
     */
    void nullImage() {
        iter = iterator();
        try {
            addValue(SIMPLE.card().value(true).comment("ntf::header:simple:2"));
            addValue(BITPIX.card().value(8).comment("ntf::header:bitpix:2"));
            addValue(NAXIS.card().value(0).comment("ntf::header:naxis:2"));
            addValue(EXTEND.card().value(true).comment("ntf::header:extend:2"));
        } catch (HeaderCardException e) {
        }
    }

    /**
     * Set the SIMPLE keyword to the given value.
     * 
     * @param val
     *            The boolean value -- Should be true for FITS data.
     */
    public void setSimple(boolean val) {
        deleteKey(SIMPLE);
        deleteKey(XTENSION);

        // If we're flipping back to and from the primary header
        // we need to add in the EXTEND keyword whenever we become
        // a primary, because it's not permitted in the extensions
        // (at least not where it needs to be in the primary array).
        if (findCard(NAXIS) != null) {
            int nax = getIntValue(NAXIS);

            iter = iterator();

            if (findCard(NAXISn.n(nax)) != null) {
                iter.next();
                try {
                    removeCard(EXTEND);
                    iter.addKeyed(EXTEND.card().value(true).comment("ntf::header:extend:1"));
                } catch (Exception e) { // Ignore the exception
                }
            }
        }

        iter = iterator();
        try {
            iter.add(SIMPLE.key(), SIMPLE.card().value(val).comment("ntf::header:simple:1"));
        } catch (HeaderCardException e) {
            System.err.println("Impossible exception at setSimple " + e);
        }
    }

    /**
     * Set the XTENSION keyword to the given value.
     * 
     * @param val
     *            The name of the extension. "IMAGE" and "BINTABLE" are
     *            supported.
     */
    public void setXtension(String val) {
        deleteKey(SIMPLE);
        deleteKey(XTENSION);
        deleteKey(EXTEND);
        iter = iterator();
        try {
            iter.addKeyed(XTENSION.card().value(val).comment("ntf::header:xtension:1"));
        } catch (HeaderCardException e) {
            System.err.println("Impossible exception at setXtension " + e);
        }
    }

    /**
     * Set the BITPIX value for the header. The following values are permitted
     * by FITS conventions:
     * <ul>
     * <li>8 -- signed byte data. Also used for tables.</li>
     * <li>16 -- signed short data.</li>
     * <li>32 -- signed int data.</li>
     * <li>64 -- signed long data.</li>
     * <li>-32 -- IEEE 32 bit floating point numbers.</li>
     * <li>-64 -- IEEE 64 bit floating point numbers.</li>
     * </ul>
     * 
     * @param val
     *            The value set by the user.
     */
    public void setBitpix(int val) {
        iter = iterator();
        iter.next();
        try {
            iter.addKeyed(BITPIX.card().value(val).comment("ntf::header:bitpix:1"));
        } catch (HeaderCardException e) {
            System.err.println("Impossible exception at setBitpix " + e);
        }
    }

    /**
     * Set the value of the NAXIS keyword
     * 
     * @param val
     *            The dimensionality of the data.
     */
    public void setNaxes(int val) {
        iter.setKey(BITPIX.key());
        if (iter.hasNext()) {
            iter.next();
        }

        try {
            iter.addKeyed(NAXIS.card().value(val).comment("ntf::header:naxis:1"));
        } catch (HeaderCardException e) {
            System.err.println("Impossible exception at setNaxes " + e);
        }
    }

    /**
     * Set the dimension for a given axis.
     * 
     * @param axis
     *            The axis being set.
     * @param dim
     *            The dimension
     */
    public void setNaxis(int axis, int dim) {
        if (axis <= 0) {
            return;
        }
        if (axis == 1) {
            iter.setKey(NAXIS.key());
        } else if (axis > 1) {
            iter.setKey(NAXISn.n(axis - 1).key());
        }
        if (iter.hasNext()) {
            iter.next();
        }
        try {
            iter.addKeyed(NAXISn.n(axis).card().value(dim).comment("ntf::header:naxisN:1"));

        } catch (HeaderCardException e) {
            System.err.println("Impossible exception at setNaxis " + e);
        }
    }

    /**
     * Ensure that the header begins with a valid set of keywords. Note that we
     * do not check the values of these keywords.
     */
    void checkBeginning() throws FitsException {

        iter = iterator();

        if (!iter.hasNext()) {
            throw new FitsException("Empty Header");
        }
        HeaderCard card = (HeaderCard) iter.next();
        IFitsHeader key = card.getKey();
        if (key != SIMPLE && key != XTENSION) {
            throw new FitsException("No SIMPLE or XTENSION at beginning of Header");
        }
        boolean isTable = false;
        boolean isExtension = false;
        if (key == XTENSION) {
            String value = card.getValue();
            if (value == null) {
                throw new FitsException("Empty XTENSION keyword");
            }
            value = value.trim();
            isExtension = true;

            if (value.equals("BINTABLE") || value.equals("A3DTABLE") || value.equals("TABLE")) {
                isTable = true;
            }
        }

        cardCheck(BITPIX);
        cardCheck(NAXIS);

        int nax = getIntValue(NAXIS);
        iter.next();

        for (int i = 1; i <= nax; i += 1) {
            cardCheck(NAXISn.n(i));
        }

        if (isExtension) {
            cardCheck(PCOUNT);
            cardCheck(GCOUNT);
            if (isTable) {
                cardCheck(TFIELDS);
            }
        }
        // This does not check for the EXTEND keyword which
        // if present in the primary array must immediately follow
        // the NAXISn.
    }

    /**
     * Check if the given key is the next one available in the header.
     */
    private void cardCheck(IFitsHeader key) throws FitsException {

        if (!iter.hasNext()) {
            throw new FitsException("Header terminates before " + key);
        }
        HeaderCard card = (HeaderCard) iter.next();
        if (!card.getKey().equals(key)) {
            throw new FitsException("Key " + key + " not found where expected. Found " + card.getKey());
        }
    }

    /**
     * Ensure that the header has exactly one END keyword in the appropriate
     * location.
     */
    void checkEnd() {

        // Ensure we have an END card only at the end of the
        // header.
        //
        iter = iterator();
        HeaderCard card;

        while (iter.hasNext()) {
            card = (HeaderCard) iter.next();
            if (!card.isKeyValuePair() && card.getKey() == END) {
                iter.remove();
            }
        }
        try {
            // End cannot have a comment
            iter.addUnKeyed(END.card());
        } catch (HeaderCardException e) {
        }
    }

    /**
     * Print the header to a given stream.
     * 
     * @param ps
     *            the stream to which the card images are dumped.
     */
    public void dumpHeader(PrintStream ps) {
        iter = iterator();
        while (iter.hasNext()) {
            ps.println(iter.next());
        }
    }

    /***** Deprecated methods *******/
    /**
     * Find the number of cards in the header
     * 
     * @deprecated see numberOfCards(). The units of the size of the header may
     *             be unclear.
     */
    @Deprecated
    public int size() {
        return cards.size();
    }

    /**
     * Get the n'th card image in the header
     * 
     * @return the card image; return <CODE>null</CODE> if the n'th card does
     *         not exist.
     * @deprecated An iterator should be used for sequential access to the
     *             header.
     */
    @Deprecated
    public String getCard(int n) {
        if (n >= 0 && n < cards.size()) {
            iter = cards.iterator(n);
            HeaderCard c = (HeaderCard) iter.next();
            return c.toString();
        }
        return null;
    }

    /**
     * Get the n'th key in the header.
     * 
     * @return the card image; return <CODE>null</CODE> if the n'th key does not
     *         exist.
     * @deprecated An iterator should be used for sequential access to the
     *             header.
     */
    @Deprecated
    public String getKey(int n) {

        String card = getCard(n);
        if (card == null) {
            return null;
        }

        String key = card.substring(0, 8);
        if (key.charAt(0) == ' ') {
            return "";
        }

        if (key.indexOf(' ') >= 1) {
            key = key.substring(0, key.indexOf(' '));
        }
        return key;
    }

    /**
     * Create a header which points to the given data object.
     * 
     * @param o
     *            The data object to be described.
     * @exception FitsException
     *                if the data was not valid for this header.
     * @deprecated Use the appropriate Header constructor.
     */
    @Deprecated
    public void pointToData(Data o) throws FitsException {
        o.fillHeader(this);
    }

    /**
     * Find the end of a set of keywords describing a column or axis (or
     * anything else terminated by an index. This routine leaves the header
     * ready to add keywords after any existing keywords with the index
     * specified. The user should specify a prefix to a keyword that is
     * guaranteed to be present.
     */
    Cursor<String, HeaderCard> positionAfter(IFitsHeader key) {
        iter.setKey(key.key());
        int colNr = colNrFromKey(key);
        if (iter.hasNext()) {

            // Bug fix (references to forward) here by Laurent Borges
            boolean forward = false;

            while (iter.hasNext()) {

                IFitsHeader nextKey = iter.next().getKey();
                int nextColNr = colNrFromKey(nextKey);
                if (colNr != nextColNr) {
                    forward = true;
                    break;
                }
            }
            if (forward) {
                iter.prev(); // Gone one too far, so skip back an element.
            }
        }
        return iter;
    }

    private int colNrFromKey(IFitsHeader nextKey) {
        String keyString = nextKey.key();
        int charIndex = keyString.length() - 1;
        while (Character.isDigit(keyString.charAt(charIndex))) {
            charIndex--;
        }
        int startOfColIndex = charIndex + 1;
        int colNr = -1;
        if (startOfColIndex < keyString.length()) {
            colNr = Integer.parseInt(keyString.substring(startOfColIndex));
        }
        return colNr;
    }

    /** Get the next card in the Header using the current iterator */
    public HeaderCard nextCard() {
        if (iter == null) {
            return null;
        }
        if (iter.hasNext()) {
            return (HeaderCard) iter.next();
        } else {
            return null;
        }
    }

    /**
     * Move after the EXTEND keyword in images. Used in bug fix noted by V.
     * Forchi
     */
    void afterExtend() {
        if (findCard(EXTEND) != null) {
            nextCard();
        }
    }
}
