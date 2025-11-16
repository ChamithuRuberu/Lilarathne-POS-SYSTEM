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
import java.util.ArrayList;
import java.util.List;

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
     * Prints a PDF file to the default printer using Java Desktop API
     * This will automatically print to the default printer (e.g., XPrinter)
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
            
            // Try command-line printing first (works best for automatic printing on macOS/Linux)
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("mac") || os.contains("linux")) {
                if (printPDFUsingCommandLine(pdfFilePath)) {
                    return true;
                }
            }
            
            // Fallback to Desktop API
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.PRINT)) {
                    desktop.print(pdfFile);
                    System.out.println("PDF sent to printer via Desktop API: " + pdfFilePath);
                    return true;
                } else {
                    System.err.println("Desktop PRINT action not supported. Trying alternative method...");
                    // Fallback: try to find and use default printer
                    return printPDFUsingPrintService(pdfFilePath);
                }
            } else {
                System.err.println("Desktop is not supported. Trying alternative method...");
                return printPDFUsingPrintService(pdfFilePath);
            }
            
        } catch (Exception e) {
            System.err.println("Error printing PDF: " + e.getMessage());
            e.printStackTrace();
            // Try alternative method as fallback
            return printPDFUsingPrintService(pdfFilePath);
        }
    }
    
    /**
     * Prints PDF using command-line tools (lp on macOS/Linux)
     * This provides true automatic printing without dialogs
     * 
     * @param pdfFilePath Path to the PDF file to print
     * @return true if printing was successful, false otherwise
     */
    private boolean printPDFUsingCommandLine(String pdfFilePath) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            List<String> command = new ArrayList<>();
            
            // Try to find XPrinter first
            String printerName = findXPrinterName();
            
            if (os.contains("mac")) {
                // macOS: use lp command
                command.add("lp");
                
                // If XPrinter found, use it; otherwise use default
                if (printerName != null && !printerName.isEmpty()) {
                    command.add("-d"); // Destination printer
                    command.add(printerName);
                    System.out.println("Using XPrinter: " + printerName);
                }
                
                command.add("-o"); // Options
                command.add("media=Custom.80x297mm"); // Thermal printer size (optional)
                command.add("-o");
                command.add("fit-to-page"); // Fit to page
                command.add(pdfFilePath);
            } else if (os.contains("linux")) {
                // Linux: use lp command
                command.add("lp");
                
                // If XPrinter found, use it; otherwise use default
                if (printerName != null && !printerName.isEmpty()) {
                    command.add("-d"); // Destination printer
                    command.add(printerName);
                    System.out.println("Using XPrinter: " + printerName);
                }
                
                command.add(pdfFilePath);
            } else {
                return false; // Not macOS or Linux
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("PDF sent to printer via command line: " + pdfFilePath);
                return true;
            } else {
                System.err.println("Command-line printing failed with exit code: " + exitCode);
                // Try without printer specification (use default)
                if (printerName != null && !printerName.isEmpty()) {
                    System.out.println("Retrying with default printer...");
                    return printPDFUsingCommandLineDefault(pdfFilePath, os);
                }
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error printing PDF via command line: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Fallback method to print using default printer via command line
     */
    private boolean printPDFUsingCommandLineDefault(String pdfFilePath, String os) {
        try {
            List<String> command = new ArrayList<>();
            command.add("lp");
            if (os.contains("mac")) {
                command.add("-o");
                command.add("media=Custom.80x297mm");
                command.add("-o");
                command.add("fit-to-page");
            }
            command.add(pdfFilePath);
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Finds XPrinter name from available printers
     * 
     * @return XPrinter name if found, null otherwise
     */
    private String findXPrinterName() {
        try {
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            
            for (PrintService service : printServices) {
                String printerName = service.getName().toLowerCase();
                if (printerName.contains("xprinter") || printerName.contains("xp-") || 
                    printerName.contains("xp ")) {
                    return service.getName();
                }
            }
            
            // If not found, return default printer name
            PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
            if (defaultPrinter != null) {
                return defaultPrinter.getName();
            }
            
        } catch (Exception e) {
            System.err.println("Error finding XPrinter: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Alternative method to print PDF using Java Print Service
     * This is a fallback if Desktop.print() doesn't work
     */
    private boolean printPDFUsingPrintService(String pdfFilePath) {
        try {
            File pdfFile = new File(pdfFilePath);
            
            // Find all available printers
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            
            if (printServices.length == 0) {
                System.err.println("No printers found.");
                return false;
            }
            
            // Try to find XPrinter or use default
            PrintService selectedPrinter = null;
            for (PrintService service : printServices) {
                String printerName = service.getName().toLowerCase();
                if (printerName.contains("xprinter") || printerName.contains("xp-")) {
                    selectedPrinter = service;
                    System.out.println("Found XPrinter: " + service.getName());
                    break;
                }
            }
            
            // If XPrinter not found, use default printer
            if (selectedPrinter == null) {
                selectedPrinter = PrintServiceLookup.lookupDefaultPrintService();
                if (selectedPrinter == null) {
                    selectedPrinter = printServices[0]; // Use first available printer
                }
                System.out.println("Using printer: " + selectedPrinter.getName());
            }
            
            // Read PDF file and create print job
            try (FileInputStream fis = new FileInputStream(pdfFile)) {
                DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
                Doc doc = new SimpleDoc(fis, flavor, null);
                DocPrintJob job = selectedPrinter.createPrintJob();
                PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
                attributes.add(new Copies(1));
                
                job.print(doc, attributes);
                System.out.println("PDF sent to printer successfully: " + selectedPrinter.getName());
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("Error printing PDF using PrintService: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Complete bill printing process: prints receipt automatically
     * Cash drawer functionality is disabled as per user requirement
     * 
     * @param receiptPdfPath Path to the receipt PDF file
     */
    public void printBillAndOpenDrawer(String receiptPdfPath) {
        // Print the receipt automatically to default printer (XPrinter)
        boolean printed = printPDF(receiptPdfPath);
        
        if (!printed) {
            System.err.println("Failed to print automatically. Opening PDF as fallback.");
            openReceiptPDF(receiptPdfPath);
        }
        
        // Cash drawer opening is disabled - user requested to ignore it
        // openCashDrawer();
    }
    
    /**
     * Prints bill automatically without opening cash drawer
     * 
     * @param receiptPdfPath Path to the receipt PDF file
     */
    public void printBill(String receiptPdfPath) {
        printBillAndOpenDrawer(receiptPdfPath);
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

