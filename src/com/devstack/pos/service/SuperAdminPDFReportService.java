package com.devstack.pos.service;

import com.devstack.pos.entity.SuperAdminOrderDetail;
import com.devstack.pos.entity.SuperAdminOrderItem;
import com.devstack.pos.entity.SystemSettings;
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
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
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
    
    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    // Cache for Unicode fonts
    private static PdfFont unicodeFont = null;
    private static PdfFont unicodeMonospaceFont = null;
    private static String loadedFontPath = null; // Track which font was loaded
    
    // Initialize fonts on first use
    {
        try {
            // Pre-load fonts to see what we get
            System.out.println("\n" + "=".repeat(70));
            System.out.println("=== INITIALIZING UNICODE FONTS FOR SINHALA SUPPORT ===");
            System.out.println("=".repeat(70));
            PdfFont initFont = getUnicodeFont();
            if (initFont != null) {
                System.out.println("✓ Unicode font initialized successfully");
                // Check if it's a standard font (which won't support Sinhala)
                String fontName = initFont.getFontProgram() != null ? 
                    "Custom/Embedded Font" : "Standard Font (Helvetica)";
                System.out.println("   Font type: " + fontName);
                if (fontName.contains("Standard")) {
                    System.err.println("   ⚠⚠⚠ WARNING: Using standard font - Sinhala will NOT work!");
                    System.err.println("   ⚠⚠⚠ You MUST install a Sinhala-supporting font!");
                } else {
                    System.out.println("   ✓ Ready for Sinhala text rendering");
                }
            } else {
                System.err.println("✗ FAILED: Could not initialize Unicode font");
                System.err.println("   Sinhala text will NOT display correctly in PDFs!");
            }
            System.out.println("=".repeat(70) + "\n");
        } catch (Exception e) {
            System.err.println("✗ ERROR initializing Unicode font: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get a Unicode font that supports Sinhala and other languages
     * Uses multiple strategies to find a font that supports Sinhala
     */
    private PdfFont getUnicodeFont() throws IOException {
        if (unicodeFont != null) {
            return unicodeFont;
        }
        
        try {
            // Strategy 1: FIRST try direct font file paths (most reliable)
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
                                unicodeFont = pdfFont;
                                loadedFontPath = fontPath;
                                System.out.println("✓✓✓ SUCCESS: Loaded and VERIFIED Unicode font from file: " + fontPath);
                                System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                System.out.println("═══════════════════════════════════════════════════════════");
                                return unicodeFont;
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
                                                unicodeFont = pdfFont;
                                                loadedFontPath = fontPath;
                                                System.out.println("✓✓✓ SUCCESS: Loaded and VERIFIED PREFERRED Unicode font '" + fontName + "' from: " + fontPath);
                                                System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                                System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                                System.out.println("═══════════════════════════════════════════════════════════");
                                                return unicodeFont;
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
                                        unicodeFont = pdfFont;
                                        loadedFontPath = fontPath;
                                        System.out.println("✓✓✓ SUCCESS: Loaded and VERIFIED Unicode font '" + fontName + "' from: " + fontPath);
                                        System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                        System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                        System.out.println("═══════════════════════════════════════════════════════════");
                                        return unicodeFont;
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
                                unicodeFont = pdfFont;
                                loadedFontPath = foundFont;
                                System.out.println("✓✓✓ SUCCESS: Found and VERIFIED Unicode font: " + foundFont);
                                System.out.println("   Font encoding: IDENTITY_H (Unicode)");
                                System.out.println("   ✓✓✓ VERIFIED: This font CAN render Sinhala characters!");
                                System.out.println("═══════════════════════════════════════════════════════════");
                                return unicodeFont;
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
                                unicodeFont = testFont;
                                loadedFontPath = dir;
                                System.out.println("✓✓✓ Found Sinhala font on final search: " + dir);
                                System.out.println("═══════════════════════════════════════════════════════════");
                                return unicodeFont;
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
                                    unicodeFont = testFont;
                                    loadedFontPath = found;
                                    System.out.println("✓✓✓ Found Sinhala font on final recursive search: " + found);
                                    System.out.println("═══════════════════════════════════════════════════════════");
                                    return unicodeFont;
                                }
                            } catch (Exception e) {
                                // Continue
                            }
                        }
                    }
                }
            }
            
            unicodeFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            loadedFontPath = "Standard Helvetica (NO SINHALA SUPPORT)";
            System.err.println("⚠ Using fallback font: Standard Helvetica (Sinhala will show as boxes)");
            System.err.println("═══════════════════════════════════════════════════════════");
            return unicodeFont;
            
        } catch (Exception e) {
            System.err.println("Error loading Unicode font: " + e.getMessage());
            e.printStackTrace();
            try {
                unicodeFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                return unicodeFont;
            } catch (Exception ex) {
                throw new IOException("Failed to load any font", ex);
            }
        }
    }
    
    /**
     * Verify that a PdfFont actually supports Sinhala by testing rendering
     * This is more reliable than canDisplay() which can return true for fonts without glyphs
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
                // Still do a basic test
                try {
                    // Try to create a paragraph with Sinhala text
                    // This will work if the font supports it
                    String testText = "සිංහල";
                    Paragraph testPara = new Paragraph(testText);
                    testPara.setFont(font);
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
                
                // Try to add Sinhala text
                Paragraph testPara = new Paragraph(testText);
                testPara.setFont(font);
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
     * Get a Unicode monospace font for aligned text
     * Uses the same font as regular Unicode font for consistency (monospace not critical for alignment)
     */
    private PdfFont getUnicodeMonospaceFont() throws IOException {
        if (unicodeMonospaceFont != null) {
            return unicodeMonospaceFont;
        }
        
        try {
            // First try to get the regular Unicode font (it will work for alignment too)
            PdfFont regularUnicodeFont = getUnicodeFont();
            if (regularUnicodeFont != null) {
                unicodeMonospaceFont = regularUnicodeFont;
                return unicodeMonospaceFont;
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
                        unicodeMonospaceFont = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
                        System.out.println("✓ Loaded Unicode monospace font from: " + fontPath);
                        return unicodeMonospaceFont;
                    }
                } catch (Exception e) {
                    System.out.println("Could not load monospace font from " + fontPath);
                }
            }
            
            System.out.println("Warning: No Unicode monospace font found, using standard Courier.");
            unicodeMonospaceFont = PdfFontFactory.createFont(StandardFonts.COURIER);
            return unicodeMonospaceFont;
            
        } catch (Exception e) {
            System.err.println("Error loading Unicode monospace font: " + e.getMessage());
            try {
                unicodeMonospaceFont = PdfFontFactory.createFont(StandardFonts.COURIER);
                return unicodeMonospaceFont;
            } catch (Exception ex) {
                throw new IOException("Failed to load any font", ex);
            }
        }
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
                "Item", "Price", "Qty", "Disc", "Total"));
            receipt.append("................................................\n");
            
            // Items
            for (SuperAdminOrderItem item : orderItems) {
                String itemName = item.getProductName();
                if (itemName.length() > 14) {
                    itemName = itemName.substring(0, 11) + "...";
                }
                
                double discount = item.getDiscountPerUnit() != null ? item.getDiscountPerUnit() : 0.0;
                
                Double qty = item.getQuantity();
                String qtyStr;
                if (qty != null) {
                    if (qty == qty.intValue()) {
                        qtyStr = String.format("x%-3d", qty.intValue());
                    } else {
                        qtyStr = String.format("x%-5.2f", qty);
                    }
                } else {
                    qtyStr = "x0  ";
                }
                receipt.append(String.format("%-14s %9.1f %s %6.2f %9.2f\n",
                    itemName,
                    item.getUnitPrice(),
                    qtyStr,
                    discount,
                    item.getLineTotal()
                ));
            }
            
            receipt.append("................................................\n");
            
            // Totals section
            double subtotal = orderItems.stream().mapToDouble(item -> {
                Double qty = item.getQuantity();
                return item.getUnitPrice() * (qty != null ? qty : 0.0);
            }).sum();
            double totalDiscount = orderDetail.getDiscount();
            
            receipt.append(String.format("%-30s %17.2f\n", "Subtotal", subtotal));
            receipt.append(String.format("%-30s %17.2f\n", "Total Discount", totalDiscount));
            receipt.append(String.format("%-30s %17.2f\n", "Items", (double)orderItems.size()));
            receipt.append("------------------------------------------------\n");
            receipt.append(String.format("%-30s %17.2f\n", "TOTAL", orderDetail.getTotalCost()));
            
            // Customer Paid
            double customerPaid = orderDetail.getCustomerPaid() != null ? orderDetail.getCustomerPaid() : orderDetail.getTotalCost();
            receipt.append(String.format("%-30s %17.2f\n", "Customer Paid", customerPaid));
            
            // Payment method
            receipt.append(String.format("%-30s %17s\n", 
                "Payment: " + orderDetail.getPaymentMethod(), ""));
            
            // Change
            if ("PAID".equals(orderDetail.getPaymentStatus()) && customerPaid > orderDetail.getTotalCost()) {
                double change = customerPaid - orderDetail.getTotalCost();
                receipt.append(String.format("%-30s %17.2f\n", "Change", change));
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
                receipt.append(String.format("%-30s %17.2f\n", "Balance", balance));
            } else {
                receipt.append(String.format("%-30s %17.2f\n", "Balance", 0.00));
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
        
        // Create directory structure: ~/POS_Receipts/SuperAdmin/[CustomerName]/
        String userHome = System.getProperty("user.home");
        String operatorEmail = orderDetail.getOperatorEmail();
        
        // Extract operator username from email
        String operatorName = operatorEmail != null && operatorEmail.contains("@") 
            ? operatorEmail.substring(0, operatorEmail.indexOf("@")) 
            : (operatorEmail != null ? operatorEmail : "unknown");
        
        // Sanitize operator name for file system
        operatorName = operatorName.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Get customer name - use "Guest" if null or empty
        String customerName = orderDetail.getCustomerName();
        if (customerName == null || customerName.trim().isEmpty() || "Guest".equalsIgnoreCase(customerName.trim())) {
            customerName = "Guest";
        }
        
        // Sanitize customer name for file system
        customerName = customerName.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Create nested directory path: ~/POS_Receipts/SuperAdmin/[OperatorName]/[CustomerName]/
        String receiptsDir = userHome + File.separator + "POS_Receipts" + File.separator + "SuperAdmin" + File.separator + operatorName + File.separator + customerName;
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
        String fileName = "SuperAdmin_Receipt_" + orderDetail.getCode() + "_" + System.currentTimeMillis() + ".pdf";
        String filePath = receiptsDir + File.separator + fileName;
        
        // Create PDF
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(20, 20, 20, 20);
        
        // Get system settings
        SystemSettings settings = systemSettingsService.getSystemSettings();
        
        // Store/Company Header
        String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
            ? settings.getBusinessName().toUpperCase()
            : "KUMARA ENTERPRISES";
        Paragraph storeName = new Paragraph(businessName)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(22)
            .setFont(getUnicodeFont()) // Use Unicode font for Sinhala support
            .setBold()
            .setMarginBottom(5);
        document.add(storeName);
        
        // Store Address
        StringBuilder addressText = new StringBuilder();
        if (settings.getAddress() != null && !settings.getAddress().trim().isEmpty()) {
            addressText.append(settings.getAddress());
            if (!settings.getAddress().toLowerCase().contains("wewala")) {
                addressText.append("\nWewala,Piliyandala");
            }
        } else {
            addressText.append("58k Gagabada Rd,");
            addressText.append("\nWewala,Piliyandala");
        }
        
        if (settings.getContactNumber() != null && !settings.getContactNumber().trim().isEmpty()) {
            if (addressText.length() > 0) {
                addressText.append("\n");
            }
            addressText.append(settings.getContactNumber());
        } else {
            if (addressText.length() > 0) {
                addressText.append("\n");
            }
            addressText.append("077 781 5955 / 011 261 3606");
        }
        
        Paragraph storeAddress = new Paragraph(addressText.toString())
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setFont(getUnicodeFont()) // Use Unicode font for Sinhala support
            .setMarginBottom(15);
        document.add(storeAddress);
        
        // Get Unicode font once for reuse
        PdfFont unicodeFontForAll = getUnicodeFont();
        
        // Super Admin Order Label
        Paragraph superAdminLabel = new Paragraph("*** SUPER ADMIN ORDER ***")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(14)
            .setFont(unicodeFontForAll) // Ensure Unicode font
            .setBold()
            .setMarginBottom(10);
        document.add(superAdminLabel);
        
        // Divider line
        document.add(new Paragraph("=" .repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setFont(unicodeFontForAll) // Ensure Unicode font
            .setMarginBottom(10));
        
        // Receipt Title
        Paragraph receiptTitle = new Paragraph("SALES RECEIPT")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(14)
            .setFont(unicodeFontForAll) // Ensure Unicode font
            .setBold()
            .setMarginBottom(15);
        document.add(receiptTitle);
        
        // Order Information - use Unicode font for all text
        PdfFont unicodeFontForText = getUnicodeFont();
        
        document.add(new Paragraph("Receipt No: " + orderDetail.getCode())
            .setFontSize(10)
            .setFont(unicodeFontForText)
            .setMarginBottom(3));
        
        document.add(new Paragraph("Date: " + orderDetail.getIssuedDate().format(RECEIPT_DATE_FORMATTER))
            .setFontSize(10)
            .setFont(unicodeFontForText)
            .setMarginBottom(3));
        
        document.add(new Paragraph("Customer: " + orderDetail.getCustomerName())
            .setFontSize(10)
            .setFont(unicodeFontForText) // Use Unicode font for Sinhala customer names
            .setMarginBottom(3));
        
        document.add(new Paragraph("Operator: " + orderDetail.getOperatorEmail())
            .setFontSize(10)
            .setFont(unicodeFontForText)
            .setMarginBottom(3));
        
        document.add(new Paragraph("Payment Method: " + orderDetail.getPaymentMethod())
            .setFontSize(10)
            .setFont(unicodeFontForText)
            .setMarginBottom(3));
        
        // Order Type
        String orderTypeDisplay = orderDetail.getOrderType() != null && orderDetail.getOrderType().equals("CONSTRUCTION") 
            ? "Construction" 
            : "Hardware";
        document.add(new Paragraph("Order Type: " + orderTypeDisplay)
            .setFontSize(10)
            .setFont(unicodeFontForText)
            .setMarginBottom(10));
        
        // Divider line
        document.add(new Paragraph("-" .repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setFont(unicodeFontForText) // Ensure Unicode font
            .setMarginBottom(10));
        
        // Items table for better alignment with Sinhala text
        // IMPORTANT: Use Unicode font for item names to support Sinhala
        PdfFont unicodeFontForItems = getUnicodeFont();
        PdfFont unicodeMonospaceFont = getUnicodeMonospaceFont();
        
        Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1.5f, 1f, 1f, 1.5f}));
        itemsTable.setWidth(UnitValue.createPercentValue(100));
        itemsTable.setFontSize(9);
        itemsTable.setFont(unicodeMonospaceFont);
        
        // Header row - use Unicode font
        itemsTable.addHeaderCell(new Cell().add(new Paragraph("Item").setBold()).setFont(unicodeFontForItems));
        itemsTable.addHeaderCell(new Cell().add(new Paragraph("Price").setBold()).setFont(unicodeFontForItems));
        itemsTable.addHeaderCell(new Cell().add(new Paragraph("Qty").setBold()).setFont(unicodeFontForItems));
        itemsTable.addHeaderCell(new Cell().add(new Paragraph("Disc").setBold()).setFont(unicodeFontForItems));
        itemsTable.addHeaderCell(new Cell().add(new Paragraph("Total").setBold()).setFont(unicodeFontForItems));
        
        // Items rows
        for (SuperAdminOrderItem item : orderItems) {
            String itemName = item.getProductName();
            // Don't truncate Sinhala text - let it wrap naturally
            if (itemName.length() > 20) {
                itemName = itemName.substring(0, 17) + "...";
            }
            
            double discount = item.getDiscountPerUnit() != null ? item.getDiscountPerUnit() : 0.0;
            
            Double qty = item.getQuantity();
            String qtyStr;
            if (qty != null) {
                if (qty == qty.intValue()) {
                    qtyStr = "x" + qty.intValue();
                } else {
                    qtyStr = String.format("x%.2f", qty);
                }
            } else {
                qtyStr = "x0";
            }
            
            // Use Unicode font for item names to support Sinhala
            // IMPORTANT: Set font on Paragraph, not just Cell, to ensure it's applied
            Paragraph itemNamePara = new Paragraph(itemName);
            itemNamePara.setFont(unicodeFontForItems); // Use the pre-loaded Unicode font
            Cell itemNameCell = new Cell().add(itemNamePara);
            itemNameCell.setFont(unicodeFontForItems); // Also set on cell for extra safety
            itemsTable.addCell(itemNameCell);
            itemsTable.addCell(new Cell().add(new Paragraph(String.format("%.1f", item.getUnitPrice()))).setFont(getUnicodeMonospaceFont()));
            itemsTable.addCell(new Cell().add(new Paragraph(qtyStr)).setFont(getUnicodeMonospaceFont()));
            itemsTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", discount))).setFont(getUnicodeMonospaceFont()));
            itemsTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", item.getLineTotal()))).setFont(getUnicodeMonospaceFont()));
        }
        
        document.add(itemsTable);
        document.add(new Paragraph().setMarginBottom(10));
        
        // Divider line
        document.add(new Paragraph("-" .repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setFont(unicodeFontForText) // Ensure Unicode font
            .setMarginBottom(10));
        
        // Total section
        double subtotal = orderItems.stream().mapToDouble(item -> 
            item.getUnitPrice() * item.getQuantity()).sum();
        double totalDiscount = orderDetail.getDiscount();
        
        // Use monospace font for proper alignment (with Unicode support)
        PdfFont monospaceFont = getUnicodeMonospaceFont();
        
        document.add(new Paragraph(String.format("%-30s %17.2f", "Subtotal", subtotal))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph(String.format("%-30s %17.2f", "Total Discount", totalDiscount))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph(String.format("%-30s %17.2f", "Items", (double)orderItems.size()))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph("-" .repeat(48))
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph(String.format("%-30s %17.2f", "TOTAL", orderDetail.getTotalCost()))
            .setFont(monospaceFont)
            .setFontSize(12)
            .setBold()
            .setMarginBottom(3));
        
        // Customer Paid
        double customerPaid = orderDetail.getCustomerPaid() != null ? orderDetail.getCustomerPaid() : orderDetail.getTotalCost();
        document.add(new Paragraph(String.format("%-30s %17.2f", "Customer Paid", customerPaid))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setMarginBottom(3));
        
        // Payment method
        document.add(new Paragraph(String.format("%-30s %17s", "Payment: " + orderDetail.getPaymentMethod(), ""))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setMarginBottom(3));
        
        // Change
        if ("PAID".equals(orderDetail.getPaymentStatus()) && customerPaid > orderDetail.getTotalCost()) {
            double change = customerPaid - orderDetail.getTotalCost();
            document.add(new Paragraph(String.format("%-30s %17.2f", "Change", change))
                .setFont(monospaceFont)
                .setFontSize(10)
                .setMarginBottom(3));
        }
        
        document.add(new Paragraph("-" .repeat(48))
            .setFontSize(10)
            .setMarginBottom(3));
        
        // Order Type
        String orderType = orderDetail.getOrderType() != null && orderDetail.getOrderType().equals("CONSTRUCTION") 
            ? "Construction" : "Hardware";
        document.add(new Paragraph(String.format("Type: %s", orderType))
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph("-" .repeat(48))
            .setFontSize(10)
            .setMarginBottom(3));
        
        // Balance
        double balance = orderDetail.getBalance() != null ? orderDetail.getBalance() : 0.00;
        document.add(new Paragraph(String.format("%-30s %17.2f", "Balance", balance))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setBold()
            .setMarginBottom(5));
        
        // Payment Status
        String paymentStatusText = "PAID".equals(orderDetail.getPaymentStatus()) ? 
            "*** PAID ***" : "*** PAYMENT PENDING ***";
        document.add(new Paragraph(paymentStatusText)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12)
            .setBold()
            .setMarginBottom(20));
        
        // Footer
        String footerMessage = settings.getFooterMessage() != null && !settings.getFooterMessage().trim().isEmpty()
            ? settings.getFooterMessage()
            : "Thank you for your business!";
        document.add(new Paragraph(footerMessage)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(11)
            .setFont(getUnicodeFont()) // Use Unicode font for Sinhala footer messages
            .setMarginBottom(10));
        
        document.add(new Paragraph("=" .repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10));
        
        document.close();
        return filePath;
    }
}
