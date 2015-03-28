package nom.tam.fits.header;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final Map<String, Enum<? extends IFitsHeader>> enumMap = new HashMap<>();
    static {
        fitsEnums.add(Standard.class);
        fitsEnums.add(Checksum.class);
        fitsEnums.add(DataDescription.class);
        fitsEnums.add(HierarchicalGrouping.class);
        fitsEnums.add(InstrumentDescription.class);
        fitsEnums.add(NonStandard.class);
        fitsEnums.add(ObservationDescription.class);
        fitsEnums.add(ObservationDurationDescription.class);
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
        return (IFitsHeader) enumMap.get(key);
    }

    /**
     * rebuild the map, based on all available enums.
     */
    private static void rebuildIndex() {
        enumMap.clear();
        for (Class<? extends Enum<? extends IFitsHeader>> fitsEnum : fitsEnums) {
            for (Enum<? extends IFitsHeader> enumValue : fitsEnum.getEnumConstants()) {
                if (!enumMap.containsKey(enumValue.name())) {
                    enumMap.put(enumValue.name(), enumValue);
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
}
