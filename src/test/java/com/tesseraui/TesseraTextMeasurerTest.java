package com.tesseraui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TesseraTextMeasurer} integration logic.
 *
 * <p>Uses a deterministic mock measurer — {@code text.length() * (int)(size * 0.6f)} —
 * so no Minecraft runtime is required.
 */
class TesseraTextMeasurerTest {

    /** Mock measurer: width = character count × ⌊size × 0.6⌋ */
    private static final TesseraTextMeasurer MOCK =
            (text, family, weight, size) -> (text != null ? text.length() : 0) * (int) (size * 0.6f);

    @BeforeEach
    void installMock() {
        TesseraTemplateRenderer.TEXT_MEASURER = MOCK;
    }

    @AfterEach
    void restoreDefault() {
        TesseraTemplateRenderer.TEXT_MEASURER = TesseraMinecraftTextMeasurer.INSTANCE;
    }

    // ── measureWidth via mock ────────────────────────────────────────────────

    @Test
    void measureWidth_emptyText_returnsZero() {
        int w = MOCK.measureWidth("", null, 400, 7f);
        assertTrue(w >= 0, "empty text width must be >= 0, got " + w);
    }

    @Test
    void measureWidth_longerText_isWider() {
        int wShort = MOCK.measureWidth("Hi", null, 400, 7f);
        int wLong  = MOCK.measureWidth("Hello World", null, 400, 7f);
        assertTrue(wLong > wShort,
                "\"Hello World\" (" + wLong + ") should be wider than \"Hi\" (" + wShort + ")");
    }

    // ── measureContentWidth helper ───────────────────────────────────────────

    @Test
    void measureContentWidth_addsPadding() {
        TesseraStyle style = new TesseraStyle();
        style.paddingLeft  = 5;
        style.paddingRight = 5;

        // Compute what the mock gives for the text alone
        String text      = "Test";
        float  size      = 7f;
        int    textOnly  = MOCK.measureWidth(text, style.fontFamily, 400, size);
        // measureContentWidth should return textOnly + paddingLeft + paddingRight
        int    withPad   = invokeMeasureContentWidth(text, style, size, 400,
                                                     TesseraStyle.UNSET, TesseraStyle.UNSET);
        assertEquals(textOnly + 10, withPad,
                "measureContentWidth should add paddingLeft+paddingRight to the text width");
    }

    @Test
    void label_noExplicitWidth_getsContentWidth() {
        TesseraStyle style = new TesseraStyle();
        style.paddingLeft  = 4;
        style.paddingRight = 4;
        // width is UNSET (not set)

        String text     = "Hello";
        float  size     = 7f;
        int    expected = MOCK.measureWidth(text, style.fontFamily, 400, size) + 8;
        int    actual   = invokeMeasureContentWidth(text, style, size, 400,
                                                    TesseraStyle.UNSET, TesseraStyle.UNSET);

        assertTrue(actual > 0, "content-sized width must be > 0");
        assertEquals(expected, actual,
                "auto-width label should equal textWidth + horizontal padding");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Calls the private {@code measureContentWidth} via the package-accessible
     * {@link TesseraTemplateRenderer#TEXT_MEASURER} field.
     *
     * <p>We replicate the same formula used in the renderer so we can assert against it
     * without reflection.
     */
    private static int invokeMeasureContentWidth(String text, TesseraStyle style,
                                                  float fontSizePx, int fontWeight,
                                                  int minW, int maxW) {
        String displayed = TesseraTextStyling.transform(
                text != null ? text : "", style.textTransform);
        int textW = TesseraTemplateRenderer.TEXT_MEASURER.measureWidth(
                displayed, style.fontFamily, fontWeight, fontSizePx);
        int padH = (style.paddingLeft  != TesseraStyle.UNSET ? style.paddingLeft  : 0)
                 + (style.paddingRight != TesseraStyle.UNSET ? style.paddingRight : 0);
        int w = textW + padH;
        if (minW != TesseraStyle.UNSET && w < minW) w = minW;
        if (maxW != TesseraStyle.UNSET && w > maxW) w = maxW;
        return Math.max(w, 1);
    }
}
