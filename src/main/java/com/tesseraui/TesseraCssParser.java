package com.tesseraui;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TesseraCssParser {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern RULE        = Pattern.compile("([^{]+)\\{([^}]*)\\}", Pattern.DOTALL);
    private static final Pattern VAR_USE     = Pattern.compile("var\\(\\s*(--[\\w-]+)\\s*\\)");
    private static final Pattern ROOT_BLOCK  = Pattern.compile(":root\\s*\\{([^}]*)\\}", Pattern.DOTALL);
    private static final Pattern MEDIA_COND  = Pattern.compile("\\(\\s*(min|max)-width\\s*:\\s*(\\d+)(?:px)?\\s*\\)");
    private static final Pattern INT_PREFIX  = Pattern.compile("^(-?\\d+)");
    private static final Pattern KF_BLOCK    = Pattern.compile(
            "@keyframes\\s+(\\w+)\\s*\\{([^{}]*(?:\\{[^}]*\\}[^{}]*)*)\\}",
            Pattern.DOTALL);
    private static final Pattern KF_STOP     = Pattern.compile(
            "(from|to|\\d+%)\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Map<String, Integer> NAMED_COLORS = new HashMap<>();
    static {
        NAMED_COLORS.put("transparent", 0x00000000);
        NAMED_COLORS.put("white",       0xFFFFFFFF);
        NAMED_COLORS.put("black",       0xFF000000);
        NAMED_COLORS.put("red",         0xFFFF0000);
        NAMED_COLORS.put("green",       0xFF008000);
        NAMED_COLORS.put("lime",        0xFF32CD32);
        NAMED_COLORS.put("blue",        0xFF0000FF);
        NAMED_COLORS.put("yellow",      0xFFFFFF00);
        NAMED_COLORS.put("gray",        0xFF808080);
        NAMED_COLORS.put("grey",        0xFF808080);
        NAMED_COLORS.put("silver",      0xFFC0C0C0);
        NAMED_COLORS.put("orange",      0xFFFFA500);
        NAMED_COLORS.put("purple",      0xFF800080);
        NAMED_COLORS.put("cyan",        0xFF00FFFF);
        NAMED_COLORS.put("magenta",     0xFFFF00FF);
        NAMED_COLORS.put("pink",        0xFFFF69B4);
        NAMED_COLORS.put("brown",       0xFF8B4513);
        NAMED_COLORS.put("navy",        0xFF000080);
        NAMED_COLORS.put("teal",        0xFF008080);
        NAMED_COLORS.put("gold",        0xFFFFD700);
        NAMED_COLORS.put("copper",      0xFFB87333);
        NAMED_COLORS.put("maroon",      0xFF800000);
        NAMED_COLORS.put("olive",       0xFF808000);
        NAMED_COLORS.put("aqua",        0xFF00FFFF);
        NAMED_COLORS.put("fuchsia",     0xFFFF00FF);
        NAMED_COLORS.put("indigo",      0xFF4B0082);
        NAMED_COLORS.put("violet",      0xFFEE82EE);
        NAMED_COLORS.put("coral",       0xFFFF7F50);
        NAMED_COLORS.put("salmon",      0xFFFA8072);
        NAMED_COLORS.put("khaki",       0xFFF0E68C);
        NAMED_COLORS.put("beige",       0xFFF5F5DC);
    }

    public static TesseraStyleSheet parse(String css) {
        List<TesseraStyleSheet.Rule> base     = new ArrayList<>();
        List<TesseraStyleSheet.Rule> hover    = new ArrayList<>();
        List<TesseraStyleSheet.Rule> active   = new ArrayList<>();
        List<TesseraStyleSheet.Rule> disabled = new ArrayList<>();
        List<TesseraStyleSheet.Rule> focus    = new ArrayList<>();
        List<TesseraStyleSheet.MediaBlock> mediaBlocks = new ArrayList<>();

        String processed = resolveVariables(stripComments(css));

        // ── Step 1: extract @media blocks (nested braces, not caught by flat RULE) ──
        int[] orderRef = { 0 };
        processed = extractMediaBlocks(processed, mediaBlocks, orderRef);
        int order = orderRef[0];

        // ── Step 1b: extract and parse @keyframes blocks ─────────────────────────
        List<TesseraKeyframes> keyframesList = new ArrayList<>();
        Matcher kfMatcher = KF_BLOCK.matcher(processed);
        while (kfMatcher.find()) {
            String kfName = kfMatcher.group(1);
            String kfBody = kfMatcher.group(2);
            List<TesseraKeyframes.Stop> stops = new ArrayList<>();
            Matcher stopMatcher = KF_STOP.matcher(kfBody);
            while (stopMatcher.find()) {
                String progressStr = stopMatcher.group(1).trim().toLowerCase(java.util.Locale.ROOT);
                float progress = switch (progressStr) {
                    case "from" -> 0f;
                    case "to"   -> 1f;
                    default     -> Float.parseFloat(progressStr.replace("%", "")) / 100f;
                };
                TesseraStyle stopStyle = new TesseraStyle();
                String stopBody = stopMatcher.group(2);
                for (String decl : stopBody.split(";")) {
                    String[] kv = decl.split(":", 2);
                    if (kv.length == 2) {
                        try { applyProp(stopStyle, kv[0].trim().toLowerCase(java.util.Locale.ROOT), kv[1].trim()); }
                        catch (Exception ignored) {}
                    }
                }
                stops.add(new TesseraKeyframes.Stop(progress, stopStyle));
            }
            if (!stops.isEmpty()) {
                keyframesList.add(new TesseraKeyframes(kfName, stops));
            }
        }
        // Strip @keyframes blocks before normal rule processing
        processed = KF_BLOCK.matcher(processed).replaceAll("");

        // ── Step 2: strip remaining at-rules (@supports, @font-face, …) ──────────
        // Remove block at-rules (potentially with nested braces one level deep)
        processed = processed.replaceAll("@[^;{]+\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}", "");
        // Remove single-line at-rules (@charset, @import)
        processed = processed.replaceAll("@[^;]+;", "");

        // ── Step 3: parse remaining flat rules ──────────────────────────────────────
        Matcher m = RULE.matcher(processed);
        while (m.find()) {
            String rawSel = m.group(1).trim();
            String body   = m.group(2).trim();
            if (rawSel.equals(":root")) continue;

            TesseraStyle style = parseBody(body);
            for (String sel : rawSel.split(",")) {
                sel = sel.trim();
                String state = "base";
                String coreSel = sel;
                if (sel.endsWith(":hover"))        { state = "hover";    coreSel = sel.substring(0, sel.length() - 6).trim(); }
                else if (sel.endsWith(":active"))   { state = "active";   coreSel = sel.substring(0, sel.length() - 7).trim(); }
                else if (sel.endsWith(":disabled")) { state = "disabled"; coreSel = sel.substring(0, sel.length() - 9).trim(); }
                else if (sel.endsWith(":focus"))    { state = "focus";    coreSel = sel.substring(0, sel.length() - 6).trim(); }

                TesseraSelector parsed = TesseraSelector.parse(coreSel);
                if (parsed == null) continue;
                TesseraStyleSheet.Rule rule = new TesseraStyleSheet.Rule(parsed, style, order++);
                switch (state) {
                    case "hover"    -> hover.add(rule);
                    case "active"   -> active.add(rule);
                    case "disabled" -> disabled.add(rule);
                    case "focus"    -> focus.add(rule);
                    default         -> base.add(rule);
                }
            }
        }
        TesseraStyleSheet sheet = new TesseraStyleSheet(base, hover, active, disabled, focus, mediaBlocks);
        for (TesseraKeyframes kf : keyframesList) {
            sheet.registerKeyframes(kf);
        }
        return sheet;
    }

    /**
     * Scans {@code css} for {@code @media (...) { ... }} blocks.  Each block is
     * parsed into a {@link TesseraStyleSheet.MediaBlock} and appended to
     * {@code out}.  Returns the CSS string with all @media blocks replaced by
     * whitespace (so the flat RULE regex can run cleanly afterwards).
     *
     * @param css      input CSS (comments already stripped, variables resolved)
     * @param out      list to append parsed media blocks to
     * @param orderRef mutable single-element array; orderRef[0] is incremented
     *                 for each rule to preserve global order with flat rules
     */
    private static String extractMediaBlocks(String css,
            List<TesseraStyleSheet.MediaBlock> out, int[] orderRef) {
        StringBuilder result = new StringBuilder(css.length());
        int i = 0;
        while (i < css.length()) {
            // Look for @media
            int atIdx = css.indexOf("@media", i);
            if (atIdx < 0) {
                result.append(css, i, css.length());
                break;
            }
            // Keep everything before @media
            result.append(css, i, atIdx);

            // Find the opening brace of the @media condition+block
            int openBrace = css.indexOf('{', atIdx);
            if (openBrace < 0) { i = css.length(); break; }

            // Parse the condition between @media and {
            String condition = css.substring(atIdx + 6, openBrace).trim();
            int minW = -1, maxW = -1;
            Matcher cond = MEDIA_COND.matcher(condition.toLowerCase(java.util.Locale.ROOT));
            while (cond.find()) {
                int val = Integer.parseInt(cond.group(2));
                if ("min".equals(cond.group(1))) minW = val;
                else                              maxW = val;
            }

            // Find matching closing brace (handle nesting depth)
            int depth = 1;
            int j = openBrace + 1;
            while (j < css.length() && depth > 0) {
                char c = css.charAt(j);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                j++;
            }
            // css[openBrace+1 .. j-2] is the body of the @media block
            String blockBody = css.substring(openBrace + 1, j - 1).trim();

            // Parse the inner rules exactly like flat CSS
            List<TesseraStyleSheet.Rule> mBase     = new ArrayList<>();
            List<TesseraStyleSheet.Rule> mHover    = new ArrayList<>();
            List<TesseraStyleSheet.Rule> mActive   = new ArrayList<>();
            List<TesseraStyleSheet.Rule> mDisabled = new ArrayList<>();
            List<TesseraStyleSheet.Rule> mFocus    = new ArrayList<>();
            Matcher rm = RULE.matcher(blockBody);
            while (rm.find()) {
                String rawSel = rm.group(1).trim();
                String body   = rm.group(2).trim();
                if (rawSel.equals(":root")) continue;
                TesseraStyle style = parseBody(body);
                for (String sel : rawSel.split(",")) {
                    sel = sel.trim();
                    String state = "base";
                    String coreSel = sel;
                    if (sel.endsWith(":hover"))        { state = "hover";    coreSel = sel.substring(0, sel.length() - 6).trim(); }
                    else if (sel.endsWith(":active"))   { state = "active";   coreSel = sel.substring(0, sel.length() - 7).trim(); }
                    else if (sel.endsWith(":disabled")) { state = "disabled"; coreSel = sel.substring(0, sel.length() - 9).trim(); }
                    else if (sel.endsWith(":focus"))    { state = "focus";    coreSel = sel.substring(0, sel.length() - 6).trim(); }
                    TesseraSelector parsed = TesseraSelector.parse(coreSel);
                    if (parsed == null) continue;
                    TesseraStyleSheet.Rule rule = new TesseraStyleSheet.Rule(parsed, style, orderRef[0]++);
                    switch (state) {
                        case "hover"    -> mHover.add(rule);
                        case "active"   -> mActive.add(rule);
                        case "disabled" -> mDisabled.add(rule);
                        case "focus"    -> mFocus.add(rule);
                        default         -> mBase.add(rule);
                    }
                }
            }

            out.add(new TesseraStyleSheet.MediaBlock(minW, maxW, mBase, mHover, mActive, mDisabled, mFocus));

            // Replace the @media block with spaces to keep character offsets stable
            result.append(" ".repeat(j - atIdx));
            i = j;
        }
        return result.toString();
    }

    private static String resolveVariables(String css) {
        Map<String, String> vars = new HashMap<>();
        Matcher root = ROOT_BLOCK.matcher(css);
        while (root.find()) {
            for (String decl : root.group(1).split(";")) {
                decl = decl.trim();
                int colon = decl.indexOf(':');
                if (colon > 0 && decl.startsWith("--")) {
                    vars.put(decl.substring(0, colon).trim(), decl.substring(colon + 1).trim());
                }
            }
        }
        if (vars.isEmpty()) return css;
        StringBuffer sb = new StringBuffer();
        Matcher v = VAR_USE.matcher(css);
        while (v.find()) {
            String val = vars.getOrDefault(v.group(1), v.group(0));
            v.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        v.appendTail(sb);
        return sb.toString();
    }

    private static TesseraStyle parseBody(String body) {
        TesseraStyle s = new TesseraStyle();
        for (String decl : body.split(";")) {
            decl = decl.trim();
            if (decl.isEmpty()) continue;
            int colon = decl.indexOf(':');
            if (colon < 0) continue;
            String prop  = decl.substring(0, colon).trim().toLowerCase(java.util.Locale.ROOT);
            String value = decl.substring(colon + 1).trim();
            try { applyProp(s, prop, value); }
            catch (Exception e) {
                LOGGER.warn("[TesseraUI] CSS ignored '{}:{}': {}", prop, value, e.getMessage());
            }
        }
        return s;
    }

    private static void applyProp(TesseraStyle s, String prop, String value) {
        switch (prop) {
            case "background", "background-color" -> s.background  = parseColor(value);
            case "color"                           -> s.color       = parseColor(value);
            case "border-color"                    -> s.borderColor = parseColor(value);

            case "width"      -> applyLengthDim(s, "width", value);
            case "height"     -> applyLengthDim(s, "height", value);
            case "min-width"  -> applyLengthDim(s, "minWidth", value);
            case "max-width"  -> applyLengthDim(s, "maxWidth", value);
            case "min-height" -> applyLengthDim(s, "minHeight", value);
            case "max-height" -> applyLengthDim(s, "maxHeight", value);

            case "padding"        -> applyPaddingShorthand(s, value);
            case "padding-top"    -> s.paddingTop    = parseLength(value);
            case "padding-right"  -> s.paddingRight  = parseLength(value);
            case "padding-bottom" -> s.paddingBottom = parseLength(value);
            case "padding-left"   -> s.paddingLeft   = parseLength(value);

            case "margin"        -> applyMarginShorthand(s, value);
            case "margin-right"  -> s.marginRight  = parseLength(value);
            case "margin-bottom" -> s.marginBottom = parseLength(value);
            case "margin-left"   -> s.marginLeft   = parseLength(value);

            case "gap"             -> s.gap = parseLength(value);
            case "flex"            -> applyFlexShorthand(s, value);
            case "flex-grow"       -> s.flexGrow   = parseFloatVal(value);
            case "flex-shrink"     -> s.flexShrink = parseFloatVal(value);
            case "flex-basis"      -> {
                String fv = value.trim().toLowerCase(java.util.Locale.ROOT);
                s.flexBasis = "auto".equals(fv) ? TesseraStyle.UNSET : parseLength(fv);
            }
            case "order"           -> s.order  = parseInt(value);
            case "z-index"         -> s.zIndex = parseInt(value);
            case "border"          -> applyBorderShorthand(s, value);
            case "display"         -> s.display         = value;
            case "flex-direction"  -> s.flexDirection   = value;
            case "flex-wrap"       -> s.flexWrap        = value;
            case "align-items"     -> s.alignItems      = value;
            case "justify-content" -> s.justifyContent  = value;

            case "border-top"    -> applyBorderSide(s, value, "top");
            case "border-bottom" -> applyBorderSide(s, value, "bottom");
            case "border-left"   -> applyBorderSide(s, value, "left");
            case "border-right"  -> applyBorderSide(s, value, "right");

            case "text-align"      -> s.textAlign     = value;
            case "text-transform"  -> s.textTransform = value.trim().toLowerCase(java.util.Locale.ROOT);
            case "opacity"         -> s.opacity        = Float.parseFloat(value.trim());
            case "font-family"     -> s.fontFamily     = parseFontFamily(value);
            case "font-size"       -> s.fontSize       = parseFontSize(value);
            case "font-weight"     -> s.fontWeight     = parseFontWeight(value);
            case "overflow"          -> s.overflow        = value.trim().toLowerCase(java.util.Locale.ROOT);
            case "white-space"       -> s.whiteSpace      = value.trim().toLowerCase(java.util.Locale.ROOT);
            case "text-decoration"   -> s.textDecoration  = value.trim().toLowerCase(java.util.Locale.ROOT);
            case "border-radius"     -> s.borderRadius    = parseLength(value);
            case "box-sizing"      -> s.boxSizing      = value.trim().toLowerCase(java.util.Locale.ROOT);
            case "position" -> s.position = value.trim().toLowerCase(java.util.Locale.ROOT);
            case "top"      -> s.top    = parseLength(value);
            case "left"     -> s.left   = parseLength(value);
            case "right"    -> s.right  = parseLength(value);
            case "bottom"   -> s.bottom = parseLength(value);
            case "align-self"      -> s.alignSelf      = value.trim();

            case "grid-template-columns" -> s.gridTemplateColumns = value.trim().split("\\s+");

            case "--arca-corner-dots" -> applyCornerDots(s, value);

            case "margin-top" -> {
                if ("auto".equals(value.trim())) { s.marginTopAuto = true;  s.marginTopAutoSet = true; s.marginTop = TesseraStyle.UNSET; }
                else                             { s.marginTop = parseLength(value); s.marginTopAuto = false; s.marginTopAutoSet = true; }
            }
            case "transition" -> {
                // Supports: "prop duration [easing] [delay], prop2 duration2 ..."
                List<TesseraTransitionDef> defs = new ArrayList<>();
                for (String entry : value.split(",")) {
                    String[] parts = entry.trim().split("\\s+");
                    if (parts.length < 2) continue;
                    String property  = parts[0];
                    int    duration  = parseMs(parts[1]);
                    TesseraEasing easing = parts.length >= 3 ? TesseraEasing.parse(parts[2]) : TesseraEasing.EASE;
                    int    delay     = parts.length >= 4 ? parseMs(parts[3]) : 0;
                    defs.add(TesseraTransitionDef.of(property, duration, easing, delay));
                }
                if (!defs.isEmpty()) s.transitions = defs;
                // Also maintain legacy fields for backward compatibility
                if (!defs.isEmpty()) {
                    s.transitionDurationMs = defs.get(0).durationMs();
                    s.transitionProperty   = defs.get(0).property();
                }
            }
            case "animation" -> {
                // Supports: "name duration [easing] [delay] [iteration-count] [direction]"
                List<TesseraAnimationDef> defs = new ArrayList<>();
                for (String entry : value.split(",")) {
                    String[] parts = entry.trim().split("\\s+");
                    if (parts.length < 2) continue;
                    String name       = parts[0];
                    int    duration   = parseMs(parts[1]);
                    TesseraEasing easing    = TesseraEasing.EASE;
                    int    delay      = 0;
                    int    iterations = 1;
                    boolean alternate = false;
                    for (int i = 2; i < parts.length; i++) {
                        String p = parts[i].toLowerCase(java.util.Locale.ROOT);
                        if (p.equals("infinite"))    { iterations = -1; continue; }
                        if (p.equals("alternate"))   { alternate = true; continue; }
                        if (p.matches("\\d+"))       { iterations = Integer.parseInt(p); continue; }
                        if (p.endsWith("ms") || p.endsWith("s")) { delay = parseMs(p); continue; }
                        TesseraEasing e = TesseraEasing.parse(p);
                        if (e != TesseraEasing.EASE || p.equals("ease")) easing = e;
                    }
                    defs.add(TesseraAnimationDef.of(name, duration, easing, delay, iterations, alternate));
                }
                if (!defs.isEmpty()) s.animations = defs;
            }
        }
    }

    private static void applyLengthDim(TesseraStyle s, String which, String value) {
        String trimmed = value.trim();
        // calc() — store the raw expression; will be evaluated at layout time
        if (trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("calc(")) {
            switch (which) {
                case "width"  -> { s.widthCalc  = trimmed; s.width  = TesseraStyle.UNSET; s.widthPercent  = false; }
                case "height" -> { s.heightCalc = trimmed; s.height = TesseraStyle.UNSET; s.heightPercent = false; }
                // min/max calc: fall through to 0 for now (rare, can extend later)
            }
            return;
        }
        boolean pct = trimmed.endsWith("%");
        int v = parseLength(trimmed);
        switch (which) {
            case "width"     -> { s.width = v;     s.widthPercent = pct;  s.widthCalc  = null; }
            case "height"    -> { s.height = v;    s.heightPercent = pct; s.heightCalc = null; }
            case "minWidth"  -> { s.minWidth = v;  s.minWidthPercent = pct; }
            case "maxWidth"  -> { s.maxWidth = v;  s.maxWidthPercent = pct; }
            case "minHeight" -> { s.minHeight = v; s.minHeightPercent = pct; }
            case "maxHeight" -> { s.maxHeight = v; s.maxHeightPercent = pct; }
        }
    }

    private static void applyCornerDots(TesseraStyle s, String value) {
        String[] parts = value.trim().split("\\s+");
        for (String p : parts) {
            if (p.matches("\\d+(px)?")) s.cornerDotSize = parseLength(p);
            else {
                try { s.cornerDotColor = parseColor(p); } catch (Exception ignored) {}
            }
        }
        if (s.cornerDotSize == TesseraStyle.UNSET) s.cornerDotSize = 4;
    }

    private static float parseFontSize(String value) {
        String v = value.trim().toLowerCase(java.util.Locale.ROOT);
        try {
            if (v.endsWith("em")) return Float.parseFloat(v.substring(0, v.length() - 2).trim()) * 7f;
            if (v.endsWith("px")) return Float.parseFloat(v.substring(0, v.length() - 2).trim());
            return Float.parseFloat(v);
        } catch (NumberFormatException e) { return TesseraStyle.UNSET_F; }
    }

    private static int parseFontWeight(String value) {
        String v = value.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (v) {
            case "normal"  -> 400;
            case "bold"    -> 700;
            case "bolder"  -> 800;
            case "lighter" -> 300;
            default -> {
                try { yield Integer.parseInt(v); }
                catch (NumberFormatException e) { yield TesseraStyle.UNSET; }
            }
        };
    }

    private static String parseFontFamily(String value) {
        value = value.toLowerCase(java.util.Locale.ROOT).trim()
                     .replace("'", "").replace("\"", "");
        if (value.equals("fantasy") || value.contains("cormorant") || value.contains("garamond"))
            return "fantasy";
        if (value.equals("mono") || value.contains("jetbrains") || value.contains("ibm plex")
                || value.contains("monospace"))
            return "mono";
        return null;
    }

    private static void applyBorderSide(TesseraStyle s, String value, String side) {
        int color = extractBorderColor(value);
        if (color == TesseraStyle.UNSET) return;
        switch (side) {
            case "top"    -> s.borderTopColor    = color;
            case "bottom" -> s.borderBottomColor = color;
            case "left"   -> s.borderLeftColor   = color;
            case "right"  -> s.borderRightColor  = color;
        }
    }

    private static int extractBorderColor(String value) {
        for (String part : value.trim().split("\\s+")) {
            part = part.trim();
            if (part.isEmpty() || part.equals("solid") || part.equals("dashed")
                    || part.equals("dotted") || part.equals("none")) continue;
            if (part.matches("\\d.*")) continue;
            try { return parseColor(part); } catch (Exception ignored) {}
        }
        return TesseraStyle.UNSET;
    }

    private static void applyBorderShorthand(TesseraStyle s, String value) {
        for (String part : value.trim().split("\\s+")) {
            part = part.trim();
            if (part.isEmpty() || part.equals("solid") || part.equals("dashed") || part.equals("dotted") || part.equals("none")) continue;
            if (part.matches("\\d.*")) {
                s.border = parseLength(part);
            } else {
                try { s.borderColor = parseColor(part); } catch (Exception ignored) {}
            }
        }
        if (s.border == TesseraStyle.UNSET && s.borderColor != TesseraStyle.UNSET) s.border = 1;
    }

    private static void applyPaddingShorthand(TesseraStyle s, String value) {
        int[] vals = parseMultiLength(value);
        switch (vals.length) {
            case 1 -> { s.paddingTop = s.paddingRight = s.paddingBottom = s.paddingLeft = vals[0]; }
            case 2 -> { s.paddingTop = s.paddingBottom = vals[0]; s.paddingRight = s.paddingLeft = vals[1]; }
            case 3 -> { s.paddingTop = vals[0]; s.paddingRight = s.paddingLeft = vals[1]; s.paddingBottom = vals[2]; }
            case 4 -> { s.paddingTop = vals[0]; s.paddingRight = vals[1]; s.paddingBottom = vals[2]; s.paddingLeft = vals[3]; }
        }
    }

    private static void applyMarginShorthand(TesseraStyle s, String value) {
        int[] vals = parseMultiLength(value);
        switch (vals.length) {
            case 1 -> { s.marginTop = s.marginRight = s.marginBottom = s.marginLeft = vals[0]; }
            case 2 -> { s.marginTop = s.marginBottom = vals[0]; s.marginRight = s.marginLeft = vals[1]; }
            case 3 -> { s.marginTop = vals[0]; s.marginRight = s.marginLeft = vals[1]; s.marginBottom = vals[2]; }
            case 4 -> { s.marginTop = vals[0]; s.marginRight = vals[1]; s.marginBottom = vals[2]; s.marginLeft = vals[3]; }
        }
    }

    private static int[] parseMultiLength(String value) {
        String[] parts = value.trim().split("\\s+");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = parseLength(parts[i]);
        return result;
    }

    public static int parseColor(String value) {
        value = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (NAMED_COLORS.containsKey(value)) return NAMED_COLORS.get(value);
        if (value.startsWith("#")) {
            String hex = value.substring(1);
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            } else if (hex.length() == 4) {
                // CSS4 #RGBA — alpha is last; reorder to ARGB during expansion
                String rr = "" + hex.charAt(0) + hex.charAt(0);
                String gg = "" + hex.charAt(1) + hex.charAt(1);
                String bb = "" + hex.charAt(2) + hex.charAt(2);
                String aa = "" + hex.charAt(3) + hex.charAt(3);
                hex = aa + rr + gg + bb; // now ARGB order
            }
            if (hex.length() == 6) return (int) (0xFF000000L | Long.parseLong(hex, 16));
            if (hex.length() == 8) return (int) Long.parseLong(hex, 16);
        }
        if (value.startsWith("rgb(") && value.endsWith(")")) {
            int[] c = parseIntList(value.substring(4, value.length() - 1));
            if (c.length == 3) return 0xFF000000 | (clamp(c[0]) << 16) | (clamp(c[1]) << 8) | clamp(c[2]);
        }
        if (value.startsWith("rgba(") && value.endsWith(")")) {
            String inner = value.substring(5, value.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length == 4) {
                int r = clamp(parseInt(parts[0]));
                int g = clamp(parseInt(parts[1]));
                int b = clamp(parseInt(parts[2]));
                float alphaF = Float.parseFloat(parts[3].trim());
                int a = (int)(Math.min(1f, Math.max(0f, alphaF)) * 255);
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        throw new IllegalArgumentException("Unknown color: " + value);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private static int[] parseIntList(String s) {
        String[] parts = s.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = parseInt(parts[i]);
        return result;
    }

    private static int parseLength(String value) {
        if (value == null) return 0;
        String v = value.trim();
        Matcher m = INT_PREFIX.matcher(v);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private static int parseInt(String value) {
        if (value == null) return 0;
        String v = value.trim();
        Matcher m = INT_PREFIX.matcher(v);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    // ── flex shorthand ────────────────────────────────────────────────────────

    /**
     * Expands the {@code flex} shorthand into {@code flexGrow}, {@code flexShrink},
     * and {@code flexBasis} fields.
     *
     * <ul>
     *   <li>{@code flex: none}    → 0  0  auto</li>
     *   <li>{@code flex: auto}    → 1  1  auto</li>
     *   <li>{@code flex: N}       → N  1  0   (N&gt;0) or 0  0  auto (N==0)</li>
     *   <li>{@code flex: N M}     → N  M  0</li>
     *   <li>{@code flex: N M Xpx} → N  M  X</li>
     * </ul>
     */
    private static void applyFlexShorthand(TesseraStyle s, String value) {
        String v = value.trim().toLowerCase(java.util.Locale.ROOT);
        switch (v) {
            case "none"    -> { s.flexGrow = 0; s.flexShrink = 0; s.flexBasis = TesseraStyle.UNSET; return; }
            case "auto"    -> { s.flexGrow = 1; s.flexShrink = 1; s.flexBasis = TesseraStyle.UNSET; return; }
            case "initial" -> { s.flexGrow = 0; s.flexShrink = 1; s.flexBasis = TesseraStyle.UNSET; return; }
        }
        String[] parts = v.split("\\s+");
        float grow = parseFloatVal(parts[0]);
        s.flexGrow   = grow;
        s.flexShrink = parts.length >= 2 ? parseFloatVal(parts[1]) : 1f;
        if (parts.length >= 3) {
            s.flexBasis = parseLength(parts[2]);
        } else {
            // flex: N → basis=0 if N>0 (item starts at 0 and grows); basis=auto if N==0
            s.flexBasis = grow > 0 ? 0 : TesseraStyle.UNSET;
        }
    }

    // ── calc() evaluator ─────────────────────────────────────────────────────

    /**
     * Evaluates a CSS {@code calc()} expression against a known parent dimension.
     *
     * <p>Supports {@code +} and {@code -} operators on {@code px} and {@code %}
     * terms.  Operators <strong>must</strong> be surrounded by whitespace, matching
     * the CSS spec.  Example: {@code calc(100% - 120px)} → {@code basis - 120}.</p>
     *
     * @param expr  the raw CSS value (with or without the outer {@code calc(…)})
     * @param basis the available size in pixels used to resolve {@code %} terms
     * @return resolved pixel value
     */
    public static int evalCalc(String expr, int basis) {
        if (expr == null) return 0;
        expr = expr.trim();
        String lower = expr.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("calc(") && expr.endsWith(")"))
            expr = expr.substring(5, expr.length() - 1).trim();

        // Tokenise on whitespace; operators (+/-) are standalone tokens
        String[] tokens = expr.split("\\s+");
        int total = 0;
        int sign  = 1;
        for (String tok : tokens) {
            tok = tok.trim();
            if (tok.isEmpty())  continue;
            if (tok.equals("+")) { sign =  1; continue; }
            if (tok.equals("-")) { sign = -1; continue; }
            total += sign * evalCalcTerm(tok, basis);
            sign = 1;
        }
        return total;
    }

    private static int evalCalcTerm(String token, int basis) {
        token = token.trim().toLowerCase(java.util.Locale.ROOT);
        if (token.endsWith("%")) {
            try {
                float pct = Float.parseFloat(token.substring(0, token.length() - 1));
                return Math.round(basis * pct / 100f);
            } catch (NumberFormatException e) { return 0; }
        }
        if (token.endsWith("px")) {
            try { return (int) Float.parseFloat(token.substring(0, token.length() - 2)); }
            catch (NumberFormatException e) { return 0; }
        }
        try { return (int) Float.parseFloat(token); }
        catch (NumberFormatException e) { return 0; }
    }

    // ── float helper ─────────────────────────────────────────────────────────

    private static float parseFloatVal(String value) {
        if (value == null) return 0f;
        String v = value.trim().replaceAll("[^0-9.\\-]", "");
        try { return Float.parseFloat(v); }
        catch (NumberFormatException e) { return 0f; }
    }

    private static int parseMs(String s) {
        if (s == null || s.isBlank()) return 0;
        s = s.trim().toLowerCase(java.util.Locale.ROOT);
        try {
            if (s.endsWith("ms")) return Integer.parseInt(s.replace("ms", "").trim());
            if (s.endsWith("s"))  return (int)(Double.parseDouble(s.replace("s", "").trim()) * 1000);
            return Integer.parseInt(s);
        } catch (NumberFormatException e) { return 0; }
    }

    private static String stripComments(String css) {
        return css.replaceAll("(?s)/\\*.*?\\*/", " ");
    }
}
