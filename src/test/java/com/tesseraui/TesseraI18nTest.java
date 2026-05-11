package com.tesseraui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TesseraI18n}.
 * No Minecraft runtime required — the TRANSLATOR field is replaced with a mock.
 */
class TesseraI18nTest {

    @BeforeEach
    void setUpTranslator() {
        // Simulate a translator that prepends "fr:" so we can detect translations
        TesseraI18n.TRANSLATOR = key -> "fr:" + key;
    }

    @AfterEach
    void resetTranslator() {
        // Simulate Minecraft returning the key unchanged (key absent)
        TesseraI18n.TRANSLATOR = key -> key;
    }

    @Test
    void translate_knownKey_returnsTranslation() {
        assertEquals("fr:ui.confirm", TesseraI18n.translate("ui.confirm"));
    }

    @Test
    void isTranslated_knownKey_returnsTrue() {
        // TRANSLATOR returns "fr:ui.confirm" which differs from "ui.confirm"
        assertTrue(TesseraI18n.isTranslated("ui.confirm"));
    }

    @Test
    void isTranslated_unknownKey_returnsFalse() {
        // After reset (in teardown), TRANSLATOR returns the key unchanged.
        // Override here explicitly to simulate an absent key.
        TesseraI18n.TRANSLATOR = key -> key;
        assertFalse(TesseraI18n.isTranslated("ui.missing.key"));
    }
}
