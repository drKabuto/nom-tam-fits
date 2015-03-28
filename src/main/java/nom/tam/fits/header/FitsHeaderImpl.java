package nom.tam.fits.header;

import java.util.HashMap;
import java.util.Map;

import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;

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

public class FitsHeaderImpl implements IFitsHeader {

    private final String comment;

    private final HDU hdu;

    private final String key;

    private final SOURCE status;

    private final VALUE valueType;

    private final Map<String, FitsHeaderImpl> ns;

    public FitsHeaderImpl(String headerName, SOURCE status, HDU hdu, VALUE valueType, String comment) {
        key = headerName;
        this.status = status;
        this.hdu = hdu;
        this.valueType = valueType;
        this.comment = comment;
        if (key.indexOf('n') >= 0) {
            ns = new HashMap<>();
        } else {
            ns = null;
        }
    }

    @Override
    public String comment() {
        return comment;
    }

    @Override
    public HDU hdu() {
        return hdu;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public IFitsHeader n(int... numbers) {
        StringBuffer headerName = new StringBuffer(key);
        for (int number : numbers) {
            int indexOfN = headerName.indexOf("n");
            headerName.replace(indexOfN, indexOfN + 1, Integer.toString(number));
        }
        String newKey = headerName.toString();
        FitsHeaderImpl found = ns.get(newKey);
        if (found == null) {
            found = creatNewN(newKey);
        }
        return found;
    }

    /**
     * double check the map because of the concurenry.
     * 
     * @param newKey
     *            the new indexed key to use.
     * @return the found or created element.
     */
    private synchronized FitsHeaderImpl creatNewN(String newKey) {
        FitsHeaderImpl found = ns.get(newKey);
        if (found == null) {
            found = new FitsHeaderImpl(newKey, status, hdu, valueType, comment);
            ns.put(newKey, found);
        }
        return found;
    }

    @Override
    public SOURCE status() {
        return status;
    }

    @Override
    public VALUE valueType() {
        return valueType;
    }

    @Override
    public HeaderCard card() throws HeaderCardException {
        return new HeaderCard(this);
    }
}
