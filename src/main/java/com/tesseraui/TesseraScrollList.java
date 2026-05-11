package com.tesseraui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public final class TesseraScrollList extends TesseraElement {

    private static final int SCROLLBAR_WIDTH = 3;

    private int scrollOffset = 0;
    private final int rowH;
    private final List<TesseraWidget> rows = new ArrayList<>();

    public TesseraScrollList(int x, int y, int w, int h, int rowH) {
        super(x, y, w, h);
        this.rowH = rowH;
    }

    public void setItems(List<? extends TesseraWidget> items) {
        rows.clear();
        rows.addAll(items);
        scrollOffset = Math.min(scrollOffset, maxScroll());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        g.enableScissor(x, y, x + width, y + height);
        int effectiveRowWidth = maxScroll() > 0 ? width - SCROLLBAR_WIDTH - 1 : width;
        int ry = y - scrollOffset;
        for (TesseraWidget row : rows) {
            if (ry + rowH >= y && ry < y + height) {
                row.setPosition(x, ry);
                row.setSize(effectiveRowWidth, rowH);
                row.render(g, mx, my);
            }
            ry += rowH;
        }
        g.disableScissor();
        renderScrollbar(g);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!isInBounds(mx, my)) return false;
        // 1. scrollbar zone first — consumes the click to prevent row overlap
        if (maxScroll() > 0 && mx >= x + width - SCROLLBAR_WIDTH && mx <= x + width) {
            return true;
        }
        // 2. then propagate to rows
        int effectiveRowWidth = maxScroll() > 0 ? width - SCROLLBAR_WIDTH - 1 : width;
        int ry = y - scrollOffset;
        for (TesseraWidget row : rows) {
            if (ry + rowH >= y && ry < y + height) {
                row.setPosition(x, ry);
                row.setSize(effectiveRowWidth, rowH);
                if (row.mouseClicked(mx, my, btn)) return true;
            }
            ry += rowH;
        }
        return false;
    }

    public boolean mouseScrolled(double dy) {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset - (int) (dy * rowH)));
        return true;
    }

    private boolean isInBounds(double mx, double my) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() * rowH - height);
    }

    private void renderScrollbar(GuiGraphics g) {
        int max = maxScroll();
        if (max <= 0) return;
        int barH = Math.max(10, height * height / (rows.size() * rowH));
        int barY = y + (int) ((long) scrollOffset * (height - barH) / max);
        int barX = x + width - SCROLLBAR_WIDTH;
        g.fill(barX, y, barX + SCROLLBAR_WIDTH, y + height, 0x20FFFFFF);
        g.fill(barX, barY, barX + SCROLLBAR_WIDTH, barY + barH, 0x80FFFFFF);
    }
}
