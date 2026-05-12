package com.tesseraui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TesseraHotReload#resolveHtml} path validation.
 * Uses reflection to set {@code resourcesRoot} without triggering the
 * NeoForge-dependent {@code activate()} path.
 */
class TesseraHotReloadTest {

    private static final Field RESOURCES_ROOT_FIELD;

    static {
        try {
            RESOURCES_ROOT_FIELD = TesseraHotReload.class.getDeclaredField("resourcesRoot");
            RESOURCES_ROOT_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Path tempRoot;
    private Object previousRoot;

    @BeforeEach
    void setUp() throws Exception {
        tempRoot     = Files.createTempDirectory("tessera-hr-test");
        previousRoot = RESOURCES_ROOT_FIELD.get(null);
        Files.createDirectories(tempRoot.resolve("assets").resolve("mymod").resolve("ui"));
        RESOURCES_ROOT_FIELD.set(null, tempRoot.toAbsolutePath().normalize());
    }

    @AfterEach
    void tearDown() throws Exception {
        TesseraHotReload.invalidateAll();
        RESOURCES_ROOT_FIELD.set(null, previousRoot);
    }

    @Test
    void resolveHtml_validId_resolvedUnderAssets() {
        Path result = TesseraHotReload.resolveHtml("mymod:ui/menu");
        assertTrue(result.startsWith(tempRoot.resolve("assets")),
                "result must be under assets/");
        assertTrue(result.toString().endsWith(".html"),
                "result must have .html extension");
    }

    @Test
    void resolveHtml_pathTraversal_throwsSecurityException() {
        assertThrows(SecurityException.class,
                () -> TesseraHotReload.resolveHtml("mymod:../../etc/passwd"),
                "path traversal must throw SecurityException");
    }

    @Test
    void resolveCss_validId_endsWithCss() {
        Path result = TesseraHotReload.resolveCss("mymod:ui/menu");
        assertTrue(result.toString().endsWith(".css"));
    }

    @Test
    void load_linkStylesheetFromHtml_appliesBeforeCompanionCss() throws Exception {
        Path ui = tempRoot.resolve("assets").resolve("mymod").resolve("ui");
        Files.writeString(ui.resolve("shared.css"), ".box { color: #112233; background: #010203; }");
        Files.writeString(ui.resolve("menu.css"), ".box { color: #445566; }");
        Files.writeString(ui.resolve("menu.html"),
                "<col><link rel=\"stylesheet\" href=\"mymod:ui/shared.css\"/><label class=\"box\">Hi</label></col>");

        TesseraTemplate template = TesseraTemplate.load("mymod:ui/menu");
        TesseraNode label = template.root().children().stream()
                .filter(n -> "label".equals(n.tag()))
                .findFirst()
                .orElseThrow();
        TesseraStyle style = template.styleSheet().resolve(label, new ArrayDeque<>());

        assertEquals(0xFF445566, style.color, "companion CSS should override linked CSS");
        assertEquals(0xFF010203, style.background, "linked CSS should provide shared properties");
    }

    @Test
    void load_relativeLinkStylesheet_resolvesBesideTemplate() throws Exception {
        Path ui = tempRoot.resolve("assets").resolve("mymod").resolve("ui");
        Files.writeString(ui.resolve("shared.css"), ".box { color: #112233; }");
        Files.writeString(ui.resolve("menu.html"),
                "<col><link rel=\"stylesheet\" href=\"shared.css\"/><label class=\"box\">Hi</label></col>");

        TesseraTemplate template = TesseraTemplate.load("mymod:ui/menu");
        TesseraNode label = new TesseraNode("label", Map.of("class", "box"), java.util.List.of(), "");
        TesseraStyle style = template.styleSheet().resolve(label, new ArrayDeque<>());

        assertEquals(0xFF112233, style.color);
    }
}
