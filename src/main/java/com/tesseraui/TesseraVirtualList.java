package com.tesseraui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A scrollable virtual-list widget that only instantiates and renders the rows
 * currently visible in the viewport.  Crucial for lists of 100+ items
 * (inventories, logs, recipes) where materialising every widget upfront would
 * be prohibitively expensive.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * List<TesseraModel> items = ...; // 500 items
 * TesseraVirtualList list = TesseraVirtualList.of(items, 16, model -> {
 *     TesseraLabel lbl = new TesseraLabel(0, 0, 200, 14, model.resolve("name"));
 *     lbl.color(0xFFFFFFFF).fontSize(7f);
 *     return lbl;
 * });
 * }</pre>
 *
 * <p>Scroll the list with the mouse wheel.  Click events are forwarded to the
 * widget under the mouse cursor.  A thin scrollbar is rendered on the right
 * when the total content height exceeds the visible area.</p>
 */
public final class TesseraVirtualList extends TesseraElement {

    private static final int SCROLLBAR_WIDTH = 3;
    private static final int CACHE_BUFFER_ROWS = 8;

    private final List<TesseraModel> items;
    private final Function<TesseraModel, TesseraWidget> rowFactory;
    private final int rowHeight;

    /** Current scroll position in pixels (0 = top). */
    private int scrollY = 0;

    /** Cache: index → widget. Widgets are created lazily and retained. */
    private final Map<Integer, TesseraWidget> rowCache = new LinkedHashMap<>(16, 0.75f, true);

    private int background = 0;
    private int rowGap = 0;
    private int scrollBarColor = 0x80FFFFFF;

    private TesseraVirtualList(List<TesseraModel> items, int rowHeight,
                                Function<TesseraModel, TesseraWidget> factory,
                                int x, int y, int w, int h) {
        super(x, y, w, h);
        this.items = items;
        this.rowHeight = Math.max(1, rowHeight);
        this.rowFactory = factory;
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Creates a new {@code TesseraVirtualList} positioned at (0,0) with zero size.
     * Call {@link #setPosition} / {@link #setSize} or add to a {@link TesseraPanel}
     * to position it.
     */
    public static TesseraVirtualList of(List<TesseraModel> items, int rowHeight,
                                         Function<TesseraModel, TesseraWidget> factory) {
        return new TesseraVirtualList(items, rowHeight, factory, 0, 0, 0, 0);
    }

    // ── Fluent setters ────────────────────────────────────────────────────────

    public TesseraVirtualList background(int color)    { this.background = color; return this; }
    public TesseraVirtualList rowGap(int px)           { this.rowGap = Math.max(0, px); return this; }
    public TesseraVirtualList scrollBarColor(int color){ this.scrollBarColor = color; return this; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int effectiveRowHeight() { return rowHeight + rowGap; }

    private int totalContentHeight() {
        if (items.isEmpty()) return 0;
        return items.size() * rowHeight + (items.size() - 1) * rowGap;
    }

    private int maxScroll() {
        return Math.max(0, totalContentHeight() - height);
    }

    private boolean needsScrollbar() { return maxScroll() > 0; }

    private int innerWidth() { return needsScrollbar() ? width - SCROLLBAR_WIDTH - 1 : width; }

    /** Returns or creates the widget for the given item index. */
    private TesseraWidget getRow(int index) {
        return rowCache.computeIfAbsent(index, i -> rowFactory.apply(items.get(i)));
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        if (!visible) return;

        // Background
        if (background != 0) g.fill(x, y, x + width, y + height, background);

        int effRowH = effectiveRowHeight();
        int iW = innerWidth();

        // Compute visible range
        int firstVisible = scrollY / effRowH;
        int lastVisible = (scrollY + height) / effRowH;
        if (lastVisible >= items.size()) lastVisible = items.size() - 1;

        TesseraWidget hoveredRow = null;

        // Clip rendering to the list bounds
        g.enableScissor(x, y, x + width, y + height);

        for (int i = firstVisible; i <= lastVisible; i++) {
            TesseraWidget row = getRow(i);
            int rowY = y + (i * rowHeight) + (i * rowGap) - scrollY;
            row.setPosition(x, rowY);
            row.setSize(iW, rowHeight);
            if (row.isVisible()) row.render(g, mx, my);
            if (row.isVisible() && row.bounds().contains(mx, my)) hoveredRow = row;
        }

        pruneCache(firstVisible, lastVisible);

        g.disableScissor();

        // Scrollbar
        renderScrollbar(g);

        // Row tooltips must be drawn after the list scissor is disabled, otherwise
        // long tooltips get clipped at the virtual-list bounds.
        if (hoveredRow instanceof TesseraPanel rowPanel) {
            rowPanel.renderTooltips(g, mx, my);
        } else if (hoveredRow != null) {
            String tip = hoveredRow.getTooltip();
            if (tip != null && !tip.isBlank()) {
                TesseraPanel.renderTooltipBox(g, tip, mx, my);
            }
        }
    }

    private void renderScrollbar(GuiGraphics g) {
        int max = maxScroll();
        if (max <= 0) return;
        int totalH = totalContentHeight();
        int barH = Math.max(8, height * height / totalH);
        int barY = y + (int) ((long) scrollY * (height - barH) / max);
        int barX = x + width - SCROLLBAR_WIDTH;
        g.fill(barX, y, barX + SCROLLBAR_WIDTH, y + height, 0x20FFFFFF);
        g.fill(barX, barY, barX + SCROLLBAR_WIDTH, barY + barH, scrollBarColor);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    /**
     * Handles mouse scroll: updates {@link #scrollY} clamped to valid range.
     *
     * @param dy scroll delta (positive = scroll down in screen convention, but most
     *           MC wheels report positive = up; we follow TesseraScrollList convention:
     *           positive dy scrolls content upward i.e. decrements scrollY)
     * @return {@code true} if the scroll was consumed (list is hovered)
     */
    public boolean mouseScrolled(double dy) {
        if (!isInBounds()) return false;
        int prev = scrollY;
        int delta = (int)(dy * rowHeight);
        scrollY = Math.max(0, Math.min(maxScroll(), scrollY - delta));
        return scrollY != prev;
    }

    /** Variant that also takes the mouse position for bounds checking. */
    public boolean mouseScrolled(double mx, double my, double dy) {
        if (mx < x || mx >= x + width || my < y || my >= y + height) return false;
        int prev = scrollY;
        int delta = (int)(dy * rowHeight);
        scrollY = Math.max(0, Math.min(maxScroll(), scrollY - delta));
        return scrollY != prev;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (mx < x || mx >= x + width || my < y || my >= y + height) return false;

        // Scrollbar zone: consume click without forwarding
        if (needsScrollbar() && mx >= x + width - SCROLLBAR_WIDTH) return true;

        // Forward to visible row under the cursor
        int effRowH = effectiveRowHeight();
        int iW = innerWidth();
        int firstVisible = scrollY / effRowH;
        int lastVisible = (scrollY + height) / effRowH;
        if (lastVisible >= items.size()) lastVisible = items.size() - 1;

        for (int i = firstVisible; i <= lastVisible; i++) {
            TesseraWidget row = getRow(i);
            int rowY = y + (i * rowHeight) + (i * rowGap) - scrollY;
            row.setPosition(x, rowY);
            row.setSize(iW, rowHeight);
            if (row.isVisible() && row.mouseClicked(mx, my, btn)) return true;
        }
        return false;
    }

    private boolean isInBounds() { return true; } // used internally, real bounds check elsewhere

    // ── Keyboard forwarding ───────────────────────────────────────────────────

    /**
     * Forwards key events to all cached row widgets until one consumes it.
     * Required so that {@link TesseraInput} widgets inside virtual-list rows
     * can receive key input (backspace, arrows, ctrl+a, etc.) after being clicked.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        for (TesseraWidget row : rowCache.values()) {
            if (row.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    /**
     * Forwards typed characters to all cached row widgets until one consumes it.
     * Required so that {@link TesseraInput} widgets inside virtual-list rows
     * can receive character input after being focused via mouse click.
     */
    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!visible) return false;
        for (TesseraWidget row : rowCache.values()) {
            if (row.charTyped(c, modifiers)) return true;
        }
        return false;
    }

    // ── Cache management ──────────────────────────────────────────────────────

    /**
     * Clears the row widget cache.  Call this when the items list changes or
     * row styles need to be refreshed.
     */
    public void clearCache() {
        rowCache.clear();
        scrollY = Math.min(scrollY, maxScroll());
    }

    private void pruneCache(int firstVisible, int lastVisible) {
        int min = Math.max(0, firstVisible - CACHE_BUFFER_ROWS);
        int max = Math.min(items.size() - 1, lastVisible + CACHE_BUFFER_ROWS);
        rowCache.entrySet().removeIf(e -> {
            int index = e.getKey();
            if (index >= min && index <= max) return false;
            return !containsFocused(e.getValue());
        });
    }

    private static boolean containsFocused(TesseraWidget widget) {
        if (widget == null) return false;
        if (widget.isFocused()) return true;
        if (widget instanceof TesseraPanel panel) {
            for (TesseraWidget child : panel.debugChildren()) {
                if (containsFocused(child)) return true;
            }
        }
        return false;
    }

    int cachedRowCount() { return rowCache.size(); }

    /** Returns the current scroll offset in pixels. */
    public int getScrollY() { return scrollY; }

    /** Programmatically sets the scroll offset (clamped). */
    public void setScrollY(int py) { scrollY = Math.max(0, Math.min(maxScroll(), py)); }
}
