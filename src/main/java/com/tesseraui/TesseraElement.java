package com.tesseraui;

import net.minecraft.client.gui.GuiGraphics;

public abstract class TesseraElement implements TesseraWidget {

    protected int x, y, width, height;
    protected boolean active = true;
    protected boolean pressed = false;
    protected boolean visible = true;
    protected String tooltip = null;

    protected TesseraElement(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public Rect bounds() { return new Rect(x, y, width, height); }

    @Override
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean isActive() { return active; }

    @Override
    public boolean isVisible() { return visible; }

    @Override
    public void setVisible(boolean visible) { this.visible = visible; }

    @Override
    public String getTooltip() { return tooltip; }

    @Override
    public void setTooltip(String text) { this.tooltip = text; }

    /** Fluent setter for the tooltip text. */
    public TesseraElement tooltip(String text) { this.tooltip = text; return this; }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    public void setSize(int w, int h) { this.width = w; this.height = h; }

    protected boolean isHovered(int mx, int my) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    public boolean hasClickHandler() { return false; }

    public boolean isPressed() { return pressed; }

    protected void renderStateOverlays(GuiGraphics g, int mx, int my) {
        if (!active) {
            g.fill(x, y, x + width, y + height, 0x55000000);
            return;
        }
        if (pressed) {
            g.fill(x, y, x + width, y + height, 0x35000000);
            g.fill(x, y, x + width, y + 1, TesseraPalette.COPPER_LO);
            return;
        }
        if (hasClickHandler() && isHovered(mx, my)) {
            g.fill(x, y, x + width, y + height, 0x25FFFFFF);
            g.fill(x, y, x + width, y + 1, 0x40FFFFFF);
        }
    }

    @Override
    public abstract void render(GuiGraphics g, int mx, int my);

    @Override
    public boolean mouseClicked(double mx, double my, int btn) { return false; }
}
