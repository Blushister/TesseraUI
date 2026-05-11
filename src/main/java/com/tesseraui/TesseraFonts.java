package com.tesseraui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * Font registry for TesseraUI.
 *
 * <p>Two built-in font families are supported: {@code "fantasy"} and {@code "mono"}.
 * These must be provided as resource pack font definitions under the {@code tesseraui} namespace.
 * If not present, Minecraft falls back to the default font automatically.
 */
public final class TesseraFonts {

    public static final ResourceLocation FANTASY = ResourceLocation.fromNamespaceAndPath("tesseraui", "fantasy");
    public static final ResourceLocation MONO    = ResourceLocation.fromNamespaceAndPath("tesseraui", "mono");

    private TesseraFonts() {}

    public static float naturalPx(String fontFamily) {
        if ("fantasy".equals(fontFamily)) return 9f;
        if ("mono".equals(fontFamily))    return 8f;
        return 7f;
    }

    public static Component component(String text, String fontFamily) {
        return component(text, fontFamily, 400);
    }

    public static Component component(String text, String fontFamily, int fontWeight) {
        Style style = Style.EMPTY;
        if ("fantasy".equals(fontFamily)) style = style.withFont(FANTASY);
        else if ("mono".equals(fontFamily)) style = style.withFont(MONO);
        if (fontWeight >= 600) style = style.withBold(true);
        return Component.literal(text).withStyle(style);
    }
}
