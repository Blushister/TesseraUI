package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v1.0 semantic HTML features:
 * text wrapping ({@code white-space:normal}), {@code <p>}, {@code <h1>}–{@code <h6>},
 * {@code <ul>}/{@code <ol>}/{@code <li>}, {@code <strong>}, {@code <em>}, {@code <span>},
 * {@code <a>} and semantic container tags.
 *
 * <p>Open with the in-game command: {@code /tessera test-html}</p>
 */
public final class TesseraTestScreenHtml extends TesseraScreen {

    private TesseraPanel root;

    public TesseraTestScreenHtml() {
        super(Component.literal("TesseraUI v1.0 — Semantic HTML Test"));
    }

    @Override
    protected void init() { rebuild(); }

    private void rebuild() {
        int pw  = Math.min(width,  444);
        int ph  = Math.min(height, 296);
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
                "TesseraUI v1.0 — Semantic HTML   (ESC to close)")
                .color(TesseraPalette.COPPER_HI).fontSize(7f));

        // ── Top row (115 px): wrap | headings ────────────────────────────────
        TesseraPanel top = TesseraPanel.row(0, 0, iW, 115).gap(4).alignItems("flex-start");
        top.add(buildWrap    (cW,  115));
        top.add(buildHeadings(cWb, 115));
        root.add(top);

        // ── Bottom row (136 px): lists | inline ──────────────────────────────
        TesseraPanel bot = TesseraPanel.row(0, 0, iW, 136).gap(4).alignItems("flex-start");
        bot.add(buildLists (cW,  136));
        bot.add(buildInline(cWb, 136));
        root.add(bot);

        root.layout();
    }

    // ── sections ─────────────────────────────────────────────────────────────

    /** {@code <p>} wrapping + {@code white-space:normal} on {@code <label>}. */
    private TesseraPanel buildWrap(int w, int h) {
        String html = """
            <col>
              <p>This paragraph is long enough to wrap across multiple lines automatically.</p>
              <label class="wl">A label with white-space:normal also wraps at the container edge.</label>
              <label class="cl">A plain label stays on one line and is clipped if too long.</label>
            </col>""";
        String css = "col{gap:4px}"
                   + " p{font-size:7px;color:#F3E7D3}"
                   + " .wl{font-size:6px;color:#C2AD8E;white-space:normal}"
                   + " .cl{font-size:6px;color:#7A6A55;overflow:hidden}";
        return secPanel(w, h, "p + wrap label", html, css);
    }

    /** {@code <h1>}–{@code <h6>} UA-default sizes + bold. */
    private TesseraPanel buildHeadings(int w, int h) {
        String html = """
            <col>
              <h1>Heading 1</h1>
              <h2>Heading 2</h2>
              <h3>Heading 3</h3>
              <h4>Heading 4</h4>
              <h5>Heading 5</h5>
              <h6>Heading 6</h6>
            </col>""";
        String css = "col{gap:1px} h1,h2,h3,h4,h5,h6{color:#F0B27A}";
        return secPanel(w, h, "h1–h6 headings", html, css);
    }

    /** {@code <ul>}/{@code <ol>} with wrapping {@code <li>} items. */
    private TesseraPanel buildLists(int w, int h) {
        String html = """
            <col>
              <label class="sec">Unordered:</label>
              <ul>
                <li>First item</li>
                <li>Second item</li>
                <li>Third item with a longer label that wraps onto two lines</li>
              </ul>
              <label class="sec">Ordered:</label>
              <ol>
                <li>Alpha</li>
                <li>Beta</li>
                <li>Gamma</li>
              </ol>
            </col>""";
        String css = "col{gap:3px}"
                   + " .sec{font-size:6px;color:#D89255;height:8px}"
                   + " ul,ol{gap:2px;padding-left:4px}"
                   + " li{font-size:6px;color:#F3E7D3}";
        return secPanel(w, h, "ul / ol / li", html, css);
    }

    /** Inline semantic tags, {@code <a>}, {@code <span>} and {@code <section>}. */
    private TesseraPanel buildInline(int w, int h) {
        String html = """
            <col>
              <strong>Bold via &lt;strong&gt;</strong>
              <em>Italic hint via &lt;em&gt;</em>
              <span class="hi">Span with custom color</span>
              <a onclick="openLink">Link button — click me</a>
              <section>
                <p>Paragraph inside a &lt;section&gt; container.</p>
              </section>
            </col>""";
        String css = "col{gap:4px}"
                   + " strong{color:#F3E7D3;font-size:7px}"
                   + " em{font-size:7px}"
                   + " .hi{color:#8FB96B;font-size:7px}"
                   + " a{font-size:7px;color:#F0B27A}"
                   + " section{padding:3px;background:#1F1812} p{font-size:7px;color:#C2AD8E}";
        return secPanel(w, h, "strong / em / span / a / section", html, css);
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
