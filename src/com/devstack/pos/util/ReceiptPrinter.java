package com.devstack.pos.util;

import javafx.print.*;
import javafx.scene.control.Alert;
import org.springframework.stereotype.Component;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Utility class for receipt printing and cash drawer operations
 */
@Component
public class ReceiptPrinter {
    
    // ESC/POS command to open cash drawer
    // ESC = 0x1B, p = 0x70, m = 0x00, t1 = 0x19 (25ms), t2 = 0xFA (250ms)
    private static final byte[] OPEN_DRAWER_COMMAND = {0x1B, 0x70, 0x00, 0x19, (byte) 0xFA};
    
    /**
     * Opens the cash drawer by sending ESC/POS command to the default printer
     * This works with most POS thermal printers that have a cash drawer connected
     */
    public void openCashDrawer() {
        try {
            // Find the default print service
            PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
            
            if (defaultPrintService == null) {
                System.err.println("No default printer found. Cash drawer cannot be opened.");
                return;
            }
            
            // Create a Doc with the ESC/POS command
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(OPEN_DRAWER_COMMAND, flavor, null);
            
            // Create print job
            DocPrintJob job = defaultPrintService.createPrintJob();
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            
            // Send the command to open the drawer
            job.print(doc, attributes);
            
            System.out.println("Cash drawer open command sent successfully.");
            
        } catch (PrintException e) {
            System.err.println("Error opening cash drawer: " + e.getMessage());
            e.printStackTrace();
            // Don't show error to user as this is not critical
        }
    }
    
    /**
     * Opens a PDF receipt file with the default PDF viewer
     * This simulates printing by opening the PDF for the user
     * 
     * @param pdfFilePath Path to the PDF receipt file
     */
    public void openReceiptPDF(String pdfFilePath) {
        try {
            File pdfFile = new File(pdfFilePath);
            
            if (!pdfFile.exists()) {
                throw new IOException("PDF file not found: " + pdfFilePath);
            }
            
            // Open the PDF with the default application
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(pdfFile);
                    System.out.println("Receipt PDF opened: " + pdfFilePath);
                } else {
                    System.err.println("Desktop OPEN action not supported");
                }
            } else {
                System.err.println("Desktop is not supported on this platform");
            }
            
        } catch (IOException e) {
            System.err.println("Error opening receipt PDF: " + e.getMessage());
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, 
                "Could not open receipt PDF. Please check: " + pdfFilePath).show();
        }
    }
    
    /**
     * Prints a PDF file to the default printer using Java Print Service
     * 
     * @param pdfFilePath Path to the PDF file to print
     * @return true if printing was successful, false otherwise
     */
    public boolean printPDF(String pdfFilePath) {
        try {
            File pdfFile = new File(pdfFilePath);
            
            if (!pdfFile.exists()) {
                System.err.println("PDF file not found: " + pdfFilePath);
                return false;
            }
            
            // Find the default print service
            PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
            
            if (defaultPrintService == null) {
                System.err.println("No default printer found.");
                // Fall back to opening the PDF
                openReceiptPDF(pdfFilePath);
                return false;
            }
            
            // For actual PDF printing, you would need additional libraries
            // For now, we'll just open the PDF
            System.out.println("Opening PDF for printing: " + pdfFilePath);
            openReceiptPDF(pdfFilePath);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error printing PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Complete bill printing process: prints receipt and opens cash drawer
     * 
     * @param receiptPdfPath Path to the receipt PDF file
     */
    public void printBillAndOpenDrawer(String receiptPdfPath) {
        // Open/print the receipt
        openReceiptPDF(receiptPdfPath);
        
        // Open the cash drawer
        openCashDrawer();
    }
    
    /**
     * Checks if a default printer is available
     * 
     * @return true if a default printer is available, false otherwise
     */
    public boolean isPrinterAvailable() {
        PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
        return defaultPrintService != null;
    }
    
    /**
     * Gets the name of the default printer
     * 
     * @return Name of default printer, or "No printer" if none available
     */
    public String getDefaultPrinterName() {
        PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
        return defaultPrintService != null ? defaultPrintService.getName() : "No printer";
    }
}

