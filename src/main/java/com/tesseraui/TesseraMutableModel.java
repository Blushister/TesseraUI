package com.tesseraui;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link TesseraModel} that also supports writing values — enabling two-way data binding.
 *
 * <p>Create an instance with {@link #of()} or {@link #of(Map)}, share it between the template
 * renderer and your widget handlers, then call {@link #set} from handler lambdas to update the
 * model without rebuilding the entire panel hierarchy.</p>
 *
 * <p>Example — input field with live display:</p>
 * <pre>{@code
 * TesseraMutableModel model = TesseraMutableModel.of(Map.of("name", "player"));
 *
 * Map<String, Consumer<String>> handlers = Map.of(
 *     "onName", v -> { model.set("name", v); /* rebuild or reactive update *\/ }
 * );
 *
 * TesseraPanel ui = TesseraTemplateRenderer.build(template, model, Map.of(), handlers, x, y, w, h);
 * }</pre>
 */
public interface TesseraMutableModel extends TesseraModel {

    /**
     * Sets the value associated with {@code key}.
     *
     * @param key   the model key (same syntax used in {@code {{ key }}} expressions)
     * @param value the new value; {@code null} removes the key (subsequent resolves return {@code null})
     */
    void set(String key, String value);

    // ── Factory methods ────────────────────────────────────────────────────────

    /**
     * Creates an empty mutable model backed by a {@link HashMap}.
     */
    static TesseraMutableModel of() {
        return of(Map.of());
    }

    /**
     * Creates a mutable model pre-populated with the given key-value pairs.
     *
     * @param initial initial entries (copied — mutations do not affect the source map)
     */
    static TesseraMutableModel of(Map<String, String> initial) {
        Map<String, String> map = new HashMap<>(initial);
        return new TesseraMutableModel() {
            @Override
            public String resolve(String key) { return map.get(key); }

            @Override
            public void set(String key, String value) {
                if (value == null) map.remove(key);
                else map.put(key, value);
            }
        };
    }
}
