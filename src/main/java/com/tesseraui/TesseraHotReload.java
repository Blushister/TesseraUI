package com.tesseraui;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hot-reload engine for {@link TesseraTemplate}.
 *
 * <h3>Activation</h3>
 * <ol>
 *   <li><b>Auto-detect (dev mode)</b>: if {@code src/main/resources/} exists relative to
 *       the working directory and NeoForge reports a non-production environment,
 *       hot reload is enabled automatically.</li>
 *   <li><b>Explicit</b>: pass {@code -Dtessera.hotreload=<path>} on the JVM command line,
 *       e.g. {@code -Dtessera.hotreload=src/main/resources}.</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * Edit any {@code .html} or {@code .css} file under {@code assets/}. The watcher detects
 * the change, evicts the cached template, and sets the {@linkplain #isDirty() dirty flag}.
 * {@link TesseraScreen#tick()} polls this flag and calls {@code init()} to rebuild the UI —
 * no client restart needed.
 *
 * <p>F3+T also clears the full cache via {@link #invalidateAll()}.</p>
 */
public final class TesseraHotReload {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Template cache: resourceId → parsed template. Thread-safe. */
    private static final Map<String, TesseraTemplate> CACHE = new ConcurrentHashMap<>();

    /** Set to true by the watcher thread when any template is invalidated. */
    private static final AtomicBoolean DIRTY = new AtomicBoolean(false);

    /** Non-null when hot reload is active; points to the {@code resources/} root. */
    private static volatile Path resourcesRoot = null;

    private TesseraHotReload() {}

    // ── Activation ────────────────────────────────────────────────────────────

    /**
     * Tries to enable hot reload. Called once during client setup.
     *
     * <p>Priority: explicit system property → auto-detect {@code src/main/resources}.</p>
     *
     * @param devEnvironment {@code true} when NeoForge reports a non-production build
     *                       (used to gate auto-detection so it never fires in prod JARs)
     */
    public static void tryEnable(boolean devEnvironment) {
        // 1. Explicit JVM flag
        String explicit = System.getProperty("tessera.hotreload");
        if (explicit != null) {
            Path p = Path.of(explicit);
            if (Files.isDirectory(p)) {
                activate(p);
                return;
            }
            LOGGER.warn("[TesseraUI] tessera.hotreload path not found: {}", p.toAbsolutePath());
        }

        // 2. Auto-detect in dev mode only.
        // NeoForge runClient sets CWD to <project>/run/, so we walk up the tree
        // looking for a src/main/resources/assets directory (up to 4 levels).
        if (devEnvironment) {
            Path found = findResourcesDir();
            if (found != null) {
                activate(found);
            } else {
                LOGGER.debug("[TesseraUI] Hot reload: src/main/resources not found (CWD={})",
                        Path.of("").toAbsolutePath());
            }
        }
    }

    /**
     * Walks up from the current working directory looking for
     * {@code src/main/resources/assets}.  Handles both CWD=project root and
     * CWD=project/run/ (the default NeoForge runClient layout).
     */
    private static Path findResourcesDir() {
        Path cwd = Path.of("").toAbsolutePath();
        for (int up = 0; up <= 4; up++) {
            Path candidate = cwd;
            for (int i = 0; i < up; i++) {
                if (candidate.getParent() == null) break;
                candidate = candidate.getParent();
            }
            Path resources = candidate.resolve("src/main/resources");
            if (Files.isDirectory(resources.resolve("assets"))) {
                return resources;
            }
        }
        return null;
    }

    private static void activate(Path root) {
        resourcesRoot = root.toAbsolutePath().normalize();
        LOGGER.info("[TesseraUI] Hot reload ENABLED — watching: {}", resourcesRoot);
        startWatcher(resourcesRoot.resolve("assets"));
        // Post a chat message so it's visible in-game without checking logs.
        // Deferred to the next client tick so Minecraft's chat is ready.
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§a[TesseraUI]§r Hot reload active — watching " + resourcesRoot),
                    false);
            }
        });
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns {@code true} when hot reload is active and templates should be loaded from disk. */
    public static boolean isActive() { return resourcesRoot != null; }

    /** Returns the cached template for {@code resourceId}, or {@code null} if not cached. */
    public static TesseraTemplate get(String resourceId) { return CACHE.get(resourceId); }

    /** Stores a freshly loaded template in the cache. */
    public static void put(String resourceId, TesseraTemplate template) {
        CACHE.put(resourceId, template);
    }

    /**
     * Returns the path to the {@code .html} file for the given resource id, resolved
     * against the hot-reload resources root.
     *
     * <p>Example: {@code "mymod:ui/main_menu"} → {@code <root>/assets/mymod/ui/main_menu.html}</p>
     */
    public static Path resolveHtml(String resourceId) {
        return resolveExt(resourceId, ".html");
    }

    /** Like {@link #resolveHtml} but for the companion {@code .css} file. */
    public static Path resolveCss(String resourceId) {
        return resolveExt(resourceId, ".css");
    }

    private static Path resolveExt(String resourceId, String ext) {
        String[] parts = resourceId.split(":", 2);
        String namespace = parts[0];
        String path      = parts.length > 1 ? parts[1] : parts[0];
        return resourcesRoot.resolve("assets").resolve(namespace).resolve(path + ext);
    }

    /**
     * Removes all entries from the template cache. Called on F3+T so stale templates
     * are never served from the ResourceManager cache either.
     */
    public static void invalidateAll() {
        if (!CACHE.isEmpty()) {
            LOGGER.info("[TesseraUI] Hot reload: cache cleared ({} templates)", CACHE.size());
            CACHE.clear();
            DIRTY.set(true);
        }
    }

    /**
     * Atomically reads and clears the dirty flag.
     *
     * @return {@code true} if at least one template was invalidated since the last call
     */
    public static boolean consumeDirty() { return DIRTY.getAndSet(false); }

    /** Non-destructive dirty check (does not clear the flag). */
    public static boolean isDirty() { return DIRTY.get(); }

    // ── WatchService ──────────────────────────────────────────────────────────

    private static void startWatcher(Path assetsDir) {
        if (!Files.isDirectory(assetsDir)) {
            LOGGER.warn("[TesseraUI] Hot reload: assets dir not found: {}", assetsDir);
            return;
        }
        Thread t = new Thread(() -> runWatcher(assetsDir), "TesseraUI-HotReload");
        t.setDaemon(true);
        t.start();
    }

    private static void runWatcher(Path assetsDir) {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Map<WatchKey, Path> keyDir = new HashMap<>();
            registerAll(assetsDir, watcher, keyDir);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException e) {
                    break;
                }

                Path dir = keyDir.get(key);
                if (dir != null) {
                    for (WatchEvent<?> ev : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = ev.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                        Path changed = dir.resolve((Path) ev.context());

                        // Register newly created subdirectories
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE
                                && Files.isDirectory(changed)) {
                            try {
                                registerAll(changed, watcher, keyDir);
                            } catch (IOException ignored) {}
                        }

                        // Invalidate template on .html / .css change
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY
                                || kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            String name = changed.getFileName().toString();
                            if (name.endsWith(".html") || name.endsWith(".css")) {
                                String id = pathToResourceId(assetsDir, changed);
                                if (id != null) {
                                    CACHE.remove(id);
                                    DIRTY.set(true);
                                    LOGGER.info("[TesseraUI] Hot reload: '{}' changed → invalidated", id);
                                    // Notify player in-game
                                    net.minecraft.client.Minecraft mc =
                                            net.minecraft.client.Minecraft.getInstance();
                                    mc.execute(() -> {
                                        if (mc.player != null) {
                                            mc.player.displayClientMessage(
                                                net.minecraft.network.chat.Component.literal(
                                                    "§e[TesseraUI]§r Reloading §f" + id),
                                                true); // true = action bar (less intrusive)
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
                key.reset();
            }
        } catch (IOException e) {
            LOGGER.error("[TesseraUI] Hot reload watcher error", e);
        }
    }

    /** Registers {@code root} and every subdirectory it contains. */
    private static void registerAll(Path root, WatchService watcher,
                                    Map<WatchKey, Path> keyDir) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                WatchKey k = dir.register(watcher,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY);
                keyDir.put(k, dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Converts a full file path under {@code assetsDir} to a TesseraUI resource id.
     *
     * <p>Example: {@code <assetsDir>/mymod/ui/main_menu.html} → {@code "mymod:ui/main_menu"}</p>
     */
    private static String pathToResourceId(Path assetsDir, Path file) {
        try {
            Path rel = assetsDir.relativize(file);
            // rel = namespace / sub / dirs / filename.ext
            int count = rel.getNameCount();
            if (count < 2) return null;
            String namespace = rel.getName(0).toString();
            // Build the path part (without extension)
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < count; i++) {
                if (i > 1) sb.append('/');
                String seg = rel.getName(i).toString();
                if (i == count - 1) {
                    // Strip .html or .css extension
                    int dot = seg.lastIndexOf('.');
                    sb.append(dot >= 0 ? seg.substring(0, dot) : seg);
                } else {
                    sb.append(seg);
                }
            }
            return namespace + ":" + sb;
        } catch (Exception e) {
            return null;
        }
    }
}
