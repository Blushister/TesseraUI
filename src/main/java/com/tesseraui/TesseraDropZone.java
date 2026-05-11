package com.tesseraui;

/**
 * Interface implemented by widgets that accept drag-and-drop payloads.
 *
 * <p>Register a {@code TesseraDropZone} on a {@link TesseraPanel} via
 * {@link TesseraPanel#dropZone(TesseraDropZone)}.  During a drag operation,
 * {@link TesseraDragContext} calls {@link #dropBounds()} every frame to
 * highlight the currently hovered zone, and {@link #onDrop(Object)} when the
 * user releases the mouse button over an accepting zone.</p>
 */
public interface TesseraDropZone {

    /**
     * Returns {@code true} if this zone accepts the given payload type/value.
     *
     * @param payload the drag payload set on the dragged widget
     * @return {@code true} to highlight and accept
     */
    boolean accepts(Object payload);

    /**
     * Called when the user drops a payload onto this zone after
     * {@link #accepts(Object)} returned {@code true}.
     *
     * @param payload the drag payload
     */
    void onDrop(Object payload);

    /**
     * Returns the screen-space bounding rectangle of this drop zone.
     * Used by {@link TesseraDragContext} to hit-test the cursor position.
     */
    Rect dropBounds();
}
