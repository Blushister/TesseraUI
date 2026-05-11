package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v2.2 features:
 * drag-and-drop panels, {@link TesseraItemSlot}, {@link TesseraItemGrid},
 * and declarative {@code <item-slot>} HTML elements.
 *
 * <p>Open with: {@code /tessera test-v22}</p>
 */
public final class TesseraTestScreenV22 extends TesseraScreen {

    private TesseraPanel root;

    // Drop zone states
    private String dropZone1Label = "Drop here (A)";
    private int    dropZone1Bg    = 0xFF1e3a5f;
    private String dropZone2Label = "Drop here (B)";
    private int    dropZone2Bg    = 0xFF14532d;

    public TesseraTestScreenV22() {
        super(Component.literal("TesseraUI v2.2 — Drag & Drop + Item Slots"));
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        int pw = Math.min(width,  480);
        int ph = Math.min(height, 320);
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;

        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(6).gap(6);

        int iW = pw - 14;

        // ── Title ─────────────────────────────────────────────────────────────
        root.add(new TesseraLabel(0, 0, iW, 10,
                "TesseraUI v2.2 — Drag & Drop + Item Slots   (ESC pour fermer)")
                .color(TesseraPalette.COPPER_HI).fontSize(7f));

        // ── Section 1 : TesseraItemGrid ───────────────────────────────────────
        root.add(new TesseraLabel(0, 0, iW, 9, "[ TesseraItemGrid — grille 4×2, drag entre slots ]")
                .color(TesseraPalette.COPPER).fontSize(6f));

        TesseraItemGrid grid = new TesseraItemGrid(0, 0, 4, 2, 22);
        grid.setItem(0, new ItemStack(Items.DIAMOND));
        grid.setItem(1, new ItemStack(Items.GOLD_INGOT, 3));
        grid.setItem(2, new ItemStack(Items.IRON_INGOT, 8));
        grid.setItem(3, new ItemStack(Items.COAL, 12));
        grid.setItem(4, new ItemStack(Items.EMERALD));
        grid.setItem(5, new ItemStack(Items.LAPIS_LAZULI, 5));
        // slots 6, 7 intentionally empty
        root.add(grid);

        // ── Section 2 : individual TesseraItemSlot ────────────────────────────
        root.add(new TesseraLabel(0, 0, iW, 9, "[ TesseraItemSlot — slots individuels 32px ]")
                .color(TesseraPalette.COPPER).fontSize(6f));

        TesseraPanel slotRow = TesseraPanel.row(0, 0, iW, 34).gap(3);
        ItemStack[] items = {
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.BOW),
            new ItemStack(Items.BREAD, 16),
            new ItemStack(Items.GOLDEN_APPLE),
            new ItemStack(Items.ARROW, 64),
        };
        for (ItemStack stack : items) {
            slotRow.add(new TesseraItemSlot(0, 0, 32).item(stack));
        }
        root.add(slotRow);

        // ── Section 3 : Drag & Drop panels ────────────────────────────────────
        root.add(new TesseraLabel(0, 0, iW, 9, "[ Drag & Drop — glisse un panneau coloré dans une zone ]")
                .color(TesseraPalette.COPPER).fontSize(6f));

        // Draggable panels row
        TesseraPanel dragRow = TesseraPanel.row(0, 0, iW, 28).gap(6);

        TesseraPanel redPanel = TesseraPanel.column(0, 0, 52, 24)
                .background(0xFF7f1d1d).border(1, 0xFFef4444)
                .draggable(true).dragPayload("rouge");
        redPanel.add(new TesseraLabel(0, 0, 44, 9, "Rouge").color(0xFFfecaca).fontSize(6f));

        TesseraPanel bluePanel = TesseraPanel.column(0, 0, 52, 24)
                .background(0xFF1e3a5f).border(1, 0xFF3b82f6)
                .draggable(true).dragPayload("bleu");
        bluePanel.add(new TesseraLabel(0, 0, 44, 9, "Bleu").color(0xFFbfdbfe).fontSize(6f));

        TesseraPanel greenPanel = TesseraPanel.column(0, 0, 52, 24)
                .background(0xFF14532d).border(1, 0xFF22c55e)
                .draggable(true).dragPayload("vert");
        greenPanel.add(new TesseraLabel(0, 0, 44, 9, "Vert").color(0xFFbbf7d0).fontSize(6f));

        dragRow.add(redPanel).add(bluePanel).add(greenPanel);
        root.add(dragRow);

        // Drop zones row
        TesseraPanel dropRow = TesseraPanel.row(0, 0, iW, 32).gap(8);

        final TesseraLabel dz1Label = new TesseraLabel(0, 0, 80, 9, dropZone1Label)
                .color(TesseraPalette.CREAM_DIM).fontSize(6f);
        TesseraPanel dropZone1 = TesseraPanel.column(0, 0, 90, 28)
                .background(dropZone1Bg).border(1, 0xFF3b82f6).padding(4);
        dropZone1.add(dz1Label);
        dropZone1.dropZone(new TesseraDropZone() {
            @Override
            public boolean accepts(Object payload) { return payload instanceof String; }

            @Override
            public void onDrop(Object payload) {
                dropZone1Label = "Recu : " + payload;
                dropZone1Bg    = 0xFF3730a3;
                rebuild();
            }

            @Override
            public Rect dropBounds() { return dropZone1.bounds(); }
        });

        final TesseraLabel dz2Label = new TesseraLabel(0, 0, 80, 9, dropZone2Label)
                .color(TesseraPalette.CREAM_DIM).fontSize(6f);
        TesseraPanel dropZone2 = TesseraPanel.column(0, 0, 90, 28)
                .background(dropZone2Bg).border(1, 0xFF22c55e).padding(4);
        dropZone2.add(dz2Label);
        dropZone2.dropZone(new TesseraDropZone() {
            @Override
            public boolean accepts(Object payload) { return payload instanceof String; }

            @Override
            public void onDrop(Object payload) {
                dropZone2Label = "Recu : " + payload;
                dropZone2Bg    = 0xFF065f46;
                rebuild();
            }

            @Override
            public Rect dropBounds() { return dropZone2.bounds(); }
        });

        dropRow.add(dropZone1).add(dropZone2);
        root.add(dropRow);

        // ── Section 4 : declarative HTML item-slots ───────────────────────────
        root.add(new TesseraLabel(0, 0, iW, 9, "[ <item-slot> HTML — attribut item= ]")
                .color(TesseraPalette.COPPER).fontSize(6f));

        root.add(TesseraTemplateRenderer.build(
                TesseraTemplate.load("tesseraui:ui/test_v22"),
                TesseraModel.EMPTY, Map.of(),
                0, 0, iW, 50));

        root.layout();
    }

    // ── TesseraScreen ─────────────────────────────────────────────────────────

    @Override
    protected TesseraPanel tesseraRoot() { return root; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        if (root != null) root.render(g, mx, my);
        // Drag ghost rendered on top of everything else
        TesseraDragContext.render(g);
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
