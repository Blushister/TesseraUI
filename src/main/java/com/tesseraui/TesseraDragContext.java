package com.tesseraui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Global singleton state for the current drag-and-drop operation.
 *
 * <p>A drag is started by {@link TesseraPanel#mouseClicked} on a
 * {@code draggable} panel, updated via {@link TesseraScreen#mouseDragged},
 * and ended by {@link TesseraScreen#mouseReleased} or
 * {@link TesseraPanel#mouseReleased}.</p>
 *
 * <p>Call {@link #render(GuiGraphics)} <em>last</em> in your screen's
 * {@code render()} so the ghost widget paints on top of everything else.</p>
 */
public final class TesseraDragContext {

    private TesseraDragContext() {}

    // ── State ─────────────────────────────────────────────────────────────────

    private static TesseraWidget draggedWidget = null;
    private static Object        dragPayload   = null;
    private static int           startX, startY;
    private static int           currentX, currentY;
    private static TesseraDropZone dropTarget  = null;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Begins a drag operation.
     *
     * @param source  the widget being dragged
     * @param payload the data carried by the drag (may be {@code null})
     * @param mx      mouse X at drag start
     * @param my      mouse Y at drag start
     */
    public static void startDrag(TesseraWidget source, Object payload, int mx, int my) {
        draggedWidget = source;
        dragPayload   = payload;
        startX   = mx;
        startY   = my;
        currentX = mx;
        currentY = my;
        dropTarget = null;
    }

    /**
     * Updates the ghost position during a drag.
     *
     * @param mx current mouse X
     * @param my current mouse Y
     */
    public static void updatePosition(int mx, int my) {
        currentX = mx;
        currentY = my;
    }

    /**
     * Ends the current drag, delivering the payload to the hovered
     * {@link TesseraDropZone} if it accepts the payload.
     *
     * @param mx mouse X at release
     * @param my mouse Y at release
     */
    public static void endDrag(int mx, int my) {
        if (draggedWidget != null && dropTarget != null) {
            if (dropTarget.accepts(dragPayload)) {
                dropTarget.onDrop(dragPayload);
            }
        }
        draggedWidget = null;
        dragPayload   = null;
        dropTarget    = null;
    }

    /** Returns {@code true} if a drag is currently in progress. */
    public static boolean isDragging() {
        return draggedWidget != null;
    }

    /** Returns the payload carried by the current drag, or {@code null}. */
    public static Object payload() {
        return dragPayload;
    }

    /** Sets the drop zone currently under the cursor (called by registered zones). */
    public static void setDropTarget(TesseraDropZone zone) {
        dropTarget = zone;
    }

    /** Returns the drop zone currently under the cursor, or {@code null}. */
    public static TesseraDropZone dropTarget() {
        return dropTarget;
    }

    /**
     * Renders the dragged widget as a ghost at the current cursor position.
     * Must be called <em>last</em> in the screen's render method so it paints
     * on top of all other widgets.
     *
     * @param g the GuiGraphics context
     */
    public static void render(GuiGraphics g) {
        if (draggedWidget == null) return;

        int w = draggedWidget.getWidth();
        int h = draggedWidget.getHeight();
        // Offset the ghost slightly so the cursor is inside it
        int gx = currentX - w / 2;
        int gy = currentY - h / 2;

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

        // Temporarily reposition the widget to the ghost location
        int origX = draggedWidget.bounds().x();
        int origY = draggedWidget.bounds().y();
        draggedWidget.setPosition(gx, gy);

        // Semi-transparent ghost overlay effect
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 0.7f);
        draggedWidget.render(g, -1, -1);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // Draw a highlight border on the ghost
        g.fill(gx, gy, gx + w, gy + 1, TesseraPalette.COPPER_HI);
        g.fill(gx, gy + h - 1, gx + w, gy + h, TesseraPalette.COPPER_HI);
        g.fill(gx, gy, gx + 1, gy + h, TesseraPalette.COPPER_HI);
        g.fill(gx + w - 1, gy, gx + w, gy + h, TesseraPalette.COPPER_HI);

        // Restore original position
        draggedWidget.setPosition(origX, origY);

        g.pose().popPose();
    }
}
