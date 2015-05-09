package nom.tam.fits.test;

import nom.tam.fits.*;
import nom.tam.util.*;

import static nom.tam.fits.header.Standard.*;

public class TestNew {

    public static void main(String[] args) throws Exception {

        double[][] data = new double[5][5];

        Fits f = new Fits();

        BasicHDU hdu = FitsFactory.HDUFactory(data);
        Header hdr = hdu.getHeader();

        hdr.card(CTYPEn.n(1)).value("RA---TAN").comment("Tangent plane projection");
        hdr.card(CTYPEn.n(2)).value("DEC--TAN").comment("Tangent plane projection");
        hdr.card(CDELTn.n(1)).value(-1.).comment("Pixel size: X");
        hdr.card(CDELTn.n(2)).value(1.).comment("Pixel size: Y");
        hdr.card(CRPIXn.n(1)).value(data[0].length / 2.).comment("Reference pixel: x");
        hdr.card(CRPIXn.n(2)).value(data.length / 2.).comment("Reference pixel: y");
        hdr.card(CRVALn.n(1)).value(10.2).comment("Reference RA");
        hdr.card(CRVALn.n(2)).value(8.3).comment("Reference Declination");

        f.addHDU(hdu);

        BufferedFile bf = new BufferedFile("save.file", "rw");
        f.write(bf);

    }
}
