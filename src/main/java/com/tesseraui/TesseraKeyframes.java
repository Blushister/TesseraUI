package com.tesseraui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Parsed {@code @keyframes} block.
 *
 * <p>Each stop maps a progress value (0.0 – 1.0) to a partial {@link TesseraStyle}
 * whose non-UNSET properties are animated.</p>
 */
public final class TesseraKeyframes {

    /** A single keyframe stop (e.g. {@code 50% { opacity: 0.5; }}). */
    public record Stop(
            /** Progress in [0, 1] (0 = from / 1 = to). */
            float progress,
            /** Partial style — only set properties are interpolated. */
            TesseraStyle style
    ) {}

    private final String name;
    private final List<Stop> stops;

    public TesseraKeyframes(String name, List<Stop> stops) {
        this.name  = name;
        // Ensure stops are sorted by progress
        this.stops = stops.stream()
                .sorted(java.util.Comparator.comparingDouble(Stop::progress))
                .toList();
    }

    public String     name()  { return name; }
    public List<Stop> stops() { return stops; }

    // ── Fluent builder ────────────────────────────────────────────────────────

    /**
     * Creates a fluent builder for a named {@code @keyframes} block.
     *
     * <pre>{@code
     * TesseraKeyframes kf = TesseraKeyframes.builder("pulse")
     *     .from(s -> { s.background = 0xFF1a2a1a; s.borderColor = 0xFF22c55e; })
     *     .at(50, s -> { s.background = 0xFF14532d; s.borderColor = 0xFF4ade80; })
     *     .to(s ->  { s.background = 0xFF1a2a1a; s.borderColor = 0xFF22c55e; })
     *     .build();
     * }</pre>
     */
    public static Builder builder(String name) { return new Builder(name); }

    public static final class Builder {
        private final String     name;
        private final List<Stop> stops = new ArrayList<>();

        private Builder(String name) { this.name = name; }

        /** Adds a stop at the given progress (0.0 – 1.0). */
        public Builder stop(float progress, Consumer<TesseraStyle> styler) {
            TesseraStyle s = new TesseraStyle();
            styler.accept(s);
            stops.add(new Stop(progress, s));
            return this;
        }

        /** Adds the {@code from} stop (progress = 0). */
        public Builder from(Consumer<TesseraStyle> styler) { return stop(0f, styler); }

        /** Adds the {@code to} stop (progress = 1). */
        public Builder to(Consumer<TesseraStyle> styler)   { return stop(1f, styler); }

        /** Adds a percentage stop, e.g. {@code at(50, s -> ...)} for 50%. */
        public Builder at(int percent, Consumer<TesseraStyle> styler) {
            return stop(percent / 100f, styler);
        }

        public TesseraKeyframes build() { return new TesseraKeyframes(name, stops); }
    }

    /**
     * Returns the two stops that bracket {@code progress}, for interpolation.
     * Returns {@code null} if the keyframe list is empty.
     */
    public Stop[] bracket(float progress) {
        if (stops.isEmpty()) return null;
        Stop from = stops.get(0), to = stops.get(stops.size() - 1);
        for (int i = 0; i < stops.size() - 1; i++) {
            if (progress >= stops.get(i).progress() && progress <= stops.get(i + 1).progress()) {
                from = stops.get(i);
                to   = stops.get(i + 1);
                break;
            }
        }
        return new Stop[]{ from, to };
    }
}
