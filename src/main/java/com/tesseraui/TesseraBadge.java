package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class TesseraBadge extends TesseraElement {

    private String label;
    private int bgColor;
    private int textColor = TesseraPalette.CREAM;
    private String fontFamily = null;
    private int borderColor = 0;
    private int borderThickness = 0;
    private int paddingH = 10;
    private float fontSize = 7f;
    private int fontWeight = 400;
    private String textTransform = null;
    private float opacity = 1f;

    public TesseraBadge(int x, int y, int height, String label, int bgColor) {
        super(x, y, 0, height);
        this.label = label;
        this.bgColor = bgColor;
    }

    @Override
    public int getWidth() {
        return width > 0 ? width : measuredWidth();
    }

    public TesseraBadge label(String label) {
        this.label = label;
        this.width = 0;
        return this;
    }

    public TesseraBadge bg(int bgColor)              { this.bgColor = bgColor; return this; }
    public TesseraBadge textColor(int textColor)     { this.textColor = textColor; return this; }
    public TesseraBadge font(String fontFamily)      { this.fontFamily = fontFamily; this.width = 0; return this; }
    public TesseraBadge fontSize(float px)           { if (px > 0) { this.fontSize = px; this.width = 0; } return this; }
    public TesseraBadge fontWeight(int w)            { if (w > 0) { this.fontWeight = w; this.width = 0; } return this; }
    public TesseraBadge textTransform(String tt)     { this.textTransform = tt; this.width = 0; return this; }
    public TesseraBadge opacity(float o)             { this.opacity = Math.max(0f, Math.min(1f, o)); return this; }
    public TesseraBadge paddingH(int paddingH)       { this.paddingH = paddingH; this.width = 0; return this; }
    public TesseraBadge border(int thickness, int color) {
        this.borderThickness = thickness;
        this.borderColor = color;
        return this;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        var font = Minecraft.getInstance().font;
        String displayed = TesseraTextStyling.transform(label, textTransform);
        var comp = TesseraFonts.component(displayed, fontFamily, fontWeight);
        float scale = fontSize / TesseraFonts.naturalPx(fontFamily);
        int renderWidth = getWidth();

        g.fill(x, y, x + renderWidth, y + height, bgColor);

        if (borderThickness > 0 && borderColor != 0) {
            int t = borderThickness;
            g.fill(x,             y,              x + renderWidth,    y + t,        borderColor);
            g.fill(x,             y + height - t, x + renderWidth,    y + height,   borderColor);
            g.fill(x,             y,              x + t,        y + height,   borderColor);
            g.fill(x + renderWidth - t, y,              x + renderWidth,    y + height,   borderColor);
        }

        int textW = (int) Math.ceil(font.width(comp) * scale);
        int textX = x + (renderWidth - textW) / 2;
        int textY = y + (height - (int) Math.ceil(8 * scale)) / 2;
        int drawColor = TesseraLabel.applyOpacity(textColor, opacity);

        if (Math.abs(scale - 1f) < 1e-3f) {
            g.drawString(font, comp, textX, textY, drawColor, false);
        } else {
            g.pose().pushPose();
            g.pose().translate(textX, textY, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(font, comp, 0, 0, drawColor, false);
            g.pose().popPose();
        }
        renderStateOverlays(g, mx, my);
    }

    private int measuredWidth() {
        var font = Minecraft.getInstance().font;
        if (font == null) return 20;
        String displayed = TesseraTextStyling.transform(label, textTransform);
        var comp = TesseraFonts.component(displayed, fontFamily, fontWeight);
        float scale = fontSize / TesseraFonts.naturalPx(fontFamily);
        return Math.max(20, (int) Math.ceil(font.width(comp) * scale) + paddingH);
    }
}
