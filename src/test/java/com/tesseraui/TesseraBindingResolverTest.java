package com.tesseraui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TesseraBindingResolver}.
 * Pure-Java — no Minecraft runtime required.
 */
class TesseraBindingResolverTest {

    private static TesseraModel model(Map<String, String> data) {
        return TesseraModel.of(data);
    }

    // ── passthrough ──────────────────────────────────────────────────────────

    @Test
    void resolve_nullReturnsNull() {
        assertNull(TesseraBindingResolver.resolve(null, TesseraModel.EMPTY));
    }

    @Test
    void resolve_emptyReturnsEmpty() {
        assertEquals("", TesseraBindingResolver.resolve("", TesseraModel.EMPTY));
    }

    @Test
    void resolve_noBindingsPassthrough() {
        assertEquals("hello world", TesseraBindingResolver.resolve("hello world", TesseraModel.EMPTY));
    }

    // ── simple key binding ───────────────────────────────────────────────────

    @Test
    void resolve_simpleBinding() {
        TesseraModel m = model(Map.of("name", "Arcadia"));
        assertEquals("Arcadia", TesseraBindingResolver.resolve("{{name}}", m));
    }

    @Test
    void resolve_bindingWithSpaces() {
        TesseraModel m = model(Map.of("name", "Arcadia"));
        assertEquals("Arcadia", TesseraBindingResolver.resolve("{{ name }}", m));
    }

    @Test
    void resolve_missingKeyKeepsVerbatim() {
        String result = TesseraBindingResolver.resolve("{{unknown}}", TesseraModel.EMPTY);
        assertTrue(result.contains("unknown"), "Missing key should keep binding text: " + result);
    }

    @Test
    void resolve_mixedTextAndBinding() {
        TesseraModel m = model(Map.of("level", "42"));
        assertEquals("Level: 42!", TesseraBindingResolver.resolve("Level: {{level}}!", m));
    }

    @Test
    void resolve_multipleBindings() {
        TesseraModel m = model(Map.of("a", "Hello", "b", "World"));
        assertEquals("Hello World", TesseraBindingResolver.resolve("{{a}} {{b}}", m));
    }

    // ── arithmetic ───────────────────────────────────────────────────────────

    @Test
    void resolve_addition() {
        TesseraModel m = model(Map.of("x", "10", "y", "5"));
        assertEquals("15", TesseraBindingResolver.resolve("{{x + y}}", m));
    }

    @Test
    void resolve_subtraction() {
        TesseraModel m = model(Map.of("x", "10", "y", "3"));
        assertEquals("7", TesseraBindingResolver.resolve("{{x - y}}", m));
    }

    @Test
    void resolve_multiplication() {
        TesseraModel m = model(Map.of("x", "6", "y", "7"));
        assertEquals("42", TesseraBindingResolver.resolve("{{x * y}}", m));
    }

    @Test
    void resolve_division() {
        TesseraModel m = model(Map.of("x", "10", "y", "4"));
        assertEquals("2.5", TesseraBindingResolver.resolve("{{x / y}}", m));
    }

    @Test
    void resolve_divisionByZeroReturns0() {
        TesseraModel m = model(Map.of("x", "10", "y", "0"));
        assertEquals("0", TesseraBindingResolver.resolve("{{x / y}}", m));
    }

    // ── ternary / conditional ────────────────────────────────────────────────

    @Test
    void resolve_ternaryTrueKey() {
        TesseraModel m = model(Map.of("enabled", "true"));
        assertEquals("yes", TesseraBindingResolver.resolve("{{ enabled ? 'yes' : 'no' }}", m));
    }

    @Test
    void resolve_ternaryFalseKey() {
        TesseraModel m = model(Map.of("enabled", "false"));
        assertEquals("no", TesseraBindingResolver.resolve("{{ enabled ? 'yes' : 'no' }}", m));
    }

    @Test
    void resolve_ternaryMissingKeyFalse() {
        assertEquals("no", TesseraBindingResolver.resolve("{{ missing ? 'yes' : 'no' }}", TesseraModel.EMPTY));
    }

    @Test
    void resolve_ternaryGreaterThan() {
        TesseraModel m = model(Map.of("score", "80"));
        assertEquals("pass", TesseraBindingResolver.resolve("{{ score >= 50 ? 'pass' : 'fail' }}", m));
    }

    @Test
    void resolve_ternaryLessThan() {
        TesseraModel m = model(Map.of("score", "30"));
        assertEquals("fail", TesseraBindingResolver.resolve("{{ score >= 50 ? 'pass' : 'fail' }}", m));
    }

    @Test
    void resolve_ternaryEquals() {
        TesseraModel m = model(Map.of("x", "42"));
        assertEquals("yes", TesseraBindingResolver.resolve("{{ x == 42 ? 'yes' : 'no' }}", m));
    }

    @Test
    void resolve_ternaryNotEquals() {
        TesseraModel m = model(Map.of("x", "5"));
        assertEquals("yes", TesseraBindingResolver.resolve("{{ x != 42 ? 'yes' : 'no' }}", m));
    }

    // ── truthy values ────────────────────────────────────────────────────────

    @Test
    void resolve_truthyValues() {
        for (String v : new String[]{"true", "1", "yes", "on"}) {
            TesseraModel m = model(Map.of("flag", v));
            assertEquals("T", TesseraBindingResolver.resolve("{{ flag ? 'T' : 'F' }}", m),
                    "Expected truthy for value: " + v);
        }
    }

    @Test
    void resolve_falsyValues() {
        for (String v : new String[]{"false", "0", "no", "off"}) {
            TesseraModel m = model(Map.of("flag", v));
            assertEquals("F", TesseraBindingResolver.resolve("{{ flag ? 'T' : 'F' }}", m),
                    "Expected falsy for value: " + v);
        }
    }

    // ── ternary robustness (v1.8 fix) ────────────────────────────────────────

    @Test
    void resolve_ternaryMissingColon_doesNotCrash() {
        // {{ flag ? 'yes' }} — no ':' part. Must not throw, returns null/empty gracefully.
        // The fix adds a LOGGER.warn; here we just verify no exception is thrown.
        assertDoesNotThrow(() -> {
            String result = TesseraBindingResolver.resolve("{{ flag ? 'yes' }}", TesseraModel.EMPTY);
            // result may be null or the raw expression — we only care it doesn't crash
            // and doesn't return "yes" (that would be a silent wrong result)
            assertNotEquals("yes", result, "Incomplete ternary must not silently return the true branch");
        });
    }

    @Test
    void resolve_ternaryComplete_stillWorks() {
        // Regression: complete ternary must still work after the fix
        TesseraModel m = model(Map.of("x", "true"));
        assertEquals("yes", TesseraBindingResolver.resolve("{{ x ? 'yes' : 'no' }}", m));
    }

    // ── i18n / t: syntax ─────────────────────────────────────────────────────

    @AfterEach
    void resetI18nTranslator() {
        TesseraI18n.TRANSLATOR = key -> key;
    }

    @Test
    void resolve_tKey_returnsTranslation() {
        TesseraI18n.TRANSLATOR = key -> "translated:" + key;
        assertEquals("translated:ui.confirm",
                TesseraBindingResolver.resolve("{{ t:ui.confirm }}", TesseraModel.EMPTY));
    }

    @Test
    void resolve_ternary_tKey_trueVal() {
        TesseraI18n.TRANSLATOR = key -> "loc:" + key;
        TesseraModel m = model(Map.of("x", "1"));
        assertEquals("loc:ui.yes",
                TesseraBindingResolver.resolve("{{ x == 1 ? t:ui.yes : t:ui.no }}", m));
    }

    @Test
    void resolve_ternary_tKey_falseVal() {
        TesseraI18n.TRANSLATOR = key -> "loc:" + key;
        TesseraModel m = model(Map.of("x", "0"));
        assertEquals("loc:ui.no",
                TesseraBindingResolver.resolve("{{ x == 1 ? t:ui.yes : t:ui.no }}", m));
    }

    // ── evaluateCondition — bare expressions (v-if / v-show) ─────────────────

    @Test
    void evaluateCondition_bareGreaterThan_true() {
        TesseraModel m = model(Map.of("count", "5"));
        assertTrue(TesseraBindingResolver.evaluateCondition("count > 0", m));
    }

    @Test
    void evaluateCondition_bareGreaterThan_false() {
        TesseraModel m = model(Map.of("count", "0"));
        assertFalse(TesseraBindingResolver.evaluateCondition("count > 0", m));
    }

    @Test
    void evaluateCondition_bareLessThan_true() {
        TesseraModel m = model(Map.of("hp", "3"));
        assertTrue(TesseraBindingResolver.evaluateCondition("hp < 10", m));
    }

    @Test
    void evaluateCondition_truthy_trueValue() {
        TesseraModel m = model(Map.of("visible", "true"));
        assertTrue(TesseraBindingResolver.evaluateCondition("visible", m));
    }

    @Test
    void evaluateCondition_truthy_falseValue() {
        TesseraModel m = model(Map.of("visible", "false"));
        assertFalse(TesseraBindingResolver.evaluateCondition("visible", m));
    }

    // ── evaluateCondition — string equality ───────────────────────────────────

    @Test
    void evaluateCondition_stringEq_singleQuotes_matches() {
        TesseraModel m = model(Map.of("status", "ready"));
        assertTrue(TesseraBindingResolver.evaluateCondition("status == 'ready'", m));
    }

    @Test
    void evaluateCondition_stringEq_singleQuotes_noMatch() {
        TesseraModel m = model(Map.of("status", "idle"));
        assertFalse(TesseraBindingResolver.evaluateCondition("status == 'ready'", m));
    }

    @Test
    void evaluateCondition_stringEq_doubleQuotes_matches() {
        TesseraModel m = model(Map.of("mode", "dark"));
        assertTrue(TesseraBindingResolver.evaluateCondition("mode == \"dark\"", m));
    }

    @Test
    void evaluateCondition_stringNeq_matches() {
        TesseraModel m = model(Map.of("state", "active"));
        assertTrue(TesseraBindingResolver.evaluateCondition("state != 'idle'", m));
    }

    @Test
    void evaluateCondition_numericStrings_notTreatedAsString() {
        // Both sides numeric: should use numeric comparison (5 == 5 → true)
        TesseraModel m = model(Map.of("x", "5"));
        assertTrue(TesseraBindingResolver.evaluateCondition("x == 5", m));
    }

    // ── evaluateCondition — RHS from model ───────────────────────────────────

    @Test
    void evaluateCondition_twoModelKeys_equal_true() {
        TesseraModel m = model(Map.of("a", "hello", "b", "hello"));
        assertTrue(TesseraBindingResolver.evaluateCondition("a == b", m));
    }

    @Test
    void evaluateCondition_twoModelKeys_equal_false() {
        TesseraModel m = model(Map.of("a", "hello", "b", "world"));
        assertFalse(TesseraBindingResolver.evaluateCondition("a == b", m));
    }

    @Test
    void evaluateCondition_twoModelKeys_notEqual_true() {
        TesseraModel m = model(Map.of("a", "foo", "b", "bar"));
        assertTrue(TesseraBindingResolver.evaluateCondition("a != b", m));
    }

    @Test
    void evaluateCondition_twoNumericModelKeys_greaterThan_true() {
        TesseraModel m = model(Map.of("hp", "10", "maxHp", "5"));
        assertTrue(TesseraBindingResolver.evaluateCondition("hp > maxHp", m));
    }

    @Test
    void evaluateCondition_twoNumericModelKeys_greaterThan_false() {
        TesseraModel m = model(Map.of("hp", "3", "maxHp", "10"));
        assertFalse(TesseraBindingResolver.evaluateCondition("hp > maxHp", m));
    }

    // ── evaluateCondition — quote-aware operator detection ────────────────────

    @Test
    void evaluateCondition_operatorInsideQuotedLiteral_notSplit() {
        // 'a > b' is a string literal — the > must not be treated as an operator
        TesseraModel m = model(Map.of("status", "a > b"));
        assertTrue(TesseraBindingResolver.evaluateCondition("status == 'a > b'", m));
    }

    @Test
    void evaluateCondition_operatorInsideDoubleQuotedLiteral_notSplit() {
        TesseraModel m = model(Map.of("label", "x != y"));
        assertTrue(TesseraBindingResolver.evaluateCondition("label == \"x != y\"", m));
    }

    // ── evaluateCondition — missing key behaviour ─────────────────────────────

    @Test
    void evaluateCondition_missingKey_equalsZero_false() {
        // missing key resolves to null → treated as "" → "0" != "" → false (no silent 0==0 collapse)
        TesseraModel m = model(Map.of());
        assertFalse(TesseraBindingResolver.evaluateCondition("missing == 0", m));
    }

    @Test
    void evaluateCondition_missingKey_equalsEmptyString_true() {
        TesseraModel m = model(Map.of());
        assertTrue(TesseraBindingResolver.evaluateCondition("missing == ''", m));
    }
}
