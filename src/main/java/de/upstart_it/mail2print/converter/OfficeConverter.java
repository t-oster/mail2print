package de.upstart_it.mail2print.converter;

import de.upstart_it.mail2print.ConverterPlugin;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.java.Log;
import org.jodconverter.JodConverter;
import org.jodconverter.document.DefaultDocumentFormatRegistry;
import org.jodconverter.office.LocalOfficeManager;
import org.jodconverter.office.OfficeException;
import org.pf4j.Extension;

/**
 *
 * @author Thomas Oster <thomas.oster@upstart-it.de>
 */
@Log
@Extension
public class OfficeConverter implements ConverterPlugin {

    private LocalOfficeManager officeManager;

    public OfficeConverter() {
        try {
            officeManager = LocalOfficeManager.install();
            officeManager.start();
        } catch (OfficeException ex) {
            Logger.getLogger(OfficeConverter.class.getName()).log(Level.SEVERE, null, ex);
            officeManager = null;
        }
    }

    @Override
    public boolean canConvertFile(String contentType, String filename, String subject) {
        if (officeManager == null) {
            log.info("The office manager could not be initialized, so the office plugin is not working");
            return false;
        }
        return contentType.startsWith("application/vnd.openxmlformats-officedocument");
    }

    @Override
    public byte[] convertToPdf(InputStream data, String contentType, String filename, String subject) throws IOException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            JodConverter
                    .convert(data)
                    .as(DefaultDocumentFormatRegistry.getFormatByMediaType(contentType))
                    .to(buffer, true)
                    .as(DefaultDocumentFormatRegistry.PDF)
                    .execute();
            return buffer.toByteArray();
        } catch (OfficeException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public String getName() {
        return "LibreOffice converter";
    }

    @Override
    public void shutdown() {
        if (officeManager != null && officeManager.isRunning()) {
            try {
                officeManager.stop();
            } catch (OfficeException ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }
    }
}
