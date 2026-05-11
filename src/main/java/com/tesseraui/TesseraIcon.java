package com.tesseraui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class TesseraIcon extends TesseraElement {

    private ResourceLocation texture;
    private int iconW = 16;
    private int iconH = 16;
    private int tint = 0xFFF0B27A;
    private Runnable onClick;

    public TesseraIcon(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public TesseraIcon texture(ResourceLocation texture) {
        this.texture = texture;
        return this;
    }

    public TesseraIcon size(int w, int h) {
        this.iconW = w;
        this.iconH = h;
        return this;
    }

    public TesseraIcon tint(int color) {
        this.tint = color;
        return this;
    }

    public TesseraIcon onClick(Runnable onClick) {
        this.onClick = onClick;
        return this;
    }

    @Override
    public boolean hasClickHandler() {
        return onClick != null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        int s = Math.min(Math.min(iconW, iconH), Math.min(width, height));
        int cx = x + (width - s) / 2;
        int cy = y + (height - s) / 2;

        if (texture != null) {
            g.blit(texture, cx, cy, 0f, 0f, s, s, s, s);
        } else {
            // Fallback: diamond shape (◆)
            int h1 = Math.max(1, s / 4);
            g.fill(cx + s / 4, cy,           cx + s * 3 / 4, cy + h1,         tint);
            g.fill(cx,         cy + h1,       cx + s,         cy + s - h1,     tint);
            g.fill(cx + s / 4, cy + s - h1,  cx + s * 3 / 4, cy + s,          tint);
        }
        renderStateOverlays(g, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0 || !active || onClick == null) return false;
        if (mx >= x && mx < x + width && my >= y && my < y + height) {
            onClick.run();
            return true;
        }
        return false;
    }
}
