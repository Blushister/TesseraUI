package com.tesseraui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TesseraKeyframes}: fluent builder and {@code bracket()} interpolation helper.
 */
class TesseraKeyframesTest {

    // ── builder ───────────────────────────────────────────────────────────────

    @Test
    void builder_name_preserved() {
        TesseraKeyframes kf = TesseraKeyframes.builder("pulse").build();
        assertEquals("pulse", kf.name());
    }

    @Test
    void builder_from_and_to_produceTwoStops() {
        TesseraKeyframes kf = TesseraKeyframes.builder("fade")
                .from(s -> s.background = 0xFF001122)
                .to(s ->   s.background = 0xFF334455)
                .build();
        assertEquals(2, kf.stops().size());
        assertEquals(0f, kf.stops().get(0).progress(), 0.001f);
        assertEquals(1f, kf.stops().get(1).progress(), 0.001f);
    }

    @Test
    void builder_from_setsBackground() {
        TesseraKeyframes kf = TesseraKeyframes.builder("b")
                .from(s -> s.background = 0xFF001122)
                .to(s ->   s.background = 0xFF334455)
                .build();
        assertEquals(0xFF001122, kf.stops().get(0).style().background);
    }

    @Test
    void builder_to_setsBackground() {
        TesseraKeyframes kf = TesseraKeyframes.builder("b")
                .from(s -> s.background = 0xFF001122)
                .to(s ->   s.background = 0xFF334455)
                .build();
        assertEquals(0xFF334455, kf.stops().get(1).style().background);
    }

    @Test
    void builder_at_convertsPercentToProgress() {
        TesseraKeyframes kf = TesseraKeyframes.builder("pulse")
                .from(s -> s.background = 0xFF000000)
                .at(50, s -> s.background = 0xFF808080)
                .to(s ->   s.background = 0xFFFFFFFF)
                .build();
        assertEquals(3, kf.stops().size());
        assertEquals(0.5f, kf.stops().get(1).progress(), 0.001f);
        assertEquals(0xFF808080, kf.stops().get(1).style().background);
    }

    @Test
    void builder_stopsAreSortedByProgress_evenIfAddedOutOfOrder() {
        TesseraKeyframes kf = TesseraKeyframes.builder("sorted")
                .to(s ->    s.background = 0xFF999999)  // progress=1
                .from(s ->  s.background = 0xFF111111)  // progress=0
                .at(50, s -> s.background = 0xFF555555) // progress=0.5
                .build();
        assertEquals(0f,   kf.stops().get(0).progress(), 0.001f);
        assertEquals(0.5f, kf.stops().get(1).progress(), 0.001f);
        assertEquals(1f,   kf.stops().get(2).progress(), 0.001f);
    }

    @Test
    void builder_stop_rawProgress() {
        TesseraKeyframes kf = TesseraKeyframes.builder("b")
                .stop(0.25f, s -> s.background = 0xFF112233)
                .stop(0.75f, s -> s.background = 0xFF445566)
                .build();
        assertEquals(0.25f, kf.stops().get(0).progress(), 0.001f);
        assertEquals(0.75f, kf.stops().get(1).progress(), 0.001f);
    }

    @Test
    void builder_multipleProperties_singleStop() {
        TesseraKeyframes kf = TesseraKeyframes.builder("multi")
                .from(s -> { s.background = 0xFF001122; s.borderColor = 0xFF334455; s.opacity = 0f; })
                .to(s ->   { s.background = 0xFFFFFFFF; s.borderColor = 0xFF000000; s.opacity = 1f; })
                .build();
        TesseraKeyframes.Stop from = kf.stops().get(0);
        assertEquals(0xFF001122, from.style().background);
        assertEquals(0xFF334455, from.style().borderColor);
        assertEquals(0f, from.style().opacity, 0.001f);
    }

    // ── bracket ───────────────────────────────────────────────────────────────

    @Test
    void bracket_emptyStops_returnsNull() {
        TesseraKeyframes kf = new TesseraKeyframes("empty", List.of());
        assertNull(kf.bracket(0.5f));
    }

    @Test
    void bracket_atExactStart_returnsFirstPair() {
        TesseraKeyframes kf = TesseraKeyframes.builder("b")
                .from(s -> s.background = 0xFF001122)
                .at(50, s -> s.background = 0xFF223344)
                .to(s ->   s.background = 0xFF334455)
                .build();
        TesseraKeyframes.Stop[] pair = kf.bracket(0f);
        assertNotNull(pair);
        assertEquals(0f,   pair[0].progress(), 0.001f);
        assertEquals(0.5f, pair[1].progress(), 0.001f);
    }

    @Test
    void bracket_atExactEnd_returnsLastPair() {
        TesseraKeyframes kf = TesseraKeyframes.builder("b")
                .from(s -> s.background = 0xFF001122)
                .to(s ->   s.background = 0xFF334455)
                .build();
        TesseraKeyframes.Stop[] pair = kf.bracket(1f);
        assertNotNull(pair);
        assertEquals(0f, pair[0].progress(), 0.001f);
        assertEquals(1f, pair[1].progress(), 0.001f);
    }

    @Test
    void bracket_midpoint_returnsCorrectPair() {
        TesseraKeyframes kf = TesseraKeyframes.builder("mid")
                .from(s -> s.background = 0xFF000000)
                .at(50, s -> s.background = 0xFF888888)
                .to(s ->   s.background = 0xFFFFFFFF)
                .build();
        // 0.75 is in [0.5, 1.0]
        TesseraKeyframes.Stop[] pair = kf.bracket(0.75f);
        assertNotNull(pair);
        assertEquals(0.5f, pair[0].progress(), 0.001f);
        assertEquals(1f,   pair[1].progress(), 0.001f);
    }

    @Test
    void bracket_singleStop_returnsFirstAndFirst() {
        TesseraKeyframes kf = new TesseraKeyframes("single",
                List.of(new TesseraKeyframes.Stop(0.5f, new TesseraStyle())));
        TesseraKeyframes.Stop[] pair = kf.bracket(0.5f);
        assertNotNull(pair);
        assertSame(pair[0], pair[1], "single-stop bracket returns same stop for both sides");
    }

    @Test
    void bracket_resultHasTwoElements() {
        TesseraKeyframes kf = TesseraKeyframes.builder("b")
                .from(s -> s.background = 0xFF000000)
                .to(s ->   s.background = 0xFFFFFFFF)
                .build();
        TesseraKeyframes.Stop[] pair = kf.bracket(0.3f);
        assertNotNull(pair);
        assertEquals(2, pair.length);
    }
}
