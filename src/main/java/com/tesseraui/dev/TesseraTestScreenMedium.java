package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Developer test screen for TesseraUI v1.1 medium-priority features.
 *
 * <p>Tests CSS variables, {@code <hr>}, tooltip, {@link TesseraMutableModel}
 * and {@link TesseraDropdown} ({@code <select>/<option>}) — all wired through
 * {@link TesseraTemplateRenderer} using inline HTML+CSS strings.</p>
 *
 * <p>Open with the in-game command: {@code /tessera test-medium}</p>
 */
public final class TesseraTestScreenMedium extends TesseraScreen {

    // ── mutable state ─────────────────────────────────────────────────────────

    private final TesseraMutableModel inputModel = TesseraMutableModel.of(Map.of("name", "player"));
    /** Persists input-field text + cursor across rebuild() calls. */
    private final Map<String, TesseraInputState> inputStates = new HashMap<>();
    private String  theme     = "dark";
    private String  lastEvent = "none";

    private TesseraPanel root;

    public TesseraTestScreenMedium() {
        super(Component.literal("TesseraUI v1.1 — Medium Features Test"));
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        int pw    = Math.min(width, 422);
        int ph    = Math.min(height, 216);
        int px    = (width  - pw) / 2;
        int py    = (height - ph) / 2;
        int inner = pw - 12; // padding(6) × 2

        root = TesseraPanel.column(px, py, pw, ph)
            .background(TesseraPalette.BG0)
            .border(1, TesseraPalette.COPPER_LO)
            .padding(6).gap(4);

        // ── Title ──────────────────────────────────────────────────────────────
        root.add(lbl(inner, 10, "TesseraUI v1.1 — Medium Features   (ESC to close)",
                     TesseraPalette.COPPER_HI, 7f));

        // ── Top row: css-vars+hr | tooltip ─────────────────────────────────────
        int topH = 82;
        int topW2  = (inner - 4) / 2;
        int topW2b = inner - topW2 - 4;

        TesseraPanel top = rowPanel(inner, topH);
        top.add(buildCssVarsAndHr(topW2,  topH));
        top.add(buildTooltip     (topW2b, topH));
        root.add(top);

        // ── Bottom row: mutable-model | dropdown ───────────────────────────────
        int botH  = 82;
        int botW2  = (inner - 4) / 2;
        int botW2b = inner - botW2 - 4;

        TesseraPanel bot = rowPanel(inner, botH);
        bot.add(buildMutableModel(botW2,  botH));
        bot.add(buildDropdown    (botW2b, botH));
        root.add(bot);

        // ── Event log ──────────────────────────────────────────────────────────
        TesseraPanel log = rowPanel(inner, 10);
        log.add(lbl(52, 10, "Last event:", TesseraPalette.TEXT_MUTE, 6f));
        log.add(lbl(inner - 56, 10, lastEvent, TesseraPalette.GOOD, 6f));
        root.add(log);

        root.layout();
    }

    // ── sections ──────────────────────────────────────────────────────────────

    /**
     * Tests CSS variables ({@code :root { --var: value }} / {@code var(--var)})
     * and the {@code <hr>} horizontal-rule element.
     */
    private TesseraPanel buildCssVarsAndHr(int w, int h) {
        String html = """
            <col>
              <label class="note">--accent via var()</label>
              <label class="accent">colored with --accent</label>
              <label class="note">hr separator below:</label>
              <hr class="sep"/>
              <label class="note">thick hr (4px) below:</label>
              <hr class="thick"/>
              <label class="note">custom color hr below:</label>
              <hr class="good"/>
            </col>""";
        String css = ":root{ --accent:#D89255; --good:#8FB96B }"
                   + " col{gap:3px}"
                   + " .note{font-size:6px;color:#7A6A55;height:8px}"
                   + " .accent{font-size:6px;color:var(--accent);height:8px}"
                   + " .sep{height:1px;color:#5A3A1C}"
                   + " .thick{height:4px;color:var(--accent)}"
                   + " .good{height:1px;color:var(--good)}";

        TesseraPanel sec = secPanel(w, h, "css-vars + hr");
        sec.add(tr(html, css, TesseraModel.EMPTY, Map.of(), w - 8, h - 8 - 9 - 3));
        return sec;
    }

    /**
     * Tests tooltip: labels and a button have {@code tooltip} attrs.
     * Hover any widget to see the tooltip box appear.
     */
    private TesseraPanel buildTooltip(int w, int h) {
        String html = """
            <col>
              <label class="ti" tooltip="I am a label tooltip!">hover me (label)</label>
              <label class="ti" tooltip="Second tooltip here">hover me too</label>
              <button class="btn" tooltip="Buttons support tooltips too">hover btn</button>
              <label class="hint">(no click handler needed)</label>
            </col>""";
        String css = "col{gap:5px}"
                   + " .ti{font-size:6px;color:#F0B27A;height:9px}"
                   + " .hint{font-size:6px;color:#7A6A55;height:9px}"
                   + " .btn{font-size:6px;height:11px}";

        TesseraPanel sec = secPanel(w, h, "tooltip");
        sec.add(tr(html, css, TesseraModel.EMPTY, Map.of(), w - 8, h - 8 - 9 - 3));
        return sec;
    }

    /**
     * Tests {@link TesseraMutableModel}: an input field whose value is reflected
     * live in the label below via model.set() + rebuild().
     */
    private TesseraPanel buildMutableModel(int w, int h) {
        // inputModel is shared — read by resolver, written by handler
        String html = """
            <col>
              <label class="note">type in the field:</label>
              <input class="inp" id="name" value="{{ name }}" oninput="onName"
                     placeholder="enter name…"/>
              <label class="note">live value:</label>
              <label class="val">→ {{ name }}</label>
            </col>""";
        String css = "col{gap:4px}"
                   + " .note{font-size:6px;color:#7A6A55;height:8px}"
                   + " .inp{height:12px}"
                   + " .val{font-size:6px;color:#8FB96B;height:8px}";

        Map<String, Consumer<String>> inp = Map.of(
            // model.set() updates the value; rebuild() re-renders the label.
            // inputStates preserves the cursor position so typing feels natural.
            "onName", v -> { inputModel.set("name", v); lastEvent = "name=" + v; rebuild(); }
        );

        TesseraPanel sec = secPanel(w, h, "TesseraMutableModel");
        // Pass inputStates so the input field keeps its cursor across rebuilds
        sec.add(TesseraTemplateRenderer.build(
                TesseraTemplate.fromString(html, css),
                inputModel, Map.of(), inp, inputStates, 0, 0, w - 8, h - 8 - 9 - 3));
        return sec;
    }

    /**
     * Tests {@link TesseraDropdown} ({@code <select>/<option>}):
     * choose a theme from the dropdown; the selection is reflected below.
     */
    private TesseraPanel buildDropdown(int w, int h) {
        TesseraModel m = k -> switch (k) {
            case "theme" -> theme;
            default      -> null;
        };
        String html = """
            <col>
              <label class="note">pick a theme:</label>
              <select value="{{ theme }}" oninput="onTheme">
                <option value="dark">Dark copper</option>
                <option value="light">Light sand</option>
                <option value="verdigris">Verdigris</option>
                <option value="danger">Danger red</option>
              </select>
              <label class="note">selected:</label>
              <label class="val">→ {{ theme }}</label>
            </col>""";
        String css = "col{gap:4px}"
                   + " .note{font-size:6px;color:#7A6A55;height:8px}"
                   + " select{height:14px}"
                   + " .val{font-size:6px;color:#F0B27A;height:8px}";

        Map<String, Consumer<String>> inp = Map.of(
            "onTheme", v -> { theme = v; lastEvent = "theme=" + v; rebuild(); }
        );

        TesseraPanel sec = secPanel(w, h, "dropdown (select)");
        sec.add(tr(html, css, m, inp, w - 8, h - 8 - 9 - 3));
        return sec;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static TesseraPanel tr(String html, String css, TesseraModel model,
                                    Map<String, Consumer<String>> inputHandlers,
                                    int w, int h) {
        return TesseraTemplateRenderer.build(
                TesseraTemplate.fromString(html, css), model, Map.of(), inputHandlers, 0, 0, w, h);
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
