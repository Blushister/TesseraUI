package com.tesseraui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Core rendering contract for all TesseraUI widgets.
 *
 * <p>Implement this interface (or extend {@link TesseraElement}) to create custom widgets
 * that participate in {@link TesseraPanel} layout and receive mouse events.</p>
 */
public interface TesseraWidget {

    /** Renders this widget at the given mouse position. */
    void render(GuiGraphics g, int mx, int my);

    /**
     * Handles a mouse button press.
     *
     * @return {@code true} if this widget consumed the event
     */
    boolean mouseClicked(double mx, double my, int btn);

    /** Called when a previously pressed mouse button is released. */
    default void mouseReleased(double mx, double my, int btn) {}

    /** Called when the mouse is dragged (button held + moved). */
    default void mouseDragged(double mx, double my, int btn) {}

    /** Returns the axis-aligned bounding box of this widget. */
    Rect bounds();

    /** Enables or disables this widget. Disabled widgets ignore input and render dimmed. */
    void setActive(boolean active);

    /** Returns {@code true} if this widget is currently enabled. */
    boolean isActive();

    /** Repositions this widget. Called by {@link TesseraPanel} during layout. */
    void setPosition(int x, int y);

    /** Resizes this widget. Called by {@link TesseraPanel} during layout for flex children. */
    void setSize(int w, int h);

    /** Returns the current pixel width. Used by {@link TesseraPanel} during layout. */
    int getWidth();

    /** Returns the current pixel height. Used by {@link TesseraPanel} during layout. */
    int getHeight();

    /**
     * Handles a mouse scroll event. The coordinates are in screen space; {@code dy} is
     * the vertical scroll delta (positive = scroll up in most systems).
     *
     * @return {@code true} if this widget consumed the event
     */
    default boolean mouseScrolled(double mx, double my, double dy) { return false; }

    /** Handles a key press while focused. Returns {@code true} if consumed. */
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }

    /** Handles a typed character while focused. Returns {@code true} if consumed. */
    default boolean charTyped(char c, int modifiers) { return false; }

    /** Sets the focus state of this widget. */
    default void setFocused(boolean focused) {}

    /** Returns {@code true} if this widget currently holds keyboard focus. */
    default boolean isFocused() { return false; }

    /** Returns {@code true} if this widget is visible (rendered and interactive). */
    default boolean isVisible() { return true; }

    /**
     * Sets the visibility of this widget.
     * An invisible widget is not rendered and does not receive input events,
     * but its space in the parent layout is preserved (v-show semantics).
     */
    default void setVisible(boolean visible) {}

    /**
     * Returns the tooltip text to display when this widget is hovered, or {@code null} / blank
     * if no tooltip is configured.
     */
    default String getTooltip() { return null; }

    /**
     * Sets the tooltip text for this widget.
     * The tooltip is displayed by the parent {@link TesseraPanel} when the mouse hovers
     * over this widget.
     */
    default void setTooltip(String text) {}
}
