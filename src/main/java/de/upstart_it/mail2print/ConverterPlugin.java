package de.upstart_it.mail2print;

import java.io.IOException;
import java.io.InputStream;
import org.pf4j.ExtensionPoint;

/**
 *
 * @author Thomas Oster <thomas.oster@upstart-it.de>
 */
public interface ConverterPlugin extends ExtensionPoint {
    String getName();
    boolean canConvertFile(String contentType, String filename, String subject);
    byte[] convertToPdf(InputStream data, String contentType, String filename, String subject) throws IOException;
    void shutdown();
}
