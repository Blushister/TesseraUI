package com.tesseraui;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TesseraSelector}: parsing and matching logic.
 * Pure-Java — no Minecraft runtime required.
 */
class TesseraSelectorTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private static TesseraNode node(String tag, List<String> classes) {
        String classAttr = String.join(" ", classes);
        Map<String, String> attrs = classes.isEmpty() ? Map.of() : Map.of("class", classAttr);
        return new TesseraNode(tag, attrs, List.of(), "");
    }

    private static TesseraNode nodeNth(String tag, int nthIndex) {
        return new TesseraNode(tag, Map.of("__nth-index", String.valueOf(nthIndex)), List.of(), "");
    }

    private static Deque<TesseraNode> ancestors(TesseraNode... nodes) {
        Deque<TesseraNode> d = new ArrayDeque<>();
        for (TesseraNode n : nodes) d.push(n);
        return d;
    }

    // ── parse: basic shapes ──────────────────────────────────────────────────

    @Test
    void parse_tagSelector() {
        TesseraSelector sel = TesseraSelector.parse("div");
        assertNotNull(sel);
        assertEquals(1, sel.segments.size());
        assertEquals("div", sel.segments.get(0).tag);
        assertTrue(sel.segments.get(0).classes.isEmpty());
    }

    @Test
    void parse_classSelector() {
        TesseraSelector sel = TesseraSelector.parse(".card");
        assertNotNull(sel);
        assertEquals(1, sel.segments.size());
        assertNull(sel.segments.get(0).tag);
        assertTrue(sel.segments.get(0).classes.contains("card"));
    }

    @Test
    void parse_tagAndClass() {
        TesseraSelector sel = TesseraSelector.parse("div.card");
        assertNotNull(sel);
        assertEquals("div", sel.segments.get(0).tag);
        assertTrue(sel.segments.get(0).classes.contains("card"));
    }

    @Test
    void parse_multipleClasses() {
        TesseraSelector sel = TesseraSelector.parse(".a.b.c");
        assertNotNull(sel);
        List<String> cls = sel.segments.get(0).classes;
        assertTrue(cls.contains("a"));
        assertTrue(cls.contains("b"));
        assertTrue(cls.contains("c"));
    }

    @Test
    void parse_descendantCombinator() {
        TesseraSelector sel = TesseraSelector.parse(".parent .child");
        assertNotNull(sel);
        assertEquals(2, sel.segments.size());
        assertEquals(1, sel.combinators.size());
        assertEquals(TesseraSelector.Combinator.DESCENDANT, sel.combinators.get(0));
    }

    @Test
    void parse_childCombinator() {
        TesseraSelector sel = TesseraSelector.parse(".parent > .child");
        assertNotNull(sel);
        assertEquals(TesseraSelector.Combinator.CHILD, sel.combinators.get(0));
    }

    @Test
    void parse_nthChild() {
        TesseraSelector sel = TesseraSelector.parse("tr:nth-child(odd)");
        assertNotNull(sel);
        assertEquals("tr", sel.segments.get(0).tag);
        assertEquals("odd", sel.segments.get(0).nthChild);
    }

    @Test
    void parse_nthChildEven() {
        TesseraSelector sel = TesseraSelector.parse("tr:nth-child(even)");
        assertNotNull(sel);
        assertEquals("even", sel.segments.get(0).nthChild);
    }

    @Test
    void parse_nthChildNumeric() {
        TesseraSelector sel = TesseraSelector.parse("li:nth-child(3)");
        assertNotNull(sel);
        assertEquals("3", sel.segments.get(0).nthChild);
    }

    @Test
    void parse_emptyOrNullReturnsNull() {
        assertNull(TesseraSelector.parse(""));
        assertNull(TesseraSelector.parse("   "));
    }

    // ── matches: tag selector ────────────────────────────────────────────────

    @Test
    void matches_tagSelector_hit() {
        TesseraSelector sel = TesseraSelector.parse("div");
        assertTrue(sel.matches(node("div", List.of()), new ArrayDeque<>()));
    }

    @Test
    void matches_tagSelector_miss() {
        TesseraSelector sel = TesseraSelector.parse("div");
        assertFalse(sel.matches(node("span", List.of()), new ArrayDeque<>()));
    }

    @Test
    void matches_classSelector_hit() {
        TesseraSelector sel = TesseraSelector.parse(".active");
        assertTrue(sel.matches(node("button", List.of("active")), new ArrayDeque<>()));
    }

    @Test
    void matches_classSelector_miss() {
        TesseraSelector sel = TesseraSelector.parse(".active");
        assertFalse(sel.matches(node("button", List.of("inactive")), new ArrayDeque<>()));
    }

    @Test
    void matches_tagAndClass_hit() {
        TesseraSelector sel = TesseraSelector.parse("div.card");
        assertTrue(sel.matches(node("div", List.of("card")), new ArrayDeque<>()));
    }

    @Test
    void matches_tagAndClass_wrongTag() {
        TesseraSelector sel = TesseraSelector.parse("div.card");
        assertFalse(sel.matches(node("span", List.of("card")), new ArrayDeque<>()));
    }

    // ── matches: combinators ─────────────────────────────────────────────────

    @Test
    void matches_descendant_hit() {
        TesseraSelector sel = TesseraSelector.parse(".panel .label");
        TesseraNode label = node("span", List.of("label"));
        TesseraNode panel = node("div", List.of("panel"));
        Deque<TesseraNode> ancs = ancestors(panel);
        assertTrue(sel.matches(label, ancs));
    }

    @Test
    void matches_descendant_miss_noAncestor() {
        TesseraSelector sel = TesseraSelector.parse(".panel .label");
        TesseraNode label = node("span", List.of("label"));
        assertFalse(sel.matches(label, new ArrayDeque<>()));
    }

    @Test
    void matches_child_hit() {
        TesseraSelector sel = TesseraSelector.parse(".panel > .label");
        TesseraNode label = node("span", List.of("label"));
        TesseraNode panel = node("div", List.of("panel"));
        Deque<TesseraNode> ancs = ancestors(panel);
        assertTrue(sel.matches(label, ancs));
    }

    @Test
    void matches_child_miss_notDirectParent() {
        // label's direct parent is "middle", grandparent is "panel".
        // Child combinator (.panel > .label) requires panel to be the DIRECT parent — must not match.
        // ancestors() uses push(), so the LAST arg ends up at the head (= direct parent during iteration).
        TesseraSelector sel = TesseraSelector.parse(".panel > .label");
        TesseraNode label = node("span", List.of("label"));
        TesseraNode middle = node("div", List.of("middle"));
        TesseraNode panel  = node("div", List.of("panel"));
        // push panel first, then middle → middle is at head = direct parent
        Deque<TesseraNode> ancs = ancestors(panel, middle);
        assertFalse(sel.matches(label, ancs));
    }

    // ── matches: nth-child ───────────────────────────────────────────────────

    @Test
    void matches_nthOdd_hit() {
        TesseraSelector sel = TesseraSelector.parse("tr:nth-child(odd)");
        TesseraNode tr = nodeNth("tr", 1);
        assertTrue(sel.matches(tr, new ArrayDeque<>()));
        TesseraNode tr3 = nodeNth("tr", 3);
        assertTrue(sel.matches(tr3, new ArrayDeque<>()));
    }

    @Test
    void matches_nthEven_hit() {
        TesseraSelector sel = TesseraSelector.parse("tr:nth-child(even)");
        TesseraNode tr = nodeNth("tr", 2);
        assertTrue(sel.matches(tr, new ArrayDeque<>()));
    }

    @Test
    void matches_nthEven_miss() {
        TesseraSelector sel = TesseraSelector.parse("tr:nth-child(even)");
        TesseraNode tr = nodeNth("tr", 1);
        assertFalse(sel.matches(tr, new ArrayDeque<>()));
    }

    @Test
    void matches_nthNumeric_hit() {
        TesseraSelector sel = TesseraSelector.parse("li:nth-child(2)");
        TesseraNode li = nodeNth("li", 2);
        assertTrue(sel.matches(li, new ArrayDeque<>()));
    }

    @Test
    void matches_nthNumeric_miss() {
        TesseraSelector sel = TesseraSelector.parse("li:nth-child(2)");
        TesseraNode li = nodeNth("li", 3);
        assertFalse(sel.matches(li, new ArrayDeque<>()));
    }

    // ── specificity ──────────────────────────────────────────────────────────

    @Test
    void specificity_tagOnly() {
        TesseraSelector sel = TesseraSelector.parse("div");
        assertEquals(1, sel.specificity());
    }

    @Test
    void specificity_classOnly() {
        TesseraSelector sel = TesseraSelector.parse(".card");
        assertEquals(10, sel.specificity());
    }

    @Test
    void specificity_tagAndClass() {
        TesseraSelector sel = TesseraSelector.parse("div.card");
        assertEquals(11, sel.specificity());
    }

    @Test
    void specificity_twoClasses() {
        TesseraSelector sel = TesseraSelector.parse(".a.b");
        assertEquals(20, sel.specificity());
    }

    @Test
    void specificity_descendantCombinator() {
        // Two segments: ".panel" (10) + ".label" (10) = 20
        TesseraSelector sel = TesseraSelector.parse(".panel .label");
        assertEquals(20, sel.specificity());
    }

    // ── matches: descendant ordering (v1.8 fix — backtracking) ───────────────

    @Test
    void matches_descendant_threeLevel_correctOrder() {
        // .a .b .c : node has .c, direct parent has .b, grandparent has .a → must match
        TesseraSelector sel = TesseraSelector.parse(".a .b .c");
        TesseraNode nodeC = node("div", List.of("c"));
        TesseraNode nodeB = node("div", List.of("b"));
        TesseraNode nodeA = node("div", List.of("a"));
        // ancestors() uses push() so last pushed = head = direct parent
        Deque<TesseraNode> ancs = ancestors(nodeA, nodeB); // nodeB is direct parent, nodeA is grandparent
        assertTrue(sel.matches(nodeC, ancs), ".a .b .c should match when a→b→c in hierarchy");
    }

    @Test
    void matches_descendant_threeLevel_wrongOrder_miss() {
        // .a .b .c : if ancestors are only [.a] (no .b between .a and .c) → must NOT match
        TesseraSelector sel = TesseraSelector.parse(".a .b .c");
        TesseraNode nodeC = node("div", List.of("c"));
        TesseraNode nodeA = node("div", List.of("a"));
        Deque<TesseraNode> ancs = ancestors(nodeA); // missing .b in hierarchy
        assertFalse(sel.matches(nodeC, ancs), ".a .b .c must not match when .b is absent");
    }

    @Test
    void matches_descendant_ancestorOrderMatters() {
        // .a .b with ancestors [.b, .a] where .b is closer → should match (.a is above .b)
        // but with ancestors [.a] only (no .b) → should NOT match
        TesseraSelector sel = TesseraSelector.parse(".a .b");
        TesseraNode target = node("div", List.of("b"));
        TesseraNode nodeA  = node("div", List.of("a"));
        TesseraNode nodeB  = node("div", List.of("b")); // intermediate b — not the target

        // Case 1: ancestors = [nodeA] → .a is ancestor, .b is the node itself: OK
        assertTrue(sel.matches(target, ancestors(nodeA)));

        // Case 2: no ancestors → miss
        assertFalse(sel.matches(target, new ArrayDeque<>()));
    }

    @Test
    void matches_descendant_deepHierarchy() {
        // .root .item with several intermediate nodes — should still match
        TesseraSelector sel = TesseraSelector.parse(".root .item");
        TesseraNode item     = node("div", List.of("item"));
        TesseraNode mid1     = node("div", List.of("mid1"));
        TesseraNode mid2     = node("div", List.of("mid2"));
        TesseraNode root     = node("div", List.of("root"));
        // hierarchy: root → mid1 → mid2 → item
        Deque<TesseraNode> ancs = ancestors(root, mid1, mid2); // mid2 is head = direct parent
        assertTrue(sel.matches(item, ancs), ".root .item should match through intermediate nodes");
    }

    @Test
    void parse_emptySelector_returnsNull() {
        // Guard against infinite loop on empty input (v1.8 fix)
        assertNull(TesseraSelector.parse(""));
        assertNull(TesseraSelector.parse("   "));
        assertNull(TesseraSelector.parse(null));
    }

    @Test
    void matches_child_chain_threeLevel() {
        // .a > .b > .c : strict parent chain
        TesseraSelector sel = TesseraSelector.parse(".a > .b > .c");
        TesseraNode nodeC = node("div", List.of("c"));
        TesseraNode nodeB = node("div", List.of("b"));
        TesseraNode nodeA = node("div", List.of("a"));
        Deque<TesseraNode> ancs = ancestors(nodeA, nodeB); // nodeB = direct parent, nodeA = grandparent
        assertTrue(sel.matches(nodeC, ancs), ".a > .b > .c should match strict chain");
    }

    @Test
    void matches_child_chain_threeLevel_miss_whenNotDirect() {
        // .a > .b > .c : if there's an extra node between .a and .b, must NOT match
        TesseraSelector sel = TesseraSelector.parse(".a > .b > .c");
        TesseraNode nodeC   = node("div", List.of("c"));
        TesseraNode nodeB   = node("div", List.of("b"));
        TesseraNode middle  = node("div", List.of("middle")); // extra node between a and b
        TesseraNode nodeA   = node("div", List.of("a"));
        Deque<TesseraNode> ancs = ancestors(nodeA, middle, nodeB); // nodeB=direct parent, middle between a and b
        assertFalse(sel.matches(nodeC, ancs), ".a > .b > .c must fail when .a is not direct parent of .b");
    }
}
