package com.tesseraui;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
    private static final List<TesseraStyleSheet> GLOBAL_STYLESHEETS = new ArrayList<>();

    private final TesseraNode root;
    private final TesseraStyleSheet styleSheet;

    private TesseraTemplate(TesseraNode root, TesseraStyleSheet styleSheet) {
        this.root = root;
        this.styleSheet = styleSheet != null ? styleSheet : TesseraStyleSheet.EMPTY;
    }

    /**
     * Registers a stylesheet applied before every template's local CSS.
     * Later global sheets override earlier global sheets, while a template's own
     * CSS still wins over globals with equal specificity.
     */
    public static void addGlobalStylesheet(TesseraStyleSheet sheet) {
        if (sheet != null && !sheet.isEmpty()) GLOBAL_STYLESHEETS.add(sheet);
    }

    /** Convenience overload for registering raw CSS as a global stylesheet. */
    public static void addGlobalStylesheet(String css) {
        if (css != null && !css.isBlank()) addGlobalStylesheet(TesseraCssParser.parse(css));
    }

    /** Clears registered global stylesheets. Useful for tests and hot reload. */
    public static void clearGlobalStylesheets() {
        GLOBAL_STYLESHEETS.clear();
    }

    private static TesseraStyleSheet withGlobalStyles(TesseraStyleSheet local) {
        TesseraStyleSheet sheet = TesseraStyleSheet.EMPTY;
        for (TesseraStyleSheet global : GLOBAL_STYLESHEETS) sheet = sheet.merge(global);
        return sheet.merge(local != null ? local : TesseraStyleSheet.EMPTY);
    }

    private static TesseraStyleSheet mergeLinkedStyles(TesseraNode root, StylesheetLoader loader) {
        TesseraStyleSheet sheet = TesseraStyleSheet.EMPTY;
        for (String href : linkedStylesheets(root)) {
            TesseraStyleSheet linked = loader.load(href);
            if (linked != null) sheet = sheet.merge(linked);
        }
        return sheet;
    }

    private static List<String> linkedStylesheets(TesseraNode root) {
        List<String> out = new ArrayList<>();
        collectLinkedStylesheets(root, out);
        return out;
    }

    private static void collectLinkedStylesheets(TesseraNode node, List<String> out) {
        if (node == null) return;
        if ("link".equals(node.tag())) {
            String rel = node.attr("rel");
            String href = node.attr("href");
            if ("stylesheet".equalsIgnoreCase(rel) && !href.isBlank()) out.add(href.trim());
        }
        for (TesseraNode child : node.children()) collectLinkedStylesheets(child, out);
    }

    private static ResourceRef resolveCssRef(String href, String currentNamespace, String currentPath) {
        String namespace = currentNamespace;
        String path = href;
        if (href.contains(":")) {
            String[] parts = href.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        } else if (!href.startsWith("/") && currentPath.contains("/")) {
            path = currentPath.substring(0, currentPath.lastIndexOf('/') + 1) + href;
        } else if (href.startsWith("/")) {
            path = href.substring(1);
        }
        if (path.endsWith(".css")) path = path.substring(0, path.length() - 4);
        return new ResourceRef(namespace, path);
    }

    private record ResourceRef(String namespace, String path) {
        String id() { return namespace + ":" + path; }
    }

    @FunctionalInterface
    private interface StylesheetLoader {
        TesseraStyleSheet load(String href);
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

    // â”€â”€ Filesystem loader (hot reload) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static TesseraTemplate loadFromFilesystem(String resourceId) {
        Path htmlPath = TesseraHotReload.resolveHtml(resourceId);
        TesseraNode root;
        try {
            String html = Files.readString(htmlPath, StandardCharsets.UTF_8);
            root = TesseraHtmlParser.parseWithComponents(html);
        } catch (IOException e) {
            throw new RuntimeException(
                    "[TesseraUI] Hot reload: HTML not found on disk: " + htmlPath, e);
        }

        TesseraStyleSheet sheet = TesseraStyleSheet.EMPTY;
        ResourceRef current = parseResourceRef(resourceId);
        sheet = sheet.merge(mergeLinkedStyles(root, href -> loadLinkedStylesheetFromFilesystem(href, current)));
        Path cssPath = TesseraHotReload.resolveCss(resourceId);
        if (Files.exists(cssPath)) {
            try {
                String css = Files.readString(cssPath, StandardCharsets.UTF_8);
                sheet = sheet.merge(TesseraCssParser.parse(css));
            } catch (IOException e) {
                LOGGER.warn("[TesseraUI] Hot reload: could not read CSS {}: {}", cssPath, e.getMessage());
            }
        }

        LOGGER.debug("[TesseraUI] Hot reload: loaded '{}' from disk", resourceId);
        return new TesseraTemplate(root, withGlobalStyles(sheet));
    }

    // â”€â”€ ResourceManager loader (production) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
                root = TesseraHtmlParser.parseWithComponents(is);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template: " + resourceId, e);
        }

        TesseraStyleSheet sheet = TesseraStyleSheet.EMPTY;
        ResourceRef current = new ResourceRef(namespace, path);
        sheet = sheet.merge(mergeLinkedStyles(root, href -> loadLinkedStylesheetFromResourceManager(href, current)));
        String cssPath = namespace + ":" + path + ".css";
        try {
            var cssLoc = ResourceLocation.fromNamespaceAndPath(namespace, path + ".css");
            var cssRes = rm.getResource(cssLoc);
            if (cssRes.isPresent()) {
                try (var is = cssRes.get().open()) {
                    String css = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    try {
                        sheet = sheet.merge(TesseraCssParser.parse(css));
                    } catch (Exception parseEx) {
                        LOGGER.warn("[TesseraUI] CSS parse error in '{}': {}", cssPath, parseEx.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            // CSS file absent â€” silent, this is expected
        }

        return new TesseraTemplate(root, withGlobalStyles(sheet));
    }

    /** Creates a template from a raw HTML string with no stylesheet. */
    public static TesseraTemplate fromString(String html) {
        return fromString(html, "");
    }

    /** Creates a template from raw HTML and CSS strings. Does not require a Minecraft runtime. */
    public static TesseraTemplate fromString(String html, String css) {
        TesseraNode root = TesseraHtmlParser.parseWithComponents(html);
        TesseraStyleSheet sheet = css.isBlank() ? TesseraStyleSheet.EMPTY : TesseraCssParser.parse(css);
        return new TesseraTemplate(root, withGlobalStyles(sheet));
    }

    /**
     * Creates a template from raw HTML and multiple CSS fragments. Fragments are
     * applied in argument order, so later fragments override earlier fragments.
     */
    public static TesseraTemplate fromString(String html, String... cssFragments) {
        TesseraNode root = TesseraHtmlParser.parseWithComponents(html);
        TesseraStyleSheet sheet = TesseraStyleSheet.EMPTY;
        if (cssFragments != null) {
            for (String css : cssFragments) {
                if (css != null && !css.isBlank()) sheet = sheet.merge(TesseraCssParser.parse(css));
            }
        }
        return new TesseraTemplate(root, withGlobalStyles(sheet));
    }

    private static ResourceRef parseResourceRef(String resourceId) {
        String[] parts = resourceId.split(":", 2);
        return new ResourceRef(parts[0], parts.length > 1 ? parts[1] : parts[0]);
    }

    private static TesseraStyleSheet loadLinkedStylesheetFromFilesystem(String href, ResourceRef current) {
        ResourceRef ref = resolveCssRef(href, current.namespace(), current.path());
        Path cssPath = TesseraHotReload.resolveStylesheet(ref.id());
        if (!Files.exists(cssPath)) {
            LOGGER.warn("[TesseraUI] Linked stylesheet not found on disk: {}", cssPath);
            return TesseraStyleSheet.EMPTY;
        }
        try {
            return TesseraCssParser.parse(Files.readString(cssPath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.warn("[TesseraUI] Could not read linked stylesheet {}: {}", cssPath, e.getMessage());
            return TesseraStyleSheet.EMPTY;
        } catch (Exception e) {
            LOGGER.warn("[TesseraUI] CSS parse error in linked stylesheet '{}': {}", cssPath, e.getMessage());
            return TesseraStyleSheet.EMPTY;
        }
    }

    private static TesseraStyleSheet loadLinkedStylesheetFromResourceManager(String href, ResourceRef current) {
        ResourceRef ref = resolveCssRef(href, current.namespace(), current.path());
        var rm = Minecraft.getInstance().getResourceManager();
        var cssLoc = ResourceLocation.fromNamespaceAndPath(ref.namespace(), ref.path() + ".css");
        try {
            var cssRes = rm.getResource(cssLoc);
            if (cssRes.isEmpty()) {
                LOGGER.warn("[TesseraUI] Linked stylesheet not found: {}", cssLoc);
                return TesseraStyleSheet.EMPTY;
            }
            try (var is = cssRes.get().open()) {
                return TesseraCssParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            LOGGER.warn("[TesseraUI] Could not read linked stylesheet {}: {}", cssLoc, e.getMessage());
            return TesseraStyleSheet.EMPTY;
        } catch (Exception e) {
            LOGGER.warn("[TesseraUI] CSS parse error in linked stylesheet '{}': {}", cssLoc, e.getMessage());
            return TesseraStyleSheet.EMPTY;
        }
    }

    public TesseraNode root() { return root; }

    public TesseraStyleSheet styleSheet() { return styleSheet; }
}

