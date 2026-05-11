package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Multi-line text area widget for TesseraUI.
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>Multi-line editing — Enter inserts a newline</li>
 *   <li>Cursor navigation: ←/→/↑/↓, Home/End, Ctrl+Home/End</li>
 *   <li>Selection with Shift + navigation keys</li>
 *   <li>Ctrl+A/C/X/V (clipboard), Backspace/Delete</li>
 *   <li>Vertical scroll when content overflows, with a mini scrollbar</li>
 *   <li>Placeholder text when empty and unfocused</li>
 * </ul>
 *
 * <p>CSS properties wired by {@code TesseraTemplateRenderer}: {@code width}, {@code height},
 * {@code rows} (HTML attr, default 4), {@code background}, {@code color},
 * {@code border-color}, {@code padding}, {@code font-size}, {@code font-weight},
 * {@code font-family}.  Focus colour override via {@code :focus { border-color: ... }}.</p>
 */
public class TesseraTextArea extends TesseraElement {

    // ── State ──────────────────────────────────────────────────────────────

    private TesseraInputState state;

    // ── Style ──────────────────────────────────────────────────────────────

    private String placeholder = "";
    private int maxLength = 4096;

    private int bgColor          = 0xFF17120D;
    private int borderColor      = 0xFFA0642C;
    private int focusBorderColor = 0xFFF0B27A;
    private int textColor        = 0xFFF3E7D3;
    private int placeholderColor = 0xFF7A6A55;
    private int selectionColor   = 0x803060A0;
    private int scrollbarBg      = 0xFF2A1E12;
    private int scrollbarThumb   = TesseraPalette.COPPER_LO;

    private int   padH = 5, padV = 3;
    private String fontFamily = null;
    private float  fontSize   = 7f;
    private int    fontWeight = 400;

    private Consumer<String> onChange = s -> {};

    // ── Constructor ────────────────────────────────────────────────────────

    public TesseraTextArea(int x, int y, int w, int h) {
        super(x, y, w, h);
        this.state = new TesseraInputState();
    }

    // ── Fluent builder ─────────────────────────────────────────────────────

    public TesseraTextArea state(TesseraInputState s)            { if (s != null) this.state = s; return this; }
    public TesseraInputState state()                             { return state; }
    public TesseraTextArea text(String t)                        { this.state.text = t != null ? t : ""; return this; }
    public TesseraTextArea placeholder(String p)                 { this.placeholder = p != null ? p : ""; return this; }
    public TesseraTextArea maxLength(int n)                      { this.maxLength = Math.max(1, n); return this; }
    public TesseraTextArea bgColor(int c)                        { this.bgColor = c; return this; }
    public TesseraTextArea borderColor(int c)                    { this.borderColor = c; return this; }
    public TesseraTextArea focusBorderColor(int c)               { this.focusBorderColor = c; return this; }
    public TesseraTextArea textColor(int c)                      { this.textColor = c; return this; }
    public TesseraTextArea placeholderColor(int c)               { this.placeholderColor = c; return this; }
    public TesseraTextArea padding(int h, int v)                 { this.padH = Math.max(0, h); this.padV = Math.max(0, v); return this; }
    public TesseraTextArea font(String family, float size)       { this.fontFamily = family; if (size > 0) this.fontSize = size; return this; }
    public TesseraTextArea fontWeight(int w)                     { if (w > 0) this.fontWeight = w; return this; }
    public TesseraTextArea onChange(Consumer<String> h)          { this.onChange = h != null ? h : s -> {}; return this; }
    public String getText()                                      { return state.text; }

    @Override public boolean isFocused()         { return state.focused; }
    @Override public boolean hasClickHandler()   { return true; }

    @Override
    public void setFocused(boolean f) {
        if (f && !state.focused) state.focusStartMs = System.currentTimeMillis();
        state.focused = f;
        if (!f) state.selStart = state.cursor;
    }

    // ── Render ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        if (!visible) return;
        Font font = Minecraft.getInstance().font;
        float sc   = scale();
        int   lh   = lineHeight(sc);
        String[] ls = lines();

        // ── Background + border ────────────────────────────────────────────
        g.fill(x, y, x + width, y + height, bgColor);
        int bc = state.focused ? focusBorderColor : borderColor;
        g.fill(x,             y,              x + width,     y + 1,          bc);
        g.fill(x,             y + height - 1, x + width,     y + height,     bc);
        g.fill(x,             y,              x + 1,         y + height,     bc);
        g.fill(x + width - 1, y,              x + width,     y + height,     bc);

        // ── Inner geometry ─────────────────────────────────────────────────
        int SCROLLBAR_W = 3;
        int innerX = x + padH;
        int innerY = y + padV;
        int innerW = Math.max(1, width  - padH * 2 - SCROLLBAR_W);
        int innerH = Math.max(1, height - padV * 2);
        int totalH = ls.length * lh;
        int maxSY  = Math.max(0, totalH - innerH);

        // ── Scroll-into-view clamping (runs every frame) ───────────────────
        int[] curLC = cursorToLineCol(state.cursor);
        int curTop    = curLC[0] * lh;
        int curBottom = curTop + lh;
        if (curBottom - state.scrollY > innerH) state.scrollY = curBottom - innerH;
        if (curTop    - state.scrollY < 0)      state.scrollY = curTop;
        state.scrollY = Math.max(0, Math.min(state.scrollY, maxSY));

        // ── Scissor inner rect ─────────────────────────────────────────────
        g.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);

        // ── Selection highlight ────────────────────────────────────────────
        if (state.focused && state.selStart != state.cursor) {
            int selA = Math.min(state.selStart, state.cursor);
            int selZ = Math.max(state.selStart, state.cursor);
            int[] lcA = cursorToLineCol(selA);
            int[] lcZ = cursorToLineCol(selZ);
            for (int li = lcA[0]; li <= lcZ[0] && li < ls.length; li++) {
                int colStart = (li == lcA[0]) ? lcA[1] : 0;
                int colEnd   = (li == lcZ[0]) ? lcZ[1] : ls[li].length();
                int sx0 = innerX + colPx(colStart, ls[li], font, sc);
                int sx1 = innerX + colPx(colEnd,   ls[li], font, sc);
                int sy0 = innerY + li * lh - state.scrollY;
                int sy1 = sy0 + lh;
                g.fill(sx0, Math.max(sy0, innerY),
                       Math.max(sx0 + 1, sx1), Math.min(sy1, innerY + innerH),
                       selectionColor);
            }
        }

        // ── Text lines (or placeholder) ────────────────────────────────────
        if (state.text.isEmpty() && !state.focused && !placeholder.isEmpty()) {
            var comp = TesseraFonts.component(placeholder, fontFamily, fontWeight);
            drawLine(g, font, comp, innerX, innerY, sc, placeholderColor);
        } else {
            for (int li = 0; li < ls.length; li++) {
                int ty = innerY + li * lh - state.scrollY;
                if (ty + lh < innerY || ty > innerY + innerH) continue; // culled
                var comp = TesseraFonts.component(ls[li], fontFamily, fontWeight);
                drawLine(g, font, comp, innerX, ty, sc, textColor);
            }
        }

        // ── Blinking cursor ────────────────────────────────────────────────
        if (state.focused) {
            long elapsed = System.currentTimeMillis() - state.focusStartMs;
            if ((elapsed / 500L) % 2L == 0L) {
                String curLine = curLC[0] < ls.length ? ls[curLC[0]] : "";
                int cx  = innerX + colPx(curLC[1], curLine, font, sc);
                int cy0 = innerY + curLC[0] * lh - state.scrollY;
                int cy1 = cy0 + lh;
                if (cy1 > innerY && cy0 < innerY + innerH) {
                    g.fill(cx, Math.max(cy0, innerY), cx + 1, Math.min(cy1, innerY + innerH), textColor);
                }
            }
        }

        g.disableScissor();

        // ── Scrollbar ──────────────────────────────────────────────────────
        if (totalH > innerH) {
            int sbX    = x + width - SCROLLBAR_W - 1;
            int trackH = height - padV * 2;
            int thumbH = Math.max(4, trackH * innerH / Math.max(1, totalH));
            int thumbY = y + padV + (int) ((long) state.scrollY * (trackH - thumbH) / Math.max(1, maxSY));
            g.fill(sbX, y + padV, sbX + SCROLLBAR_W - 1, y + padV + trackH, scrollbarBg);
            g.fill(sbX, thumbY,   sbX + SCROLLBAR_W - 1, thumbY + thumbH,   scrollbarThumb);
        }
    }

    private static void drawLine(GuiGraphics g, Font font, net.minecraft.network.chat.Component comp,
                                 int drawX, int drawY, float sc, int color) {
        if (Math.abs(sc - 1f) < 1e-3f) {
            g.drawString(font, comp, drawX, drawY, color, false);
        } else {
            g.pose().pushPose();
            g.pose().translate(drawX, drawY, 0);
            g.pose().scale(sc, sc, 1f);
            g.drawString(font, comp, 0, 0, color, false);
            g.pose().popPose();
        }
    }

    // ── Mouse ──────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return false;
        boolean inside = mx >= x && mx < x + width && my >= y && my < y + height;
        if (inside) {
            if (!state.focused) {
                state.focused = true;
                state.focusStartMs = System.currentTimeMillis();
            }
            state.cursor = computeCursorAt(mx, my);
            state.selStart = state.cursor;
            return true;
        }
        state.focused = false;
        return false;
    }

    // ── Keyboard ───────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!state.focused) return false;
        boolean ctrl  = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT)   != 0;

        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                state.focused = false;
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                insertText("\n");
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (deleteSelection()) { onChange.accept(state.text); return true; }
                if (state.cursor > 0) {
                    state.text = state.text.substring(0, state.cursor - 1)
                               + state.text.substring(state.cursor);
                    state.cursor--;
                    state.selStart = state.cursor;
                    onChange.accept(state.text);
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (deleteSelection()) { onChange.accept(state.text); return true; }
                if (state.cursor < state.text.length()) {
                    state.text = state.text.substring(0, state.cursor)
                               + state.text.substring(state.cursor + 1);
                    state.selStart = state.cursor;
                    onChange.accept(state.text);
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (state.cursor > 0) {
                    if (ctrl) {
                        int p = state.cursor - 1;
                        while (p > 0 && !isBoundary(state.text.charAt(p - 1))) p--;
                        state.cursor = p;
                    } else {
                        state.cursor--;
                    }
                }
                if (!shift) state.selStart = state.cursor;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (state.cursor < state.text.length()) {
                    if (ctrl) {
                        int p = state.cursor + 1;
                        while (p < state.text.length() && !isBoundary(state.text.charAt(p))) p++;
                        state.cursor = p;
                    } else {
                        state.cursor++;
                    }
                }
                if (!shift) state.selStart = state.cursor;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                int[] lc = cursorToLineCol(state.cursor);
                state.cursor = lc[0] > 0
                        ? lineColToCursor(lc[0] - 1, lc[1])
                        : 0;
                if (!shift) state.selStart = state.cursor;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                int[] lc = cursorToLineCol(state.cursor);
                String[] ls = lines();
                state.cursor = lc[0] < ls.length - 1
                        ? lineColToCursor(lc[0] + 1, lc[1])
                        : state.text.length();
                if (!shift) state.selStart = state.cursor;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                if (ctrl) {
                    state.cursor = 0;
                } else {
                    int[] lc = cursorToLineCol(state.cursor);
                    state.cursor = lineColToCursor(lc[0], 0);
                }
                if (!shift) state.selStart = state.cursor;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                if (ctrl) {
                    state.cursor = state.text.length();
                } else {
                    int[] lc  = cursorToLineCol(state.cursor);
                    String[] ls = lines();
                    int lineLen = lc[0] < ls.length ? ls[lc[0]].length() : 0;
                    state.cursor = lineColToCursor(lc[0], lineLen);
                }
                if (!shift) state.selStart = state.cursor;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                float sc = scale();
                int lh = lineHeight(sc);
                int innerH = Math.max(1, height - padV * 2);
                int visibleLines = Math.max(1, innerH / lh);
                int[] lc = cursorToLineCol(state.cursor);
                state.cursor = lineColToCursor(Math.max(0, lc[0] - visibleLines), lc[1]);
                if (!shift) state.selStart = state.cursor;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                float sc = scale();
                int lh = lineHeight(sc);
                int innerH = Math.max(1, height - padV * 2);
                int visibleLines = Math.max(1, innerH / lh);
                String[] ls = lines();
                int[] lc = cursorToLineCol(state.cursor);
                state.cursor = lineColToCursor(Math.min(ls.length - 1, lc[0] + visibleLines), lc[1]);
                if (!shift) state.selStart = state.cursor;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) { state.selStart = 0; state.cursor = state.text.length(); return true; }
            }
            case GLFW.GLFW_KEY_C -> {
                if (ctrl) {
                    String sel = currentSelection();
                    if (!sel.isEmpty()) Minecraft.getInstance().keyboardHandler.setClipboard(sel);
                    return true;
                }
            }
            case GLFW.GLFW_KEY_X -> {
                if (ctrl) {
                    String sel = currentSelection();
                    if (!sel.isEmpty()) {
                        Minecraft.getInstance().keyboardHandler.setClipboard(sel);
                        deleteSelection();
                        onChange.accept(state.text);
                    }
                    return true;
                }
            }
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) {
                    String paste = Minecraft.getInstance().keyboardHandler.getClipboard();
                    if (paste != null && !paste.isEmpty()) {
                        deleteSelection();
                        StringBuilder filtered = new StringBuilder();
                        for (int i = 0; i < paste.length()
                                && state.text.length() + filtered.length() < maxLength; i++) {
                            char ch = paste.charAt(i);
                            if (ch >= ' ' || ch == '\n') filtered.append(ch);
                        }
                        insertText(filtered.toString());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!state.focused) return false;
        // Accept printable chars; explicitly allow '\n' here but GLFW_KEY_ENTER handles it above
        if (c < ' ' && c != '\n') return false;
        if (c == 127) return false; // DEL char
        insertText(String.valueOf(c));
        return true;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private float scale() {
        return fontSize / TesseraFonts.naturalPx(fontFamily);
    }

    private int lineHeight(float sc) {
        return (int) Math.ceil(8 * sc) + 2;
    }

    /** Split text into lines, preserving empty trailing entries after final '\n'. */
    private String[] lines() {
        return state.text.split("\n", -1);
    }

    /** Flat cursor index → [lineIndex, colIndex]. */
    private int[] cursorToLineCol(int cursor) {
        String[] ls = lines();
        int pos = 0;
        for (int i = 0; i < ls.length; i++) {
            int lineEnd = pos + ls[i].length();
            if (cursor <= lineEnd || i == ls.length - 1) {
                return new int[]{i, Math.min(cursor - pos, ls[i].length())};
            }
            pos = lineEnd + 1; // +1 for the '\n'
        }
        return new int[]{0, 0};
    }

    /** [lineIndex, colIndex] → flat cursor index. */
    private int lineColToCursor(int line, int col) {
        String[] ls = lines();
        int pos = 0;
        for (int i = 0; i < line && i < ls.length; i++) {
            pos += ls[i].length() + 1; // +1 for '\n'
        }
        int lineLen = (line < ls.length) ? ls[line].length() : 0;
        return pos + Math.min(col, lineLen);
    }

    /** X pixel offset of column {@code col} within {@code lineText} at scale {@code sc}. */
    private static int colPx(int col, String lineText, Font font, float sc) {
        if (col <= 0) return 0;
        String sub = col <= lineText.length() ? lineText.substring(0, col) : lineText;
        return (int) Math.ceil(font.width(Component.literal(sub)) * sc);
    }

    private int computeCursorAt(double mx, double my) {
        Font   font = Minecraft.getInstance().font;
        float  sc   = scale();
        int    lh   = lineHeight(sc);
        String[] ls = lines();

        int innerX = x + padH;
        int innerY = y + padV;

        // Which line?
        int relY     = (int)(my - innerY) + state.scrollY;
        int lineIdx  = Math.max(0, Math.min(relY / lh, ls.length - 1));

        // Which column?
        String line  = ls[lineIdx];
        int    relX  = (int)(mx - innerX);
        if (relX <= 0) return lineColToCursor(lineIdx, 0);

        int acc = 0;
        for (int i = 0; i < line.length(); i++) {
            int chW = (int) Math.ceil(font.width(Component.literal(String.valueOf(line.charAt(i)))) * sc);
            if (relX < acc + chW / 2) return lineColToCursor(lineIdx, i);
            acc += chW;
        }
        return lineColToCursor(lineIdx, line.length());
    }

    private void insertText(String s) {
        deleteSelection();
        int space = maxLength - state.text.length();
        if (space <= 0) return;
        if (s.length() > space) s = s.substring(0, space);
        state.text   = state.text.substring(0, state.cursor) + s + state.text.substring(state.cursor);
        state.cursor += s.length();
        state.selStart = state.cursor;
        resetBlink();
        onChange.accept(state.text);
    }

    private boolean deleteSelection() {
        if (state.selStart == state.cursor) return false;
        int a = Math.min(state.selStart, state.cursor);
        int z = Math.max(state.selStart, state.cursor);
        state.text   = state.text.substring(0, a) + state.text.substring(z);
        state.cursor = a;
        state.selStart = a;
        return true;
    }

    private String currentSelection() {
        if (state.selStart == state.cursor) return "";
        int a = Math.min(state.selStart, state.cursor);
        int z = Math.max(state.selStart, state.cursor);
        return state.text.substring(a, z);
    }

    private void resetBlink() { state.focusStartMs = System.currentTimeMillis(); }

    private static boolean isBoundary(char c) { return c == ' ' || c == '\n'; }
}
