package com.tesseraui;

public enum TesseraEasing {
    LINEAR       (0.00f, 0.00f, 1.00f, 1.00f),
    EASE         (0.25f, 0.10f, 0.25f, 1.00f),
    EASE_IN      (0.42f, 0.00f, 1.00f, 1.00f),
    EASE_OUT     (0.00f, 0.00f, 0.58f, 1.00f),
    EASE_IN_OUT  (0.42f, 0.00f, 0.58f, 1.00f);

    private final float p1x, p1y, p2x, p2y;

    TesseraEasing(float p1x, float p1y, float p2x, float p2y) {
        this.p1x = p1x; this.p1y = p1y;
        this.p2x = p2x; this.p2y = p2y;
    }

    /** Maps linear progress t∈[0,1] through this easing curve. */
    public float apply(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        return (float) sampleCurveY(solveCurveX(t, 1e-6f));
    }

    public static TesseraEasing parse(String s) {
        if (s == null) return EASE;
        return switch (s.trim().toLowerCase()) {
            case "linear"      -> LINEAR;
            case "ease-in"     -> EASE_IN;
            case "ease-out"    -> EASE_OUT;
            case "ease-in-out" -> EASE_IN_OUT;
            default            -> EASE;
        };
    }

    // ── cubic-bezier internals ──────────────────────────────────────────────

    private double cx() { return 3 * p1x; }
    private double bx() { return 3 * (p2x - p1x) - cx(); }
    private double ax() { return 1 - cx() - bx(); }

    private double cy() { return 3 * p1y; }
    private double by() { return 3 * (p2y - p1y) - cy(); }
    private double ay() { return 1 - cy() - by(); }

    private double sampleCurveX(double t) { return ((ax() * t + bx()) * t + cx()) * t; }
    private double sampleCurveY(double t) { return ((ay() * t + by()) * t + cy()) * t; }
    private double sampleCurveDerivativeX(double t) { return (3 * ax() * t + 2 * bx()) * t + cx(); }

    private double solveCurveX(double x, double eps) {
        // Newton-Raphson
        double t = x;
        for (int i = 0; i < 8; i++) {
            double ex = sampleCurveX(t) - x;
            if (Math.abs(ex) < eps) return t;
            double d = sampleCurveDerivativeX(t);
            if (Math.abs(d) < 1e-6) break;
            t -= ex / d;
        }
        // Bisection fallback
        double lo = 0, hi = 1;
        t = x;
        if (t < lo) return lo;
        if (t > hi) return hi;
        while (lo < hi) {
            double mid = (lo + hi) / 2;
            double v = sampleCurveX(mid);
            if (Math.abs(v - x) < eps) return mid;
            if (v < x) lo = mid; else hi = mid;
        }
        return t;
    }
}
