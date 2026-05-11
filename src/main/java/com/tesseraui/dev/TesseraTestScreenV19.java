package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v1.9 features:
 * {@link TesseraTabPanel}, {@link TesseraModal}, and {@link TesseraFocusManager}.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>A {@code <tabs>} widget with three tabs: "Général" (form + focus), "Avancé" (sliders),
 *       "À propos" (text).</li>
 *   <li>A "Ouvrir modal" button that opens a {@link TesseraModal.ModalBuilder} confirm dialog.</li>
 *   <li>Tab-key navigation between inputs in the first tab.</li>
 * </ul>
 *
 * <p>Open with: {@code /tessera test-v19}</p>
 */
public final class TesseraTestScreenV19 extends TesseraScreen {

    private TesseraPanel root;

    public TesseraTestScreenV19() {
        super(Component.literal("TesseraUI v1.9 — Tabs · Modal · Focus"));
    }

    @Override
    protected void init() {
        beforeInit(); // clears TesseraFocusManager
        rebuild();
    }

    private void rebuild() {
        int pw = Math.min(width,  440);
        int ph = Math.min(height, 280);
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;

        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(6).gap(4);

        int iW = pw - 12;

        // Header
        root.add(new TesseraLabel(0, 0, iW, 10,
                "TesseraUI v1.9 — Tabs · Modal · Focus   (ESC pour fermer)")
                .color(TesseraPalette.COPPER_HI).fontSize(7f));

        // Build template (tabs + modal button)
        TesseraPanel content = TesseraTemplateRenderer.build(
                TesseraTemplate.load("tesseraui:ui/test_v19"),
                TesseraModel.EMPTY,
                Map.of("openModal", this::openConfirmModal),
                0, 0, iW, ph - 22);

        root.add(content);
        root.layout();
    }

    private void openConfirmModal() {
        TesseraModal.builder()
                .title("Confirmer l'action")
                .onConfirm(() -> {
                    TesseraModal.close();
                    // Could trigger feedback here
                })
                .onCancel(TesseraModal::close)
                .confirmLabel("Confirmer")
                .cancelLabel("Annuler")
                .show();
    }

    // ── TesseraScreen boilerplate ─────────────────────────────────────────────

    @Override
    protected TesseraPanel tesseraRoot() { return root; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        if (root != null) root.render(g, mx, my);
        // Modal overlay rendered on top of everything
        TesseraModal.render(g, width, height, mx, my);
        super.render(g, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (handleModalEvents(mx, my, btn)) return true;
        if (root != null && root.mouseClicked(mx, my, btn)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // super handles Escape (→ closes modal), Tab (→ focus nav), and focused-widget key events
        if (super.keyPressed(key, scan, mods)) return true;
        if (root != null && root.keyPressed(key, scan, mods)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        TesseraWidget focused = TesseraFocusManager.focused();
        if (focused != null && focused.charTyped(c, mods)) return true;
        if (root != null && root.charTyped(c, mods)) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
