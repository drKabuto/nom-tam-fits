package nom.tam.fits;

import static nom.tam.fits.header.FitsHeaderIndex.findOrCreateKey;

import java.util.LinkedList;
import java.util.List;

import nom.tam.fits.header.IFitsHeader;
import nom.tam.fits.header.NonStandard;
import nom.tam.fits.header.Standard;

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
 * This class describes methods to access and manipulate the individual cards
 * for a FITS Header.
 */
public class HeaderCard {

    /**
     * this string represents the boolean false value in a fits header.
     */
    private static final String FITS_FALSE_STRING = "F";

    /**
     * this string represents the boolean true value in a fits header.
     */
    private static final String FITS_TRUE_STRING = "T";

    /** The keyword part of the card (set to null if there's no keyword) */
    private IFitsHeader key;

    /** The value part of the card (set to null if there's no value) */
    private String value;

    /** The comment part of the card (set to null if there's no comment) */
    private String comment;

    /** Does this card represent a nullable field. ? */
    private boolean nullable;

    /** A flag indicating whether or not this is a string value */
    private boolean isString;

    /** Maximum length of a FITS keyword field */
    public static final int MAX_KEYWORD_LENGTH = 8;

    /** Maximum length of a FITS value field */
    public static final int MAX_VALUE_LENGTH = 70;

    /** padding for building card images */
    private static String space80 = "                                                                                ";

    public HeaderCard(IFitsHeader key) throws HeaderCardException {
        if (key == null && value != null) {
            throw new HeaderCardException("Null keyword with non-null value");
        }
        if (key != null && key.key().length() > MAX_KEYWORD_LENGTH) {
            if (!FitsFactory.getUseHierarch() || !key.key().substring(0, 9).equals("HIERARCH.")) {
                throw new HeaderCardException("Keyword too long");
            }
        }
        this.key = key;
        this.isString = true;
    }

    /**
     * Create a string from a double making sure that it's not more than 20
     * characters long. Probably would be better if we had a way to override
     * this since we can loose precision for some doubles.
     */
    private static String dblString(double input) {
        String value = String.valueOf(input);
        if (value.length() > 20) {
            java.util.Formatter formatter = new java.util.Formatter();
            value = formatter.format("%20.13G", input).out().toString();
            formatter.close();
        }
        return value;
    }

    /**
     * Create a HeaderCard from a FITS card image
     * 
     * @param card
     *            the 80 character card image
     */
    public HeaderCard(String card) {
        key = null;
        value = null;
        comment = null;
        isString = false;

        if (card.length() > 80) {
            card = card.substring(0, 80);
        }

        if (FitsFactory.getUseHierarch() && card.length() > 9 && card.substring(0, 9).equals("HIERARCH ")) {
            hierarchCard(card);
            return;
        }

        // We are going to assume that the value has no blanks in
        // it unless it is enclosed in quotes. Also, we assume that
        // a / terminates the string (except inside quotes)

        // treat short lines as special keywords
        if (card.length() < 9) {
            key = findOrCreateKey(card);
            return;
        } else {

            // extract the key
            String keyString = card.substring(0, 8).trim();
            IFitsHeader potentialKey = findOrCreateKey(keyString);
            key = potentialKey;
        }
        // if it is an empty key, assume the remainder of the card is a comment
        if (key.key().length() == 0) {
            comment = card.substring(8);
            return;
        }

        // Non-key/value pair lines are treated as keyed comments
        if (key.equals(Standard.COMMENT) || key.equals(Standard.HISTORY) || !card.substring(8, 10).equals("= ")) {
            comment = card.substring(8).trim();
            return;
        }

        // extract the value/comment part of the string
        String valueAndComment = card.substring(10).trim();

        // If there is no value/comment part, we are done.
        if (valueAndComment.length() == 0) {
            value = "";
            return;
        }

        int vend = -1;
        // If we have a ' then find the matching '.
        if (valueAndComment.charAt(0) == '\'') {

            int offset = 1;
            while (offset < valueAndComment.length()) {

                // look for next single-quote character
                vend = valueAndComment.indexOf("'", offset);

                // if the quote character is the last character on the line...
                if (vend == valueAndComment.length() - 1) {
                    break;
                }

                // if we did not find a matching single-quote...
                if (vend == -1) {
                    // pretend this is a comment card
                    key = null;
                    comment = card;
                    return;
                }

                // if this is not an escaped single-quote, we are done
                if (valueAndComment.charAt(vend + 1) != '\'') {
                    break;
                }

                // skip past escaped single-quote
                offset = vend + 2;
            }

            // break apart character string
            value = valueAndComment.substring(1, vend).trim();
            value = value.replace("''", "'");

            if (vend + 1 >= valueAndComment.length()) {
                comment = null;
            } else {

                comment = valueAndComment.substring(vend + 1).trim();
                if (comment.charAt(0) == '/') {
                    if (comment.length() > 1) {
                        comment = comment.substring(1);
                    } else {
                        comment = "";
                    }
                }

                if (comment.length() == 0) {
                    comment = null;
                }

            }
            isString = true;

        } else {

            // look for a / to terminate the field.
            int slashLoc = valueAndComment.indexOf('/');
            if (slashLoc != -1) {
                comment = valueAndComment.substring(slashLoc + 1).trim();
                value = valueAndComment.substring(0, slashLoc).trim();
            } else {
                value = valueAndComment;
            }
        }
    }

    /**
     * Process HIERARCH style cards... HIERARCH LEV1 LEV2 ... = value / comment
     * The keyword for the card will be "HIERARCH.LEV1.LEV2..." A '/' is assumed
     * to start a comment.
     */
    private void hierarchCard(String card) {

        String name = "";
        String token = null;
        String separator = "";
        int[] tokLimits;
        int posit = 0;
        int commStart = -1;

        // First get the hierarchy levels
        while ((tokLimits = getToken(card, posit)) != null) {
            token = card.substring(tokLimits[0], tokLimits[1]);
            if (!token.equals("=")) {
                name += separator + token;
                separator = ".";
            } else {
                tokLimits = getToken(card, tokLimits[1]);
                if (tokLimits != null) {
                    token = card.substring(tokLimits[0], tokLimits[1]);
                } else {
                    key = findOrCreateKey(name);
                    value = null;
                    comment = null;
                    return;
                }
                break;
            }
            posit = tokLimits[1];
        }
        key = findOrCreateKey(name);

        // At the end?
        if (tokLimits == null) {
            value = null;
            comment = null;
            isString = false;
            return;
        }

        // Really should consolidate the two instances
        // of this test in this class!
        if (token.charAt(0) == '\'') {
            // Find the next undoubled quote...
            isString = true;
            if (token.length() > 1 && token.charAt(1) == '\'' && (token.length() == 2 || token.charAt(2) != '\'')) {
                value = "";
                commStart = tokLimits[0] + 2;
            } else if (card.length() < tokLimits[0] + 2) {
                value = null;
                comment = null;
                isString = false;
                return;
            } else {
                int i;
                for (i = tokLimits[0] + 1; i < card.length(); i += 1) {
                    if (card.charAt(i) == '\'') {
                        if (i == card.length() - 1) {
                            value = card.substring(tokLimits[0] + 1, i);
                            commStart = i + 1;
                            break;
                        } else if (card.charAt(i + 1) == '\'') {
                            // Doubled quotes.
                            i += 1;
                            continue;
                        } else {
                            value = card.substring(tokLimits[0] + 1, i);
                            commStart = i + 1;
                            break;
                        }
                    }
                }
            }
            if (commStart < 0) {
                value = null;
                comment = null;
                isString = false;
                return;
            }
            for (int i = commStart; i < card.length(); i += 1) {
                if (card.charAt(i) == '/') {
                    comment = card.substring(i + 1).trim();
                    break;
                } else if (card.charAt(i) != ' ') {
                    comment = null;
                    break;
                }
            }
        } else {
            isString = false;
            int sl = token.indexOf('/');
            if (sl == 0) {
                value = null;
                comment = card.substring(tokLimits[0] + 1);
            } else if (sl > 0) {
                value = token.substring(0, sl);
                comment = card.substring(tokLimits[0] + sl + 1);
            } else {
                value = token;

                for (int i = tokLimits[1]; i < card.length(); i += 1) {
                    if (card.charAt(i) == '/') {
                        comment = card.substring(i + 1).trim();
                        break;
                    } else if (card.charAt(i) != ' ') {
                        comment = null;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Get the next token. Can't use StringTokenizer since we sometimes need to
     * know the position within the string.
     */
    private int[] getToken(String card, int posit) {

        int i;
        for (i = posit; i < card.length(); i += 1) {
            if (card.charAt(i) != ' ') {
                break;
            }
        }

        if (i >= card.length()) {
            return null;
        }

        if (card.charAt(i) == '=') {
            return new int[]{
                i,
                i + 1
            };
        }

        int j;
        for (j = i + 1; j < card.length(); j += 1) {
            if (card.charAt(j) == ' ' || card.charAt(j) == '=') {
                break;
            }
        }
        return new int[]{
            i,
            j
        };
    }

    /**
     * Does this card contain a string value?
     */
    public boolean isStringValue() {
        return isString;
    }

    /**
     * Is this a key/value card?
     */
    public boolean isKeyValuePair() {
        return key != null && value != null;
    }

    /**
     * Set the key.
     */
    void setKey(IFitsHeader newKey) {
        key = newKey;
    }

    /**
     * Return the keyword from this card
     */
    public IFitsHeader getKey() {
        return key;
    }

    /**
     * Return the value from this card
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the value for this card.
     */
    public void setValue(String update) {
        value = update;
    }

    /**
     * Return the comment from this card
     */
    public String getComment() {
        return comment;
    }

    /**
     * Return the 80 character card image
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(80);

        // start with the keyword, if there is one
        if (key != null) {
            if (key.key().length() > 9 && key.key().substring(0, 9).equals("HIERARCH.")) {
                return hierarchToString();
            }
            buf.append(key.key());
            if (key.key().length() < 8) {
                buf.append(space80.substring(0, 8 - buf.length()));
            }
        }

        if (value != null || nullable) {
            buf.append("= ");

            if (value != null) {

                if (isString) {
                    // left justify the string inside the quotes
                    buf.append('\'');
                    buf.append(value.replace("'", "''"));
                    if (buf.length() < 19) {

                        buf.append(space80.substring(0, 19 - buf.length()));
                    }
                    buf.append('\'');
                    // Now add space to the comment area starting at column 40
                    if (buf.length() < 30) {
                        buf.append(space80.substring(0, 30 - buf.length()));
                    }

                } else {

                    buf.length();
                    if (value.length() < 20) {
                        buf.append(space80.substring(0, 20 - value.length()));
                    }

                    buf.append(value);

                }
            } else {
                // Pad out a null value.
                buf.append(space80.substring(0, 20));
            }

            // if there is a comment, add a comment delimiter
            if (comment != null) {
                buf.append(" / ");
            }

        } else if (comment != null && comment.startsWith("= ")) {
            buf.append("  ");
        }

        // finally, add any comment
        if (comment != null) {
            buf.append(comment);
        }

        // make sure the final string is exactly 80 characters long
        if (buf.length() > 80) {
            buf.setLength(80);

        } else {

            if (buf.length() < 80) {
                buf.append(space80.substring(0, 80 - buf.length()));
            }
        }

        return buf.toString();
    }

    private String hierarchToString() {

        StringBuffer b = new StringBuffer(80);
        int p = 0;
        String space = "";
        while (p < key.key().length()) {
            int q = key.key().indexOf('.', p);
            if (q < 0) {
                b.append(space + key.key().substring(p));
                break;
            } else {
                b.append(space + key.key().substring(p, q));
            }
            space = " ";
            p = q + 1;
        }

        if (value != null || nullable) {
            b.append("= ");

            if (value != null) {
                // Try to align values
                int avail = 80 - (b.length() + value.length());

                if (isString) {
                    avail -= 2;
                }
                if (comment != null) {
                    avail -= 3 + comment.length();
                }

                if (avail > 0 && b.length() < 29) {
                    b.append(space80.substring(0, Math.min(avail, 29 - b.length())));
                }

                if (isString) {
                    b.append('\'');
                } else if (avail > 0 && value.length() < 10) {
                    b.append(space80.substring(0, Math.min(avail, 10 - value.length())));
                }
                b.append(value);
                if (isString) {
                    b.append('\'');
                }
            } else if (b.length() < 30) {

                // Pad out a null value
                b.append(space80.substring(0, 30 - b.length()));
            }
        }

        if (comment != null) {
            b.append(" / " + comment);
        }
        if (b.length() < 80) {
            b.append(space80.substring(0, 80 - b.length()));
        }
        String card = new String(b);
        if (card.length() > 80) {
            card = card.substring(0, 80);
        }
        return card;
    }

    /**
     * setter for the value following the builder pattern
     * 
     * @param value
     *            the new value to set
     * @return this header card.
     */
    public HeaderCard value(double value) {
        this.value = dblString(value);
        this.isString = false;
        return this;
    }

    /**
     * setter for the value following the builder pattern
     * 
     * @param value
     *            the new value to set
     * @return this header card.
     */
    public HeaderCard value(boolean value) {
        this.value = value ? FITS_TRUE_STRING : FITS_FALSE_STRING;
        this.isString = false;
        return this;
    }

    /**
     * setter for the value following the builder pattern
     * 
     * @param value
     *            the new value to set
     * @return this header card.
     */
    public HeaderCard value(int value) {
        this.value = String.valueOf(value);
        this.isString = false;
        return this;
    }

    /**
     * setter for the value following the builder pattern
     * 
     * @param value
     *            the new value to set
     * @return this header card.
     */
    public HeaderCard value(long value) {
        this.value = String.valueOf(value);
        this.isString = false;
        return this;
    }

    /**
     * setter for the value following the builder pattern
     * 
     * @param value
     *            the new value to set
     * @return this header card.
     */
    public HeaderCard value(String value) throws HeaderCardException {
        if (value != null) {
            value = value.replaceAll(" *$", "");
            if (value.startsWith("'")) {
                if (value.charAt(value.length() - 1) != '\'') {
                    throw new HeaderCardException("Missing end quote in string value");
                }
                value = value.substring(1, value.length() - 1).trim();
            }
            // if this is a string value the max length is 1 less because of the
            // quotes
            if (value.length() > (MAX_VALUE_LENGTH - (isString ? 2 : 0))) {
                throw new HeaderCardException("Value too long");
            }
        }
        this.value = value;
        return this;
    }

    /**
     * setter for the value following the builder pattern
     * 
     * @param value
     *            the new value to set
     * @return this header card.
     */
    public HeaderCard value(CharSequence value) throws HeaderCardException {
        return value(value == null ? ((String) value) : value.toString());
    }

    public List<HeaderCard> longValue(String val) throws HeaderCardException {
        String comment = this.comment;
        List<HeaderCard> result = new LinkedList<>();
        // We assume that we've made the test so that
        // we need to write a long string. We need to
        // double the quotes in the string value. addValue
        // takes care of that for us, but we need to do it
        // ourselves when we are extending into the comments.
        // We also need to be careful that single quotes don't
        // make the string too long and that we don't split
        // in the middle of a quote.
        int off = getAdjustedLength(val, 67);
        String curr = val.substring(0, off) + '&';
        // No comment here since we're using as much of the card as we can
        result.add(value(curr));
        val = val.substring(off);

        while (val != null && val.length() > 0) {
            off = getAdjustedLength(val, 67);
            if (off < val.length()) {
                curr = "'" + val.substring(0, off).replace("'", "''") + "&'";
                val = val.substring(off);
            } else {
                curr = "'" + val.replace("'", "''") + "' / " + comment;
                val = null;
            }
            result.add(NonStandard.CONTINUE.card().comment(curr));
        }
        return result;
    }

    private int getAdjustedLength(String in, int max) {
        // Find the longest string that we can use when
        // we accommodate needing to double quotes.
        int size = 0;
        int i;
        for (i = 0; i < in.length() && size < max; i += 1) {
            if (in.charAt(i) == '\'') {
                size += 2;
                if (size > max) {
                    break; // Jumped over the edge
                }
            } else {
                size += 1;
            }
        }
        return i;
    }

    /**
     * setter for the comment following the builder pattern
     * 
     * @param comment
     *            the new comment to set
     * @return this header card.
     */
    public HeaderCard comment(String comment) {
        if (comment != null && comment.startsWith("ntf::")) {
            String ckey = comment.substring(5); // Get rid of ntf:: prefix
            this.comment = HeaderCommentsMap.getComment(ckey);
        } else {
            this.comment = comment;
        }
        return this;
    }

    /**
     * setter for the nullable property following the builder pattern
     * 
     * @param nullable
     *            the new nullable value to set
     * @return this header card.
     */
    public HeaderCard nullable(boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    /**
     * change the card key to the n index.
     * 
     * @param number
     *            the number indexes to aply
     * @return
     */
    public HeaderCard n(int... number) {
        key = key.n(number);
        return this;
    }
}
