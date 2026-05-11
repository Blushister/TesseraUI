package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v1.4 features: inline flow.
 *
 * <p>Demonstrates that {@code <strong>}, {@code <em>}, {@code <span>} and {@code <a>} elements
 * inside {@code <p>}, headings and {@code <li>} render inline on the same line rather than
 * stacking as separate widgets.</p>
 *
 * <p>Open with: {@code /tessera test-v14}</p>
 */
public final class TesseraTestScreenV14 extends TesseraScreen {

    private TesseraPanel root;

    public TesseraTestScreenV14() {
        super(Component.literal("TesseraUI v1.4 — inline flow"));
    }

    @Override
    protected void init() { rebuild(); }

    private void rebuild() {
        int pw = Math.min(width,  444);
        int ph = Math.min(height, 380);
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;

        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(6).gap(4);

        int iW = pw - 12;

        root.add(new TesseraLabel(0, 0, iW, 10,
                "TesseraUI v1.4 — inline flow   (ESC to close)")
                .color(TesseraPalette.COPPER_HI).fontSize(7f));

        root.add(TesseraTemplateRenderer.build(
                TesseraTemplate.load("tesseraui:ui/test_v14"),
                TesseraModel.EMPTY, Map.of(),
                0, 0, iW, ph - 30));

        root.layout();
    }

    // ── TesseraScreen boilerplate ─────────────────────────────────────────────

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
