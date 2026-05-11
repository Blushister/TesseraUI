package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v1.3 features:
 * {@code calc()} dynamic widths and full flexbox ({@code flex-grow}, {@code flex-shrink},
 * {@code flex-basis}, {@code order}).
 *
 * <ul>
 *   <li><b>Left column</b>  — {@code calc()} on widths and heights</li>
 *   <li><b>Right column</b> — flex-grow proportions, flex-shrink overflow, order reordering</li>
 * </ul>
 *
 * <p>Open with: {@code /tessera test-v13}</p>
 */
public final class TesseraTestScreenV13 extends TesseraScreen {

    private TesseraPanel root;

    public TesseraTestScreenV13() {
        super(Component.literal("TesseraUI v1.3 — calc() + flexbox"));
    }

    @Override
    protected void init() { rebuild(); }

    private void rebuild() {
        int pw = Math.min(width,  444);
        int ph = Math.min(height, 320);
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;
        int iW = pw - 12;
        int cW = (iW - 4) / 2;

        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(6).gap(4);

        root.add(new TesseraLabel(0, 0, iW, 10,
                "TesseraUI v1.3 — calc() + flex-grow/shrink/basis/order   (ESC to close)")
                .color(TesseraPalette.COPPER_HI).fontSize(7f));

        TesseraPanel row = TesseraPanel.row(0, 0, iW, 290).gap(4).alignItems("flex-start");
        row.add(buildCalcSection(cW,      290));
        row.add(buildFlexSection(iW - cW - 4, 290));
        root.add(row);

        root.layout();
    }

    // ── Left: calc() ──────────────────────────────────────────────────────────

    private TesseraPanel buildCalcSection(int w, int h) {
        String html = """
            <col>
              <p class="intro">calc() — dynamic widths</p>

              <p class="hint">calc(100% - 20px)</p>
              <div class="bar full-minus20"></div>

              <p class="hint">calc(75% - 10px)</p>
              <div class="bar three-q"></div>

              <p class="hint">calc(50% - 10px)</p>
              <div class="bar half"></div>

              <p class="hint">calc(25% - 10px)</p>
              <div class="bar quarter"></div>

              <p class="intro" style="margin-top:6px">calc() heights</p>

              <p class="hint">height: calc(50% - 20px)</p>
              <div class="tall-box"></div>

              <p class="note">
                calc() resolves % against parent available size at layout time.
              </p>
            </col>""";
        String css =
                "col{gap:5px}"
              + " .intro{font-size:6px;color:#7A6A55}"
              + " .hint{font-size:5px;color:#D89255}"
              + " .note{font-size:5px;color:#5A4A35;white-space:normal}"
              + " .bar{height:8px;border-radius:3px}"
              + " .full-minus20{width:calc(100% - 20px);background:#D89255}"
              + " .three-q{width:calc(75% - 10px);background:#C9533D}"
              + " .half{width:calc(50% - 10px);background:#8FB96B}"
              + " .quarter{width:calc(25% - 10px);background:#4A8FC9}"
              + " .tall-box{width:calc(100% - 20px);height:calc(50% - 20px);"
              +             "background:#2A1E12;border:1px solid #D89255;border-radius:4px}";
        return secPanel(w, h, "calc()", html, css);
    }

    // ── Right: flexbox ────────────────────────────────────────────────────────

    private TesseraPanel buildFlexSection(int w, int h) {
        String html = """
            <col>
              <p class="intro">flex-grow — proportional distribution</p>
              <row class="flex-row">
                <div class="fitem grow1">1</div>
                <div class="fitem grow2">2</div>
                <div class="fitem grow1">1</div>
              </row>

              <p class="intro">flex-shrink — overflow reduction</p>
              <row class="shrink-row">
                <div class="sitem shrink1">no shrink</div>
                <div class="sitem shrink2">shrinks</div>
              </row>

              <p class="intro">flex-basis — initial size</p>
              <row class="basis-row">
                <div class="bitem basis80">basis 80</div>
                <div class="bitem basis-grow">grow</div>
              </row>

              <p class="intro">order — reorder without DOM change</p>
              <row class="order-row">
                <div class="oitem order3">DOM 1 / order 3</div>
                <div class="oitem order1">DOM 2 / order 1</div>
                <div class="oitem order2">DOM 3 / order 2</div>
              </row>

              <p class="note">
                DOM 1 has order:3, DOM 2 has order:1 — it renders first.
              </p>
            </col>""";
        String css =
                "col{gap:6px}"
              + " .intro{font-size:6px;color:#7A6A55}"
              + " .note{font-size:5px;color:#5A4A35;white-space:normal}"
              // flex-grow demo: 1 / 2 / 1 weights
              + " row.flex-row{width:100%;height:14px;gap:2px;align-items:stretch}"
              + " .fitem{height:14px;border-radius:3px;font-size:5px;color:#F3E7D3;text-align:center;line-height:14px}"
              + " .grow1{flex-grow:1;background:#3A2E22}"
              + " .grow2{flex-grow:2;background:#5A3A1C}"
              // flex-shrink demo: one item refuses to shrink
              + " row.shrink-row{width:100%;height:14px;gap:2px}"
              + " .sitem{height:14px;border-radius:3px;font-size:5px;color:#F3E7D3;text-align:center;line-height:14px}"
              + " .shrink1{flex-basis:90px;flex-shrink:0;background:#C9533D}"  // holds its 90px
              + " .shrink2{flex-basis:90px;flex-shrink:1;background:#3A2E22}"  // absorbs overflow
              // flex-basis demo
              + " row.basis-row{width:100%;height:14px;gap:2px}"
              + " .bitem{height:14px;border-radius:3px;font-size:5px;color:#F3E7D3;text-align:center;line-height:14px}"
              + " .basis80{flex-basis:80px;flex-grow:0;flex-shrink:0;background:#A0642C}"
              + " .basis-grow{flex-basis:0;flex-grow:1;background:#D89255}"
              // order demo
              + " row.order-row{width:100%;height:14px;gap:2px}"
              + " .oitem{height:14px;flex-grow:1;border-radius:3px;font-size:5px;color:#F3E7D3;text-align:center;line-height:14px}"
              + " .order1{order:1;background:#8FB96B}"
              + " .order2{order:2;background:#4A8FC9}"
              + " .order3{order:3;background:#C9533D}";
        return secPanel(w, h, "flexbox", html, css);
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

    // ── TesseraScreen boilerplate ──────────────────────────────────────────────

    @Override protected TesseraPanel tesseraRoot() { return root; }

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
    public boolean keyPressed(int key, int scan, int mods) {
        if (root != null && root.keyPressed(key, scan, mods)) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
