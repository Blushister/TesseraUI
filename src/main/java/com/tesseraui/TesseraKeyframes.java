package com.tesseraui;

import java.util.List;

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
