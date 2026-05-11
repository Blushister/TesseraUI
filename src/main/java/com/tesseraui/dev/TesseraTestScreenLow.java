package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Developer test screen for TesseraUI v1.1 low-priority features.
 *
 * <p>Tests CSS inheritance, {@code :focus} pseudo-class, {@code :nth-child()} selectors
 * and the readable-error panel — all wired through {@link TesseraTemplateRenderer}
 * using inline HTML+CSS strings.</p>
 *
 * <p>Open with the in-game command: {@code /tessera test-low}</p>
 */
public final class TesseraTestScreenLow extends TesseraScreen {

    // ── state ─────────────────────────────────────────────────────────────────

    private String lastEvent = "none";
    private TesseraPanel root;

    public TesseraTestScreenLow() {
        super(Component.literal("TesseraUI v1.1 — Low Priority Test"));
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void init() { rebuild(); }

    private void rebuild() {
        int pw    = Math.min(width, 422);
        int ph    = Math.min(height, 216);
        int px    = (width  - pw) / 2;
        int py    = (height - ph) / 2;
        int inner = pw - 12;

        root = TesseraPanel.column(px, py, pw, ph)
            .background(TesseraPalette.BG0)
            .border(1, TesseraPalette.COPPER_LO)
            .padding(6).gap(4);

        root.add(lbl(inner, 10, "TesseraUI v1.1 — Low Priority   (ESC to close)",
                     TesseraPalette.COPPER_HI, 7f));

        // ── Top row: CSS inheritance | :focus ─────────────────────────────────
        int topH = 82;
        int colW2  = (inner - 4) / 2;
        int colW2b = inner - colW2 - 4;

        TesseraPanel top = rowPanel(inner, topH);
        top.add(buildInheritance(colW2,  topH));
        top.add(buildFocus      (colW2b, topH));
        root.add(top);

        // ── Bottom row: nth-child | error panel ───────────────────────────────
        int botH = 82;
        TesseraPanel bot = rowPanel(inner, botH);
        bot.add(buildNthChild (colW2,  botH));
        bot.add(buildError    (colW2b, botH));
        root.add(bot);

        // ── Event log ─────────────────────────────────────────────────────────
        TesseraPanel log = rowPanel(inner, 10);
        log.add(lbl(52, 10, "Last event:", TesseraPalette.TEXT_MUTE, 6f));
        log.add(lbl(inner - 56, 10, lastEvent, TesseraPalette.GOOD, 6f));
        root.add(log);

        root.layout();
    }

    // ── sections ──────────────────────────────────────────────────────────────

    /**
     * Tests CSS inheritance: a parent {@code <col>} sets {@code color} and
     * {@code font-size}; children with no explicit color/size should inherit.
     */
    private TesseraPanel buildInheritance(int w, int h) {
        String html = """
            <col class="parent">
              <label>inherits color + size</label>
              <label>also inherited</label>
              <label class="override">overrides color only</label>
              <label class="sm">overrides size (6px)</label>
            </col>""";
        String css = ".parent{color:#F0B27A;font-size:7px;gap:4px}"
                   + " .override{color:#8FB96B}"
                   + " .sm{font-size:6px}";

        TesseraPanel sec = secPanel(w, h, "CSS inheritance");
        sec.add(tr(html, css, w - 8, h - 8 - 9 - 3));
        return sec;
    }

    /**
     * Tests the {@code :focus} pseudo-class on an input field.
     * Click the field to focus it — border should change to the focus color.
     */
    private TesseraPanel buildFocus(int w, int h) {
        String html = """
            <col>
              <label class="note">click field to focus:</label>
              <input class="inp" id="fi" placeholder="click me…" oninput="onFocus"/>
              <label class="note">:focus { border-color: #8FB96B }</label>
              <label class="note">default border: copper-lo</label>
            </col>""";
        String css = "col{gap:5px}"
                   + " .note{font-size:6px;color:#7A6A55;height:8px}"
                   + " .inp{height:13px;border-color:#A0642C}"
                   + " .inp:focus{border-color:#8FB96B}";

        Map<String, Consumer<String>> inp = Map.of(
            "onFocus", v -> { lastEvent = "focus-input=" + v; }
        );

        TesseraPanel sec = secPanel(w, h, ":focus");
        sec.add(trWithHandlers(html, css, inp, w - 8, h - 8 - 9 - 3));
        return sec;
    }

    /**
     * Tests {@code :nth-child()} selectors: odd rows get a dark background,
     * even rows a slightly lighter one, and the 3rd item gets a special color.
     */
    private TesseraPanel buildNthChild(int w, int h) {
        String html = """
            <col>
              <label class="row">Row 1  (odd → dark)</label>
              <label class="row">Row 2  (even → mid)</label>
              <label class="row">Row 3  (odd + nth(3))</label>
              <label class="row">Row 4  (even → mid)</label>
              <label class="row">Row 5  (odd → dark)</label>
            </col>""";
        String css = "col{gap:1px}"
                   + " .row{font-size:6px;color:#C2AD8E;height:11px;padding-left:4px}"
                   + " .row:nth-child(odd){background:#2A2019;color:#F0B27A}"
                   + " .row:nth-child(even){background:#1F1812;color:#C2AD8E}"
                   + " .row:nth-child(3){color:#8FB96B}";

        TesseraPanel sec = secPanel(w, h, "nth-child()");
        sec.add(tr(html, css, w - 8, h - 8 - 9 - 3));
        return sec;
    }

    /**
     * Tests the readable-error panel: builds a deliberately broken template
     * (malformed {@code {{ }} } binding) and expects a friendly error panel
     * instead of a crash.
     */
    private TesseraPanel buildError(int w, int h) {
        // Valid template that demonstrates what the error panel looks like
        // by triggering a divide-by-zero in a custom model
        TesseraModel brokenModel = k -> { throw new RuntimeException("Simulated template error: " + k); };

        String html = "<col><label>{{ value }}</label></col>";
        String css  = "col{gap:2px}";

        TesseraPanel sec = secPanel(w, h, "error panel");
        // The renderer catches the exception and returns the error panel
        sec.add(TesseraTemplateRenderer.build(
                TesseraTemplate.fromString(html, css),
                brokenModel, Map.of(), 0, 0, w - 8, h - 8 - 9 - 3));
        return sec;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static TesseraPanel tr(String html, String css, int w, int h) {
        return TesseraTemplateRenderer.build(
                TesseraTemplate.fromString(html, css),
                TesseraModel.EMPTY, Map.of(), 0, 0, w, h);
    }

    private static TesseraPanel trWithHandlers(String html, String css,
                                                Map<String, Consumer<String>> inp,
                                                int w, int h) {
        return TesseraTemplateRenderer.build(
                TesseraTemplate.fromString(html, css),
                TesseraModel.EMPTY, Map.of(), inp, 0, 0, w, h);
    }

    private static TesseraPanel secPanel(int w, int h, String title) {
        TesseraPanel sec = TesseraPanel.column(0, 0, w, h)
            .background(TesseraPalette.BG1)
            .border(1, TesseraPalette.COPPER_DEEP)
            .padding(4).gap(3);
        sec.add(lbl(w - 8, 9, "[ " + title + " ]", TesseraPalette.COPPER, 6f));
        return sec;
    }

    private static TesseraLabel lbl(int w, int h, String text, int color, float size) {
        return new TesseraLabel(0, 0, w, h, text).color(color).fontSize(size);
    }

    private static TesseraPanel rowPanel(int w, int h) {
        return TesseraPanel.row(0, 0, w, h).gap(4).alignItems("flex-start");
    }

    // ── TesseraScreen ─────────────────────────────────────────────────────────

    @Override
    protected TesseraPanel tesseraRoot() { return root; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        if (root != null) root.render(g, mx, my);
        super.render(g, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (root != null && root.mouseClicked(mx, my, btn)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (root != null) root.mouseReleased(mx, my, btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (root != null && root.keyPressed(key, scan, mods)) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (root != null && root.charTyped(c, mods)) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
