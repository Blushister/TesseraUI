package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * A single Minecraft item slot widget.
 *
 * <p>Displays a slot background, an {@link ItemStack} icon (scaled when the slot
 * size differs from 18), and — when {@link #showCount(boolean)} is {@code true} —
 * the stack count in the bottom-right corner using the vanilla MC font.</p>
 *
 * <p>Minimum recommended size: 18px (the vanilla slot size).  Larger values scale
 * the item icon proportionally via a pose matrix.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * TesseraItemSlot slot = new TesseraItemSlot(0, 0, 18)
 *         .item(new ItemStack(Items.DIAMOND, 3))
 *         .onClick(() -> System.out.println("clicked!"));
 * }</pre>
 */
public final class TesseraItemSlot extends TesseraElement {

    ItemStack stack       = ItemStack.EMPTY;   // package-private for TesseraItemGrid access
    private int  borderColor  = 0xFF374151;   // dark grey
    private int  slotBg       = 0xFF1f2937;   // slot background
    private int  hoverBg      = 0xFF2d3748;   // hover background
    private boolean showCount  = true;
    private Runnable onClick   = null;

    /** When {@code true}, clicking the slot opens the inventory picker overlay. */
    private boolean pickerEnabled = false;
    /** Optional callback overriding the default "copy item into slot" behaviour. */
    private Consumer<ItemStack> onItemPicked = null;

    /**
     * Creates a square item slot.
     *
     * @param x    left edge
     * @param y    top edge
     * @param size slot width and height in pixels
     */
    public TesseraItemSlot(int x, int y, int size) {
        super(x, y, size, size);
    }

    // ── Fluent builder ─────────────────────────────────────────────────────────

    /** Sets the item stack displayed in this slot. */
    public TesseraItemSlot item(ItemStack stack) {
        this.stack = stack != null ? stack : ItemStack.EMPTY;
        return this;
    }

    /** Sets the 1px border colour. */
    public TesseraItemSlot borderColor(int c) { this.borderColor = c; return this; }

    /** Sets the slot background colour (normal state). */
    public TesseraItemSlot slotBg(int c) { this.slotBg = c; return this; }

    /** Sets the slot background colour while hovered. */
    public TesseraItemSlot hoverBg(int c) { this.hoverBg = c; return this; }

    /** When {@code true} (default), the stack count is shown for stacks &gt; 1. */
    public TesseraItemSlot showCount(boolean b) { this.showCount = b; return this; }

    /** Registers a click handler for this slot. */
    public TesseraItemSlot onClick(Runnable r) { this.onClick = r; return this; }

    /**
     * Enables the inventory picker: clicking this slot opens a floating panel
     * showing the player's inventory so they can pick an item.
     * The picked {@link ItemStack} is copied into this slot by default; supply
     * {@link #onItemPicked(Consumer)} to override that behaviour.
     */
    public TesseraItemSlot inventoryPicker(boolean enabled) {
        this.pickerEnabled = enabled;
        return this;
    }

    /**
     * Custom callback invoked when the player picks an item from the inventory
     * picker.  If not set, the picked stack is simply placed into this slot via
     * {@link #item(ItemStack)}.
     */
    public TesseraItemSlot onItemPicked(Consumer<ItemStack> cb) {
        this.onItemPicked = cb;
        return this;
    }

    @Override
    public boolean hasClickHandler() { return onClick != null || pickerEnabled; }

    // ── Rendering ──────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        if (!visible) return;

        boolean hovered = isHovered(mx, my);
        int bg = (hovered && active) ? hoverBg : slotBg;

        // 1. Background fill
        g.fill(x, y, x + width, y + height, bg);

        // 2. 1-px border
        if (borderColor != 0) {
            g.fill(x,             y,              x + width,     y + 1,          borderColor);
            g.fill(x,             y + height - 1, x + width,     y + height,     borderColor);
            g.fill(x,             y,              x + 1,         y + height,     borderColor);
            g.fill(x + width - 1, y,              x + width,     y + height,     borderColor);
        }

        // 3. Item icon
        if (!stack.isEmpty()) {
            int innerSize = width - 2; // 1px border each side
            if (innerSize >= 16) {
                if (innerSize == 16) {
                    // Standard 16px render — no scaling needed
                    g.renderItem(stack, x + 1, y + 1);
                } else {
                    // Scale the 16×16 item to fill the inner area
                    float scale = innerSize / 16.0f;
                    g.pose().pushPose();
                    g.pose().translate(x + 1, y + 1, 0);
                    g.pose().scale(scale, scale, 1f);
                    g.renderItem(stack, 0, 0);
                    g.pose().popPose();
                }

                // 4. Stack count
                if (showCount && stack.getCount() > 1) {
                    var font = Minecraft.getInstance().font;
                    String countStr = String.valueOf(stack.getCount());
                    // Vanilla renders count at small scale at bottom-right of 16px area
                    // We position it at the bottom-right of our inner area
                    float countScale = 6f / 7f;
                    int textW = (int) (font.width(countStr) * countScale);
                    int tx = x + width  - 2 - textW;
                    int ty = y + height - 2 - (int)(8 * countScale);
                    g.pose().pushPose();
                    g.pose().translate(tx, ty, 200);
                    g.pose().scale(countScale, countScale, 1f);
                    g.drawString(font, countStr, 0, 0, 0xFFFFFFFF, true);
                    g.pose().popPose();
                }
            }
        }

        // 5. Hover overlay
        if (hovered && active && (onClick != null || pickerEnabled)) {
            g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x30FFFFFF);
        }

        // 6. Disabled overlay
        if (!active) {
            g.fill(x, y, x + width, y + height, 0x55000000);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!active || !visible) return false;
        if (btn == 0 && isHovered((int) mx, (int) my)) {
            if (pickerEnabled) {
                Consumer<ItemStack> cb = onItemPicked != null ? onItemPicked : s -> item(s);
                // Open below the slot; TesseraInventoryPicker clamps to screen bounds.
                TesseraInventoryPicker.open(x, y + height + 2, cb);
                return true;
            }
            if (onClick != null) {
                onClick.run();
                return true;
            }
        }
        return false;
    }
}
