package com.tesseraui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class TesseraPanel implements TesseraWidget {

    public enum Mode { ROW, COLUMN, GRID }

    /** Sentinel — absolute-position offset not set by CSS. */
    static final int ABS_UNSET = Integer.MIN_VALUE;

    /**
     * A normal-flow child with full CSS flexbox metadata.
     *
     * <ul>
     *   <li>{@code flexGrow}   — proportion of free space to absorb (0 = don't grow)</li>
     *   <li>{@code flexShrink} — proportion of overflow to absorb (0 = don't shrink)</li>
     *   <li>{@code flexBasis}  — initial main-axis size; {@link TesseraStyle#UNSET} = auto (content size)</li>
     *   <li>{@code order}      — layout ordering override (default 0 = DOM order)</li>
     * </ul>
     */
    private record Entry(TesseraWidget widget,
                         float flexGrow, float flexShrink, int flexBasis,
                         int order, int zIndex,
                         String alignSelf, boolean marginTopAuto,
                         int marginTop, int marginRight, int marginBottom, int marginLeft) {}

    /**
     * Absolutely-positioned child: drawn after normal-flow children, positioned each frame
     * relative to the panel's own top-left corner + padding.
     * Any offset that equals {@link #ABS_UNSET} is ignored.
     */
    private record AbsEntry(TesseraWidget widget, int top, int left, int right, int bottom, int zIndex) {}

    private final Mode mode;
    private final int cols;
    private int x, y, w, h;
    private int gap = 0;
    private int padLeft, padRight, padTop, padBottom;
    private boolean active = true;
    private boolean clip = false;
    private boolean wrap = false;
    private final List<Entry> children = new ArrayList<>();
    private Runnable onClickAction;
    private int background = 0;
    private int borderColor = 0;
    private int borderThickness = 0;

    private int borderTopColor    = 0;
    private int borderBottomColor = 0;
    private int borderLeftColor   = 0;
    private int borderRightColor  = 0;

    private int hoverBackground       = 0;
    private int hoverBorderColor      = 0;
    private int hoverBorderTopColor    = 0;
    private int hoverBorderBottomColor = 0;
    private int hoverBorderLeftColor   = 0;
    private int hoverBorderRightColor  = 0;
    private boolean hoverBackgroundSet  = false;
    private boolean hoverBorderColorSet = false;

    private int borderRadius = 0;

    private int cornerDotSize  = 0;
    private int cornerDotColor = 0;

    private float opacity      = 1f;
    private float hoverOpacity = -1f;   // -1 = not set (no hover override)

    private String[] gridTemplateColumns = null;

    private String justifyContent = "flex-start";
    private String alignItems = "center";

    private final List<AbsEntry> absChildren = new ArrayList<>();

    private boolean layoutDirty = true;
    private boolean visible = true;
    private String tooltip = null;

    /** Pending children swap requested from a reactive model listener (runs on next render). */
    private List<Entry> pendingChildren = null;

    // ── Hover transition animation state ─────────────────────────────────────
    private int     transitionDurationMs = 0;    // set from style in transition()
    private String  transitionProperty   = null;
    private boolean wasHovered           = false;
    private long    animStartMs          = 0L;
    private int     animFromBg           = 0, animToBg = 0;
    private float   animFromOpacity      = 1f, animToOpacity = 1f;
    private int     animFromBorder       = 0, animToBorder   = 0;
    private int     animFromColor        = 0, animToColor    = 0;
    /** Original color of direct label/button children — captured on first hover-in, used to restore on hover-out. */
    private int     baseChildrenColor    = 0;

    // ── Drag & Drop ────────────────────────────────────────────────────────────
    private boolean         draggable        = false;
    private Object          dragPayload      = null;
    private TesseraDropZone dropZoneHandler  = null;

    // ── Right-click ───────────────────────────────────────────────────────────
    private Runnable onRightClick = null;

    // ── Hover text color ──────────────────────────────────────────────────────
    private int     hoverTextColor    = 0;
    private boolean hoverTextColorSet = false;

    private TesseraPanel(Mode mode, int cols, int x, int y, int w, int h) {
        this.mode = mode;
        this.cols = cols;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        if (mode == Mode.COLUMN) this.alignItems = "stretch";
    }

    // ── Factory methods ────────────────────────────────────────────────────

    public static TesseraPanel row(int x, int y, int w, int h)    { return new TesseraPanel(Mode.ROW, 0, x, y, w, h); }
    public static TesseraPanel column(int x, int y, int w, int h) { return new TesseraPanel(Mode.COLUMN, 0, x, y, w, h); }
    public static TesseraPanel grid(int cols, int x, int y, int w, int h) { return new TesseraPanel(Mode.GRID, cols, x, y, w, h); }

    // ── Builder fluent ─────────────────────────────────────────────────────

    // ── add() overloads ────────────────────────────────────────────────────────

    /** Adds a widget with no flex participation (natural size, no grow/shrink). */
    public TesseraPanel add(TesseraWidget widget) {
        children.add(new Entry(widget, 0f, 0f, TesseraStyle.UNSET, 0, 0, null, false, 0, 0, 0, 0));
        layoutDirty = true;
        return this;
    }

    /**
     * Adds a widget with a legacy integer flex weight.
     * <ul>
     *   <li>{@code flex == 0} → no grow, no shrink, auto basis (content size)</li>
     *   <li>{@code flex >  0} → grows with weight {@code flex}, shrink=1, basis=0</li>
     * </ul>
     */
    public TesseraPanel add(TesseraWidget widget, int flex) {
        float grow  = flex > 0 ? (float) flex : 0f;
        float shrink = flex > 0 ? 1f : 0f;
        int   basis  = flex > 0 ? 0 : TesseraStyle.UNSET;
        children.add(new Entry(widget, grow, shrink, basis, 0, 0, null, false, 0, 0, 0, 0));
        layoutDirty = true;
        return this;
    }

    /** @see #add(TesseraWidget, int) */
    public TesseraPanel add(TesseraWidget widget, int flex, String alignSelf, boolean marginTopAuto) {
        float grow  = flex > 0 ? (float) flex : 0f;
        float shrink = flex > 0 ? 1f : 0f;
        int   basis  = flex > 0 ? 0 : TesseraStyle.UNSET;
        children.add(new Entry(widget, grow, shrink, basis, 0, 0, alignSelf, marginTopAuto, 0, 0, 0, 0));
        layoutDirty = true;
        return this;
    }

    /** @see #add(TesseraWidget, int) */
    public TesseraPanel add(TesseraWidget widget, int flex, String alignSelf, boolean marginTopAuto,
                            int marginTop, int marginRight, int marginBottom, int marginLeft) {
        float grow  = flex > 0 ? (float) flex : 0f;
        float shrink = flex > 0 ? 1f : 0f;
        int   basis  = flex > 0 ? 0 : TesseraStyle.UNSET;
        children.add(new Entry(widget, grow, shrink, basis, 0, 0, alignSelf, marginTopAuto,
                marginTop, marginRight, marginBottom, marginLeft));
        layoutDirty = true;
        return this;
    }

    /**
     * Adds a widget with full CSS flexbox parameters.
     *
     * @param flexGrow   CSS {@code flex-grow}  (0 = don't grow)
     * @param flexShrink CSS {@code flex-shrink} (0 = don't shrink)
     * @param flexBasis  initial main-axis size in px; {@link TesseraStyle#UNSET} = auto
     * @param order      CSS {@code order} (0 = DOM position)
     * @param zIndex     CSS {@code z-index} (0 = default stacking order)
     */
    public TesseraPanel add(TesseraWidget widget,
                            float flexGrow, float flexShrink, int flexBasis, int order, int zIndex,
                            String alignSelf, boolean marginTopAuto,
                            int marginTop, int marginRight, int marginBottom, int marginLeft) {
        children.add(new Entry(widget, flexGrow, flexShrink, flexBasis, order, zIndex,
                alignSelf, marginTopAuto, marginTop, marginRight, marginBottom, marginLeft));
        layoutDirty = true;
        return this;
    }

    /**
     * Adds an absolutely-positioned child.  The child is rendered after all normal-flow
     * children and positioned each frame relative to this panel's padded area.
     * Pass {@link #ABS_UNSET} for any offset that should not constrain the child's position.
     *
     * @param top    distance from padded top edge, or {@link #ABS_UNSET}
     * @param left   distance from padded left edge, or {@link #ABS_UNSET}
     * @param right  distance from padded right edge, or {@link #ABS_UNSET}
     * @param bottom distance from padded bottom edge, or {@link #ABS_UNSET}
     */
    public TesseraPanel addAbsolute(TesseraWidget widget, int top, int left, int right, int bottom) {
        absChildren.add(new AbsEntry(widget, top, left, right, bottom, 0));
        return this;
    }

    public TesseraPanel gap(int gap) { this.gap = gap; return this; }

    public TesseraPanel padding(int all)                             { padLeft = padRight = padTop = padBottom = all; return this; }
    public TesseraPanel padding(int horizontal, int vertical)        { padLeft = padRight = horizontal; padTop = padBottom = vertical; return this; }
    public TesseraPanel padding(int top, int right, int bottom, int left) { padTop = top; padRight = right; padBottom = bottom; padLeft = left; return this; }

    public TesseraPanel clip(boolean clip)            { this.clip = clip; return this; }
    public TesseraPanel wrap(boolean wrap)            { this.wrap = wrap; return this; }
    public TesseraPanel onClick(Runnable action)      { this.onClickAction = action; return this; }
    public TesseraPanel background(int color)         { this.background = color; return this; }

    public TesseraPanel border(int thickness, int color) {
        this.borderThickness = thickness;
        this.borderColor = color;
        return this;
    }

    public TesseraPanel borderSide(String side, int color) {
        switch (side) {
            case "top"    -> borderTopColor    = color;
            case "bottom" -> borderBottomColor = color;
            case "left"   -> borderLeftColor   = color;
            case "right"  -> borderRightColor  = color;
        }
        return this;
    }

    public TesseraPanel hoverBackground(int color)       { this.hoverBackground = color; this.hoverBackgroundSet = true; return this; }
    public TesseraPanel hoverBorder(int color)           { this.hoverBorderColor = color; this.hoverBorderColorSet = true; return this; }
    public TesseraPanel hoverBorderSide(String side, int color) {
        switch (side) {
            case "top"    -> hoverBorderTopColor    = color;
            case "bottom" -> hoverBorderBottomColor = color;
            case "left"   -> hoverBorderLeftColor   = color;
            case "right"  -> hoverBorderRightColor  = color;
        }
        return this;
    }

    public TesseraPanel borderRadius(int radius) { this.borderRadius = Math.max(0, radius); return this; }

    public TesseraPanel cornerDots(int size, int color) {
        this.cornerDotSize = size;
        this.cornerDotColor = color;
        return this;
    }

    public TesseraPanel opacity(float o)      { this.opacity = Math.max(0f, Math.min(1f, o)); return this; }
    public TesseraPanel hoverOpacity(float o) { this.hoverOpacity = Math.max(0f, Math.min(1f, o)); return this; }

    /** Sets the tooltip text shown when the mouse hovers over this panel. */
    public TesseraPanel tooltip(String text)  { this.tooltip = text; return this; }

    /** Sets the CSS transition for hover animations. */
    public TesseraPanel transition(int durationMs, String property) {
        this.transitionDurationMs = durationMs;
        this.transitionProperty   = property;
        return this;
    }

    /** Fluent alias — argument order (property, durationMs) matches CSS shorthand. */
    public TesseraPanel transition(String property, int durationMs) {
        this.transitionDurationMs = durationMs;
        this.transitionProperty   = property;
        return this;
    }

    /**
     * Sets the hover text/foreground color that children with {@code color}
     * transitions will animate toward.
     */
    public TesseraPanel hoverColor(int color) {
        this.hoverTextColor    = color;
        this.hoverTextColorSet = true;
        return this;
    }

    /** Marks this panel as draggable (left-click starts a drag). */
    public TesseraPanel draggable(boolean d) { this.draggable = d; return this; }

    /** Sets the payload that will be carried during a drag from this panel. */
    public TesseraPanel dragPayload(Object payload) { this.dragPayload = payload; return this; }

    /**
     * Registers a {@link TesseraDropZone} handler on this panel so it can receive
     * drag-and-drop payloads.
     */
    public TesseraPanel dropZone(TesseraDropZone handler) { this.dropZoneHandler = handler; return this; }

    /** Sets the right-click handler for this panel. */
    public TesseraPanel onRightClick(Runnable r) { this.onRightClick = r; return this; }

    @Override
    public String getTooltip() { return tooltip; }

    @Override
    public void setTooltip(String text) { this.tooltip = text; }

    /**
     * Replaces this panel's normal-flow children with {@code newChildren} and triggers layout.
     * Called either directly or via the pending-swap mechanism from a reactive model listener.
     */
    private void swapChildren(List<Entry> newChildren) {
        children.clear();
        children.addAll(newChildren);
        layoutDirty = true;
        layout();
    }

    /**
     * Watches a {@link TesseraReactiveModel} and rebuilds this panel's children whenever the
     * model changes.  {@code rebuild} should return a freshly configured panel whose children
     * will replace the current ones (the panel itself is discarded — only its children are used).
     *
     * <p>The actual swap is deferred to the next {@link #render} call so it always happens on
     * the render thread.</p>
     *
     * @param model   the reactive model to observe
     * @param rebuild supplier that produces a new panel on each model change
     */
    public void watchModel(TesseraReactiveModel model, java.util.function.Supplier<TesseraPanel> rebuild) {
        model.addChangeListener(() -> {
            TesseraPanel fresh = rebuild.get();
            List<Entry> newEntries = new ArrayList<>(fresh.children);
            pendingChildren = newEntries;
        });
    }

    public TesseraPanel gridTemplateColumns(String[] tpl) { this.gridTemplateColumns = tpl; return this; }

    public TesseraPanel justifyContent(String value) { if (value != null) this.justifyContent = value; return this; }
    public TesseraPanel alignItems(String value)     { if (value != null) this.alignItems = value; return this; }

    // ── Layout engine ──────────────────────────────────────────────────────

    public void layout() {
        layoutDirty = false;
        if (children.isEmpty()) return;
        switch (mode) {
            case ROW    -> layoutRow();
            case COLUMN -> layoutColumn();
            case GRID   -> layoutGrid();
        }
    }

    private boolean allFlexOrdersZero() {
        return children.stream().allMatch(e -> e.order() == 0);
    }

    private boolean allZIndexZero() {
        return children.stream().allMatch(e -> e.zIndex() == 0);
    }

    private void layoutRow() {
        int availW = w - padLeft - padRight;
        int availH = h - padTop - padBottom;
        if (wrap) { layoutRowWrap(availW, availH); return; }

        // CSS order: stable sort (equal-order items keep DOM position)
        List<Entry> ord = allFlexOrdersZero() ? children
                : children.stream().sorted(Comparator.comparingInt(Entry::order)).toList();
        if (ord.isEmpty()) return;
        int n = ord.size();
        int totalGaps = Math.max(0, n - 1) * gap;

        // ── Step 1: base sizes (flex-basis or content width) ──────────────────
        int[] bases = new int[n];
        float totalGrow = 0, totalShrinkFactor = 0;
        int totalBase = 0, totalMargins = 0;
        for (int i = 0; i < n; i++) {
            Entry e = ord.get(i);
            int basis = e.flexBasis();
            bases[i] = (basis != TesseraStyle.UNSET && basis >= 0) ? basis : e.widget().getWidth();
            totalBase   += bases[i];
            totalMargins += e.marginLeft() + e.marginRight();
            totalGrow   += e.flexGrow();
            totalShrinkFactor += e.flexShrink() * bases[i];
        }

        // ── Step 2: grow or shrink ────────────────────────────────────────────
        int freeSpace = availW - totalBase - totalGaps - totalMargins;
        int[] finalSizes = Arrays.copyOf(bases, n);

        if (freeSpace > 0 && totalGrow > 0) {
            int remain = freeSpace;
            int lastGrowIdx = -1;
            for (int i = 0; i < n; i++) {
                float grow = ord.get(i).flexGrow();
                if (grow > 0) {
                    int add = (int)(freeSpace * grow / totalGrow);
                    finalSizes[i] = bases[i] + add;
                    remain -= add;
                    lastGrowIdx = i;
                }
            }
            if (lastGrowIdx >= 0) finalSizes[lastGrowIdx] += remain; // absorb rounding remainder
        } else if (freeSpace < 0 && totalShrinkFactor > 0) {
            int deficit = -freeSpace;
            for (int i = 0; i < n; i++) {
                float s = ord.get(i).flexShrink();
                if (s > 0 && bases[i] > 0) {
                    int reduce = (int)(deficit * (s * bases[i]) / totalShrinkFactor);
                    finalSizes[i] = Math.max(0, bases[i] - reduce);
                }
            }
        }

        // ── Step 3: justify-content (no effect when flex-grow items consumed free space) ──
        int startX = x + padLeft;
        int extraGap = 0;
        if (totalGrow == 0) {
            int contentW = totalGaps + totalMargins;
            for (int sz : finalSizes) contentW += sz;
            int free = availW - contentW;
            switch (justifyContent) {
                case "flex-end"      -> startX = x + padLeft + free;
                case "center"        -> startX = x + padLeft + free / 2;
                case "space-between" -> { if (n > 1) extraGap = free / (n - 1); }
                case "space-around"  -> { int p = n > 0 ? free / n : 0; startX = x + padLeft + p / 2; extraGap = p; }
                default              -> {}
            }
        }

        // ── Step 4: place items ───────────────────────────────────────────────
        int curX = startX;
        for (int i = 0; i < n; i++) {
            Entry e = ord.get(i);
            int mT = e.marginTop(), mR = e.marginRight(), mB = e.marginBottom(), mL = e.marginLeft();
            int childW = finalSizes[i];
            int childH = e.widget().getHeight();
            String align = e.alignSelf() != null ? e.alignSelf() : this.alignItems;
            int childY = switch (align) {
                case "flex-start" -> y + padTop + mT;
                case "flex-end"   -> y + padTop + availH - mB - childH;
                case "stretch"    -> { e.widget().setSize(childW, availH - mT - mB); yield y + padTop + mT; }
                default           -> y + padTop + mT + (availH - mT - mB - childH) / 2;
            };
            curX += mL;
            e.widget().setPosition(curX, childY);
            if (childW != e.widget().getWidth()) e.widget().setSize(childW, e.widget().getHeight());
            curX += childW + gap + extraGap + mR;
        }
    }

    private void layoutRowWrap(int availW, int availH) {
        int curX = x + padLeft;
        int curY = y + padTop;
        int lineH = 0;
        for (Entry e : children) {
            int childW = e.widget().getWidth();
            int childH = e.widget().getHeight();
            if (curX + childW > x + padLeft + availW && curX > x + padLeft) {
                curX = x + padLeft;
                curY += lineH + gap;
                lineH = 0;
            }
            e.widget().setPosition(curX, curY);
            curX += childW + gap;
            lineH = Math.max(lineH, childH);
        }
    }

    private void layoutColumn() {
        int availW = w - padLeft - padRight;
        int availH = h - padTop - padBottom;

        // CSS order: stable sort (equal-order items keep DOM position)
        List<Entry> ord = allFlexOrdersZero() ? children
                : children.stream().sorted(Comparator.comparingInt(Entry::order)).toList();
        if (ord.isEmpty()) return;
        int n = ord.size();

        // Pre-pass: set stretch widths BEFORE summing heights so TesseraLabel(wrap)
        // can recompute line count and therefore report the correct height.
        for (Entry e : ord) {
            String align = e.alignSelf() != null ? e.alignSelf() : this.alignItems;
            if ("stretch".equals(align)) {
                int childW = availW - e.marginLeft() - e.marginRight();
                if (childW > 0 && childW != e.widget().getWidth())
                    e.widget().setSize(childW, e.widget().getHeight());
            }
        }

        // ── Step 1: base sizes (flex-basis or content height) ─────────────────
        int[] bases = new int[n];
        float totalGrow = 0, totalShrinkFactor = 0;
        int totalBase = 0, totalMargins = 0;
        for (int i = 0; i < n; i++) {
            Entry e = ord.get(i);
            int basis = e.flexBasis();
            bases[i] = (basis != TesseraStyle.UNSET && basis >= 0) ? basis : e.widget().getHeight();
            totalBase   += bases[i];
            totalMargins += e.marginTop() + e.marginBottom();
            totalGrow   += e.flexGrow();
            totalShrinkFactor += e.flexShrink() * bases[i];
        }

        // ── Step 2: grow or shrink ────────────────────────────────────────────
        int totalGaps = Math.max(0, n - 1) * gap;
        int freeSpace = availH - totalBase - totalGaps - totalMargins;
        int[] finalSizes = Arrays.copyOf(bases, n);

        if (freeSpace > 0 && totalGrow > 0) {
            int remain = freeSpace;
            int lastGrowIdx = -1;
            for (int i = 0; i < n; i++) {
                float grow = ord.get(i).flexGrow();
                if (grow > 0) {
                    int add = (int)(freeSpace * grow / totalGrow);
                    finalSizes[i] = bases[i] + add;
                    remain -= add;
                    lastGrowIdx = i;
                }
            }
            if (lastGrowIdx >= 0) finalSizes[lastGrowIdx] += remain;
        } else if (freeSpace < 0 && totalShrinkFactor > 0) {
            int deficit = -freeSpace;
            for (int i = 0; i < n; i++) {
                float s = ord.get(i).flexShrink();
                if (s > 0 && bases[i] > 0) {
                    int reduce = (int)(deficit * (s * bases[i]) / totalShrinkFactor);
                    finalSizes[i] = Math.max(0, bases[i] - reduce);
                }
            }
        }

        // ── Step 3: auto-margin and justify-content ───────────────────────────
        int autoMarginIdx = -1;
        for (int i = 0; i < n; i++) {
            if (ord.get(i).marginTopAuto()) { autoMarginIdx = i; break; }
        }

        int startY = y + padTop;
        int extraGap = 0;
        if (totalGrow == 0 && autoMarginIdx < 0) {
            int contentH = totalGaps + totalMargins;
            for (int sz : finalSizes) contentH += sz;
            int free = availH - contentH;
            switch (justifyContent) {
                case "flex-end"      -> startY = y + padTop + free;
                case "center"        -> startY = y + padTop + free / 2;
                case "space-between" -> { if (n > 1) extraGap = free / (n - 1); }
                case "space-around"  -> { int p = n > 0 ? free / n : 0; startY = y + padTop + p / 2; extraGap = p; }
                default              -> {}
            }
        }

        // ── Step 4: place items ───────────────────────────────────────────────
        int curY = startY;
        for (int i = 0; i < n; i++) {
            Entry e = ord.get(i);
            int mT = e.marginTop(), mR = e.marginRight(), mB = e.marginBottom(), mL = e.marginLeft();
            int innerAvailW = availW - mL - mR;

            // margin-top: auto — push item down to consume all remaining free space
            if (i == autoMarginIdx && totalGrow == 0) {
                int usedSoFar = 0;
                for (int j = 0; j < i; j++) usedSoFar += finalSizes[j] + gap;
                int freeAuto = availH - totalBase - totalGaps - totalMargins;
                if (freeAuto > 0) curY = y + padTop + usedSoFar + freeAuto;
            }

            int childH = finalSizes[i];
            String align = e.alignSelf() != null ? e.alignSelf() : this.alignItems;
            int childX = switch (align) {
                case "flex-end" -> x + padLeft + mL + innerAvailW - e.widget().getWidth();
                case "center"   -> x + padLeft + mL + (innerAvailW - e.widget().getWidth()) / 2;
                case "stretch"  -> { e.widget().setSize(innerAvailW, childH); yield x + padLeft + mL; }
                default         -> x + padLeft + mL;
            };

            curY += mT;
            e.widget().setPosition(childX, curY);
            if (childH != e.widget().getHeight()) e.widget().setSize(e.widget().getWidth(), childH);
            curY += childH + gap + extraGap + mB;
        }
    }

    private void layoutGrid() {
        int availW = w - padLeft - padRight;

        int[] colWidths;
        int colsCount;
        if (gridTemplateColumns != null && gridTemplateColumns.length > 0) {
            colsCount = gridTemplateColumns.length;
            colWidths = computeGridTrackWidths(gridTemplateColumns, availW, gap);
        } else {
            if (cols <= 0) return;
            colsCount = cols;
            int cellW = (availW - gap * (cols - 1)) / cols;
            colWidths = new int[cols];
            for (int i = 0; i < cols; i++) colWidths[i] = cellW;
        }

        int numRows = (children.size() + colsCount - 1) / colsCount;
        int[] rowHeights = new int[Math.max(1, numRows)];
        for (int i = 0; i < children.size(); i++) {
            int row = i / colsCount;
            rowHeights[row] = Math.max(rowHeights[row], children.get(i).widget().getHeight());
        }

        int[] rowOffsets = new int[Math.max(1, numRows)];
        int cumY = 0;
        for (int r = 0; r < numRows; r++) {
            rowOffsets[r] = cumY;
            cumY += rowHeights[r] + gap;
        }

        int[] colOffsets = new int[colsCount];
        int cumX = 0;
        for (int c = 0; c < colsCount; c++) {
            colOffsets[c] = cumX;
            cumX += colWidths[c] + gap;
        }

        for (int i = 0; i < children.size(); i++) {
            int col = i % colsCount;
            int row = i / colsCount;
            int cx = x + padLeft + colOffsets[col];
            int cy = y + padTop + rowOffsets[row];
            Entry e = children.get(i);
            e.widget().setPosition(cx, cy);
            e.widget().setSize(colWidths[col], e.widget().getHeight());
        }
    }

    private static int[] computeGridTrackWidths(String[] tokens, int availW, int gap) {
        int n = tokens.length;
        int[] widths = new int[n];
        float[] frs = new float[n];
        int fixedPx = 0;
        float totalFr = 0f;
        for (int i = 0; i < n; i++) {
            String tok = tokens[i].trim().toLowerCase(java.util.Locale.ROOT);
            if (tok.endsWith("fr")) {
                try { frs[i] = Float.parseFloat(tok.substring(0, tok.length() - 2)); }
                catch (NumberFormatException e) { frs[i] = 1f; }
                totalFr += frs[i];
            } else if (tok.endsWith("px")) {
                try { widths[i] = Integer.parseInt(tok.substring(0, tok.length() - 2)); }
                catch (NumberFormatException e) { widths[i] = 0; }
                fixedPx += widths[i];
            } else if (tok.equals("auto")) {
                widths[i] = 0;
            } else {
                try { widths[i] = Integer.parseInt(tok); fixedPx += widths[i]; }
                catch (NumberFormatException e) { widths[i] = 0; }
            }
        }
        int totalGaps = Math.max(0, n - 1) * gap;
        int free = Math.max(0, availW - fixedPx - totalGaps);
        for (int i = 0; i < n; i++) {
            if (frs[i] > 0 && totalFr > 0) widths[i] = (int) (free * frs[i] / totalFr);
        }
        return widths;
    }

    // ── TesseraWidget ──────────────────────────────────────────────────────

    @Override
    public boolean isVisible() { return visible; }

    @Override
    public void setVisible(boolean visible) { this.visible = visible; }

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        if (!visible) return;
        // Apply deferred children swap from reactive model listener
        if (pendingChildren != null) {
            swapChildren(pendingChildren);
            pendingChildren = null;
        }
        if (layoutDirty) { layout(); }
        boolean hovered = bounds().contains(mx, my);

        int bg;
        int bColor;
        float effectiveOpacity;

        if (transitionDurationMs > 0) {
            long now = System.currentTimeMillis();
            // Detect hover state change → start transition
            if (hovered != wasHovered) {
                animFromBg      = computeCurrentBg(now);
                animToBg        = (hovered && hoverBackgroundSet) ? hoverBackground : background;
                animFromOpacity = computeCurrentOpacity(now);
                animToOpacity   = (hovered && hoverOpacity >= 0f) ? hoverOpacity : opacity;
                animFromBorder  = computeCurrentBorder(now);
                animToBorder    = (hovered && hoverBorderColorSet) ? hoverBorderColor : borderColor;
                if (hoverTextColorSet) {
                    if (hovered) {
                        // Capture the original child text color the first time hover starts
                        if (baseChildrenColor == 0) {
                            for (Entry e : children) {
                                if (e.widget() instanceof TesseraLabel lbl) {
                                    int c = lbl.getColor();
                                    if (c != 0) { baseChildrenColor = c; break; }
                                }
                            }
                            if (baseChildrenColor == 0) baseChildrenColor = TesseraPalette.CREAM;
                        }
                        animFromColor = baseChildrenColor;
                        animToColor   = hoverTextColor;
                    } else {
                        animFromColor = computeCurrentColor(now);
                        animToColor   = baseChildrenColor != 0 ? baseChildrenColor : TesseraPalette.CREAM;
                    }
                } else {
                    animFromColor = computeCurrentColor(now);
                    animToColor   = 0;
                }
                animStartMs     = now;
                wasHovered      = hovered;
            }
            float t = Math.min(1f, (float)(now - animStartMs) / transitionDurationMs);
            boolean animBg     = "background".equals(transitionProperty) || "all".equals(transitionProperty);
            boolean animBorder = "border-color".equals(transitionProperty) || "all".equals(transitionProperty);
            boolean animOp     = "opacity".equals(transitionProperty) || "all".equals(transitionProperty);
            boolean animColor  = "color".equals(transitionProperty) || "all".equals(transitionProperty);
            bg            = animBg     ? lerpColor(animFromBg,     animToBg,     t) : ((hovered && hoverBackgroundSet) ? hoverBackground : background);
            bColor        = animBorder ? lerpColor(animFromBorder, animToBorder, t) : ((hovered && hoverBorderColorSet) ? hoverBorderColor : borderColor);
            effectiveOpacity = animOp  ? lerp(animFromOpacity, animToOpacity, t)    : ((hovered && hoverOpacity >= 0f) ? hoverOpacity : opacity);
            // Propagate animated color to direct label/button children
            if (animColor && hoverTextColorSet && (animFromColor != 0 || animToColor != 0)) {
                int animatedColor = lerpColor(animFromColor, animToColor, t);
                propagateColorToChildren(animatedColor);
            }
        } else {
            bg       = (hovered && hoverBackgroundSet) ? hoverBackground : background;
            bColor   = (hovered && hoverBorderColorSet) ? hoverBorderColor : borderColor;
            effectiveOpacity = (hovered && hoverOpacity >= 0f) ? hoverOpacity : opacity;
        }
        int bTop    = (hovered && hoverBorderTopColor    != 0) ? hoverBorderTopColor    : borderTopColor;
        int bBottom = (hovered && hoverBorderBottomColor != 0) ? hoverBorderBottomColor : borderBottomColor;
        int bLeft   = (hovered && hoverBorderLeftColor   != 0) ? hoverBorderLeftColor   : borderLeftColor;
        int bRight  = (hovered && hoverBorderRightColor  != 0) ? hoverBorderRightColor  : borderRightColor;

        boolean useShader = effectiveOpacity < 1f;
        if (useShader) RenderSystem.setShaderColor(1f, 1f, 1f, effectiveOpacity);

        int r = borderRadius;
        if (r > 0) {
            // Rounded rendering: border as outer filled round rect, bg as inner
            int bt = (borderThickness > 0 && bColor != 0) ? borderThickness : 0;
            if (bt > 0) {
                fillRounded(g, x, y, w, h, r, bColor);
                int innerR = Math.max(0, r - bt);
                if (bg != 0) fillRounded(g, x + bt, y + bt, w - 2 * bt, h - 2 * bt, innerR, bg);
            } else if (bg != 0) {
                fillRounded(g, x, y, w, h, r, bg);
            }
        } else {
            if (bg != 0) g.fill(x, y, x + w, y + h, bg);
        }

        if (clip) {
            g.enableScissor(x, y, x + w, y + h);
        }
        // Render normal-flow children sorted by z-index (stable — DOM order preserved for equal values).
        if (allZIndexZero()) {
            for (Entry e : children) { if (e.widget().isVisible()) e.widget().render(g, mx, my); }
        } else {
            children.stream()
                    .filter(e -> e.widget().isVisible())
                    .sorted(Comparator.comparingInt(Entry::zIndex))
                    .forEach(e -> e.widget().render(g, mx, my));
        }
        if (clip) g.disableScissor();

        // Square borders only when borderRadius == 0
        if (r <= 0) {
            if (borderThickness > 0 && bColor != 0) {
                int t = borderThickness;
                g.fill(x,         y,         x + w,     y + t,     bColor);
                g.fill(x,         y + h - t, x + w,     y + h,     bColor);
                g.fill(x,         y,         x + t,     y + h,     bColor);
                g.fill(x + w - t, y,         x + w,     y + h,     bColor);
            }

            if (bTop    != 0) g.fill(x,         y,         x + w, y + 1,     bTop);
            if (bBottom != 0) g.fill(x,         y + h - 1, x + w, y + h,     bBottom);
            if (bLeft   != 0) g.fill(x,         y,         x + 1, y + h,     bLeft);
            if (bRight  != 0) g.fill(x + w - 1, y,         x + w, y + h,     bRight);
        }

        if (cornerDotSize > 0 && cornerDotColor != 0) {
            int s = cornerDotSize;
            int c = cornerDotColor;
            g.fill(x + 1,         y + 1,         x + 1 + s,         y + 1 + s,         c);
            g.fill(x + w - 1 - s, y + 1,         x + w - 1,         y + 1 + s,         c);
            g.fill(x + 1,         y + h - 1 - s, x + 1 + s,         y + h - 1,         c);
            g.fill(x + w - 1 - s, y + h - 1 - s, x + w - 1,         y + h - 1,         c);
        }

        // Absolute children — positioned each frame, drawn on top of normal flow, sorted by z-index.
        absChildren.stream()
                .filter(ae -> ae.widget().isVisible())
                .sorted(Comparator.comparingInt(AbsEntry::zIndex))
                .forEach(ae -> { positionAbsChild(ae); ae.widget().render(g, mx, my); });

        if (useShader) RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // Render tooltip for any directly-hovered child that has one
        renderHoveredChildTooltip(g, mx, my);

        // Render this panel's own tooltip when hovered (and no child tooltip was shown)
        if (tooltip != null && !tooltip.isBlank() && hovered) {
            renderTooltipBox(g, tooltip, mx, my);
        }
    }

    // ── Transition animation helpers ──────────────────────────────────────────

    private int computeCurrentBg(long now) {
        if (animStartMs == 0 || transitionDurationMs <= 0) return wasHovered ? ((hoverBackgroundSet) ? hoverBackground : background) : background;
        float t = Math.min(1f, (float)(now - animStartMs) / transitionDurationMs);
        return lerpColor(animFromBg, animToBg, t);
    }

    private float computeCurrentOpacity(long now) {
        if (animStartMs == 0 || transitionDurationMs <= 0)
            return wasHovered ? ((hoverOpacity >= 0f) ? hoverOpacity : opacity) : opacity;
        float t = Math.min(1f, (float)(now - animStartMs) / transitionDurationMs);
        return lerp(animFromOpacity, animToOpacity, t);
    }

    private int computeCurrentBorder(long now) {
        if (animStartMs == 0 || transitionDurationMs <= 0) return wasHovered ? ((hoverBorderColorSet) ? hoverBorderColor : borderColor) : borderColor;
        float t = Math.min(1f, (float)(now - animStartMs) / transitionDurationMs);
        return lerpColor(animFromBorder, animToBorder, t);
    }

    private int computeCurrentColor(long now) {
        if (animStartMs == 0 || transitionDurationMs <= 0)
            return wasHovered ? (hoverTextColorSet ? hoverTextColor : 0) : 0;
        float t = Math.min(1f, (float)(now - animStartMs) / transitionDurationMs);
        return lerpColor(animFromColor, animToColor, t);
    }

    /**
     * Propagates a text color to direct {@link TesseraLabel} and {@link TesseraButton}
     * children so the {@code color} transition affects inline text.
     */
    private void propagateColorToChildren(int color) {
        for (Entry e : children) {
            TesseraWidget w = e.widget();
            if (w instanceof TesseraLabel lbl) lbl.color(color);
            else if (w instanceof TesseraButton btn) btn.labelColor(color);
        }
    }

    private static int lerpColor(int from, int to, float t) {
        if (t <= 0f) return from;
        if (t >= 1f) return to;
        int aF = (from >> 24) & 0xFF, rF = (from >> 16) & 0xFF, gF = (from >> 8) & 0xFF, bF = from & 0xFF;
        int aT = (to   >> 24) & 0xFF, rT = (to   >> 16) & 0xFF, gT = (to   >> 8) & 0xFF, bT = to   & 0xFF;
        int a = (int)(aF + (aT - aF) * t) & 0xFF;
        int r = (int)(rF + (rT - rF) * t) & 0xFF;
        int g = (int)(gF + (gT - gF) * t) & 0xFF;
        int b = (int)(bF + (bT - bF) * t) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /**
     * Fills a rectangle with rounded corners using a scanline approach.
     * The corner arc radius is clamped to half the smaller dimension.
     *
     * @param g     GuiGraphics context
     * @param x     left edge (screen coords)
     * @param y     top edge (screen coords)
     * @param w     width
     * @param h     height
     * @param r     corner radius in pixels
     * @param color ARGB color (alpha must be in the high byte)
     */
    private static void fillRounded(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        if (r <= 0) { g.fill(x, y, x + w, y + h, color); return; }
        r = Math.min(r, Math.min(w / 2, h / 2));
        // Center horizontal strip (full width, no corners to clip)
        if (h - 2 * r > 0) g.fill(x, y + r, x + w, y + h - r, color);
        // Top and bottom arcs, one scanline at a time
        for (int dy = 0; dy < r; dy++) {
            double inner = r - Math.sqrt((double) r * r - (double) (r - dy) * (r - dy));
            int startX = (int) Math.ceil(inner);
            int lineW = w - 2 * startX;
            if (lineW <= 0) continue;
            g.fill(x + startX, y + dy,         x + startX + lineW, y + dy + 1,         color); // top arc
            g.fill(x + startX, y + h - dy - 1, x + startX + lineW, y + h - dy,         color); // bottom arc
        }
    }

    /**
     * Resolves the screen position of an absolutely-positioned child and calls
     * {@link TesseraWidget#setPosition}.  Offsets are relative to the padded area of
     * this panel.  If both {@code left} and {@code right} are set, {@code left} wins.
     * If both {@code top} and {@code bottom} are set, {@code top} wins.
     */
    private void positionAbsChild(AbsEntry ae) {
        int cx = (ae.left()   != ABS_UNSET) ? x + padLeft + ae.left()
               : (ae.right()  != ABS_UNSET) ? x + w - padRight  - ae.right()  - ae.widget().getWidth()
               : x + padLeft;
        int cy = (ae.top()    != ABS_UNSET) ? y + padTop  + ae.top()
               : (ae.bottom() != ABS_UNSET) ? y + h - padBottom - ae.bottom() - ae.widget().getHeight()
               : y + padTop;
        ae.widget().setPosition(cx, cy);
    }

    /**
     * Checks direct children for hover + tooltip, and renders the tooltip box on top.
     * Each panel handles its own immediate children; nesting works automatically.
     */
    private void renderHoveredChildTooltip(GuiGraphics g, int mx, int my) {
        for (Entry e : children) {
            if (!e.widget().isVisible()) continue;
            String tip = e.widget().getTooltip();
            if (tip == null || tip.isBlank()) continue;
            if (e.widget().bounds().contains(mx, my)) {
                renderTooltipBox(g, tip, mx, my);
                return; // one tooltip at a time
            }
        }
    }

    /**
     * Renders a small copper-bordered tooltip box near the mouse cursor.
     * Uses a Z-translate of 500 so it appears above sibling widgets.
     */
    private static void renderTooltipBox(GuiGraphics g, String text, int mx, int my) {
        var font = Minecraft.getInstance().font;
        float scale = 6f / 7f;
        int tw = (int) (font.width(text) * scale) + 10;
        int th = 14;
        int tx = mx + 6;
        int ty = my - th - 4;
        if (ty < 2) ty = my + 4;

        g.pose().pushPose();
        g.pose().translate(tx, ty, 500);
        // bg + border
        g.fill(0, 0, tw, th, TesseraPalette.BG2);
        g.fill(0, 0, tw, 1, TesseraPalette.COPPER_LO);
        g.fill(0, th - 1, tw, th, TesseraPalette.COPPER_LO);
        g.fill(0, 0, 1, th, TesseraPalette.COPPER_LO);
        g.fill(tw - 1, 0, tw, th, TesseraPalette.COPPER_LO);
        // text
        g.pose().translate(5, 4, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, TesseraPalette.CREAM, false);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Inventory picker has the highest priority — intercept before any widget.
        if (TesseraInventoryPicker.isOpen()) {
            boolean consumed = TesseraInventoryPicker.mouseClicked(mx, my, btn);
            if (consumed) return true;
            // Picker closed (click was outside it) — fall through so the widget below also reacts.
        }
        // Absolute children are visually on top — check them first (reverse paint order)
        for (int i = absChildren.size() - 1; i >= 0; i--) {
            AbsEntry ae = absChildren.get(i);
            if (!ae.widget().isVisible()) continue;
            if (ae.widget().mouseClicked(mx, my, btn)) {
                defocusAllExcept(ae.widget());
                return true;
            }
        }
        // Hit-test in descending z-index order (highest z-index = visually on top = checked first).
        List<Entry> hitOrder = children.stream()
                .sorted(Comparator.comparingInt(Entry::zIndex).reversed())
                .toList();
        TesseraWidget hit = null;
        for (Entry e : hitOrder) {
            if (!e.widget().isVisible()) continue;
            if (e.widget().mouseClicked(mx, my, btn)) { hit = e.widget(); break; }
        }
        if (hit != null) {
            final TesseraWidget hitFinal = hit;
            for (Entry e : children)      { if (e.widget() != hitFinal) e.widget().setFocused(false); }
            for (AbsEntry ae : absChildren) ae.widget().setFocused(false);
            return true;
        }
        // No child consumed the event — fire this panel's own click action if inside bounds.
        if (btn == 0 && bounds().contains(mx, my)) {
            if (draggable) {
                TesseraDragContext.startDrag(this, dragPayload, (int) mx, (int) my);
                defocusAll();
                return true;
            }
            if (onClickAction != null) {
                onClickAction.run();
                defocusAll();
                return true;
            }
        }
        // Right-click: fire onRightClick handler if inside bounds.
        if (btn == 1 && bounds().contains(mx, my) && onRightClick != null) {
            onRightClick.run();
            defocusAll();
            return true;
        }
        defocusAll();
        return false;
    }

    private void defocusAll() {
        for (Entry e : children) e.widget().setFocused(false);
        for (AbsEntry ae : absChildren) ae.widget().setFocused(false);
    }

    private void defocusAllExcept(TesseraWidget hit) {
        for (Entry e : children) e.widget().setFocused(false);
        for (AbsEntry ae : absChildren) if (ae.widget() != hit) ae.widget().setFocused(false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Absolute children may hold focus (e.g. an overlapping textarea)
        for (AbsEntry ae : absChildren) {
            if (ae.widget().keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        for (Entry e : children) {
            if (e.widget().keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        for (AbsEntry ae : absChildren) {
            if (ae.widget().charTyped(c, modifiers)) return true;
        }
        for (Entry e : children) {
            if (e.widget().charTyped(c, modifiers)) return true;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused) defocusAll();
    }

    @Override
    public void mouseReleased(double mx, double my, int btn) {
        // If a drag is active and this panel has a drop zone, check for a drop
        if (btn == 0 && TesseraDragContext.isDragging() && dropZoneHandler != null) {
            if (dropZoneHandler.dropBounds().contains(mx, my)
                    && dropZoneHandler.accepts(TesseraDragContext.payload())) {
                TesseraDragContext.setDropTarget(dropZoneHandler);
            }
        }
        for (AbsEntry ae : absChildren) {
            if (ae.widget().isVisible()) ae.widget().mouseReleased(mx, my, btn);
        }
        for (Entry e : children) {
            if (e.widget().isVisible()) e.widget().mouseReleased(mx, my, btn);
        }
    }

    @Override
    public void mouseDragged(double mx, double my, int btn) {
        for (AbsEntry ae : absChildren) {
            if (ae.widget().isVisible()) ae.widget().mouseDragged(mx, my, btn);
        }
        for (Entry e : children) {
            if (e.widget().isVisible()) e.widget().mouseDragged(mx, my, btn);
        }
    }

    @Override
    public Rect bounds() { return new Rect(x, y, w, h); }

    @Override
    public void setActive(boolean active) {
        this.active = active;
        for (Entry e : children) e.widget().setActive(active);
    }

    @Override
    public boolean isActive() { return active; }

    @Override
    public void setPosition(int x, int y) { this.x = x; this.y = y; layoutDirty = true; }

    @Override
    public void setSize(int w, int h) { this.w = w; this.h = h; layoutDirty = true; }

    @Override
    public int getWidth()  { return w; }

    @Override
    public int getHeight() { return h; }

    // ── Debug overlay support ─────────────────────────────────────────────────

    /**
     * Returns a flat list of all direct children (normal-flow + absolute) as
     * {@link TesseraWidget} instances.  Used by {@link TesseraDebugOverlay} for
     * recursive widget traversal; package-private on purpose.
     */
    java.util.List<TesseraWidget> debugChildren() {
        java.util.List<TesseraWidget> result = new java.util.ArrayList<>(children.size() + absChildren.size());
        for (Entry e : children) result.add(e.widget());
        for (AbsEntry ae : absChildren) result.add(ae.widget());
        return result;
    }

    /**
     * Natural content height: sum of children heights (column) or max child height (row/grid)
     * plus vertical paddings and border thickness.  Used by semantic container tags to
     * auto-size their height after layout so content is never clipped.
     */
    public int fitContentHeight() {
        int padV = padTop + padBottom + 2 * borderThickness;
        if (mode == Mode.COLUMN) {
            int total = 0;
            for (int i = 0; i < children.size(); i++) {
                total += children.get(i).widget().getHeight();
                if (i < children.size() - 1) total += gap;
            }
            return Math.max(total + padV, padV);
        } else {
            int maxH = 0;
            for (Entry e : children) maxH = Math.max(maxH, e.widget().getHeight());
            return maxH + padV;
        }
    }

    /**
     * Natural content width: sum of children widths + gaps + horizontal paddings + borders.
     */
    public int fitContentWidth() {
        int contentW = 0;
        if (mode == Mode.ROW) {
            for (int i = 0; i < children.size(); i++) {
                contentW += children.get(i).widget().getWidth();
                if (i < children.size() - 1) contentW += gap;
            }
        } else {
            for (Entry e : children) {
                contentW = Math.max(contentW, e.widget().getWidth());
            }
        }
        return contentW + padLeft + padRight + 2 * borderThickness;
    }
}
