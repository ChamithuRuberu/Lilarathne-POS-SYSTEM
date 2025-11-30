package com.devstack.pos.util;

import com.devstack.pos.service.SuperAdminPDFReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for printing super admin receipts
 * Uses separate methods from existing receipt printer
 */
@Component
@RequiredArgsConstructor
public class SuperAdminReceiptPrinter {
    
    private final SuperAdminPDFReportService superAdminPDFReportService;
    
    /**
     * Print super admin receipt as plain text to thermal printer
     * @param orderId Super admin order ID
     * @return true if printing was successful, false otherwise
     */
    public boolean printSuperAdminReceipt(Long orderId) {
        try {
            // Generate plain text receipt
            String receiptText = superAdminPDFReportService.generateSuperAdminPlainTextReceipt(orderId);
            
            // Print using raw text method with ESC/POS commands
            return printRawText(receiptText);
            
        } catch (Exception e) {
            System.err.println("Error printing super admin receipt: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Print raw text to default printer (thermal printer) using ESC/POS commands
     * For Sinhala text, converts text to image first, then to ESC/POS raster format
     * This ensures proper rendering of Sinhala characters on XP-Q80K printer
     * @param text Text to print (may contain Sinhala)
     * @return true if printing was successful, false otherwise
     */
    private boolean printRawText(String text) {
        try {
            // Find default printer
            PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
            
            if (printer == null) {
                System.err.println("No printer found for super admin receipt.");
                return false;
            }
            
            System.out.println("Printing Super Admin Receipt to: " + printer.getName());
            
            // Check if text contains Sinhala characters (Unicode range 0D80-0DFF) or other Unicode
            // Always use image-based approach for any Unicode text (Sinhala, Tamil, etc.)
            // ESC/POS UTF-8 does NOT work reliably on most thermal printers
            boolean containsUnicode = containsSinhalaText(text) || containsUnicodeText(text);
            
            byte[] escposData;
            if (containsUnicode) {
                // Use image-based approach for Unicode text (Sinhala, etc.)
                System.out.println("Detected Unicode text - using image-based printing");
                escposData = generateSinhalaReceiptImage(text);
            } else {
                // Use direct text approach for ASCII-only text
                escposData = formatReceiptAsESCPOS(text);
            }
            
            // Create print job with raw data
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(escposData, flavor, null);
            DocPrintJob job = printer.createPrintJob();
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            
            // Print the raw data
            job.print(doc, attributes);
            
            System.out.println("Super Admin Receipt sent to thermal printer successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error printing super admin raw text: " + e.getMessage());
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
    private byte[] generateSinhalaReceiptImage(String receiptText) throws Exception {
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
     * 
     * @param image BufferedImage to convert
     * @return ESC/POS formatted byte array
     */
    private byte[] convertImageToEscPos(BufferedImage image) {
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
            System.err.println("Error formatting Super Admin ESC/POS data: " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }
    
}

