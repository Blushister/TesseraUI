package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v1.1 feature: {@code border-radius}.
 *
 * <p>Three sections arranged in two rows:</p>
 * <ul>
 *   <li><b>Top-left</b> — radius progression (0 → 2 → 4 → 6 → 8 px) on plain divs</li>
 *   <li><b>Top-right</b> — radius + border combinations (solid border, colored border, hover)</li>
 *   <li><b>Bottom</b> — card grid with radius + real content (icon + label + button)</li>
 * </ul>
 *
 * <p>Open with the in-game command: {@code /tessera test-radius}</p>
 */
public final class TesseraTestScreenRadius extends TesseraScreen {

    private TesseraPanel root;

    public TesseraTestScreenRadius() {
        super(Component.literal("TesseraUI v1.1 — border-radius Test"));
    }

    @Override
    protected void init() { rebuild(); }

    private void rebuild() {
        int pw  = Math.min(width,  444);
        int ph  = Math.min(height, 320);
        int px  = (width  - pw) / 2;
        int py  = (height - ph) / 2;
        int iW  = pw - 12;
        int cW  = (iW - 4) / 2;
        int cWb = iW - cW - 4;

        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(6).gap(4);

        root.add(new TesseraLabel(0, 0, iW, 10,
                "TesseraUI v1.1 — border-radius   (ESC to close)")
                .color(TesseraPalette.COPPER_HI).fontSize(7f));

        // ── Top row: radius progression | radius + border ─────────────────────
        // 168 px: combos section needs intro(8) + 6×div(18) + 5×gap(4) + title(12) + padding(8) = 160 px
        // progression needs intro(8) + 6×div(18) + 5×gap(4) + title(12) + padding(8) = 160 px (6 steps now)
        TesseraPanel top = TesseraPanel.row(0, 0, iW, 168).gap(4).alignItems("flex-start");
        top.add(buildRadiusProgression(cW,  168));
        top.add(buildRadiusBorder    (cWb, 168));
        root.add(top);

        // ── Bottom row: card grid (full width) ────────────────────────────────
        root.add(buildCardGrid(iW, 106));

        root.layout();
    }

    // ── sections ──────────────────────────────────────────────────────────────

    /** Radius progression: same div, increasing border-radius 0–8 px. */
    private TesseraPanel buildRadiusProgression(int w, int h) {
        String html = """
            <col>
              <p class="intro">border-radius: 0 → 2 → 4 → 6 → 8 → 12</p>
              <div class="r0" ><label class="lbl">radius: 0px (square)</label></div>
              <div class="r2" ><label class="lbl">radius: 2px</label></div>
              <div class="r4" ><label class="lbl">radius: 4px</label></div>
              <div class="r6" ><label class="lbl">radius: 6px</label></div>
              <div class="r8" ><label class="lbl">radius: 8px</label></div>
              <div class="r12"><label class="lbl">radius: 12px (pill)</label></div>
            </col>""";
        String css =
                "col{gap:4px}"
              + " .intro{font-size:6px;color:#7A6A55}"
              + " div{padding:3px 5px;background:#1F1812;border:1px solid #3A2E22}"
              + " .lbl{font-size:6px;color:#C2AD8E}"
              + " .r0 {border-radius:0}"
              + " .r2 {border-radius:2px;border-color:#5A3A1C}"
              + " .r4 {border-radius:4px;border-color:#A0642C}"
              + " .r6 {border-radius:6px;border-color:#D89255}"
              + " .r8 {border-radius:8px;border-color:#F0B27A}"
              + " .r12{border-radius:12px;border-color:#F3E7D3}";
        return secPanel(w, h, "radius progression", html, css);
    }

    /** Radius with various border styles: no border, colored border, thick border. */
    private TesseraPanel buildRadiusBorder(int w, int h) {
        String html = """
            <col>
              <p class="intro">radius + border combinations</p>
              <div class="bg-only">
                <label class="lbl">bg only, radius 4px</label>
              </div>
              <div class="thin">
                <label class="lbl">1px border, radius 4px</label>
              </div>
              <div class="thick">
                <label class="lbl">2px border, radius 6px</label>
              </div>
              <div class="copper">
                <label class="lbl">copper border, radius 5px</label>
              </div>
              <div class="danger">
                <label class="lbl">danger border, radius 4px</label>
              </div>
              <div class="good">
                <label class="lbl">good border, radius 3px</label>
              </div>
            </col>""";
        String css =
                "col{gap:4px}"
              + " .intro{font-size:6px;color:#7A6A55}"
              + " div{padding:3px 5px}"
              + " .lbl{font-size:6px;color:#C2AD8E}"
              + " .bg-only{background:#362A20;border-radius:4px}"
              + " .thin{background:#1F1812;border:1px solid #3A2E22;border-radius:4px}"
              + " .thick{background:#1F1812;border:2px solid #5A3A1C;border-radius:6px}"
              + " .copper{background:#1A1008;border:1px solid #D89255;border-radius:5px}"
              + " .danger{background:#1A0A08;border:1px solid #C9533D;border-radius:4px}"
              + " .good{background:#0D1A08;border:1px solid #8FB96B;border-radius:3px}";
        return secPanel(w, h, "radius + border combos", html, css);
    }

    /** Card grid — rounded cards with a coloured top accent bar + label. */
    private TesseraPanel buildCardGrid(int w, int h) {
        String html = """
            <col>
              <row>
                <div class="card copper-card">
                  <label class="cap">copper</label>
                  <label class="sub">#D89255 · r=5</label>
                </div>
                <div class="card danger-card">
                  <label class="cap">danger</label>
                  <label class="sub">#C9533D · r=5</label>
                </div>
                <div class="card good-card">
                  <label class="cap">good</label>
                  <label class="sub">#8FB96B · r=5</label>
                </div>
                <div class="card warn-card">
                  <label class="cap">warn</label>
                  <label class="sub">#E0A84A · r=5</label>
                </div>
                <div class="card mute-card">
                  <label class="cap">mute</label>
                  <label class="sub">#7A6A55 · r=5</label>
                </div>
              </row>
              <p class="note">
                Cards above use border-radius:5px + per-card accent border colour.
              </p>
            </col>""";
        String css =
                "col{gap:5px}"
              + " row{gap:6px;align-items:center;height:52px}"
              + " .card{gap:3px;align-items:center;padding:4px;"
              +         "background:#1F1812;border:1px solid;border-radius:5px;width:64px}"
              + " .cap{font-size:6px;color:#F3E7D3}"
              + " .sub{font-size:5px;color:#7A6A55}"
              + " .copper-card{border-color:#D89255}"
              + " .danger-card{border-color:#C9533D}"
              + " .good-card{border-color:#8FB96B}"
              + " .warn-card{border-color:#E0A84A}"
              + " .mute-card{border-color:#7A6A55}"
              + " .note{font-size:6px;color:#7A6A55}";
        return secPanel(w, h, "border-radius on cards — 5 palette accents", html, css);
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
