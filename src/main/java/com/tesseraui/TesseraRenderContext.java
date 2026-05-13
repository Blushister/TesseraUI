package com.tesseraui;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

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

    /** Clears the persisted state for one input id. */
    public boolean clearInput(String id) {
        return inputStates.remove(id) != null;
    }

    /** Clears every input state whose id starts with the given prefix. */
    public int clearInputsWithPrefix(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        return clearInputsMatching(id -> id.startsWith(prefix));
    }

    /** Clears every input state whose id matches the predicate. */
    public int clearInputsMatching(Predicate<String> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = inputStates.size();
        inputStates.keySet().removeIf(predicate);
        return before - inputStates.size();
    }

    /** Clears all transient state. Useful when closing/reusing a screen. */
    public void clear() {
        inputStates.clear();
    }
}
