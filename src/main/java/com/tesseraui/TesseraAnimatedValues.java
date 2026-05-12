package com.tesseraui;

/**
 * Snapshot of interpolated animation values for a single widget during one render frame.
 *
 * <p>A value of {@code 0} (for colors) or {@code -1f} (for opacity) means
 * "not currently animated — use the static CSS value instead."</p>
 */
public record TesseraAnimatedValues(
        /** Animated background color. Only valid when {@link #hasBackground()} is true. */
        int   background,
        /** Animated text/foreground color. Only valid when {@link #hasColor()} is true. */
        int   color,
        /** Animated opacity in [0, 1], or {@code -1} if not animated. */
        float opacity,
        /** Animated border color. Only valid when {@link #hasBorderColor()} is true. */
        int   borderColor,
        /**
         * Presence bitmask: bit 0 = background, bit 1 = color, bit 2 = borderColor.
         * Replaces the old {@code != 0} sentinel so that {@code 0x00000000} (transparent)
         * is a valid animated value rather than "not animated".
         */
        int   mask
) {
    static final int BG_BIT = 1, COLOR_BIT = 2, BORDER_BIT = 4;

    /**
     * Backward-compatible 4-arg constructor.
     * Computes the mask using the same {@code != 0} sentinel as the pre-bitmask API,
     * so existing callers that pass raw color values still get correct {@code has*()} results.
     */
    public TesseraAnimatedValues(int background, int color, float opacity, int borderColor) {
        this(background, color, opacity, borderColor,
             (background   != 0 ? BG_BIT     : 0)
           | (color        != 0 ? COLOR_BIT  : 0)
           | (borderColor  != 0 ? BORDER_BIT : 0));
    }

    /** Sentinel: nothing is animated. */
    public static final TesseraAnimatedValues NONE =
            new TesseraAnimatedValues(0, 0, -1f, 0, 0);

    public boolean hasBackground()  { return (mask & BG_BIT)     != 0; }
    public boolean hasColor()       { return (mask & COLOR_BIT)  != 0; }
    public boolean hasOpacity()     { return opacity >= 0f; }
    public boolean hasBorderColor() { return (mask & BORDER_BIT) != 0; }
}
