package com.tesseraui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TesseraStyleTest {

    // ── TesseraStyle.merge — marginTopAuto ────────────────────────────────────

    @Test
    void merge_marginTopAuto_directFieldTrue_survivesBase() {
        // Backward-compat: if code writes style.marginTopAuto = true directly,
        // merge() must still honour it even without marginTopAutoSet.
        TesseraStyle base     = new TesseraStyle();
        TesseraStyle override = new TesseraStyle();
        override.marginTopAuto = true; // direct field, no marginTopAutoSet
        TesseraStyle merged = base.merge(override);
        assertTrue(merged.marginTopAuto, "direct marginTopAuto=true must survive merge");
    }

    @Test
    void merge_marginTopAuto_setterFalse_overridesBaseTrue() {
        // Explicit setMarginTopAuto(false) must win over a base that has auto=true.
        TesseraStyle base = new TesseraStyle();
        base.setMarginTopAuto(true);
        TesseraStyle override = new TesseraStyle();
        override.setMarginTopAuto(false);
        TesseraStyle merged = base.merge(override);
        assertFalse(merged.marginTopAuto, "explicit setMarginTopAuto(false) must override base true");
    }

    @Test
    void merge_marginTopAuto_notSet_inheritsBase() {
        // Override that never mentions margin-top must leave the base value intact.
        TesseraStyle base = new TesseraStyle();
        base.setMarginTopAuto(true);
        TesseraStyle override = new TesseraStyle(); // untouched
        TesseraStyle merged = base.merge(override);
        assertTrue(merged.marginTopAuto, "unset override must inherit base marginTopAuto=true");
    }

    @Test
    void merge_marginTopAuto_setterTrue_overridesBaseFalse() {
        TesseraStyle base = new TesseraStyle(); // marginTopAuto = false by default
        TesseraStyle override = new TesseraStyle();
        override.setMarginTopAuto(true);
        TesseraStyle merged = base.merge(override);
        assertTrue(merged.marginTopAuto);
    }

    // ── TesseraAnimatedValues 4-arg constructor ───────────────────────────────

    @Test
    void animatedValues_4argCtor_nonZeroBg_hasBackground() {
        TesseraAnimatedValues v = new TesseraAnimatedValues(0xFF112233, 0, -1f, 0);
        assertTrue(v.hasBackground());
        assertFalse(v.hasColor());
        assertFalse(v.hasBorderColor());
    }

    @Test
    void animatedValues_4argCtor_zeroBg_noBackground() {
        // Legacy behaviour: zero background → treated as "not animated"
        TesseraAnimatedValues v = new TesseraAnimatedValues(0, 0xFF112233, -1f, 0);
        assertFalse(v.hasBackground(), "zero bg with 4-arg ctor treated as not animated");
        assertTrue(v.hasColor());
    }

    @Test
    void animatedValues_4argCtor_nonZeroBorder_hasBorderColor() {
        TesseraAnimatedValues v = new TesseraAnimatedValues(0, 0, -1f, 0xFF001122);
        assertTrue(v.hasBorderColor());
    }
}
