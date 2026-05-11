package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Developer debug overlay — draws widget bounds when enabled.
 *
 * <p>Toggle with {@code TesseraDebugOverlay.toggle()} (bound to the {@code I} key
 * inside any {@link TesseraScreen}).  Each nesting depth is drawn in a different
 * colour:</p>
 * <ul>
 *   <li>depth 0 → red   {@code 0xFFFF4444}</li>
 *   <li>depth 1 → green {@code 0xFF44FF44}</li>
 *   <li>depth 2 → blue  {@code 0xFF4488FF}</li>
 *   <li>depth 3+ → yellow {@code 0xFFFFFF44}</li>
 * </ul>
 *
 * <p>When the mouse hovers a widget a small tooltip is rendered that shows the
 * widget's Java class, its bounds (x, y, w, h) and its tooltip text (if any).</p>
 */
public final class TesseraDebugOverlay {

    private static boolean enabled = false;

    private TesseraDebugOverlay() {}

    public static void toggle() { enabled = !enabled; }

    public static boolean isEnabled() { return enabled; }

    /**
     * Renders debug bounds for every widget in {@code root} recursively.
     * Call this at the very end of {@link TesseraScreen#render} so the outlines
     * appear on top of everything else.
     *
     * @param g    GuiGraphics context
     * @param root the root panel of the screen
     * @param mx   mouse x
     * @param my   mouse y
     */
    public static void render(GuiGraphics g, TesseraPanel root, int mx, int my) {
        if (!enabled || root == null) return;
        renderWidget(g, root, mx, my, 0);
    }

    // ── colours indexed by depth ──────────────────────────────────────────────

    private static int depthColor(int depth) {
        return switch (depth) {
            case 0  -> 0xFFFF4444; // red
            case 1  -> 0xFF44FF44; // green
            case 2  -> 0xFF4488FF; // blue
            default -> 0xFFFFFF44; // yellow
        };
    }

    // ── recursive walker ──────────────────────────────────────────────────────

    private static void renderWidget(GuiGraphics g, TesseraWidget w, int mx, int my, int depth) {
        Rect b = w.bounds();
        int color = depthColor(depth);

        // 1-pixel outline using four fill() calls
        drawOutline(g, b.x(), b.y(), b.w(), b.h(), color);

        // Tooltip when hovered
        if (b.contains(mx, my)) {
            renderDebugTooltip(g, w, b, mx, my);
        }

        // Recurse into TesseraPanel children
        if (w instanceof TesseraPanel panel) {
            for (var child : getChildren(panel)) {
                if (child.isVisible()) {
                    renderWidget(g, child, mx, my, depth + 1);
                }
            }
        }
    }

    private static void drawOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        g.fill(x,         y,         x + w,     y + 1,     color); // top
        g.fill(x,         y + h - 1, x + w,     y + h,     color); // bottom
        g.fill(x,         y,         x + 1,     y + h,     color); // left
        g.fill(x + w - 1, y,         x + w,     y + h,     color); // right
    }

    private static void renderDebugTooltip(GuiGraphics g, TesseraWidget w, Rect b, int mx, int my) {
        var font = Minecraft.getInstance().font;

        String className = w.getClass().getSimpleName();
        String bounds    = "x=" + b.x() + " y=" + b.y() + " w=" + b.w() + " h=" + b.h();
        String tooltip   = w.getTooltip();

        // Build lines
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(className);
        lines.add(bounds);
        if (tooltip != null && !tooltip.isBlank()) lines.add("tip: " + tooltip);

        float scale = 6f / 7f;
        int lineH   = 9;
        int padH    = 5;
        int padV    = 3;

        int maxW = 0;
        for (String line : lines) maxW = Math.max(maxW, (int)(font.width(line) * scale));
        int boxW = maxW + padH * 2;
        int boxH = lines.size() * lineH + padV * 2;

        int tx = mx + 8;
        int ty = my - boxH - 4;
        if (ty < 2)              ty = my + 4;
        if (tx + boxW > 1920)    tx = mx - boxW - 4;

        g.pose().pushPose();
        g.pose().translate(tx, ty, 600);

        // Background + border
        g.fill(0, 0, boxW, boxH, 0xE0000000);
        g.fill(0, 0, boxW, 1,    0xFF00FFFF);
        g.fill(0, boxH - 1, boxW, boxH, 0xFF00FFFF);
        g.fill(0, 0, 1, boxH,    0xFF00FFFF);
        g.fill(boxW - 1, 0, boxW, boxH, 0xFF00FFFF);

        // Text lines
        g.pose().translate(padH, padV, 0);
        g.pose().scale(scale, scale, 1f);
        for (int i = 0; i < lines.size(); i++) {
            int lineColor = i == 0 ? 0xFF00FFFF : 0xFFFFFFFF;
            g.drawString(font, lines.get(i), 0, (int)(i * lineH / scale), lineColor, false);
        }

        g.pose().popPose();
    }

    /**
     * Reflectively extracts the children list from a {@link TesseraPanel}.
     * TesseraPanel stores children in a private field; we use the public
     * {@link TesseraPanel#fitContentHeight()} as a proxy to detect if children
     * exist, but we still need access for recursion.
     *
     * <p>We work around the access restriction by casting via the known API:
     * {@link TesseraPanel} is in the same package so we can call package-private
     * helpers if added there.  For now we expose a package-private accessor.</p>
     */
    static java.util.List<TesseraWidget> getChildren(TesseraPanel panel) {
        return panel.debugChildren();
    }
}
