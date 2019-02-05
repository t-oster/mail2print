package de.upstart_it.mail2print;

import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.PrintException;
import javax.print.PrintService;
import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

public class PrintHelper {
    
    @Getter
    @Setter
    private Scaling scaling = Scaling.SCALE_TO_FIT;
    
    public Scaling[] getAvailableScalings() {
        return Scaling.values();
    }
    
    public PrintService getPrinter(String cName)
    {
        for (PrintService s : PrinterJob.lookupPrintServices()) {
            if (s.getName().equals(cName))
            {
                return s;
            }
        }
        return null;
    }
    
    private List<String> _availablePrinters;
    public synchronized List<String> getAvailablePrinters() {
        if (_availablePrinters == null) {
            _availablePrinters = new LinkedList<>();
            for (PrintService s : PrinterJob.lookupPrintServices()) {
                _availablePrinters.add(s.getName());
            }
        }
        return _availablePrinters;
    }
    
    public void printPDF(PrintService printService, byte[] byteStream, Integer forceWidth, Integer forceHeight) throws PrintException {
        try (PDDocument doc = PDDocument.load(byteStream)) {
            PrinterJob job = PrinterJob.getPrinterJob();    
            job.setPrintService(printService);
            if (forceWidth != null && forceHeight != null) {
                Paper paper = new Paper();
                double cm2unit = 72.0/2.54;// 1/72 inch
                paper.setSize(forceWidth *cm2unit, forceHeight * cm2unit); 
                paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight()); // no margins
                PageFormat pageFormat = new PageFormat();
                pageFormat.setPaper(paper);
                Book book = new Book();
                book.append(new PDFPrintable(doc, scaling), pageFormat, doc.getNumberOfPages());
                job.setPageable(book);
            }
            else {
                job.setPrintable(new PDFPrintable(doc, scaling));
            }
            job.print();
        } catch (IOException | PrinterException ex) {
            Logger.getLogger(PrintHelper.class.getName()).log(Level.SEVERE, null, ex);
            throw new PrintException(ex);
        }
    }
}
