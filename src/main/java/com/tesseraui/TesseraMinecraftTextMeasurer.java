package com.tesseraui;

/**
 * Production {@link TesseraTextMeasurer} that delegates to {@code Minecraft.getInstance().font}.
 *
 * <p>Must only be called from the Minecraft client thread (or after the game has initialised).
 */
public final class TesseraMinecraftTextMeasurer implements TesseraTextMeasurer {

    public static final TesseraMinecraftTextMeasurer INSTANCE = new TesseraMinecraftTextMeasurer();

    private TesseraMinecraftTextMeasurer() {}

    @Override
    public int measureWidth(String text, String fontFamily, int fontWeight, float fontSizePx) {
        float scale = fontSizePx / TesseraFonts.naturalPx(fontFamily);
        String displayed = text != null ? text : "";
        var comp = TesseraFonts.component(displayed, fontFamily, fontWeight);
        return (int) Math.ceil(net.minecraft.client.Minecraft.getInstance().font.width(comp) * scale);
    }
}
