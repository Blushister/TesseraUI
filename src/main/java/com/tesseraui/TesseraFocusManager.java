package com.tesseraui;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages keyboard focus navigation (Tab / Shift+Tab) between focusable widgets.
 *
 * <p>Widgets that should participate in focus traversal call {@link #register(TesseraWidget)}
 * during screen initialisation. {@link TesseraScreen} intercepts {@code GLFW_KEY_TAB} and
 * delegates to {@link #focusNext()} / {@link #focusPrev()} accordingly.
 *
 * <p>Call {@link #clear()} at the start of each {@link TesseraScreen#init()} call to flush
 * stale widget references from the previous build.
 */
public final class TesseraFocusManager {

    private static final List<TesseraWidget> focusables = new ArrayList<>();
    private static int focusIndex = -1;

    private TesseraFocusManager() {}

    /** Registers a widget for focus traversal. */
    public static void register(TesseraWidget w) {
        if (w != null && !focusables.contains(w)) {
            focusables.add(w);
        }
    }

    /** Clears all registered widgets and resets focus. Call this from {@code init()}. */
    public static void clear() {
        focusables.clear();
        focusIndex = -1;
    }

    /**
     * Moves focus to the next focusable widget (Tab key).
     * Wraps around after the last widget.
     */
    public static void focusNext() {
        if (focusables.isEmpty()) return;
        defocusCurrent();
        focusIndex = (focusIndex + 1) % focusables.size();
        focusCurrent();
    }

    /**
     * Moves focus to the previous focusable widget (Shift+Tab).
     * Wraps around before the first widget.
     */
    public static void focusPrev() {
        if (focusables.isEmpty()) return;
        defocusCurrent();
        focusIndex = (focusIndex - 1 + focusables.size()) % focusables.size();
        focusCurrent();
    }

    /** Returns the currently focused widget, or {@code null} if none. */
    public static TesseraWidget focused() {
        if (focusIndex < 0 || focusIndex >= focusables.size()) return null;
        return focusables.get(focusIndex);
    }

    private static void defocusCurrent() {
        if (focusIndex >= 0 && focusIndex < focusables.size()) {
            focusables.get(focusIndex).setFocused(false);
        }
    }

    private static void focusCurrent() {
        if (focusIndex >= 0 && focusIndex < focusables.size()) {
            focusables.get(focusIndex).setFocused(true);
        }
    }
}
