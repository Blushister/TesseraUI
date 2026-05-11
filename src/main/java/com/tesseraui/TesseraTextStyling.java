package com.tesseraui;

import java.util.Locale;

final class TesseraTextStyling {
    private TesseraTextStyling() {}

    static String transform(String text, String transform) {
        if (text == null || transform == null) return text;
        return switch (transform) {
            case "uppercase" -> text.toUpperCase(Locale.ROOT);
            case "lowercase" -> text.toLowerCase(Locale.ROOT);
            case "capitalize" -> capitalize(text);
            default -> text;
        };
    }

    private static String capitalize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        boolean atWordStart = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) { sb.append(c); atWordStart = true; }
            else if (atWordStart) { sb.append(Character.toUpperCase(c)); atWordStart = false; }
            else sb.append(c);
        }
        return sb.toString();
    }
}
