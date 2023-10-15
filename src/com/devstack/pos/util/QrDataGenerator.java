package com.devstack.pos.util;

import java.util.Random;

public class QrDataGenerator {
    private final static String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$";

    public static String generate(int length){
        StringBuilder stringBuilder= new StringBuilder();
        for (int i = 0; i < length; i++) {
            stringBuilder.append(ALPHA.charAt(new Random().nextInt(39)));
        }
        return stringBuilder.toString();
    }
}
