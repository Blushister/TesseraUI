package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Developer test screen for TesseraUI v1.1 features.
 *
 * <p>Tests v-if, v-show, margin, checkbox and slider — all wired through
 * {@link TesseraTemplateRenderer} using inline HTML+CSS strings.</p>
 *
 * <p>Open with the in-game command: {@code /tessera test}</p>
 */
public final class TesseraTestScreen extends TesseraScreen {

    // ── mutable state ─────────────────────────────────────────────────────────

    private boolean vifFlag   = false;
    private boolean vshowFlag = false;
    private boolean fog       = true;
    private boolean music     = false;
    private float   vol       = 50f;
    private String  lastEvent = "none";

    private TesseraPanel root;

    public TesseraTestScreen() {
        super(Component.literal("TesseraUI v1.1 Dev Test"));
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        rebuild();
    }

    /**
     * Rebuilds the entire panel hierarchy from the current state.
     * Called on init and every time interactive state changes.
     */
    private void rebuild() {
        int pw    = Math.min(width, 422);
        int ph    = Math.min(height, 216);
        int px    = (width  - pw) / 2;
        int py    = (height - ph) / 2;
        int inner = pw - 12; // padding(6) * 2

        root = TesseraPanel.column(px, py, pw, ph)
            .background(TesseraPalette.BG0)
            .border(1, TesseraPalette.COPPER_LO)
            .padding(6).gap(4);

        // ── Title ──────────────────────────────────────────────────────────────
        root.add(lbl(inner, 10, "TesseraUI v1.1 — Dev Test Screen   (ESC to close)",
                     TesseraPalette.COPPER_HI, 7f));

        // ── Top row: v-if | v-show | margin ────────────────────────────────────
        int topH  = 88;
        int colW  = (inner - 8) / 3;           // 3 equal cols with 2 gaps of 4px
        int colW3 = inner - colW * 2 - 8;      // last col takes the remainder

        TesseraPanel top = rowPanel(inner, topH);
        top.add(buildVif  (colW,  topH));
        top.add(buildVshow(colW,  topH));
        top.add(buildMargin(colW3, topH));
        root.add(top);

        // ── Bottom row: checkbox | slider ──────────────────────────────────────
        int botH  = 78;
        int colW2 = (inner - 4) / 2;
        int colW2b = inner - colW2 - 4;

        TesseraPanel bot = rowPanel(inner, botH);
        bot.add(buildCheckbox(colW2,  botH));
        bot.add(buildSlider  (colW2b, botH));
        root.add(bot);

        // ── Event log ──────────────────────────────────────────────────────────
        TesseraPanel log = rowPanel(inner, 10);
        log.add(lbl(52, 10, "Last event:", TesseraPalette.TEXT_MUTE, 6f));
        log.add(lbl(inner - 56, 10, lastEvent, TesseraPalette.GOOD, 6f));
        root.add(log);

        root.layout();
    }

    // ── sections ──────────────────────────────────────────────────────────────

    /** Tests v-if: node absent from tree when flag=false, present when flag=true. */
    private TesseraPanel buildVif(int w, int h) {
        String html = """
            <col>
              <label class="mi">flag={{ flag }}</label>
              <label class="ok" v-if="{{ flag }}">visible (flag=true)</label>
              <label class="bad" v-if="{{ no }}">BUG: should not render</label>
              <label class="mi">(no widget when false)</label>
            </col>""";
        String css = "col{gap:3px}"
                   + " .mi{font-size:6px;color:#C2AD8E;height:9px}"
                   + " .ok{font-size:6px;color:#8FB96B;height:9px}"
                   + " .bad{font-size:6px;color:#C9533D;height:9px}";
        TesseraModel m = k -> switch (k) {
            case "flag" -> String.valueOf(vifFlag);
            case "no"   -> "false";
            default     -> null;
        };

        TesseraPanel sec = secPanel(w, h, "v-if");
        int iw = w - 8;
        int ih = h - 8 - 9 - 3 - 3 - 11; // padding + title + 2 gaps + toggle button
        sec.add(tr(html, css, m, Map.of(), iw, ih));
        sec.add(new TesseraButton(0, 0, iw, 11)
            .label("flag=" + vifFlag + "  [toggle]").fontSize(6f)
            .onClick(() -> { vifFlag = !vifFlag; lastEvent = "v-if flag → " + vifFlag; rebuild(); }));
        return sec;
    }

    /**
     * Tests v-show: widget invisible + layout space preserved when show=false.
     * The gap between the hidden label and "always here" should not collapse.
     */
    private TesseraPanel buildVshow(int w, int h) {
        String html = """
            <col>
              <label class="mi">show={{ show }}</label>
              <label class="dim" v-show="{{ show }}">[space reserved here]</label>
              <label class="acc">↑ gap preserved?</label>
              <label class="mi">always here</label>
            </col>""";
        String css = "col{gap:3px}"
                   + " .mi{font-size:6px;color:#C2AD8E;height:9px}"
                   + " .dim{font-size:6px;color:#7A6A55;height:9px}"
                   + " .acc{font-size:6px;color:#E0A84A;height:9px}";
        TesseraModel m = k -> "show".equals(k) ? String.valueOf(vshowFlag) : null;

        TesseraPanel sec = secPanel(w, h, "v-show");
        int iw = w - 8;
        int ih = h - 8 - 9 - 3 - 3 - 11;
        sec.add(tr(html, css, m, Map.of(), iw, ih));
        sec.add(new TesseraButton(0, 0, iw, 11)
            .label("show=" + vshowFlag + "  [toggle]").fontSize(6f)
            .onClick(() -> { vshowFlag = !vshowFlag; lastEvent = "v-show → " + vshowFlag; rebuild(); }));
        return sec;
    }

    /**
     * Tests margin: A has no margin, B has margin:3px, C has margin-left:8px.
     * Watch how they shift relative to each other in the row.
     */
    private TesseraPanel buildMargin(int w, int h) {
        String html = """
            <col>
              <label class="note">A: no margin</label>
              <label class="note">B: margin 3px</label>
              <label class="note">C: margin-left 8px</label>
              <row>
                <label class="box">A</label>
                <label class="box bm">B</label>
                <label class="box cm">C</label>
              </row>
            </col>""";
        String css = "col{gap:2px}"
                   + " row{align-items:center;gap:0px}"
                   + " .note{font-size:6px;color:#7A6A55;height:8px}"
                   + " .box{font-size:6px;background:#3A2A1E;color:#F0B27A;"
                   + "      width:16px;height:12px;text-align:center}"
                   + " .bm{margin:3px}"
                   + " .cm{margin-left:8px}";

        TesseraPanel sec = secPanel(w, h, "margin");
        int iw = w - 8;
        int ih = h - 8 - 9 - 3;
        sec.add(tr(html, css, TesseraModel.EMPTY, Map.of(), iw, ih));
        return sec;
    }

    /**
     * Tests checkbox: two checkboxes wired to boolean state.
     * Clicking toggles state, fires onFog/onMusic handler, rebuilds.
     */
    private TesseraPanel buildCheckbox(int w, int h) {
        String html = """
            <col>
              <row>
                <checkbox class="cb" checked="{{ fog }}" oninput="onFog"></checkbox>
                <label class="cbl">fog={{ fog }}</label>
              </row>
              <row>
                <checkbox class="cb" checked="{{ music }}" oninput="onMusic"></checkbox>
                <label class="cbl">music={{ music }}</label>
              </row>
            </col>""";
        String css = "col{gap:8px}"
                   + " row{gap:4px;align-items:center;height:14px}"
                   + " .cb{width:10px;height:10px}"
                   + " .cbl{font-size:6px;color:#C2AD8E}";
        TesseraModel m = k -> switch (k) {
            case "fog"   -> String.valueOf(fog);
            case "music" -> String.valueOf(music);
            default      -> null;
        };
        Map<String, Consumer<String>> inp = Map.of(
            "onFog",   v -> { fog   = "true".equals(v); lastEvent = "fog="   + fog;   rebuild(); },
            "onMusic", v -> { music = "true".equals(v); lastEvent = "music=" + music; rebuild(); }
        );

        TesseraPanel sec = secPanel(w, h, "checkbox");
        sec.add(tr(html, css, m, inp, w - 8, h - 8 - 9 - 3));
        return sec;
    }

    /**
     * Tests slider: drag thumb → value label updates on release.
     * Requires the host screen to forward mouseDragged (done in overrides below).
     */
    private TesseraPanel buildSlider(int w, int h) {
        String html = """
            <col>
              <label class="sll">vol: {{ vol }}</label>
              <slider class="sl" min=0 max=100 value="{{ vol }}" oninput="onVol"></slider>
              <label class="hint">drag thumb, release to fire handler</label>
            </col>""";
        String css = "col{gap:6px}"
                   + " .sll{font-size:6px;color:#C2AD8E;height:9px}"
                   + " .sl{height:10px}"
                   + " .hint{font-size:6px;color:#7A6A55;height:9px}";
        TesseraModel m = k -> "vol".equals(k) ? String.valueOf((int) vol) : null;
        Map<String, Consumer<String>> inp = Map.of(
            "onVol", v -> {
                try { vol = Float.parseFloat(v); } catch (NumberFormatException ignored) {}
                lastEvent = "vol=" + v;
                rebuild();
            }
        );

        TesseraPanel sec = secPanel(w, h, "slider");
        sec.add(tr(html, css, m, inp, w - 8, h - 8 - 9 - 3));
        return sec;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Builds a template panel from inline HTML+CSS. */
    private static TesseraPanel tr(String html, String css, TesseraModel model,
                                    Map<String, Consumer<String>> inputHandlers,
                                    int w, int h) {
        return TesseraTemplateRenderer.build(
                TesseraTemplate.fromString(html, css), model, Map.of(), inputHandlers, 0, 0, w, h);
    }

    /** Titled section card. */
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

    // ── Screen event forwarding ───────────────────────────────────────────────

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
