package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v2.1 features:
 * <ul>
 *   <li>{@link TesseraContextMenu} — right-click context menus on panels</li>
 *   <li>{@link TesseraComponentRegistry} — reusable {@code <template>} / {@code <slot>} components</li>
 *   <li>{@link TesseraDebugOverlay} — press {@code I} or {@code [D]} button to toggle</li>
 * </ul>
 *
 * <p>The HTML template ({@code test_v21.html}) declares a {@code <card>} component via
 * {@code <template name="card">} and then uses it three times with different slot content.
 * The card panels are wired with right-click context menus directly in Java.</p>
 *
 * <p>Open with: {@code /tessera test-v21}</p>
 */
public final class TesseraTestScreenV21 extends TesseraScreen {

    private TesseraPanel root;
    private TesseraLabel actionLog;

    public TesseraTestScreenV21() {
        super(Component.literal("TesseraUI v2.1 — ContextMenu + Components + Debug"));
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        int pw = Math.min(width,  520);
        int ph = Math.min(height, 360);
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;
        int iW = pw - 12;

        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(6).gap(5);

        // ── Toolbar ──────────────────────────────────────────────────────────
        TesseraPanel toolbar = TesseraPanel.row(0, 0, iW, 18)
                .background(TesseraPalette.BG1)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(3, 5, 3, 5).gap(5);

        toolbar.add(new TesseraLabel(0, 0, iW - 80, 10,
                "TesseraUI v2.1 — ContextMenu + Components + Debug")
                .color(TesseraPalette.COPPER_HI).fontSize(7f), 1);

        TesseraButton dbgBtn = new TesseraButton(0, 0, 60, 12)
                .label("[D] Debug")
                .bgColor(TesseraPalette.BG2).labelColor(TesseraPalette.CREAM)
                .fontSize(6f).onClick(TesseraDebugOverlay::toggle);
        toolbar.add(dbgBtn);
        root.add(toolbar);

        // ── Cards row (3 cards with context menus) ────────────────────────────
        TesseraPanel cardsRow = TesseraPanel.row(0, 0, iW, 90)
                .gap(5);

        String[][] cardData = {
            { "Carte Alpha",  "Contenu Alpha.",  "Clic droit pour options." },
            { "Carte Beta",   "Contenu Beta.",   "Clic droit pour options." },
            { "Carte Gamma",  "Contenu Gamma.",  "Clic droit pour options." },
        };

        for (String[] data : cardData) {
            String title   = data[0];
            String body    = data[1];
            String hint    = data[2];

            TesseraPanel card = TesseraPanel.column(0, 0, 0, 90)
                    .background(TesseraPalette.BG1)
                    .border(1, TesseraPalette.LINE)
                    .padding(5).gap(3)
                    .hoverBackground(TesseraPalette.BG2)
                    .hoverBorder(TesseraPalette.COPPER_LO)
                    .transition(150, "all");

            card.add(new TesseraLabel(0, 0, 100, 10, title)
                    .color(TesseraPalette.COPPER_HI).fontSize(7f).fontWeight(700));
            card.add(new TesseraLabel(0, 0, 100, 9, body)
                    .color(TesseraPalette.CREAM_DIM).fontSize(6f));
            card.add(new TesseraLabel(0, 0, 100, 9, hint)
                    .color(TesseraPalette.TEXT_MUTE).fontSize(6f));

            // Context menu via onRightClick — mouse position resolved via mouseHandler at call time
            card.onRightClick(() -> showCardMenu(title));

            cardsRow.add(card, 1);
        }
        root.add(cardsRow);

        // ── Component demo: load test_v21.html which registers <card> template ──
        TesseraPanel compDemo = TesseraPanel.column(0, 0, iW, 0)
                .background(TesseraPalette.BG1)
                .border(1, TesseraPalette.LINE)
                .padding(4).gap(3);
        compDemo.add(new TesseraLabel(0, 0, iW - 8, 9,
                "Composants <template> via HTML:")
                .color(TesseraPalette.COPPER).fontSize(6f).fontWeight(700));

        // Load the template file to register the <card> component and get the body
        TesseraPanel htmlDemo = TesseraTemplateRenderer.build(
                TesseraTemplate.load("tesseraui:ui/test_v21"),
                TesseraModel.EMPTY,
                Map.of(
                    "toggleDebug", TesseraDebugOverlay::toggle,
                    "noop",        () -> {}
                ),
                0, 0, iW - 8, 120);
        compDemo.add(htmlDemo);
        int natH = compDemo.fitContentHeight();
        compDemo.setSize(iW, natH);
        compDemo.layout();
        root.add(compDemo);

        // ── Nested widgets section (debug overlay depth demo) ─────────────────
        TesseraPanel nestedDemo = TesseraPanel.column(0, 0, iW, 0)
                .background(TesseraPalette.BG1)
                .border(1, TesseraPalette.LINE)
                .padding(4).gap(3);
        nestedDemo.add(new TesseraLabel(0, 0, iW - 8, 9,
                "Niveaux imbriques (debug overlay):")
                .color(TesseraPalette.COPPER).fontSize(6f).fontWeight(700));

        TesseraPanel l0 = TesseraPanel.column(0, 0, iW - 8, 0)
                .background(0xFF1e293b).border(1, 0xFF334155).padding(3).gap(2);
        TesseraPanel l1row = TesseraPanel.row(0, 0, iW - 22, 0)
                .background(0xFF0f172a).border(1, 0xFF475569).padding(3).gap(3);
        TesseraPanel l2a = TesseraPanel.column(0, 0, 0, 0)
                .background(0xFF1e3a5f).border(1, 0xFF3b82f6).padding(3).gap(2);
        l2a.add(new TesseraLabel(0, 0, 70, 8, "Profondeur 2").color(0xFFe2e8f0).fontSize(6f));
        TesseraPanel l3 = TesseraPanel.column(0, 0, 70, 18)
                .background(0xFF1e4d2f).border(1, 0xFF22c55e).padding(2);
        l3.add(new TesseraLabel(0, 0, 66, 8, "Profondeur 3+").color(0xFFe2e8f0).fontSize(6f));
        l2a.add(l3);
        l2a.setSize(l2a.fitContentWidth(), l2a.fitContentHeight());
        l2a.layout();
        TesseraPanel l2b = TesseraPanel.column(0, 0, 0, 0)
                .background(0xFF3b1e5f).border(1, 0xFFa855f7).padding(3);
        l2b.add(new TesseraLabel(0, 0, 70, 8, "Autre branche").color(0xFFe2e8f0).fontSize(6f));
        l2b.setSize(l2b.fitContentWidth(), l2b.fitContentHeight());
        l2b.layout();
        l1row.add(l2a, 1);
        l1row.add(l2b, 1);
        l1row.setSize(l1row.getWidth(), l1row.fitContentHeight());
        l1row.layout();
        l0.add(l1row);
        l0.setSize(l0.getWidth(), l0.fitContentHeight());
        l0.layout();
        nestedDemo.add(l0);
        nestedDemo.setSize(iW, nestedDemo.fitContentHeight());
        nestedDemo.layout();
        root.add(nestedDemo);

        // ── Action log ────────────────────────────────────────────────────────
        actionLog = new TesseraLabel(0, 0, iW, 8, "Action: —")
                .color(TesseraPalette.TEXT_MUTE).fontSize(6f);
        root.add(actionLog);

        // ── Hint ──────────────────────────────────────────────────────────────
        root.add(new TesseraLabel(0, 0, iW, 8,
                "I = debug overlay  |  clic droit sur une carte = context menu")
                .color(TesseraPalette.TEXT_MUTE).fontSize(6f));

        root.layout();
    }

    private void showCardMenu(String title) {
        int mx = (int)(Minecraft.getInstance().mouseHandler.xpos()
                / Minecraft.getInstance().getWindow().getGuiScale());
        int my = (int)(Minecraft.getInstance().mouseHandler.ypos()
                / Minecraft.getInstance().getWindow().getGuiScale());
        TesseraContextMenu.builder()
            .item("Copier " + title,    () -> logAction("Copier: " + title))
            .item("Supprimer " + title, () -> logAction("Supprimer: " + title))
            .separator()
            .item("Renommer " + title,  () -> logAction("Renommer: " + title))
            .showAt(mx, my);
    }

    private void logAction(String action) {
        if (actionLog != null) actionLog.text("Action: " + action);
    }

    // ── TesseraScreen boilerplate ─────────────────────────────────────────────

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
        // Context menu gets first priority
        if (TesseraContextMenu.mouseClicked(mx, my, btn)) return true;
        if (root != null && root.mouseClicked(mx, my, btn)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
