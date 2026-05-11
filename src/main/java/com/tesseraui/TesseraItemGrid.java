package com.tesseraui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * A grid of {@link TesseraItemSlot}s arranged in {@code cols × rows}.
 *
 * <p>Supports drag-and-drop between slots: left-clicking a non-empty slot and
 * dragging it over another slot swaps the two stacks when the mouse is released.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * TesseraItemGrid grid = new TesseraItemGrid(x, y, 4, 2, 18);
 * grid.setItem(0, new ItemStack(Items.DIAMOND));
 * grid.setItem(1, new ItemStack(Items.GOLD_INGOT, 3));
 * }</pre>
 */
public final class TesseraItemGrid extends TesseraElement {

    private static final int GAP = 1;

    private final int cols, rows, slotSize;
    private final ItemStack[] contents;
    private final TesseraItemSlot[] slots;

    /** Index of the slot currently being dragged (-1 = none). */
    private int dragSourceIndex = -1;

    /**
     * Creates a new item grid.
     *
     * @param x        left edge
     * @param y        top edge
     * @param cols     number of columns
     * @param rows     number of rows
     * @param slotSize pixel size of each (square) slot
     */
    public TesseraItemGrid(int x, int y, int cols, int rows, int slotSize) {
        super(x, y,
              cols * slotSize + (cols - 1) * GAP,
              rows * slotSize + (rows - 1) * GAP);
        this.cols     = cols;
        this.rows     = rows;
        this.slotSize = slotSize;
        this.contents = new ItemStack[cols * rows];
        this.slots    = new TesseraItemSlot[cols * rows];
        for (int i = 0; i < contents.length; i++) {
            contents[i] = ItemStack.EMPTY;
        }
        rebuildSlots();
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Sets the item stack at a slot index.
     *
     * @param index 0-based index (row-major: index = row * cols + col)
     * @param stack the stack to place, or {@link ItemStack#EMPTY} to clear
     * @return {@code this} for chaining
     */
    public TesseraItemGrid setItem(int index, ItemStack stack) {
        if (index >= 0 && index < contents.length) {
            contents[index] = stack != null ? stack : ItemStack.EMPTY;
            slots[index].item(contents[index]);
        }
        return this;
    }

    /**
     * Returns the stack at the given slot index.
     *
     * @param index 0-based index
     * @return the stack, or {@link ItemStack#EMPTY} if empty / out of range
     */
    public ItemStack getItem(int index) {
        if (index < 0 || index >= contents.length) return ItemStack.EMPTY;
        return contents[index];
    }

    // ── TesseraWidget ──────────────────────────────────────────────────────────

    @Override
    public void setPosition(int nx, int ny) {
        this.x = nx;
        this.y = ny;
        repositionSlots();
    }

    @Override
    public void setSize(int w, int h) {
        this.width  = w;
        this.height = h;
        // Slots keep their original size; only position changes with the grid
    }

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        if (!visible) return;

        // Highlight the slot under cursor during a drag from this grid
        int hoveredIdx = slotIndexAt((int) mx, (int) my);

        for (int i = 0; i < slots.length; i++) {
            TesseraItemSlot slot = slots[i];
            // When dragging internally, mark the source slot as empty visually
            if (i == dragSourceIndex && TesseraDragContext.isDragging()) {
                ItemStack saved = slot.stack;
                slot.item(ItemStack.EMPTY);
                slot.render(g, mx, my);
                slot.item(saved);
            } else {
                slot.render(g, mx, my);
            }
            // Drop-target highlight
            if (i == hoveredIdx && dragSourceIndex >= 0 && TesseraDragContext.isDragging()) {
                g.fill(slot.x + 1, slot.y + 1,
                       slot.x + slotSize - 1, slot.y + slotSize - 1,
                       0x4000FF00);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!active || !visible) return false;
        int idx = slotIndexAt((int) mx, (int) my);
        if (idx < 0) return false;

        if (btn == 0) {
            ItemStack stack = contents[idx];
            if (!stack.isEmpty()) {
                dragSourceIndex = idx;
                // Start drag with the stack as payload
                TesseraDragContext.startDrag(slots[idx], stack, (int) mx, (int) my);
                return true;
            }
            // Click on empty slot — fire slot's click handler if any
            return slots[idx].mouseClicked(mx, my, btn);
        }
        return false;
    }

    @Override
    public void mouseReleased(double mx, double my, int btn) {
        if (btn == 0 && dragSourceIndex >= 0 && TesseraDragContext.isDragging()) {
            int targetIdx = slotIndexAt((int) mx, (int) my);
            if (targetIdx >= 0 && targetIdx != dragSourceIndex) {
                // Swap the two stacks
                ItemStack tmp = contents[targetIdx];
                contents[targetIdx] = contents[dragSourceIndex];
                contents[dragSourceIndex] = tmp;
                slots[dragSourceIndex].item(contents[dragSourceIndex]);
                slots[targetIdx].item(contents[targetIdx]);
            }
            TesseraDragContext.endDrag((int) mx, (int) my);
            dragSourceIndex = -1;
        }
    }

    @Override
    public void mouseDragged(double mx, double my, int btn) {
        if (btn == 0 && dragSourceIndex >= 0) {
            TesseraDragContext.updatePosition((int) mx, (int) my);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void rebuildSlots() {
        for (int i = 0; i < contents.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int sx = x + col * (slotSize + GAP);
            int sy = y + row * (slotSize + GAP);
            slots[i] = new TesseraItemSlot(sx, sy, slotSize).item(contents[i]);
        }
    }

    private void repositionSlots() {
        for (int i = 0; i < slots.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int sx = x + col * (slotSize + GAP);
            int sy = y + row * (slotSize + GAP);
            slots[i].setPosition(sx, sy);
        }
    }

    /** Returns the slot index at screen position {@code (mx, my)}, or {@code -1}. */
    private int slotIndexAt(int mx, int my) {
        if (mx < x || my < y || mx >= x + width || my >= y + height) return -1;
        int relX = mx - x;
        int relY = my - y;
        int col = relX / (slotSize + GAP);
        int row = relY / (slotSize + GAP);
        if (col >= cols || row >= rows) return -1;
        // Verify we're inside the slot and not in the gap
        int slotStartX = col * (slotSize + GAP);
        int slotStartY = row * (slotSize + GAP);
        if (relX - slotStartX >= slotSize || relY - slotStartY >= slotSize) return -1;
        return row * cols + col;
    }
}
