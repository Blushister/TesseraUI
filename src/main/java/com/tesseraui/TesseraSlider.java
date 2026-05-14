package com.tesseraui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * A horizontal slider widget with a draggable thumb.
 *
 * <p>Extends {@link TesseraElement} and therefore inherits visibility support
 * ({@code isVisible()}/{@code setVisible()}) for {@code v-show} semantics.</p>
 *
 * <p>The {@code oninput} handler is called with the current value as a String
 * whenever a click or drag changes the value.</p>
 */
public class TesseraSlider extends TesseraElement {

    private static String activeDragKey = null;

    private final float min;
    private final float max;
    private float value;
    private boolean dragging = false;
    private String dragKey = null;

    private Consumer<String> onInput;

    private static final int COLOR_TRACK       = 0xFF3A3A3A;
    private static final int COLOR_TRACK_FILL  = TesseraPalette.COPPER_LO;
    private static final int COLOR_THUMB        = TesseraPalette.COPPER_HI;
    private static final int COLOR_THUMB_HOVER  = 0xFFF0C090;
    private static final int THUMB_W            = 4;

    public TesseraSlider(int x, int y, int width, int height, float min, float max, float value) {
        super(x, y, width, height);
        // Guard: if min >= max the slider is a no-op
        this.min = min;
        this.max = max;
        this.value = (min < max) ? clamp(value, min, max) : min;
    }

    public TesseraSlider onInput(Consumer<String> handler) {
        this.onInput = handler;
        return this;
    }

    public TesseraSlider dragKey(String key) {
        this.dragKey = (key == null || key.isBlank()) ? null : key;
        return this;
    }

    public float getValue() { return value; }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Returns the thumb's left-edge X coordinate in screen space. */
    private int thumbX() {
        if (min >= max) return x;
        float ratio = (value - min) / (max - min);
        int trackW = width - THUMB_W;
        return x + Math.round(ratio * trackW);
    }

    /** Converts a screen X position to a slider value, clamped to [min, max]. */
    private float xToValue(double mx) {
        if (min >= max) return min;
        int trackW = width - THUMB_W;
        float ratio = (float) ((mx - x - THUMB_W / 2.0) / Math.max(1, trackW));
        return clamp(min + ratio * (max - min), min, max);
    }

    private void updateValueFromMouse(double mx) {
        float next = xToValue(mx);
        if (Float.compare(next, value) == 0) return;
        value = next;
        emitInput();
    }

    private void emitInput() {
        if (onInput == null) return;
        onInput.accept(formatValue(value));
    }

    private static String formatValue(float value) {
        if (!Float.isFinite(value)) return String.valueOf(value);
        float rounded = Math.round(value * 100.0f) / 100.0f;
        if (rounded == Math.floor(rounded)) return String.valueOf((int) rounded);
        String text = String.format(Locale.ROOT, "%.2f", rounded);
        while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
        if (text.endsWith(".")) text = text.substring(0, text.length() - 1);
        return text;
    }

    private boolean ownsActiveDrag() {
        return dragKey != null && dragKey.equals(activeDragKey);
    }

    private void captureDrag() {
        if (dragKey != null) activeDragKey = dragKey;
    }

    private void releaseDrag() {
        if (ownsActiveDrag()) activeDragKey = null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        if (!visible) return;
        // Track background
        int trackY = y + height / 2 - 1;
        g.fill(x, trackY, x + width, trackY + 2, COLOR_TRACK);

        // Track fill (left of thumb)
        int tx = thumbX();
        if (tx > x) g.fill(x, trackY, tx, trackY + 2, COLOR_TRACK_FILL);

        // Thumb
        boolean thumbHovered = mx >= tx && mx < tx + THUMB_W && my >= y && my < y + height;
        int thumbColor = (active && (dragging || thumbHovered)) ? COLOR_THUMB_HOVER : COLOR_THUMB;
        g.fill(tx, y, tx + THUMB_W, y + height, thumbColor);

        // Disabled overlay
        if (!active) g.fill(x, y, x + width, y + height, 0x55000000);
    }

    @Override
    public boolean hasClickHandler() { return true; }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0 || !active || !visible || min >= max) return false;
        if (mx >= x && mx < x + width && my >= y && my < y + height) {
            dragging = true;
            pressed = true;
            captureDrag();
            updateValueFromMouse(mx);
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mx, double my, int btn) {
        if (btn == 0 && visible && (dragging || ownsActiveDrag())) {
            dragging = false;
            pressed = false;
            releaseDrag();
        }
    }

    @Override
    public void mouseDragged(double mx, double my, int btn) {
        if (btn == 0 && active && visible && min < max && (dragging || ownsActiveDrag())) {
            dragging = true;
            pressed = true;
            updateValueFromMouse(mx);
        }
    }
}
