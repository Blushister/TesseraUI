package com.tesseraui;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class TesseraTemplateRenderer {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int COPPER_LO = TesseraPalette.COPPER_LO;

    /** Text measurer — swappable for tests (package-private). */
    static TesseraTextMeasurer TEXT_MEASURER = TesseraMinecraftTextMeasurer.INSTANCE;

    /** Inline-level HTML tags that may appear inside block containers. */
    private static final java.util.Set<String> INLINE_TAGS =
            java.util.Set.of("strong", "b", "em", "i", "span", "a");

    private TesseraTemplateRenderer() {}

    public static TesseraPanel build(TesseraTemplate template, TesseraModel model,
                                  Map<String, Runnable> handlers,
                                  int x, int y, int w, int h) {
        return build(template, model, handlers, Map.of(), x, y, w, h);
    }

    public static TesseraPanel build(TesseraTemplate template, TesseraModel model,
                                  Map<String, Runnable> handlers,
                                  Map<String, Consumer<String>> inputHandlers,
                                  int x, int y, int w, int h) {
        return build(template, model, handlers, inputHandlers,
                (Map<String, TesseraInputState>) null, x, y, w, h);
    }

    public static TesseraPanel build(TesseraTemplate template, TesseraModel model,
                                  Map<String, Runnable> handlers,
                                  Map<String, Consumer<String>> inputHandlers,
                                  TesseraRenderContext context,
                                  int x, int y, int w, int h) {
        return build(template, model, handlers, inputHandlers,
                context != null ? context.inputStates() : null,
                x, y, w, h);
    }

    public static TesseraPanel build(TesseraTemplate template, TesseraModel model,
                                  Map<String, Runnable> handlers,
                                  Map<String, Consumer<String>> inputHandlers,
                                  Map<String, TesseraInputState> inputStates,
                                  int x, int y, int w, int h) {
        try {
            if (template == null) {
                throw new IllegalArgumentException("Tessera template is null");
            }
            if (template.root() == null) {
                throw new IllegalArgumentException("Tessera template root is null");
            }
            Deque<TesseraNode> ancestors = new ArrayDeque<>();
            // Resolve @media rules against the current GUI viewport width
            int vw = guiScaledWidth();
            TesseraStyleSheet sheet = template.styleSheet().forViewport(vw);
            return buildNode(template.root(), sheet, model,
                    handlers, inputHandlers, inputStates,
                    ancestors, x, y, w, h, 0, TesseraStyle.EMPTY);
        } catch (Exception e) {
            LOGGER.error("[TesseraUI] Template render error: {}", e.getMessage(), e);
            return buildErrorPanel(e.getMessage(), x, y, w, h);
        }
    }

    /** Returns the current GUI-scaled screen width, or MAX_VALUE in non-Minecraft contexts. */
    private static int guiScaledWidth() {
        try {
            return net.minecraft.client.Minecraft.getInstance()
                    .getWindow().getGuiScaledWidth();
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    /** Renders an in-world error panel when template processing fails. */
    private static TesseraPanel buildErrorPanel(String msg, int x, int y, int w, int h) {
        TesseraPanel err = TesseraPanel.column(x, y, w, h)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.DANGER)
                .padding(4).gap(3);
        err.add(new TesseraLabel(0, 0, w - 8, 9, "⚠ TesseraUI error")
                .color(TesseraPalette.DANGER).fontSize(6f));
        String safe = msg != null ? msg : "unknown error";
        if (safe.length() > 60) safe = safe.substring(0, 57) + "…";
        err.add(new TesseraLabel(0, 0, w - 8, 9, safe)
                .color(TesseraPalette.CREAM_DIM).fontSize(6f));
        err.add(new TesseraLabel(0, 0, w - 8, 9, "Check logs for details.")
                .color(TesseraPalette.TEXT_MUTE).fontSize(6f));
        err.layout();
        return err;
    }

    private static TesseraPanel buildNode(TesseraNode node, TesseraStyleSheet sheet,
                                       TesseraModel model, Map<String, Runnable> handlers,
                                       Map<String, Consumer<String>> inputHandlers,
                                       Map<String, TesseraInputState> inputStates,
                                       Deque<TesseraNode> ancestors,
                                       int x, int y, int w, int h, int depth,
                                       TesseraStyle inherited) {
        if (depth > 64) {
            LOGGER.warn("[TesseraUI] Max depth reached — node ignored");
            return TesseraPanel.column(x, y, w, h);
        }
        // Tables always use the dedicated layout engine regardless of nesting depth
        if ("table".equals(node.tag())) {
            return buildTablePanel(node, sheet, model, handlers, inputHandlers, inputStates,
                    ancestors, x, y, w, h, depth, inherited);
        }
        TesseraStyle style = sheet.resolve(node, ancestors);
        TesseraStyle hoverStyle = sheet.resolveHover(node, ancestors);

        boolean isRow  = isRowMode(node, style);
        boolean isGrid = node.tag().equals("grid") || (style.gridTemplateColumns != null);

        int padT = style.paddingTop    != TesseraStyle.UNSET ? style.paddingTop    : 0;
        int padR = style.paddingRight  != TesseraStyle.UNSET ? style.paddingRight  : 0;
        int padB = style.paddingBottom != TesseraStyle.UNSET ? style.paddingBottom : 0;
        int padL = style.paddingLeft   != TesseraStyle.UNSET ? style.paddingLeft   : 0;

        int gapVal = style.gap != TesseraStyle.UNSET ? style.gap : 0;

        int borderT = (style.border != TesseraStyle.UNSET) ? style.border : 0;

        TesseraPanel panel;
        if (isGrid) {
            int cols = parseIntAttr(node.attr("cols"), 2);
            panel = TesseraPanel.grid(cols, x, y, w, h);
            if (style.gridTemplateColumns != null) panel.gridTemplateColumns(style.gridTemplateColumns);
        } else if (isRow) {
            panel = TesseraPanel.row(x, y, w, h);
        } else {
            panel = TesseraPanel.column(x, y, w, h);
        }

        panel.gap(gapVal);
        if (padT > 0 || padR > 0 || padB > 0 || padL > 0)
            panel.padding(padT, padR, padB, padL);
        if (style.background  != TesseraStyle.UNSET) panel.background(style.background);
        if (style.borderRadius != TesseraStyle.UNSET && style.borderRadius > 0)
            panel.borderRadius(style.borderRadius);
        if (style.border     != TesseraStyle.UNSET && style.borderColor != TesseraStyle.UNSET)
            panel.border(style.border, style.borderColor);
        if (style.justifyContent != null) panel.justifyContent(style.justifyContent);
        if (style.alignItems     != null) panel.alignItems(style.alignItems);
        if ("wrap".equals(style.flexWrap)) panel.wrap(true);
        if ("hidden".equals(style.overflow)) panel.clip(true);
        if (style.borderTopColor    != TesseraStyle.UNSET) panel.borderSide("top",    style.borderTopColor);
        if (style.borderBottomColor != TesseraStyle.UNSET) panel.borderSide("bottom", style.borderBottomColor);
        if (style.borderLeftColor   != TesseraStyle.UNSET) panel.borderSide("left",   style.borderLeftColor);
        if (style.borderRightColor  != TesseraStyle.UNSET) panel.borderSide("right",  style.borderRightColor);

        if (style.opacity != TesseraStyle.UNSET_F) panel.opacity(style.opacity);

        if (hoverStyle.background != TesseraStyle.UNSET) panel.hoverBackground(hoverStyle.background);
        if (hoverStyle.borderColor != TesseraStyle.UNSET) panel.hoverBorder(hoverStyle.borderColor);
        if (hoverStyle.borderTopColor    != TesseraStyle.UNSET) panel.hoverBorderSide("top",    hoverStyle.borderTopColor);
        if (hoverStyle.borderBottomColor != TesseraStyle.UNSET) panel.hoverBorderSide("bottom", hoverStyle.borderBottomColor);
        if (hoverStyle.borderLeftColor   != TesseraStyle.UNSET) panel.hoverBorderSide("left",   hoverStyle.borderLeftColor);
        if (hoverStyle.borderRightColor  != TesseraStyle.UNSET) panel.hoverBorderSide("right",  hoverStyle.borderRightColor);
        if (hoverStyle.opacity != TesseraStyle.UNSET_F) panel.hoverOpacity(hoverStyle.opacity);
        if (hoverStyle.color != TesseraStyle.UNSET) panel.hoverColor(hoverStyle.color);

        // Apply CSS transition to the panel
        if (style.transitionDurationMs > 0)
            panel.transition(style.transitionDurationMs, style.transitionProperty != null ? style.transitionProperty : "all");

        // CSS multi-property transitions and @keyframes animations (v2.3)
        if (style.transitions != null)
            panel.cssTransitions(style.transitions, style, hoverStyle);
        if (style.animations != null)
            panel.cssAnimation(style.animations, sheet);

        if (style.cornerDotSize != TesseraStyle.UNSET && style.cornerDotSize > 0) {
            int dotColor = style.cornerDotColor != TesseraStyle.UNSET ? style.cornerDotColor
                         : style.borderColor != TesseraStyle.UNSET ? style.borderColor : COPPER_LO;
            panel.cornerDots(style.cornerDotSize, dotColor);
        } else if (node.classNames().contains("arc-panel")) {
            int dotColor = style.borderColor != TesseraStyle.UNSET ? style.borderColor : COPPER_LO;
            panel.cornerDots(4, dotColor);
        }

        String onClickHandler = node.onClickHandler();
        if (!onClickHandler.isEmpty() && handlers.containsKey(onClickHandler))
            panel.onClick(handlers.get(onClickHandler));

        // oncontextmenu attribute → wire right-click handler
        String onCtxHandler = node.attr("oncontextmenu");
        if (!onCtxHandler.isEmpty() && handlers.containsKey(onCtxHandler))
            panel.onRightClick(handlers.get(onCtxHandler));

        // Drag & Drop attributes
        String draggableAttr = node.attr("draggable");
        if ("true".equalsIgnoreCase(draggableAttr)) {
            panel.draggable(true);
            String payloadAttr = node.attr("drag-payload");
            if (!payloadAttr.isBlank()) {
                panel.dragPayload(TesseraBindingResolver.resolve(payloadAttr, model));
            }
        }

        int innerW = w - padL - padR;
        int innerH = h - padT - padB;
        if ("border-box".equals(style.boxSizing)) {
            innerW -= 2 * borderT;
            innerH -= 2 * borderT;
        }

        boolean stretchChildWidth  = !isRow && !isGrid;
        boolean stretchChildHeight = isRow;

        // Compute the inheritable context to pass down to children.
        // Effective style = inherited defaults merged under explicit node style.
        TesseraStyle nodeEffective = inherited.merge(style);
        TesseraStyle childInherited = extractInheritable(nodeEffective);

        ancestors.push(node);
        try {
            // ── Direct text content ───────────────────────────────────────────
            // Handles leaf-like containers such as <td>Name</td> and <div>Text</div>
            // where text lives in node.text() rather than a child element node.
            String directText = node.text() != null ? node.text().trim() : "";
            if (!directText.isEmpty()) {
                String resolvedDT = TesseraBindingResolver.resolve(directText, model);
                if (!resolvedDT.isBlank()) {
                    // UA defaults for <th>: bold + centre (CSS in nodeEffective overrides these)
                    float  dtSize   = nodeEffective.fontSize   != TesseraStyle.UNSET_F ? nodeEffective.fontSize   : 7f;
                    int    dtWeight = nodeEffective.fontWeight != TesseraStyle.UNSET   ? nodeEffective.fontWeight
                                    : "th".equals(node.tag()) ? 700 : 400;
                    String dtAlign  = nodeEffective.textAlign  != null                 ? nodeEffective.textAlign
                                    : "th".equals(node.tag()) ? "center" : "left";
                    int    dtColor  = nodeEffective.color      != TesseraStyle.UNSET   ? nodeEffective.color      : TesseraPalette.CREAM;
                    int    dtLblH   = (int) Math.ceil(dtSize) + 4;
                    var    dtLbl    = new TesseraLabel(0, 0, Math.max(1, innerW), dtLblH, resolvedDT);
                    dtLbl.color(dtColor).fontSize(dtSize).fontWeight(dtWeight).textAlign(dtAlign);
                    if (nodeEffective.textDecoration != null) dtLbl.textDecoration(nodeEffective.textDecoration);
                    panel.add(dtLbl, 0, null, false, 0, 0, 0, 0);
                }
            }

            int nthIndex = 0; // 1-based child index for :nth-child selectors
            for (TesseraNode child : expandChildren(node, sheet, model)) {
                nthIndex++;
                // Inject __nth-index so TesseraSelector.Segment can evaluate :nth-child(...)
                TesseraNode indexedChild = withAttr(child, "__nth-index", String.valueOf(nthIndex));

                TesseraStyle childStyle = sheet.resolve(indexedChild, ancestors);
                // Absolutely-positioned children must NOT inherit parent width/height.
                // They size themselves from explicit CSS or natural text content.
                boolean isAbsChild = "absolute".equals(childStyle.position);
                TesseraWidget widget = buildWidget(indexedChild, sheet, model, handlers, inputHandlers, inputStates, ancestors,
                        innerW, innerH,
                        isAbsChild ? false : stretchChildWidth,
                        isAbsChild ? false : stretchChildHeight,
                        depth + 1, childInherited);
                if (widget == null) continue;
                // <td>/<th> default to flex:1 (equal column distribution) unless overridden by CSS
                String childTag = indexedChild.tag();
                // Route absolutely-positioned children outside the normal flex flow
                if ("absolute".equals(childStyle.position)) {
                    int aTop    = childStyle.top    != TesseraStyle.UNSET ? childStyle.top    : TesseraPanel.ABS_UNSET;
                    int aLeft   = childStyle.left   != TesseraStyle.UNSET ? childStyle.left   : TesseraPanel.ABS_UNSET;
                    int aRight  = childStyle.right  != TesseraStyle.UNSET ? childStyle.right  : TesseraPanel.ABS_UNSET;
                    int aBottom = childStyle.bottom != TesseraStyle.UNSET ? childStyle.bottom : TesseraPanel.ABS_UNSET;
                    panel.addAbsolute(widget, aTop, aLeft, aRight, aBottom);
                    continue; // not part of normal flex flow
                }

                boolean isTdTh = "td".equals(childTag) || "th".equals(childTag);
                boolean isVirtualList = "virtual-list".equals(childTag)
                        && childStyle.height == TesseraStyle.UNSET
                        && childStyle.heightCalc == null
                        && !childStyle.heightPercent;
                // flex-grow: explicit CSS > td/th default (1) > 0
                float fGrow   = childStyle.flexGrow   != TesseraStyle.UNSET_F ? childStyle.flexGrow
                              : isVirtualList ? 1f
                              : isTdTh ? 1f : 0f;
                // flex-shrink: explicit CSS > 1 when item can grow, 0 otherwise
                float fShrink = childStyle.flexShrink != TesseraStyle.UNSET_F ? childStyle.flexShrink
                              : fGrow > 0 ? 1f : 0f;
                int   fBasis  = childStyle.flexBasis != TesseraStyle.UNSET ? childStyle.flexBasis
                              : isVirtualList ? 0 : TesseraStyle.UNSET;
                int   fOrder  = childStyle.order  != TesseraStyle.UNSET ? childStyle.order  : 0;
                int   fZIndex = childStyle.zIndex  != TesseraStyle.UNSET ? childStyle.zIndex : 0;
                int mT = childStyle.marginTop    != TesseraStyle.UNSET ? childStyle.marginTop    : 0;
                int mR = childStyle.marginRight  != TesseraStyle.UNSET ? childStyle.marginRight  : 0;
                int mB = childStyle.marginBottom != TesseraStyle.UNSET ? childStyle.marginBottom : 0;
                int mL = childStyle.marginLeft   != TesseraStyle.UNSET ? childStyle.marginLeft   : 0;
                panel.add(widget, fGrow, fShrink, fBasis, fOrder, fZIndex,
                        childStyle.alignSelf, childStyle.marginTopAuto, mT, mR, mB, mL);
            }
        } finally {
            ancestors.pop();
        }
        panel.layout();
        return panel;
    }

    private static boolean isRowMode(TesseraNode node, TesseraStyle style) {
        if (node.tag().equals("row") || node.tag().equals("tr")) return true;
        return "row".equals(style.flexDirection);
    }

    private static List<TesseraNode> expandChildren(TesseraNode parent, TesseraStyleSheet sheet, TesseraModel model) {
        return parent.children().stream()
            .flatMap(child -> {
                // v-if: evaluated FIRST — if false, emit nothing (no widget created, no layout space)
                String vIf = child.vIf();
                if (!vIf.isEmpty()) {
                    String expr = vIf.trim();
                    if (expr.startsWith("{{") && expr.endsWith("}}")) {
                        expr = expr.substring(2, expr.length() - 2).trim();
                    }
                    if (!TesseraBindingResolver.evaluateCondition(expr, model)) {
                        return java.util.stream.Stream.of();
                    }
                }

                // v-show: evaluate BEFORE v-for so hidden marker propagates to all expanded clones.
                // If false, inject __vshow-hidden=true into the node used for expansion.
                TesseraNode effectiveChild = child;
                String vShow = child.vShow();
                if (!vShow.isEmpty()) {
                    String showExpr = vShow.trim();
                    if (showExpr.startsWith("{{") && showExpr.endsWith("}}")) {
                        showExpr = showExpr.substring(2, showExpr.length() - 2).trim();
                    }
                    if (!TesseraBindingResolver.evaluateCondition(showExpr, model)) {
                        java.util.Map<String, String> newAttrs = new java.util.HashMap<>(child.attrs());
                        newAttrs.put("__vshow-hidden", "true");
                        effectiveChild = new TesseraNode(child.tag(), newAttrs, child.children(), child.text());
                    }
                }

                // v-for: expand into multiple nodes (v-show hidden marker already on effectiveChild)
                // <virtual-list> handles its own v-for internally — skip generic expansion for it.
                String vFor = effectiveChild.vFor();
                if (!vFor.isEmpty() && vFor.contains(" in ")
                        && !effectiveChild.tag().equals("virtual-list")) {
                    String[] parts = vFor.split(" in ", 2);
                    String varName = parts[0].trim();
                    String listKey = parts[1].trim();
                    String resolved = model.resolve(listKey);
                    if (resolved == null) return java.util.stream.Stream.of();
                    int count = parseIntAttr(resolved, 0);
                    java.util.List<TesseraModel> items = new java.util.ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        final int idx = i;
                        items.add(k -> model.resolve(varName + "." + k + "." + idx));
                    }
                    return TesseraForEach.expand(effectiveChild, items, varName).stream();
                }

                // If v-show was false and no v-for, emit the hidden-marker node directly
                if (effectiveChild != child) {
                    return java.util.stream.Stream.of(effectiveChild);
                }

                boolean hasBinding = child.attrs().values().stream().anyMatch(v -> v.contains("{{"))
                        || child.text().contains("{{");
                if (hasBinding) {
                    return java.util.stream.Stream.of(TesseraForEach.resolveAttrs(child, model));
                }
                return java.util.stream.Stream.of(child);
            })
            .toList();
    }

    private static int resolveLength(int raw, boolean isPercent, int basis, int fallback) {
        if (raw == TesseraStyle.UNSET) return fallback;
        if (isPercent) {
            if (basis <= 0) return fallback;
            return basis * raw / 100;
        }
        return raw;
    }

    /**
     * Measures the natural display width of {@code text} including horizontal padding,
     * then clamps the result to {@code [minW, maxW]} if those constraints are set.
     */
    private static int measureContentWidth(String text, TesseraStyle style, float fontSizePx,
                                           int fontWeight, int minW, int maxW) {
        String displayed = TesseraTextStyling.transform(text != null ? text : "", style.textTransform);
        int textW = TEXT_MEASURER.measureWidth(displayed, style.fontFamily, fontWeight, fontSizePx);
        int padH = (style.paddingLeft  != TesseraStyle.UNSET ? style.paddingLeft  : 0)
                 + (style.paddingRight != TesseraStyle.UNSET ? style.paddingRight : 0);
        int w = textW + padH;
        if (minW != TesseraStyle.UNSET && w < minW) w = minW;
        if (maxW != TesseraStyle.UNSET && w > maxW) w = maxW;
        return Math.max(w, 1);
    }

    private static TesseraWidget buildWidget(TesseraNode node, TesseraStyleSheet sheet,
                                             TesseraModel model, Map<String, Runnable> handlers,
                                             Map<String, Consumer<String>> inputHandlers,
                                             Map<String, TesseraInputState> inputStates,
                                             Deque<TesseraNode> ancestors,
                                             int availW, int availH,
                                             boolean inheritWidth, boolean inheritHeight,
                                             int depth,
                                             TesseraStyle inherited) {
        TesseraStyle style = inherited.merge(sheet.resolve(node, ancestors)); // apply CSS inheritance

        // calc() — resolved here because availW/availH are only known at layout time
        int wRaw = style.widthCalc  != null ? TesseraCssParser.evalCalc(style.widthCalc,  availW)
                 : resolveLength(style.width,  style.widthPercent,  availW, TesseraStyle.UNSET);
        int hRaw = style.heightCalc != null ? TesseraCssParser.evalCalc(style.heightCalc, availH)
                 : resolveLength(style.height, style.heightPercent, availH, TesseraStyle.UNSET);

        int wVal = wRaw != TesseraStyle.UNSET ? wRaw
                 : inheritWidth  && availW > 0 ? availW
                 : 0;
        int hVal = hRaw != TesseraStyle.UNSET ? hRaw
                 : inheritHeight && availH > 0 ? availH
                 : naturalHeight(style);

        int minW = resolveLength(style.minWidth, style.minWidthPercent, availW, TesseraStyle.UNSET);
        int maxW = resolveLength(style.maxWidth, style.maxWidthPercent, availW, TesseraStyle.UNSET);
        int minH = resolveLength(style.minHeight, style.minHeightPercent, availH, TesseraStyle.UNSET);
        int maxH = resolveLength(style.maxHeight, style.maxHeightPercent, availH, TesseraStyle.UNSET);

        if (minW != TesseraStyle.UNSET && wVal < minW) wVal = minW;
        if (maxW != TesseraStyle.UNSET && wVal > maxW) wVal = maxW;
        if (minH != TesseraStyle.UNSET && hVal < minH) hVal = minH;
        if (maxH != TesseraStyle.UNSET && hVal > maxH) hVal = maxH;

        // CSS display:none — widget not created, no layout space (different from v-show)
        if ("none".equals(style.display)) return null;

        float fontSizePx = style.fontSize != TesseraStyle.UNSET_F ? style.fontSize : 7f;
        int fontWeight = style.fontWeight != TesseraStyle.UNSET ? style.fontWeight : 400;
        float opacityVal = style.opacity != TesseraStyle.UNSET_F ? style.opacity : 1f;

        TesseraWidget widget = switch (node.tag()) {
            // ── Virtual text nodes emitted by the parser for tail text ────────
            case TesseraNode.TEXT_TAG -> {
                String text = resolveNodeText(node, model);
                if (text == null || text.isBlank()) yield null;
                int color  = style.color    != TesseraStyle.UNSET   ? style.color    : TesseraPalette.CREAM;
                float sz   = style.fontSize != TesseraStyle.UNSET_F ? style.fontSize : 7f;
                int wt     = style.fontWeight != TesseraStyle.UNSET  ? style.fontWeight : 400;
                int lw     = inheritWidth && availW > 0 ? availW : 80;
                var lbl    = new TesseraLabel(0, 0, lw, 0, text.trim()).color(color);
                if (style.fontFamily != null) lbl.font(style.fontFamily);
                lbl.fontSize(sz).fontWeight(wt).wrap(true);
                yield lbl;
            }

            // ── Explicit layout containers (div / row / col / grid) ──────────
            case "div", "row", "col", "grid" -> {
                boolean explicitW = wRaw != TesseraStyle.UNSET;
                boolean explicitH = hRaw != TesseraStyle.UNSET;
                // When inheritWidth=false AND no explicit width (e.g. position:absolute child):
                // measure the natural text width so right/bottom offsets compute correctly.
                int initialW;
                if (wVal > 0) {
                    initialW = wVal;
                } else if (!inheritWidth) {
                    initialW = measureNaturalDivWidth(node, style, fontSizePx, fontWeight);
                    if (initialW <= 0) initialW = availW; // fallback
                } else {
                    initialW = availW;
                }
                TesseraPanel built = buildNode(node, sheet, model, handlers, inputHandlers, inputStates, ancestors, 0, 0, initialW, hVal, depth, inherited);
                if (!explicitW && !inheritWidth) {
                    int natural = built.fitContentWidth();
                    if (natural > 0 && natural < built.getWidth()) {
                        built.setSize(natural, built.getHeight());
                        built.layout();
                    }
                }
                // Auto-height: grow to fit content when the parent does not stretch this child
                if (!explicitH && !inheritHeight) {
                    int natH = built.fitContentHeight();
                    if (natH > built.getHeight()) {
                        built.setSize(built.getWidth(), natH);
                        built.layout();
                    }
                }
                yield built;
            }

            // ── Semantic containers — auto-size height to content ─────────────
            case "section", "article", "main", "nav", "header", "footer",
                 "ul", "ol" -> {
                boolean explicitW = wRaw != TesseraStyle.UNSET;
                boolean explicitH = hRaw != TesseraStyle.UNSET;
                int initialW = wVal > 0 ? wVal : availW;
                TesseraPanel built = buildNode(node, sheet, model, handlers, inputHandlers, inputStates, ancestors, 0, 0, initialW, hVal, depth, inherited);
                // Auto-width (same logic as div/col)
                if (!explicitW && !inheritWidth) {
                    int naturalW = built.fitContentWidth();
                    if (naturalW > 0 && naturalW < built.getWidth()) {
                        built.setSize(naturalW, built.getHeight());
                        built.layout();
                    }
                }
                // Auto-height: semantic containers grow to fit their content,
                // so wrapped <li> items and nested <p>/<h*> elements are never clipped.
                if (!explicitH) {
                    int naturalH = built.fitContentHeight();
                    if (naturalH > built.getHeight()) {
                        built.setSize(built.getWidth(), naturalH);
                        built.layout();
                    }
                }
                yield built;
            }

            // ── Table ─────────────────────────────────────────────────────────
            case "table" -> {
                int tW = wVal > 0 ? wVal : availW;
                yield buildTablePanel(node, sheet, model, handlers, inputHandlers, inputStates,
                        ancestors, 0, 0, tW, hVal, depth, inherited);
            }

            // Table section wrappers — transparent column containers
            case "thead", "tbody", "tfoot" -> {
                boolean explicitH = hRaw != TesseraStyle.UNSET;
                int sW = wVal > 0 ? wVal : availW;
                TesseraPanel built = buildNode(node, sheet, model, handlers, inputHandlers, inputStates,
                        ancestors, 0, 0, sW, hVal, depth, inherited);
                if (!explicitH) {
                    int natH = built.fitContentHeight();
                    if (natH > built.getHeight()) { built.setSize(built.getWidth(), natH); built.layout(); }
                }
                yield built;
            }

            // Table row — row-mode container (isRowMode already returns true for "tr")
            case "tr" -> {
                int rW = wVal > 0 ? wVal : availW;
                TesseraPanel built = buildNode(node, sheet, model, handlers, inputHandlers, inputStates,
                        ancestors, 0, 0, rW, hVal, depth, inherited);
                if (hRaw == TesseraStyle.UNSET) {
                    int natH = built.fitContentHeight();
                    if (natH > built.getHeight()) { built.setSize(built.getWidth(), natH); built.layout(); }
                }
                yield built;
            }

            // Table cell — column container; width comes from parent tr via flex:1
            case "td", "th" -> {
                boolean explicitH = hRaw != TesseraStyle.UNSET;
                int cW = wVal > 0 ? wVal : availW;
                TesseraPanel built = buildNode(node, sheet, model, handlers, inputHandlers, inputStates,
                        ancestors, 0, 0, cW, hVal, depth, inherited);
                if (!explicitH) {
                    int natH = built.fitContentHeight();
                    if (natH > built.getHeight()) { built.setSize(built.getWidth(), natH); built.layout(); }
                }
                yield built;
            }

            // ── Image ─────────────────────────────────────────────────────────
            case "img" -> {
                int imgW = wVal > 0 ? wVal : (hVal > 0 ? hVal : 16);
                int imgH = hVal > 0 ? hVal : imgW;
                var icon = new TesseraIcon(0, 0, imgW, imgH);
                String src = TesseraBindingResolver.resolve(node.attr("src"), model);
                if (!src.isEmpty()) {
                    net.minecraft.resources.ResourceLocation loc;
                    if (src.contains(":")) {
                        String[] parts = src.split(":", 2);
                        loc = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
                    } else {
                        loc = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", src);
                    }
                    icon.texture(loc);
                }
                int tint = style.color != TesseraStyle.UNSET ? style.color : 0xFFFFFFFF;
                icon.tint(tint).size(imgW, imgH);
                String handler = node.onClickHandler();
                if (!handler.isEmpty() && handlers.containsKey(handler))
                    icon.onClick(handlers.get(handler));
                yield icon;
            }

            case "input" -> {
                String placeholder = TesseraBindingResolver.resolve(node.attr("placeholder"), model);
                String initialValue = TesseraBindingResolver.resolve(node.attr("value"), model);
                int maxLen = parseIntAttr(node.attr("maxlength"), 64);
                int iw = wVal > 0 ? wVal : (inheritWidth && availW > 0 ? availW : 100);
                int ih = hVal > 0 ? hVal : (style.fontSize != TesseraStyle.UNSET_F && style.fontSize > 0 ? (int) style.fontSize + 6 : 14);
                var input = new TesseraInput(0, 0, iw, ih);
                String idAttr = node.attr("id");
                if (idAttr != null && !idAttr.isEmpty()) input.inputId(idAttr);
                if (inputStates != null && idAttr != null && !idAttr.isEmpty()) {
                    TesseraInputState st = inputStates.get(idAttr);
                    if (st == null) {
                        st = new TesseraInputState();
                        if (initialValue != null && !initialValue.isEmpty()) st.text = initialValue;
                        inputStates.put(idAttr, st);
                    }
                    input.state(st);
                } else if (initialValue != null && !initialValue.isEmpty()) {
                    input.text(initialValue);
                }
                if ((idAttr == null || idAttr.isEmpty()) && inputStates != null) {
                    LOGGER.warn("[TesseraUI] <input> without id cannot persist state across rebuilds");
                }
                if (placeholder != null && !placeholder.isEmpty()) input.placeholder(placeholder);
                input.maxLength(maxLen);
                configureInputSuggestions(input, node, model);
                if (style.background  != TesseraStyle.UNSET) input.bgColor(style.background);
                if (style.borderColor != TesseraStyle.UNSET) input.borderColor(style.borderColor);
                if (style.color       != TesseraStyle.UNSET) input.textColor(style.color);
                int padHv = style.paddingLeft != TesseraStyle.UNSET ? style.paddingLeft : 5;
                int padVv = style.paddingTop  != TesseraStyle.UNSET ? style.paddingTop  : 3;
                input.padding(padHv, padVv);
                if (style.fontFamily != null) input.font(style.fontFamily, fontSizePx);
                else if (style.fontSize != TesseraStyle.UNSET_F) input.font(null, fontSizePx);
                input.fontWeight(fontWeight);
                String onInputName  = node.attr("oninput");
                String onSubmitName = node.attr("onsubmit");
                if (onInputName != null && !onInputName.isEmpty() && inputHandlers != null && inputHandlers.containsKey(onInputName))
                    input.onChange(inputHandlers.get(onInputName));
                if (onSubmitName != null && !onSubmitName.isEmpty() && inputHandlers != null && inputHandlers.containsKey(onSubmitName))
                    input.onSubmit(inputHandlers.get(onSubmitName));
                // :focus pseudo-class — override the focused-border colour
                TesseraStyle focusStyle = sheet.resolveFocus(node, ancestors);
                if (focusStyle.borderColor != TesseraStyle.UNSET)
                    input.focusBorderColor(focusStyle.borderColor);
                // Focus management: register unless tabindex="-1"
                String tabIndexAttr = node.attr("tabindex");
                if (!"-1".equals(tabIndexAttr)) TesseraFocusManager.register(input);
                yield input;
            }

            case "textarea" -> {
                String taPlaceholder = TesseraBindingResolver.resolve(node.attr("placeholder"), model);
                String taInitValue   = TesseraBindingResolver.resolve(node.attr("value"),       model);
                int taMaxLen = parseIntAttr(node.attr("maxlength"), 4096);
                int taRows   = parseIntAttr(node.attr("rows"), 4);
                int taw = wVal > 0 ? wVal : (inheritWidth && availW > 0 ? availW : 100);
                // Use CSS height if provided, otherwise derive from rows attribute
                float taFsPx = style.fontSize != TesseraStyle.UNSET_F ? style.fontSize : 7f;
                String taFam = style.fontFamily;
                float taSc   = taFsPx / TesseraFonts.naturalPx(taFam);
                int taLh     = (int) Math.ceil(8 * taSc) + 2;
                int taPadV   = style.paddingTop  != TesseraStyle.UNSET ? style.paddingTop  : 3;
                int tah = hRaw != TesseraStyle.UNSET ? hVal : (taRows * taLh + taPadV * 2 + 2);
                var ta = new TesseraTextArea(0, 0, taw, tah);
                String idAttr = node.attr("id");
                if (inputStates != null && idAttr != null && !idAttr.isEmpty()) {
                    TesseraInputState st = inputStates.get(idAttr);
                    if (st == null) {
                        st = new TesseraInputState();
                        if (taInitValue != null && !taInitValue.isEmpty()) st.text = taInitValue;
                        inputStates.put(idAttr, st);
                    }
                    ta.state(st);
                } else if (taInitValue != null && !taInitValue.isEmpty()) {
                    ta.text(taInitValue);
                }
                if ((idAttr == null || idAttr.isEmpty()) && inputStates != null) {
                    LOGGER.warn("[TesseraUI] <textarea> without id cannot persist state across rebuilds");
                }
                if (taPlaceholder != null && !taPlaceholder.isEmpty()) ta.placeholder(taPlaceholder);
                ta.maxLength(taMaxLen);
                if (style.background  != TesseraStyle.UNSET) ta.bgColor(style.background);
                if (style.borderColor != TesseraStyle.UNSET) ta.borderColor(style.borderColor);
                if (style.color       != TesseraStyle.UNSET) ta.textColor(style.color);
                int taPadH = style.paddingLeft != TesseraStyle.UNSET ? style.paddingLeft : 5;
                ta.padding(taPadH, taPadV);
                if (taFam != null) ta.font(taFam, taFsPx);
                else if (style.fontSize != TesseraStyle.UNSET_F) ta.font(null, taFsPx);
                ta.fontWeight(fontWeight);
                String taOnInput = node.attr("oninput");
                if (!taOnInput.isEmpty() && inputHandlers != null && inputHandlers.containsKey(taOnInput))
                    ta.onChange(inputHandlers.get(taOnInput));
                TesseraStyle taFocusStyle = sheet.resolveFocus(node, ancestors);
                if (taFocusStyle.borderColor != TesseraStyle.UNSET) ta.focusBorderColor(taFocusStyle.borderColor);
                // Focus management: register unless tabindex="-1"
                String taTabIndex = node.attr("tabindex");
                if (!"-1".equals(taTabIndex)) TesseraFocusManager.register(ta);
                yield ta;
            }

            case "button" -> {
                String text = resolveNodeText(node, model);
                int bw;
                if (wRaw != TesseraStyle.UNSET) {
                    bw = wVal;
                } else {
                    bw = measureContentWidth(text, style, fontSizePx, fontWeight, minW, maxW);
                }
                var btn = new TesseraButton(0, 0, bw, hVal).label(text);
                if (style.background != TesseraStyle.UNSET) btn.bgColor(style.background);
                if (style.color      != TesseraStyle.UNSET) btn.labelColor(style.color);
                if (style.textAlign  != null)             btn.textAlign(style.textAlign);
                if (style.fontFamily != null)             btn.font(style.fontFamily);
                btn.fontSize(fontSizePx).fontWeight(fontWeight).opacity(opacityVal);
                if (style.textTransform != null)          btn.textTransform(style.textTransform);
                // Apply CSS :hover background (if defined)
                TesseraStyle btnHoverStyle = sheet.resolveHover(node, ancestors);
                if (btnHoverStyle.background != TesseraStyle.UNSET) btn.hoverBgColor(btnHoverStyle.background);
                String handler = node.onClickHandler();
                if (!handler.isEmpty() && handlers.containsKey(handler))
                    btn.onClick(handlers.get(handler));
                else warnMissingHandler("onclick", handler, node);
                yield btn;
            }

            case "label" -> {
                String text = resolveNodeText(node, model);
                int color   = style.color != TesseraStyle.UNSET ? style.color : TesseraPalette.CREAM;
                int lw;
                if (wVal > 0) {
                    lw = wVal;
                } else if (wRaw == TesseraStyle.UNSET) {
                    // no explicit CSS width → measure the content
                    lw = measureContentWidth(text, style, fontSizePx, fontWeight, minW, maxW);
                } else {
                    lw = 80; // width: 0 explicit or fallback
                }
                // wrap = true when white-space:normal is set explicitly; height is then computed
                boolean doWrap = "normal".equals(style.whiteSpace);
                int labelH = doWrap ? 0 : hVal; // 0 → recomputeLines() will set the height
                var lbl = new TesseraLabel(0, 0, lw, labelH, text).color(color);
                if (style.textAlign  != null) lbl.textAlign(style.textAlign);
                if (style.fontFamily != null) lbl.font(style.fontFamily);
                lbl.fontSize(fontSizePx).fontWeight(fontWeight).opacity(opacityVal);
                if (style.textTransform != null) lbl.textTransform(style.textTransform);
                if ("hidden".equals(style.overflow)) lbl.clipOverflow(true);
                if (style.textDecoration != null) lbl.textDecoration(style.textDecoration);
                if (doWrap) lbl.wrap(true); // must be set AFTER fontSize/fontFamily/width
                yield lbl;
            }

            // ── Semantic block: paragraph ─────────────────────────────────────
            case "p" -> {
                int color = style.color != TesseraStyle.UNSET ? style.color : TesseraPalette.CREAM;
                int lw    = wVal > 0 ? wVal : (inheritWidth && availW > 0 ? availW : 80);
                // Inline mixed content → TesseraRichLabel for correct styling per-segment.
                if (hasInlineChildren(node)) {
                    TesseraStyle eff = inherited.merge(style);
                    ancestors.push(node);
                    List<TesseraRichLabel.TextRun> runs;
                    try { runs = collectRuns(node, sheet, model, eff, ancestors); }
                    finally { ancestors.pop(); }
                    if (!runs.isEmpty()) yield new TesseraRichLabel(0, 0, lw, 0, runs).opacity(opacityVal);
                }
                String text = node.attr("data-i18n").isBlank() ? flattenText(node, model) : resolveNodeText(node, model);
                var lbl = new TesseraLabel(0, 0, lw, 0, text).color(color);
                if (style.textAlign  != null) lbl.textAlign(style.textAlign);
                if (style.fontFamily != null) lbl.font(style.fontFamily);
                lbl.fontSize(fontSizePx).fontWeight(fontWeight).opacity(opacityVal);
                if (style.textTransform != null) lbl.textTransform(style.textTransform);
                if (style.textDecoration != null) lbl.textDecoration(style.textDecoration);
                if (!"nowrap".equals(style.whiteSpace)) lbl.wrap(true);
                yield lbl;
            }

            // ── Semantic headings h1–h6 ───────────────────────────────────────
            case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                int color   = style.color != TesseraStyle.UNSET ? style.color : TesseraPalette.CREAM;
                int lw      = wVal > 0 ? wVal : (inheritWidth && availW > 0 ? availW : 80);
                float hSize = style.fontSize != TesseraStyle.UNSET_F ? style.fontSize
                        : switch (node.tag()) {
                            case "h1" -> 14f; case "h2" -> 12f; case "h3" -> 10f;
                            case "h4" -> 9f;  case "h5" -> 8f;  default   -> 7f;
                        };
                int hWeight = style.fontWeight != TesseraStyle.UNSET ? style.fontWeight : 700;
                // Inline mixed content → TesseraRichLabel
                if (hasInlineChildren(node)) {
                    TesseraStyle eff = inherited.merge(style);
                    if (eff.fontSize   == TesseraStyle.UNSET_F) eff.fontSize   = hSize;
                    if (eff.fontWeight == TesseraStyle.UNSET)   eff.fontWeight = hWeight;
                    ancestors.push(node);
                    List<TesseraRichLabel.TextRun> runs;
                    try { runs = collectRuns(node, sheet, model, eff, ancestors); }
                    finally { ancestors.pop(); }
                    if (!runs.isEmpty()) yield new TesseraRichLabel(0, 0, lw, 0, runs).opacity(opacityVal);
                }
                String text = node.attr("data-i18n").isBlank() ? flattenText(node, model) : resolveNodeText(node, model);
                var lbl = new TesseraLabel(0, 0, lw, 0, text).color(color);
                if (style.textAlign  != null) lbl.textAlign(style.textAlign);
                if (style.fontFamily != null) lbl.font(style.fontFamily);
                lbl.fontSize(hSize).fontWeight(hWeight).opacity(opacityVal);
                if (style.textTransform != null) lbl.textTransform(style.textTransform);
                if (style.textDecoration != null) lbl.textDecoration(style.textDecoration);
                if (!"nowrap".equals(style.whiteSpace)) lbl.wrap(true);
                yield lbl;
            }

            // ── List item ─────────────────────────────────────────────────────
            case "li" -> {
                int color = style.color != TesseraStyle.UNSET ? style.color : TesseraPalette.CREAM;
                int lw    = wVal > 0 ? wVal : (inheritWidth && availW > 0 ? availW : 80);
                String prefix = isInsideOrderedList(ancestors)
                        ? (parseIntAttr(node.attr("__nth-index"), 0) + ". ")
                        : "• ";
                // Inline mixed content → TesseraRichLabel (prefix prepended as a plain run)
                if (hasInlineChildren(node)) {
                    TesseraStyle eff = inherited.merge(style);
                    ancestors.push(node);
                    List<TesseraRichLabel.TextRun> runs;
                    try { runs = collectRuns(node, sheet, model, eff, ancestors); }
                    finally { ancestors.pop(); }
                    if (!runs.isEmpty()) {
                        // Prepend bullet/number as a plain run with the list-item style.
                        List<TesseraRichLabel.TextRun> prefixed = new ArrayList<>();
                        prefixed.add(runOf(prefix, eff));
                        prefixed.addAll(runs);
                        yield new TesseraRichLabel(0, 0, lw, 0, prefixed).opacity(opacityVal);
                    }
                }
                String rawText = node.attr("data-i18n").isBlank() ? flattenText(node, model) : resolveNodeText(node, model);
                var lbl = new TesseraLabel(0, 0, lw, 0, prefix + rawText).color(color);
                if (style.textAlign  != null) lbl.textAlign(style.textAlign);
                if (style.fontFamily != null) lbl.font(style.fontFamily);
                lbl.fontSize(fontSizePx).fontWeight(fontWeight).opacity(opacityVal);
                if (style.textTransform != null) lbl.textTransform(style.textTransform);
                if (style.textDecoration != null) lbl.textDecoration(style.textDecoration);
                if (!"nowrap".equals(style.whiteSpace)) lbl.wrap(true);
                yield lbl;
            }

            // ── Inline semantic: bold ─────────────────────────────────────────
            case "strong", "b" -> {
                String text = node.attr("data-i18n").isBlank() ? flattenText(node, model) : resolveNodeText(node, model);
                int color   = style.color != TesseraStyle.UNSET ? style.color : TesseraPalette.CREAM;
                int bWeight = style.fontWeight != TesseraStyle.UNSET ? style.fontWeight : 700;
                int lw      = wVal > 0 ? wVal : (wRaw == TesseraStyle.UNSET
                        ? measureContentWidth(text, style, fontSizePx, bWeight, minW, maxW) : 80);
                var lbl     = new TesseraLabel(0, 0, lw, hVal, text).color(color);
                if (style.fontFamily != null) lbl.font(style.fontFamily);
                lbl.fontSize(fontSizePx).fontWeight(bWeight).opacity(opacityVal);
                if (style.textDecoration != null) lbl.textDecoration(style.textDecoration);
                yield lbl;
            }

            // ── Inline semantic: italic ───────────────────────────────────────
            case "em", "i" -> {
                String text = node.attr("data-i18n").isBlank() ? flattenText(node, model) : resolveNodeText(node, model);
                // No true italic in vanilla MC font; use cream-dim color as visual hint
                int color   = style.color != TesseraStyle.UNSET ? style.color : TesseraPalette.CREAM_DIM;
                int lw      = wVal > 0 ? wVal : (wRaw == TesseraStyle.UNSET
                        ? measureContentWidth(text, style, fontSizePx, fontWeight, minW, maxW) : 80);
                var lbl     = new TesseraLabel(0, 0, lw, hVal, text).color(color);
                if (style.fontFamily != null) lbl.font(style.fontFamily);
                lbl.fontSize(fontSizePx).fontWeight(fontWeight).opacity(opacityVal);
                if (style.textDecoration != null) lbl.textDecoration(style.textDecoration);
                yield lbl;
            }

            // ── Inline: generic span ──────────────────────────────────────────
            case "span" -> {
                String text = node.attr("data-i18n").isBlank() ? flattenText(node, model) : resolveNodeText(node, model);
                int color   = style.color != TesseraStyle.UNSET ? style.color : TesseraPalette.CREAM;
                int lw      = wVal > 0 ? wVal : (wRaw == TesseraStyle.UNSET
                        ? measureContentWidth(text, style, fontSizePx, fontWeight, minW, maxW) : 80);
                var lbl     = new TesseraLabel(0, 0, lw, hVal, text).color(color);
                if (style.textAlign  != null) lbl.textAlign(style.textAlign);
                if (style.fontFamily != null) lbl.font(style.fontFamily);
                lbl.fontSize(fontSizePx).fontWeight(fontWeight).opacity(opacityVal);
                if (style.textTransform != null) lbl.textTransform(style.textTransform);
                if (style.textDecoration != null) lbl.textDecoration(style.textDecoration);
                yield lbl;
            }

            // ── Link / anchor ─────────────────────────────────────────────────
            case "a" -> {
                String text  = node.attr("data-i18n").isBlank() ? flattenText(node, model) : resolveNodeText(node, model);
                int linkColor = style.color != TesseraStyle.UNSET ? style.color : TesseraPalette.COPPER_HI;
                int bw = wVal > 0 ? wVal : (wRaw == TesseraStyle.UNSET
                        ? measureContentWidth(text, style, fontSizePx, fontWeight, minW, maxW) : 60);
                var btn = new TesseraButton(0, 0, bw, hVal).label(text);
                btn.bgColor(0); // transparent background
                btn.labelColor(linkColor);
                // Links are left-aligned by default (unlike regular buttons which center)
                btn.textAlign(style.textAlign != null ? style.textAlign : "left");
                if (style.fontFamily != null) btn.font(style.fontFamily);
                btn.fontSize(fontSizePx).fontWeight(fontWeight).opacity(opacityVal);
                String handler = node.onClickHandler();
                if (!handler.isEmpty() && handlers.containsKey(handler))
                    btn.onClick(handlers.get(handler));
                yield btn;
            }

            case "badge" -> {
                String text = resolveNodeText(node, model);
                int bg = style.background != TesseraStyle.UNSET ? style.background : 0x00000000;
                int fg = style.color      != TesseraStyle.UNSET ? style.color      : TesseraPalette.CREAM;
                int padH = (style.paddingLeft  != TesseraStyle.UNSET ? style.paddingLeft  : 5)
                         + (style.paddingRight != TesseraStyle.UNSET ? style.paddingRight : 5);
                var badge = new TesseraBadge(0, 0, hVal, text, bg).textColor(fg).paddingH(padH);
                if (style.border     != TesseraStyle.UNSET && style.borderColor != TesseraStyle.UNSET)
                    badge.border(style.border, style.borderColor);
                if (style.fontFamily != null) badge.font(style.fontFamily);
                badge.fontSize(fontSizePx).fontWeight(fontWeight).opacity(opacityVal);
                if (style.textTransform != null) badge.textTransform(style.textTransform);
                yield badge;
            }

            case "icon" -> {
                var icon = new TesseraIcon(0, 0, hVal, hVal);
                int tint = style.color != TesseraStyle.UNSET ? style.color : TesseraPalette.COPPER_HI;
                icon.tint(tint).size(hVal, hVal);
                String src = TesseraBindingResolver.resolve(node.attr("src"), model);
                if (!src.isEmpty()) {
                    icon.texture(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        "tesseraui", "textures/gui/icons/" + src + ".png"));
                }
                String handler = node.onClickHandler();
                if (!handler.isEmpty() && handlers.containsKey(handler))
                    icon.onClick(handlers.get(handler));
                yield icon;
            }

            case "checkbox" -> {
                int cbw = wVal > 0 ? wVal : 10;
                int cbh = hVal > 0 ? hVal : 10;
                String checkedRaw = TesseraBindingResolver.resolve(node.attr("checked"), model);
                boolean checked = "true".equalsIgnoreCase(checkedRaw);
                var cb = new TesseraCheckbox(0, 0, cbw, cbh, checked);
                String onInputName = node.attr("oninput");
                if (!onInputName.isEmpty() && inputHandlers != null && inputHandlers.containsKey(onInputName))
                    cb.onToggle(v -> inputHandlers.get(onInputName).accept(String.valueOf(v)));
                yield cb;
            }

            case "slider" -> {
                int slw = wVal > 0 ? wVal : 80;
                int slh = hVal > 0 ? hVal : 10;
                float slMin = parseFloatAttr(node.attr("min"), 0f);
                float slMax = parseFloatAttr(node.attr("max"), 100f);
                String valueRaw = TesseraBindingResolver.resolve(node.attr("value"), model);
                float slVal = parseFloatAttr(valueRaw, slMin);
                var sl = new TesseraSlider(0, 0, slw, slh, slMin, slMax, slVal);
                String onInputName = node.attr("oninput");
                if (!onInputName.isEmpty() && inputHandlers != null && inputHandlers.containsKey(onInputName))
                    sl.onInput(inputHandlers.get(onInputName));
                yield sl;
            }

            case "hr" -> {
                // Horizontal rule: a 1px-tall colored bar spanning available width
                int hrw = wVal > 0 ? wVal : availW;
                int hrh = Math.max(1, hVal > 0 ? hVal : 1);
                int lineColor = style.color != TesseraStyle.UNSET ? style.color : TesseraPalette.LINE;
                yield TesseraPanel.row(0, 0, hrw, hrh).background(lineColor);
            }

            case "select" -> {
                // Dropdown: reads <option value="v">Label</option> children
                int sw = wVal > 0 ? wVal : (inheritWidth && availW > 0 ? availW : 80);
                int sh = hVal > 0 ? hVal : 14;
                var dd = new TesseraDropdown(0, 0, sw, sh);
                String selectedValue = TesseraBindingResolver.resolve(node.attr("value"), model);
                for (TesseraNode child : node.children()) {
                    if ("option".equals(child.tag())) {
                        String val   = TesseraBindingResolver.resolve(child.attr("value"), model);
                        String label = TesseraBindingResolver.resolve(child.text(), model);
                        if (label == null || label.isBlank()) label = val;
                        dd.addOption(val, label);
                    }
                }
                if (selectedValue != null && !selectedValue.isBlank()) dd.select(selectedValue);
                String onInputName = node.attr("oninput");
                if (!onInputName.isEmpty() && inputHandlers != null && inputHandlers.containsKey(onInputName))
                    dd.onSelect(inputHandlers.get(onInputName));
                yield dd;
            }

            // ── Tabs container ────────────────────────────────────────────────
            case "tabs" -> {
                int tpW = wVal > 0 ? wVal : (inheritWidth && availW > 0 ? availW : 160);
                int tpH = hVal > 0 ? hVal : 100;
                var tabPanel = new TesseraTabPanel(0, 0, tpW, tpH);
                for (TesseraNode child : node.children()) {
                    if (!"tab".equals(child.tag())) continue;
                    String tabLabel = child.attr("label");
                    if (tabLabel.isBlank()) {
                        String i18nKey = child.attr("data-i18n");
                        if (!i18nKey.isBlank()) tabLabel = TesseraI18n.translate(i18nKey);
                    }
                    if (tabLabel.isBlank()) tabLabel = "Tab";
                    int tabBarH = 14;
                    int contentH = tpH - tabBarH;
                    ancestors.push(node);
                    TesseraPanel tabContent;
                    try {
                        tabContent = buildNode(child, sheet, model, handlers, inputHandlers, inputStates,
                                ancestors, 0, 0, tpW, contentH, depth + 1, inherited);
                    } finally {
                        ancestors.pop();
                    }
                    tabPanel.addTab(tabLabel, tabContent);
                }
                yield tabPanel;
            }

            // ── Virtual list ──────────────────────────────────────────────────
            case "virtual-list" -> {
                int vlW = wVal > 0 ? wVal : (inheritWidth && availW > 0 ? availW : 100);
                int vlH = hRaw != TesseraStyle.UNSET ? hVal
                        : inheritHeight && availH > 0 ? availH
                        : 80;
                int vlRowH = parseIntAttr(node.attr("row-height"), 16);
                String vFor = node.vFor();
                java.util.List<TesseraModel> vlItems = new java.util.ArrayList<>();
                String varName = "item";
                if (!vFor.isEmpty() && vFor.contains(" in ")) {
                    String[] parts = vFor.split(" in ", 2);
                    varName = parts[0].trim();
                    String listKey = parts[1].trim();
                    String resolved = model.resolve(listKey);
                    int count = parseIntAttr(resolved, 0);
                    for (int i = 0; i < count; i++) {
                        final int idx = i;
                        final String vn = varName;
                        vlItems.add(k -> model.resolve(vn + "." + k + "." + idx));
                    }
                }
                final java.util.List<TesseraNode> rowTemplate = new java.util.ArrayList<>(node.children());
                final String finalVarName = varName;
                final int finalVlW = vlW;
                final int finalVlRowH = vlRowH;
                final TesseraStyleSheet finalSheet = sheet;
                final Map<String, Runnable> finalHandlers = handlers;
                final Map<String, Consumer<String>> finalInputHandlers = inputHandlers;
                final Map<String, TesseraInputState> finalInputStates = inputStates;
                final TesseraStyle finalInherited = inherited.merge(style);
                java.util.function.Function<TesseraModel, TesseraWidget> factory = rowModel -> {
                    TesseraModel scopedRowModel = key -> {
                        if (key != null && key.startsWith(finalVarName + ".")) {
                            return rowModel.resolve(key.substring(finalVarName.length() + 1));
                        }
                        return rowModel.resolve(key);
                    };
                    TesseraPanel rowPanel = TesseraPanel.row(0, 0, finalVlW, finalVlRowH).gap(2);
                    java.util.Deque<TesseraNode> rowAncestors = new java.util.ArrayDeque<>();
                    for (TesseraNode child : rowTemplate) {
                        TesseraNode resolved2 = TesseraForEach.resolveAttrs(child, scopedRowModel);
                        TesseraWidget w = buildWidget(resolved2, finalSheet, scopedRowModel,
                                finalHandlers, finalInputHandlers, finalInputStates,
                                rowAncestors, finalVlW, finalVlRowH, true, false, depth + 1, finalInherited);
                        if (w != null) rowPanel.add(w);
                    }
                    rowPanel.layout();
                    return rowPanel;
                };
                TesseraVirtualList vl = TesseraVirtualList.of(vlItems, vlRowH, factory);
                vl.setSize(vlW, vlH);
                if (style.background != TesseraStyle.UNSET) vl.background(style.background);
                yield vl;
            }

            // ── Item slot ─────────────────────────────────────────────────────
            case "item-slot" -> {
                int ssz = parseIntAttr(node.attr("size"), 18);
                if (wVal > 0) ssz = wVal;
                var slot = new TesseraItemSlot(0, 0, ssz);
                String itemId = TesseraBindingResolver.resolve(node.attr("item"), model);
                if (itemId != null && !itemId.isBlank()) {
                    try {
                        net.minecraft.resources.ResourceLocation rl =
                                net.minecraft.resources.ResourceLocation.parse(itemId);
                        net.minecraft.world.item.Item item =
                                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                        slot.item(new net.minecraft.world.item.ItemStack(item));
                    } catch (Exception ignored) {}
                }
                String showCntRaw = node.attr("show-count");
                if (!showCntRaw.isBlank()) slot.showCount(!"false".equalsIgnoreCase(showCntRaw));
                String handler = node.onClickHandler();
                if (!handler.isEmpty() && handlers.containsKey(handler))
                    slot.onClick(handlers.get(handler));
                yield slot;
            }

            // ── Component registry: custom tags ───────────────────────────────
            default -> {
                if (TesseraComponentRegistry.has(node.tag())) {
                    TesseraNode templateRoot = TesseraComponentRegistry.get(node.tag());
                    TesseraNode instantiated = TesseraComponentRegistry.instantiate(
                            templateRoot, node.children());
                    if (instantiated == null) yield null;
                    int compW = wVal > 0 ? wVal : (inheritWidth && availW > 0 ? availW : availW);
                    int compH = hVal > 0 ? hVal : availH;
                    TesseraPanel built = buildNode(instantiated, sheet, model, handlers,
                            inputHandlers, inputStates, ancestors, 0, 0, compW, compH, depth, inherited);
                    if (hRaw == TesseraStyle.UNSET && !inheritHeight) {
                        int natH = built.fitContentHeight();
                        if (natH > built.getHeight()) { built.setSize(built.getWidth(), natH); built.layout(); }
                    }
                    yield built;
                }
                yield null;
            }
        };

        // v-show=false: widget built and in layout (space preserved), but marked invisible
        if (widget != null && "true".equals(node.attr("__vshow-hidden"))) {
            widget.setVisible(false);
        }
        // tooltip attr: wire tooltip text (resolves bindings)
        // tooltip-i18n: resolves a static i18n key via Minecraft language system (no model binding)
        if (widget != null) {
            String tipAttr = node.attr("tooltip");
            if (!tipAttr.isBlank()) {
                widget.setTooltip(TesseraBindingResolver.resolve(tipAttr, model));
            }
            String tipI18n = node.attr("tooltip-i18n");
            if (!tipI18n.isBlank()) {
                widget.setTooltip(TesseraI18n.translate(tipI18n));
            }
        }
        // draggable attr on any widget that is a TesseraPanel
        if (widget instanceof TesseraPanel panelWidget) {
            String dAttr = node.attr("draggable");
            if ("true".equalsIgnoreCase(dAttr)) {
                panelWidget.draggable(true);
                String payloadAttr = node.attr("drag-payload");
                if (!payloadAttr.isBlank()) {
                    panelWidget.dragPayload(TesseraBindingResolver.resolve(payloadAttr, model));
                }
            }
        }
        return widget;
    }

    private static void warnMissingHandler(String attr, String handler, TesseraNode node) {
        if (handler == null || handler.isBlank()) return;
        if (handler.contains("{{")) {
            LOGGER.warn("[TesseraUI] Unresolved binding in {} on <{}>: {}", attr, node.tag(), handler);
        }
    }

    /**
     * Resolves the display text for a node, honouring {@code data-i18n} first.
     * Falls back to the binding resolver if the key is absent from the active lang file.
     */
    private static String resolveNodeText(TesseraNode node, TesseraModel model) {
        String i18nKey = node.attr("data-i18n");
        if (!i18nKey.isBlank()) {
            String translated = TesseraI18n.translate(i18nKey);
            return TesseraI18n.isTranslated(i18nKey) ? translated
                 : TesseraBindingResolver.resolve(node.text(), model);
        }
        return TesseraBindingResolver.resolve(node.text(), model);
    }

    private static int naturalHeight(TesseraStyle style) {
        int padV = 0;
        if (style.paddingTop    != TesseraStyle.UNSET) padV += style.paddingTop;
        if (style.paddingBottom != TesseraStyle.UNSET) padV += style.paddingBottom;
        int textH = style.fontSize != TesseraStyle.UNSET_F
                ? (int) Math.ceil(style.fontSize)
                : 8;
        return Math.max(12, textH + padV + 4);
    }

    private static int parseIntAttr(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    private static float parseFloatAttr(String value, float fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return Float.parseFloat(value.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    private static void configureInputSuggestions(TesseraInput input, TesseraNode node, TesseraModel model) {
        String suggestionsAttr = TesseraBindingResolver.resolve(node.attr("suggestions"), model);
        String autocompleteAttr = TesseraBindingResolver.resolve(node.attr("autocomplete"), model);

        List<String> suggestions = parseSuggestionList(suggestionsAttr);
        if (suggestions.isEmpty() && !isAutocompleteBoolean(autocompleteAttr)) {
            suggestions = parseSuggestionList(autocompleteAttr);
        }
        if (!suggestions.isEmpty()) input.suggestions(suggestions);

        if (!autocompleteAttr.isBlank() && isAutocompleteBoolean(autocompleteAttr)) {
            input.autocomplete(isTruthyAutocomplete(autocompleteAttr));
        } else if (!suggestions.isEmpty()) {
            input.autocomplete(true);
        }
    }

    private static List<String> parseSuggestionList(String value) {
        if (value == null || value.isBlank()) return List.of();
        String raw = value.trim();
        if (isAutocompleteBoolean(raw)) return List.of();
        if (raw.startsWith("[") && raw.endsWith("]")) raw = raw.substring(1, raw.length() - 1);

        String[] parts = raw.split("[,;|]");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String item = part.trim();
            if ((item.startsWith("\"") && item.endsWith("\"")) || (item.startsWith("'") && item.endsWith("'"))) {
                item = item.substring(1, item.length() - 1).trim();
            }
            if (!item.isBlank()) out.add(item);
        }
        return out;
    }

    private static boolean isAutocompleteBoolean(String value) {
        if (value == null || value.isBlank()) return false;
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("true") || normalized.equals("false")
                || normalized.equals("on") || normalized.equals("off");
    }

    private static boolean isTruthyAutocomplete(String value) {
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("true") || normalized.equals("on");
    }

    /**
     * Extracts only the CSS-inheritable properties from a style.
     * These values are passed down to child nodes as defaults when those children
     * don't explicitly define the property themselves.
     *
     * <p>Inheritable properties: {@code color}, {@code font-size}, {@code font-family},
     * {@code font-weight}, {@code white-space}.</p>
     */
    private static TesseraStyle extractInheritable(TesseraStyle s) {
        TesseraStyle inh = new TesseraStyle();
        inh.color      = s.color;
        inh.fontSize   = s.fontSize;
        inh.fontFamily = s.fontFamily;
        inh.fontWeight = s.fontWeight;
        inh.whiteSpace = s.whiteSpace;
        return inh;
    }

    // ── Inline-flow helpers ───────────────────────────────────────────────────

    /**
     * Returns {@code true} when {@code node} has at least one child that is either a
     * {@linkplain TesseraNode#isTextNode() virtual text node} or an inline-level HTML
     * element ({@code strong}, {@code b}, {@code em}, {@code i}, {@code span}, {@code a}).
     * This triggers {@link TesseraRichLabel} rendering instead of plain {@link TesseraLabel}.
     */
    private static boolean hasInlineChildren(TesseraNode node) {
        for (TesseraNode child : node.children()) {
            if (child.isTextNode() || INLINE_TAGS.contains(child.tag())) return true;
        }
        return false;
    }

    /**
     * Recursively collects {@link TesseraRichLabel.TextRun}s from {@code node} and its
     * inline descendants.  Leading text ({@code node.text()}) and {@code #text} children
     * use {@code parentStyle}; inline-element children have their own resolved style merged
     * on top of {@code parentStyle} (with UA defaults applied per tag).
     */
    private static List<TesseraRichLabel.TextRun> collectRuns(
            TesseraNode node, TesseraStyleSheet sheet, TesseraModel model,
            TesseraStyle parentStyle, Deque<TesseraNode> ancestors) {

        List<TesseraRichLabel.TextRun> runs = new ArrayList<>();

        // Leading text (stored in node.text() — before the first child element).
        String lead = TesseraBindingResolver.resolve(node.text(), model);
        if (lead != null && !lead.isBlank()) runs.add(runOf(lead.trim(), parentStyle));

        for (TesseraNode child : node.children()) {
            if (child.isTextNode()) {
                String t = TesseraBindingResolver.resolve(child.text(), model);
                if (t != null && !t.isBlank()) runs.add(runOf(t.trim(), parentStyle));
            } else if (INLINE_TAGS.contains(child.tag())) {
                // Resolve and merge the child's own CSS rules onto the parent style.
                TesseraStyle cs = parentStyle.merge(sheet.resolve(child, ancestors));
                // Apply UA defaults that the stylesheet would not set automatically.
                if (("strong".equals(child.tag()) || "b".equals(child.tag()))
                        && cs.fontWeight == TesseraStyle.UNSET) {
                    cs.fontWeight = 700;
                }
                if (("em".equals(child.tag()) || "i".equals(child.tag()))
                        && cs.color == TesseraStyle.UNSET) {
                    // No true italic in vanilla MC font; use a dimmer cream as visual hint.
                    cs.color = TesseraPalette.CREAM_DIM;
                }
                // Recurse: an inline element may itself contain #text children.
                runs.addAll(collectRuns(child, sheet, model, cs, ancestors));
            }
            // Block-level children inside <p>/<li>/etc. are intentionally skipped;
            // they would break inline flow and are better rendered by the block path.
        }
        return runs;
    }

    /**
     * Creates a {@link TesseraRichLabel.TextRun} from a string and a resolved style,
     * filling in sensible defaults for any UNSET fields.
     */
    private static TesseraRichLabel.TextRun runOf(String text, TesseraStyle style) {
        int    color  = style.color      != TesseraStyle.UNSET   ? style.color      : TesseraPalette.CREAM;
        int    weight = style.fontWeight != TesseraStyle.UNSET   ? style.fontWeight : 400;
        float  size   = style.fontSize   != TesseraStyle.UNSET_F ? style.fontSize   : 7f;
        String fam    = style.fontFamily;
        String deco   = style.textDecoration;
        return new TesseraRichLabel.TextRun(text, color, weight, size, fam, deco);
    }

    // ── flattenText ───────────────────────────────────────────────────────────

    /**
     * Recursively collects all text content from a node and its descendants.
     * Used as a fallback for semantic inline containers ({@code <p>}, {@code <h1>},
     * {@code <li>}, etc.) when inline-flow detection is disabled or finds no runs.
     * Handles {@linkplain TesseraNode#isTextNode() virtual text nodes} correctly.
     */
    private static String flattenText(TesseraNode node, TesseraModel model) {
        String self = TesseraBindingResolver.resolve(node.text(), model);
        if (node.children().isEmpty()) return self != null ? self.trim() : "";
        StringBuilder sb = new StringBuilder(self != null ? self.trim() : "");
        for (TesseraNode child : node.children()) {
            String childText = child.isTextNode()
                    ? TesseraBindingResolver.resolve(child.text(), model)
                    : flattenText(child, model);
            if (childText != null && !childText.isEmpty()) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') sb.append(' ');
                sb.append(childText.trim());
            }
        }
        return sb.toString().trim();
    }

    /**
     * Returns {@code true} when any ancestor in the stack is an {@code <ol>} element
     * (and no closer {@code <ul>} overrides it).  Used to decide bullet vs. number prefix
     * for {@code <li>} items.
     */
    private static boolean isInsideOrderedList(Deque<TesseraNode> ancestors) {
        for (TesseraNode a : ancestors) {
            String t = a.tag();
            if ("ol".equals(t)) return true;
            if ("ul".equals(t)) return false;
        }
        return false;
    }

    /**
     * Returns a copy of {@code node} with one extra attribute injected.
     * Used to inject {@code __nth-index} without mutating the original node.
     */
    private static TesseraNode withAttr(TesseraNode node, String key, String value) {
        Map<String, String> newAttrs = new HashMap<>(node.attrs());
        newAttrs.put(key, value);
        return new TesseraNode(node.tag(), newAttrs, node.children(), node.text());
    }

    /**
     * Estimates the natural pixel width of a {@code div/row/col/grid} element
     * that has no explicit CSS {@code width} and should NOT inherit the parent width.
     *
     * <p>Used for {@code position:absolute} children: a div containing only direct
     * text (e.g. {@code <div class="badge">NEW</div>}) should be as wide as its
     * text content so that {@code right/bottom} offsets work correctly.</p>
     *
     * <p>Returns 0 if the element has no direct text (caller should fall back to
     * {@code availW}).</p>
     */
    private static int measureNaturalDivWidth(TesseraNode node, TesseraStyle style,
                                               float fontSizePx, int fontWeight) {
        String raw = node.text() != null ? node.text().trim() : "";
        if (raw.isEmpty()) return 0;
        try {
            var mcFont = net.minecraft.client.Minecraft.getInstance().font;
            float sc = fontSizePx / TesseraFonts.naturalPx(style.fontFamily);
            int padL = style.paddingLeft  != TesseraStyle.UNSET ? style.paddingLeft  : 0;
            int padR = style.paddingRight != TesseraStyle.UNSET ? style.paddingRight : 0;
            int bt   = style.border       != TesseraStyle.UNSET ? style.border       : 0;
            int textW = (int) Math.ceil(
                    mcFont.width(TesseraFonts.component(raw, style.fontFamily, fontWeight)) * sc);
            return textW + padL + padR + 2 * bt + 2;
        } catch (Exception e) {
            return 0; // Minecraft font not available (unit-test context)
        }
    }

    // ── Table helpers ─────────────────────────────────────────────────────────

    /**
     * Builds a {@code <table>} as a column of row panels.
     *
     * <p>Column widths are estimated equally: {@code (tableW - gaps) / numCols}.
     * Each cell is a column panel built by the normal {@link #buildNode} path so
     * it inherits all CSS (background, padding, border, fonts, etc.).
     * Row and table heights are auto-sized via {@link TesseraPanel#fitContentHeight()}.
     */
    private static TesseraPanel buildTablePanel(TesseraNode node, TesseraStyleSheet sheet,
                                                TesseraModel model, Map<String, Runnable> handlers,
                                                Map<String, Consumer<String>> inputHandlers,
                                                Map<String, TesseraInputState> inputStates,
                                                Deque<TesseraNode> ancestors,
                                                int x, int y, int tableW, int tableH, int depth,
                                                TesseraStyle inherited) {
        TesseraStyle style = sheet.resolve(node, ancestors);
        int padT = style.paddingTop    != TesseraStyle.UNSET ? style.paddingTop    : 0;
        int padR = style.paddingRight  != TesseraStyle.UNSET ? style.paddingRight  : 0;
        int padB = style.paddingBottom != TesseraStyle.UNSET ? style.paddingBottom : 0;
        int padL = style.paddingLeft   != TesseraStyle.UNSET ? style.paddingLeft   : 0;
        int gapV  = style.gap   != TesseraStyle.UNSET ? style.gap   : 1; // vertical gap between rows
        int borderT = style.border != TesseraStyle.UNSET ? style.border : 0;
        int innerW  = tableW - padL - padR - 2 * borderT;

        List<TesseraNode> rows = collectTableRows(node);
        int numCols  = Math.max(1, rows.stream().mapToInt(TesseraTemplateRenderer::countCells).max().orElse(1));
        int cellGapH = 1; // horizontal gap between cells within a row
        int cellW    = Math.max(8, (innerW - (numCols - 1) * cellGapH) / numCols);

        TesseraPanel table = TesseraPanel.column(x, y, tableW, tableH);
        table.gap(gapV);
        if (padT > 0 || padR > 0 || padB > 0 || padL > 0) table.padding(padT, padR, padB, padL);
        if (style.background  != TesseraStyle.UNSET) table.background(style.background);
        if (style.borderRadius != TesseraStyle.UNSET && style.borderRadius > 0) table.borderRadius(style.borderRadius);
        if (style.border      != TesseraStyle.UNSET && style.borderColor != TesseraStyle.UNSET)
            table.border(style.border, style.borderColor);

        TesseraStyle nodeEffective  = inherited.merge(style);
        TesseraStyle childInherited = extractInheritable(nodeEffective);

        ancestors.push(node);
        try {
            for (TesseraNode tr : rows) {
                TesseraStyle rowStyle = sheet.resolve(tr, ancestors);
                int rowGapH = rowStyle.gap != TesseraStyle.UNSET ? rowStyle.gap : cellGapH;

                TesseraPanel rowPanel = TesseraPanel.row(0, 0, innerW, naturalHeight(rowStyle));
                rowPanel.gap(rowGapH);
                if (rowStyle.background != TesseraStyle.UNSET) rowPanel.background(rowStyle.background);
                if (rowStyle.border != TesseraStyle.UNSET && rowStyle.borderColor != TesseraStyle.UNSET)
                    rowPanel.border(rowStyle.border, rowStyle.borderColor);

                TesseraStyle rowEffective   = childInherited.merge(rowStyle);
                TesseraStyle cellInherited  = extractInheritable(rowEffective);

                ancestors.push(tr);
                try {
                    for (TesseraNode cell : tr.children()) {
                        if (!"td".equals(cell.tag()) && !"th".equals(cell.tag())) continue;
                        TesseraStyle cellStyle = cellInherited.merge(sheet.resolve(cell, ancestors));
                        int explicitCW = cellStyle.width != TesseraStyle.UNSET ? cellStyle.width : cellW;
                        // <th> defaults: bold + center-align (can be overridden by CSS)
                        if ("th".equals(cell.tag())) {
                            if (cellStyle.fontWeight == TesseraStyle.UNSET) cellStyle.fontWeight = 700;
                            if (cellStyle.textAlign  == null)               cellStyle.textAlign  = "center";
                        }
                        TesseraPanel cellPanel = buildNode(cell, sheet, model, handlers, inputHandlers, inputStates,
                                ancestors, 0, 0, explicitCW, 0, depth + 1, cellInherited);
                        // Auto-size cell height to content
                        int natH = cellPanel.fitContentHeight();
                        if (natH > cellPanel.getHeight()) { cellPanel.setSize(cellPanel.getWidth(), natH); cellPanel.layout(); }
                        rowPanel.add(cellPanel);
                    }
                } finally {
                    ancestors.pop();
                }
                // Auto-size row height to tallest cell
                rowPanel.layout();
                int natRowH = rowPanel.fitContentHeight();
                if (natRowH > rowPanel.getHeight()) rowPanel.setSize(rowPanel.getWidth(), natRowH);
                table.add(rowPanel);
            }
        } finally {
            ancestors.pop();
        }

        table.layout();
        int natTableH = table.fitContentHeight();
        if (natTableH > table.getHeight()) { table.setSize(table.getWidth(), natTableH); table.layout(); }
        return table;
    }

    /**
     * Collects all {@code <tr>} nodes from a {@code <table>}, traversing
     * {@code <thead>}/{@code <tbody>}/{@code <tfoot>} wrappers transparently.
     */
    private static List<TesseraNode> collectTableRows(TesseraNode table) {
        var rows = new java.util.ArrayList<TesseraNode>();
        for (TesseraNode child : table.children()) {
            String t = child.tag();
            if ("tr".equals(t)) {
                rows.add(child);
            } else if ("thead".equals(t) || "tbody".equals(t) || "tfoot".equals(t)) {
                for (TesseraNode row : child.children())
                    if ("tr".equals(row.tag())) rows.add(row);
            }
        }
        return rows;
    }

    /** Counts the number of {@code <td>}/{@code <th>} cells inside a {@code <tr>}. */
    private static int countCells(TesseraNode tr) {
        int n = 0;
        for (TesseraNode c : tr.children())
            if ("td".equals(c.tag()) || "th".equals(c.tag())) n++;
        return n;
    }
}
