package de.upstart_it.mail2print.converter;

import de.upstart_it.mail2print.ConverterPlugin;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import lombok.Data;
import lombok.extern.java.Log;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.pf4j.Extension;

/**
 *
 * @author Thomas Oster <thomas.oster@upstart-it.de>
 */
@Log
@Extension
public class ImageConverter implements ConverterPlugin {

    @Override
    public String getName() {
        return "ImageConverter";
    }

    @Override
    public boolean canConvertFile(String contentType, String filename, String subject) {
        return  filename.endsWith(".png")
                || filename.endsWith(".jpg")
                || contentType.equals("image/jpeg")
                || contentType.equals("image/png");
    }

    @Override
    public void shutdown() { }
    
    @Data
    static class Size {
        final double widthInCm;
        final double heightInCmt;
    }
    
    Pattern pSizeInCm = Pattern.compile(".*?(\\d+[.,]?\\d*) ?x ?(\\d+[.,]?\\d*) ?([cm]m).*?");
    Size parseSizeFromSubject(String subject) {
        try {
            Matcher m = pSizeInCm.matcher(subject);
            if (m.matches()) {
                double factor = ("mm".equals(m.group(3)) ? 0.1 : 1);
                return new Size(
                        Double.parseDouble(m.group(1).replace(",",".")) * factor,
                        Double.parseDouble(m.group(2).replace(",",".")) * factor
                );
            }
        }
        catch (NumberFormatException e) {
            log.warning(e.getLocalizedMessage());
        }
        return null;
    }
    
    private double getFactorToFit(double imgWidth, double imgHeight, double targetWidth, double targetHeight) {
        return Math.min(targetWidth/imgWidth, targetHeight/imgHeight);
    }
    
    @Override
    public byte[] convertToPdf(InputStream data, String contentType, String filename, String subject) throws IOException {
        PDDocument document = new PDDocument();
        PDImageXObject pdfImage = LosslessFactory.createFromImage(document, ImageIO.read(data));
        PDPage page = new PDPage();
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        Size size = parseSizeFromSubject(subject);
        double cm2px = PDRectangle.A4.getHeight()/29.7;
        if (size == null) {
            contentStream.drawImage(pdfImage, (int) (1*cm2px), (int) (1*cm2px));
        }
        else {
            int w = pdfImage.getWidth();
            int h = pdfImage.getHeight();
            double factor = getFactorToFit(w, h, cm2px*size.widthInCm, cm2px*size.heightInCmt);
            contentStream.drawImage(pdfImage, (int) (1*cm2px), (int) (1*cm2px), (int) (w*factor), (int) (h*factor));
        }
        contentStream.close();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        document.close();
        return out.toByteArray();
    }
    
}
