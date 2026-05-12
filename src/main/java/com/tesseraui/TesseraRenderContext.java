package com.tesseraui;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-screen render state kept across template rebuilds.
 *
 * <p>Pass the same context to {@link TesseraTemplateRenderer#build} each time a
 * screen rebuilds its widget tree so transient widget state survives rebuilds.
 * The first supported state is {@link TesseraInputState}, keyed by the
 * {@code id} attribute of {@code <input>} and {@code <textarea>} elements.</p>
 */
public final class TesseraRenderContext {

    private final Map<String, TesseraInputState> inputStates = new HashMap<>();

    /** Returns the mutable input-state map used by the renderer. */
    public Map<String, TesseraInputState> inputStates() {
        return inputStates;
    }

    /** Returns the state for an input id, creating it when absent. */
    public TesseraInputState inputState(String id) {
        return inputStates.computeIfAbsent(id, ignored -> new TesseraInputState());
    }

    /** Clears all transient state. Useful when closing/reusing a screen. */
    public void clear() {
        inputStates.clear();
    }
}
