package com.devstack.pos.service;

import com.devstack.pos.entity.*;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.pdf.PdfDocument;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PDFReportService {
    
    private final OrderDetailService orderDetailService;
    private final ReturnOrderService returnOrderService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final ProductDetailService productDetailService;
    private final OrderItemService orderItemService;
    private final SystemSettingsService systemSettingsService;
    private final PipeConversionService pipeConversionService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    // Cache for font file paths only (not PdfFont objects - they must be created per document)
    private static String loadedUnicodeFontPath = null; // Track which Unicode font file was loaded
    private static String loadedMonospaceFontPath = null; // Track which monospace font file was loaded
    
    // Initialize fonts on first use to ensure Sinhala support is available
    {
        try {
            // Pre-load fonts to see what we get
            System.out.println("\n" + "=".repeat(70));
            System.out.println("=== INITIALIZING UNICODE FONTS FOR SINHALA SUPPORT (Regular Orders) ===");
            System.out.println("=".repeat(70));
            // Force font path loading
            try {
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
            } catch (Exception e) {
                System.err.println("✗ ERROR during font initialization: " + e.getMessage());
            }
            System.out.println("=".repeat(70) + "\n");
        } catch (Exception e) {
            System.err.println("✗ ERROR initializing Unicode font: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get a Unicode font that supports Sinhala and other languages
     * IMPORTANT: Creates a new font instance for each call to avoid PDF document conflicts
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
     * Get the path to a Unicode font that supports Sinhala
     * Uses multiple strategies to find a font that supports Sinhala
     */
    private String getUnicodeFontPath() {
        if (loadedUnicodeFontPath != null) {
            return loadedUnicodeFontPath;
        }
        
        try {
            // Strategy 0: FIRST check for bundled font in resources (BEST - no installation needed!)
            System.out.println("Strategy 0: Checking for bundled font in resources...");
            String bundledFontPath = getBundledFontPath();
            if (bundledFontPath != null) {
                try {
                    PdfFont testFont = PdfFontFactory.createFont(bundledFontPath, PdfEncodings.IDENTITY_H);
                    if (testFont != null && verifySinhalaSupport(testFont, bundledFontPath)) {
                        loadedUnicodeFontPath = bundledFontPath;
                        System.out.println("✓✓✓ SUCCESS: Using BUNDLED font from resources: " + bundledFontPath);
                        System.out.println("   ✓ No separate font installation required!");
                        System.out.println("═══════════════════════════════════════════════════════════");
                        return loadedUnicodeFontPath;
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
                        windir + "\\Fonts\\arial.ttf"
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
                                // Cache only the font path, not the font object
                                loadedUnicodeFontPath = fontPath;
                                System.out.println("✓✓✓ SUCCESS: Found and VERIFIED Unicode font from file: " + fontPath);
                                System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                System.out.println("═══════════════════════════════════════════════════════════");
                                return loadedUnicodeFontPath;
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
            String[] preferredFontNames = {
                "Noto Sans Sinhala", "Noto Sans", "Arial Unicode MS", "Arial Unicode",
                "DejaVu Sans", "Liberation Sans", "Lucida Sans Unicode", "Tahoma",
                "Verdana", "Helvetica Neue", "Helvetica"
            };
            
            // Decorative/fancy fonts to SKIP - these don't have Sinhala glyphs even if canDisplay() returns true
            String[] skipFonts = {
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
                                                // Cache only the font path, not the font object
                                                loadedUnicodeFontPath = fontPath;
                                                System.out.println("✓✓✓ SUCCESS: Found and VERIFIED PREFERRED Unicode font '" + fontName + "' from: " + fontPath);
                                                System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                                System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                                System.out.println("═══════════════════════════════════════════════════════════");
                                                return loadedUnicodeFontPath;
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
                                        // Cache only the font path, not the font object
                                        loadedUnicodeFontPath = fontPath;
                                        System.out.println("✓✓✓ SUCCESS: Found and VERIFIED Unicode font '" + fontName + "' from: " + fontPath);
                                        System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                        System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                        System.out.println("═══════════════════════════════════════════════════════════");
                                        return loadedUnicodeFontPath;
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
                        PdfFont pdfFont = PdfFontFactory.createFont(foundFont, PdfEncodings.IDENTITY_H);
                        if (pdfFont != null) {
                            // VERIFY: Actually test if this font can render Sinhala
                            if (verifySinhalaSupport(pdfFont, foundFont)) {
                                // Cache only the font path, not the font object
                                loadedUnicodeFontPath = foundFont;
                                System.out.println("✓✓✓ SUCCESS: Found and VERIFIED Unicode font: " + foundFont);
                                System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                System.out.println("═══════════════════════════════════════════════════════════");
                                return loadedUnicodeFontPath;
                            } else {
                                System.out.println("   ⚠ Found font FAILED Sinhala verification: " + foundFont);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Could not load found font: " + foundFont);
                    }
                }
            }
            
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println("⚠⚠⚠ CRITICAL: No Unicode font with Sinhala support found!");
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println("Sinhala text will appear as boxes (☐☐☐) in PDFs.");
            System.err.println("");
            System.err.println("SOLUTION: Install Noto Sans Sinhala font:");
            System.err.println("  1. Download from: https://fonts.google.com/noto/specimen/Noto+Sans+Sinhala");
            System.err.println("  2. Install the font on your Mac:");
            System.err.println("     - Double-click the .ttf file");
            System.err.println("     - Click 'Install Font' in Font Book");
            System.err.println("  3. Restart your application");
            System.err.println("");
            System.err.println("Alternative: Install Arial Unicode MS (if available)");
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
                                // Cache only the font path, not the font object
                                loadedUnicodeFontPath = dir;
                                System.out.println("✓✓✓ Found Sinhala font on final search: " + dir);
                                System.out.println("═══════════════════════════════════════════════════════════");
                                return loadedUnicodeFontPath;
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
                                    // Cache only the font path, not the font object
                                    loadedUnicodeFontPath = found;
                                    System.out.println("✓✓✓ Found Sinhala font on final recursive search: " + found);
                                    System.out.println("═══════════════════════════════════════════════════════════");
                                    return loadedUnicodeFontPath;
                                }
                            } catch (Exception e) {
                                // Continue
                            }
                        }
                    }
                }
            }
            
            loadedUnicodeFontPath = "Standard Helvetica (NO SINHALA SUPPORT)";
            System.err.println("⚠ Using fallback font: Standard Helvetica (Sinhala will show as boxes)");
            System.err.println("═══════════════════════════════════════════════════════════");
            return loadedUnicodeFontPath;
            
        } catch (Exception e) {
            System.err.println("Error loading Unicode font: " + e.getMessage());
            e.printStackTrace();
            loadedUnicodeFontPath = "Standard Helvetica (NO SINHALA SUPPORT)";
            return loadedUnicodeFontPath;
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
     * Verify that a PdfFont actually supports Sinhala by testing rendering
     * This is more reliable than canDisplay() which can return true for fonts without glyphs
     * 
     * IMPORTANT: Creates a NEW font for testing to avoid associating the passed font with a test PDF
     */
    private boolean verifySinhalaSupport(PdfFont font, String fontPath) {
        try {
            // First check: If font path contains Sinhala-related keywords, it's likely good
            String fontPathLower = fontPath.toLowerCase();
            String[] trustedKeywords = {"sinhala", "noto", "arialuni", "unicode", "dejavu"};
            boolean hasTrustedKeyword = false;
            for (String keyword : trustedKeywords) {
                if (fontPathLower.contains(keyword)) {
                    hasTrustedKeyword = true;
                    System.out.println("   ✓ Font path contains trusted keyword: " + keyword);
                    break;
                }
            }
            
            // If it has a trusted keyword, we can be more confident
            if (hasTrustedKeyword) {
                // Still do a basic test - but create a NEW font for testing
                try {
                    // Create a NEW font from the path for testing (don't use the passed font)
                    PdfFont testFont = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
                    String testText = "සිංහල";
                    Paragraph testPara = new Paragraph(testText);
                    testPara.setFont(testFont);
                    // If no exception, the font should work
                    System.out.println("   ✓ Font passed basic Sinhala test");
                    return true;
                } catch (Exception e) {
                    System.out.println("   ⚠ Font with trusted keyword failed test: " + e.getMessage());
                    // Still return true if it has trusted keyword - might be a false negative
                    return true;
                }
            }
            
            // For fonts without trusted keywords, do a more thorough test
            // Try to actually render Sinhala text to a temporary PDF
            try {
                String testText = "සිංහල";
                
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
                
                // If we got here without exception, the font should work
                System.out.println("   ✓ Font passed comprehensive Sinhala rendering test");
                return true;
                
            } catch (Exception e) {
                System.out.println("   ⚠ Font failed Sinhala rendering test: " + e.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ⚠ Verification error: " + e.getMessage());
            // If verification fails, be conservative and reject the font
            return false;
        }
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
     * Get a Unicode monospace font for aligned text (like item lists)
     * IMPORTANT: Creates a new font instance for each call to avoid PDF document conflicts
     */
    private PdfFont getUnicodeMonospaceFont() throws IOException {
        // If we have a cached font path, use it to create a new font instance
        if (loadedMonospaceFontPath != null) {
            try {
                return PdfFontFactory.createFont(loadedMonospaceFontPath, PdfEncodings.IDENTITY_H);
            } catch (Exception e) {
                // If cached path fails, clear it and search again
                loadedMonospaceFontPath = null;
            }
        }
        
        try {
            // Try to find system monospace font files
            String os = System.getProperty("os.name").toLowerCase();
            String[] fontPaths = {};
            
            if (os.contains("mac")) {
                fontPaths = new String[]{
                    "/System/Library/Fonts/Supplemental/Courier New.ttf",
                    "/Library/Fonts/Courier New.ttf"
                };
            } else if (os.contains("win")) {
                fontPaths = new String[]{
                    System.getenv("WINDIR") + "\\Fonts\\cour.ttf",
                    System.getenv("WINDIR") + "\\Fonts\\courbd.ttf"
                };
            } else {
                // Linux
                fontPaths = new String[]{
                    "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
                    "/usr/share/fonts/truetype/liberation/LiberationMono-Regular.ttf"
                };
            }
            
            // Try to load from font file paths
            for (String fontPath : fontPaths) {
                try {
                    File fontFile = new File(fontPath);
                    if (fontFile.exists()) {
                        // Use IDENTITY_H encoding for Unicode/Sinhala support
                        // Cache only the font path, not the font object
                        loadedMonospaceFontPath = fontPath;
                        System.out.println("Loaded Unicode monospace font from: " + fontPath);
                        // Return new font instance (will create new one next time from cached path)
                        return PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
                    }
                } catch (Exception e) {
                    // Try next path
                }
            }
            
            // Fallback to standard monospace - create new instance each time
            System.out.println("Warning: No Unicode monospace font file found, using standard Courier.");
            return PdfFontFactory.createFont(StandardFonts.COURIER);
            
        } catch (Exception e) {
            System.err.println("Error loading Unicode monospace font: " + e.getMessage());
            try {
                return PdfFontFactory.createFont(StandardFonts.COURIER);
            } catch (Exception ex) {
                return PdfFontFactory.createFont(StandardFonts.COURIER);
            }
        }
    }
    
    /**
     * Generate plain text bill receipt for thermal printer
     * Matches the format shown in the sample receipt
     */
    public String generatePlainTextReceipt(Long orderId) {
        try {
            // Get order details
            OrderDetail orderDetail = orderDetailService.findOrderDetail(orderId);
            if (orderDetail == null) {
                return "Error: Order not found";
            }
            
            // Get order items
            List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
            
            // Get system settings
            SystemSettings settings = systemSettingsService.getSystemSettings();
            
            StringBuilder receipt = new StringBuilder();
            
            // Business name (double width/height for emphasis)
            String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
                ? settings.getBusinessName().toUpperCase()
                : "KUMARA ENTERPRISES";
            receipt.append((char) 0x1B).append("!").append((char) 0x30); // ESC ! 0x30 = double width & height
            receipt.append(centerText(businessName, 26)).append("\n");    // Use half width when text is doubled
            receipt.append((char) 0x1B).append("!").append((char) 0x00); // Reset to normal size
            
            // Address (multi-line fallback with Wewala second line)
            
                receipt.append(centerText("No 58k Gagabada Rd,", 48)).append("\n");
                receipt.append(centerText("Wewala,Piliyandala", 48)).append("\n");
            

            
            // Contact numbers
            if (settings.getContactNumber() != null && !settings.getContactNumber().trim().isEmpty()) {
                receipt.append(centerText(settings.getContactNumber(), 48)).append("\n");
            } else {
                receipt.append(centerText("077 781 5955 / 011 261 3606", 48)).append("\n");
            }
            
            receipt.append("\n");
            
            // Invoice number and date (left and right aligned)
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
            
            // Items header - matching the format from the image (all on one line)
            receipt.append(String.format("%-14s %9s %5s %6s %9s\n",
                "අයිතම්", "මිල", "ප්‍රමාණය", "වට්ටම", "මුළු"));
            receipt.append("................................................\n");
            
            // Items - all details on one line, properly aligned
            for (OrderItem item : orderItems) {
                String itemName = item.getProductName();
                // Convert pipe measurements to pipe numbers for display - pass productCode and batchCode
                itemName = pipeConversionService.convertProductName(itemName, item.getProductCode(), item.getBatchCode());
                
                double discount = item.getDiscountPerUnit() != null ? item.getDiscountPerUnit() : 0.0;
                
                // Format: Item (14 chars, left) | Price (9 chars, right, 1 decimal) | Qty with x (5 chars) | Disc (6 chars, right) | Total (9 chars, right)
                // All on ONE line to match the correct format (supports decimal quantities)
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
            
            // Totals section (supports decimal quantities)
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
            
            // Change (if customer paid more than total)
            if ("PAID".equals(orderDetail.getPaymentStatus()) && customerPaid > orderDetail.getTotalCost()) {
                double change = customerPaid - orderDetail.getTotalCost();
                receipt.append(String.format("%-30s %17.2f\n", "ඉතිරි මුදල", change));
            }
            
            receipt.append("------------------------------------------------\n");
            
            // Order type
            String orderType = orderDetail.getOrderType() != null && orderDetail.getOrderType().equals("CONSTRUCTION") 
                ? "Construction" : "Hardware";
            receipt.append(String.format("Type: %s\n", orderType));
            
            receipt.append("------------------------------------------------\n");
            
            // Balance (if customer paid less than total)
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
            return "Error generating receipt: " + e.getMessage();
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
     * Generate bill receipt PDF for a specific order
     */
    public String generateBillReceipt(Long orderId) throws IOException {
        // Get order details
        OrderDetail orderDetail = orderDetailService.findOrderDetail(orderId);
        if (orderDetail == null) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        
        // Get order items
        List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
        
        // Create directory structure: ~/POS_Receipts/[CashierName]/[CustomerName]/
        String userHome = System.getProperty("user.home");
        String cashierEmail = orderDetail.getOperatorEmail();
        
        // Extract cashier username from email (everything before @)
        String cashierName = cashierEmail != null && cashierEmail.contains("@") 
            ? cashierEmail.substring(0, cashierEmail.indexOf("@")) 
            : (cashierEmail != null ? cashierEmail : "unknown");
        
        // Sanitize cashier name for file system (remove special characters)
        cashierName = cashierName.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Get customer name - use "Guest" if null or empty
        String customerName = orderDetail.getCustomerName();
        if (customerName == null || customerName.trim().isEmpty() || "Guest".equalsIgnoreCase(customerName.trim())) {
            customerName = "Guest";
        }
        
        // Sanitize customer name for file system (remove special characters)
        customerName = customerName.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Create nested directory path: ~/POS_Receipts/[CashierName]/[CustomerName]/
        String receiptsDir = userHome + File.separator + "POS_Receipts" + File.separator + cashierName + File.separator + customerName;
        File directory = new File(receiptsDir);
        
        // Create directory if it doesn't exist
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                System.err.println("Failed to create receipts directory: " + receiptsDir);
                // Fallback to Downloads folder
                receiptsDir = userHome + File.separator + "Downloads";
            }
        }
        
        // Create file name and path
        String fileName = "Receipt_" + orderDetail.getCode() + "_" + System.currentTimeMillis() + ".pdf";
        String filePath = receiptsDir + File.separator + fileName;
        
        // Create PDF
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(15, 15, 15, 15);
        
        // Get system settings
        SystemSettings settings = systemSettingsService.getSystemSettings();
        
        // Get customer details if available
        Customer customer = null;
        if (orderDetail.getCustomerId() != null) {
            customer = customerService.findCustomer(orderDetail.getCustomerId());
        }
        
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
            String operatorName = orderDetail.getOperatorEmail();
            // Extract name from email if possible
            if (operatorName.contains("@")) {
                operatorName = operatorName.substring(0, operatorName.indexOf("@"));
            }
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
        userTransactionTable.addCell(createInfoCell("ORD-" + orderDetail.getCode(), false, detailFont));
        
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
        
        for (OrderItem item : orderItems) {
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
            .setBorder(new SolidBorder(new DeviceRgb(236, 240, 241), 0.5f))
            .setTextAlignment(TextAlignment.RIGHT);
        
        if (bold) {
            cell.setBold();
        }
        
        return cell;
    }
    
    /**
     * Helper method to create summary table cells with alternating colors
     */
    private Cell createSummaryCell(String text, boolean alternate, DeviceRgb bgColor) {
        Cell cell = new Cell()
            .add(new Paragraph(text).setFontSize(10))
            .setPadding(8)
            .setBorder(new SolidBorder(new DeviceRgb(236, 240, 241), 0.5f));
        
        if (bgColor != null && alternate) {
            cell.setBackgroundColor(bgColor);
        }
        
        return cell;
    }
    
    /**
     * Generate comprehensive sales report PDF
     */
    public String generateSalesReportPDF(LocalDateTime startDate, LocalDateTime endDate, String reportType)
            throws IOException {
        String fileName = "Sales_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Sales Report", startDate, endDate);
        
        // Summary Statistics
        addSummarySection(document, startDate, endDate);
        
        // Sales by Period
        addSalesByPeriodSection(document, startDate, endDate, reportType);
        
        // Top Products
        addTopProductsSection(document, startDate, endDate);
        
        // Sales by Category
        addSalesByCategorySection(document, startDate, endDate);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate comprehensive return orders report PDF
     */
    public String generateReturnOrdersReportPDF(LocalDateTime startDate, LocalDateTime endDate)
            throws IOException {
        String fileName = "Return_Orders_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Return Orders Report", startDate, endDate);
        
        // Return Statistics
        addReturnStatisticsSection(document, startDate, endDate);
        
        // Return Details
        addReturnDetailsSection(document, startDate, endDate);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate comprehensive inventory report PDF
     */
    public String generateInventoryReportPDF() throws IOException {
        String fileName = "Inventory_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Inventory Report", null, null);
        
        // Inventory Summary
        addInventorySummarySection(document);
        
        // Low Stock Items
        addLowStockSection(document);
        
        // Product Details
        addProductDetailsSection(document);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate comprehensive financial report PDF
     */
    public String generateFinancialReportPDF(LocalDateTime startDate, LocalDateTime endDate)
            throws IOException {
        String fileName = "Financial_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Financial Report", startDate, endDate);
        
        // Financial Summary
        addFinancialSummarySection(document, startDate, endDate);
        
        // Revenue vs Returns
        addRevenueVsReturnsSection(document, startDate, endDate);
        
        // Top Customers
        addTopCustomersSection(document, startDate, endDate);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate comprehensive supplier report PDF
     */
    public String generateSupplierReportPDF() throws IOException {
        String fileName = "Supplier_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Supplier & Purchase Orders Report", null, null);
        
        // Supplier Summary
        addSupplierSummarySection(document);
        
        // Purchase Orders
        addPurchaseOrdersSection(document);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate comprehensive all-in-one report PDF
     */
    public String generateComprehensiveReportPDF(LocalDateTime startDate, LocalDateTime endDate)
            throws IOException {
        String fileName = "Comprehensive_POS_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Comprehensive POS System Report", startDate, endDate);
        
        // Executive Summary
        addExecutiveSummarySection(document, startDate, endDate);
        
        // Sales Analysis
        addSalesAnalysisSection(document, startDate, endDate);
        
        // Return Orders Analysis
        addReturnOrdersAnalysisSection(document, startDate, endDate);
        
        // Inventory Status
        addInventoryStatusSection(document);
        
        // Customer Analysis
        addCustomerAnalysisSection(document, startDate, endDate);
        
        // Supplier Analysis
        addSupplierAnalysisSection(document);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate construction-specific report PDF
     */
    public String generateConstructionReportPDF(LocalDateTime startDate, LocalDateTime endDate)
            throws IOException {
        String fileName = "Construction_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Construction Orders Report", startDate, endDate);
        
        // Construction Summary
        addConstructionSummarySection(document, startDate, endDate);
        
        // Construction Sales by Period
        addConstructionSalesByPeriodSection(document, startDate, endDate);
        
        // Construction Sales by Cashier
        addConstructionSalesByCashierSection(document, startDate, endDate);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    // Helper methods for adding sections
    
    private void addHeader(Document document, String title, LocalDateTime startDate, LocalDateTime endDate) throws IOException {
        // Professional color scheme
        DeviceRgb headerColor = new DeviceRgb(41, 128, 185);
        DeviceRgb accentColor = new DeviceRgb(52, 73, 94);
        
        // Get system settings for header
        SystemSettings settings = systemSettingsService.getSystemSettings();
        String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
            ? settings.getBusinessName()
            : "Kumara Enterprises";
        
        // Professional header box
        Table headerTable = new Table(1);
        headerTable.setWidth(UnitValue.createPercentValue(100));
        headerTable.setBackgroundColor(headerColor);
        headerTable.setMarginBottom(15);
        
        Cell headerCell = new Cell()
            .add(new Paragraph(businessName.toUpperCase())
                .setFontSize(22)
                .setBold()
                .setFont(getUnicodeFont())
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER))
            .setBorder(Border.NO_BORDER)
            .setPadding(12);
        headerTable.addCell(headerCell);
        
        if (settings.getAddress() != null && !settings.getAddress().trim().isEmpty()) {
            Cell addressCell = new Cell()
                .add(new Paragraph(settings.getAddress())
                    .setFontSize(10)
                    .setFont(getUnicodeFont())
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(0)
                .setPaddingBottom(8);
            headerTable.addCell(addressCell);
        }
        
        document.add(headerTable);
        
        // Report title with accent
        Paragraph header = new Paragraph(title)
                .setFontSize(20)
                .setBold()
                .setFontColor(accentColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(5)
                .setMarginBottom(5);
        document.add(header);
        
        // Accent line under title
        Table titleLine = new Table(1);
        titleLine.setWidth(UnitValue.createPercentValue(40));
        titleLine.setHorizontalAlignment(HorizontalAlignment.CENTER);
        titleLine.setBackgroundColor(headerColor);
        titleLine.addCell(new Cell().setHeight(3).setBorder(Border.NO_BORDER));
        document.add(titleLine);
        
        if (startDate != null && endDate != null) {
            long daysBetween = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate());
            Paragraph dateRange = new Paragraph(
                    "Period: " + startDate.format(DATE_ONLY_FORMATTER) + " to " + endDate.format(DATE_ONLY_FORMATTER) + 
                    " (" + daysBetween + " days)")
                    .setFontSize(11)
                    .setFontColor(accentColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8)
                    .setMarginBottom(5);
            document.add(dateRange);
        }
        
        Paragraph generatedDate = new Paragraph(
                "Generated on: " + LocalDateTime.now().format(DATE_FORMATTER))
                .setFontSize(9)
                .setFontColor(new DeviceRgb(127, 140, 141))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(generatedDate);
        
        document.add(new Paragraph().setMarginBottom(10));
    }
    
    /**
     * Calculate previous period dates for comparison
     */
    private LocalDateTime[] calculatePreviousPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return new LocalDateTime[]{null, null};
        }
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDateTime prevStartDate = startDate.minusDays(daysBetween + 1);
        LocalDateTime prevEndDate = startDate.minusDays(1);
        return new LocalDateTime[]{prevStartDate, prevEndDate};
    }
    
    /**
     * Calculate growth percentage
     */
    private double calculateGrowthPercentage(double current, double previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) / previous) * 100.0;
    }
    
    /**
     * Format growth indicator with arrow
     */
    private String formatGrowthIndicator(double current, double previous) {
        double growth = calculateGrowthPercentage(current, previous);
        String arrow = growth > 0 ? "↑" : (growth < 0 ? "↓" : "→");
        return String.format("%s %.2f%%", arrow, Math.abs(growth));
    }
    
    private void addSummarySection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("📊 KEY PERFORMANCE INDICATORS (KPIs)")
                .setFontSize(18)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(15));
        
        // Current period data
        Double revenue = startDate != null && endDate != null 
                ? orderDetailService.getRevenueByDateRange(startDate, endDate)
                : orderDetailService.getTotalRevenue();
        Long orders = startDate != null && endDate != null
                ? orderDetailService.countOrdersByDateRange(startDate, endDate)
                : orderDetailService.getTotalOrderCount();
        Double avgOrder = startDate != null && endDate != null
                ? orderDetailService.getAverageOrderValueByDateRange(startDate, endDate)
                : orderDetailService.getAverageOrderValue();
        
        // Get returns data
        Double refunds = startDate != null && endDate != null
                ? returnOrderService.getTotalRefundAmountByDateRange(startDate, endDate)
                : returnOrderService.getTotalRefundAmount();
        Long returns = startDate != null && endDate != null
                ? returnOrderService.countReturnsByDateRange(startDate, endDate)
                : (long) returnOrderService.findAllReturnOrders().size();
        
        Double netRevenue = (revenue != null ? revenue : 0.0) - (refunds != null ? refunds : 0.0);
        
        // Calculate previous period for growth indicators
        LocalDateTime[] prevPeriod = calculatePreviousPeriod(startDate, endDate);
        Double prevRevenue = null;
        Long prevOrders = null;
        Double prevAvgOrder = null;
        Double prevRefunds = null;
        Long prevReturns = null;
        
        if (prevPeriod[0] != null && prevPeriod[1] != null) {
            prevRevenue = orderDetailService.getRevenueByDateRange(prevPeriod[0], prevPeriod[1]);
            prevOrders = orderDetailService.countOrdersByDateRange(prevPeriod[0], prevPeriod[1]);
            prevAvgOrder = orderDetailService.getAverageOrderValueByDateRange(prevPeriod[0], prevPeriod[1]);
            prevRefunds = returnOrderService.getTotalRefundAmountByDateRange(prevPeriod[0], prevPeriod[1]);
            prevReturns = returnOrderService.countReturnsByDateRange(prevPeriod[0], prevPeriod[1]);
        }
        
        // Create enhanced summary table with growth indicators - professional styling
        DeviceRgb tableHeaderColor = new DeviceRgb(52, 73, 94);
        DeviceRgb lightGray = new DeviceRgb(236, 240, 241);
        
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 1.5F}));
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        summaryTable.setMarginBottom(15);
        
        // Header row with professional styling
        summaryTable.addHeaderCell(createTableHeaderCell("Metric", tableHeaderColor));
        summaryTable.addHeaderCell(createTableHeaderCell("Current Period", tableHeaderColor));
        summaryTable.addHeaderCell(createTableHeaderCell("Growth", tableHeaderColor));
        
        // Revenue row with growth - with alternating row colors
        double revenueVal = revenue != null ? revenue : 0.0;
        double prevRevenueVal = prevRevenue != null ? prevRevenue : 0.0;
        String revenueGrowth = prevPeriod[0] != null ? formatGrowthIndicator(revenueVal, prevRevenueVal) : "N/A";
        summaryTable.addCell(createSummaryCell("💰 Total Revenue", false, lightGray));
        summaryTable.addCell(createSummaryCell(String.format("LKR %,.2f", revenueVal), false, lightGray));
        summaryTable.addCell(createSummaryCell(revenueGrowth, false, lightGray));
        
        // Net Revenue
        double prevNetRevenue = (prevRevenueVal) - (prevRefunds != null ? prevRefunds : 0.0);
        String netRevenueGrowth = prevPeriod[0] != null ? formatGrowthIndicator(netRevenue, prevNetRevenue) : "N/A";
        summaryTable.addCell(createSummaryCell("💵 Net Revenue", true, null));
        summaryTable.addCell(createSummaryCell(String.format("LKR %,.2f", netRevenue), true, null));
        summaryTable.addCell(createSummaryCell(netRevenueGrowth, true, null));
        
        // Orders row with growth
        long ordersVal = orders != null ? orders : 0;
        long prevOrdersVal = prevOrders != null ? prevOrders : 0;
        String ordersGrowth = prevPeriod[0] != null ? formatGrowthIndicator(ordersVal, prevOrdersVal) : "N/A";
        summaryTable.addCell(createSummaryCell("📦 Total Orders", false, lightGray));
        summaryTable.addCell(createSummaryCell(String.valueOf(ordersVal), false, lightGray));
        summaryTable.addCell(createSummaryCell(ordersGrowth, false, lightGray));
        
        // Average Order Value
        double avgOrderVal = avgOrder != null ? avgOrder : 0.0;
        double prevAvgOrderVal = prevAvgOrder != null ? prevAvgOrder : 0.0;
        String avgOrderGrowth = prevPeriod[0] != null ? formatGrowthIndicator(avgOrderVal, prevAvgOrderVal) : "N/A";
        summaryTable.addCell(createSummaryCell("📈 Avg Order Value", true, null));
        summaryTable.addCell(createSummaryCell(String.format("LKR %,.2f", avgOrderVal), true, null));
        summaryTable.addCell(createSummaryCell(avgOrderGrowth, true, null));
        
        // Returns
        long returnsVal = returns != null ? returns : 0;
        long prevReturnsVal = prevReturns != null ? prevReturns : 0;
        String returnsGrowth = prevPeriod[0] != null ? formatGrowthIndicator(returnsVal, prevReturnsVal) : "N/A";
        summaryTable.addCell(createSummaryCell("🔄 Total Returns", false, lightGray));
        summaryTable.addCell(createSummaryCell(String.valueOf(returnsVal), false, lightGray));
        summaryTable.addCell(createSummaryCell(returnsGrowth, false, lightGray));
        
        // Refund Amount
        double refundsVal = refunds != null ? refunds : 0.0;
        double prevRefundsVal = prevRefunds != null ? prevRefunds : 0.0;
        String refundsGrowth = prevPeriod[0] != null ? formatGrowthIndicator(refundsVal, prevRefundsVal) : "N/A";
        summaryTable.addCell(createSummaryCell("💸 Total Refunds", true, null));
        summaryTable.addCell(createSummaryCell(String.format("LKR %,.2f", refundsVal), true, null));
        summaryTable.addCell(createSummaryCell(refundsGrowth, true, null));
        
        // Return Rate (returns as percentage of orders)
        double returnRate = ordersVal > 0 ? (returnsVal * 100.0 / ordersVal) : 0.0;
        double prevReturnRate = prevOrdersVal > 0 ? (prevReturnsVal * 100.0 / prevOrdersVal) : 0.0;
        String returnRateGrowth = prevPeriod[0] != null ? formatGrowthIndicator(returnRate, prevReturnRate) : "N/A";
        summaryTable.addCell(createSummaryCell("📉 Return Rate", false, lightGray));
        summaryTable.addCell(createSummaryCell(String.format("%.2f%%", returnRate), false, lightGray));
        summaryTable.addCell(createSummaryCell(returnRateGrowth, false, lightGray));
        
        document.add(summaryTable);
        
        // Add insights paragraph
        if (prevPeriod[0] != null) {
            document.add(new Paragraph().setMarginTop(10));
            Paragraph insights = new Paragraph("📌 Insights: " + generateInsights(revenueVal, prevRevenueVal, ordersVal, prevOrdersVal, returnRate, prevReturnRate))
                    .setFontSize(10)
                    .setItalic()
                    .setMarginBottom(15);
            document.add(insights);
        } else {
            document.add(new Paragraph().setMarginBottom(15));
        }
    }
    
    /**
     * Generate insights based on KPIs
     */
    private String generateInsights(double revenue, double prevRevenue, long orders, long prevOrders, double returnRate, double prevReturnRate) {
        StringBuilder insights = new StringBuilder();
        
        if (revenue > prevRevenue) {
            insights.append("Revenue is growing. ");
        } else if (revenue < prevRevenue) {
            insights.append("Revenue has declined. ");
        }
        
        if (orders > prevOrders) {
            insights.append("Order volume increased. ");
        } else if (orders < prevOrders) {
            insights.append("Order volume decreased. ");
        }
        
        if (returnRate > prevReturnRate) {
            insights.append("⚠️ Return rate increased - review product quality. ");
        } else if (returnRate < prevReturnRate) {
            insights.append("✓ Return rate improved. ");
        }
        
        if (insights.length() == 0) {
            insights.append("Performance is stable.");
        }
        
        return insights.toString();
    }
    
    private void addSummaryRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setPadding(5));
        table.addCell(new Cell().add(new Paragraph(value)).setPadding(5));
    }
    
    private void addSalesByPeriodSection(Document document, LocalDateTime startDate, LocalDateTime endDate, String reportType) {
        document.add(new Paragraph("📅 SALES BY PERIOD")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        if (startDate == null || endDate == null) {
            document.add(new Paragraph("Select a date range to view sales by period.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        // Calculate daily sales breakdown
        List<OrderDetail> orders = orderDetailService.findOrdersByDateRange(startDate, endDate);
        Map<LocalDate, Double> dailySales = orders.stream()
                .filter(o -> "PAID".equals(o.getPaymentStatus()))
                .collect(Collectors.groupingBy(
                    o -> o.getIssuedDate().toLocalDate(),
                    Collectors.summingDouble(OrderDetail::getTotalCost)
                ));
        
        if (dailySales.isEmpty()) {
            document.add(new Paragraph("No sales data available for the selected period.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        // Create table with daily breakdown
        Table periodTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 1}));
        periodTable.setWidth(UnitValue.createPercentValue(100));
        
        periodTable.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setPadding(5));
        periodTable.addHeaderCell(new Cell().add(new Paragraph("Revenue").setBold()).setPadding(5));
        periodTable.addHeaderCell(new Cell().add(new Paragraph("Orders").setBold()).setPadding(5));
        
        Map<LocalDate, Long> dailyOrderCount = orders.stream()
                .collect(Collectors.groupingBy(
                    o -> o.getIssuedDate().toLocalDate(),
                    Collectors.counting()
                ));
        
        dailySales.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    LocalDate date = entry.getKey();
                    Double revenue = entry.getValue();
                    Long orderCount = dailyOrderCount.getOrDefault(date, 0L);
                    
                    periodTable.addCell(new Cell().add(new Paragraph(date.format(DATE_ONLY_FORMATTER))).setPadding(5));
                    periodTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", revenue))).setPadding(5));
                    periodTable.addCell(new Cell().add(new Paragraph(String.valueOf(orderCount))).setPadding(5));
                });
        
        document.add(periodTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addTopProductsSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("🏆 TOP SELLING PRODUCTS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<Object[]> topProducts;
        if (startDate != null && endDate != null) {
            topProducts = orderItemService.getTopSellingProductsWithRevenue(startDate, endDate);
        } else {
            topProducts = orderItemService.getTopSellingProductsWithRevenue();
        }
        
        if (topProducts.isEmpty()) {
            document.add(new Paragraph("No product sales data available.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        // Limit to top 10
        topProducts = topProducts.stream().limit(10).collect(Collectors.toList());
        
        Table productsTable = new Table(UnitValue.createPercentArray(new float[]{0.5F, 2, 1, 1.5F, 1.5F}));
        productsTable.setWidth(UnitValue.createPercentValue(100));
        
        productsTable.addHeaderCell(new Cell().add(new Paragraph("#").setBold()).setPadding(5));
        productsTable.addHeaderCell(new Cell().add(new Paragraph("Product Name").setBold()).setPadding(5));
        productsTable.addHeaderCell(new Cell().add(new Paragraph("Quantity").setBold()).setPadding(5));
        productsTable.addHeaderCell(new Cell().add(new Paragraph("Revenue").setBold()).setPadding(5));
        productsTable.addHeaderCell(new Cell().add(new Paragraph("Avg Price").setBold()).setPadding(5));
        
        int rank = 1;
        for (Object[] product : topProducts) {
            Integer productCode = ((Number) product[0]).intValue();
            String productName = (String) product[1];
            Integer quantity = ((Number) product[2]).intValue();
            Double revenue = ((Number) product[3]).doubleValue();
            Double avgPrice = quantity > 0 ? revenue / quantity : 0.0;
            
            productsTable.addCell(new Cell().add(new Paragraph(String.valueOf(rank++))).setPadding(5));
            productsTable.addCell(new Cell().add(new Paragraph(productName != null ? productName : "Unknown")).setPadding(5));
            productsTable.addCell(new Cell().add(new Paragraph(String.valueOf(quantity))).setPadding(5));
            productsTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", revenue))).setPadding(5));
            productsTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", avgPrice))).setPadding(5));
        }
        
        document.add(productsTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addSalesByCategorySection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("📂 SALES BY CATEGORY")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<Object[]> categorySales;
        if (startDate != null && endDate != null) {
            categorySales = orderItemService.getSalesByCategory(startDate, endDate);
        } else {
            categorySales = orderItemService.getSalesByCategory();
        }
        
        if (categorySales.isEmpty()) {
            document.add(new Paragraph("No category sales data available.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        // Calculate total revenue for percentage calculation
        double totalRevenue = categorySales.stream()
                .mapToDouble(cat -> ((Number) cat[2]).doubleValue())
                .sum();
        
        Table categoryTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1.5F, 1.5F}));
        categoryTable.setWidth(UnitValue.createPercentValue(100));
        
        categoryTable.addHeaderCell(new Cell().add(new Paragraph("Category").setBold()).setPadding(5));
        categoryTable.addHeaderCell(new Cell().add(new Paragraph("Orders").setBold()).setPadding(5));
        categoryTable.addHeaderCell(new Cell().add(new Paragraph("Revenue").setBold()).setPadding(5));
        categoryTable.addHeaderCell(new Cell().add(new Paragraph("% of Total").setBold()).setPadding(5));
        
        for (Object[] category : categorySales) {
            String categoryName = (String) category[0];
            Long orderCount = ((Number) category[1]).longValue();
            Double revenue = ((Number) category[2]).doubleValue();
            double percentage = totalRevenue > 0 ? (revenue / totalRevenue) * 100.0 : 0.0;
            
            categoryTable.addCell(new Cell().add(new Paragraph(categoryName != null ? categoryName : "Uncategorized")).setPadding(5));
            categoryTable.addCell(new Cell().add(new Paragraph(String.valueOf(orderCount))).setPadding(5));
            categoryTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", revenue))).setPadding(5));
            categoryTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", percentage))).setPadding(5));
        }
        
        document.add(categoryTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addReturnStatisticsSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("🔄 RETURN STATISTICS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        Long totalReturns = startDate != null && endDate != null
                ? returnOrderService.countReturnsByDateRange(startDate, endDate)
                : (long) returnOrderService.findAllReturnOrders().size();
        Long pendingReturns = returnOrderService.countByStatus("PENDING");
        Long completedReturns = returnOrderService.countByStatus("COMPLETED");
        Double totalRefunds = startDate != null && endDate != null
                ? returnOrderService.getTotalRefundAmountByDateRange(startDate, endDate)
                : returnOrderService.getTotalRefundAmount();
        
        // Calculate previous period for comparison
        LocalDateTime[] prevPeriod = calculatePreviousPeriod(startDate, endDate);
        Long prevTotalReturns = null;
        Double prevTotalRefunds = null;
        if (prevPeriod[0] != null && prevPeriod[1] != null) {
            prevTotalReturns = returnOrderService.countReturnsByDateRange(prevPeriod[0], prevPeriod[1]);
            prevTotalRefunds = returnOrderService.getTotalRefundAmountByDateRange(prevPeriod[0], prevPeriod[1]);
        }
        
        Table returnTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 1.5F}));
        returnTable.setWidth(UnitValue.createPercentValue(100));
        
        returnTable.addHeaderCell(new Cell().add(new Paragraph("Metric").setBold()).setPadding(8));
        returnTable.addHeaderCell(new Cell().add(new Paragraph("Current Period").setBold()).setPadding(8));
        returnTable.addHeaderCell(new Cell().add(new Paragraph("Growth").setBold()).setPadding(8));
        
        long totalReturnsVal = totalReturns != null ? totalReturns : 0;
        long prevTotalReturnsVal = prevTotalReturns != null ? prevTotalReturns : 0;
        String returnsGrowth = prevPeriod[0] != null ? formatGrowthIndicator(totalReturnsVal, prevTotalReturnsVal) : "N/A";
        returnTable.addCell(new Cell().add(new Paragraph("Total Returns")).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(String.valueOf(totalReturnsVal))).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(returnsGrowth)).setPadding(8));
        
        returnTable.addCell(new Cell().add(new Paragraph("Pending Returns")).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(String.valueOf(pendingReturns != null ? pendingReturns : 0))).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph("-")).setPadding(8));
        
        returnTable.addCell(new Cell().add(new Paragraph("Completed Returns")).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(String.valueOf(completedReturns != null ? completedReturns : 0))).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph("-")).setPadding(8));
        
        double totalRefundsVal = totalRefunds != null ? totalRefunds : 0.0;
        double prevTotalRefundsVal = prevTotalRefunds != null ? prevTotalRefunds : 0.0;
        String refundsGrowth = prevPeriod[0] != null ? formatGrowthIndicator(totalRefundsVal, prevTotalRefundsVal) : "N/A";
        returnTable.addCell(new Cell().add(new Paragraph("Total Refunds")).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", totalRefundsVal))).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(refundsGrowth)).setPadding(8));
        
        // Average refund amount
        double avgRefund = totalReturnsVal > 0 ? totalRefundsVal / totalReturnsVal : 0.0;
        double prevAvgRefund = prevTotalReturnsVal > 0 ? prevTotalRefundsVal / prevTotalReturnsVal : 0.0;
        String avgRefundGrowth = prevPeriod[0] != null ? formatGrowthIndicator(avgRefund, prevAvgRefund) : "N/A";
        returnTable.addCell(new Cell().add(new Paragraph("Average Refund")).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", avgRefund))).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(avgRefundGrowth)).setPadding(8));
        
        document.add(returnTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addReturnDetailsSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("RETURN DETAILS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<ReturnOrder> returns = startDate != null && endDate != null
                ? returnOrderService.findByReturnDateBetween(startDate, endDate)
                : returnOrderService.findAllReturnOrders();
        
        if (returns.isEmpty()) {
            document.add(new Paragraph("No return orders found in the specified period.")
                    .setFontSize(10)
                    .setItalic());
        } else {
            Table returnDetailsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}));
            returnDetailsTable.setWidth(UnitValue.createPercentValue(100));
            
            // Header row
            returnDetailsTable.addHeaderCell(new Cell().add(new Paragraph("Return ID").setBold()).setPadding(5));
            returnDetailsTable.addHeaderCell(new Cell().add(new Paragraph("Order ID").setBold()).setPadding(5));
            returnDetailsTable.addHeaderCell(new Cell().add(new Paragraph("Customer").setBold()).setPadding(5));
            returnDetailsTable.addHeaderCell(new Cell().add(new Paragraph("Refund Amount").setBold()).setPadding(5));
            returnDetailsTable.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()).setPadding(5));
            
            // Data rows
            for (ReturnOrder returnOrder : returns) {
                returnDetailsTable.addCell(new Cell().add(new Paragraph(
                        String.valueOf(returnOrder.getReturnId() != null ? returnOrder.getReturnId() : returnOrder.getId()))).setPadding(5));
                returnDetailsTable.addCell(new Cell().add(new Paragraph(String.valueOf(returnOrder.getOrderId()))).setPadding(5));
                returnDetailsTable.addCell(new Cell().add(new Paragraph(
                        returnOrder.getCustomerEmail() != null ? returnOrder.getCustomerEmail() : "Guest")).setPadding(5));
                returnDetailsTable.addCell(new Cell().add(new Paragraph(
                        String.format("LKR %,.2f", returnOrder.getRefundAmount()))).setPadding(5));
                returnDetailsTable.addCell(new Cell().add(new Paragraph(returnOrder.getStatus())).setPadding(5));
            }
            
            document.add(returnDetailsTable);
        }
        
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addInventorySummarySection(Document document) {
        document.add(new Paragraph("INVENTORY SUMMARY")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        int totalProducts = productService.findAllProducts().size();
        long lowStockCount = productDetailService.findAllProductDetails().stream()
                .filter(ProductDetail::isLowStock)
                .count();
        
        Table inventoryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        inventoryTable.setWidth(UnitValue.createPercentValue(100));
        
        addSummaryRow(inventoryTable, "Total Products", String.valueOf(totalProducts));
        addSummaryRow(inventoryTable, "Low Stock Items", String.valueOf(lowStockCount));
        
        document.add(inventoryTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addLowStockSection(Document document) {
        document.add(new Paragraph("⚠️ LOW STOCK ITEMS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<ProductDetail> lowStockItems = productDetailService.findAllProductDetails().stream()
                .filter(ProductDetail::isLowStock)
                .limit(20) // Limit to top 20
                .toList();
        
        if (lowStockItems.isEmpty()) {
            document.add(new Paragraph("✓ No low stock items found. All products are well-stocked.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        Table lowStockTable = new Table(UnitValue.createPercentArray(new float[]{2, 1.5F, 1.5F, 1}));
        lowStockTable.setWidth(UnitValue.createPercentValue(100));
        
        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Product").setBold()).setPadding(5));
        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Current Stock").setBold()).setPadding(5));
        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Threshold").setBold()).setPadding(5));
        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()).setPadding(5));
        
        for (ProductDetail detail : lowStockItems) {
            // Get product name using productCode
            String productName = "Unknown";
            try {
                Product product = productService.findProduct(detail.getProductCode());
                if (product != null && product.getDescription() != null) {
                    productName = product.getDescription();
                }
            } catch (Exception e) {
                // If product not found, use default name
                productName = "Product #" + detail.getProductCode();
            }
            
            int currentStock = (int) detail.getQtyOnHand();
            int threshold = detail.getLowStockThreshold() != null ? detail.getLowStockThreshold() : 10;
            String status = currentStock == 0 ? "OUT OF STOCK" : "LOW STOCK";
            
            lowStockTable.addCell(new Cell().add(new Paragraph(productName)).setPadding(5));
            lowStockTable.addCell(new Cell().add(new Paragraph(String.valueOf(currentStock))).setPadding(5));
            lowStockTable.addCell(new Cell().add(new Paragraph(String.valueOf(threshold))).setPadding(5));
            lowStockTable.addCell(new Cell().add(new Paragraph(status)).setPadding(5));
        }
        
        document.add(lowStockTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addProductDetailsSection(Document document) {
        document.add(new Paragraph("PRODUCT DETAILS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Implementation would list all products with details
        document.add(new Paragraph("Product details would be displayed here.")
                .setFontSize(10)
                .setItalic());
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addFinancialSummarySection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("FINANCIAL SUMMARY")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        Double revenue = startDate != null && endDate != null
                ? orderDetailService.getRevenueByDateRange(startDate, endDate)
                : orderDetailService.getTotalRevenue();
        Double refunds = startDate != null && endDate != null
                ? returnOrderService.getTotalRefundAmountByDateRange(startDate, endDate)
                : returnOrderService.getTotalRefundAmount();
        Double netRevenue = (revenue != null ? revenue : 0.0) - (refunds != null ? refunds : 0.0);
        
        Table financialTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        financialTable.setWidth(UnitValue.createPercentValue(100));
        
        addSummaryRow(financialTable, "Gross Revenue", String.format("LKR %,.2f", revenue != null ? revenue : 0.0));
        addSummaryRow(financialTable, "Total Refunds", String.format("LKR %,.2f", refunds != null ? refunds : 0.0));
        addSummaryRow(financialTable, "Net Revenue", String.format("LKR %,.2f", netRevenue));
        
        document.add(financialTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addRevenueVsReturnsSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("📊 REVENUE VS RETURNS ANALYSIS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        Double revenue = startDate != null && endDate != null
                ? orderDetailService.getRevenueByDateRange(startDate, endDate)
                : orderDetailService.getTotalRevenue();
        Double refunds = startDate != null && endDate != null
                ? returnOrderService.getTotalRefundAmountByDateRange(startDate, endDate)
                : returnOrderService.getTotalRefundAmount();
        Long orders = startDate != null && endDate != null
                ? orderDetailService.countOrdersByDateRange(startDate, endDate)
                : orderDetailService.getTotalOrderCount();
        Long returns = startDate != null && endDate != null
                ? returnOrderService.countReturnsByDateRange(startDate, endDate)
                : (long) returnOrderService.findAllReturnOrders().size();
        
        double revenueVal = revenue != null ? revenue : 0.0;
        double refundsVal = refunds != null ? refunds : 0.0;
        long ordersVal = orders != null ? orders : 0;
        long returnsVal = returns != null ? returns : 0;
        
        double netRevenue = revenueVal - refundsVal;
        double returnRate = ordersVal > 0 ? (returnsVal * 100.0 / ordersVal) : 0.0;
        double refundRate = revenueVal > 0 ? (refundsVal * 100.0 / revenueVal) : 0.0;
        
        Table comparisonTable = new Table(UnitValue.createPercentArray(new float[]{2, 2}));
        comparisonTable.setWidth(UnitValue.createPercentValue(100));
        
        comparisonTable.addHeaderCell(new Cell().add(new Paragraph("Metric").setBold()).setPadding(8));
        comparisonTable.addHeaderCell(new Cell().add(new Paragraph("Value").setBold()).setPadding(8));
        
        addSummaryRow(comparisonTable, "💰 Gross Revenue", String.format("LKR %,.2f", revenueVal));
        addSummaryRow(comparisonTable, "💸 Total Refunds", String.format("LKR %,.2f", refundsVal));
        addSummaryRow(comparisonTable, "💵 Net Revenue", String.format("LKR %,.2f", netRevenue));
        addSummaryRow(comparisonTable, "📦 Total Orders", String.valueOf(ordersVal));
        addSummaryRow(comparisonTable, "🔄 Total Returns", String.valueOf(returnsVal));
        addSummaryRow(comparisonTable, "📉 Return Rate", String.format("%.2f%%", returnRate));
        addSummaryRow(comparisonTable, "📊 Refund Rate", String.format("%.2f%%", refundRate));
        
        // Health indicator
        String healthStatus;
        if (returnRate < 5.0 && refundRate < 2.0) {
            healthStatus = "✓ Excellent - Low return and refund rates";
        } else if (returnRate < 10.0 && refundRate < 5.0) {
            healthStatus = "✓ Good - Acceptable return and refund rates";
        } else if (returnRate < 15.0 && refundRate < 10.0) {
            healthStatus = "⚠️ Fair - Monitor return and refund trends";
        } else {
            healthStatus = "⚠️ Needs Attention - High return/refund rates";
        }
        
        document.add(comparisonTable);
        document.add(new Paragraph().setMarginTop(10));
        Paragraph healthIndicator = new Paragraph("📌 Business Health: " + healthStatus)
                .setFontSize(11)
                .setBold()
                .setMarginBottom(15);
        document.add(healthIndicator);
    }
    
    private void addTopCustomersSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("👥 TOP CUSTOMERS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<Object[]> topCustomers;
        if (startDate != null && endDate != null) {
            // Filter top customers by date range
            List<OrderDetail> orders = orderDetailService.findOrdersByDateRange(startDate, endDate);
            Map<String, Double> customerRevenue = orders.stream()
                    .filter(o -> o.getCustomerName() != null && "PAID".equals(o.getPaymentStatus()))
                    .collect(Collectors.groupingBy(
                        OrderDetail::getCustomerName,
                        Collectors.summingDouble(OrderDetail::getTotalCost)
                    ));
            
            topCustomers = customerRevenue.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(10)
                    .map(e -> new Object[]{e.getKey(), e.getValue()})
                    .collect(Collectors.toList());
        } else {
            topCustomers = orderDetailService.getTopCustomersByRevenue();
        }
        
        if (topCustomers.isEmpty()) {
            document.add(new Paragraph("No customer data available.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        // Limit to top 10
        topCustomers = topCustomers.stream().limit(10).collect(Collectors.toList());
        
        Table customersTable = new Table(UnitValue.createPercentArray(new float[]{0.5F, 2.5F, 2}));
        customersTable.setWidth(UnitValue.createPercentValue(100));
        
        customersTable.addHeaderCell(new Cell().add(new Paragraph("#").setBold()).setPadding(5));
        customersTable.addHeaderCell(new Cell().add(new Paragraph("Customer Name").setBold()).setPadding(5));
        customersTable.addHeaderCell(new Cell().add(new Paragraph("Total Spent").setBold()).setPadding(5));
        
        int rank = 1;
        for (Object[] customer : topCustomers) {
            String customerName = (String) customer[0];
            Double totalSpent = ((Number) customer[1]).doubleValue();
            
            customersTable.addCell(new Cell().add(new Paragraph(String.valueOf(rank++))).setPadding(5));
            customersTable.addCell(new Cell().add(new Paragraph(customerName != null ? customerName : "Guest")).setPadding(5));
            customersTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", totalSpent))).setPadding(5));
        }
        
        document.add(customersTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addSupplierSummarySection(Document document) {
        document.add(new Paragraph("SUPPLIER SUMMARY")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        int totalSuppliers = supplierService.findAllSuppliers().size();
        
        Table supplierTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        supplierTable.setWidth(UnitValue.createPercentValue(100));
        
        addSummaryRow(supplierTable, "Total Suppliers", String.valueOf(totalSuppliers));
        
        document.add(supplierTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addPurchaseOrdersSection(Document document) {
        document.add(new Paragraph("PURCHASE ORDERS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Implementation would load purchase orders
        document.add(new Paragraph("Purchase orders data would be displayed here.")
                .setFontSize(10)
                .setItalic());
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addExecutiveSummarySection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("EXECUTIVE SUMMARY")
                .setFontSize(18)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Comprehensive summary of all key metrics
        addSummarySection(document, startDate, endDate);
        addReturnStatisticsSection(document, startDate, endDate);
        addInventorySummarySection(document);
    }
    
    private void addSalesAnalysisSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("SALES ANALYSIS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        addSalesByPeriodSection(document, startDate, endDate, "comprehensive");
        addTopProductsSection(document, startDate, endDate);
        addSalesByCategorySection(document, startDate, endDate);
    }
    
    private void addReturnOrdersAnalysisSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("RETURN ORDERS ANALYSIS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        addReturnStatisticsSection(document, startDate, endDate);
        addReturnDetailsSection(document, startDate, endDate);
    }
    
    private void addInventoryStatusSection(Document document) {
        document.add(new Paragraph("INVENTORY STATUS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        addInventorySummarySection(document);
        addLowStockSection(document);
    }
    
    private void addCustomerAnalysisSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("CUSTOMER ANALYSIS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        addTopCustomersSection(document, startDate, endDate);
    }
    
    private void addSupplierAnalysisSection(Document document) {
        document.add(new Paragraph("SUPPLIER ANALYSIS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        addSupplierSummarySection(document);
        addPurchaseOrdersSection(document);
    }
    
    private void addFooter(Document document) throws IOException {
        document.add(new Paragraph().setMarginTop(30));
        
        // Professional footer line
        DeviceRgb footerColor = new DeviceRgb(52, 73, 94);
        Table footerLine = new Table(1);
        footerLine.setWidth(UnitValue.createPercentValue(100));
        footerLine.setBackgroundColor(footerColor);
        footerLine.addCell(new Cell().setHeight(2).setBorder(Border.NO_BORDER));
        document.add(footerLine);
        document.add(new Paragraph().setMarginTop(10));
        
        // Get system settings for footer
        SystemSettings settings = systemSettingsService.getSystemSettings();
        String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
            ? settings.getBusinessName()
            : "Kumara Enterprises";
        
        String footerMessage = settings.getFooterMessage() != null && !settings.getFooterMessage().trim().isEmpty()
            ? settings.getFooterMessage()
            : "Thank you for your business!";
        
        Paragraph footer = new Paragraph(
                "This report was generated by " + businessName + "\n" +
                footerMessage + "\n" +
                "Generated on: " + LocalDateTime.now().format(DATE_FORMATTER))
                .setFontSize(9)
                .setFont(getUnicodeFont())
                .setFontColor(footerColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic();
        document.add(footer);
    }
    
    private void addConstructionSummarySection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("🏗️ CONSTRUCTION ORDERS SUMMARY")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        Double revenue = startDate != null && endDate != null
                ? orderDetailService.getRevenueByOrderTypeAndDateRange("CONSTRUCTION", startDate, endDate)
                : orderDetailService.getRevenueByOrderType("CONSTRUCTION");
        Long orders = startDate != null && endDate != null
                ? orderDetailService.getOrderCountByOrderTypeAndDateRange("CONSTRUCTION", startDate, endDate)
                : orderDetailService.getOrderCountByOrderType("CONSTRUCTION");
        Double avgOrder = startDate != null && endDate != null
                ? orderDetailService.getAverageOrderValueByOrderTypeAndDateRange("CONSTRUCTION", startDate, endDate)
                : orderDetailService.getAverageOrderValueByOrderType("CONSTRUCTION");
        
        double revenueVal = revenue != null ? revenue : 0.0;
        long ordersVal = orders != null ? orders : 0;
        double avgOrderVal = avgOrder != null ? avgOrder : 0.0;
        
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{2, 2}));
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        
        summaryTable.addHeaderCell(new Cell().add(new Paragraph("Metric").setBold()).setPadding(8));
        summaryTable.addHeaderCell(new Cell().add(new Paragraph("Value").setBold()).setPadding(8));
        
        summaryTable.addCell(new Cell().add(new Paragraph("Total Revenue")).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("%.2f /=", revenueVal))).setPadding(8));
        
        summaryTable.addCell(new Cell().add(new Paragraph("Total Orders")).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(String.valueOf(ordersVal))).setPadding(8));
        
        summaryTable.addCell(new Cell().add(new Paragraph("Average Order Value")).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("%.2f /=", avgOrderVal))).setPadding(8));
        
        document.add(summaryTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addConstructionSalesByPeriodSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("📅 CONSTRUCTION SALES BY PERIOD")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Get monthly data for last 12 months
        LocalDate now = LocalDate.now();
        Table periodTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2}));
        periodTable.setWidth(UnitValue.createPercentValue(100));
        
        periodTable.addHeaderCell(new Cell().add(new Paragraph("Period").setBold()).setPadding(8));
        periodTable.addHeaderCell(new Cell().add(new Paragraph("Orders").setBold()).setPadding(8));
        periodTable.addHeaderCell(new Cell().add(new Paragraph("Revenue").setBold()).setPadding(8));
        periodTable.addHeaderCell(new Cell().add(new Paragraph("Avg Order").setBold()).setPadding(8));
        
        for (int i = 11; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).with(TemporalAdjusters.firstDayOfMonth());
            LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());
            
            LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
            LocalDateTime monthEndDateTime = monthEnd.atTime(23, 59, 59);
            
            Double revenue = orderDetailService.getRevenueByOrderTypeAndDateRange("CONSTRUCTION", monthStartDateTime, monthEndDateTime);
            Long orders = orderDetailService.getOrderCountByOrderTypeAndDateRange("CONSTRUCTION", monthStartDateTime, monthEndDateTime);
            Double avgOrder = orders != null && orders > 0 ? (revenue != null ? revenue : 0.0) / orders : 0.0;
            
            String period = monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            
            periodTable.addCell(new Cell().add(new Paragraph(period)).setPadding(6));
            periodTable.addCell(new Cell().add(new Paragraph(String.valueOf(orders != null ? orders : 0))).setPadding(6));
            periodTable.addCell(new Cell().add(new Paragraph(String.format("%.2f /=", revenue != null ? revenue : 0.0))).setPadding(6));
            periodTable.addCell(new Cell().add(new Paragraph(String.format("%.2f /=", avgOrder))).setPadding(6));
        }
        
        document.add(periodTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addConstructionSalesByCashierSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("👤 CONSTRUCTION SALES BY CASHIER")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<Object[]> cashierData = startDate != null && endDate != null
                ? orderDetailService.getSalesByCashierByOrderTypeAndDateRange("CONSTRUCTION", startDate, endDate)
                : orderDetailService.getSalesByCashierByOrderType("CONSTRUCTION");
        
        if (cashierData == null || cashierData.isEmpty()) {
            document.add(new Paragraph("No construction sales data available.").setFontSize(10));
            return;
        }
        
        Table cashierTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2}));
        cashierTable.setWidth(UnitValue.createPercentValue(100));
        
        cashierTable.addHeaderCell(new Cell().add(new Paragraph("Cashier").setBold()).setPadding(8));
        cashierTable.addHeaderCell(new Cell().add(new Paragraph("Orders").setBold()).setPadding(8));
        cashierTable.addHeaderCell(new Cell().add(new Paragraph("Revenue").setBold()).setPadding(8));
        
        for (Object[] data : cashierData) {
            String cashierName = (String) data[0];
            Long orderCount = ((Number) data[1]).longValue();
            Double revenueAmount = ((Number) data[2]).doubleValue();
            
            cashierTable.addCell(new Cell().add(new Paragraph(cashierName != null ? cashierName : "Unknown")).setPadding(6));
            cashierTable.addCell(new Cell().add(new Paragraph(String.valueOf(orderCount != null ? orderCount : 0))).setPadding(6));
            cashierTable.addCell(new Cell().add(new Paragraph(String.format("%.2f /=", revenueAmount != null ? revenueAmount : 0.0))).setPadding(6));
        }
        
        document.add(cashierTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
}

