package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v1.0 features:
 * {@code <table>/<thead>/<tbody>/<tr>/<td>/<th>},
 * {@code text-decoration: underline | line-through}, and
 * {@code <img>} (arbitrary ResourceLocation texture).
 *
 * <p>Open with the in-game command: {@code /tessera test-table}</p>
 */
public final class TesseraTestScreenTable extends TesseraScreen {

    private TesseraPanel root;

    public TesseraTestScreenTable() {
        super(Component.literal("TesseraUI v1.0 — Table / Text-dec / Img Test"));
    }

    @Override
    protected void init() { rebuild(); }

    private void rebuild() {
        int pw  = Math.min(width,  444);
        int ph  = Math.min(height, 280);
        int px  = (width  - pw) / 2;
        int py  = (height - ph) / 2;
        int iW  = pw - 12;          // inner width (padding 6 each side)
        int cW  = (iW - 4) / 2;    // half-column width
        int cWb = iW - cW - 4;     // other half

        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(6).gap(4);

        root.add(new TesseraLabel(0, 0, iW, 10,
                "TesseraUI v1.0 — Table / Text-dec / Img   (ESC to close)")
                .color(TesseraPalette.COPPER_HI).fontSize(7f));

        // ── Top row: table | text-decoration ─────────────────────────────────
        TesseraPanel top = TesseraPanel.row(0, 0, iW, 130).gap(4).alignItems("flex-start");
        top.add(buildTable    (cW,  130));
        top.add(buildTextDec  (cWb, 130));
        root.add(top);

        // ── Bottom row: img (full width) ──────────────────────────────────────
        root.add(buildImg(iW, 100));

        root.layout();
    }

    // ── sections ─────────────────────────────────────────────────────────────

    /** {@code <table>} with {@code <thead>} and {@code <tbody>}. */
    private TesseraPanel buildTable(int w, int h) {
        String html = """
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Value</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>copper_ore</td>
                  <td>Block</td>
                  <td>12</td>
                </tr>
                <tr>
                  <td>iron_ingot</td>
                  <td>Item</td>
                  <td>64</td>
                </tr>
                <tr>
                  <td>oak_log</td>
                  <td>Block</td>
                  <td>3</td>
                </tr>
              </tbody>
            </table>""";
        String css = "table{gap:1px;background:#0D0A07}"
                   + " th{font-size:6px;color:#D89255;background:#1F1812;padding:2px 3px}"
                   + " td{font-size:6px;color:#F3E7D3;background:#120F0A;padding:2px 3px}";
        return secPanel(w, h, "table / thead / tbody / td / th", html, css);
    }

    /** {@code text-decoration: underline} and {@code line-through}. */
    private TesseraPanel buildTextDec(int w, int h) {
        String html = """
            <col>
              <p>Normal paragraph text</p>
              <p class="ul">Underline decoration</p>
              <p class="lt">Line-through decoration</p>
              <hr/>
              <label class="lbl">Plain label — underline</label>
              <label class="ltl">Plain label — line-through</label>
              <hr/>
              <span class="sp">Span with underline</span>
            </col>""";
        String css = "col{gap:4px}"
                   + " p{font-size:7px;color:#F3E7D3}"
                   + " .ul{text-decoration:underline;color:#8FB96B}"
                   + " .lt{text-decoration:line-through;color:#D07070}"
                   + " hr{color:#3A2E22;height:1px;margin:1px 0}"
                   + " .lbl{font-size:6px;color:#C2AD8E;text-decoration:underline}"
                   + " .ltl{font-size:6px;color:#7A6A55;text-decoration:line-through}"
                   + " .sp{font-size:7px;color:#F0B27A;text-decoration:underline}";
        return secPanel(w, h, "text-decoration", html, css);
    }

    /** {@code <img>} referencing Minecraft item textures (16×16, always present in the jar). */
    private TesseraPanel buildImg(int w, int h) {
        // Cards need an explicit width so fitContentWidth() can shrink them correctly.
        // Row needs an explicit height because cards inside a row inherit that height
        // (stretchChildHeight = true) and naturalHeight defaults to 12 px which clips 16 px icons.
        String html = """
            <col>
              <row>
                <div class="card">
                  <img src="minecraft:textures/item/diamond.png"
                       width="16" height="16"/>
                  <label class="cap">diamond</label>
                </div>
                <div class="card">
                  <img src="minecraft:textures/item/emerald.png"
                       width="16" height="16"/>
                  <label class="cap">emerald</label>
                </div>
                <div class="card">
                  <img src="minecraft:textures/item/iron_ingot.png"
                       width="16" height="16"/>
                  <label class="cap">iron_ingot</label>
                </div>
                <div class="card">
                  <img src="minecraft:textures/item/gold_ingot.png"
                       width="16" height="16"/>
                  <label class="cap">gold_ingot</label>
                </div>
                <div class="card">
                  <img src="minecraft:textures/item/copper_ingot.png"
                       width="16" height="16"/>
                  <label class="cap">copper_ingot</label>
                </div>
              </row>
              <p class="note">
                &lt;img src="namespace:path" width="W" height="H"/&gt; loads any ResourceLocation inline.
              </p>
            </col>""";
        // row height:36 → cards stretch to 36 px, giving 28 px inner height (enough for 16 px icon).
        // card width:58 → 5 × 58 + 4 × 6 gaps = 314 px, fits comfortably in the ~424 px content area.
        String css = "col{gap:5px}"
                   + " row{gap:6px;align-items:center;height:36px}"
                   + " .card{gap:2px;align-items:center;padding:3px;background:#120F0A;"
                   +         "border:1px solid #3A2E22;width:58px}"
                   + " .cap{font-size:6px;color:#7A6A55}"
                   + " .note{font-size:6px;color:#7A6A55}";
        return secPanel(w, h, "img — arbitrary ResourceLocation textures", html, css);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TesseraPanel secPanel(int w, int h, String title, String html, String css) {
        TesseraPanel sec = TesseraPanel.column(0, 0, w, h)
                .background(TesseraPalette.BG1)
                .border(1, TesseraPalette.COPPER_DEEP)
                .padding(4).gap(3);
        sec.add(new TesseraLabel(0, 0, w - 8, 9, "[ " + title + " ]")
                .color(TesseraPalette.COPPER).fontSize(6f));
        sec.add(TesseraTemplateRenderer.build(
                TesseraTemplate.fromString(html, css),
                TesseraModel.EMPTY, Map.of(),
                0, 0, w - 8, h - 8 - 9 - 3));
        return sec;
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
