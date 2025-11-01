package com.devstack.pos.util;

import java.util.Random;

/**
 * Generates unique barcode data for POS system
 */
public class BarcodeGenerator {
    private final static String ALPHA_NUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Generates a unique barcode string for products
     * @param length length of the barcode string
     * @return unique barcode string
     */
    public static String generate(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            stringBuilder.append(ALPHA_NUMERIC.charAt(random.nextInt(ALPHA_NUMERIC.length())));
        }
        return stringBuilder.toString();
    }

    /**
     * Generates a numeric barcode (for EAN-13, CODE-128, etc.)
     * @param length length of the numeric barcode
     * @return numeric barcode string
     */
    public static String generateNumeric(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            stringBuilder.append(random.nextInt(10));
        }
        return stringBuilder.toString();
    }
}

