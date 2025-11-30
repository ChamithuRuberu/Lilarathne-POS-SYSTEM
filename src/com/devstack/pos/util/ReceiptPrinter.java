package com.devstack.pos.util;

import javafx.scene.control.Alert;
import org.springframework.stereotype.Component;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;

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
     * For Sinhala text, converts text to image first, then to ESC/POS raster format
     * This ensures proper rendering of Sinhala characters on XP-Q80K printer
     * 
     * @param receiptText Plain text receipt content (may contain Sinhala)
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
            
            // Check if text contains Sinhala characters (Unicode range 0D80-0DFF) or other Unicode
            // Always use image-based approach for any Unicode text (Sinhala, Tamil, etc.)
            // ESC/POS UTF-8 does NOT work reliably on most thermal printers
            boolean containsUnicode = containsSinhalaText(receiptText) || containsUnicodeText(receiptText);
            
            byte[] escposData;
            if (containsUnicode) {
                // Use image-based approach for Unicode text (Sinhala, etc.)
                System.out.println("Detected Unicode text - using image-based printing");
                escposData = generateSinhalaReceiptImage(receiptText);
            } else {
                // Use direct text approach for ASCII-only text
                escposData = formatReceiptAsESCPOS(receiptText);
            }
            
            // Create print job with raw data
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(escposData, flavor, null);
            DocPrintJob job = printer.createPrintJob();
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            
            // Print the raw data
            job.print(doc, attributes);
            
            System.out.println("Receipt sent to thermal printer successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error printing raw text: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Checks if text contains Sinhala characters (Unicode range 0D80-0DFF)
     * @param text Text to check
     * @return true if Sinhala characters are found
     */
    private boolean containsSinhalaText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (char c : text.toCharArray()) {
            if (c >= 0x0D80 && c <= 0x0DFF) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if text contains any Unicode characters beyond ASCII (0-127)
     * @param text Text to check
     * @return true if non-ASCII characters are found
     */
    private boolean containsUnicodeText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (char c : text.toCharArray()) {
            if (c > 127) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a font is available on the system
     * @param fontName Name of the font to check
     * @return true if font is available
     */
    private boolean isFontAvailable(String fontName) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();
        for (String font : availableFonts) {
            if (font.equalsIgnoreCase(fontName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the best available font for Sinhala text
     * @return Font object for Sinhala text, with fallback to system default
     */
    private Font getSinhalaFont() {
        int fontSize = 18; // Reduced from 24 for more professional look
        // Try Noto Sans Sinhala first
        if (isFontAvailable("Noto Sans Sinhala")) {
            return new Font("Noto Sans Sinhala", Font.PLAIN, fontSize);
        }
        // Try other common Sinhala fonts
        if (isFontAvailable("Iskoola Pota")) {
            return new Font("Iskoola Pota", Font.PLAIN, fontSize);
        }
        if (isFontAvailable("Malithi Web")) {
            return new Font("Malithi Web", Font.PLAIN, fontSize);
        }
        // Fallback to default font (may show square boxes if Sinhala not supported)
        System.err.println("Warning: Sinhala font not found. Install 'Noto Sans Sinhala' for proper Sinhala rendering.");
        return new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
    }
    
    /**
     * Generates ESC/POS raster data from Sinhala receipt text
     * Step 1: Convert Sinhala text to image
     * Step 2: Convert image to ESC/POS raster format
     * 
     * @param receiptText Receipt text containing Sinhala characters
     * @return ESC/POS formatted byte array ready for printing
     * @throws Exception if image generation or conversion fails
     */
    public byte[] generateSinhalaReceiptImage(String receiptText) throws Exception {
        int width = 576; // For 80mm paper (XP-Q80K)
        
        // Calculate required height based on text lines
        String[] lines = receiptText.split("\n");
        int lineHeight = 22; // Reduced from 30 for more compact professional look
        int padding = 15; // Reduced from 20
        int height = (lines.length * lineHeight) + (padding * 2);
        
        // Create the actual image using TYPE_INT_RGB for better quality (not TYPE_BYTE_BINARY)
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // Enable anti-aliasing for better text quality
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Fill with white background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        
        // Set black text color
        g.setColor(Color.BLACK);
        
        // Get best available Sinhala font with fallback
        Font sinhalaFont = getSinhalaFont();
        g.setFont(sinhalaFont);
        
        // Draw text line by line with proper spacing
        int y = padding + 18; // Start position with padding (reduced from 25)
        for (String line : lines) {
            if (line != null && !line.trim().isEmpty()) {
                // Remove ESC/POS commands from text for rendering (less aggressive)
                String cleanLine = removeEscPosCommands(line);
                g.drawString(cleanLine, 10, y);
            }
            y += lineHeight;
        }
        
        g.dispose();
        
        // Convert image to ESC/POS raster format
        return convertImageToEscPos(image);
    }
    
    /**
     * Removes ESC/POS command sequences from text for image rendering
     * Less aggressive - only removes actual ESC/POS control sequences, not valid text
     * @param text Text that may contain ESC/POS commands
     * @return Clean text without ESC/POS commands
     */
    private String removeEscPosCommands(String text) {
        if (text == null) {
            return "";
        }
        // Only remove actual ESC/POS control sequences (ESC/GS followed by specific command bytes)
        // Pattern: ESC (0x1B) or GS (0x1D) followed by command character
        // Be more specific to avoid removing valid text like dates or dashes
        String result = text;
        
        // Remove ESC @ (initialize) - ESC 0x40
        result = result.replaceAll("\u001B@", "");
        // Remove ESC ! n (character size) - ESC 0x21 followed by byte
        result = result.replaceAll("\u001B![\\x00-\\xFF]", "");
        // Remove ESC d n (line feed) - ESC 0x64 followed by byte
        result = result.replaceAll("\u001Bd[\\x00-\\xFF]", "");
        // Remove GS V m (cut) - GS 0x56 followed by mode byte
        result = result.replaceAll("\u001DV[\\x00-\\xFF]", "");
        // Remove GS v 0 (raster image) - GS 0x76 followed by mode and parameters
        // This is more complex, so we'll be conservative
        result = result.replaceAll("\u001Dv0[\\x00-\\xFF]{4,}", "");
        
        return result.trim();
    }
    
    /**
     * Converts BufferedImage to ESC/POS raster format
     * Step 2: Convert the image to ESC/POS raster format
     * Uses double density (m=33) for better quality on XP-Q80K
     * 
     * @param image BufferedImage to convert
     * @return ESC/POS formatted byte array
     */
    public byte[] convertImageToEscPos(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // ESC/POS raster image command: GS v 0 (Print raster image)
        // Format: GS v 0 m xL xH yL yH [raster data]
        // m = 33 (double density mode for better quality on XP-Q80K)
        // xL/xH = width in bytes (low/high byte), yL/yH = height in pixels (low/high byte)
        
        int widthBytes = (width + 7) / 8; // Convert pixels to bytes (8 pixels per byte)
        
        baos.write(0x1D); // GS
        baos.write('v');  // v
        baos.write('0');  // 0 = standard raster format
        baos.write(33);   // m = 33 (double density mode for XP-Q80K)
        baos.write(widthBytes & 0xFF);        // xL (low byte of width in bytes)
        baos.write((widthBytes >> 8) & 0xFF); // xH (high byte of width in bytes)
        baos.write(height & 0xFF);            // yL (low byte of height in pixels)
        baos.write((height >> 8) & 0xFF);     // yH (high byte of height in pixels)
        
        // Convert image to raster data (1 bit per pixel)
        // Handle both TYPE_INT_RGB and TYPE_BYTE_BINARY
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x += 8) {
                int b = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int xx = x + bit;
                    if (xx < width) {
                        int pixel = image.getRGB(xx, y);
                        // Convert RGB to grayscale and check if pixel is dark (black)
                        int gray;
                        if (image.getType() == BufferedImage.TYPE_BYTE_BINARY) {
                            // Already binary, just check if it's black
                            gray = (pixel & 0xFF);
                        } else {
                            // TYPE_INT_RGB - convert to grayscale
                            int r = (pixel >> 16) & 0xFF;
                            int g = (pixel >> 8) & 0xFF;
                            int blue = pixel & 0xFF;
                            gray = (r + g + blue) / 3;
                        }
                        // If pixel is dark (gray < 128), set the bit
                        if (gray < 128) {
                            b |= (1 << (7 - bit));
                        }
                    }
                }
                baos.write(b);
            }
        }
        
        // Add feed and cut commands
        baos.write(0x0A); // LF - line feed
        baos.write(0x0A); // LF
        baos.write(0x0A); // LF
        baos.write(0x1B); // ESC
        baos.write(0x64); // d
        baos.write(0x02); // feed 2 lines
        baos.write(0x1D); // GS
        baos.write(0x56); // V
        baos.write(0x41); // A = full cut with feed
        baos.write(0x10); // Feed 16 dots (0x10 = 16)
        
        return baos.toByteArray();
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
            
            // Note: Do NOT use UTF-8 encoding directly - most thermal printers don't support it
            // Convert receipt text to bytes using ISO-8859-1 (Latin-1) for ASCII compatibility
            // This only works for ASCII text (0-255 range)
            byte[] textBytes = receiptText.getBytes(StandardCharsets.ISO_8859_1);
            
            // Send text bytes (ASCII only - Unicode should use image mode)
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
     * Converts PDF to PNG image using PDFBox (Windows support)
     * Pure Java solution - no external dependencies required
     * 
     * @param pdfFilePath Path to the PDF file
     * @param imagePath Output image path
     * @return Path to the generated image file, or null if conversion failed
     */
    private String convertPDFToImageUsingPDFBox(String pdfFilePath, String imagePath) {
        try {
            File pdfFile = new File(pdfFilePath);
            PDDocument document = Loader.loadPDF(pdfFile);
            
            // Render first page only
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            // Render at 203 DPI for thermal printer quality (80mm width = ~576 pixels)
            BufferedImage image = pdfRenderer.renderImageWithDPI(0, 203, ImageType.RGB);
            
            // Resize to 576 pixels width (80mm thermal paper) while maintaining aspect ratio
            int targetWidth = 576;
            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();
            int targetHeight = (int) ((double) originalHeight * targetWidth / originalWidth);
            
            // Create resized image
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(image, 0, 0, targetWidth, targetHeight, null);
            g.dispose();
            
            // Save as PNG
            javax.imageio.ImageIO.write(resizedImage, "PNG", new File(imagePath));
            
            document.close();
            
            System.out.println("PDF converted to image using PDFBox: " + imagePath);
            return imagePath;
            
        } catch (Exception e) {
            System.err.println("Error converting PDF to image using PDFBox: " + e.getMessage());
            e.printStackTrace();
            return null;
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
            } else if (os.contains("win")) {
                // Windows: Use PDFBox (pure Java, no external dependencies)
                try {
                    return convertPDFToImageUsingPDFBox(pdfFilePath, imagePath);
                } catch (Exception e) {
                    System.err.println("PDFBox conversion failed: " + e.getMessage());
                    // Fallback: try Ghostscript if available on Windows
                    command.clear();
                    command.add("gswin64c"); // Ghostscript 64-bit Windows executable
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
                    
                    ProcessBuilder pb = new ProcessBuilder(command);
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    
                    if (exitCode == 0 && new File(imagePath).exists()) {
                        System.out.println("PDF converted to image using Ghostscript (Windows): " + imagePath);
                        return imagePath;
                    }
                    
                    // Try 32-bit Ghostscript
                    command.set(0, "gswin32c");
                    pb = new ProcessBuilder(command);
                    process = pb.start();
                    exitCode = process.waitFor();
                    
                    if (exitCode == 0 && new File(imagePath).exists()) {
                        System.out.println("PDF converted to image using Ghostscript 32-bit (Windows): " + imagePath);
                        return imagePath;
                    }
                }
            }
            
            System.err.println("Failed to convert PDF to image. On Windows, PDFBox is used (included).");
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

