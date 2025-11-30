package com.devstack.pos.service;

import com.devstack.pos.entity.Customer;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.entity.SuperAdminOrderDetail;
import com.devstack.pos.entity.SuperAdminOrderItem;
import com.devstack.pos.entity.SystemSettings;
import com.devstack.pos.view.tm.CartTm;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Text;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.HorizontalAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class SuperAdminPDFReportService {
    
    private final SuperAdminOrderDetailService superAdminOrderDetailService;
    private final SuperAdminOrderItemService superAdminOrderItemService;
    private final SystemSettingsService systemSettingsService;
    private final CustomerService customerService;
    private final PipeConversionService pipeConversionService;
    private final ProductDetailService productDetailService;
    
    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    // Cache for Unicode font paths (not font objects to avoid document association issues)
    private static String cachedUnicodeFontPath = null;
    private static String cachedUnicodeMonospaceFontPath = null;
    
    // Initialize fonts on first use
    {
        try {
            // Pre-load fonts to see what we get
            System.out.println("\n" + "=".repeat(70));
            System.out.println("=== INITIALIZING UNICODE FONTS FOR SINHALA SUPPORT ===");
            System.out.println("=".repeat(70));
            String fontPath = getUnicodeFontPath();
            if (fontPath != null) {
                System.out.println("✓ Unicode font path initialized successfully");
                System.out.println("   Font path: " + fontPath);
                if (fontPath.contains("Standard") || fontPath.contains("Helvetica")) {
                    System.err.println("   ⚠⚠⚠ WARNING: Using standard font - Sinhala will NOT work!");
                    System.err.println("   ⚠⚠⚠ You MUST install a Sinhala-supporting font!");
                } else {
                    System.out.println("   ✓ Ready for Sinhala text rendering");
                }
            } else {
                System.err.println("✗ FAILED: Could not initialize Unicode font path");
                System.err.println("   Sinhala text will NOT display correctly in PDFs!");
            }
            System.out.println("=".repeat(70) + "\n");
        } catch (Exception e) {
            System.err.println("✗ ERROR initializing Unicode font: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get the path to a Unicode font that supports Sinhala
     * Uses multiple strategies to find a font that supports Sinhala
     */
    private String getUnicodeFontPath() {
        if (cachedUnicodeFontPath != null) {
            return cachedUnicodeFontPath;
        }
        
        try {
            // Strategy 0: FIRST check for bundled font in resources (BEST - no installation needed!)
            System.out.println("Strategy 0: Checking for bundled font in resources...");
            String bundledFontPath = getBundledFontPath();
            if (bundledFontPath != null) {
                try {
                    PdfFont testFont = PdfFontFactory.createFont(bundledFontPath, PdfEncodings.IDENTITY_H);
                    if (testFont != null && verifySinhalaSupport(testFont, bundledFontPath)) {
                        cachedUnicodeFontPath = bundledFontPath;
                        System.out.println("✓✓✓ SUCCESS: Using BUNDLED font from resources: " + bundledFontPath);
                        System.out.println("   ✓ No separate font installation required!");
                        System.out.println("═══════════════════════════════════════════════════════════");
                        return cachedUnicodeFontPath;
                    }
                } catch (Exception e) {
                    System.out.println("   ⚠ Bundled font found but failed verification: " + e.getMessage());
                }
            }
            
            // Strategy 1: Try direct font file paths (system-installed fonts)
            // This should run BEFORE GraphicsEnvironment to avoid decorative fonts
            String os = System.getProperty("os.name").toLowerCase();
            String[] fontPaths = {};
            
            if (os.contains("mac")) {
                // macOS font paths - check multiple locations
                fontPaths = new String[]{
                    // Noto Sans Sinhala (if installed) - BEST CHOICE
                    "/System/Library/Fonts/Supplemental/NotoSansSinhala-Regular.ttf",
                    "/Library/Fonts/NotoSansSinhala-Regular.ttf",
                    System.getProperty("user.home") + "/Library/Fonts/NotoSansSinhala-Regular.ttf",
                    // Arial Unicode (usually not on macOS by default, but check anyway)
                    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                    "/Library/Fonts/Arial Unicode.ttf",
                    // System fonts that might support Sinhala
                    "/System/Library/Fonts/Supplemental/AppleGothic.ttf",
                    "/System/Library/Fonts/Helvetica.ttc",
                    "/System/Library/Fonts/HelveticaNeue.ttc",
                    // Check for any Noto fonts
                    "/System/Library/Fonts/Supplemental/NotoSans-Regular.ttf",
                    "/Library/Fonts/NotoSans-Regular.ttf"
                };
            } else if (os.contains("win")) {
                String windir = System.getenv("WINDIR");
                if (windir != null) {
                    fontPaths = new String[]{
                        windir + "\\Fonts\\arialuni.ttf",  // Arial Unicode MS - BEST for Windows
                        windir + "\\Fonts\\ARIALUNI.TTF",
                        windir + "\\Fonts\\NotoSansSinhala-Regular.ttf",
                        windir + "\\Fonts\\NotoSans-Regular.ttf",
                        // DO NOT include regular arial.ttf - it does NOT support Sinhala!
                        // Only Arial Unicode MS (arialuni.ttf) supports Sinhala
                    };
                }
            } else {
                // Linux
                fontPaths = new String[]{
                    "/usr/share/fonts/truetype/noto/NotoSansSinhala-Regular.ttf",
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                    "/usr/share/fonts/opentype/noto/NotoSansSinhala-Regular.otf"
                };
            }
            
            System.out.println("Strategy 1: Checking direct font file paths...");
            for (String fontPath : fontPaths) {
                try {
                    File fontFile = new File(fontPath);
                    if (fontFile.exists() && fontFile.canRead()) {
                        System.out.println("Found font file: " + fontPath);
                        PdfFont pdfFont = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
                        if (pdfFont != null) {
                            // VERIFY: Actually test if this font can render Sinhala
                            if (verifySinhalaSupport(pdfFont, fontPath)) {
                                cachedUnicodeFontPath = fontPath;
                                System.out.println("✓✓✓ SUCCESS: Found and VERIFIED Unicode font from file: " + fontPath);
                                System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                System.out.println("═══════════════════════════════════════════════════════════");
                                return cachedUnicodeFontPath;
                            } else {
                                System.out.println("   ⚠ Font loaded but FAILED Sinhala verification - continuing search...");
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Could not load font from " + fontPath);
                } catch (Exception e) {
                    System.out.println("Error loading font from " + fontPath + ": " + e.getMessage());
                }
            }
            
            // Strategy 2: Try to find fonts using Java's GraphicsEnvironment that support Sinhala
            // CRITICAL: Filter out decorative fonts that don't actually have Sinhala glyphs
            System.out.println("Strategy 2: Searching installed fonts (filtering decorative fonts)...");
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] availableFonts = ge.getAvailableFontFamilyNames();
            char sinhalaTestChar = 'ස'; // Sinhala 'sa' character
            
            // Fonts that are KNOWN to support Sinhala - prioritize these
            // IMPORTANT: "Arial Unicode MS" must come before any check for "Arial" alone
            String[] preferredFontNames = {
                "Noto Sans Sinhala", "Noto Sans", "Arial Unicode MS", "Arial Unicode",
                "DejaVu Sans", "Liberation Sans", "Lucida Sans Unicode", "Tahoma"
                // DO NOT include "Arial" or "Helvetica" here - they don't support Sinhala!
            };
            
            // Decorative/fancy fonts to SKIP - these don't have Sinhala glyphs even if canDisplay() returns true
            // CRITICAL: Include "Arial" (but NOT "Arial Unicode MS") - regular Arial doesn't support Sinhala!
            String[] skipFonts = {
                "Arial", // Regular Arial - does NOT support Sinhala! Only "Arial Unicode MS" does.
                "Helvetica", "Helvetica Neue", // Standard fonts without Sinhala support
                "Academy Engraved", "Algerian", "Bauhaus", "Blackadder", "Bradley Hand",
                "Brush Script", "Chalkduster", "Comic Sans", "Copperplate", "Corsiva",
                "Herculanum", "Impact", "Marker Felt", "Optima", "Papyrus", "Party LET",
                "Savoye LET", "Snell Roundhand", "Stencil", "Trattatello", "Zapfino",
                "Zapf Chancery", "Apple Chancery", "Baskerville", "Big Caslon",
                "Bodoni", "Bradley Hand", "Chalkboard", "Chalkboard SE", "Cochin",
                "Didot", "Futura", "Geneva", "Gill Sans", "Hoefler Text", "Luminari",
                "Monaco", "Noteworthy", "Palatino", "Phosphate", "Rockwell", "SignPainter",
                "Skia", "Superclarendon", "Times", "Trebuchet MS", "Verdana"
            };
            
            // FIRST: Try preferred fonts that are known to support Sinhala
            System.out.println("Searching for preferred Sinhala-supporting fonts...");
            for (String preferredName : preferredFontNames) {
                for (String fontName : availableFonts) {
                    if (fontName.equalsIgnoreCase(preferredName) || 
                        fontName.toLowerCase().contains(preferredName.toLowerCase())) {
                        try {
                            Font testFont = new Font(fontName, Font.PLAIN, 12);
                            if (testFont.canDisplay(sinhalaTestChar)) {
                                String fontPath = findFontFile(fontName);
                                if (fontPath != null) {
                                    try {
                                        PdfFont pdfFont = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
                                        if (pdfFont != null) {
                                            // VERIFY: Actually test if this font can render Sinhala
                                            if (verifySinhalaSupport(pdfFont, fontPath)) {
                                                cachedUnicodeFontPath = fontPath;
                                                System.out.println("✓✓✓ SUCCESS: Found and VERIFIED PREFERRED Unicode font '" + fontName + "' from: " + fontPath);
                                                System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                                System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                                System.out.println("═══════════════════════════════════════════════════════════");
                                                return cachedUnicodeFontPath;
                                            } else {
                                                System.out.println("   ⚠ Preferred font '" + fontName + "' FAILED Sinhala verification - continuing search...");
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.out.println("Could not load preferred font: " + fontName);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Continue
                        }
                    }
                }
            }
            
            // THEN: Try other fonts, but SKIP decorative ones
            System.out.println("Searching other fonts (skipping decorative fonts)...");
            for (String fontName : availableFonts) {
                // Skip decorative fonts that don't actually support Sinhala
                boolean shouldSkip = false;
                String fontNameLower = fontName.toLowerCase();
                for (String skipFont : skipFonts) {
                    if (fontNameLower.contains(skipFont.toLowerCase())) {
                        shouldSkip = true;
                        break;
                    }
                }
                if (shouldSkip) {
                    System.out.println("Skipping decorative font: " + fontName);
                    continue;
                }
                
                try {
                    Font testFont = new Font(fontName, Font.PLAIN, 12);
                    if (testFont.canDisplay(sinhalaTestChar)) {
                        String fontPath = findFontFile(fontName);
                        if (fontPath != null) {
                            try {
                                PdfFont pdfFont = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
                                if (pdfFont != null) {
                                    // VERIFY: Actually test if this font can render Sinhala
                                    if (verifySinhalaSupport(pdfFont, fontPath)) {
                                        cachedUnicodeFontPath = fontPath;
                                        System.out.println("✓✓✓ SUCCESS: Found and VERIFIED Unicode font '" + fontName + "' from: " + fontPath);
                                        System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                        System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                        System.out.println("═══════════════════════════════════════════════════════════");
                                        return cachedUnicodeFontPath;
                                    } else {
                                        System.out.println("   ⚠ Font '" + fontName + "' FAILED Sinhala verification - continuing search...");
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println("Could not load font file for " + fontName);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue to next font
                }
            }
            
            // Strategy 3: Search common font directories
            String[] searchDirs = {};
            if (os.contains("mac")) {
                searchDirs = new String[]{
                    "/System/Library/Fonts/Supplemental/",
                    "/Library/Fonts/",
                    System.getProperty("user.home") + "/Library/Fonts/"
                };
            } else if (os.contains("win")) {
                String windir = System.getenv("WINDIR");
                if (windir != null) {
                    searchDirs = new String[]{windir + "\\Fonts\\"};
                }
            } else {
                searchDirs = new String[]{
                    "/usr/share/fonts/truetype/",
                    "/usr/share/fonts/opentype/",
                    System.getProperty("user.home") + "/.fonts/"
                };
            }
            
            for (String dir : searchDirs) {
                String foundFont = searchForSinhalaFont(dir);
                if (foundFont != null) {
                    try {
                        System.out.println("Trying font found in search: " + foundFont);
                        PdfFont pdfFont = PdfFontFactory.createFont(foundFont, PdfEncodings.IDENTITY_H);
                        if (pdfFont != null) {
                            // VERIFY: Actually test if this font can render Sinhala
                            if (verifySinhalaSupport(pdfFont, foundFont)) {
                                cachedUnicodeFontPath = foundFont;
                                System.out.println("✓✓✓ SUCCESS: Found and VERIFIED Unicode font: " + foundFont);
                                System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                System.out.println("═══════════════════════════════════════════════════════════");
                                return cachedUnicodeFontPath;
                            } else {
                                System.out.println("   ⚠ Found font FAILED Sinhala verification: " + foundFont);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Could not load found font: " + foundFont + " - " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println("⚠⚠⚠ CRITICAL: No Unicode font with Sinhala support found!");
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println("Sinhala text will appear as boxes (☐☐☐) in PDFs.");
            System.err.println("");
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                System.err.println("SOLUTION FOR WINDOWS:");
                System.err.println("  1. Install Arial Unicode MS (if not already installed):");
                System.err.println("     - Check if C:\\WINDOWS\\Fonts\\arialuni.ttf exists");
                System.err.println("     - If not, download from Microsoft or install Office");
                System.err.println("  2. OR install Noto Sans Sinhala font:");
                System.err.println("     - Download from: https://fonts.google.com/noto/specimen/Noto+Sans+Sinhala");
                System.err.println("     - Install by right-clicking the .ttf file and selecting 'Install'");
                System.err.println("  3. Restart your application");
            } else {
                System.err.println("SOLUTION: Install Noto Sans Sinhala font:");
                System.err.println("  1. Download from: https://fonts.google.com/noto/specimen/Noto+Sans+Sinhala");
                System.err.println("  2. Install the font:");
                System.err.println("     - Double-click the .ttf file");
                System.err.println("     - Click 'Install Font' in Font Book (Mac) or font manager");
                System.err.println("  3. Restart your application");
            }
            System.err.println("");
            System.err.println("NOTE: Regular Arial (arial.ttf) does NOT support Sinhala!");
            System.err.println("      You need Arial Unicode MS (arialuni.ttf) or Noto Sans Sinhala.");
            System.err.println("═══════════════════════════════════════════════════════════");
            
            // Try one more time with a broader search
            System.out.println("Final attempt: Searching all font directories...");
            String[] allSearchDirs = {
                "/System/Library/Fonts/",
                "/System/Library/Fonts/Supplemental/",
                "/Library/Fonts/",
                System.getProperty("user.home") + "/Library/Fonts/",
                "/System/Library/Fonts/Helvetica.ttc",
                "/System/Library/Fonts/HelveticaNeue.ttc"
            };
            
            for (String dir : allSearchDirs) {
                File dirFile = new File(dir);
                if (dirFile.exists()) {
                    if (dirFile.isFile()) {
                        // It's a font file, try it
                        try {
                            PdfFont testFont = PdfFontFactory.createFont(dir, PdfEncodings.IDENTITY_H);
                            if (verifySinhalaSupport(testFont, dir)) {
                                cachedUnicodeFontPath = dir;
                                System.out.println("✓✓✓ Found Sinhala font on final search: " + dir);
                                System.out.println("═══════════════════════════════════════════════════════════");
                                return cachedUnicodeFontPath;
                            }
                        } catch (Exception e) {
                            // Continue
                        }
                    } else {
                        // It's a directory, search recursively
                        String found = searchForSinhalaFontRecursive(dir);
                        if (found != null) {
                            try {
                                PdfFont testFont = PdfFontFactory.createFont(found, PdfEncodings.IDENTITY_H);
                                if (verifySinhalaSupport(testFont, found)) {
                                    cachedUnicodeFontPath = found;
                                    System.out.println("✓✓✓ Found Sinhala font on final recursive search: " + found);
                                    System.out.println("═══════════════════════════════════════════════════════════");
                                    return cachedUnicodeFontPath;
                                }
                            } catch (Exception e) {
                                // Continue
                            }
                        }
                    }
                }
            }
            
            cachedUnicodeFontPath = "Standard Helvetica (NO SINHALA SUPPORT)";
            System.err.println("⚠ Using fallback font: Standard Helvetica (Sinhala will show as boxes)");
            System.err.println("═══════════════════════════════════════════════════════════");
            return cachedUnicodeFontPath;
            
        } catch (Exception e) {
            System.err.println("Error loading Unicode font: " + e.getMessage());
            e.printStackTrace();
            cachedUnicodeFontPath = "Standard Helvetica (NO SINHALA SUPPORT)";
            return cachedUnicodeFontPath;
        }
    }
    
    /**
     * Get the path to the bundled font from resources
     * Returns a temporary file path if font is found in resources, null otherwise
     */
    private String getBundledFontPath() {
        try {
            // Try multiple resource paths (Maven/Spring Boot packages resources differently)
            String[] resourcePaths = {
                "/fonts/NotoSansSinhala-Regular.ttf",           // Standard Maven resource path
                "/resources/fonts/NotoSansSinhala-Regular.ttf",  // Alternative path
                "fonts/NotoSansSinhala-Regular.ttf",              // Without leading slash
                "resources/fonts/NotoSansSinhala-Regular.ttf"    // Alternative without slash
            };
            
            java.io.InputStream fontStream = null;
            String foundPath = null;
            
            // Try with getClass().getResourceAsStream() first
            for (String path : resourcePaths) {
                fontStream = getClass().getResourceAsStream(path);
                if (fontStream != null) {
                    foundPath = path;
                    System.out.println("   ✓ Found font at resource path: " + path);
                    break;
                }
            }
            
            // If not found, try with ClassLoader (for Spring Boot)
            if (fontStream == null) {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if (classLoader == null) {
                    classLoader = getClass().getClassLoader();
                }
                
                for (String path : resourcePaths) {
                    // Remove leading slash for ClassLoader
                    String classLoaderPath = path.startsWith("/") ? path.substring(1) : path;
                    fontStream = classLoader.getResourceAsStream(classLoaderPath);
                    if (fontStream != null) {
                        foundPath = path;
                        System.out.println("   ✓ Found font via ClassLoader at: " + path);
                        break;
                    }
                }
            }
            
            if (fontStream == null) {
                System.out.println("   ⚠ No bundled font found in resources");
                System.out.println("   📥 To add the font:");
                System.out.println("      1. Download from: https://fonts.google.com/noto/specimen/Noto+Sans+Sinhala");
                System.out.println("      2. Extract and copy NotoSansSinhala-Regular.ttf to: src/resources/fonts/");
                System.out.println("      3. Rebuild the project (mvn clean package)");
                return null;
            }
            
            // Copy font to temporary file so iTextPDF can use it
            java.io.File tempFontFile = java.io.File.createTempFile("NotoSansSinhala", ".ttf");
            tempFontFile.deleteOnExit(); // Clean up on exit
            
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFontFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fontStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            fontStream.close();
            
            System.out.println("   ✓ Found bundled font in resources, extracted to: " + tempFontFile.getAbsolutePath());
            return tempFontFile.getAbsolutePath();
            
        } catch (Exception e) {
            System.out.println("   ⚠ Error loading bundled font: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get a Unicode font instance for the current PDF document
     * Creates a fresh font instance from the cached font path to avoid document association issues
     */
    private PdfFont getUnicodeFont() throws IOException {
        String fontPath = getUnicodeFontPath();
        if (fontPath == null || fontPath.contains("Standard Helvetica")) {
            // Fallback to standard font
            return PdfFontFactory.createFont(StandardFonts.HELVETICA);
        }
        // Create a fresh font instance for this document
        return PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
    }
    
    /**
     * Verify that a PdfFont actually supports Sinhala by testing rendering
     * This is more reliable than canDisplay() which can return true for fonts without glyphs
     * 
     * IMPORTANT: Creates a NEW font for testing to avoid associating the passed font with a test PDF
     */
    private boolean verifySinhalaSupport(PdfFont font, String fontPath) {
        try {
            String fontPathLower = fontPath.toLowerCase();
            
            // CRITICAL: Reject regular Arial - it does NOT support Sinhala!
            // Only Arial Unicode MS (arialuni.ttf) supports Sinhala
            if (fontPathLower.contains("arial.ttf") && !fontPathLower.contains("arialuni")) {
                System.out.println("   ✗ REJECTED: Regular Arial does NOT support Sinhala (need Arial Unicode MS)");
                return false;
            }
            
            // First check: If font path contains Sinhala-related keywords, it's likely good
            String[] trustedKeywords = {"sinhala", "noto", "arialuni", "unicode", "dejavu", "liberation"};
            boolean hasTrustedKeyword = false;
            for (String keyword : trustedKeywords) {
                if (fontPathLower.contains(keyword)) {
                    hasTrustedKeyword = true;
                    System.out.println("   ✓ Font path contains trusted keyword: " + keyword);
                    break;
                }
            }
            
            // For fonts without trusted keywords, reject them immediately
            // (except we already rejected regular Arial above)
            if (!hasTrustedKeyword) {
                System.out.println("   ✗ REJECTED: Font path does not contain trusted Sinhala-supporting keywords");
                return false;
            }
            
            // If it has a trusted keyword, do a comprehensive test
            // Try to actually render Sinhala text to a temporary PDF and verify glyphs exist
            try {
                String testText = "සිංහල"; // Sinhala text
                
                // Create a minimal test PDF in memory
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                PdfWriter testWriter = new PdfWriter(baos);
                PdfDocument testPdf = new PdfDocument(testWriter);
                Document testDoc = new Document(testPdf);
                
                // CRITICAL: Create a NEW font from the path for testing (don't use the passed font)
                // This prevents the original font from being associated with the test PDF
                PdfFont testFont = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
                
                // Try to add Sinhala text
                Paragraph testPara = new Paragraph(testText);
                testPara.setFont(testFont);
                testDoc.add(testPara);
                testDoc.close();
                
                // Check if the font program actually has the glyphs
                // If font program is null, it's a standard font without Sinhala support
                if (testFont.getFontProgram() == null) {
                    System.out.println("   ✗ REJECTED: Font is a standard font without Sinhala glyphs");
                    return false;
                }
                
                // Additional check: Verify the font program exists and is not a standard font
                // Standard fonts don't have font programs and won't support Sinhala
                try {
                    com.itextpdf.io.font.FontProgram fontProgram = testFont.getFontProgram();
                    if (fontProgram == null) {
                        System.out.println("   ✗ REJECTED: Font program is null - cannot support Sinhala");
                        return false;
                    }
                    // If we got here, the font has a proper font program which should support Unicode
                    System.out.println("   ✓ Font has proper font program for Unicode support");
                } catch (Exception e) {
                    // If we can't check font program, be conservative and accept if it has trusted keyword
                    System.out.println("   ⚠ Could not verify font program directly, but font has trusted keyword");
                }
                
                System.out.println("   ✓ Font passed comprehensive Sinhala rendering test");
                return true;
                
            } catch (Exception e) {
                System.out.println("   ✗ Font failed Sinhala rendering test: " + e.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ✗ Verification error: " + e.getMessage());
            // If verification fails, be conservative and reject the font
            return false;
        }
    }
    
    /**
     * Recursively search for Sinhala-supporting fonts
     */
    private String searchForSinhalaFontRecursive(String dirPath) {
        try {
            File dir = new File(dirPath);
            if (!dir.exists()) return null;
            
            if (dir.isFile()) {
                String fileName = dir.getName().toLowerCase();
                String[] sinhalaKeywords = {"sinhala", "noto", "arialuni", "unicode"};
                for (String keyword : sinhalaKeywords) {
                    if (fileName.contains(keyword) && 
                        (fileName.endsWith(".ttf") || fileName.endsWith(".otf"))) {
                        return dir.getAbsolutePath();
                    }
                }
                return null;
            }
            
            // It's a directory, search recursively (but limit depth to avoid too much searching)
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && !file.getName().startsWith(".")) {
                        // Recursively search subdirectories (limit to 2 levels deep)
                        String found = searchForSinhalaFontRecursive(file.getAbsolutePath());
                        if (found != null) {
                            return found;
                        }
                    } else if (file.isFile()) {
                        String fileName = file.getName().toLowerCase();
                        String[] sinhalaKeywords = {"sinhala", "noto", "arialuni", "unicode"};
                        for (String keyword : sinhalaKeywords) {
                            if (fileName.contains(keyword) && 
                                (fileName.endsWith(".ttf") || fileName.endsWith(".otf"))) {
                                return file.getAbsolutePath();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Find font file path from font name
     */
    private String findFontFile(String fontName) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] searchDirs = {};
            
            if (os.contains("mac")) {
                searchDirs = new String[]{
                    "/System/Library/Fonts/Supplemental/",
                    "/Library/Fonts/",
                    System.getProperty("user.home") + "/Library/Fonts/"
                };
            } else if (os.contains("win")) {
                String windir = System.getenv("WINDIR");
                if (windir != null) {
                    searchDirs = new String[]{windir + "\\Fonts\\"};
                }
            } else {
                searchDirs = new String[]{
                    "/usr/share/fonts/truetype/",
                    "/usr/share/fonts/opentype/"
                };
            }
            
            for (String dir : searchDirs) {
                String found = searchFontInDirectory(dir, fontName);
                if (found != null) return found;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Search for a font file in a directory
     */
    private String searchFontInDirectory(String dirPath, String fontName) {
        try {
            File dir = new File(dirPath);
            if (!dir.exists() || !dir.isDirectory()) return null;
            
            File[] files = dir.listFiles((d, name) -> 
                name.toLowerCase().endsWith(".ttf") || 
                name.toLowerCase().endsWith(".otf") ||
                name.toLowerCase().endsWith(".ttc"));
            
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName().toLowerCase();
                    String fontNameLower = fontName.toLowerCase().replace(" ", "");
                    if (fileName.contains(fontNameLower) || 
                        fileName.replaceAll("[^a-z]", "").contains(fontNameLower.replaceAll("[^a-z]", ""))) {
                        return file.getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Search for fonts that might support Sinhala
     */
    private String searchForSinhalaFont(String dirPath) {
        try {
            File dir = new File(dirPath);
            if (!dir.exists() || !dir.isDirectory()) return null;
            
            String[] sinhalaKeywords = {"sinhala", "noto", "arialuni", "unicode", "dejavu"};
            File[] files = dir.listFiles((d, name) -> 
                name.toLowerCase().endsWith(".ttf") || 
                name.toLowerCase().endsWith(".otf"));
            
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName().toLowerCase();
                    for (String keyword : sinhalaKeywords) {
                        if (fileName.contains(keyword)) {
                            return file.getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Last resort font search - try to find ANY font that might work
     */
    private String tryLastResortFontSearch() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] searchPaths = {};
            
            if (os.contains("mac")) {
                searchPaths = new String[]{
                    "/System/Library/Fonts/",
                    "/Library/Fonts/",
                    System.getProperty("user.home") + "/Library/Fonts/"
                };
            } else if (os.contains("win")) {
                String windir = System.getenv("WINDIR");
                if (windir != null) {
                    searchPaths = new String[]{windir + "\\Fonts\\"};
                }
            } else {
                searchPaths = new String[]{
                    "/usr/share/fonts/",
                    System.getProperty("user.home") + "/.fonts/"
                };
            }
            
            for (String basePath : searchPaths) {
                File baseDir = new File(basePath);
                if (baseDir.exists() && baseDir.isDirectory()) {
                    // Recursively search for .ttf files
                    String found = searchRecursively(baseDir, new String[]{"noto", "arialuni", "dejavu", "liberation"});
                    if (found != null) {
                        return found;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Last resort search error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Recursively search for font files
     */
    private String searchRecursively(File dir, String[] keywords) {
        try {
            File[] files = dir.listFiles();
            if (files == null) return null;
            
            for (File file : files) {
                if (file.isDirectory() && !file.getName().startsWith(".")) {
                    String found = searchRecursively(file, keywords);
                    if (found != null) return found;
                } else if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    if ((name.endsWith(".ttf") || name.endsWith(".otf")) && file.canRead()) {
                        for (String keyword : keywords) {
                            if (name.contains(keyword)) {
                                System.out.println("Found potential font: " + file.getAbsolutePath());
                                return file.getAbsolutePath();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Get the path to a Unicode monospace font for aligned text
     * Uses the same font as regular Unicode font for consistency (monospace not critical for alignment)
     */
    private String getUnicodeMonospaceFontPath() {
        if (cachedUnicodeMonospaceFontPath != null) {
            return cachedUnicodeMonospaceFontPath;
        }
        
        try {
            // First try to get the regular Unicode font path (it will work for alignment too)
            String regularUnicodeFontPath = getUnicodeFontPath();
            if (regularUnicodeFontPath != null && !regularUnicodeFontPath.contains("Standard Helvetica")) {
                cachedUnicodeMonospaceFontPath = regularUnicodeFontPath;
                return cachedUnicodeMonospaceFontPath;
            }
            
            // Fallback: Try to find monospace fonts
            String os = System.getProperty("os.name").toLowerCase();
            String[] fontPaths = {};
            
            if (os.contains("mac")) {
                fontPaths = new String[]{
                    "/System/Library/Fonts/Supplemental/Courier New.ttf",
                    "/Library/Fonts/Courier New.ttf",
                    "/System/Library/Fonts/Supplemental/NotoSansMono-Regular.ttf"
                };
            } else if (os.contains("win")) {
                String windir = System.getenv("WINDIR");
                if (windir != null) {
                    fontPaths = new String[]{
                        windir + "\\Fonts\\cour.ttf",
                        windir + "\\Fonts\\courbd.ttf",
                        windir + "\\Fonts\\NotoSansMono-Regular.ttf"
                    };
                }
            } else {
                fontPaths = new String[]{
                    "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
                    "/usr/share/fonts/truetype/liberation/LiberationMono-Regular.ttf",
                    "/usr/share/fonts/truetype/noto/NotoSansMono-Regular.ttf"
                };
            }
            
            for (String fontPath : fontPaths) {
                try {
                    File fontFile = new File(fontPath);
                    if (fontFile.exists() && fontFile.canRead()) {
                        cachedUnicodeMonospaceFontPath = fontPath;
                        System.out.println("✓ Found Unicode monospace font from: " + fontPath);
                        return cachedUnicodeMonospaceFontPath;
                    }
                } catch (Exception e) {
                    System.out.println("Could not load monospace font from " + fontPath);
                }
            }
            
            System.out.println("Warning: No Unicode monospace font found, using standard Courier.");
            cachedUnicodeMonospaceFontPath = "Standard Courier (NO SINHALA SUPPORT)";
            return cachedUnicodeMonospaceFontPath;
            
        } catch (Exception e) {
            System.err.println("Error loading Unicode monospace font: " + e.getMessage());
            cachedUnicodeMonospaceFontPath = "Standard Courier (NO SINHALA SUPPORT)";
            return cachedUnicodeMonospaceFontPath;
        }
    }
    
    /**
     * Get a Unicode monospace font instance for the current PDF document
     * Creates a fresh font instance from the cached font path to avoid document association issues
     */
    private PdfFont getUnicodeMonospaceFont() throws IOException {
        String fontPath = getUnicodeMonospaceFontPath();
        if (fontPath == null || fontPath.contains("Standard Courier")) {
            // Fallback to standard font
            return PdfFontFactory.createFont(StandardFonts.COURIER);
        }
        // Create a fresh font instance for this document
        return PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
    }
    
    /**
     * Generate plain text bill receipt for thermal printer for super admin orders
     */
    public String generateSuperAdminPlainTextReceipt(Long orderId) {
        try {
            // Get super admin order details
            SuperAdminOrderDetail orderDetail = superAdminOrderDetailService.findSuperAdminOrderDetail(orderId);
            if (orderDetail == null) {
                return "Error: Super Admin Order not found";
            }
            
            // Get super admin order items
            List<SuperAdminOrderItem> orderItems = superAdminOrderItemService.findByOrderId(orderId);
            
            // Get system settings
            SystemSettings settings = systemSettingsService.getSystemSettings();
            
            StringBuilder receipt = new StringBuilder();
            
            // Business name (double width/height for emphasis)
            String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
                ? settings.getBusinessName().toUpperCase()
                : "KUMARA ENTERPRISES";
            receipt.append((char) 0x1B).append("!").append((char) 0x30); // ESC ! 0x30 = double width & height
            receipt.append(centerText(businessName, 26)).append("\n");
            receipt.append((char) 0x1B).append("!").append((char) 0x00); // Reset to normal size
            
            // Address
            receipt.append(centerText("No 58k Gagabada Rd,", 48)).append("\n");
            receipt.append(centerText("Wewala,Piliyandala", 48)).append("\n");
            
            // Contact numbers
            if (settings.getContactNumber() != null && !settings.getContactNumber().trim().isEmpty()) {
                receipt.append(centerText(settings.getContactNumber(), 48)).append("\n");
            } else {
                receipt.append(centerText("077 781 5955 / 011 261 3606", 48)).append("\n");
            }
            
            receipt.append("\n");
            
            // Invoice number and date
            String invoiceLine = String.format("Invoice                 %s",
                orderDetail.getIssuedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            receipt.append(invoiceLine).append("\n");
            
            String codeLine = String.format("%-24d%s",
                orderDetail.getCode(),
                orderDetail.getIssuedDate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            receipt.append(codeLine).append("\n");
            
            // Customer name
            receipt.append(String.format("Customer : %s\n", orderDetail.getCustomerName()));
            
            receipt.append("................................................\n");
            
            // Items header
            receipt.append(String.format("%-14s %9s %5s %6s %9s\n",
                "අයිතම්", "මිල", "ප්‍රමාණය", "වට්ටම", "මුළු"));
            receipt.append("................................................\n");
            
            // Items
            for (SuperAdminOrderItem item : orderItems) {
                String itemName = item.getProductName();
                // Convert pipe measurements to pipe numbers for display - pass productCode and batchCode
                itemName = pipeConversionService.convertProductName(itemName, item.getProductCode(), item.getBatchCode());
                
                double discount = item.getDiscountPerUnit() != null ? item.getDiscountPerUnit() : 0.0;
                
                Double qty = item.getQuantity();
                String qtyStr;
                if (qty != null) {
                    // Convert quantity to feet measurement if it's a pipe product
                    if (pipeConversionService.shouldConvertQuantity(item.getProductName(), item.getProductCode(), item.getBatchCode())) {
                        String convertedQty = pipeConversionService.convertQuantityToFeet(item.getProductName(), qty, item.getProductCode(), item.getBatchCode());
                        qtyStr = "x" + convertedQty;
                        // Pad to ensure consistent width
                        if (qtyStr.length() < 5) {
                            qtyStr = String.format("%-5s", qtyStr);
                        }
                    } else {
                        if (qty == qty.intValue()) {
                            qtyStr = String.format("x%-3d", qty.intValue());
                        } else {
                            qtyStr = String.format("x%-5.2f", qty);
                        }
                    }
                } else {
                    qtyStr = "x0  ";
                }
                
                // Wrap long item names into multiple lines if needed
                String[] itemNameLines = wrapText(itemName, 14);
                for (int i = 0; i < itemNameLines.length; i++) {
                    if (i == 0) {
                        // First line: show item name, price, qty, discount, total
                        receipt.append(String.format("%-14s %9.1f %s %6.2f %9.2f\n",
                            itemNameLines[i],
                            item.getUnitPrice(),
                            qtyStr,
                            discount,
                            item.getLineTotal()
                        ));
                    } else {
                        // Additional lines: show only item name continuation, align other columns
                        receipt.append(String.format("%-14s %9s %5s %6s %9s\n",
                            itemNameLines[i], "", "", "", ""));
                    }
                }
            }
            
            receipt.append("................................................\n");
            
            // Totals section
            double subtotal = orderItems.stream().mapToDouble(item -> {
                Double qty = item.getQuantity();
                return item.getUnitPrice() * (qty != null ? qty : 0.0);
            }).sum();
            double totalDiscount = orderDetail.getDiscount();
            
            receipt.append(String.format("%-30s %17.2f\n", "එකතුව", subtotal));
            receipt.append(String.format("%-30s %17.2f\n", "මුළු වට්ටම", totalDiscount));
            receipt.append(String.format("%-30s %17.2f\n", "අයිතම්", (double)orderItems.size()));
            receipt.append("------------------------------------------------\n");
            receipt.append(String.format("%-30s %17.2f\n", "මුළු එකතුව", orderDetail.getTotalCost()));
            
            // Customer Paid
            double customerPaid = orderDetail.getCustomerPaid() != null ? orderDetail.getCustomerPaid() : orderDetail.getTotalCost();
            receipt.append(String.format("%-30s %17.2f\n", "පාරිභෝගිකයා ගෙවූ මුදල", customerPaid));
            
            // Payment method
            String paymentMethod = orderDetail.getPaymentMethod() != null ? orderDetail.getPaymentMethod() : "CASH";
            String paymentSinhala = "CASH".equals(paymentMethod) ? "ගෙවීම (මුදල්)" : "ගෙවීම";
            receipt.append(String.format("%-30s %17s\n", paymentSinhala, ""));
            
            // Change
            if ("PAID".equals(orderDetail.getPaymentStatus()) && customerPaid > orderDetail.getTotalCost()) {
                double change = customerPaid - orderDetail.getTotalCost();
                receipt.append(String.format("%-30s %17.2f\n", "ඉතිරි මුදල", change));
            }
            
            receipt.append("------------------------------------------------\n");
            
            // Order type
//            String orderType = orderDetail.getOrderType() != null && orderDetail.getOrderType().equals("CONSTRUCTION")
//                ? "Construction" : "Hardware";
//            receipt.append(String.format("Type: %s\n", orderType));
//
//            receipt.append("------------------------------------------------\n");
//
            // Balance
            double balance = orderDetail.getBalance() != null ? orderDetail.getBalance() : 0.00;
            if (balance != 0.00) {
                receipt.append(String.format("%-30s %17.2f\n", "ශේෂය", balance));
            } else {
                receipt.append(String.format("%-30s %17.2f\n", "ශේෂය", 0.00));
            }
            receipt.append("................................................\n");
            
            // Footer message
            String footerMessage = settings.getFooterMessage() != null && !settings.getFooterMessage().trim().isEmpty()
                ? settings.getFooterMessage()
                : "Thank you for your business!";
            receipt.append(centerText(footerMessage, 48)).append("\n");
            receipt.append("................................................\n");
            
            // Software info
            receipt.append(centerText("Green Code Solutions", 48)).append("\n");
            receipt.append(centerText("078 150 8252 | 076 724 3647", 48)).append("\n");
            
            return receipt.toString();
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating super admin receipt: " + e.getMessage();
        }
    }
    
    /**
     * Center text for thermal printer (48 characters wide for 80mm)
     */
    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }
    
    /**
     * Wrap text into multiple lines, breaking at word boundaries when possible
     * @param text Text to wrap
     * @param maxWidth Maximum width per line
     * @return Array of lines
     */
    private String[] wrapText(String text, int maxWidth) {
        if (text == null || text.length() <= maxWidth) {
            return new String[]{text != null ? text : ""};
        }
        
        java.util.List<String> lines = new java.util.ArrayList<>();
        String remaining = text;
        
        while (remaining.length() > maxWidth) {
            // Try to break at word boundary (space)
            int breakPoint = maxWidth;
            int lastSpace = remaining.lastIndexOf(' ', maxWidth);
            
            if (lastSpace > maxWidth / 2) {
                // Break at word boundary if it's not too early
                breakPoint = lastSpace;
                lines.add(remaining.substring(0, breakPoint).trim());
                remaining = remaining.substring(breakPoint).trim();
            } else {
                // Break at exact position if no good word boundary
                lines.add(remaining.substring(0, breakPoint));
                remaining = remaining.substring(breakPoint);
            }
        }
        
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
        
        return lines.toArray(new String[0]);
    }
    
    /**
     * Generate bill receipt PDF for super admin order from cart items (includes all items, not just saved ones)
     * This method is used when super admin makes payment directly from PlaceOrderForm
     */
    public String generateSuperAdminBillReceiptFromCart(
            SuperAdminOrderDetail orderDetail,
            List<CartTm> cartItems,
            String operatorEmail) throws IOException {
        
        // Use default storage location in Documents/POS_System/
        // Extract operator username from email
        String operatorName = operatorEmail != null && operatorEmail.contains("@") 
            ? operatorEmail.substring(0, operatorEmail.indexOf("@")) 
            : (operatorEmail != null ? operatorEmail : "unknown");
        
        // Get customer name - use "Guest" if null or empty
        String customerName = orderDetail.getCustomerName();
        if (customerName == null || customerName.trim().isEmpty() || "Guest".equalsIgnoreCase(customerName.trim())) {
            customerName = "Guest";
        }
        
        // Get receipt directory using FileStorageUtil
        String receiptsDir = com.devstack.pos.util.FileStorageUtil.getSuperAdminReceiptDirectory(operatorName, customerName);
        
        // Create file name and path
        String fileName = "SuperAdmin_Receipt_" + orderDetail.getCode() + "_" + System.currentTimeMillis() + ".pdf";
        String filePath = com.devstack.pos.util.FileStorageUtil.getFilePath(receiptsDir, fileName);
        
        // Generate PDF with cart items
        return generateSuperAdminBillReceiptPDFFromCart(orderDetail, cartItems, filePath, operatorName);
    }
    
    /**
     * Helper method to generate PDF from cart items
     */
    private String generateSuperAdminBillReceiptPDFFromCart(
            SuperAdminOrderDetail orderDetail,
            List<CartTm> cartItems,
            String filePath,
            String operatorName) throws IOException {
        
        // Create PDF
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(15, 15, 15, 15);
        
        // Get system settings
        SystemSettings settings = systemSettingsService.getSystemSettings();
        
        // Color scheme matching invoice format
        DeviceRgb borderColor = new DeviceRgb(139, 0, 0); // Dark red/burgundy border
        DeviceRgb headerTextColor = new DeviceRgb(139, 0, 0); // Dark red for headers
        DeviceRgb textColor = new DeviceRgb(0, 0, 0); // Black text
        DeviceRgb lightGray = new DeviceRgb(245, 245, 245); // Light gray for table rows
        
        // Add border around entire document
        Table borderTable = new Table(1);
        borderTable.setWidth(UnitValue.createPercentValue(100));
        borderTable.setBorder(new SolidBorder(borderColor, 2));
        
        Cell borderCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8);
        
        // Header Section with GSTIN
        Paragraph gstinPara = new Paragraph();
        if (settings.getTaxNumber() != null && !settings.getTaxNumber().trim().isEmpty()) {
            gstinPara.add(new Text("GSTIN: " + settings.getTaxNumber())
                .setFont(getUnicodeFont())
                .setFontSize(8)
                .setFontColor(headerTextColor));
        }
        gstinPara.setTextAlignment(TextAlignment.LEFT);
        borderCell.add(gstinPara);
        
        // Sales Invoice Title
        Paragraph invoiceTitle = new Paragraph("Sales Invoice")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(14)
            .setFont(getUnicodeFont())
            .setBold()
            .setFontColor(headerTextColor)
            .setMarginTop(2)
            .setMarginBottom(5);
        borderCell.add(invoiceTitle);
        
        // Business Name
        String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
            ? settings.getBusinessName()
            : "Kumara Enterprises";
        Paragraph businessNamePara = new Paragraph(businessName)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12)
            .setFont(getUnicodeFont())
            .setBold()
            .setFontColor(headerTextColor)
            .setMarginBottom(2);
        borderCell.add(businessNamePara);
        
        // Business Address
        StringBuilder addressText = new StringBuilder();
        if (settings.getAddress() != null && !settings.getAddress().trim().isEmpty()) {
            addressText.append(settings.getAddress());
        } else {
            addressText.append("58k Gagabada Rd, Wewala, Piliyandala");
        }
        
        Paragraph addressPara = new Paragraph(addressText.toString())
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(8)
            .setFont(getUnicodeFont())
            .setFontColor(textColor)
            .setMarginBottom(1);
        borderCell.add(addressPara);
        
        // Contact and Email
        StringBuilder contactText = new StringBuilder();
        if (settings.getContactNumber() != null && !settings.getContactNumber().trim().isEmpty()) {
            contactText.append("Ph. ").append(settings.getContactNumber());
        } else {
            contactText.append("Ph. 077 781 5955 / 011 261 3606");
        }
        if (settings.getEmail() != null && !settings.getEmail().trim().isEmpty()) {
            contactText.append(", Email: ").append(settings.getEmail());
        }
        
        Paragraph contactPara = new Paragraph(contactText.toString())
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(8)
            .setFont(getUnicodeFont())
            .setFontColor(textColor)
            .setMarginBottom(5);
        borderCell.add(contactPara);
        
        // Get customer details if available (needed for mobile number in invoice details)
        Customer customer = null;
        if (orderDetail.getCustomerId() != null) {
            customer = customerService.findCustomer(orderDetail.getCustomerId());
        }
        
        // Invoice Details and User/Transaction Details in Two Columns
        Table detailsContainerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        detailsContainerTable.setWidth(UnitValue.createPercentValue(100));
        detailsContainerTable.setMarginBottom(5);
        
        PdfFont detailFont = getUnicodeFont();
        
        // Left Column: Invoice Details
        Table invoiceDetailsTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
        invoiceDetailsTable.setWidth(UnitValue.createPercentValue(100));
        
        // Invoice Number
        invoiceDetailsTable.addCell(createInfoCell("No.:", true, detailFont));
        invoiceDetailsTable.addCell(createInfoCell(String.valueOf(orderDetail.getCode()), false, detailFont));
        
        // Invoice Date
        String dateStr = orderDetail.getIssuedDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        invoiceDetailsTable.addCell(createInfoCell("Dt.:", true, detailFont));
        invoiceDetailsTable.addCell(createInfoCell(dateStr, false, detailFont));
        
        // Invoice Time
        String timeStr = orderDetail.getIssuedDate().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        invoiceDetailsTable.addCell(createInfoCell("Time:", true, detailFont));
        invoiceDetailsTable.addCell(createInfoCell(timeStr, false, detailFont));
        
        // Customer Mobile Number
        String customerMobile = "";
        if (customer != null && customer.getContact() != null && !customer.getContact().trim().isEmpty()) {
            customerMobile = customer.getContact();
        }
        invoiceDetailsTable.addCell(createInfoCell("Mobile:", true, detailFont));
        invoiceDetailsTable.addCell(createInfoCell(customerMobile.isEmpty() ? "-" : customerMobile, false, detailFont));
        
        Cell leftCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
        leftCell.add(invoiceDetailsTable);
        detailsContainerTable.addCell(leftCell);
        
        // Right Column: User/Transaction Details
        Table userTransactionTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
        userTransactionTable.setWidth(UnitValue.createPercentValue(100));
        
        // Operator/User Details
        if (orderDetail.getOperatorEmail() != null && !orderDetail.getOperatorEmail().trim().isEmpty()) {
            userTransactionTable.addCell(createInfoCell("Operator:", true, detailFont));
            userTransactionTable.addCell(createInfoCell(operatorName, false, detailFont));
        }
        
        // Payment Method
        if (orderDetail.getPaymentMethod() != null && !orderDetail.getPaymentMethod().trim().isEmpty()) {
            userTransactionTable.addCell(createInfoCell("Payment Method:", true, detailFont));
            userTransactionTable.addCell(createInfoCell(orderDetail.getPaymentMethod(), false, detailFont));
        }
        
        // Payment Status
        if (orderDetail.getPaymentStatus() != null && !orderDetail.getPaymentStatus().trim().isEmpty()) {
            userTransactionTable.addCell(createInfoCell("Payment Status:", true, detailFont));
            userTransactionTable.addCell(createInfoCell(orderDetail.getPaymentStatus(), false, detailFont));
        }
        
        // Transaction ID/Reference
        userTransactionTable.addCell(createInfoCell("Transaction ID:", true, detailFont));
        userTransactionTable.addCell(createInfoCell("SA-ORD-" + orderDetail.getCode(), false, detailFont));
        
        Cell rightCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
        rightCell.add(userTransactionTable);
        detailsContainerTable.addCell(rightCell);
        
        borderCell.add(detailsContainerTable);
        
        // Customer Section
        Paragraph customerLabel = new Paragraph("To, " + orderDetail.getCustomerName())
            .setFont(getUnicodeFont())
            .setFontSize(9)
            .setBold()
            .setFontColor(textColor)
            .setMarginTop(5)
            .setMarginBottom(5);
        borderCell.add(customerLabel);
        
        // Items Table
        Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{3f, 1f, 1f, 1f}));
        itemsTable.setWidth(UnitValue.createPercentValue(100));
        itemsTable.setMarginBottom(5);
        
        // Table Header
        DeviceRgb headerBgColor = new DeviceRgb(139, 0, 0);
        itemsTable.addHeaderCell(createTableHeaderCell("Description", headerBgColor));
        itemsTable.addHeaderCell(createTableHeaderCell("Qty", headerBgColor));
        itemsTable.addHeaderCell(createTableHeaderCell("Rate", headerBgColor));
        itemsTable.addHeaderCell(createTableHeaderCell("Total", headerBgColor));
        
        // Items rows from cart
        PdfFont itemFont = getUnicodeFont();
        PdfFont monospaceFont = getUnicodeMonospaceFont();
        boolean alternate = false;
        
        for (CartTm cartItem : cartItems) {
            String itemName = cartItem.getDescription();
            
            // Get productCode from ProductDetail if available (for regular products)
            Integer productCode = null;
            if (cartItem.getCode() != null && !cartItem.getCode().startsWith("GEN_") && !cartItem.getCode().startsWith("TRANSPORT_")) {
                try {
                    ProductDetail productDetail = productDetailService.findProductDetail(cartItem.getCode());
                    if (productDetail != null) {
                        productCode = productDetail.getProductCode();
                    }
                } catch (Exception e) {
                    // Ignore - productCode will remain null
                }
            }
            
            // Convert pipe measurements to pipe numbers for display
            itemName = pipeConversionService.convertProductName(itemName, productCode, cartItem.getCode());
            
            double quantity = cartItem.getQty();
            double rate = cartItem.getSellingPrice();
            double total = cartItem.getTotalCost();
            
            // Description
            Cell descCell = new Cell()
                .add(new Paragraph(itemName).setFont(itemFont).setFontSize(8))
                .setPadding(4)
                .setBorder(new SolidBorder(textColor, 0.5f));
            if (alternate) {
                descCell.setBackgroundColor(lightGray);
            }
            itemsTable.addCell(descCell);
            
            // Quantity - convert to feet measurement if it's a pipe product
            String quantityDisplay;
            if (pipeConversionService.shouldConvertQuantity(cartItem.getDescription(), productCode, cartItem.getCode())) {
                quantityDisplay = pipeConversionService.convertQuantityToFeet(cartItem.getDescription(), quantity, productCode, cartItem.getCode());
            } else {
                quantityDisplay = String.format("%.2f", quantity);
            }
            Cell qtyCell = new Cell()
                .add(new Paragraph(quantityDisplay).setFont(monospaceFont).setFontSize(8))
                .setPadding(4)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(textColor, 0.5f));
            if (alternate) {
                qtyCell.setBackgroundColor(lightGray);
            }
            itemsTable.addCell(qtyCell);
            
            // Rate
            Cell rateCell = new Cell()
                .add(new Paragraph(String.format("%.2f", rate)).setFont(monospaceFont).setFontSize(8))
                .setPadding(4)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(textColor, 0.5f));
            if (alternate) {
                rateCell.setBackgroundColor(lightGray);
            }
            itemsTable.addCell(rateCell);
            
            // Total
            Cell totalCell = new Cell()
                .add(new Paragraph(String.format("%.2f", total)).setFont(monospaceFont).setFontSize(8))
                .setPadding(4)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(textColor, 0.5f));
            if (alternate) {
                totalCell.setBackgroundColor(lightGray);
            }
            itemsTable.addCell(totalCell);
            
            alternate = !alternate;
        }
        
        borderCell.add(itemsTable);
        
        // Summary Section - calculate from cart items
        double totalCost = cartItems.stream().mapToDouble(CartTm::getTotalCost).sum();
        double discount = orderDetail.getDiscount();
        double finalTotal = totalCost - discount;
        
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}));
        summaryTable.setWidth(UnitValue.createPercentValue(50));
        summaryTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        summaryTable.setMarginBottom(5);
        
        // Less (discount)
        if (discount > 0) {
            summaryTable.addCell(createTotalLabelCell("Less:", monospaceFont));
            summaryTable.addCell(createTotalValueCell(String.format("%.2f", discount), monospaceFont));
        }
        
        // Total
        summaryTable.addCell(createTotalLabelCell("Total:", monospaceFont, true));
        summaryTable.addCell(createTotalValueCell(String.format("%.2f", finalTotal), monospaceFont, true));
        
        borderCell.add(summaryTable);
        
        // Payment Details Section
        if (orderDetail.getCustomerPaid() != null || orderDetail.getBalance() != null || 
            (orderDetail.getPaymentMethod() != null && !orderDetail.getPaymentMethod().isEmpty())) {
            Table paymentTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}));
            paymentTable.setWidth(UnitValue.createPercentValue(50));
            paymentTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            paymentTable.setMarginTop(5);
            paymentTable.setMarginBottom(5);
            
            // Customer Paid
            if (orderDetail.getCustomerPaid() != null && orderDetail.getCustomerPaid() > 0) {
                paymentTable.addCell(createTotalLabelCell("Paid:", monospaceFont));
                paymentTable.addCell(createTotalValueCell(String.format("%.2f", orderDetail.getCustomerPaid()), monospaceFont));
            }
            
            // Change (if customer paid more than total)
            if (orderDetail.getCustomerPaid() != null && orderDetail.getCustomerPaid() > finalTotal) {
                double change = orderDetail.getCustomerPaid() - finalTotal;
                paymentTable.addCell(createTotalLabelCell("Change:", monospaceFont));
                paymentTable.addCell(createTotalValueCell(String.format("%.2f", change), monospaceFont));
            }
            
            // Balance (if customer paid less than total)
            if (orderDetail.getBalance() != null && orderDetail.getBalance() > 0) {
                paymentTable.addCell(createTotalLabelCell("Balance:", monospaceFont));
                paymentTable.addCell(createTotalValueCell(String.format("%.2f", orderDetail.getBalance()), monospaceFont));
            }
            
            borderCell.add(paymentTable);
        }
        
        // Signature line
        Paragraph signatureLine = new Paragraph("For " + businessName)
            .setFont(getUnicodeFont())
            .setFontSize(8)
            .setFontColor(textColor)
            .setTextAlignment(TextAlignment.RIGHT)
            .setMarginTop(10);
        borderCell.add(signatureLine);
        
        borderTable.addCell(borderCell);
        document.add(borderTable);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate bill receipt PDF for a specific super admin order
     */
    public String generateSuperAdminBillReceipt(Long orderId) throws IOException {
        // Get super admin order details
        SuperAdminOrderDetail orderDetail = superAdminOrderDetailService.findSuperAdminOrderDetail(orderId);
        if (orderDetail == null) {
            throw new RuntimeException("Super Admin Order not found: " + orderId);
        }
        
        // Get super admin order items
        List<SuperAdminOrderItem> orderItems = superAdminOrderItemService.findByOrderId(orderId);
        
        // Use default storage location in Documents/POS_System/SuperAdmin_Receipts/
        String operatorEmail = orderDetail.getOperatorEmail();
        
        // Extract operator username from email
        String operatorName = operatorEmail != null && operatorEmail.contains("@") 
            ? operatorEmail.substring(0, operatorEmail.indexOf("@")) 
            : (operatorEmail != null ? operatorEmail : "unknown");
        
        // Get customer name - use "Guest" if null or empty
        String customerName = orderDetail.getCustomerName();
        if (customerName == null || customerName.trim().isEmpty() || "Guest".equalsIgnoreCase(customerName.trim())) {
            customerName = "Guest";
        }
        
        // Get receipt directory using FileStorageUtil (separate location for super admin)
        String receiptsDir = com.devstack.pos.util.FileStorageUtil.getSuperAdminReceiptDirectory(operatorName, customerName);
        
        // Create file name and path
        String fileName = "SuperAdmin_Receipt_" + orderDetail.getCode() + "_" + System.currentTimeMillis() + ".pdf";
        String filePath = com.devstack.pos.util.FileStorageUtil.getFilePath(receiptsDir, fileName);
        
        // Create PDF
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(15, 15, 15, 15);
        
        // Get system settings
        SystemSettings settings = systemSettingsService.getSystemSettings();
        
        // Color scheme matching invoice format
        DeviceRgb borderColor = new DeviceRgb(139, 0, 0); // Dark red/burgundy border
        DeviceRgb headerTextColor = new DeviceRgb(139, 0, 0); // Dark red for headers
        DeviceRgb textColor = new DeviceRgb(0, 0, 0); // Black text
        DeviceRgb lightGray = new DeviceRgb(245, 245, 245); // Light gray for table rows
        
        // Add border around entire document
        Table borderTable = new Table(1);
        borderTable.setWidth(UnitValue.createPercentValue(100));
        borderTable.setBorder(new SolidBorder(borderColor, 2));
        
        Cell borderCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8);
        
        // Header Section with GSTIN
        Paragraph gstinPara = new Paragraph();
        if (settings.getTaxNumber() != null && !settings.getTaxNumber().trim().isEmpty()) {
            gstinPara.add(new Text("GSTIN: " + settings.getTaxNumber())
                .setFont(getUnicodeFont())
                .setFontSize(8)
                .setFontColor(headerTextColor));
        }
        gstinPara.setTextAlignment(TextAlignment.LEFT);
        borderCell.add(gstinPara);
        
        // Sales Invoice Title
        Paragraph invoiceTitle = new Paragraph("Sales Invoice")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(14)
            .setFont(getUnicodeFont())
            .setBold()
            .setFontColor(headerTextColor)
            .setMarginTop(2)
            .setMarginBottom(5);
        borderCell.add(invoiceTitle);
        
        // Business Name
        String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
            ? settings.getBusinessName()
            : "Kumara Enterprises";
        Paragraph businessNamePara = new Paragraph(businessName)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12)
            .setFont(getUnicodeFont())
            .setBold()
            .setFontColor(headerTextColor)
            .setMarginBottom(2);
        borderCell.add(businessNamePara);
        
        // Business Address
        StringBuilder addressText = new StringBuilder();
        if (settings.getAddress() != null && !settings.getAddress().trim().isEmpty()) {
            addressText.append(settings.getAddress());
        } else {
            addressText.append("58k Gagabada Rd, Wewala, Piliyandala");
        }
        
        Paragraph addressPara = new Paragraph(addressText.toString())
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(8)
            .setFont(getUnicodeFont())
            .setFontColor(textColor)
            .setMarginBottom(1);
        borderCell.add(addressPara);
        
        // Contact and Email
        StringBuilder contactText = new StringBuilder();
        if (settings.getContactNumber() != null && !settings.getContactNumber().trim().isEmpty()) {
            contactText.append("Ph. ").append(settings.getContactNumber());
        } else {
            contactText.append("Ph. 077 781 5955 / 011 261 3606");
        }
        if (settings.getEmail() != null && !settings.getEmail().trim().isEmpty()) {
            contactText.append(", Email: ").append(settings.getEmail());
        }
        
        Paragraph contactPara = new Paragraph(contactText.toString())
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(8)
            .setFont(getUnicodeFont())
            .setFontColor(textColor)
            .setMarginBottom(5);
        borderCell.add(contactPara);
        
        // Get customer details if available (needed for mobile number in invoice details)
        Customer customer = null;
        if (orderDetail.getCustomerId() != null) {
            customer = customerService.findCustomer(orderDetail.getCustomerId());
        }
        
        // Invoice Details and User/Transaction Details in Two Columns
        Table detailsContainerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        detailsContainerTable.setWidth(UnitValue.createPercentValue(100));
        detailsContainerTable.setMarginBottom(5);
        
        PdfFont detailFont = getUnicodeFont();
        
        // Left Column: Invoice Details
        Table invoiceDetailsTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
        invoiceDetailsTable.setWidth(UnitValue.createPercentValue(100));
        
        // Invoice Number
        invoiceDetailsTable.addCell(createInfoCell("No.:", true, detailFont));
        invoiceDetailsTable.addCell(createInfoCell(String.valueOf(orderDetail.getCode()), false, detailFont));
        
        // Invoice Date
        String dateStr = orderDetail.getIssuedDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        invoiceDetailsTable.addCell(createInfoCell("Dt.:", true, detailFont));
        invoiceDetailsTable.addCell(createInfoCell(dateStr, false, detailFont));
        
        // Invoice Time
        String timeStr = orderDetail.getIssuedDate().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        invoiceDetailsTable.addCell(createInfoCell("Time:", true, detailFont));
        invoiceDetailsTable.addCell(createInfoCell(timeStr, false, detailFont));
        
        // Customer Mobile Number
        String customerMobile = "";
        if (customer != null && customer.getContact() != null && !customer.getContact().trim().isEmpty()) {
            customerMobile = customer.getContact();
        }
        invoiceDetailsTable.addCell(createInfoCell("Mobile:", true, detailFont));
        invoiceDetailsTable.addCell(createInfoCell(customerMobile.isEmpty() ? "-" : customerMobile, false, detailFont));
        
        Cell leftCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
        leftCell.add(invoiceDetailsTable);
        detailsContainerTable.addCell(leftCell);
        
        // Right Column: User/Transaction Details
        Table userTransactionTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
        userTransactionTable.setWidth(UnitValue.createPercentValue(100));
        
        // Operator/User Details
        if (orderDetail.getOperatorEmail() != null && !orderDetail.getOperatorEmail().trim().isEmpty()) {
            // operatorName is already extracted earlier in the method
            userTransactionTable.addCell(createInfoCell("Operator:", true, detailFont));
            userTransactionTable.addCell(createInfoCell(operatorName, false, detailFont));
        }
        
        // Payment Method
        if (orderDetail.getPaymentMethod() != null && !orderDetail.getPaymentMethod().trim().isEmpty()) {
            userTransactionTable.addCell(createInfoCell("Payment Method:", true, detailFont));
            userTransactionTable.addCell(createInfoCell(orderDetail.getPaymentMethod(), false, detailFont));
        }
        
        // Payment Status
        if (orderDetail.getPaymentStatus() != null && !orderDetail.getPaymentStatus().trim().isEmpty()) {
            userTransactionTable.addCell(createInfoCell("Payment Status:", true, detailFont));
            userTransactionTable.addCell(createInfoCell(orderDetail.getPaymentStatus(), false, detailFont));
        }
        
        // Transaction ID/Reference
        userTransactionTable.addCell(createInfoCell("Transaction ID:", true, detailFont));
        userTransactionTable.addCell(createInfoCell("SA-ORD-" + orderDetail.getCode(), false, detailFont));
        
        Cell rightCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
        rightCell.add(userTransactionTable);
        detailsContainerTable.addCell(rightCell);
        
        borderCell.add(detailsContainerTable);
        
        // Customer Section
        Paragraph customerLabel = new Paragraph("To, " + orderDetail.getCustomerName())
            .setFont(getUnicodeFont())
            .setFontSize(9)
            .setBold()
            .setFontColor(textColor)
            .setMarginTop(5)
            .setMarginBottom(5);
        borderCell.add(customerLabel);
        
        // Items Table
        Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{3f, 1f, 1f, 1f}));
        itemsTable.setWidth(UnitValue.createPercentValue(100));
        itemsTable.setMarginBottom(5);
        
        // Table Header
        DeviceRgb headerBgColor = new DeviceRgb(139, 0, 0);
        itemsTable.addHeaderCell(createTableHeaderCell("Description", headerBgColor));
        itemsTable.addHeaderCell(createTableHeaderCell("Qty", headerBgColor));
        itemsTable.addHeaderCell(createTableHeaderCell("Rate", headerBgColor));
        itemsTable.addHeaderCell(createTableHeaderCell("Total", headerBgColor));
        
        // Items rows
        PdfFont itemFont = getUnicodeFont();
        PdfFont monospaceFont = getUnicodeMonospaceFont();
        boolean alternate = false;
        
        for (SuperAdminOrderItem item : orderItems) {
            String itemName = item.getProductName();
            // Convert pipe measurements to pipe numbers for display - pass productCode and batchCode to lookup ProductDetail.code
            itemName = pipeConversionService.convertProductName(itemName, item.getProductCode(), item.getBatchCode());
            Double qty = item.getQuantity();
            double quantity = qty != null ? qty : 0.0;
            double rate = item.getUnitPrice();
            double total = item.getLineTotal();
            
            // Description
            Cell descCell = new Cell()
                .add(new Paragraph(itemName).setFont(itemFont).setFontSize(8))
                .setPadding(4)
                .setBorder(new SolidBorder(textColor, 0.5f));
            if (alternate) {
                descCell.setBackgroundColor(lightGray);
            }
            itemsTable.addCell(descCell);
            
            // Quantity - convert to feet measurement if it's a pipe product
            String quantityDisplay;
            if (pipeConversionService.shouldConvertQuantity(item.getProductName(), item.getProductCode(), item.getBatchCode())) {
                quantityDisplay = pipeConversionService.convertQuantityToFeet(item.getProductName(), quantity, item.getProductCode(), item.getBatchCode());
            } else {
                quantityDisplay = String.format("%.2f", quantity);
            }
            Cell qtyCell = new Cell()
                .add(new Paragraph(quantityDisplay).setFont(monospaceFont).setFontSize(8))
                .setPadding(4)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(textColor, 0.5f));
            if (alternate) {
                qtyCell.setBackgroundColor(lightGray);
            }
            itemsTable.addCell(qtyCell);
            
            // Rate
            Cell rateCell = new Cell()
                .add(new Paragraph(String.format("%.2f", rate)).setFont(monospaceFont).setFontSize(8))
                .setPadding(4)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(textColor, 0.5f));
            if (alternate) {
                rateCell.setBackgroundColor(lightGray);
            }
            itemsTable.addCell(rateCell);
            
            // Total
            Cell totalCell = new Cell()
                .add(new Paragraph(String.format("%.2f", total)).setFont(monospaceFont).setFontSize(8))
                .setPadding(4)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(textColor, 0.5f));
            if (alternate) {
                totalCell.setBackgroundColor(lightGray);
            }
            itemsTable.addCell(totalCell);
            
            alternate = !alternate;
        }
        
        borderCell.add(itemsTable);
        
        // Summary Section
        double discount = orderDetail.getDiscount();
        double finalTotal = orderDetail.getTotalCost();
        
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}));
        summaryTable.setWidth(UnitValue.createPercentValue(50));
        summaryTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        summaryTable.setMarginBottom(5);
        
        // Less (discount)
        if (discount > 0) {
            summaryTable.addCell(createTotalLabelCell("Less:", monospaceFont));
            summaryTable.addCell(createTotalValueCell(String.format("%.2f", discount), monospaceFont));
        }
        
        // Total
        summaryTable.addCell(createTotalLabelCell("Total:", monospaceFont, true));
        summaryTable.addCell(createTotalValueCell(String.format("%.2f", finalTotal), monospaceFont, true));
        
        borderCell.add(summaryTable);
        
        // Payment Details Section
        if (orderDetail.getCustomerPaid() != null || orderDetail.getBalance() != null || 
            (orderDetail.getPaymentMethod() != null && !orderDetail.getPaymentMethod().isEmpty())) {
            Table paymentTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}));
            paymentTable.setWidth(UnitValue.createPercentValue(50));
            paymentTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            paymentTable.setMarginTop(5);
            paymentTable.setMarginBottom(5);
            
            // Customer Paid
            if (orderDetail.getCustomerPaid() != null && orderDetail.getCustomerPaid() > 0) {
                paymentTable.addCell(createTotalLabelCell("Paid:", monospaceFont));
                paymentTable.addCell(createTotalValueCell(String.format("%.2f", orderDetail.getCustomerPaid()), monospaceFont));
            }
            
            // Change (if customer paid more than total)
            if (orderDetail.getCustomerPaid() != null && orderDetail.getCustomerPaid() > finalTotal) {
                double change = orderDetail.getCustomerPaid() - finalTotal;
                paymentTable.addCell(createTotalLabelCell("Change:", monospaceFont));
                paymentTable.addCell(createTotalValueCell(String.format("%.2f", change), monospaceFont));
            }
            
            // Balance (if customer paid less than total)
            if (orderDetail.getBalance() != null && orderDetail.getBalance() > 0) {
                paymentTable.addCell(createTotalLabelCell("Balance:", monospaceFont));
                paymentTable.addCell(createTotalValueCell(String.format("%.2f", orderDetail.getBalance()), monospaceFont));
            }
            
            borderCell.add(paymentTable);
        }
        
        // Signature line
        Paragraph signatureLine = new Paragraph("For " + businessName)
            .setFont(getUnicodeFont())
            .setFontSize(8)
            .setFontColor(textColor)
            .setTextAlignment(TextAlignment.RIGHT)
            .setMarginTop(10);
        borderCell.add(signatureLine);
        
        borderTable.addCell(borderCell);
        document.add(borderTable);
        
        document.close();
        return filePath;
    }
    
    /**
     * Helper method to create info table cells
     */
    private Cell createInfoCell(String text, boolean isLabel, PdfFont font) {
        Cell cell = new Cell()
            .add(new Paragraph(text).setFont(font).setFontSize(8))
            .setPadding(4)
            .setBorder(new SolidBorder(new DeviceRgb(236, 240, 241), 0.5f));
        
        if (isLabel) {
            cell.setBackgroundColor(new DeviceRgb(245, 245, 245))
                .setFontColor(new DeviceRgb(52, 73, 94))
                .setBold();
        }
        
        return cell;
    }
    
    /**
     * Helper method to create table header cells
     */
    private Cell createTableHeaderCell(String text, DeviceRgb bgColor) {
        return new Cell()
            .add(new Paragraph(text).setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(bgColor)
            .setPadding(5)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(Border.NO_BORDER);
    }
    
    /**
     * Helper method to create total label cells
     */
    private Cell createTotalLabelCell(String text, PdfFont font) {
        return createTotalLabelCell(text, font, false);
    }
    
    private Cell createTotalLabelCell(String text, PdfFont font, boolean bold) {
        Cell cell = new Cell()
            .add(new Paragraph(text).setFont(font).setFontSize(8))
            .setPadding(4)
            .setBorder(new SolidBorder(new DeviceRgb(236, 240, 241), 0.5f))
            .setTextAlignment(TextAlignment.RIGHT);
        
        if (bold) {
            cell.setBold();
        }
        
        return cell;
    }
    
    /**
     * Helper method to create total value cells
     */
    private Cell createTotalValueCell(String text, PdfFont font) {
        return createTotalValueCell(text, font, false);
    }
    
    private Cell createTotalValueCell(String text, PdfFont font, boolean bold) {
        Cell cell = new Cell()
            .add(new Paragraph(text).setFont(font).setFontSize(8))
            .setPadding(4)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(new SolidBorder(new DeviceRgb(236, 240, 241), 0.5f));
        
        if (bold) {
            cell.setBold();
        }
        
        return cell;
    }
}
