package de.upstart_it.mail2print.converter;

import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author Thomas Oster <thomas.oster@upstart-it.de>
 */
public class ImageConverterNGTest {
    
    ImageConverter instance;
    
    @BeforeClass
    void setUp() {
        instance = new ImageConverter();
    }
    
    @Test
    public void testParseSubject() {
        assertEquals(instance.parseSizeFromSubject("Test 10x15cm asd"),new ImageConverter.Size(10,15));
        assertEquals(instance.parseSizeFromSubject("Test 5.3x6.4cm asdaf"),new ImageConverter.Size(5.3,6.4));
        assertEquals(instance.parseSizeFromSubject("Test 7,2x9,5cm asdads"),new ImageConverter.Size(7.2,9.5));
        assertEquals(instance.parseSizeFromSubject("Test 7,2 x 9,5cm asdfsdf"),new ImageConverter.Size(7.2,9.5));
        assertEquals(instance.parseSizeFromSubject("Test 80 x 100 mm asdfsdf"),new ImageConverter.Size(8,10));
    }

    @Test
    public void testCanConvert() {
        assertTrue(instance.canConvertFile("application/jpg", "test.jpg", "bla bla"));
        assertFalse(instance.canConvertFile("application/pdf", "test.pdf", "bla bla"));
    }
    
}
