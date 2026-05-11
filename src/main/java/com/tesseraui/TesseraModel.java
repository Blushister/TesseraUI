package com.tesseraui;

import java.util.Map;

/**
 * Data model that resolves named keys to string values for template binding.
 *
 * <p>{@code TesseraModel} is a {@link FunctionalInterface} and can be provided as a lambda,
 * a method reference, or created from a {@link Map} via {@link #of(Map)}.
 *
 * <p>Example usage:
 * <pre>{@code
 * TesseraModel model = TesseraModel.of(Map.of(
 *     "dungeon.name", "The Dark Citadel",
 *     "dungeon.enabled", "true"
 * ));
 * String result = TesseraBindingResolver.resolve("{{ dungeon.name }}", model);
 * // → "The Dark Citadel"
 * }</pre>
 *
 * @see TesseraBindingResolver
 * @see TesseraForEach
 */
@FunctionalInterface
public interface TesseraModel {

    /**
     * An empty model that returns {@code null} for every key.
     * Unresolved bindings in templates are kept verbatim (e.g. {@code {{ key }}} stays as-is).
     */
    TesseraModel EMPTY = key -> null;

    /**
     * Resolves a key to its string value.
     *
     * @param key the binding key (e.g. {@code "dungeon.name"})
     * @return the resolved string, or {@code null} if the key is not present
     */
    String resolve(String key);

    /**
     * Creates a model backed by a {@link Map}.
     *
     * @param data key-value pairs; absent keys resolve to {@code null}
     */
    static TesseraModel of(Map<String, String> data) {
        return key -> data.getOrDefault(key, null);
    }
}
