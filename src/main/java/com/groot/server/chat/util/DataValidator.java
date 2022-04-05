package com.groot.server.chat.util;

/**
 * @author Melanga Kasun
 * @date (Sat) 26-Feb-2022
 */
public class DataValidator {
    public static boolean validateString(String text) {
        return text.matches("[a-zA-Z][a-zA-Z0-9]{2,15}$");
    }
}
