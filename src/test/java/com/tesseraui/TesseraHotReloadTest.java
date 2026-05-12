package com.tesseraui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
