package com.tesseraui;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of reusable HTML component templates declared with
 * {@code <template name="my-component">…</template>} inside an HTML file.
 *
 * <p>Components can also be registered programmatically:</p>
 * <pre>{@code
 * TesseraComponentRegistry.register("my-card",
 *     "<col class='card'><slot/></col>");
 * }</pre>
 *
 * <p>Once registered, the component name becomes a valid HTML tag that
 * {@link TesseraTemplateRenderer} expands by cloning the template tree and
 * injecting the caller's child content into {@code <slot>} nodes.</p>
 */
public final class TesseraComponentRegistry {

    private static final Map<String, TesseraNode> components = new HashMap<>();

    private TesseraComponentRegistry() {}

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a named component from a raw HTML fragment.
     *
     * @param name the tag name to register (e.g. {@code "my-card"})
     * @param html the template HTML (must have a single root element)
     */
    public static void register(String name, String html) {
        TesseraNode root = TesseraHtmlParser.parseFragment(html);
        if (root != null) {
            components.put(name, root);
        }
    }

    /** Registers a pre-parsed {@link TesseraNode} as a named component. */
    public static void register(String name, TesseraNode templateRoot) {
        if (name != null && !name.isBlank() && templateRoot != null) {
            components.put(name, templateRoot);
        }
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    /** Returns {@code true} if a component with the given name is registered. */
    public static boolean has(String name) {
        return components.containsKey(name);
    }

    /**
     * Returns the template root node for the given component name, or
     * {@code null} if the name is not registered.
     */
    public static TesseraNode get(String name) {
        return components.get(name);
    }

    // ── Slot injection ────────────────────────────────────────────────────────

    /**
     * Clones {@code templateRoot} and injects {@code callerChildren} into the
     * {@code <slot>} nodes within the clone.
     *
     * <p>Slot resolution rules:</p>
     * <ul>
     *   <li>Named slots: {@code <slot name="title"/>} receives caller children
     *       that have {@code slot="title"}.</li>
     *   <li>Default slot: {@code <slot/>} (no name attribute) receives all
     *       caller children that do NOT have a {@code slot} attribute.</li>
     *   <li>If the template has no slot, caller children are ignored.</li>
     * </ul>
     *
     * @param templateRoot   the template node tree (not mutated)
     * @param callerChildren the children of the use-site element
     * @return a fresh node tree with slots filled
     */
    public static TesseraNode instantiate(TesseraNode templateRoot,
                                          java.util.List<TesseraNode> callerChildren) {
        return injectSlots(templateRoot, callerChildren);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static TesseraNode injectSlots(TesseraNode node,
                                            java.util.List<TesseraNode> callerChildren) {
        // If this node is a <slot>, replace it with the appropriate caller content
        if ("slot".equals(node.tag())) {
            String slotName = node.attr("name"); // "" = default slot
            java.util.List<TesseraNode> matching = callerChildren.stream()
                .filter(child -> {
                    String childSlot = child.attr("slot");
                    if (slotName.isEmpty()) {
                        // default slot: children without a slot attribute
                        return childSlot.isEmpty();
                    } else {
                        return slotName.equals(childSlot);
                    }
                })
                .toList();

            if (matching.isEmpty()) {
                // Return fallback content (slot children) if present, else nothing
                // Return a wrapper with slot children as children
                if (node.children().isEmpty()) return null;
                // Wrap fallback content in a transparent div-like container by returning first child
                // Actually we can't return multiple nodes from here; return a synthetic wrapper.
                // Use a col wrapper with no styling for multi-child fallback.
                return new TesseraNode("col",
                    Map.of(),
                    node.children(),
                    "");
            }

            if (matching.size() == 1) {
                // Single match — return directly
                return matching.get(0);
            }

            // Multiple matches — wrap in a col
            return new TesseraNode("col", Map.of(), matching, "");
        }

        // Otherwise recursively process children, replacing slots
        java.util.List<TesseraNode> newChildren = new java.util.ArrayList<>();
        for (TesseraNode child : node.children()) {
            TesseraNode injected = injectSlots(child, callerChildren);
            if (injected != null) newChildren.add(injected);
        }

        return new TesseraNode(node.tag(), node.attrs(), newChildren, node.text());
    }
}
