package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * A floating context menu that can be attached to any {@link TesseraPanel} via
 * {@link TesseraPanel#onRightClick(Runnable)}.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * panel.onRightClick(() ->
 *     TesseraContextMenu.builder()
 *         .item("Copy",   () -> doCopy())
 *         .item("Delete", () -> doDelete())
 *         .separator()
 *         .item("Rename", () -> doRename(), canRename)
 *         .showAt(mx, my)
 * );
 * }</pre>
 *
 * <p>Call {@link #render(GuiGraphics, int, int)} at the end of your screen's
 * {@code render()} (after all other panels but before toasts/popups).</p>
 * <p>Forward mouse events with {@link #mouseClicked(double, double, int)} before
 * delegating to your own widgets.</p>
 */
public final class TesseraContextMenu {

    // ── Style constants ────────────────────────────────────────────────────────

    private static final int BG         = 0xFF1e293b;
    private static final int BORDER     = 0xFF334155;
    private static final int ITEM_HOVER = 0xFF2d3f55;
    private static final int SEP_COLOR  = 0xFF334155;
    private static final int TEXT_ON    = 0xFFe2e8f0;
    private static final int TEXT_OFF   = 0xFF64748b;

    private static final int ITEM_H   = 12;   // height of a normal item row
    private static final int SEP_H    = 5;    // height of a separator row
    private static final int PAD_V    = 3;    // vertical text padding inside item
    private static final int PAD_H    = 8;    // horizontal text padding

    // ── State ─────────────────────────────────────────────────────────────────

    private static List<Item> activeItems = null;
    private static int menuX, menuY, menuW, menuH;

    private TesseraContextMenu() {}

    // ── Public API ─────────────────────────────────────────────────────────────

    /** An item in the context menu. {@code label == null} means separator. */
    public record Item(String label, Runnable action, boolean enabled) {
        /** Separator sentinel. */
        static Item separator() { return new Item(null, null, false); }
        boolean isSeparator()   { return label == null; }
    }

    /** Returns a builder for constructing and showing a context menu. */
    public static Builder builder() { return new Builder(); }

    /** Closes the active context menu (if any). */
    public static void close() { activeItems = null; }

    /** Returns {@code true} if a context menu is currently open. */
    public static boolean isOpen() { return activeItems != null; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static final class Builder {
        private final List<Item> items = new ArrayList<>();

        private Builder() {}

        /** Adds an enabled item. */
        public Builder item(String label, Runnable action) {
            items.add(new Item(label, action, true));
            return this;
        }

        /** Adds an item whose enabled state is controlled explicitly. */
        public Builder item(String label, Runnable action, boolean enabled) {
            items.add(new Item(label, action, enabled));
            return this;
        }

        /** Adds a visual separator. */
        public Builder separator() {
            items.add(Item.separator());
            return this;
        }

        /**
         * Displays the context menu at screen position {@code (x, y)}.
         * Any previously open menu is replaced.
         */
        public void showAt(int x, int y) {
            if (items.isEmpty()) return;
            activeItems = List.copyOf(items);
            menuX = x;
            menuY = y;

            // Measure required width from item labels
            var font = Minecraft.getInstance().font;
            int maxLabelW = 0;
            for (Item it : activeItems) {
                if (!it.isSeparator()) {
                    maxLabelW = Math.max(maxLabelW, font.width(it.label()));
                }
            }
            menuW = maxLabelW + PAD_H * 2 + 2; // +2 for border
            menuH = 2; // top + bottom border
            for (Item it : activeItems) menuH += it.isSeparator() ? SEP_H : ITEM_H;
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Renders the active context menu on top of everything else.
     * Call this after all regular panel rendering, before toasts.
     */
    public static void render(GuiGraphics g, int mx, int my) {
        if (activeItems == null) return;

        var font = Minecraft.getInstance().font;

        // Clamp position so menu stays on screen
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int rx = Math.min(menuX, sw - menuW - 2);
        int ry = Math.min(menuY, sh - menuH - 2);
        if (rx < 0) rx = 0;
        if (ry < 0) ry = 0;

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

        // Background + border
        g.fill(rx,          ry,          rx + menuW,     ry + menuH,     BG);
        g.fill(rx,          ry,          rx + menuW,     ry + 1,         BORDER); // top
        g.fill(rx,          ry + menuH - 1, rx + menuW, ry + menuH,     BORDER); // bottom
        g.fill(rx,          ry,          rx + 1,         ry + menuH,     BORDER); // left
        g.fill(rx + menuW - 1, ry,      rx + menuW,     ry + menuH,     BORDER); // right

        int curY = ry + 1;
        for (Item it : activeItems) {
            if (it.isSeparator()) {
                int sy = curY + SEP_H / 2;
                g.fill(rx + 1, sy, rx + menuW - 1, sy + 1, SEP_COLOR);
                curY += SEP_H;
            } else {
                // Hover highlight
                if (mx >= rx + 1 && mx < rx + menuW - 1 && my >= curY && my < curY + ITEM_H) {
                    g.fill(rx + 1, curY, rx + menuW - 1, curY + ITEM_H, ITEM_HOVER);
                }
                int textColor = it.enabled() ? TEXT_ON : TEXT_OFF;
                g.drawString(font, it.label(), rx + PAD_H, curY + PAD_V, textColor, false);
                curY += ITEM_H;
            }
        }

        g.pose().popPose();
    }

    // ── Mouse event forwarding ────────────────────────────────────────────────

    /**
     * Handles mouse clicks for the context menu.
     *
     * <ul>
     *   <li>Click outside the menu → close and return {@code false} (event not consumed).</li>
     *   <li>Click on an enabled item → execute its action, close, return {@code true}.</li>
     *   <li>Click on a disabled item or separator → consume the event, menu stays open.</li>
     * </ul>
     *
     * <p>Call this <em>before</em> forwarding to your own widgets so the menu always gets
     * priority.</p>
     */
    public static boolean mouseClicked(double mx, double my, int btn) {
        if (activeItems == null) return false;

        // Compute current rx/ry (same clamping logic as render)
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int rx = Math.min(menuX, sw - menuW - 2);
        int ry = Math.min(menuY, sh - menuH - 2);
        if (rx < 0) rx = 0;
        if (ry < 0) ry = 0;

        boolean inside = mx >= rx && mx < rx + menuW && my >= ry && my < ry + menuH;
        if (!inside) {
            close();
            return false; // allow click to propagate to widgets below
        }

        // Identify clicked item
        int curY = ry + 1;
        for (Item it : activeItems) {
            int rowH = it.isSeparator() ? SEP_H : ITEM_H;
            if (my >= curY && my < curY + rowH) {
                if (!it.isSeparator() && it.enabled() && it.action() != null) {
                    close();
                    it.action().run();
                }
                // Even if disabled/separator, consume the event (stays inside menu)
                return true;
            }
            curY += rowH;
        }

        return true; // click was inside the menu box
    }
}
