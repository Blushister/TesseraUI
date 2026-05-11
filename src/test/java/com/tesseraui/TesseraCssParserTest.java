package com.tesseraui;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TesseraCssParser}: color parsing, property parsing,
 * shorthand expansion, CSS variables, and new v1.0+ properties.
 *
 * <p>Colors are compared as signed {@code int} values (ARGB) — the same
 * representation used throughout TesseraUI.</p>
 *
 * <p>The class uses Mojang {@code LogUtils} (NeoForge dependency) only for
 * warn-level logging; it is available on the test classpath via the
 * {@code implementation} NeoForge dependency.</p>
 */
class TesseraCssParserTest {

    // ── parseColor ────────────────────────────────────────────────────────────

    @Test
    void parseColor_hex6_fullAlpha() {
        // #FF8000 → 0xFFFF8000 (alpha = 0xFF injected)
        assertEquals(0xFFFF8000, TesseraCssParser.parseColor("#FF8000"));
    }

    @Test
    void parseColor_hex6_lowercase() {
        assertEquals(0xFFFF8000, TesseraCssParser.parseColor("#ff8000"));
    }

    @Test
    void parseColor_hex8_explicitAlpha() {
        // #80FF8000 = alpha 0x80, R=0xFF, G=0x80, B=0x00
        assertEquals(0x80FF8000, TesseraCssParser.parseColor("#80FF8000"));
    }

    @Test
    void parseColor_namedWhite() {
        assertEquals(0xFFFFFFFF, TesseraCssParser.parseColor("white"));
    }

    @Test
    void parseColor_namedBlack() {
        assertEquals(0xFF000000, TesseraCssParser.parseColor("black"));
    }

    @Test
    void parseColor_namedTransparent() {
        assertEquals(0x00000000, TesseraCssParser.parseColor("transparent"));
    }

    @Test
    void parseColor_namedCopper() {
        assertEquals(0xFFB87333, TesseraCssParser.parseColor("copper"));
    }

    @Test
    void parseColor_rgb() {
        assertEquals(0xFFFF8000, TesseraCssParser.parseColor("rgb(255, 128, 0)"));
    }

    @Test
    void parseColor_rgba() {
        int c = TesseraCssParser.parseColor("rgba(255, 128, 0, 0.5)");
        // alpha = (int)(0.5 * 255) = 127 = 0x7F
        assertEquals(0x7F, (c >>> 24) & 0xFF);
        assertEquals(0xFF, (c >>> 16) & 0xFF);
        assertEquals(0x80, (c >>> 8)  & 0xFF);
        assertEquals(0x00,  c         & 0xFF);
    }

    @Test
    void parseColor_unknownThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> TesseraCssParser.parseColor("notacolor"));
    }

    // ── CSS rule parsing ─────────────────────────────────────────────────────

    @Test
    void parse_backgroundColor() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".box { background: #1A1A1A }");
        TesseraStyle s = resolveClass(sheet, "box");
        assertEquals(0xFF1A1A1A, s.background);
    }

    @Test
    void parse_color() {
        TesseraStyleSheet sheet = TesseraCssParser.parse("p { color: white }");
        TesseraStyle s = resolveTag(sheet, "p");
        assertEquals(0xFFFFFFFF, s.color);
    }

    @Test
    void parse_fontSize_px() {
        TesseraStyleSheet sheet = TesseraCssParser.parse("p { font-size: 7px }");
        TesseraStyle s = resolveTag(sheet, "p");
        assertEquals(7f, s.fontSize, 0.01f);
    }

    @Test
    void parse_fontSize_bare() {
        TesseraStyleSheet sheet = TesseraCssParser.parse("p { font-size: 9 }");
        TesseraStyle s = resolveTag(sheet, "p");
        assertEquals(9f, s.fontSize, 0.01f);
    }

    @Test
    void parse_fontWeight_bold() {
        TesseraStyleSheet sheet = TesseraCssParser.parse("p { font-weight: bold }");
        TesseraStyle s = resolveTag(sheet, "p");
        assertEquals(700, s.fontWeight);
    }

    @Test
    void parse_fontWeight_numeric() {
        TesseraStyleSheet sheet = TesseraCssParser.parse("p { font-weight: 400 }");
        TesseraStyle s = resolveTag(sheet, "p");
        assertEquals(400, s.fontWeight);
    }

    @Test
    void parse_padding_single() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".box { padding: 5px }");
        TesseraStyle s = resolveClass(sheet, "box");
        assertEquals(5, s.paddingTop);
        assertEquals(5, s.paddingRight);
        assertEquals(5, s.paddingBottom);
        assertEquals(5, s.paddingLeft);
    }

    @Test
    void parse_padding_twoValues() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".box { padding: 4px 8px }");
        TesseraStyle s = resolveClass(sheet, "box");
        assertEquals(4, s.paddingTop);
        assertEquals(8, s.paddingRight);
        assertEquals(4, s.paddingBottom);
        assertEquals(8, s.paddingLeft);
    }

    @Test
    void parse_padding_fourValues() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".box { padding: 1px 2px 3px 4px }");
        TesseraStyle s = resolveClass(sheet, "box");
        assertEquals(1, s.paddingTop);
        assertEquals(2, s.paddingRight);
        assertEquals(3, s.paddingBottom);
        assertEquals(4, s.paddingLeft);
    }

    @Test
    void parse_gap() {
        TesseraStyleSheet sheet = TesseraCssParser.parse("row { gap: 6px }");
        TesseraStyle s = resolveTag(sheet, "row");
        assertEquals(6, s.gap);
    }

    @Test
    void parse_border_shorthand() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".panel { border: 1px solid #3A2E22 }");
        TesseraStyle s = resolveClass(sheet, "panel");
        assertEquals(1, s.border);
        assertEquals(0xFF3A2E22, s.borderColor);
    }

    @Test
    void parse_textDecoration_underline() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".ul { text-decoration: underline }");
        TesseraStyle s = resolveClass(sheet, "ul");
        assertEquals("underline", s.textDecoration);
    }

    @Test
    void parse_textDecoration_lineThrough() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".lt { text-decoration: line-through }");
        TesseraStyle s = resolveClass(sheet, "lt");
        assertEquals("line-through", s.textDecoration);
    }

    @Test
    void parse_textDecoration_none() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".n { text-decoration: none }");
        TesseraStyle s = resolveClass(sheet, "n");
        assertEquals("none", s.textDecoration);
    }

    // ── border-radius (v1.1) ─────────────────────────────────────────────────

    @Test
    void parse_borderRadius_px() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".card { border-radius: 4px }");
        TesseraStyle s = resolveClass(sheet, "card");
        assertEquals(4, s.borderRadius);
    }

    @Test
    void parse_borderRadius_bare() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".card { border-radius: 6 }");
        TesseraStyle s = resolveClass(sheet, "card");
        assertEquals(6, s.borderRadius);
    }

    @Test
    void parse_borderRadius_zero() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".card { border-radius: 0 }");
        TesseraStyle s = resolveClass(sheet, "card");
        assertEquals(0, s.borderRadius);
    }

    // ── CSS variables ────────────────────────────────────────────────────────

    @Test
    void parse_cssVariables_resolved() {
        String css = ":root { --copper: #B87333 } .label { color: var(--copper) }";
        TesseraStyleSheet sheet = TesseraCssParser.parse(css);
        TesseraStyle s = resolveClass(sheet, "label");
        assertEquals(0xFFB87333, s.color);
    }

    @Test
    void parse_cssVariables_multipleUses() {
        String css = ":root { --r: 4 } .a { border-radius: var(--r) } .b { gap: var(--r) }";
        TesseraStyleSheet sheet = TesseraCssParser.parse(css);
        assertEquals(4, resolveClass(sheet, "a").borderRadius);
        assertEquals(4, resolveClass(sheet, "b").gap);
    }

    // ── width / height ───────────────────────────────────────────────────────

    @Test
    void parse_width_px() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".card { width: 58px }");
        TesseraStyle s = resolveClass(sheet, "card");
        assertEquals(58, s.width);
        assertFalse(s.widthPercent);
    }

    @Test
    void parse_height_px() {
        TesseraStyleSheet sheet = TesseraCssParser.parse("row { height: 36px }");
        TesseraStyle s = resolveTag(sheet, "row");
        assertEquals(36, s.height);
    }

    // ── alignItems / justifyContent ──────────────────────────────────────────

    @Test
    void parse_alignItems() {
        TesseraStyleSheet sheet = TesseraCssParser.parse("row { align-items: center }");
        TesseraStyle s = resolveTag(sheet, "row");
        assertEquals("center", s.alignItems);
    }

    @Test
    void parse_justifyContent() {
        TesseraStyleSheet sheet = TesseraCssParser.parse("row { justify-content: flex-end }");
        TesseraStyle s = resolveTag(sheet, "row");
        assertEquals("flex-end", s.justifyContent);
    }

    // ── style merge ──────────────────────────────────────────────────────────

    @Test
    void style_merge_otherOverridesBase() {
        TesseraStyle base = new TesseraStyle();
        base.color = 0xFF000000;
        base.fontSize = 7f;

        TesseraStyle other = new TesseraStyle();
        other.color = 0xFFFFFFFF;

        TesseraStyle merged = base.merge(other);
        assertEquals(0xFFFFFFFF, merged.color);
        assertEquals(7f, merged.fontSize, 0.01f); // kept from base
    }

    @Test
    void style_merge_borderRadius_fromOther() {
        TesseraStyle base = new TesseraStyle();
        base.borderRadius = 2;

        TesseraStyle other = new TesseraStyle();
        other.borderRadius = 6;

        TesseraStyle merged = base.merge(other);
        assertEquals(6, merged.borderRadius);
    }

    @Test
    void style_merge_borderRadius_keepBase_whenOtherUnset() {
        TesseraStyle base = new TesseraStyle();
        base.borderRadius = 4;

        TesseraStyle other = new TesseraStyle(); // UNSET

        TesseraStyle merged = base.merge(other);
        assertEquals(4, merged.borderRadius);
    }

    @Test
    void style_merge_textDecoration_inherits() {
        TesseraStyle base = new TesseraStyle();
        base.textDecoration = "underline";

        TesseraStyle other = new TesseraStyle(); // null

        TesseraStyle merged = base.merge(other);
        assertEquals("underline", merged.textDecoration);
    }

    // ── opacity ──────────────────────────────────────────────────────────────

    @Test
    void parse_opacity() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".ghost { opacity: 0.5 }");
        TesseraStyle s = resolveClass(sheet, "ghost");
        assertEquals(0.5f, s.opacity, 0.001f);
    }

    // ── calc() ────────────────────────────────────────────────────────────────

    @Test
    void evalCalc_pctMinusPx() {
        // calc(100% - 40px) with basis=200 → 200 - 40 = 160
        assertEquals(160, TesseraCssParser.evalCalc("calc(100% - 40px)", 200));
    }

    @Test
    void evalCalc_pctPlusPx() {
        // calc(50% + 10px) with basis=200 → 100 + 10 = 110
        assertEquals(110, TesseraCssParser.evalCalc("calc(50% + 10px)", 200));
    }

    @Test
    void evalCalc_pxMinusPx() {
        assertEquals(80, TesseraCssParser.evalCalc("calc(100px - 20px)", 999));
    }

    @Test
    void evalCalc_withoutCalcWrapper() {
        // Should work even without the "calc(" prefix (bare expression)
        assertEquals(160, TesseraCssParser.evalCalc("100% - 40px", 200));
    }

    @Test
    void evalCalc_pctOnly() {
        assertEquals(150, TesseraCssParser.evalCalc("calc(75%)", 200));
    }

    @Test
    void parse_width_calc_storedAsExpression() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".sidebar { width: calc(100% - 120px) }");
        TesseraStyle s = resolveClass(sheet, "sidebar");
        assertNotNull(s.widthCalc);
        assertEquals(TesseraStyle.UNSET, s.width); // plain width NOT set
        // Evaluating with basis=300 → 300 - 120 = 180
        assertEquals(180, TesseraCssParser.evalCalc(s.widthCalc, 300));
    }

    @Test
    void parse_height_calc_storedAsExpression() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".panel { height: calc(50% + 8px) }");
        TesseraStyle s = resolveClass(sheet, "panel");
        assertNotNull(s.heightCalc);
        assertEquals(TesseraStyle.UNSET, s.height);
        assertEquals(58, TesseraCssParser.evalCalc(s.heightCalc, 100));
    }

    @Test
    void parse_width_plain_clearsCalc() {
        // If a plain width is declared after a calc, widthCalc should be null
        TesseraStyleSheet sheet = TesseraCssParser.parse(".box { width: 80px }");
        TesseraStyle s = resolveClass(sheet, "box");
        assertNull(s.widthCalc);
        assertEquals(80, s.width);
    }

    // ── flex-grow / flex-shrink / flex-basis / order ──────────────────────────

    @Test
    void parse_flexGrow() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { flex-grow: 2 }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertEquals(2f, s.flexGrow, 0.001f);
    }

    @Test
    void parse_flexShrink() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { flex-shrink: 0 }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertEquals(0f, s.flexShrink, 0.001f);
    }

    @Test
    void parse_flexBasis_px() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { flex-basis: 80px }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertEquals(80, s.flexBasis);
    }

    @Test
    void parse_flexBasis_auto() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { flex-basis: auto }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertEquals(TesseraStyle.UNSET, s.flexBasis);
    }

    @Test
    void parse_order() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".last { order: 99 }");
        TesseraStyle s = resolveClass(sheet, "last");
        assertEquals(99, s.order);
    }

    // ── flex shorthand ────────────────────────────────────────────────────────

    @Test
    void parse_flex_single_grow() {
        // flex: 2 → flexGrow=2, flexShrink=1, flexBasis=0
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { flex: 2 }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertEquals(2f, s.flexGrow,   0.001f);
        assertEquals(1f, s.flexShrink, 0.001f);
        assertEquals(0,  s.flexBasis);
    }

    @Test
    void parse_flex_zero_autoBase() {
        // flex: 0 → flexGrow=0, flexShrink=1, flexBasis=UNSET (auto/content)
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { flex: 0 }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertEquals(0f, s.flexGrow, 0.001f);
        assertEquals(TesseraStyle.UNSET, s.flexBasis);
    }

    @Test
    void parse_flex_twoValues() {
        // flex: 1 0 → flexGrow=1, flexShrink=0, flexBasis=0
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { flex: 1 0 }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertEquals(1f, s.flexGrow,   0.001f);
        assertEquals(0f, s.flexShrink, 0.001f);
        assertEquals(0,  s.flexBasis);
    }

    @Test
    void parse_flex_threeValues() {
        // flex: 1 1 80px → flexGrow=1, flexShrink=1, flexBasis=80
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { flex: 1 1 80px }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertEquals(1f,  s.flexGrow,   0.001f);
        assertEquals(1f,  s.flexShrink, 0.001f);
        assertEquals(80,  s.flexBasis);
    }

    @Test
    void parse_flex_none() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { flex: none }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertEquals(0f, s.flexGrow,   0.001f);
        assertEquals(0f, s.flexShrink, 0.001f);
        assertEquals(TesseraStyle.UNSET, s.flexBasis);
    }

    @Test
    void parse_flex_auto() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { flex: auto }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertEquals(1f, s.flexGrow,   0.001f);
        assertEquals(1f, s.flexShrink, 0.001f);
        assertEquals(TesseraStyle.UNSET, s.flexBasis);
    }

    @Test
    void style_merge_flexGrow_fromOther() {
        TesseraStyle base  = new TesseraStyle(); base.flexGrow  = 1f;
        TesseraStyle other = new TesseraStyle(); other.flexGrow = 3f;
        assertEquals(3f, base.merge(other).flexGrow, 0.001f);
    }

    @Test
    void style_merge_flexGrow_keepBase_whenOtherUnset() {
        TesseraStyle base  = new TesseraStyle(); base.flexGrow = 2f;
        TesseraStyle other = new TesseraStyle(); // UNSET_F
        assertEquals(2f, base.merge(other).flexGrow, 0.001f);
    }

    @Test
    void style_merge_order_fromOther() {
        TesseraStyle base  = new TesseraStyle(); base.order  = 5;
        TesseraStyle other = new TesseraStyle(); other.order = 10;
        assertEquals(10, base.merge(other).order);
    }

    @Test
    void style_merge_widthCalc_fromOther() {
        TesseraStyle base  = new TesseraStyle(); base.widthCalc  = "calc(50% - 10px)";
        TesseraStyle other = new TesseraStyle(); other.widthCalc = "calc(100% - 20px)";
        assertEquals("calc(100% - 20px)", base.merge(other).widthCalc);
    }

    @Test
    void style_merge_widthCalc_keepBase_whenOtherNull() {
        TesseraStyle base  = new TesseraStyle(); base.widthCalc = "calc(80% - 5px)";
        TesseraStyle other = new TesseraStyle(); // null
        assertEquals("calc(80% - 5px)", base.merge(other).widthCalc);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Resolve the style for a class selector against a single matching node. */
    private static TesseraStyle resolveClass(TesseraStyleSheet sheet, String className) {
        TesseraNode node = new TesseraNode(
                "div",
                Map.of("class", className),
                List.of(), "");
        return sheet.resolve(node, new ArrayDeque<>());
    }

    /** Resolve the style for a tag selector against a single matching node. */
    private static TesseraStyle resolveTag(TesseraStyleSheet sheet, String tag) {
        TesseraNode node = new TesseraNode(tag, Map.of(), List.of(), "");
        return sheet.resolve(node, new ArrayDeque<>());
    }

    // ── hex short colors (v1.8 fix) ──────────────────────────────────────────

    @Test
    void parseColor_hex3_expanded() {
        // #F80 → #FF8800
        assertEquals(0xFFFF8800, TesseraCssParser.parseColor("#F80"));
    }

    @Test
    void parseColor_hex3_lowercase() {
        assertEquals(0xFFFF8800, TesseraCssParser.parseColor("#f80"));
    }

    @Test
    void parseColor_hex4_expanded() {
        // #F80F → #FF8800FF (alpha = 0xFF)
        int c = TesseraCssParser.parseColor("#F80F");
        assertEquals(0xFF, (c >>> 24) & 0xFF, "alpha");
        assertEquals(0xFF, (c >>> 16) & 0xFF, "red");
        assertEquals(0x88, (c >>> 8)  & 0xFF, "green");
        assertEquals(0x00,  c         & 0xFF, "blue");
    }

    // ── @at-rule stripping (v1.8 fix) ────────────────────────────────────────

    @Test
    void parse_keyframesBlockDoesNotLeakAsRule() {
        // @keyframes must be stripped before flat rule parsing — its body
        // must not be interpreted as CSS selectors/properties.
        String css = "@keyframes spin { from { transform: rotate(0deg) } to { transform: rotate(360deg) } }" +
                     " .box { background: #111111 }";
        TesseraStyleSheet sheet = TesseraCssParser.parse(css);
        TesseraStyle s = resolveClass(sheet, "box");
        // .box must be parsed correctly → background set
        assertEquals(0xFF111111, s.background);
        // And no spurious rule with tag "from" or "to" should exist
        TesseraStyle from = resolveTag(sheet, "from");
        assertEquals(TesseraStyle.UNSET, from.background, "from{} must not produce a rule");
    }

    @Test
    void parse_importStripped() {
        // @import must be stripped — should not produce any rule or error
        String css = "@import url('other.css'); .label { color: white }";
        TesseraStyleSheet sheet = TesseraCssParser.parse(css);
        TesseraStyle s = resolveClass(sheet, "label");
        assertEquals(0xFFFFFFFF, s.color);
    }

    // ── margin-top auto vs numeric (v1.8 fix) ────────────────────────────────

    @Test
    void parse_marginTop_auto_setsFlag_andClearsNumeric() {
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { margin-top: auto }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertTrue(s.marginTopAuto, "marginTopAuto must be true");
        assertEquals(TesseraStyle.UNSET, s.marginTop, "marginTop must be UNSET when auto");
    }

    @Test
    void parse_marginTop_numeric_clearsAuto() {
        // Numeric value must NOT set marginTopAuto
        TesseraStyleSheet sheet = TesseraCssParser.parse(".item { margin-top: 8px }");
        TesseraStyle s = resolveClass(sheet, "item");
        assertFalse(s.marginTopAuto, "marginTopAuto must be false for numeric value");
        assertEquals(8, s.marginTop);
    }

    // ── MediaBlock sentinel (v1.8 fix) ───────────────────────────────────────

    @Test
    void mediaBlock_noConstraint_matchesAnyViewport() {
        // @media without min/max should match everything
        String css = "@media { .box { background: #222222 } }";
        TesseraStyleSheet sheet = TesseraCssParser.parse(css);
        // forViewport should include this block for any width
        TesseraStyleSheet resolved = sheet.forViewport(100);
        TesseraNode node = new TesseraNode("div", java.util.Map.of("class", "box"), java.util.List.of(), "");
        TesseraStyle s = resolved.resolve(node, new java.util.ArrayDeque<>());
        assertEquals(0xFF222222, s.background);
    }

    @Test
    void mediaBlock_maxWidth_matchesBelow() {
        String css = "@media (max-width: 400px) { .narrow { background: #333333 } }";
        TesseraStyleSheet sheet = TesseraCssParser.parse(css);

        TesseraNode node = new TesseraNode("div", java.util.Map.of("class", "narrow"), java.util.List.of(), "");

        // 300px ≤ 400 → should match
        TesseraStyle matched = sheet.forViewport(300).resolve(node, new java.util.ArrayDeque<>());
        assertEquals(0xFF333333, matched.background, "should match at 300px");

        // 500px > 400 → should NOT match
        TesseraStyle unmatched = sheet.forViewport(500).resolve(node, new java.util.ArrayDeque<>());
        assertEquals(TesseraStyle.UNSET, unmatched.background, "should not match at 500px");
    }

    @Test
    void mediaBlock_minWidth_matchesAbove() {
        String css = "@media (min-width: 400px) { .wide { background: #444444 } }";
        TesseraStyleSheet sheet = TesseraCssParser.parse(css);

        TesseraNode node = new TesseraNode("div", java.util.Map.of("class", "wide"), java.util.List.of(), "");

        // 500px ≥ 400 → should match
        TesseraStyle matched = sheet.forViewport(500).resolve(node, new java.util.ArrayDeque<>());
        assertEquals(0xFF444444, matched.background, "should match at 500px");

        // 300px < 400 → should NOT match
        TesseraStyle unmatched = sheet.forViewport(300).resolve(node, new java.util.ArrayDeque<>());
        assertEquals(TesseraStyle.UNSET, unmatched.background, "should not match at 300px");
    }

    @Test
    void mediaBlock_displayNone_overriddenByMedia() {
        // Regression: flat display:none must be overridden by media display:flex
        // (the cascade-order fix in forViewport)
        String css = ".target { display: none } @media (min-width: 400px) { .target { display: flex } }";
        TesseraStyleSheet sheet = TesseraCssParser.parse(css);

        TesseraNode node = new TesseraNode("div", java.util.Map.of("class", "target"), java.util.List.of(), "");

        TesseraStyle at500 = sheet.forViewport(500).resolve(node, new java.util.ArrayDeque<>());
        assertEquals("flex", at500.display, "media rule must win over flat display:none at 500px");

        TesseraStyle at300 = sheet.forViewport(300).resolve(node, new java.util.ArrayDeque<>());
        assertEquals("none", at300.display, "flat display:none must apply at 300px (media not matched)");
    }
}
