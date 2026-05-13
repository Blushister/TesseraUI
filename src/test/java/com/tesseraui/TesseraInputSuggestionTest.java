package com.tesseraui;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TesseraInputSuggestionTest {

    @Test
    void keyNavigation_acceptsSelectedSuggestion() {
        List<String> changes = new ArrayList<>();
        TesseraInput input = new TesseraInput(0, 0, 80, 14)
                .suggestions(List.of("Alfa", "Beta", "Gamma"))
                .onChange(changes::add);

        input.setFocused(true);
        input.charTyped('b', 0);
        input.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
        input.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);

        assertEquals("Beta", input.getText());
        assertEquals(List.of("b", "Beta"), changes);
    }

    @Test
    void autocompleteOff_disablesSuggestionAcceptance() {
        List<String> changes = new ArrayList<>();
        TesseraInput input = new TesseraInput(0, 0, 80, 14)
                .suggestions(List.of("Alfa", "Beta", "Gamma"))
                .autocomplete(false)
                .onChange(changes::add);

        input.setFocused(true);
        input.charTyped('b', 0);
        input.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
        input.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);

        assertEquals("b", input.getText());
        assertEquals(List.of("b"), changes);
    }

    @Test
    void tabWithoutSelectedSuggestion_doesNotConsumeFocusTraversal() {
        TesseraInput input = new TesseraInput(0, 0, 80, 14)
                .suggestions(List.of("Alfa", "Beta", "Gamma"));

        input.setFocused(true);

        assertFalse(input.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0));
        assertEquals("", input.getText());
    }
}
