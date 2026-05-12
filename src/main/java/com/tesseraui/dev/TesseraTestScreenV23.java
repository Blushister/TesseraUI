package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v2.3 — CSS Animations.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>CSS {@code transition} on hover (background + border-color, ease-out 250ms)</li>
 *   <li>{@code @keyframes pulse} — infinite ease-in-out loop</li>
 *   <li>{@code @keyframes flash} — infinite alternate loop</li>
 *   <li>{@code @keyframes fade-in} — single-shot on open</li>
 * </ul>
 *
 * <p>Open with: {@code /tessera test-v23}</p>
 */
public final class TesseraTestScreenV23 extends TesseraScreen {

    private TesseraPanel root;

    public TesseraTestScreenV23() {
        super(Component.literal("TesseraUI v2.3 — CSS Animations"));
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        int pw = Math.min(width, 480);
        int ph = Math.min(height, 300);
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;

        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(6).gap(6);

        int iW = pw - 14;

        root.add(new TesseraLabel(0, 0, iW, 10,
                "TesseraUI v2.3 — CSS Animations   (ESC pour fermer)")
                .color(TesseraPalette.COPPER_HI).fontSize(7f));

        TesseraPanel htmlSection = TesseraTemplateRenderer.build(
                TesseraTemplate.load("tesseraui:ui/test_v23"),
                TesseraModel.EMPTY, Map.of(),
                0, 0, iW, 200);
        int htmlH = htmlSection.fitContentHeight();
        if (htmlH > 0) { htmlSection.setSize(iW, htmlH); htmlSection.layout(); }
        root.add(htmlSection);

        root.layout();
    }

    @Override
    protected TesseraPanel tesseraRoot() { return root; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        if (root != null) root.render(g, mx, my);
        renderTesseraOverlays(g, mx, my);
        super.render(g, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (root != null && root.mouseClicked(mx, my, btn)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
