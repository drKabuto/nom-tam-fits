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

import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;

/**
 * The following keywords are defined to describe the structure of the
 * compressed image.
 * 
 * <pre>
 *  @see <a href="http://www.adass.org/adass/proceedings/adass99/P2-42/">http://www.adass.org/adass/proceedings/adass99/P2-42/</a>
 * </pre>
 * 
 * @author Richard van Nieuwenhoven
 */
public enum Compression implements IFitsHeader {
    /**
     * (required keyword) This keyword must have the logical value T. It
     * indicates that the FITS binary table extension contains a compressed
     * image, and that logically this extension should be interpreted as an
     * image and not as a table.
     */
    ZIMAGE(HDU.IMAGE, VALUE.LOGICAL, "This is a tile compressed image"),

    /**
     * (required keyword) The value shall contain a character string giving the
     * name and version of the algorithm that must be used to decompress the
     * image. Currently, values of GZIP_1, RICE_1, PLIO_1, and HCOMPRESS_1 are
     * reserved to refer to several commonly used algorithms ( PLIO stands for
     * the IRAF Pixel List compression algorithm). We intend to provide a
     * detailed description of how to uncompress each of these formats in the
     * final version of the document.
     */
    ZCMPTYPE(HDU.IMAGE, VALUE.STRING, "The compression algorithm used"),

    /**
     * (required keyword) The value shall contain an integer that gives the
     * value of the BITPIX keyword in the uncompressed FITS image.
     */
    ZBITPIX(HDU.IMAGE, VALUE.INTEGER, "The original bitpix value"),

    /**
     * (required keyword) The value shall contain an integer that gives the
     * value of the NAXIS keyword in the uncompressed FITS image.
     */
    ZNAXIS(HDU.IMAGE, VALUE.INTEGER, "uncompressed value of NAXIS"),

    /**
     * (required keywords) The value shall contain a positive integer that gives
     * the value of the NAXISn keyword in the uncompressed FITS image.
     */
    ZNAXISn(HDU.IMAGE, VALUE.INTEGER, "uncompressed value of NAXISn"),

    /**
     * (optional keywords) The value of these indexed keywords (where n ranges
     * from 1 to ZNAXIS) shall contain a positive integer representing the
     * number of pixels along axis n of the compression tiles. All the pixels
     * within each tile are compressed as a contiguous data array and stored in
     * a row of a variable-length vector column in the binary table. The size of
     * each image dimension (given by ZNAXISn) is not required to be an integer
     * multiple of ZTILEn, and if it is not, then the last tile along that
     * dimension of the image will contain fewer image pixels than the other
     * tiles. If the ZTILEn keywords are not present then the default 'row by
     * row' tiling will be assumed such that ZTILE1 = ZNAXIS1, and the value of
     * all the other ZTILEn keywords equals 1. The compressed image tiles are
     * stored in the binary table in the same order that the first pixel in each
     * tile appears in the FITS image.
     */
    ZTILEn(HDU.IMAGE, VALUE.INTEGER, "pixels along axis n of tiles"),

    /**
     * These pairs of optional array keywords (where n is an integer index
     * number starting with 1) supply the name and value, respectively, of any
     * algorithm-specific parameters that are needed to compress or uncompress
     * the image. The value of ZVALn may have any valid FITS data type. The
     * order of the compression parameters may be significant, and may be
     * defined as part of the description of the specific decompression
     * algorithm.
     */
    ZNAMEn(HDU.IMAGE, VALUE.STRING, "parameter name needed to (un)compress"),
    /**
     * These pairs of optional array keywords (where n is an integer index
     * number starting with 1) supply the name and value, respectively, of any
     * algorithm-specific parameters that are needed to compress or uncompress
     * the image. The value of ZVALn may have any valid FITS data type. The
     * order of the compression parameters may be significant, and may be
     * defined as part of the description of the specific decompression
     * algorithm.
     */
    ZVALn(HDU.IMAGE, VALUE.STRING, "parameter value needed to (un)compress"),

    /**
     * (optional keyword) Used to record the name of the image compression
     * algorithm.
     */
    ZMASKCMP(HDU.IMAGE, VALUE.STRING, "name of the image compression algorithm"),

    /**
     * preserves the original SIMPLE keyword.
     */
    ZSIMPLE(HDU.IMAGE, VALUE.STRING, "original SIMPLE keyword"),
    /**
     * preserves the original XTENSION keyword.
     */
    ZTENSION(HDU.IMAGE, VALUE.STRING, "original XTENSION keyword"),
    /**
     * preserves the original EXTEND keyword.
     */
    ZEXTEND(HDU.IMAGE, VALUE.STRING, "original EXTEND keyword"),
    /**
     * preserves the original BLOCKED keyword.
     */
    ZBLOCKED(HDU.IMAGE, VALUE.STRING, "original BLOCKED keyword"),
    /**
     * preserves the original PCOUNT keyword.
     */
    ZPCOUNT(HDU.IMAGE, VALUE.STRING, "original PCOUNT keyword"),
    /**
     * preserves the original GCOUNT keyword.
     */
    ZGCOUNT(HDU.IMAGE, VALUE.STRING, "original GCOUNT keyword"),
    /**
     * preserves the original CHECKSUM keyword.
     */
    ZHECKSUM(HDU.IMAGE, VALUE.STRING, "original CHECKSUM keyword"),
    /**
     * preserves the original DATASUM keyword.
     */
    ZDATASUM(HDU.IMAGE, VALUE.STRING, "original DATASUM keyword"),
    /**
     * (optional keyword) This keyword records the name of the algorithm that
     * was used to quantize floating-point image pixels into integer values
     * which are then passed to the compression algorithm.
     */
    ZQUANTIZ(HDU.IMAGE, VALUE.STRING, "algorithm used to quantize pixels"),
    /**
     * TODO: find what this is.
     */
    ZVARn(HDU.IMAGE, VALUE.STRING, ""),
    /**
     * TODO: find what this is.
     */
    ZXTENSION(HDU.IMAGE, VALUE.STRING, ""),
    /**
     * TODO: find what this is.
     */
    ZEXTENSION(HDU.IMAGE, VALUE.STRING, ""),
    /**
     * Quantizer scaling.
     */
    ZSCALE(HDU.IMAGE, VALUE.STRING, "Quantizer scaling"),
    /**
     * Quantizer offset value
     */
    ZZERO(HDU.IMAGE, VALUE.STRING, "Quantizer offset value");

    private IFitsHeader key;

    private Compression(HDU hdu, VALUE valueType, String comment) {
        key = new FitsHeaderImpl(name(), IFitsHeader.SOURCE.UNKNOWN, hdu, valueType, comment);
    }

    @Override
    public String comment() {
        return key.comment();
    }

    @Override
    public HDU hdu() {
        return key.hdu();
    }

    @Override
    public String key() {
        return key.key();
    }

    @Override
    public IFitsHeader n(int... number) {
        return key.n(number);
    }

    @Override
    public SOURCE status() {
        return key.status();
    }

    @Override
    public VALUE valueType() {
        return key.valueType();
    }

    @Override
    public HeaderCard card() throws HeaderCardException {
        return new HeaderCard(this);
    }

}
