package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A dropdown (select) widget for TesseraUI.
 *
 * <p>Rendered as a bordered control showing the selected option label plus a ▼/▲ arrow.
 * Clicking the control opens an inline option list drawn at Z+400 so it overlays sibling
 * widgets.  Selecting an option closes the list and fires the {@link #onSelect} handler
 * with the option's value string.</p>
 *
 * <p>Declare in a template with {@code <select>} / {@code <option>}:</p>
 * <pre>{@code
 * <select value="{{ theme }}" oninput="onTheme">
 *   <option value="dark">Dark</option>
 *   <option value="light">Light</option>
 * </select>
 * }</pre>
 *
 * <p>Or build programmatically:</p>
 * <pre>{@code
 * new TesseraDropdown(0, 0, 80, 14)
 *     .addOption("dark",  "Dark")
 *     .addOption("light", "Light")
 *     .select("dark")
 *     .onSelect(v -> System.out.println("selected: " + v));
 * }</pre>
 */
public class TesseraDropdown extends TesseraElement {

    /** A single entry in the dropdown list. */
    public record Option(String value, String label) {}

    private static final int ITEM_H = 12;

    private final List<Option> options = new ArrayList<>();
    private int selectedIndex = 0;
    private boolean open = false;
    private Consumer<String> onSelect;

    public TesseraDropdown(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    // ── Builder ────────────────────────────────────────────────────────────────

    /** Appends an option to the list. */
    public TesseraDropdown addOption(String value, String label) {
        options.add(new Option(value, label));
        return this;
    }

    /**
     * Pre-selects the option whose {@link Option#value()} equals {@code value}.
     * No-op if no match is found.
     */
    public TesseraDropdown select(String value) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).value().equals(value)) { selectedIndex = i; return this; }
        }
        return this;
    }

    /** Registers the handler called with the selected option value when selection changes. */
    public TesseraDropdown onSelect(Consumer<String> handler) {
        this.onSelect = handler;
        return this;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    /** Returns the currently selected option value, or {@code ""} if no options. */
    public String selectedValue() {
        return options.isEmpty() ? "" : options.get(selectedIndex).value();
    }

    /** Returns {@code true} when the dropdown list is currently open. */
    public boolean isOpen() { return open; }

    // ── Rendering ──────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        if (!visible) return;
        var font   = Minecraft.getInstance().font;
        float scale = 6f / 7f;
        boolean hovered = isHovered(mx, my);

        // ── Control surface ────────────────────────────────────────────────────
        int bg = (!active) ? TesseraPalette.BG1
               : (hovered && !open) ? TesseraPalette.BG3
               : TesseraPalette.BG2;
        g.fill(x, y, x + width, y + height, bg);
        int bColor = open ? TesseraPalette.COPPER : TesseraPalette.COPPER_LO;
        g.fill(x,             y,              x + width,     y + 1,          bColor);
        g.fill(x,             y + height - 1, x + width,     y + height,     bColor);
        g.fill(x,             y,              x + 1,         y + height,     bColor);
        g.fill(x + width - 1, y,              x + width,     y + height,     bColor);

        // ── Arrow (▼ / ▲) ──────────────────────────────────────────────────────
        int arrowX = x + width - 9;
        int arrowMid = y + height / 2;
        int arrowColor = active ? TesseraPalette.COPPER : TesseraPalette.TEXT_MUTE;
        if (open) {
            // ▲
            g.fill(arrowX + 3, arrowMid - 2, arrowX + 4, arrowMid - 1, arrowColor);
            g.fill(arrowX + 2, arrowMid - 1, arrowX + 5, arrowMid,     arrowColor);
            g.fill(arrowX + 1, arrowMid,     arrowX + 6, arrowMid + 1, arrowColor);
        } else {
            // ▼
            g.fill(arrowX + 1, arrowMid - 1, arrowX + 6, arrowMid,     arrowColor);
            g.fill(arrowX + 2, arrowMid,     arrowX + 5, arrowMid + 1, arrowColor);
            g.fill(arrowX + 3, arrowMid + 1, arrowX + 4, arrowMid + 2, arrowColor);
        }

        // ── Selected label ─────────────────────────────────────────────────────
        if (!options.isEmpty()) {
            String label = options.get(selectedIndex).label();
            int textColor = active ? TesseraPalette.CREAM : TesseraPalette.TEXT_MUTE;
            g.pose().pushPose();
            g.pose().translate(x + 4, y + (height - (int) (6f)) / 2f, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(font, label, 0, 0, textColor, false);
            g.pose().popPose();
        }

        // ── Open dropdown list (Z+400 to overlay siblings) ─────────────────────
        if (open) {
            int listY  = y + height;
            int listH  = options.size() * ITEM_H + 2;

            g.pose().pushPose();
            g.pose().translate(0, 0, 400);

            g.fill(x,             listY,          x + width,     listY + listH,     TesseraPalette.BG1);
            g.fill(x,             listY,          x + width,     listY + 1,         TesseraPalette.COPPER_LO);
            g.fill(x,             listY + listH - 1, x + width,  listY + listH,     TesseraPalette.COPPER_LO);
            g.fill(x,             listY,          x + 1,         listY + listH,     TesseraPalette.COPPER_LO);
            g.fill(x + width - 1, listY,          x + width,     listY + listH,     TesseraPalette.COPPER_LO);

            for (int i = 0; i < options.size(); i++) {
                int itemY    = listY + 1 + i * ITEM_H;
                boolean hover = (mx >= x && mx < x + width && my >= itemY && my < itemY + ITEM_H);
                if (hover)            g.fill(x + 1, itemY, x + width - 1, itemY + ITEM_H, TesseraPalette.BG3);
                if (i == selectedIndex) g.fill(x + 1, itemY, x + 2,         itemY + ITEM_H, TesseraPalette.COPPER);

                int textColor = (i == selectedIndex) ? TesseraPalette.COPPER_HI : TesseraPalette.CREAM;
                g.pose().pushPose();
                g.pose().translate(x + 4, itemY + (ITEM_H - (int) (6f)) / 2f, 0);
                g.pose().scale(scale, scale, 1f);
                g.drawString(font, options.get(i).label(), 0, 0, textColor, false);
                g.pose().popPose();
            }

            g.pose().popPose();
        }
    }

    // ── Input handling ─────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0 || !active || !visible) return false;

        if (open) {
            int listY = y + height;
            // Check option items (extended area below widget bounds)
            for (int i = 0; i < options.size(); i++) {
                int itemY = listY + 1 + i * ITEM_H;
                if (mx >= x && mx < x + width && my >= itemY && my < itemY + ITEM_H) {
                    selectedIndex = i;
                    open = false;
                    if (onSelect != null) onSelect.accept(options.get(i).value());
                    return true;
                }
            }
            // Anywhere else (including the control header) → close
            open = false;
            return isHovered((int) mx, (int) my); // consume only if on the control
        }

        if (isHovered((int) mx, (int) my)) {
            if (!options.isEmpty()) open = true;
            return true;
        }
        return false;
    }

    /**
     * Closes the dropdown if the mouse is released outside the control + list area.
     * This ensures the dropdown closes even when a sibling widget consumed the click first.
     */
    @Override
    public void mouseReleased(double mx, double my, int btn) {
        if (!open || btn != 0) return;
        int listY = y + height;
        int listH = options.size() * ITEM_H + 2;
        boolean inControl = isHovered((int) mx, (int) my);
        boolean inList    = mx >= x && mx < x + width && my >= listY && my < listY + listH;
        if (!inControl && !inList) open = false;
    }

    @Override
    public boolean hasClickHandler() { return true; }
}
