/*
 * This class provides a modifiable map in which the comment fields for FITS
 * header keywords
 * produced by this library are set. The map is a simple String -> String
 * map where the key Strings are normally class:keyword:id where class is
 * the class name where the keyword is set, keyword is the keyword set and id
 * is an integer used to distinguish multiple instances.
 * 
 * Most users need not worry about this class, but users who wish to customize
 * the appearance of FITS files may update the map. The code itself is likely
 * to be needed to understand which values in the map must be modified.
 * 
 * Note that the Header writing utilities look for the property files see
 * NOM_TAM_FITS_COMMENT_PROPERTIES.
 */

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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import nom.tam.fits.header.IFitsHeader;

public class HeaderCommentsMap {

    /**
     * all property files in the current class loader are read and overwrite the
     * default comments of the fits keywords.
     */
    private static final String NOM_TAM_FITS_COMMENT_PROPERTIES = "nom-tam-fits-comment.properties";

    private static Properties commentMap = new Properties();
    static {
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(NOM_TAM_FITS_COMMENT_PROPERTIES);
            while (resources.hasMoreElements()) {
                URL url = (URL) resources.nextElement();
                try {
                    InputStream openStream = url.openStream();
                    commentMap.load(openStream);
                    openStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String commentFor(IFitsHeader header) {
        String propertyKey = header.definingClass().getName() + "." + header.key();
        String comment = (String) commentMap.get(propertyKey);
        if (comment == null) {
            comment = header.comment();
        }
        if (comment != null) {
            comment = comment.replace("{datetime}", FitsDate.getFitsDateString());
        }
        if (comment != null) {
            comment = comment.trim();
        }
        return comment;
    }
}
