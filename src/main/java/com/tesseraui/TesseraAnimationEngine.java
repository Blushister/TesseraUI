package com.tesseraui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Central animation runtime for TesseraUI.
 *
 * <p>Manages two types of animations per widget:
 * <ol>
 *   <li><b>Transitions</b> — triggered by state changes (hover in/out).
 *       Smoothly interpolates CSS properties from their current value to the
 *       target value over a defined duration.</li>
 *   <li><b>Keyframe animations</b> — started explicitly or via the {@code animation}
 *       CSS property.  Loops through a {@link TesseraKeyframes} sequence.</li>
 * </ol>
 *
 * <p>Call {@link #getValues(Object)} each render frame to get the current
 * animated values for a widget.  The engine uses {@link System#nanoTime()} as
 * its time source — no external tick is required.</p>
 */
public final class TesseraAnimationEngine {

    // ── Internal state ─────────────────────────────────────────────────────────

    /** Active transition per widget per property. Keyed by widget identity. */
    private static final Map<Object, List<TransitionState>> transitions =
            new WeakHashMap<>();

    /** Active keyframe animations per widget. */
    private static final Map<Object, List<KeyframeState>> keyframeAnims =
            new WeakHashMap<>();

    private TesseraAnimationEngine() {}

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Called when a widget's hover state changes.  For each property listed in
     * {@code transitionDefs}, starts a transition from the current animated
     * (or static) value to the target value.
     *
     * @param widget         the widget whose state changed (used as map key)
     * @param hovered        new hover state
     * @param base           resolved base style
     * @param hover          resolved hover style
     * @param transitionDefs transition definitions parsed from CSS
     */
    public static void onHoverChanged(Object widget, boolean hovered,
                                       TesseraStyle base, TesseraStyle hover,
                                       List<TesseraTransitionDef> transitionDefs) {
        if (transitionDefs == null || transitionDefs.isEmpty()) return;

        TesseraAnimatedValues current = getValues(widget);
        long now = System.nanoTime();

        List<TransitionState> list = transitions.computeIfAbsent(widget, k -> new ArrayList<>());

        for (TesseraTransitionDef def : transitionDefs) {
            String prop = normalise(def.property());
            switch (prop) {
                case "background", "background-color" -> {
                    int from = current.hasBackground() ? current.background()
                             : (hovered ? valueOr(base.background, 0) : valueOr(hover.hoverBackground, base.background));
                    int to   = hovered ? valueOr(hover.hoverBackground, base.background)
                                       : valueOr(base.background, 0);
                    if (from != to)
                        replaceTransition(list, prop, from, to, now, def);
                }
                case "color" -> {
                    int from = current.hasColor() ? current.color()
                             : (hovered ? valueOr(base.color, 0) : valueOr(hover.hoverColor, base.color));
                    int to   = hovered ? valueOr(hover.hoverColor, base.color)
                                       : valueOr(base.color, 0);
                    if (from != to)
                        replaceTransition(list, prop, from, to, now, def);
                }
                case "border-color" -> {
                    int from = current.hasBorderColor() ? current.borderColor()
                             : (hovered ? valueOr(base.borderColor, 0) : valueOr(hover.hoverBorderColor, base.borderColor));
                    int to   = hovered ? valueOr(hover.hoverBorderColor, base.borderColor)
                                       : valueOr(base.borderColor, 0);
                    if (from != to)
                        replaceTransition(list, prop, from, to, now, def);
                }
                case "opacity" -> {
                    // opacity transition handled via keyframes
                }
            }
        }
    }

    /**
     * Starts (or restarts) a keyframe animation on a widget.
     *
     * @param widget    the widget (used as map key)
     * @param keyframes the parsed keyframe set to play
     * @param def       animation parameters (duration, easing, iterations, alternate)
     */
    public static void startKeyframeAnimation(Object widget,
                                               TesseraKeyframes keyframes,
                                               TesseraAnimationDef def) {
        List<KeyframeState> list = keyframeAnims.computeIfAbsent(widget, k -> new ArrayList<>());
        // Remove any animation with the same name
        list.removeIf(s -> s.keyframes.name().equals(keyframes.name()));
        list.add(new KeyframeState(keyframes, def, System.nanoTime()));
    }

    /**
     * Stops all animations on a widget (transitions and keyframe animations).
     */
    public static void clearAnimations(Object widget) {
        transitions.remove(widget);
        keyframeAnims.remove(widget);
    }

    /**
     * Returns interpolated values for the current frame.
     * Call once per render pass; the engine derives time from {@link System#nanoTime()}.
     */
    public static TesseraAnimatedValues getValues(Object widget) {
        long now = System.nanoTime();

        int  bg     = 0;
        int  color  = 0;
        float opacity = -1f;
        int  border = 0;

        // ── Transitions ──────────────────────────────────────────────────────
        List<TransitionState> trList = transitions.get(widget);
        if (trList != null) {
            trList.removeIf(TransitionState::isExpired);
            for (TransitionState ts : trList) {
                float t = ts.progress(now);
                switch (ts.property) {
                    case "background", "background-color" -> bg     = lerpColor(ts.from, ts.to, t);
                    case "color"                          -> color  = lerpColor(ts.from, ts.to, t);
                    case "border-color"                   -> border = lerpColor(ts.from, ts.to, t);
                }
            }
        }

        // ── Keyframe animations ───────────────────────────────────────────────
        List<KeyframeState> kfList = keyframeAnims.get(widget);
        if (kfList != null) {
            kfList.removeIf(KeyframeState::isExpired);
            for (KeyframeState ks : kfList) {
                float progress = ks.progress(now);
                TesseraKeyframes.Stop[] bracket = ks.keyframes.bracket(progress);
                if (bracket == null) continue;

                TesseraKeyframes.Stop from = bracket[0], to = bracket[1];
                float span = to.progress() - from.progress();
                float localT = span <= 0 ? 1f : (progress - from.progress()) / span;
                localT = ks.def.easing().apply(localT);

                // Interpolate each property present in either stop
                TesseraStyle fs = from.style(), ts2 = to.style();

                if (fs.background != TesseraStyle.UNSET || ts2.background != TesseraStyle.UNSET) {
                    int f = fs.background != TesseraStyle.UNSET ? fs.background : (bg != 0 ? bg : 0);
                    int t2 = ts2.background != TesseraStyle.UNSET ? ts2.background : f;
                    bg = lerpColor(f, t2, localT);
                }
                if (fs.color != TesseraStyle.UNSET || ts2.color != TesseraStyle.UNSET) {
                    int f = fs.color != TesseraStyle.UNSET ? fs.color : (color != 0 ? color : 0);
                    int t2 = ts2.color != TesseraStyle.UNSET ? ts2.color : f;
                    color = lerpColor(f, t2, localT);
                }
                if (fs.opacity != TesseraStyle.UNSET_F || ts2.opacity != TesseraStyle.UNSET_F) {
                    float f = fs.opacity != TesseraStyle.UNSET_F ? fs.opacity : (opacity >= 0 ? opacity : 1f);
                    float t2 = ts2.opacity != TesseraStyle.UNSET_F ? ts2.opacity : f;
                    opacity = f + (t2 - f) * localT;
                }
                if (fs.borderColor != TesseraStyle.UNSET || ts2.borderColor != TesseraStyle.UNSET) {
                    int f = fs.borderColor != TesseraStyle.UNSET ? fs.borderColor : (border != 0 ? border : 0);
                    int t2 = ts2.borderColor != TesseraStyle.UNSET ? ts2.borderColor : f;
                    border = lerpColor(f, t2, localT);
                }
            }
        }

        if (bg == 0 && color == 0 && opacity < 0 && border == 0) return TesseraAnimatedValues.NONE;
        return new TesseraAnimatedValues(bg, color, opacity, border);
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private static void replaceTransition(List<TransitionState> list, String prop,
                                           int from, int to, long startNs,
                                           TesseraTransitionDef def) {
        list.removeIf(s -> s.property.equals(prop));
        list.add(new TransitionState(prop, from, to, startNs, def));
    }

    /** Returns {@code value} if it is not UNSET (0 or MIN_VALUE), otherwise {@code fallback}. */
    private static int valueOr(int value, int fallback) {
        return (value != 0 && value != TesseraStyle.UNSET) ? value : fallback;
    }

    private static String normalise(String p) {
        return p == null ? "" : p.trim().toLowerCase();
    }

    /** Component-wise linear interpolation of two packed ARGB colors. */
    public static int lerpColor(int from, int to, float t) {
        if (t <= 0f) return from;
        if (t >= 1f) return to;
        int fa = (from >>> 24) & 0xFF; if (fa == 0) fa = 0xFF;
        int fr = (from >> 16) & 0xFF;
        int fg = (from >>  8) & 0xFF;
        int fb =  from        & 0xFF;
        int ta = (to   >>> 24) & 0xFF; if (ta == 0) ta = 0xFF;
        int tr = (to   >> 16) & 0xFF;
        int tg = (to   >>  8) & 0xFF;
        int tb =  to          & 0xFF;
        int a = fa + (int)((ta - fa) * t);
        int r = fr + (int)((tr - fr) * t);
        int g = fg + (int)((tg - fg) * t);
        int b = fb + (int)((tb - fb) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ── Inner state classes ────────────────────────────────────────────────────

    private static final class TransitionState {
        final String property;
        final int from, to;
        final long startNs;
        final TesseraTransitionDef def;

        TransitionState(String property, int from, int to, long startNs, TesseraTransitionDef def) {
            this.property = property;
            this.from     = from;
            this.to       = to;
            this.startNs  = startNs + (long) def.delayMs() * 1_000_000L;
            this.def      = def;
        }

        float progress(long nowNs) {
            long elapsed = nowNs - startNs;
            if (elapsed < 0) return 0f;
            long durationNs = (long) def.durationMs() * 1_000_000L;
            if (durationNs <= 0) return 1f;
            return def.easing().apply(Math.min(1f, (float) elapsed / durationNs));
        }

        boolean isExpired() { return false; } // transitions stay until replaced
    }

    private static final class KeyframeState {
        final TesseraKeyframes  keyframes;
        final TesseraAnimationDef def;
        final long startNs;

        KeyframeState(TesseraKeyframes keyframes, TesseraAnimationDef def, long startNs) {
            this.keyframes = keyframes;
            this.def       = def;
            this.startNs   = startNs + (long) def.delayMs() * 1_000_000L;
        }

        float progress(long nowNs) {
            long elapsed = nowNs - startNs;
            if (elapsed < 0) return 0f;
            long durationNs = (long) def.durationMs() * 1_000_000L;
            if (durationNs <= 0) return 1f;

            int maxIter = def.iterationCount(); // -1 = infinite
            float raw = (float) elapsed / durationNs;

            if (maxIter > 0 && raw >= maxIter) return def.alternate() && (maxIter % 2 == 0) ? 0f : 1f;

            float iterProgress = raw % 1f;
            // Alternate: odd iterations run reversed
            long iterIndex = (long) raw;
            if (def.alternate() && (iterIndex % 2 == 1)) iterProgress = 1f - iterProgress;
            return iterProgress;
        }

        boolean isExpired() {
            if (def.iterationCount() < 0) return false; // infinite
            long elapsed = System.nanoTime() - startNs;
            long total   = (long) def.durationMs() * 1_000_000L * def.iterationCount();
            return elapsed > total;
        }
    }
}
