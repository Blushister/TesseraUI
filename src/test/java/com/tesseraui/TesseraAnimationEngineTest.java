package com.tesseraui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TesseraAnimationEngine}:
 * {@code lerpColor}, {@code startKeyframeAnimation}, {@code clearAnimations},
 * {@code getValues}, and {@code onHoverChanged}.
 *
 * <p>Timing-sensitive assertions use a 10-second animation duration so that
 * "immediately after start" reliably maps to t≈0, avoiding flakiness on
 * slow CI machines.  Exact color values are only verified for endpoints
 * (t=0 and t=1) or for the deterministic {@code lerpColor} helper.</p>
 */
class TesseraAnimationEngineTest {

    // Use fresh Object instances per test to avoid WeakHashMap cross-contamination.

    @BeforeEach
    void setUp() {
        // Nothing global to reset — each test uses its own widget Object.
    }

    // ── lerpColor ─────────────────────────────────────────────────────────────

    @Test
    void lerpColor_atZero_returnsFrom() {
        assertEquals(0xFF001122, TesseraAnimationEngine.lerpColor(0xFF001122, 0xFF334455, 0f));
    }

    @Test
    void lerpColor_atOne_returnsTo() {
        assertEquals(0xFF334455, TesseraAnimationEngine.lerpColor(0xFF001122, 0xFF334455, 1f));
    }

    @Test
    void lerpColor_belowZero_clampsToFrom() {
        assertEquals(0xFF001122, TesseraAnimationEngine.lerpColor(0xFF001122, 0xFF334455, -5f));
    }

    @Test
    void lerpColor_aboveOne_clampsToTo() {
        assertEquals(0xFF334455, TesseraAnimationEngine.lerpColor(0xFF001122, 0xFF334455, 5f));
    }

    @Test
    void lerpColor_midpoint_interpolatesRgbChannels() {
        // from = 0xFF000000 (opaque black), to = 0xFFFFFFFF (opaque white)
        int result = TesseraAnimationEngine.lerpColor(0xFF000000, 0xFFFFFFFF, 0.5f);
        int r = (result >> 16) & 0xFF;
        int g = (result >>  8) & 0xFF;
        int b =  result        & 0xFF;
        // Each channel: 0 + (int)((255 - 0) * 0.5) = 127
        assertEquals(127, r, "red channel");
        assertEquals(127, g, "green channel");
        assertEquals(127, b, "blue channel");
    }

    @Test
    void lerpColor_alphaInterpolated() {
        // from = 0x00FF0000 (alpha=0, treated as 0xFF per engine rule), to = 0x80FF0000 (alpha=0x80)
        int result = TesseraAnimationEngine.lerpColor(0x00FF0000, 0x80FF0000, 0.5f);
        int a = (result >>> 24) & 0xFF;
        // from alpha = 0xFF (treated), to alpha = 0x80 → lerp at 0.5 = 0xFF + (int)((0x80 - 0xFF) * 0.5)
        //   = 255 + (int)((128 - 255) * 0.5) = 255 + (int)(-63.5) = 255 - 63 = 192 = 0xC0
        assertEquals(0xC0, a, 1, "alpha channel interpolated correctly");
    }

    @Test
    void lerpColor_identicalColors_returnsFrom() {
        assertEquals(0xFF808080, TesseraAnimationEngine.lerpColor(0xFF808080, 0xFF808080, 0.5f));
    }

    // ── getValues — no animation ──────────────────────────────────────────────

    @Test
    void getValues_noAnimation_returnsNone() {
        Object w = new Object();
        assertSame(TesseraAnimatedValues.NONE, TesseraAnimationEngine.getValues(w));
    }

    // ── clearAnimations ───────────────────────────────────────────────────────

    @Test
    void clearAnimations_afterKeyframe_returnsNone() {
        Object w = new Object();
        TesseraKeyframes kf = TesseraKeyframes.builder("fade")
                .from(s -> s.background = 0xFF111111)
                .to(s ->   s.background = 0xFF999999)
                .build();
        TesseraAnimationDef def = TesseraAnimationDef.of("fade", 10_000, TesseraEasing.LINEAR, 0, -1, false);
        TesseraAnimationEngine.startKeyframeAnimation(w, kf, def);
        TesseraAnimationEngine.clearAnimations(w);

        assertSame(TesseraAnimatedValues.NONE, TesseraAnimationEngine.getValues(w));
    }

    @Test
    void clearAnimations_afterTransition_returnsNone() {
        Object w = new Object();
        TesseraStyle base  = new TesseraStyle(); base.background       = 0xFF111111;
        TesseraStyle hover = new TesseraStyle(); hover.hoverBackground = 0xFF999999;
        List<TesseraTransitionDef> defs = List.of(
                TesseraTransitionDef.of("background-color", 10_000, TesseraEasing.LINEAR, 0));
        TesseraAnimationEngine.onHoverChanged(w, true, base, hover, defs);
        TesseraAnimationEngine.clearAnimations(w);

        assertSame(TesseraAnimatedValues.NONE, TesseraAnimationEngine.getValues(w));
    }

    // ── startKeyframeAnimation ────────────────────────────────────────────────

    @Test
    void startKeyframeAnimation_sameNameReplacesExisting() {
        Object w = new Object();
        TesseraKeyframes kf1 = TesseraKeyframes.builder("anim")
                .from(s -> s.background = 0xFF110000)
                .to(s ->   s.background = 0xFF110000)
                .build();
        TesseraKeyframes kf2 = TesseraKeyframes.builder("anim")
                .from(s -> s.background = 0xFF220000)
                .to(s ->   s.background = 0xFF220000)
                .build();
        TesseraAnimationDef def = TesseraAnimationDef.of("anim", 10_000, TesseraEasing.LINEAR, 0, -1, false);

        TesseraAnimationEngine.startKeyframeAnimation(w, kf1, def);
        TesseraAnimationEngine.startKeyframeAnimation(w, kf2, def);

        // At t≈0, color should come from kf2 (the replacement)
        TesseraAnimatedValues v = TesseraAnimationEngine.getValues(w);
        assertTrue(v.hasBackground(), "background animation must be active");
        assertEquals(0xFF220000, v.background(), "kf2 must have replaced kf1");
    }

    @Test
    void startKeyframeAnimation_differentNames_bothActive() {
        Object w = new Object();
        TesseraKeyframes kfBg = TesseraKeyframes.builder("bg-anim")
                .from(s -> s.background = 0xFF220000)
                .to(s ->   s.background = 0xFF220000)
                .build();
        TesseraKeyframes kfBorder = TesseraKeyframes.builder("border-anim")
                .from(s -> s.borderColor = 0xFF00FF00)
                .to(s ->   s.borderColor = 0xFF00FF00)
                .build();
        TesseraAnimationDef def = TesseraAnimationDef.of("any", 10_000, TesseraEasing.LINEAR, 0, -1, false);

        TesseraAnimationEngine.startKeyframeAnimation(w, kfBg,     def);
        TesseraAnimationEngine.startKeyframeAnimation(w, kfBorder, def);

        TesseraAnimatedValues v = TesseraAnimationEngine.getValues(w);
        assertTrue(v.hasBackground(),  "background animation must be active");
        assertTrue(v.hasBorderColor(), "border-color animation must be active");
    }

    // ── getValues — keyframe at t≈0 ───────────────────────────────────────────

    @Test
    void getValues_keyframeAtStart_backgroundNearFromColor() {
        Object w = new Object();
        TesseraKeyframes kf = TesseraKeyframes.builder("test")
                .from(s -> s.background = 0xFF112233)
                .to(s ->   s.background = 0xFF998877)
                .build();
        // 10s duration: progress immediately after start is ≈ 0
        TesseraAnimationDef def = TesseraAnimationDef.of("test", 10_000, TesseraEasing.LINEAR, 0, -1, false);
        TesseraAnimationEngine.startKeyframeAnimation(w, kf, def);

        TesseraAnimatedValues v = TesseraAnimationEngine.getValues(w);
        assertTrue(v.hasBackground());
        // Allow ±2 per channel for sub-millisecond progress drift
        assertEquals(0x11, (v.background() >> 16) & 0xFF, 2, "red channel near from");
        assertEquals(0x22, (v.background() >>  8) & 0xFF, 2, "green channel near from");
        assertEquals(0x33,  v.background()        & 0xFF, 2, "blue channel near from");
    }

    @Test
    void getValues_opacityKeyframe_atStart_nearZero() {
        Object w = new Object();
        TesseraKeyframes kf = TesseraKeyframes.builder("fadeIn")
                .from(s -> s.opacity = 0f)
                .to(s ->   s.opacity = 1f)
                .build();
        TesseraAnimationDef def = TesseraAnimationDef.of("fadeIn", 10_000, TesseraEasing.LINEAR, 0, -1, false);
        TesseraAnimationEngine.startKeyframeAnimation(w, kf, def);

        TesseraAnimatedValues v = TesseraAnimationEngine.getValues(w);
        assertTrue(v.hasOpacity());
        assertEquals(0f, v.opacity(), 0.05f, "opacity near 0 immediately after start");
    }

    @Test
    void getValues_borderColorKeyframe_atStart_nearFrom() {
        Object w = new Object();
        TesseraKeyframes kf = TesseraKeyframes.builder("border")
                .from(s -> s.borderColor = 0xFF00FF00)
                .to(s ->   s.borderColor = 0xFF0000FF)
                .build();
        TesseraAnimationDef def = TesseraAnimationDef.of("border", 10_000, TesseraEasing.LINEAR, 0, -1, false);
        TesseraAnimationEngine.startKeyframeAnimation(w, kf, def);

        TesseraAnimatedValues v = TesseraAnimationEngine.getValues(w);
        assertTrue(v.hasBorderColor());
        assertEquals(0x00, (v.borderColor() >> 16) & 0xFF, 2, "red near 0");
        assertEquals(0xFF, (v.borderColor() >>  8) & 0xFF, 2, "green near 0xFF");
        assertEquals(0x00,  v.borderColor()        & 0xFF, 2, "blue near 0");
    }

    // ── onHoverChanged ────────────────────────────────────────────────────────

    @Test
    void onHoverChanged_nullDefs_noTransition() {
        Object w = new Object();
        TesseraStyle base  = new TesseraStyle(); base.background       = 0xFF111111;
        TesseraStyle hover = new TesseraStyle(); hover.hoverBackground = 0xFF999999;
        TesseraAnimationEngine.onHoverChanged(w, true, base, hover, null);
        assertSame(TesseraAnimatedValues.NONE, TesseraAnimationEngine.getValues(w));
    }

    @Test
    void onHoverChanged_emptyDefs_noTransition() {
        Object w = new Object();
        TesseraStyle base  = new TesseraStyle(); base.background       = 0xFF111111;
        TesseraStyle hover = new TesseraStyle(); hover.hoverBackground = 0xFF999999;
        TesseraAnimationEngine.onHoverChanged(w, true, base, hover, List.of());
        assertSame(TesseraAnimatedValues.NONE, TesseraAnimationEngine.getValues(w));
    }

    @Test
    void onHoverChanged_background_startsTransition() {
        Object w = new Object();
        TesseraStyle base  = new TesseraStyle(); base.background       = 0xFF111111;
        TesseraStyle hover = new TesseraStyle(); hover.hoverBackground = 0xFF999999;
        List<TesseraTransitionDef> defs = List.of(
                TesseraTransitionDef.of("background-color", 10_000, TesseraEasing.LINEAR, 0));
        TesseraAnimationEngine.onHoverChanged(w, true, base, hover, defs);

        TesseraAnimatedValues v = TesseraAnimationEngine.getValues(w);
        assertTrue(v.hasBackground(), "background transition must be active");
    }

    @Test
    void onHoverChanged_color_startsTransition() {
        Object w = new Object();
        TesseraStyle base  = new TesseraStyle(); base.color       = 0xFFCCCCCC;
        TesseraStyle hover = new TesseraStyle(); hover.hoverColor = 0xFF000000;
        List<TesseraTransitionDef> defs = List.of(
                TesseraTransitionDef.of("color", 10_000, TesseraEasing.LINEAR, 0));
        TesseraAnimationEngine.onHoverChanged(w, true, base, hover, defs);

        TesseraAnimatedValues v = TesseraAnimationEngine.getValues(w);
        assertTrue(v.hasColor(), "color transition must be active");
    }

    @Test
    void onHoverChanged_borderColor_startsTransition() {
        Object w = new Object();
        TesseraStyle base  = new TesseraStyle(); base.borderColor       = 0xFF222222;
        TesseraStyle hover = new TesseraStyle(); hover.hoverBorderColor = 0xFF888888;
        List<TesseraTransitionDef> defs = List.of(
                TesseraTransitionDef.of("border-color", 10_000, TesseraEasing.LINEAR, 0));
        TesseraAnimationEngine.onHoverChanged(w, true, base, hover, defs);

        TesseraAnimatedValues v = TesseraAnimationEngine.getValues(w);
        assertTrue(v.hasBorderColor(), "border-color transition must be active");
    }

    @Test
    void onHoverChanged_sameFromAndTo_noTransition() {
        // If base and hover backgrounds are identical, no transition should be registered.
        Object w = new Object();
        TesseraStyle base  = new TesseraStyle(); base.background       = 0xFF111111;
        TesseraStyle hover = new TesseraStyle(); hover.hoverBackground = 0xFF111111; // identical
        List<TesseraTransitionDef> defs = List.of(
                TesseraTransitionDef.of("background-color", 200, TesseraEasing.LINEAR, 0));
        TesseraAnimationEngine.onHoverChanged(w, true, base, hover, defs);

        assertSame(TesseraAnimatedValues.NONE, TesseraAnimationEngine.getValues(w),
                "no transition should start when from == to");
    }

    @Test
    void onHoverChanged_hoverOut_transitionToBase() {
        Object w = new Object();
        TesseraStyle base  = new TesseraStyle(); base.background       = 0xFF111111;
        TesseraStyle hover = new TesseraStyle(); hover.hoverBackground = 0xFF999999;
        List<TesseraTransitionDef> defs = List.of(
                TesseraTransitionDef.of("background-color", 10_000, TesseraEasing.LINEAR, 0));

        // Hover in, then hover out
        TesseraAnimationEngine.onHoverChanged(w, true,  base, hover, defs);
        TesseraAnimationEngine.onHoverChanged(w, false, base, hover, defs);

        TesseraAnimatedValues v = TesseraAnimationEngine.getValues(w);
        assertTrue(v.hasBackground(), "background transition must be active after hover-out");
    }

    // ── TesseraAnimatedValues helpers ─────────────────────────────────────────

    @Test
    void animatedValues_none_hasNoProperties() {
        assertFalse(TesseraAnimatedValues.NONE.hasBackground());
        assertFalse(TesseraAnimatedValues.NONE.hasColor());
        assertFalse(TesseraAnimatedValues.NONE.hasOpacity());
        assertFalse(TesseraAnimatedValues.NONE.hasBorderColor());
    }

    @Test
    void animatedValues_nonZeroBackground_hasBackground() {
        TesseraAnimatedValues v = new TesseraAnimatedValues(0xFF112233, 0, -1f, 0, TesseraAnimatedValues.BG_BIT);
        assertTrue(v.hasBackground());
        assertFalse(v.hasColor());
        assertFalse(v.hasOpacity());
        assertFalse(v.hasBorderColor());
    }

    @Test
    void animatedValues_zeroOpacity_hasOpacity() {
        TesseraAnimatedValues v = new TesseraAnimatedValues(0, 0, 0f, 0, 0);
        assertTrue(v.hasOpacity(), "opacity=0 means fully transparent, not absent");
    }

    // ── transition to transparent (0x00000000 is a valid animated color) ──────

    @Test
    void onHoverChanged_toTransparent_isAnimated() {
        Object w = new Object();
        TesseraStyle base  = new TesseraStyle(); base.background       = 0xFF112233;
        TesseraStyle hover = new TesseraStyle(); hover.hoverBackground = 0x00000000;
        List<TesseraTransitionDef> defs = List.of(
                TesseraTransitionDef.of("background-color", 10_000, TesseraEasing.LINEAR, 0));
        TesseraAnimationEngine.onHoverChanged(w, true, base, hover, defs);

        TesseraAnimatedValues v = TesseraAnimationEngine.getValues(w);
        // Transparent is a valid target: bitmask must report background as active
        assertTrue(v.hasBackground(), "transition to transparent must be reported as animated");
    }

    // ── expired transitions are eventually removed ─────────────────────────────

    @Test
    void transition_expiredAfterDuration_noLongerAnimated() throws InterruptedException {
        Object w = new Object();
        TesseraStyle base  = new TesseraStyle(); base.background       = 0xFF111111;
        TesseraStyle hover = new TesseraStyle(); hover.hoverBackground = 0xFF999999;
        List<TesseraTransitionDef> defs = List.of(
                TesseraTransitionDef.of("background-color", 50, TesseraEasing.LINEAR, 0));
        TesseraAnimationEngine.onHoverChanged(w, true, base, hover, defs);

        Thread.sleep(150); // well past the 50 ms duration

        // After expiry, getValues removes the state; subsequent call returns NONE
        TesseraAnimationEngine.getValues(w); // first call removes expired entries
        assertSame(TesseraAnimatedValues.NONE, TesseraAnimationEngine.getValues(w),
                "expired transition must not linger");
    }
}
