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
import java.nio.charset.StandardCharsets;
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
    
    // ESC/POS command to cut paper (GS V m)
    // GS = 0x1D, V = 0x56, m = 0x00 (full cut) or 0x01 (partial cut)
    private static final byte[] CUT_PAPER_COMMAND = {0x1D, 0x56, 0x00};
    
    // Partial cut command (most compatible with XPrinter XP-Q80K)
    // GS V 1 = partial cut (leaves small connection for easy tear)
    private static final byte[] PARTIAL_CUT_COMMAND = {0x1D, 0x56, 0x01};
    
    // Feed paper before cutting (LF = line feed, ESC d n = feed n lines)
    private static final byte[] FEED_AND_CUT_COMMAND = {
        0x1B, 0x64, 0x05,  // ESC d 5 = feed 5 lines
        0x1D, 0x56, 0x01   // GS V 1 = partial cut
    };
    
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
     * Prints directly to thermal printer using raw ESC/POS commands
     * This is the proper way to print to thermal printers - no PDF/image conversion needed
     * 
     * @param receiptText Plain text receipt content
     * @return true if printing was successful, false otherwise
     */
    public boolean printRawText(String receiptText) {
        try {
            // Find XPrinter
            PrintService printer = findXPrinterService();
            if (printer == null) {
                printer = PrintServiceLookup.lookupDefaultPrintService();
            }
            
            if (printer == null) {
                System.err.println("No printer found.");
                return false;
            }
            
            System.out.println("Printing to: " + printer.getName());
            
            // Prepare ESC/POS formatted data
            byte[] escposData = formatReceiptAsESCPOS(receiptText);
            
            // Create print job with raw data
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(escposData, flavor, null);
            DocPrintJob job = printer.createPrintJob();
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            
            // Print the raw data
            job.print(doc, attributes);
            
            System.out.println("Raw text sent to thermal printer successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error printing raw text: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Formats receipt text with ESC/POS commands for thermal printer
     * 
     * @param receiptText Plain text receipt content
     * @return ESC/POS formatted byte array
     */
    private byte[] formatReceiptAsESCPOS(String receiptText) {
        try {
            List<Byte> data = new ArrayList<>();
            
            // ESC/POS initialization
            data.add((byte) 0x1B); // ESC
            data.add((byte) 0x40); // @  - Initialize printer
            
            // Set character code table to PC437 (USA, Standard Europe)
            data.add((byte) 0x1B); // ESC
            data.add((byte) 0x74); // t
            data.add((byte) 0x00); // 0 = PC437
            
            // Convert receipt text to bytes (using US-ASCII for thermal printer compatibility)
            byte[] textBytes = receiptText.getBytes(StandardCharsets.US_ASCII);
            for (byte b : textBytes) {
                data.add(b);
            }
            
            // Feed and cut
            data.add((byte) 0x0A); // LF - line feed
            data.add((byte) 0x0A); // LF
            data.add((byte) 0x0A); // LF
            data.add((byte) 0x1B); // ESC
            data.add((byte) 0x64); // d
            data.add((byte) 0x02); // feed 2 lines
            data.add((byte) 0x1D); // GS
            data.add((byte) 0x56); // V
            data.add((byte) 0x01); // partial cut
            
            // Convert List<Byte> to byte[]
            byte[] result = new byte[data.size()];
            for (int i = 0; i < data.size(); i++) {
                result[i] = data.get(i);
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error formatting ESC/POS data: " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }
    
    /**
     * Prints a PDF file to the default printer by converting to image first
     * Thermal printers handle images much better than PDFs
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
            
            // Convert PDF to image first (thermal printers handle images better)
            String imagePath = convertPDFToImage(pdfFilePath);
            if (imagePath != null && new File(imagePath).exists()) {
                // Print the image instead of PDF
                boolean printed = printImage(imagePath);
                // Clean up temporary image file
                try {
                    new File(imagePath).delete();
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
                return printed;
            }
            
            // Fallback: try printing PDF directly if image conversion failed
            System.err.println("PDF to image conversion failed, trying direct PDF print...");
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("mac") || os.contains("linux")) {
                if (printPDFUsingCommandLine(pdfFilePath)) {
                    return true;
                }
            }
            
            // Final fallback to Desktop API
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.PRINT)) {
                    desktop.print(pdfFile);
                    System.out.println("PDF sent to printer via Desktop API: " + pdfFilePath);
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("Error printing PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Converts PDF to PNG image for better thermal printer compatibility
     * 
     * @param pdfFilePath Path to the PDF file
     * @return Path to the generated image file, or null if conversion failed
     */
    private String convertPDFToImage(String pdfFilePath) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String imagePath = pdfFilePath.replace(".pdf", "_print.png");
            List<String> command = new ArrayList<>();
            
            if (os.contains("mac")) {
                // macOS: Try multiple methods
                // Method 1: Use qlmanage (built-in, works with PDFs)
                // For 80mm thermal printer, use width of ~576 pixels (at 72 DPI)
                command.add("qlmanage");
                command.add("-t");
                command.add("-s");
                command.add("576"); // 80mm at 72 DPI = ~576 pixels width
                command.add("-o");
                command.add(new File(pdfFilePath).getParent()); // output directory
                command.add(pdfFilePath);
                
                ProcessBuilder pb = new ProcessBuilder(command);
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    // qlmanage creates file with .png extension in the same directory
                    String qlOutputPath = pdfFilePath.replace(".pdf", ".png");
                    File qlOutputFile = new File(qlOutputPath);
                    if (qlOutputFile.exists()) {
                        // Rename to our desired path
                        qlOutputFile.renameTo(new File(imagePath));
                        System.out.println("PDF converted to image using qlmanage: " + imagePath);
                        return imagePath;
                    }
                }
                
                // Method 2: Try pdftoppm if available (from poppler-utils via Homebrew)
                command.clear();
                command.add("pdftoppm");
                command.add("-png");
                command.add("-singlefile");
                command.add("-r");
                command.add("203"); // 203 DPI for thermal printers (80mm = ~640 pixels)
                command.add("-scale-to-x");
                command.add("576"); // 80mm width
                command.add("-scale-to-y");
                command.add("-1"); // maintain aspect ratio
                command.add(pdfFilePath);
                command.add(imagePath.replace(".png", ""));
                
                pb = new ProcessBuilder(command);
                process = pb.start();
                exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    File generatedFile = new File(imagePath.replace(".png", "-1.png"));
                    if (generatedFile.exists()) {
                        generatedFile.renameTo(new File(imagePath));
                        System.out.println("PDF converted to image using pdftoppm: " + imagePath);
                        return imagePath;
                    }
                }
                
                // Method 3: Try Ghostscript if available
                command.clear();
                command.add("gs");
                command.add("-dNOPAUSE");
                command.add("-dBATCH");
                command.add("-sDEVICE=png16m");
                command.add("-r203"); // 203 DPI for thermal printers
                command.add("-dFirstPage=1");
                command.add("-dLastPage=1");
                command.add("-dFIXEDMEDIA");
                command.add("-dDEVICEWIDTHPOINTS=576"); // 80mm = 576 pixels at 203 DPI
                command.add("-sOutputFile=" + imagePath);
                command.add(pdfFilePath);
                
                pb = new ProcessBuilder(command);
                process = pb.start();
                exitCode = process.waitFor();
                
                if (exitCode == 0 && new File(imagePath).exists()) {
                    System.out.println("PDF converted to image using Ghostscript: " + imagePath);
                    return imagePath;
                }
                
            } else if (os.contains("linux")) {
                // Linux: Use convert (ImageMagick) or pdftoppm
                command.add("pdftoppm");
                command.add("-png");
                command.add("-singlefile");
                command.add("-r");
                command.add("300");
                command.add(pdfFilePath);
                command.add(imagePath.replace(".png", ""));
                
                ProcessBuilder pb = new ProcessBuilder(command);
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    File generatedFile = new File(imagePath.replace(".png", "-1.png"));
                    if (generatedFile.exists()) {
                        generatedFile.renameTo(new File(imagePath));
                        System.out.println("PDF converted to image: " + imagePath);
                        return imagePath;
                    }
                }
                
                // Fallback: try convert (ImageMagick)
                command.clear();
                command.add("convert");
                command.add("-density");
                command.add("300");
                command.add("-quality");
                command.add("100");
                command.add(pdfFilePath + "[0]"); // First page only
                command.add(imagePath);
                
                pb = new ProcessBuilder(command);
                process = pb.start();
                exitCode = process.waitFor();
                
                if (exitCode == 0 && new File(imagePath).exists()) {
                    System.out.println("PDF converted to image using ImageMagick: " + imagePath);
                    return imagePath;
                }
            }
            
            System.err.println("Failed to convert PDF to image. Make sure pdftoppm, gs, or convert is installed.");
            return null;
            
        } catch (Exception e) {
            System.err.println("Error converting PDF to image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Prints an image file to the printer
     * 
     * @param imagePath Path to the image file
     * @return true if printing was successful, false otherwise
     */
    private boolean printImage(String imagePath) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            List<String> command = new ArrayList<>();
            
            // Try to find XPrinter first
            String printerName = findXPrinterName();
            
            if (os.contains("mac")) {
                command.add("lp");
                
                if (printerName != null && !printerName.isEmpty()) {
                    command.add("-d");
                    command.add(printerName);
                    System.out.println("Printing image to XPrinter: " + printerName);
                }
                
                // Options for 80mm thermal printer (XPrinter XP-Q80K)
                command.add("-o");
                command.add("media=Custom.80x297mm"); // 80mm width thermal paper
                command.add("-o");
                command.add("fit-to-page"); // Scale to fit
                command.add("-o");
                command.add("scaling=100"); // No scaling
                command.add("-o");
                command.add("print-quality=high"); // High quality for better text
                command.add(imagePath);
                
            } else if (os.contains("linux")) {
                command.add("lp");
                
                if (printerName != null && !printerName.isEmpty()) {
                    command.add("-d");
                    command.add(printerName);
                    System.out.println("Printing image to XPrinter: " + printerName);
                }
                
                command.add(imagePath);
            } else {
                return false;
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("Image sent to printer successfully: " + imagePath);
                return true;
            } else {
                System.err.println("Image printing failed with exit code: " + exitCode);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error printing image: " + e.getMessage());
            e.printStackTrace();
            return false;
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
                
                // Add options for thermal printer
                command.add("-o"); // Options
                command.add("media=Custom.80x297mm"); // Thermal printer size (optional)
                command.add("-o");
                command.add("fit-to-page"); // Fit to page
                // Add encoding option to ensure proper text rendering
                command.add("-o");
                command.add("print-quality=normal");
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
     * Sends cut/tear command to thermal printer
     * This cuts the paper after printing the receipt
     */
    public void cutPaper() {
        try {
            // Find the default print service or XPrinter
            PrintService printer = findXPrinterService();
            
            if (printer == null) {
                printer = PrintServiceLookup.lookupDefaultPrintService();
            }
            
            if (printer == null) {
                System.err.println("No printer found. Cannot send cut command.");
                return;
            }
            
            // Create a Doc with the ESC/POS cut command
            // Use FEED_AND_CUT_COMMAND for XPrinter XP-Q80K
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(FEED_AND_CUT_COMMAND, flavor, null);
            
            // Create print job
            DocPrintJob job = printer.createPrintJob();
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            
            // Send the cut command
            job.print(doc, attributes);
            
            System.out.println("Cut command sent to printer: " + printer.getName());
            
        } catch (PrintException e) {
            System.err.println("Error sending cut command: " + e.getMessage());
            e.printStackTrace();
            // Don't show error to user as this is not critical
        }
    }
    
    /**
     * Finds XPrinter service from available printers
     * 
     * @return XPrinter service if found, null otherwise
     */
    private PrintService findXPrinterService() {
        try {
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            
            for (PrintService service : printServices) {
                String printerName = service.getName().toLowerCase();
                if (printerName.contains("xprinter") || printerName.contains("xp-") || 
                    printerName.contains("xp ")) {
                    return service;
                }
            }
        } catch (Exception e) {
            System.err.println("Error finding XPrinter service: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Complete bill printing process: prints receipt automatically and cuts paper
     * Cash drawer functionality is disabled as per user requirement
     * 
     * @param receiptPdfPath Path to the receipt PDF file
     */
    public void printBillAndOpenDrawer(String receiptPdfPath) {
        // Print the receipt automatically to default printer (XPrinter)
        boolean printed = printPDF(receiptPdfPath);
        
        if (printed) {
            // Wait for printing to complete, then send cut command
            try {
                Thread.sleep(2000); // Wait 2 seconds for print job to complete
                cutPaper(); // Send cut command to tear the receipt
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
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

