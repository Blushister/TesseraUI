package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class TesseraInput extends TesseraElement {

    private TesseraInputState state = new TesseraInputState();
    private String inputId = "";
    private String placeholder = "";
    private int maxLength = 64;

    private int bgColor = 0xFF17120D;
    private int borderColor = 0xFFA0642C;
    private int focusBorderColor = 0xFFF0B27A;
    private int textColor = 0xFFF3E7D3;
    private int placeholderColor = 0xFF7A6A55;
    private int selectionColor = 0x803060A0;

    private int padH = 5, padV = 3;

    private String fontFamily = null;
    private float fontSize = 7f;
    private int fontWeight = 400;

    private Consumer<String> onChange = s -> {};
    private Consumer<String> onSubmit = s -> {};

    public TesseraInput(int x, int y, int w, int h) { super(x, y, w, h); }

    public TesseraInput state(TesseraInputState s) {
        if (s != null) {
            this.state = s;
            clampCursor();
        }
        return this;
    }

    public TesseraInputState state() { return state; }

    public TesseraInput inputId(String id)           { this.inputId = id == null ? "" : id; return this; }
    public String inputId()                       { return inputId; }

    public TesseraInput text(String t)               { this.state.text = t == null ? "" : t; clampCursor(); return this; }
    public TesseraInput placeholder(String p)        { this.placeholder = p == null ? "" : p; return this; }
    public TesseraInput maxLength(int n)             { this.maxLength = Math.max(1, n); if (state.text.length() > maxLength) state.text = state.text.substring(0, maxLength); clampCursor(); return this; }
    public TesseraInput bgColor(int c)               { this.bgColor = c; return this; }
    public TesseraInput borderColor(int c)           { this.borderColor = c; return this; }
    public TesseraInput focusBorderColor(int c)      { this.focusBorderColor = c; return this; }
    public TesseraInput textColor(int c)             { this.textColor = c; return this; }
    public TesseraInput placeholderColor(int c)      { this.placeholderColor = c; return this; }
    public TesseraInput selectionColor(int c)        { this.selectionColor = c; return this; }
    public TesseraInput padding(int h, int v)        { this.padH = Math.max(0, h); this.padV = Math.max(0, v); return this; }
    public TesseraInput font(String family, float size) { this.fontFamily = family; if (size > 0) this.fontSize = size; return this; }
    public TesseraInput fontWeight(int w)            { if (w > 0) this.fontWeight = w; return this; }
    public TesseraInput onChange(Consumer<String> h) { this.onChange = h != null ? h : s -> {}; return this; }
    public TesseraInput onSubmit(Consumer<String> h) { this.onSubmit = h != null ? h : s -> {}; return this; }

    public String getText() { return state.text; }

    @Override
    public boolean isFocused() { return state.focused; }

    @Override
    public void setFocused(boolean f) {
        if (f && !state.focused) state.focusStartMs = System.currentTimeMillis();
        this.state.focused = f;
        if (!f) state.selStart = state.cursor;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        Font font = Minecraft.getInstance().font;
        float scale = fontSize / TesseraFonts.naturalPx(fontFamily);

        g.fill(x, y, x + width, y + height, bgColor);

        int b = state.focused ? focusBorderColor : borderColor;
        g.fill(x,             y,              x + width,     y + 1,         b);
        g.fill(x,             y + height - 1, x + width,     y + height,    b);
        g.fill(x,             y,              x + 1,         y + height,    b);
        g.fill(x + width - 1, y,              x + width,     y + height,    b);

        int innerX = x + padH;
        int innerY = y + padV;
        int innerW = Math.max(0, width - padH * 2);
        int innerH = Math.max(0, height - padV * 2);

        boolean empty = state.text.isEmpty();
        String displayed = empty && !state.focused ? placeholder : state.text;
        int color = empty && !state.focused ? placeholderColor : textColor;

        int cursorPx = (int) Math.ceil(font.width(Component.literal(state.text.substring(0, state.cursor))) * scale);
        if (cursorPx - state.scrollX > innerW) state.scrollX = cursorPx - innerW;
        if (cursorPx - state.scrollX < 0)      state.scrollX = Math.max(0, cursorPx - 2);
        int totalPx = (int) Math.ceil(font.width(Component.literal(state.text)) * scale);
        if (totalPx - state.scrollX < innerW) state.scrollX = Math.max(0, totalPx - innerW);
        if (state.scrollX < 0) state.scrollX = 0;

        int textY = innerY + (innerH - (int) Math.ceil(8 * scale)) / 2;

        if (state.focused && state.selStart != state.cursor) {
            int a = Math.min(state.selStart, state.cursor);
            int z = Math.max(state.selStart, state.cursor);
            int aPx = (int) Math.ceil(font.width(Component.literal(state.text.substring(0, a))) * scale);
            int zPx = (int) Math.ceil(font.width(Component.literal(state.text.substring(0, z))) * scale);
            int sx0 = innerX - state.scrollX + aPx;
            int sx1 = innerX - state.scrollX + zPx;
            int sy0 = textY - 1;
            int sy1 = textY + (int) Math.ceil(8 * scale) + 1;
            g.fill(sx0, sy0, sx1, sy1, selectionColor);
        }

        var comp = TesseraFonts.component(displayed, fontFamily, fontWeight);
        int drawX = innerX - state.scrollX;
        if (Math.abs(scale - 1f) < 1e-3f) {
            g.drawString(font, comp, drawX, textY, color, false);
        } else {
            g.pose().pushPose();
            g.pose().translate(drawX, textY, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(font, comp, 0, 0, color, false);
            g.pose().popPose();
        }

        if (state.focused) {
            long elapsed = System.currentTimeMillis() - state.focusStartMs;
            boolean visible = (elapsed / 500L) % 2L == 0L;
            if (visible) {
                int cx = innerX - state.scrollX + cursorPx;
                int cy0 = textY - 1;
                int cy1 = textY + (int) Math.ceil(8 * scale) + 1;
                g.fill(cx, cy0, cx + 1, cy1, textColor);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return false;
        boolean inside = mx >= x && mx < x + width && my >= y && my < y + height;
        if (inside) {
            if (!state.focused) {
                state.focused = true;
                state.focusStartMs = System.currentTimeMillis();
            }
            state.cursor = computeCursorAt(mx);
            state.selStart = state.cursor;
            return true;
        }
        state.focused = false;
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!state.focused) return false;
        boolean ctrl  = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT)   != 0;

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                onSubmit.accept(state.text);
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                state.focused = false;
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (deleteSelection()) { onChange.accept(state.text); return true; }
                if (state.cursor > 0) {
                    state.text = state.text.substring(0, state.cursor - 1) + state.text.substring(state.cursor);
                    state.cursor--;
                    state.selStart = state.cursor;
                    onChange.accept(state.text);
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (deleteSelection()) { onChange.accept(state.text); return true; }
                if (state.cursor < state.text.length()) {
                    state.text = state.text.substring(0, state.cursor) + state.text.substring(state.cursor + 1);
                    state.selStart = state.cursor;
                    onChange.accept(state.text);
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (state.cursor > 0) state.cursor--;
                if (!shift) state.selStart = state.cursor;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (state.cursor < state.text.length()) state.cursor++;
                if (!shift) state.selStart = state.cursor;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                state.cursor = 0;
                if (!shift) state.selStart = state.cursor;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                state.cursor = state.text.length();
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
                        for (int i = 0; i < paste.length() && state.text.length() + filtered.length() < maxLength; i++) {
                            char ch = paste.charAt(i);
                            if (ch >= ' ' && ch != 127) filtered.append(ch);
                        }
                        String ins = filtered.toString();
                        state.text = state.text.substring(0, state.cursor) + ins + state.text.substring(state.cursor);
                        state.cursor += ins.length();
                        state.selStart = state.cursor;
                        onChange.accept(state.text);
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
        if (c < ' ' || c == 127) return false;
        deleteSelection();
        if (state.text.length() >= maxLength) return true;
        state.text = state.text.substring(0, state.cursor) + c + state.text.substring(state.cursor);
        state.cursor++;
        state.selStart = state.cursor;
        resetBlink();
        onChange.accept(state.text);
        return true;
    }

    private boolean deleteSelection() {
        if (state.selStart == state.cursor) return false;
        int a = Math.min(state.selStart, state.cursor);
        int z = Math.max(state.selStart, state.cursor);
        state.text = state.text.substring(0, a) + state.text.substring(z);
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

    private void clampCursor() {
        if (state.cursor > state.text.length()) state.cursor = state.text.length();
        if (state.cursor < 0) state.cursor = 0;
        if (state.selStart > state.text.length()) state.selStart = state.text.length();
        if (state.selStart < 0) state.selStart = 0;
    }

    private void resetBlink() {
        state.focusStartMs = System.currentTimeMillis();
    }

    private int computeCursorAt(double mx) {
        Font font = Minecraft.getInstance().font;
        float scale = fontSize / TesseraFonts.naturalPx(fontFamily);
        int relX = (int) (mx - x - padH + state.scrollX);
        if (relX <= 0) return 0;
        int acc = 0;
        for (int i = 0; i < state.text.length(); i++) {
            int chW = (int) Math.ceil(font.width(Component.literal(String.valueOf(state.text.charAt(i)))) * scale);
            if (relX < acc + chW / 2) return i;
            acc += chW;
        }
        return state.text.length();
    }

    @Override
    public boolean hasClickHandler() { return true; }
}
