package com.tesseraui;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TesseraNode {

    /** Virtual tag used for text nodes emitted by the parser for tail text content. */
    public static final String TEXT_TAG = "#text";

    /** Creates a virtual text node (no element tag, no attributes, no children). */
    public static TesseraNode textNode(String text) {
        return new TesseraNode(TEXT_TAG, Map.of(), List.of(), text);
    }

    /** Returns {@code true} when this node represents a raw text segment, not an HTML element. */
    public boolean isTextNode() { return TEXT_TAG.equals(tag); }

    public static final Set<String> KNOWN_TAGS = Set.of(
        // Core TesseraUI tags
        "div", "label", "button", "icon", "badge", "grid", "row", "col",
        "hr", "span", "input", "textarea", "checkbox", "slider", "select", "option",
        // Semantic block tags
        "p", "h1", "h2", "h3", "h4", "h5", "h6",
        "ul", "ol", "li",
        "section", "article", "main", "nav", "header", "footer",
        // Inline semantic tags
        "strong", "b", "em", "i",
        "a",
        // Table tags
        "table", "thead", "tbody", "tfoot", "tr", "th", "td",
        // Media
        "img"
    );

    private final String tag;
    private final Map<String, String> attrs;
    private final List<TesseraNode> children;
    private final String text;
    private List<String> cachedClassNames;

    public TesseraNode(String tag, Map<String, String> attrs, List<TesseraNode> children, String text) {
        this.tag = tag;
        this.attrs = Map.copyOf(attrs);
        this.children = List.copyOf(children);
        this.text = text != null ? text : "";
    }

    public String tag()                { return tag; }
    public Map<String, String> attrs() { return attrs; }
    public List<TesseraNode> children()   { return children; }
    public String text()               { return text; }

    public String attr(String key) { return attrs.getOrDefault(key, ""); }
    public boolean hasAttr(String key) { return attrs.containsKey(key); }

    public List<String> classNames() {
        if (cachedClassNames == null) {
            String cls = attrs.getOrDefault("class", "");
            cachedClassNames = cls.isBlank() ? List.of()
                : List.of(cls.trim().split("\\s+"));
        }
        return cachedClassNames;
    }

    public boolean hasClass(String name) { return classNames().contains(name); }

    public String onClickHandler() { return attr("onclick"); }

    public String vFor()  { return attr("v-for"); }
    public String vIf()   { return attr("v-if"); }
    public String vShow() { return attr("v-show"); }

    public boolean isKnown() { return KNOWN_TAGS.contains(tag); }

    @Override
    public String toString() {
        return "<" + tag + (attrs.isEmpty() ? "" : " " + attrs) + "> children=" + children.size();
    }
}
