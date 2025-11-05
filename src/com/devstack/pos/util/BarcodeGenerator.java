package com.devstack.pos.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Utility class for generating barcodes using ZXing library
 */
public class BarcodeGenerator {
    
    private static final int BARCODE_WIDTH = 300;
    private static final int BARCODE_HEIGHT = 100;

    /**
     * Generates a barcode image from the given code
     * @param barcodeValue the value to encode as barcode
     * @return JavaFX Image of the barcode
     * @throws WriterException if barcode generation fails
     */
    public static Image generateBarcodeImage(String barcodeValue) throws WriterException {
        Code128Writer barcodeWriter = new Code128Writer();
        BitMatrix bitMatrix = barcodeWriter.encode(
            barcodeValue, 
            BarcodeFormat.CODE_128, 
            BARCODE_WIDTH, 
            BARCODE_HEIGHT
        );
        
        BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        return SwingFXUtils.toFXImage(barcodeImage, null);
    }
    
    /**
     * Generates a barcode and saves it as PNG bytes
     * @param barcodeValue the value to encode as barcode
     * @return byte array of PNG image
     * @throws WriterException if barcode generation fails
     * @throws IOException if image writing fails
     */
    public static byte[] generateBarcodePNG(String barcodeValue) throws WriterException, IOException {
        Code128Writer barcodeWriter = new Code128Writer();
        BitMatrix bitMatrix = barcodeWriter.encode(
            barcodeValue, 
            BarcodeFormat.CODE_128, 
            BARCODE_WIDTH, 
            BARCODE_HEIGHT
        );
        
        BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(barcodeImage, "PNG", baos);
        return baos.toByteArray();
        }
    
    /**
     * Generates a unique barcode value based on product code and timestamp
     * @param productCode the product code
     * @return generated barcode string
     */
    public static String generateBarcodeValue(int productCode) {
        // Generate a barcode in format: PRD + zero-padded code (8 digits total)
        return String.format("PRD%05d", productCode);
    }

    /**
     * Validates if a string is a valid barcode format
     * @param barcode the barcode to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return false;
        }
        // Allow alphanumeric barcodes between 3 and 100 characters
        return barcode.matches("^[A-Za-z0-9-_]+$") && barcode.length() >= 3 && barcode.length() <= 100;
    }
    
    /**
     * Generates a random numeric barcode of specified length
     * @param length the number of digits to generate
     * @return numeric string of specified length
     */
    public static String generateNumeric(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        
        Random random = new Random();
        StringBuilder barcode = new StringBuilder();
        
        // First digit should not be zero
        barcode.append(random.nextInt(9) + 1);
        
        // Generate remaining digits
        for (int i = 1; i < length; i++) {
            barcode.append(random.nextInt(10));
        }
        
        return barcode.toString();
    }
}
