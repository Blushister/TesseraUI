package com.tesseraui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link TesseraMutableModel} that notifies registered listeners whenever a value changes.
 *
 * <p>Use {@link TesseraPanel#watchModel} to connect a reactive model to a panel so the UI
 * rebuilds automatically when the model is mutated.</p>
 *
 * <pre>{@code
 * TesseraReactiveModel model = TesseraReactiveModel.of(Map.of("count", "0"));
 *
 * panel.watchModel(model, () -> TesseraTemplateRenderer.build(template, model, handlers,
 *         x, y, w, h));
 *
 * // Later, from a button handler:
 * model.set("count", String.valueOf(Integer.parseInt(model.resolve("count")) + 1));
 * }</pre>
 */
public interface TesseraReactiveModel extends TesseraMutableModel {

    /**
     * Registers a listener that will be called after every {@link #set} invocation.
     *
     * @param listener callback invoked on the thread that calls {@code set}
     */
    void addChangeListener(Runnable listener);

    /**
     * Removes a previously registered listener.  No-op if the listener was not registered.
     *
     * @param listener the listener to remove
     */
    void removeChangeListener(Runnable listener);

    // ── Factory methods ────────────────────────────────────────────────────────

    /** Creates an empty reactive model. */
    static TesseraReactiveModel of() {
        return of(Map.of());
    }

    /**
     * Creates a reactive model pre-populated with the given key-value pairs.
     *
     * @param initial initial entries (copied — mutations do not affect the source map)
     */
    static TesseraReactiveModel of(Map<String, String> initial) {
        Map<String, String> map = new HashMap<>(initial);
        List<Runnable> listeners = new ArrayList<>();

        return new TesseraReactiveModel() {

            @Override
            public String resolve(String key) {
                return map.get(key);
            }

            @Override
            public void set(String key, String value) {
                if (value == null) map.remove(key);
                else map.put(key, value);
                // Notify all listeners after the update
                for (Runnable listener : new ArrayList<>(listeners)) {
                    listener.run();
                }
            }

            @Override
            public void addChangeListener(Runnable listener) {
                if (listener != null && !listeners.contains(listener)) {
                    listeners.add(listener);
                }
            }

            @Override
            public void removeChangeListener(Runnable listener) {
                listeners.remove(listener);
            }
        };
    }
}
