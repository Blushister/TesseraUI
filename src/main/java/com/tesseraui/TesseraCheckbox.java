package com.tesseraui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;

/**
 * A 10×10 checkbox widget that toggles its checked state on click.
 *
 * <p>Extends {@link TesseraElement} and therefore inherits visibility support
 * ({@code isVisible()}/{@code setVisible()}) for {@code v-show} semantics.</p>
 */
public class TesseraCheckbox extends TesseraElement {

    private boolean checked;
    private Consumer<Boolean> onToggle;

    private static final int COLOR_BOX_BG     = 0xFF2A2A2A;
    private static final int COLOR_BOX_BORDER  = 0xFF888888;
    private static final int COLOR_CHECK       = TesseraPalette.COPPER_HI;
    private static final int COLOR_HOVER       = 0x22FFFFFF;

    public TesseraCheckbox(int x, int y, int width, int height, boolean checked) {
        super(x, y, width, height);
        this.checked = checked;
    }

    public TesseraCheckbox onToggle(Consumer<Boolean> handler) {
        this.onToggle = handler;
        return this;
    }

    public boolean isChecked() { return checked; }

    @Override
    public boolean hasClickHandler() { return true; }

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        if (!visible) return;
        // Box background
        g.fill(x, y, x + width, y + height, COLOR_BOX_BG);

        // Border (1px)
        g.fill(x,             y,              x + width,     y + 1,         COLOR_BOX_BORDER);
        g.fill(x,             y + height - 1, x + width,     y + height,    COLOR_BOX_BORDER);
        g.fill(x,             y,              x + 1,         y + height,    COLOR_BOX_BORDER);
        g.fill(x + width - 1, y,              x + width,     y + height,    COLOR_BOX_BORDER);

        // Check mark (filled inner rect when checked)
        if (checked) {
            int inset = Math.max(2, Math.min(3, width / 3));
            g.fill(x + inset, y + inset, x + width - inset, y + height - inset, COLOR_CHECK);
        }

        // Hover overlay
        if (active && isHovered(mx, my)) {
            g.fill(x, y, x + width, y + height, COLOR_HOVER);
        }

        // Disabled overlay
        if (!active) {
            g.fill(x, y, x + width, y + height, 0x55000000);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0 || !active || !visible || !isHovered((int) mx, (int) my)) return false;
        checked = !checked;
        if (onToggle != null) onToggle.accept(checked);
        return true;
    }
}
