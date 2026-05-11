package com.tesseraui;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A parsed HTML + CSS template ready for rendering.
 *
 * <p>Load from a Minecraft resource (namespace:path):
 * <pre>{@code
 * TesseraTemplate t = TesseraTemplate.load("mymod:ui/main_menu");
 * // Loads assets/mymod/ui/main_menu.html + main_menu.css
 * }</pre>
 *
 * <p>Or build from raw strings (useful for unit tests, no MC runtime needed):
 * <pre>{@code
 * TesseraTemplate t = TesseraTemplate.fromString("<div class=\"box\">Hello</div>", ".box { background: #1A1A2E; }");
 * }</pre>
 */
public final class TesseraTemplate {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final TesseraNode root;
    private final TesseraStyleSheet styleSheet;

    private TesseraTemplate(TesseraNode root, TesseraStyleSheet styleSheet) {
        this.root = root;
        this.styleSheet = styleSheet != null ? styleSheet : TesseraStyleSheet.EMPTY;
    }

    /**
     * Loads a template from disk or Minecraft's resource manager.
     *
     * <p>When {@link TesseraHotReload} is active the template is loaded directly from
     * the filesystem and cached; subsequent calls return the cached version until the
     * file changes (detected by the background watcher) or {@link TesseraHotReload#invalidateAll()}
     * is called (F3+T).  In production the Minecraft {@code ResourceManager} is used as before.</p>
     *
     * @param resourceId resource location in {@code "namespace:path"} form,
     *                   e.g. {@code "mymod:ui/main_menu"} maps to
     *                   {@code assets/mymod/ui/main_menu.html}
     */
    public static TesseraTemplate load(String resourceId) {
        if (TesseraHotReload.isActive()) {
            // Serve from cache if still valid
            TesseraTemplate cached = TesseraHotReload.get(resourceId);
            if (cached != null) return cached;

            // Load from filesystem
            TesseraTemplate fresh = loadFromFilesystem(resourceId);
            TesseraHotReload.put(resourceId, fresh);
            return fresh;
        }
        return loadFromResourceManager(resourceId);
    }

    // ── Filesystem loader (hot reload) ────────────────────────────────────────

    private static TesseraTemplate loadFromFilesystem(String resourceId) {
        Path htmlPath = TesseraHotReload.resolveHtml(resourceId);
        TesseraNode root;
        try {
            String html = Files.readString(htmlPath, StandardCharsets.UTF_8);
            root = TesseraHtmlParser.parse(html);
        } catch (IOException e) {
            throw new RuntimeException(
                    "[TesseraUI] Hot reload: HTML not found on disk: " + htmlPath, e);
        }

        TesseraStyleSheet sheet = TesseraStyleSheet.EMPTY;
        Path cssPath = TesseraHotReload.resolveCss(resourceId);
        if (Files.exists(cssPath)) {
            try {
                String css = Files.readString(cssPath, StandardCharsets.UTF_8);
                sheet = TesseraCssParser.parse(css);
            } catch (IOException e) {
                LOGGER.warn("[TesseraUI] Hot reload: could not read CSS {}: {}", cssPath, e.getMessage());
            }
        }

        LOGGER.debug("[TesseraUI] Hot reload: loaded '{}' from disk", resourceId);
        return new TesseraTemplate(root, sheet);
    }

    // ── ResourceManager loader (production) ───────────────────────────────────

    private static TesseraTemplate loadFromResourceManager(String resourceId) {
        String[] parts = resourceId.split(":", 2);
        String namespace = parts[0];
        String path = parts.length > 1 ? parts[1] : parts[0];

        var rm = Minecraft.getInstance().getResourceManager();

        TesseraNode root;
        try {
            var htmlLoc = ResourceLocation.fromNamespaceAndPath(namespace, path + ".html");
            var htmlRes = rm.getResource(htmlLoc).orElseThrow(
                () -> new IllegalArgumentException("Template not found: " + htmlLoc));
            try (var is = htmlRes.open()) {
                root = TesseraHtmlParser.parse(is);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template: " + resourceId, e);
        }

        TesseraStyleSheet sheet = TesseraStyleSheet.EMPTY;
        String cssPath = namespace + ":" + path + ".css";
        try {
            var cssLoc = ResourceLocation.fromNamespaceAndPath(namespace, path + ".css");
            var cssRes = rm.getResource(cssLoc);
            if (cssRes.isPresent()) {
                try (var is = cssRes.get().open()) {
                    String css = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    try {
                        sheet = TesseraCssParser.parse(css);
                    } catch (Exception parseEx) {
                        LOGGER.warn("[TesseraUI] CSS parse error in '{}': {}", cssPath, parseEx.getMessage());
                        sheet = TesseraStyleSheet.EMPTY;
                    }
                }
            }
        } catch (IOException e) {
            // CSS file absent — silent, this is expected
        }

        return new TesseraTemplate(root, sheet);
    }

    /** Creates a template from a raw HTML string with no stylesheet. */
    public static TesseraTemplate fromString(String html) {
        return fromString(html, "");
    }

    /** Creates a template from raw HTML and CSS strings. Does not require a Minecraft runtime. */
    public static TesseraTemplate fromString(String html, String css) {
        TesseraNode root = TesseraHtmlParser.parse(html);
        TesseraStyleSheet sheet = css.isBlank() ? TesseraStyleSheet.EMPTY : TesseraCssParser.parse(css);
        return new TesseraTemplate(root, sheet);
    }

    public TesseraNode root() { return root; }

    public TesseraStyleSheet styleSheet() { return styleSheet; }
}
