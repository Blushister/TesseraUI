package com.tesseraui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TesseraForEach {

    private TesseraForEach() {}

    public static List<TesseraNode> expand(TesseraNode template, List<TesseraModel> items, String varName) {
        List<TesseraNode> result = new ArrayList<>(items.size());
        for (TesseraModel item : items) {
            TesseraModel scoped = key -> {
                if (key.startsWith(varName + ".")) {
                    return item.resolve(key.substring(varName.length() + 1));
                }
                return item.resolve(key);
            };
            result.add(cloneResolved(template, scoped));
        }
        return result;
    }

    public static TesseraNode resolveAttrs(TesseraNode node, TesseraModel model) {
        return cloneResolved(node, model);
    }

    private static TesseraNode cloneResolved(TesseraNode node, TesseraModel model) {
        Map<String, String> resolvedAttrs = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : node.attrs().entrySet()) {
            resolvedAttrs.put(e.getKey(), TesseraBindingResolver.resolve(e.getValue(), model));
        }
        List<TesseraNode> resolvedChildren = new ArrayList<>();
        for (TesseraNode child : node.children()) {
            resolvedChildren.add(cloneResolved(child, model));
        }
        String resolvedText = TesseraBindingResolver.resolve(node.text(), model);
        return new TesseraNode(node.tag(), resolvedAttrs, resolvedChildren, resolvedText);
    }
}
