package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class TesseraButton extends TesseraElement {

    private Runnable onClick;
    private String label;
    private String textAlign = "center";
    private int bgColor = 0;
    private int hoverBgColor = 0;
    private boolean hoverBgColorSet = false;
    private int labelColor = 0;
    private String fontFamily = null;
    private float fontSize = 7f;
    private int fontWeight = 400;
    private String textTransform = null;
    private float opacity = 1f;

    public TesseraButton(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public TesseraButton onClick(Runnable onClick) { this.onClick = onClick; return this; }
    public TesseraButton label(String label) { this.label = label; return this; }
    public TesseraButton textAlign(String align) { this.textAlign = align; return this; }
    public TesseraButton bgColor(int color) { this.bgColor = color; return this; }
    public TesseraButton hoverBgColor(int color) { this.hoverBgColor = color; this.hoverBgColorSet = true; return this; }
    public TesseraButton labelColor(int color) { this.labelColor = color; return this; }
    public TesseraButton font(String fontFamily) { this.fontFamily = fontFamily; return this; }
    public TesseraButton fontSize(float px) { if (px > 0) this.fontSize = px; return this; }
    public TesseraButton fontWeight(int w) { if (w > 0) this.fontWeight = w; return this; }
    public TesseraButton textTransform(String tt) { this.textTransform = tt; return this; }
    public TesseraButton opacity(float o) { this.opacity = Math.max(0f, Math.min(1f, o)); return this; }

    @Override
    public boolean hasClickHandler() {
        return onClick != null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        boolean hov = active && isHovered(mx, my);
        int bg = (hoverBgColorSet && hov) ? hoverBgColor : (bgColor != 0 ? bgColor : TesseraPalette.BG2);
        g.fill(x, y, x + width, y + height, bg);
        renderStateOverlays(g, mx, my);

        if (label != null) {
            var font = Minecraft.getInstance().font;
            String displayed = TesseraTextStyling.transform(label, textTransform);
            var comp = TesseraFonts.component(displayed, fontFamily, fontWeight);
            float scale = fontSize / TesseraFonts.naturalPx(fontFamily);
            int textColor = !active ? TesseraPalette.CREAM_DIM
                          : labelColor != 0 ? (hov ? brighten(labelColor) : labelColor)
                          : (hov ? 0xFFFFFFFF : TesseraPalette.CREAM);
            textColor = TesseraLabel.applyOpacity(textColor, opacity);

            int textW = (int) Math.ceil(font.width(comp) * scale);
            int textY = pressed ? y + (height - (int) Math.ceil(8 * scale)) / 2 + 1
                                : y + (height - (int) Math.ceil(8 * scale)) / 2;
            int textX;
            switch (textAlign) {
                case "left"  -> textX = x + 4;
                case "right" -> textX = x + width - textW - 4;
                default      -> textX = x + (width - textW) / 2;
            }

            if (Math.abs(scale - 1f) < 1e-3f) {
                g.drawString(font, comp, textX, textY, textColor, false);
            } else {
                g.pose().pushPose();
                g.pose().translate(textX, textY, 0);
                g.pose().scale(scale, scale, 1f);
                g.drawString(font, comp, 0, 0, textColor, false);
                g.pose().popPose();
            }
        }
    }

    private static int brighten(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((color >> 8)  & 0xFF) + 40);
        int b = Math.min(255, ( color        & 0xFF) + 40);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0 || !active || onClick == null) return false;
        if (mx >= x && mx < x + width && my >= y && my < y + height) {
            pressed = true;
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mx, double my, int btn) {
        if (btn == 0) pressed = false;
    }
}
