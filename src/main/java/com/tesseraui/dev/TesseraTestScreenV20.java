package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Developer test screen for TesseraUI v2.0 features:
 * <ul>
 *   <li>{@link TesseraVirtualList} — 200 items, lazy rendering, scroll wheel support</li>
 *   <li>CSS Transitions — {@code background}, {@code opacity}, {@code border-color},
 *       {@code color} animated at hover</li>
 *   <li>Row-height slider (8–24 px) that rebuilds the list on change</li>
 * </ul>
 *
 * <p>Open programmatically or extend command wiring in a separate module.</p>
 */
public final class TesseraTestScreenV20 extends TesseraScreen {

    private TesseraPanel root;
    private TesseraVirtualList vlist;

    /** Current row height for the virtual list (controlled by the slider). */
    private int rowHeight = 16;

    public TesseraTestScreenV20() {
        super(Component.literal("TesseraUI v2.0 — Virtual List & Transitions"));
    }

    @Override
    protected void init() {
        rebuild();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void rebuild() {
        int pw = Math.min(width,  360);
        int ph = Math.min(height, 240);
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;

        // Build the item list: 200 items with alternating row colors
        List<TesseraModel> items = buildItems(200);

        final int capturedRowH = this.rowHeight;
        final int listW = 130;

        vlist = TesseraVirtualList.of(items, capturedRowH, model -> {
            String name = model.resolve("name");
            int idx2 = 0;
            try { idx2 = Integer.parseInt(name.substring(5)) - 1; } catch (Exception ignored) {}
            int rowBg = (idx2 % 2 == 0) ? 0xFF1e293b : 0xFF0f172a;
            TesseraPanel row = TesseraPanel.row(0, 0, listW, capturedRowH)
                    .background(rowBg)
                    .padding(0, 4, 0, 4);
            TesseraLabel lbl = new TesseraLabel(0, 0, listW - 8, capturedRowH,
                    name != null ? name : "");
            lbl.color(0xFFe2e8f0).fontSize(6f);
            row.add(lbl);
            row.layout();
            return row;
        });
        vlist.setSize(listW, 120);
        vlist.background(0xFF0f172a);

        // Model: exposes rowHeight for the template and item count
        Map<String, String> modelData = new HashMap<>();
        modelData.put("rowHeight", String.valueOf(rowHeight));
        modelData.put("items", "200");
        for (int i = 0; i < 200; i++) {
            modelData.put("item.name." + i, "item_" + (i + 1));
        }
        TesseraModel model = TesseraModel.of(modelData);

        // Root panel
        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(6).gap(4);

        root.add(new TesseraLabel(0, 0, pw - 12, 9,
                "TesseraUI v2.0  —  Virtual List & Transitions  (ESC)")
                .color(TesseraPalette.COPPER_HI).fontSize(7f));

        int innerW = pw - 12;

        // Body row
        TesseraPanel body = TesseraPanel.row(0, 0, innerW, ph - 22).gap(6);

        // ── Left: virtual list ─────────────────────────────────────────────
        TesseraPanel listSection = TesseraPanel.column(0, 0, 142, ph - 22)
                .background(0xFF1e293b)
                .border(1, 0xFF334155)
                .padding(4).gap(3);

        listSection.add(new TesseraLabel(0, 0, 130, 8, "Virtual List (200 items)")
                .color(0xFF94a3b8).fontSize(6f).fontWeight(700));
        listSection.add(new TesseraLabel(0, 0, 130, 7, "Scroll avec la molette")
                .color(0xFF64748b).fontSize(5f));
        listSection.add(vlist);

        // Slider row
        TesseraPanel sliderRow = TesseraPanel.row(0, 0, 130, 12).gap(4);
        sliderRow.add(new TesseraLabel(0, 0, 38, 10, "rowHeight:").color(0xFF94a3b8).fontSize(5f));
        TesseraSlider slider = new TesseraSlider(0, 0, 56, 8, 8f, 24f, rowHeight);
        slider.onInput(val -> {
            try {
                int v = Math.round(Float.parseFloat(val));
                this.rowHeight = Math.max(8, Math.min(24, v));
                rebuild();
            } catch (NumberFormatException ignored) {}
        });
        sliderRow.add(slider);
        sliderRow.add(new TesseraLabel(0, 0, 24, 10, rowHeight + "px")
                .color(TesseraPalette.COPPER_LO).fontSize(5f));
        sliderRow.layout();
        listSection.add(sliderRow);
        listSection.layout();
        body.add(listSection);

        // ── Right: transition demos via template ───────────────────────────
        int rightW = innerW - 148;
        TesseraPanel transSection = TesseraTemplateRenderer.build(
                TesseraTemplate.load("tesseraui:ui/test_v20"),
                model, Map.of(), Map.of(),
                0, 0, rightW, ph - 22);
        body.add(transSection, 1f, 0f, TesseraStyle.UNSET, 0, 0, null, false, 0, 0, 0, 0);

        body.layout();
        root.add(body);
        root.layout();
    }

    private static List<TesseraModel> buildItems(int count) {
        List<TesseraModel> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final int idx = i;
            list.add(key -> switch (key) {
                case "name"  -> "item_" + (idx + 1);
                default      -> null;
            });
        }
        return list;
    }

    // ── TesseraScreen boilerplate ─────────────────────────────────────────────

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
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        if (vlist != null && vlist.mouseScrolled(mx, my, vScroll)) return true;
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (root != null && root.keyPressed(key, scan, mods)) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
