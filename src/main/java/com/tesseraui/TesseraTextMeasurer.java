package com.tesseraui;

/**
 * Abstraction for measuring the rendered pixel width of text strings.
 *
 * <p>The production implementation delegates to {@code Minecraft.font}; test
 * implementations can supply a lightweight substitute that does not require a
 * running game instance.
 */
public interface TesseraTextMeasurer {

    /**
     * Returns the rendered pixel width of the given text at the given font configuration.
     * The returned value already accounts for the font scale.
     */
    int measureWidth(String text, String fontFamily, int fontWeight, float fontSizePx);

    /** Returns the natural line height in pixels for the given font size. */
    default int lineHeight(float fontSizePx, String fontFamily) {
        float scale = fontSizePx / TesseraFonts.naturalPx(fontFamily);
        return Math.max(1, (int) Math.ceil(9 * scale));
    }
}
