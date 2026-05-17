package com.tesseraui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TesseraTemplateRendererStateTest {

    @AfterEach
    void clearGlobals() {
        TesseraTemplate.clearGlobalStylesheets();
    }

    @Test
    void build_withRenderContext_persistsInputStateById() {
        TesseraTemplate template = TesseraTemplate.fromString(
                "<col><input id=\"name\" value=\"{{ initial }}\" /></col>",
                "input { width: 80; height: 14; }");
        TesseraModel model = TesseraModel.of(Map.of("initial", "first"));
        TesseraRenderContext context = new TesseraRenderContext();

        TesseraPanel first = TesseraTemplateRenderer.build(
                template, model, Map.of(), Map.of(), context, 0, 0, 120, 40);
        TesseraInput firstInput = (TesseraInput) first.debugChildren().get(0);
        assertEquals("first", firstInput.getText());

        context.inputState("name").text = "edited";

        TesseraPanel second = TesseraTemplateRenderer.build(
                template, model, Map.of(), Map.of(), context, 0, 0, 120, 40);
        TesseraInput secondInput = (TesseraInput) second.debugChildren().get(0);
        assertEquals("edited", secondInput.getText());
        assertSame(context.inputState("name"), secondInput.state());
    }

    @Test
    void build_inputReadsSuggestionsAttribute() {
        TesseraTemplate template = TesseraTemplate.fromString(
                "<col><input id=\"name\" suggestions=\"Alfa, Beta, Gamma\" /></col>",
                "input { width: 80; height: 14; }");

        TesseraPanel root = TesseraTemplateRenderer.build(
                template, TesseraModel.EMPTY, Map.of(), Map.of(), 0, 0, 120, 40);
        TesseraInput input = (TesseraInput) root.debugChildren().get(0);

        assertEquals(java.util.List.of("Alfa", "Beta", "Gamma"), input.suggestions());
        assertTrue(input.autocomplete());
    }

    @Test
    void build_inputReadsAutocompleteListAttribute() {
        TesseraTemplate template = TesseraTemplate.fromString(
                "<col><input id=\"name\" autocomplete=\"{{ names }}\" /></col>",
                "input { width: 80; height: 14; }");
        TesseraModel model = TesseraModel.of(Map.of("names", "Alfa|Beta|Gamma"));

        TesseraPanel root = TesseraTemplateRenderer.build(
                template, model, Map.of(), Map.of(), 0, 0, 120, 40);
        TesseraInput input = (TesseraInput) root.debugChildren().get(0);

        assertEquals(java.util.List.of("Alfa", "Beta", "Gamma"), input.suggestions());
        assertTrue(input.autocomplete());
    }

    @Test
    void build_inputAutocompleteOffDisablesConfiguredSuggestions() {
        TesseraTemplate template = TesseraTemplate.fromString(
                "<col><input id=\"name\" suggestions=\"Alfa,Beta\" autocomplete=\"off\" /></col>",
                "input { width: 80; height: 14; }");

        TesseraPanel root = TesseraTemplateRenderer.build(
                template, TesseraModel.EMPTY, Map.of(), Map.of(), 0, 0, 120, 40);
        TesseraInput input = (TesseraInput) root.debugChildren().get(0);

        assertEquals(java.util.List.of("Alfa", "Beta"), input.suggestions());
        assertFalse(input.autocomplete());
    }

    @Test
    void renderContext_clearInput_removesOneState() {
        TesseraRenderContext context = new TesseraRenderContext();
        context.inputState("row.1.name").text = "Alice";
        context.inputState("row.2.name").text = "Bob";

        assertTrue(context.clearInput("row.1.name"));
        assertFalse(context.clearInput("row.1.name"));
        assertFalse(context.inputStates().containsKey("row.1.name"));
        assertEquals("Bob", context.inputState("row.2.name").text);
    }

    @Test
    void renderContext_clearInputsWithPrefix_removesDynamicListStates() {
        TesseraRenderContext context = new TesseraRenderContext();
        context.inputState("rows.1.name").text = "Alice";
        context.inputState("rows.1.count").text = "3";
        context.inputState("rows.2.name").text = "Bob";
        context.inputState("dialog.title").text = "Edit";

        assertEquals(2, context.clearInputsWithPrefix("rows.1."));
        assertFalse(context.inputStates().containsKey("rows.1.name"));
        assertFalse(context.inputStates().containsKey("rows.1.count"));
        assertTrue(context.inputStates().containsKey("rows.2.name"));
        assertTrue(context.inputStates().containsKey("dialog.title"));
    }

    @Test
    void renderContext_clearInputsMatching_removesPredicateMatches() {
        TesseraRenderContext context = new TesseraRenderContext();
        context.inputState("rows.1.name").text = "Alice";
        context.inputState("rows.1.count").text = "3";
        context.inputState("rows.2.name").text = "Bob";

        assertEquals(2, context.clearInputsMatching(id -> id.endsWith(".name")));
        assertEquals(1, context.inputStates().size());
        assertTrue(context.inputStates().containsKey("rows.1.count"));
    }

    @Test
    void renderContext_setInputText_updatesPersistedTextAndCaret() {
        TesseraRenderContext context = new TesseraRenderContext();
        TesseraInputState state = context.setInputText("zone.dimension", "arcadia_dungeon:dungeon");

        assertEquals("arcadia_dungeon:dungeon", state.text);
        assertEquals(state.text.length(), state.cursor);
        assertEquals(state.cursor, state.selStart);
        assertEquals(0, state.scrollX);
        assertSame(state, context.inputState("zone.dimension"));
    }

    @Test
    void globalStylesheet_appliesBeforeLocalCss() {
        TesseraTemplate.addGlobalStylesheet(".box { color: #112233; background: #010203; }");
        TesseraTemplate template = TesseraTemplate.fromString(
                "<label class=\"box\">Hi</label>",
                ".box { color: #445566; }");

        TesseraStyle style = template.styleSheet()
                .resolve(new TesseraNode("label", Map.of("class", "box"), java.util.List.of(), ""),
                        new java.util.ArrayDeque<>());

        assertEquals(0xFF445566, style.color, "local CSS must override equal-specificity global CSS");
        assertEquals(0xFF010203, style.background, "global CSS should still provide properties not set locally");
    }

    @Test
    void fromString_multipleCssFragments_applyInOrder() {
        TesseraTemplate template = TesseraTemplate.fromString(
                "<label class=\"box\">Hi</label>",
                ".box { color: #111111; }",
                ".box { color: #222222; }");

        TesseraStyle style = template.styleSheet()
                .resolve(new TesseraNode("label", Map.of("class", "box"), java.util.List.of(), ""),
                        new java.util.ArrayDeque<>());

        assertEquals(0xFF222222, style.color);
    }

    @Test
    void build_tooltipI18nOverridesStaticTooltip() {
        var previous = TesseraI18n.TRANSLATOR;
        try {
            TesseraI18n.TRANSLATOR = key -> "translated:" + key;
            TesseraTemplate template = TesseraTemplate.fromString(
                    "<col><label tooltip=\"plain\" tooltip-i18n=\"ui.tip\">Hover</label></col>",
                    "label { width: 80; height: 10; }");

            TesseraPanel root = TesseraTemplateRenderer.build(
                    template, TesseraModel.EMPTY, Map.of(), Map.of(), 0, 0, 120, 40);
            TesseraLabel label = (TesseraLabel) root.debugChildren().get(0);

            assertEquals("translated:ui.tip", label.getTooltip());
        } finally {
            TesseraI18n.TRANSLATOR = previous;
        }
    }

    @Test
    void build_hrRendersAsOnePixelSeparatorByDefault() {
        TesseraTemplate template = TesseraTemplate.fromString(
                "<col><hr/></col>",
                "hr { width: 90; }");

        TesseraPanel root = TesseraTemplateRenderer.build(
                template, TesseraModel.EMPTY, Map.of(), Map.of(), 0, 0, 120, 40);
        TesseraPanel separator = (TesseraPanel) root.debugChildren().get(0);

        assertEquals(1, separator.getHeight());
    }

    @Test
    void build_nullTemplate_returnsReadableErrorPanel() throws Exception {
        TesseraPanel root = TesseraTemplateRenderer.build(
                null, TesseraModel.EMPTY, Map.of(), Map.of(), 0, 0, 120, 40);

        assertEquals("Tessera template is null", labelText((TesseraLabel) root.debugChildren().get(1)));
    }

    @Test
    void build_nullTemplateRoot_returnsReadableErrorPanel() throws Exception {
        TesseraTemplate template = TesseraTemplate.fromString("");

        TesseraPanel root = TesseraTemplateRenderer.build(
                template, TesseraModel.EMPTY, Map.of(), Map.of(), 0, 0, 120, 40);

        assertEquals("Tessera template root is null", labelText((TesseraLabel) root.debugChildren().get(1)));
    }

    @Test
    void virtualList_acceptsLoopVariablePrefixedBindingsInRows() throws Exception {
        TesseraTemplate template = TesseraTemplate.fromString(
                """
                <col>
                  <virtual-list v-for="row in rows" row-height="12">
                    <row>
                      <label>{{ row.name }}</label>
                    </row>
                  </virtual-list>
                </col>
                """,
                "virtual-list { width: 100; height: 20; } label { width: 80; height: 10; }");
        TesseraModel model = TesseraModel.of(Map.of(
                "rows", "1",
                "row.name.0", "First row"
        ));

        TesseraPanel root = TesseraTemplateRenderer.build(
                template, model, Map.of(), Map.of(), 0, 0, 120, 40);
        TesseraVirtualList list = (TesseraVirtualList) root.debugChildren().get(0);

        var getRow = TesseraVirtualList.class.getDeclaredMethod("getRow", int.class);
        getRow.setAccessible(true);
        TesseraPanel row = (TesseraPanel) getRow.invoke(list, 0);
        TesseraPanel inner = (TesseraPanel) row.debugChildren().get(0);
        TesseraLabel label = (TesseraLabel) inner.debugChildren().get(0);

        var textField = TesseraLabel.class.getDeclaredField("text");
        textField.setAccessible(true);
        assertEquals("First row", textField.get(label));
    }

    @Test
    void virtualList_rowBindingsFallbackToParentModelForGlobalKeys() throws Exception {
        TesseraTemplate template = TesseraTemplate.fromString(
                """
                <col>
                  <virtual-list v-for="row in rows" row-height="12">
                    <row>
                      <label>{{ s.shared }}</label>
                    </row>
                  </virtual-list>
                </col>
                """,
                "virtual-list { width: 100; height: 20; } label { width: 80; height: 10; }");
        TesseraModel model = TesseraModel.of(Map.of(
                "rows", "1",
                "s.shared", "Shared value"
        ));

        TesseraPanel root = TesseraTemplateRenderer.build(
                template, model, Map.of(), Map.of(), 0, 0, 120, 40);
        TesseraVirtualList list = (TesseraVirtualList) root.debugChildren().get(0);

        var getRow = TesseraVirtualList.class.getDeclaredMethod("getRow", int.class);
        getRow.setAccessible(true);
        TesseraPanel row = (TesseraPanel) getRow.invoke(list, 0);
        TesseraPanel inner = (TesseraPanel) row.debugChildren().get(0);
        TesseraLabel label = (TesseraLabel) inner.debugChildren().get(0);

        assertEquals("Shared value", labelText(label));
    }

    private static String labelText(TesseraLabel label) throws Exception {
        var textField = TesseraLabel.class.getDeclaredField("text");
        textField.setAccessible(true);
        return (String) textField.get(label);
    }
}
