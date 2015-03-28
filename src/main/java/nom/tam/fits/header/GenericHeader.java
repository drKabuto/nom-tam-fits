package nom.tam.fits.header;

import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;

/**
 * generic fits header key. Please use with care only for cases where the key is
 * not available in one of the enums.
 * 
 * @author Richard van Nieuwenhoven
 *
 */
public class GenericHeader implements IFitsHeader {

    /**
     * key for the header card.
     */
    private final String key;

    /**
     * where can this card be used
     */
    private final HDU hdu;

    /**
     * source for the header
     */
    private final SOURCE status;

    /**
     * value type of the header.
     */
    private final VALUE valueType;

    public GenericHeader(String key, HDU hdu, SOURCE status, VALUE valueType) {
        this.key = key;
        this.hdu = hdu;
        this.status = status;
        this.valueType = valueType;
    }

    public GenericHeader(String key, HDU hdu, VALUE valueType) {
        this(key, hdu, SOURCE.UNKNOWN, valueType);
    }

    public GenericHeader(String key, VALUE valueType) {
        this(key, HDU.ANY, SOURCE.UNKNOWN, valueType);
    }

    public GenericHeader(String key) {
        this(key, HDU.ANY, SOURCE.UNKNOWN, VALUE.STRING);
    }

    @Override
    public String comment() {
        return "";
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
    public IFitsHeader n(int... number) {
        return null;
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
