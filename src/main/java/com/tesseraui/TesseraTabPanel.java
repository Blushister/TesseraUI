package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * A tabbed container widget.
 *
 * <p>Displays a row of tab buttons at the top and swaps the visible content panel when the
 * user clicks a tab. Styling is configurable via the fluent API.</p>
 *
 * <pre>{@code
 * TesseraTabPanel tabs = new TesseraTabPanel(0, 0, 200, 120)
 *     .addTab("Général", generalPanel)
 *     .addTab("Avancé",  advancedPanel)
 *     .tabBarHeight(14)
 *     .activeTabColor(TesseraPalette.COPPER_LO)
 *     .inactiveTabColor(TesseraPalette.BG2);
 * }</pre>
 */
public final class TesseraTabPanel extends TesseraElement {

    // ── Tab record ─────────────────────────────────────────────────────────────

    private static final class Tab {
        final String label;
        final TesseraPanel content;
        Tab(String label, TesseraPanel content) {
            this.label = label;
            this.content = content;
        }
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private final List<Tab> tabs = new ArrayList<>();
    private int activeIndex = 0;

    private int tabBarH        = 14;
    private int tabBarBg       = TesseraPalette.BG3;
    private int activeTabColor = TesseraPalette.COPPER_LO;
    private int inactiveTabColor = TesseraPalette.BG2;
    private int activeTabTextColor   = TesseraPalette.CREAM;
    private int inactiveTabTextColor = TesseraPalette.CREAM_DIM;

    // ── Constructor ────────────────────────────────────────────────────────────

    public TesseraTabPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    // ── Fluent API ─────────────────────────────────────────────────────────────

    /** Adds a tab with the given label and content panel. Returns {@code this}. */
    public TesseraTabPanel addTab(String label, TesseraPanel content) {
        tabs.add(new Tab(label, content));
        return this;
    }

    /** Sets the index of the active (visible) tab. */
    public TesseraTabPanel activeTab(int index) {
        if (index >= 0 && index < tabs.size()) activeIndex = index;
        return this;
    }

    /** Sets the height of the tab button bar. Default: 14. */
    public TesseraTabPanel tabBarHeight(int px) {
        tabBarH = Math.max(8, px);
        return this;
    }

    /** Sets the background colour of the tab bar strip. */
    public TesseraTabPanel tabBarBackground(int color) {
        tabBarBg = color;
        return this;
    }

    /** Sets the background colour of the active tab button. */
    public TesseraTabPanel activeTabColor(int color) {
        activeTabColor = color;
        return this;
    }

    /** Sets the background colour of inactive tab buttons. */
    public TesseraTabPanel inactiveTabColor(int color) {
        inactiveTabColor = color;
        return this;
    }

    // ── TesseraWidget ──────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        if (!visible || tabs.isEmpty()) return;

        // ── Tab bar background ────────────────────────────────────────────────
        g.fill(x, y, x + width, y + tabBarH, tabBarBg);

        // ── Tab buttons ───────────────────────────────────────────────────────
        int tabCount = tabs.size();
        int tabW     = width / tabCount;
        var font     = Minecraft.getInstance().font;

        for (int i = 0; i < tabCount; i++) {
            Tab tab   = tabs.get(i);
            int tx    = x + i * tabW;
            int ty    = y;
            int tw    = (i == tabCount - 1) ? width - i * tabW : tabW; // last absorbs rounding
            boolean isActive = (i == activeIndex);

            int bg   = isActive ? activeTabColor   : inactiveTabColor;
            int fg   = isActive ? activeTabTextColor : inactiveTabTextColor;

            g.fill(tx, ty, tx + tw, ty + tabBarH, bg);

            // Active indicator: copper line at bottom of tab
            if (isActive) {
                g.fill(tx, ty + tabBarH - 2, tx + tw, ty + tabBarH, TesseraPalette.COPPER_HI);
            } else {
                // Separator on right side of inactive tabs
                if (i < tabCount - 1) {
                    g.fill(tx + tw - 1, ty + 2, tx + tw, ty + tabBarH - 2, TesseraPalette.COPPER_DEEP);
                }
            }

            // Tab label, centred
            float scale = 6f / 7f;
            int tw2 = (int) (font.width(tab.label) * scale);
            int tlx = tx + (tw - tw2) / 2;
            int tly = ty + (tabBarH - (int) Math.ceil(8 * scale)) / 2;

            if (Math.abs(scale - 1f) < 1e-3f) {
                g.drawString(font, tab.label, tlx, tly, fg, false);
            } else {
                g.pose().pushPose();
                g.pose().translate(tlx, tly, 0);
                g.pose().scale(scale, scale, 1f);
                g.drawString(font, tab.label, 0, 0, fg, false);
                g.pose().popPose();
            }
        }

        // ── Content area ──────────────────────────────────────────────────────
        if (activeIndex >= 0 && activeIndex < tabs.size()) {
            TesseraPanel content = tabs.get(activeIndex).content;
            int contentY = y + tabBarH;
            int contentH = height - tabBarH;
            content.setPosition(x, contentY);
            content.setSize(width, contentH);
            content.layout();
            content.render(g, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!visible || !active || tabs.isEmpty()) return false;

        // ── Tab bar click ─────────────────────────────────────────────────────
        if (mx >= x && mx < x + width && my >= y && my < y + tabBarH) {
            int tabCount = tabs.size();
            int tabW     = width / tabCount;
            for (int i = 0; i < tabCount; i++) {
                int tx = x + i * tabW;
                int tw = (i == tabCount - 1) ? width - i * tabW : tabW;
                if (mx >= tx && mx < tx + tw) {
                    activeIndex = i;
                    return true;
                }
            }
            return true; // still inside tab bar
        }

        // ── Content area click ────────────────────────────────────────────────
        if (activeIndex >= 0 && activeIndex < tabs.size()) {
            return tabs.get(activeIndex).content.mouseClicked(mx, my, btn);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeIndex >= 0 && activeIndex < tabs.size()) {
            return tabs.get(activeIndex).content.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (activeIndex >= 0 && activeIndex < tabs.size()) {
            return tabs.get(activeIndex).content.charTyped(c, modifiers);
        }
        return false;
    }

    @Override
    public void mouseDragged(double mx, double my, int btn) {
        if (activeIndex >= 0 && activeIndex < tabs.size()) {
            tabs.get(activeIndex).content.mouseDragged(mx, my, btn);
        }
    }

    @Override
    public void mouseReleased(double mx, double my, int btn) {
        if (activeIndex >= 0 && activeIndex < tabs.size()) {
            tabs.get(activeIndex).content.mouseReleased(mx, my, btn);
        }
    }
}
