package com.tesseraui;

/**
 * Parsed definition of a CSS {@code animation} shorthand.
 * Example: {@code animation: pulse 1s ease-in-out infinite alternate}
 */
public record TesseraAnimationDef(
        /** Name referencing a {@code @keyframes} block. */
        String name,
        /** Total duration of one iteration in milliseconds. */
        int durationMs,
        /** Timing function applied across the full keyframe range. */
        TesseraEasing easing,
        /** Delay before the animation starts, in milliseconds. */
        int delayMs,
        /** Number of iterations; {@code -1} means infinite. */
        int iterationCount,
        /** When {@code true}, alternating iterations run in reverse. */
        boolean alternate
) {
    public static TesseraAnimationDef of(String name, int durationMs,
                                          TesseraEasing easing, int delayMs,
                                          int iterationCount, boolean alternate) {
        return new TesseraAnimationDef(name, durationMs, easing, delayMs, iterationCount, alternate);
    }
}
