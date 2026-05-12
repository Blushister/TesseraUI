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
}
