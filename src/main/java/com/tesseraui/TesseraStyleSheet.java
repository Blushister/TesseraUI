package com.tesseraui;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TesseraStyleSheet {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final TesseraStyleSheet EMPTY = new TesseraStyleSheet(List.of(), List.of(), List.of(), List.of(), List.of());

    public record Rule(TesseraSelector selector, TesseraStyle style, int order) {}

    /** Règles conditionnelles d'un bloc {@code @media}. */
    public record MediaBlock(int minWidth, int maxWidth,
                             List<Rule> base, List<Rule> hover,
                             List<Rule> active, List<Rule> disabled,
                             List<Rule> focus) {
        /** Retourne {@code true} si {@code viewportWidth} satisfait la contrainte. */
        public boolean matches(int viewportWidth) {
            if (minWidth >= 0 && viewportWidth < minWidth) return false;
            if (maxWidth >= 0 && viewportWidth > maxWidth) return false;
            return true;
        }
    }

    private final List<Rule> base;
    private final List<Rule> hover;
    private final List<Rule> active;
    private final List<Rule> disabled;
    private final List<Rule> focus;
    private final List<MediaBlock> mediaBlocks;
    private Map<String, TesseraKeyframes> keyframeRegistry = new HashMap<>();

    TesseraStyleSheet(List<Rule> base, List<Rule> hover, List<Rule> active, List<Rule> disabled) {
        this(base, hover, active, disabled, List.of());
    }

    TesseraStyleSheet(List<Rule> base, List<Rule> hover, List<Rule> active, List<Rule> disabled,
                      List<Rule> focus) {
        this(base, hover, active, disabled, focus, List.of());
    }

    TesseraStyleSheet(List<Rule> base, List<Rule> hover, List<Rule> active, List<Rule> disabled,
                      List<Rule> focus, List<MediaBlock> mediaBlocks) {
        this.base        = List.copyOf(base);
        this.hover       = List.copyOf(hover);
        this.active      = List.copyOf(active);
        this.disabled    = List.copyOf(disabled);
        this.focus       = List.copyOf(focus);
        this.mediaBlocks = List.copyOf(mediaBlocks);
    }

    TesseraStyleSheet(Map<String, TesseraStyle> base, Map<String, TesseraStyle> hover,
                   Map<String, TesseraStyle> active, Map<String, TesseraStyle> disabled) {
        this(toRules(base), toRules(hover), toRules(active), toRules(disabled), List.of());
    }

    private static List<Rule> toRules(Map<String, TesseraStyle> map) {
        List<Rule> out = new ArrayList<>();
        int i = 0;
        for (var e : map.entrySet()) {
            TesseraSelector sel = TesseraSelector.parse("." + e.getKey());
            if (sel != null) out.add(new Rule(sel, e.getValue(), i++));
        }
        return out;
    }

    public TesseraStyle resolve(Collection<String> classNames) {
        return resolveByClasses(base, classNames);
    }

    public TesseraStyle resolveHover(Collection<String> classNames) {
        return resolve(classNames).merge(resolveByClasses(hover, classNames));
    }

    public TesseraStyle resolveActive(Collection<String> classNames) {
        return resolve(classNames).merge(resolveByClasses(active, classNames));
    }

    public TesseraStyle resolveDisabled(Collection<String> classNames) {
        return resolve(classNames).merge(resolveByClasses(disabled, classNames));
    }

    private static TesseraStyle resolveByClasses(List<Rule> rules, Collection<String> classNames) {
        List<Rule> matched = new ArrayList<>();
        for (Rule r : rules) {
            if (r.selector().segments.isEmpty()) continue;
            TesseraSelector.Segment last = r.selector().segments.get(r.selector().segments.size() - 1);
            if (r.selector().segments.size() != 1) continue;
            if (last.tag != null) continue;
            boolean all = !last.classes.isEmpty();
            for (String c : last.classes) if (!classNames.contains(c)) { all = false; break; }
            if (all) matched.add(r);
        }
        return foldStyles(matched);
    }

    public TesseraStyle resolve(TesseraNode node, Deque<TesseraNode> ancestors) {
        return matchAndFold(base, node, ancestors);
    }

    public TesseraStyle resolveHover(TesseraNode node, Deque<TesseraNode> ancestors) {
        return resolve(node, ancestors).merge(matchAndFold(hover, node, ancestors));
    }

    public TesseraStyle resolveActive(TesseraNode node, Deque<TesseraNode> ancestors) {
        return resolve(node, ancestors).merge(matchAndFold(active, node, ancestors));
    }

    public TesseraStyle resolveDisabled(TesseraNode node, Deque<TesseraNode> ancestors) {
        return resolve(node, ancestors).merge(matchAndFold(disabled, node, ancestors));
    }

    /**
     * Returns the merged style for a node that currently holds keyboard focus.
     * The focus rules (parsed from {@code selector:focus { ... }}) are layered on top of
     * the base style, exactly like {@code resolveHover} does for {@code :hover}.
     */
    public TesseraStyle resolveFocus(TesseraNode node, Deque<TesseraNode> ancestors) {
        return resolve(node, ancestors).merge(matchAndFold(focus, node, ancestors));
    }

    public List<MediaBlock> mediaBlocks() { return mediaBlocks; }

    public boolean isEmpty() {
        return base.isEmpty() && hover.isEmpty() && active.isEmpty()
            && disabled.isEmpty() && focus.isEmpty() && mediaBlocks.isEmpty()
            && keyframeRegistry.isEmpty();
    }

    /**
     * Returns a stylesheet where {@code other} is appended after this sheet in
     * cascade order. Rules from {@code other} win over equally-specific rules in
     * this sheet, matching how later CSS files override earlier files.
     */
    public TesseraStyleSheet merge(TesseraStyleSheet other) {
        if (other == null || other.isEmpty()) return this;
        if (isEmpty()) return other;

        int nextOrder = maxRuleOrder() + 1;
        List<Rule> newBase     = appendWithOrderOffset(base,     other.base,     nextOrder);
        nextOrder += other.base.size();
        List<Rule> newHover    = appendWithOrderOffset(hover,    other.hover,    nextOrder);
        nextOrder += other.hover.size();
        List<Rule> newActive   = appendWithOrderOffset(active,   other.active,   nextOrder);
        nextOrder += other.active.size();
        List<Rule> newDisabled = appendWithOrderOffset(disabled, other.disabled, nextOrder);
        nextOrder += other.disabled.size();
        List<Rule> newFocus    = appendWithOrderOffset(focus,    other.focus,    nextOrder);

        List<MediaBlock> newMedia = new ArrayList<>(mediaBlocks);
        newMedia.addAll(other.mediaBlocks);

        TesseraStyleSheet result = new TesseraStyleSheet(newBase, newHover, newActive, newDisabled, newFocus, newMedia);
        result.keyframeRegistry = new HashMap<>(keyframeRegistry);
        result.keyframeRegistry.putAll(other.keyframeRegistry);
        return result;
    }

    private int maxRuleOrder() {
        int max = 0;
        for (List<Rule> list : List.of(base, hover, active, disabled, focus)) {
            for (Rule r : list) if (r.order() > max) max = r.order();
        }
        return max;
    }

    private static List<Rule> appendWithOrderOffset(List<Rule> first, List<Rule> second, int nextOrder) {
        List<Rule> out = new ArrayList<>(first.size() + second.size());
        out.addAll(first);
        int order = nextOrder;
        for (Rule r : second) out.add(new Rule(r.selector(), r.style(), order++));
        return out;
    }

    /**
     * Returns a new {@code TesseraStyleSheet} with media-block rules that match
     * {@code viewportWidth} merged into the appropriate state lists.
     * Rules from matching media blocks are appended after base rules
     * (they override by specificity/order, like in real CSS).
     *
     * @param viewportWidth the GUI-scaled screen width in pixels
     * @return a new stylesheet with matching media rules activated; returns
     *         {@code this} unchanged if there are no media blocks
     */
    public TesseraStyleSheet forViewport(int viewportWidth) {
        if (mediaBlocks.isEmpty()) return this;
        List<Rule> newBase     = new ArrayList<>(base);
        List<Rule> newHover    = new ArrayList<>(hover);
        List<Rule> newActive   = new ArrayList<>(active);
        List<Rule> newDisabled = new ArrayList<>(disabled);
        List<Rule> newFocus    = new ArrayList<>(focus);

        // Media rules must cascade AFTER flat rules when specificity is equal.
        // During CSS parsing, @media blocks are extracted first and assigned low order
        // numbers, while flat rules get higher numbers — opposite of document order.
        // Fix: re-number media rules above the current maximum so they always win.
        int maxOrder = 0;
        for (List<Rule> list : List.of(newBase, newHover, newActive, newDisabled, newFocus))
            for (Rule r : list) if (r.order() > maxOrder) maxOrder = r.order();
        int nextOrder = maxOrder + 1;

        for (MediaBlock mb : mediaBlocks) {
            if (mb.matches(viewportWidth)) {
                for (Rule r : mb.base())     newBase    .add(new Rule(r.selector(), r.style(), nextOrder++));
                for (Rule r : mb.hover())    newHover   .add(new Rule(r.selector(), r.style(), nextOrder++));
                for (Rule r : mb.active())   newActive  .add(new Rule(r.selector(), r.style(), nextOrder++));
                for (Rule r : mb.disabled()) newDisabled.add(new Rule(r.selector(), r.style(), nextOrder++));
                for (Rule r : mb.focus())    newFocus   .add(new Rule(r.selector(), r.style(), nextOrder++));
            }
        }
        // Note: forViewport result has no mediaBlocks (already resolved) — intentional.
        TesseraStyleSheet result = new TesseraStyleSheet(newBase, newHover, newActive, newDisabled, newFocus);
        result.keyframeRegistry = this.keyframeRegistry;
        return result;
    }

    private static TesseraStyle matchAndFold(List<Rule> rules, TesseraNode node, Deque<TesseraNode> ancestors) {
        Deque<TesseraNode> anc = ancestors != null ? ancestors : new ArrayDeque<>();
        List<Rule> matched = new ArrayList<>();
        for (Rule r : rules) {
            if (r.selector().matches(node, anc)) matched.add(r);
        }
        return foldStyles(matched);
    }

    private static TesseraStyle foldStyles(List<Rule> matched) {
        matched.sort(Comparator
                .<Rule>comparingInt(r -> r.selector().specificity())
                .thenComparingInt(Rule::order));
        TesseraStyle result = new TesseraStyle();
        for (Rule r : matched) result = result.merge(r.style());
        return result;
    }

    /** Registers a parsed {@code @keyframes} block. */
    public void registerKeyframes(TesseraKeyframes keyframes) {
        keyframeRegistry.put(keyframes.name(), keyframes);
    }

    /** Returns the keyframes with the given name, or {@code null} if not found. */
    public TesseraKeyframes getKeyframes(String name) {
        return keyframeRegistry.get(name);
    }
}
