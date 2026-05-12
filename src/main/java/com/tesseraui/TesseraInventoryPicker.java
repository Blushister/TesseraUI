package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * A floating inventory-picker overlay.
 *
 * <p>When opened it shows the player's full inventory (main 3×9 + hotbar 1×9).
 * Clicking a slot copies that {@link ItemStack} to the caller's callback, then
 * closes the picker.  Clicking anywhere outside the panel closes it without
 * picking anything.</p>
 *
 * <p>Usage — wire it to a {@link TesseraItemSlot}:</p>
 * <pre>{@code
 * slot.inventoryPicker(true);                      // enable picker on click
 * slot.onItemPicked(stack -> doSomething(stack));  // optional custom callback
 * }</pre>
 *
 * <p>Render and event forwarding are handled automatically by
 * {@link TesseraScreen#renderTesseraOverlays} and
 * {@link TesseraPanel#mouseClicked}.</p>
 */
public final class TesseraInventoryPicker {

    // ── Layout constants ───────────────────────────────────────────────────────

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP  =  1;
    private static final int COLS      =  9;
    private static final int PAD       =  4;
    private static final int TITLE_H   =  9;
    private static final int TITLE_GAP =  2;
    private static final int ROW_GAP   =  3; // gap between main inv and hotbar

    // Inner slot-grid width: 9 slots × (18+1) − 1 trailing gap = 170 px
    private static final int INNER_W =
            COLS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;          // 170

    // Slot-grid height: 3 main rows + hotbar + gaps
    private static final int SLOTS_H =
            3 * SLOT_SIZE + 2 * SLOT_GAP                       // 56
            + ROW_GAP                                           // +3
            + SLOT_SIZE;                                        // +18 = 77

    /** Total panel width (including 1-px border and padding on each side). */
    public static final int PANEL_W = 2 + 2 * PAD + INNER_W;  // 180

    /** Total panel height (including 1-px border, padding, title). */
    public static final int PANEL_H =
            2 + 2 * PAD + TITLE_H + TITLE_GAP + SLOTS_H;      // 98

    // ── Colors ─────────────────────────────────────────────────────────────────

    private static final int BG          = 0xFF1e293b;
    private static final int BORDER      = 0xFF475569;
    private static final int SLOT_BG     = 0xFF1f2937;
    private static final int SLOT_HOVER  = 0xFF374151;
    private static final int SLOT_BORDER = 0xFF374151;
    private static final int HOTBAR_SEP  = 0xFF334155;

    // ── State ──────────────────────────────────────────────────────────────────

    private static boolean               open     = false;
    private static int                   panelX, panelY;
    private static Consumer<ItemStack>   callback;

    private TesseraInventoryPicker() {}

    // ── Public API ─────────────────────────────────────────────────────────────

    public static boolean isOpen() { return open; }

    /**
     * Opens the picker anchored near {@code (anchorX, anchorY)}.
     * The panel is clamped so it stays fully on screen.
     *
     * @param anchorX  preferred left edge of the picker (e.g. slot x)
     * @param anchorY  preferred top edge of the picker (e.g. slot bottom + 2)
     * @param cb       called with a copy of the picked stack; never null
     */
    public static void open(int anchorX, int anchorY, Consumer<ItemStack> cb) {
        callback = cb;
        open     = true;

        var win = Minecraft.getInstance().getWindow();
        int sw  = win.getGuiScaledWidth();
        int sh  = win.getGuiScaledHeight();

        panelX = Math.max(0, Math.min(anchorX, sw - PANEL_W - 2));
        panelY = Math.max(0, Math.min(anchorY, sh - PANEL_H - 2));
    }

    /** Closes the picker without picking an item. */
    public static void close() {
        open     = false;
        callback = null;
    }

    // ── Rendering ──────────────────────────────────────────────────────────────

    public static void render(GuiGraphics g, int mx, int my) {
        if (!open) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) { close(); return; }

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

        // Background & border
        g.fill(panelX,              panelY,              panelX + PANEL_W,     panelY + PANEL_H,     BG);
        g.fill(panelX,              panelY,              panelX + PANEL_W,     panelY + 1,            BORDER);
        g.fill(panelX,              panelY + PANEL_H - 1, panelX + PANEL_W,   panelY + PANEL_H,      BORDER);
        g.fill(panelX,              panelY,              panelX + 1,           panelY + PANEL_H,      BORDER);
        g.fill(panelX + PANEL_W - 1, panelY,            panelX + PANEL_W,     panelY + PANEL_H,      BORDER);

        // Title
        g.drawString(mc.font, "Inventaire", panelX + 1 + PAD, panelY + 1 + PAD, TesseraPalette.COPPER, false);

        int slotsX = panelX + 1 + PAD;
        int slotsY = panelY + 1 + PAD + TITLE_H + TITLE_GAP;

        var inv = mc.player.getInventory();

        // Main inventory (slots 9–35, displayed in rows 0–2)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLS; col++) {
                int sx = slotsX + col * (SLOT_SIZE + SLOT_GAP);
                int sy = slotsY + row * (SLOT_SIZE + SLOT_GAP);
                renderSlot(g, mx, my, sx, sy, inv.getItem(9 + row * COLS + col));
            }
        }

        // Separator between main inv and hotbar
        int sepY    = slotsY + 3 * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP + 1;
        g.fill(slotsX, sepY, slotsX + INNER_W, sepY + 1, HOTBAR_SEP);

        // Hotbar (slots 0–8, displayed in row 3)
        int hotbarY = sepY + ROW_GAP - 1;
        for (int col = 0; col < COLS; col++) {
            int sx = slotsX + col * (SLOT_SIZE + SLOT_GAP);
            renderSlot(g, mx, my, sx, hotbarY, inv.getItem(col));
        }

        g.pose().popPose();
    }

    private static void renderSlot(GuiGraphics g, int mx, int my,
                                    int sx, int sy, ItemStack stack) {
        boolean hovered = mx >= sx && mx < sx + SLOT_SIZE
                       && my >= sy && my < sy + SLOT_SIZE;

        g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, hovered ? SLOT_HOVER : SLOT_BG);
        // 1-px border
        g.fill(sx,              sy,              sx + SLOT_SIZE, sy + 1,              SLOT_BORDER);
        g.fill(sx,              sy + SLOT_SIZE - 1, sx + SLOT_SIZE, sy + SLOT_SIZE,  SLOT_BORDER);
        g.fill(sx,              sy,              sx + 1,         sy + SLOT_SIZE,      SLOT_BORDER);
        g.fill(sx + SLOT_SIZE - 1, sy,          sx + SLOT_SIZE, sy + SLOT_SIZE,      SLOT_BORDER);

        if (!stack.isEmpty()) {
            g.renderItem(stack, sx + 1, sy + 1);
            if (stack.getCount() > 1) {
                var font  = Minecraft.getInstance().font;
                String cnt = String.valueOf(stack.getCount());
                float  sc  = 6f / 7f;
                int    tw  = (int) (font.width(cnt) * sc);
                int    tx  = sx + SLOT_SIZE - 2 - tw;
                int    ty  = sy + SLOT_SIZE - 2 - (int) (8 * sc);
                g.pose().pushPose();
                g.pose().translate(tx, ty, 10);
                g.pose().scale(sc, sc, 1f);
                g.drawString(font, cnt, 0, 0, 0xFFFFFFFF, true);
                g.pose().popPose();
            }
        }

        if (hovered) g.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0x40FFFFFF);
    }

    // ── Mouse events ───────────────────────────────────────────────────────────

    /**
     * Handles a mouse click.
     *
     * <ul>
     *   <li>Click inside a slot → pick the item, close, return {@code true}.</li>
     *   <li>Click inside the panel but not on a slot → return {@code true} (consume).</li>
     *   <li>Click outside the panel → close, return {@code false} (let the click
     *       propagate to the widget below).</li>
     * </ul>
     */
    public static boolean mouseClicked(double mx, double my, int btn) {
        if (!open) return false;

        boolean inside = mx >= panelX && mx < panelX + PANEL_W
                      && my >= panelY && my < panelY + PANEL_H;

        if (!inside) {
            close();
            return false; // propagate so the widget below can also react
        }

        if (btn == 0) {
            int idx = slotIndexAt(mx, my);
            if (idx >= 0) {
                var mc = Minecraft.getInstance();
                if (mc.player != null && callback != null) {
                    callback.accept(mc.player.getInventory().getItem(idx).copy());
                }
                close();
            }
        }
        return true;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Maps screen coordinates to a player-inventory index, or {@code -1}. */
    private static int slotIndexAt(double mx, double my) {
        int slotsX  = panelX + 1 + PAD;
        int slotsY  = panelY + 1 + PAD + TITLE_H + TITLE_GAP;
        int hotbarY = slotsY + 3 * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP + ROW_GAP;

        // Main inventory rows 0–2
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLS; col++) {
                int sx = slotsX + col * (SLOT_SIZE + SLOT_GAP);
                int sy = slotsY + row * (SLOT_SIZE + SLOT_GAP);
                if (mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE)
                    return 9 + row * COLS + col;
            }
        }

        // Hotbar row
        for (int col = 0; col < COLS; col++) {
            int sx = slotsX + col * (SLOT_SIZE + SLOT_GAP);
            if (mx >= sx && mx < sx + SLOT_SIZE && my >= hotbarY && my < hotbarY + SLOT_SIZE)
                return col;
        }

        return -1;
    }
}
