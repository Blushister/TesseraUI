package com.tesseraui;

/**
 * Snapshot of interpolated animation values for a single widget during one render frame.
 *
 * <p>A value of {@code 0} (for colors) or {@code -1f} (for opacity) means
 * "not currently animated — use the static CSS value instead."</p>
 */
public record TesseraAnimatedValues(
        /** Animated background color, or {@code 0} if not animated. */
        int  background,
        /** Animated text/foreground color, or {@code 0} if not animated. */
        int  color,
        /** Animated opacity in [0, 1], or {@code -1} if not animated. */
        float opacity,
        /** Animated border color, or {@code 0} if not animated. */
        int  borderColor
) {
    /** Sentinel: nothing is animated. */
    public static final TesseraAnimatedValues NONE =
            new TesseraAnimatedValues(0, 0, -1f, 0);

    public boolean hasBackground()  { return background  != 0; }
    public boolean hasColor()       { return color       != 0; }
    public boolean hasOpacity()     { return opacity     >= 0f; }
    public boolean hasBorderColor() { return borderColor != 0; }
}
