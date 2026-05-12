package com.tesseraui;

/**
 * Parsed definition of one entry in a CSS {@code transition} shorthand.
 * Example: {@code transition: background-color 200ms ease-out 0ms}
 */
public record TesseraTransitionDef(
        /** CSS property name (e.g. "background-color", "opacity", "color"). */
        String property,
        /** Total transition duration in milliseconds. */
        int durationMs,
        /** Timing function. */
        TesseraEasing easing,
        /** Delay before the transition starts, in milliseconds. */
        int delayMs
) {
    public static TesseraTransitionDef of(String property, int durationMs,
                                           TesseraEasing easing, int delayMs) {
        return new TesseraTransitionDef(property, durationMs, easing, delayMs);
    }
}
