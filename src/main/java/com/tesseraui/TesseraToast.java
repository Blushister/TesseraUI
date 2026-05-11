package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

/**
 * Simple on-screen toast notification system for TesseraUI screens.
 *
 * <p>Toasts are rendered bottom-right, stacked vertically, and auto-dismiss after their
 * configured duration.  Call {@link #render(GuiGraphics, int, int)} from your screen's
 * {@code render()} method to display all active toasts.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // In a button handler:
 * TesseraToast.success("Saved!");
 * TesseraToast.error("Something went wrong.");
 * TesseraToast.show("Info message");
 *
 * // In TesseraScreen.render():
 * TesseraToast.render(g, width, height);
 * }</pre>
 */
public final class TesseraToast {

    /** Internal record holding the data for a single toast notification. */
    private record Toast(String message, int color, long expireMs) {}

    /** Active toast queue — processed in insertion order. */
    private static final Queue<Toast> TOASTS = new ArrayDeque<>();

    /** Default display duration in milliseconds. */
    private static final long DEFAULT_DURATION_MS = 3000L;

    /** Background color: dark translucent navy. */
    private static final int BG_COLOR = 0xE0111827;

    /** Bottom-right margin from screen edges in pixels. */
    private static final int MARGIN = 6;

    /** Vertical padding inside the toast box. */
    private static final int PAD_V = 4;

    /** Horizontal padding inside the toast box. */
    private static final int PAD_H = 8;

    /** Height of each toast row in pixels. */
    private static final int ROW_H = 14;

    /** Vertical gap between stacked toasts. */
    private static final int GAP = 3;

    private TesseraToast() {}

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Shows an informational toast (white text) for 3 seconds.
     *
     * @param message the text to display
     */
    public static void show(String message) {
        show(message, 0xFFFFFFFF);
    }

    /**
     * Shows a toast with a custom text color for 3 seconds.
     *
     * @param message the text to display
     * @param color   ARGB text color
     */
    public static void show(String message, int color) {
        show(message, color, DEFAULT_DURATION_MS);
    }

    /**
     * Shows a toast with a custom text color and custom duration.
     *
     * @param message    the text to display
     * @param color      ARGB text color
     * @param durationMs how long to show the toast in milliseconds
     */
    public static void show(String message, int color, long durationMs) {
        if (message == null || message.isBlank()) return;
        long expire = System.currentTimeMillis() + durationMs;
        TOASTS.offer(new Toast(message, color, expire));
    }

    /**
     * Shows a success toast (green text) for 3 seconds.
     *
     * @param message the text to display
     */
    public static void success(String message) {
        show(message, 0xFF4ADE80);
    }

    /**
     * Shows an error toast (red text) for 3 seconds.
     *
     * @param message the text to display
     */
    public static void error(String message) {
        show(message, 0xFFF87171);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Renders all active toasts in the bottom-right corner of the screen.
     * Expired toasts are removed before rendering.
     *
     * <p>Call this from your {@link TesseraScreen#render} override after rendering the main UI.</p>
     *
     * @param g       the GuiGraphics context
     * @param screenW the current GUI-scaled screen width
     * @param screenH the current GUI-scaled screen height
     */
    public static void render(GuiGraphics g, int screenW, int screenH) {
        long now = System.currentTimeMillis();

        // Remove expired toasts
        TOASTS.removeIf(t -> t.expireMs() < now);

        if (TOASTS.isEmpty()) return;

        var font = Minecraft.getInstance().font;
        float scale = 6f / 7f;

        // Compute layout: stacked bottom-up from screen bottom-right
        Toast[] active = TOASTS.toArray(new Toast[0]);
        int count = active.length;

        // Total height of the toast stack
        int stackH = count * ROW_H + Math.max(0, count - 1) * GAP;
        int startY = screenH - MARGIN - stackH;

        g.pose().pushPose();
        g.pose().translate(0, 0, 400); // render above normal UI

        for (int i = 0; i < count; i++) {
            Toast toast = active[i];
            String msg = toast.message();
            int textW = (int) (font.width(msg) * scale);
            int boxW  = textW + PAD_H * 2;
            int boxX  = screenW - MARGIN - boxW;
            int boxY  = startY + i * (ROW_H + GAP);

            // Background
            g.fill(boxX, boxY, boxX + boxW, boxY + ROW_H, BG_COLOR);

            // Left accent stripe in the toast color
            int accent = toast.color();
            g.fill(boxX, boxY, boxX + 2, boxY + ROW_H, accent);

            // Text
            g.pose().pushPose();
            g.pose().translate(boxX + PAD_H, boxY + PAD_V, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(font, msg, 0, 0, accent, false);
            g.pose().popPose();
        }

        g.pose().popPose();
    }

    /**
     * Clears all pending toasts immediately.  Useful when closing a screen.
     */
    public static void clear() {
        TOASTS.clear();
    }
}
