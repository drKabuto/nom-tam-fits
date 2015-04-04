package nom.tam.fits.header;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nom.tam.fits.header.IFitsHeader.HDU;
import nom.tam.fits.header.IFitsHeader.SOURCE;
import nom.tam.fits.header.IFitsHeader.VALUE;
import nom.tam.fits.header.extra.CXCExt;
import nom.tam.fits.header.extra.CXCStclSharedExt;
import nom.tam.fits.header.extra.MaxImDLExt;
import nom.tam.fits.header.extra.NOAOExt;
import nom.tam.fits.header.extra.SBFitsExt;
import nom.tam.fits.header.extra.STScIExt;

/**
 * utility class to find a an existing header in one of the header collections
 * enums.
 * 
 * @author Richard van Nieuwenhoven
 *
 */
public final class FitsHeaderIndex {

    /**
     * list of all registered fits enums.
     */
    private static final List<Class<? extends Enum<? extends IFitsHeader>>> fitsEnums = new ArrayList<>();

    /**
     * lazy build map of all available enums.
     */
    private static final Map<String, IFitsHeader> enumMap = new HashMap<>();
    static {
        fitsEnums.add(Standard.class);
        fitsEnums.add(Checksum.class);
        fitsEnums.add(DataDescription.class);
        fitsEnums.add(HierarchicalGrouping.class);
        fitsEnums.add(InstrumentDescription.class);
        fitsEnums.add(NonStandard.class);
        fitsEnums.add(ObservationDescription.class);
        fitsEnums.add(ObservationDurationDescription.class);
        fitsEnums.add(Compression.class);
        fitsEnums.add(CXCExt.class);
        fitsEnums.add(CXCStclSharedExt.class);
        fitsEnums.add(MaxImDLExt.class);
        fitsEnums.add(NOAOExt.class);
        fitsEnums.add(SBFitsExt.class);
        fitsEnums.add(STScIExt.class);
        rebuildIndex();
    }

    /**
     * private constructor for utility class.
     */
    private FitsHeaderIndex() {
    }

    /**
     * find a enum for a key
     * 
     * @param key
     *            the key to find
     * @return the found enum value or null if none was found.
     */
    public static IFitsHeader find(String key) {
        IFitsHeader iFitsHeader = (IFitsHeader) enumMap.get(key);
        if (iFitsHeader == null) {
            StringBuffer indexedKey = new StringBuffer();
            int[] values = null;
            int valueIndex = 0;
            for (int index = 0; index < key.length(); index++) {
                char character = key.charAt(index);
                if (Character.isDigit(character)) {
                    indexedKey.append('n');
                    if (values == null) {
                        values = new int[8];
                    }
                    values[valueIndex++] = Integer.parseInt(Character.toString(character));
                } else {
                    indexedKey.append(character);
                }
            }
            iFitsHeader = (IFitsHeader) enumMap.get(indexedKey.toString());
            if (iFitsHeader != null) {
                return iFitsHeader.n(Arrays.copyOf(values, valueIndex));
            }

        }

        return iFitsHeader;
    }

    /**
     * rebuild the map, based on all available enums.
     */
    private static void rebuildIndex() {
        enumMap.clear();
        for (Class<? extends Enum<? extends IFitsHeader>> fitsEnum : fitsEnums) {
            for (Enum<? extends IFitsHeader> enumValue : fitsEnum.getEnumConstants()) {
                if (!enumMap.containsKey(enumValue.name())) {
                    enumMap.put(enumValue.name(), (IFitsHeader) enumValue);
                }
            }
        }
    }

    /**
     * add some private collection of fits enums.
     * 
     * @param privateEnum
     *            enum list.
     */
    public static void add(Class<? extends Enum<? extends IFitsHeader>> privateEnum) {
        fitsEnums.add(privateEnum);
        rebuildIndex();
    }

    public static IFitsHeader findOrCreateKey(String keyString) {
        IFitsHeader potentialKey = find(keyString);
        if (potentialKey == null) {
            potentialKey = createNewKey(keyString);
        }
        return potentialKey;
    }

    /**
     * we search the map again because of the synchronized access.
     * 
     * @param keyString
     * @return
     */
    private static synchronized IFitsHeader createNewKey(String keyString) {
        IFitsHeader potentialKey = find(keyString);
        if (potentialKey == null) {
            potentialKey = new FitsHeaderImpl(FitsHeaderImpl.class, keyString, SOURCE.UNKNOWN, HDU.ANY, VALUE.STRING, "");
            enumMap.put(potentialKey.key(), potentialKey);
        }
        return potentialKey;
    }
}
