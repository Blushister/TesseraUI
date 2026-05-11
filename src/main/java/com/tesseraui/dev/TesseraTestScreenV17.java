package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v1.7 feature: {@code @media} queries.
 *
 * <p>Demonstrates that CSS {@code @media (max-width: Xpx)} and
 * {@code @media (min-width: Xpx)} blocks are parsed and applied dynamically
 * based on the current GUI-scaled viewport width.  The demo shows:</p>
 * <ul>
 *   <li>A toolbar whose items are shown/hidden depending on viewport width.</li>
 *   <li>A wide-only block (visible only when viewport &ge; 400px).</li>
 *   <li>A narrow-only block (visible only when viewport &le; 360px).</li>
 * </ul>
 *
 * <p>The layout rebuilds automatically whenever {@code guiScaledWidth()} changes
 * (resize window or change GUI Scale in options).</p>
 *
 * <p>Open with: {@code /tessera test-v17}</p>
 */
public final class TesseraTestScreenV17 extends TesseraScreen {

    private TesseraPanel root;
    private int lastGuiWidth = -1;

    public TesseraTestScreenV17() {
        super(Component.literal("TesseraUI v1.7 — @media"));
    }

    @Override
    protected void init() {
        lastGuiWidth = guiScaledWidth();
        rebuild();
    }

    private void rebuild() {
        int vw = guiScaledWidth();
        int pw = Math.min(width,  420);
        int ph = Math.min(height, 260);
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;

        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(6).gap(4);

        int iW = pw - 12;

        // Title
        root.add(new TesseraLabel(0, 0, iW, 10,
                "TesseraUI v1.7 — @media   (ESC pour fermer)")
                .color(TesseraPalette.COPPER_HI).fontSize(7f));

        // Viewport indicator
        String breakpointLabel;
        if (vw <= 520)       breakpointLabel = "≤ 520px  →  layout réduit";
        else                 breakpointLabel = "≥ 521px  →  layout large";

        root.add(new TesseraLabel(0, 0, iW, 9,
                "viewport: " + vw + "px   [ " + breakpointLabel + " ]")
                .color(0xFF64748b).fontSize(6f));

        // Template
        root.add(TesseraTemplateRenderer.build(
                TesseraTemplate.load("tesseraui:ui/test_v17"),
                TesseraModel.EMPTY, Map.of(),
                0, 0, iW, ph - 42));

        root.layout();
    }

    private static int guiScaledWidth() {
        try { return Minecraft.getInstance().getWindow().getGuiScaledWidth(); }
        catch (Exception e) { return Integer.MAX_VALUE; }
    }

    // ── TesseraScreen boilerplate ─────────────────────────────────────────────

    @Override protected TesseraPanel tesseraRoot() { return root; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        // Rebuild dynamically if GUI scale / window size changed
        int vw = guiScaledWidth();
        if (vw != lastGuiWidth) {
            lastGuiWidth = vw;
            rebuild();
        }

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
