package de.upstart_it.mail2print.converter;

import java.io.File;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author Thomas Oster <thomas.oster@upstart-it.de>
 */
public class ImageConverterNGIT {
    
    ImageConverter instance;
    
    @BeforeClass
    void setUp() {
        instance = new ImageConverter();
    }

    @Test
    public void testConvertToPdf() throws Exception {
        InputStream pngstream = this.getClass().getResourceAsStream("Tux.png");
        byte[] result = instance.convertToPdf(pngstream, "image/png", "Tux.png", "10x10cm");
        FileUtils.writeByteArrayToFile(new File("/tmp/test.pdf"), result);
        Runtime.getRuntime().exec("xdg-open /tmp/test.pdf");
    }
    
}
