package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v1.2 features:
 * {@code <textarea>} multi-line input and {@code position: absolute} overlays.
 *
 * <ul>
 *   <li><b>Left column</b> — {@code <textarea>} with placeholder, focus ring, scrollbar</li>
 *   <li><b>Right column</b> — {@code position: absolute} overlay badges on top of cards</li>
 * </ul>
 *
 * <p>Open with: {@code /tessera test-v12}</p>
 */
public final class TesseraTestScreenV12 extends TesseraScreen {

    private TesseraPanel root;

    public TesseraTestScreenV12() {
        super(Component.literal("TesseraUI v1.2 — textarea + position:absolute"));
    }

    @Override
    protected void init() { rebuild(); }

    private void rebuild() {
        int pw = Math.min(width,  444);
        int ph = Math.min(height, 310);
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;
        int iW = pw - 12;
        int cW = (iW - 4) / 2;

        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(6).gap(4);

        root.add(new TesseraLabel(0, 0, iW, 10,
                "TesseraUI v1.2 — textarea + position:absolute   (ESC to close)")
                .color(TesseraPalette.COPPER_HI).fontSize(7f));

        // ── Two-column row ─────────────────────────────────────────────────
        TesseraPanel row = TesseraPanel.row(0, 0, iW, 272).gap(4).alignItems("flex-start");
        row.add(buildTextareaSection(cW,      272));
        row.add(buildAbsoluteSection(iW - cW - 4, 272));
        root.add(row);

        root.layout();
    }

    // ── Left: textarea ─────────────────────────────────────────────────────

    private TesseraPanel buildTextareaSection(int w, int h) {
        String html = """
            <col>
              <p class="intro">&lt;textarea&gt; — multi-line input</p>
              <textarea id="ta1" rows="4" maxlength="512"
                        placeholder="Click to type here..."
                        class="ta">
              </textarea>
              <p class="hint">Enter=newline  ↑↓=lines  Shift+←=select  Ctrl+A/C/X/V</p>
              <p class="intro">disabled / muted style</p>
              <textarea id="ta2" rows="3" class="ta-disabled"
                        placeholder="Read-only style (dark bg, muted border)">
              </textarea>
              <p class="intro">custom accent colour</p>
              <textarea id="ta3" rows="3" class="ta-gold"
                        placeholder="Gold focus border">
              </textarea>
            </col>""";
        String css =
                "col{gap:4px}"
              + " .intro{font-size:6px;color:#7A6A55}"
              + " .hint{font-size:5px;color:#5A4A35}"
              + " textarea{font-size:7px;color:#F3E7D3;background:#17120D;"
              +            "border-color:#A0642C;padding:3px}"
              + " textarea:focus{border-color:#F0B27A}"
              + " .ta{border-color:#A0642C}"
              + " .ta-disabled{color:#5A4A35;background:#12100B;border-color:#3A2E22}"
              + " .ta-gold{border-color:#E0A84A}"
              + " .ta-gold:focus{border-color:#F5C96A}";
        return secPanel(w, h, "textarea", html, css);
    }

    // ── Right: position:absolute ───────────────────────────────────────────

    private TesseraPanel buildAbsoluteSection(int w, int h) {
        String html = """
            <col>
              <p class="intro">4 overlay patterns</p>
              <div class="card-wrap">
                <div class="card copper-card">
                  <label class="cap">copper card</label>
                  <label class="sub">badge top-right</label>
                </div>
                <div class="badge-tr">NEW</div>
              </div>
              <div class="card-wrap">
                <div class="card danger-card">
                  <label class="cap">danger card</label>
                  <label class="sub">dot mid-right</label>
                </div>
                <div class="dot-mr"></div>
              </div>
              <div class="card-wrap">
                <div class="card good-card">
                  <label class="cap">good card</label>
                  <label class="sub">pip bottom-right</label>
                </div>
                <div class="pip"></div>
              </div>
              <div class="card-wrap">
                <div class="card warn-card">
                  <label class="cap">warn card</label>
                  <label class="sub">chip above card</label>
                </div>
                <div class="chip">v1.2</div>
              </div>
              <p class="note">Overlays float outside the flex flow via position:absolute.</p>
            </col>""";
        String css =
                "col{gap:5px}"
              + " .intro{font-size:6px;color:#7A6A55}"
              + " .note{font-size:5px;color:#5A4A35}"
              + " .card-wrap{position:relative;width:100%;height:36px}"
              + " .card{width:100%;height:36px;gap:2px;align-items:center;padding:4px;"
              +         "background:#1F1812;border:1px solid;border-radius:4px}"
              + " .cap{font-size:6px;color:#F3E7D3}"
              + " .sub{font-size:5px;color:#7A6A55}"
              + " .copper-card{border-color:#D89255}"
              + " .danger-card{border-color:#C9533D}"
              + " .good-card{border-color:#8FB96B}"
              + " .warn-card{border-color:#E0A84A}"
              // badge-tr: top-right, 4 px inset so it clears the 1 px card border
              + " .badge-tr{position:absolute;top:4px;right:4px;width:14px;height:9px;"
              +             "background:#D89255;border-radius:3px;font-size:5px;color:#1A0C04}"
              // dot-mr: vertically-centred on the right edge — right side never conflicts with left-aligned text
              + " .dot-mr{position:absolute;top:13px;right:4px;width:10px;height:10px;"
              +           "background:#C9533D;border-radius:5px}"
              // pip: solid 7×7 colored square anchored bottom-right (no text → no width jitter)
              + " .pip{position:absolute;bottom:5px;right:5px;width:7px;height:7px;"
              +        "background:#8FB96B;border-radius:4px}"
              // chip: floats 4 px ABOVE the card (into the 5 px gap between card-wraps)
              + " .chip{position:absolute;top:-4px;left:5px;width:20px;height:8px;"
              +         "background:#1A1208;border:1px solid #E0A84A;border-radius:2px;"
              +         "font-size:5px;color:#E0A84A}";
        return secPanel(w, h, "position:absolute", html, css);
    }

    // ── helpers ───────────────────────────────────────────────────────────

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

    // ── TesseraScreen boilerplate ──────────────────────────────────────────

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
