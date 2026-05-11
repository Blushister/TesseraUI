package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Global overlay modal system for TesseraUI.
 *
 * <p>Only one modal can be open at a time. Call {@link #show(TesseraPanel, Runnable)} to
 * display a panel as a centred overlay with a semi-transparent backdrop, then
 * {@link #close()} to dismiss it.</p>
 *
 * <p>The {@link #builder()} factory method creates a standard confirm/cancel dialog.</p>
 *
 * <p>In your screen's {@code render()} call {@link #render(GuiGraphics, int, int, int, int)}
 * after drawing normal content, and forward mouse/key events via
 * {@link TesseraScreen#handleModalEvents(double, double, int)}.</p>
 */
public final class TesseraModal {

    private static TesseraPanel currentModal = null;
    private static Runnable onClose = null;

    private TesseraModal() {}

    /**
     * Opens {@code content} as a modal overlay.
     *
     * @param content the panel to display centred on screen
     * @param onClose callback invoked when the modal is closed (may be {@code null})
     */
    public static void show(TesseraPanel content, Runnable onClose) {
        currentModal = content;
        TesseraModal.onClose = onClose;
    }

    /** Closes the current modal and fires the {@code onClose} callback. */
    public static void close() {
        Runnable cb = onClose;
        currentModal = null;
        onClose = null;
        if (cb != null) cb.run();
    }

    /** Returns {@code true} when a modal is currently visible. */
    public static boolean isOpen() {
        return currentModal != null;
    }

    /**
     * Renders the modal overlay.
     *
     * <p>Call this from {@link TesseraScreen#render} <em>after</em> normal content so the
     * modal appears on top. Does nothing when no modal is open.</p>
     *
     * @param g       GuiGraphics context
     * @param screenW GUI-scaled screen width
     * @param screenH GUI-scaled screen height
     * @param mx      mouse X
     * @param my      mouse Y
     */
    public static void render(GuiGraphics g, int screenW, int screenH, int mx, int my) {
        if (currentModal == null) return;
        // Full-screen semi-transparent backdrop
        g.fill(0, 0, screenW, screenH, 0x80000000);
        // Centre the panel
        int px = (screenW - currentModal.getWidth()) / 2;
        int py = (screenH - currentModal.getHeight()) / 2;
        currentModal.setPosition(px, py);
        currentModal.layout();
        currentModal.render(g, mx, my);
    }

    /**
     * Forwards a mouse click to the active modal.
     *
     * @return {@code true} if the modal consumed the event (always {@code true} when open,
     *         so underlying widgets are blocked)
     */
    public static boolean mouseClicked(double mx, double my, int btn) {
        if (currentModal == null) return false;
        currentModal.mouseClicked(mx, my, btn);
        return true; // absorb all clicks while modal is open
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    /** Returns a builder for a standard title-bar + content + button-row modal. */
    public static ModalBuilder builder() {
        return new ModalBuilder();
    }

    /**
     * Fluent builder for a standard confirm/cancel modal dialog.
     *
     * <pre>{@code
     * TesseraModal.builder()
     *     .title("Confirmer")
     *     .onConfirm(() -> doSomething())
     *     .onCancel(TesseraModal::close)
     *     .show();
     * }</pre>
     */
    public static final class ModalBuilder {

        private String title = "Modal";
        private TesseraPanel content = null;
        private Runnable onConfirm = null;
        private Runnable onCancel = TesseraModal::close;
        private String confirmLabel = "Confirmer";
        private String cancelLabel  = "Annuler";
        private int modalW = 200;
        private int modalH = 0; // computed

        private ModalBuilder() {}

        public ModalBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ModalBuilder content(TesseraPanel content) {
            this.content = content;
            return this;
        }

        public ModalBuilder onConfirm(Runnable onConfirm) {
            this.onConfirm = onConfirm;
            return this;
        }

        public ModalBuilder onCancel(Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        public ModalBuilder confirmLabel(String label) {
            this.confirmLabel = label;
            return this;
        }

        public ModalBuilder cancelLabel(String label) {
            this.cancelLabel = label;
            return this;
        }

        public ModalBuilder width(int w) {
            this.modalW = w;
            return this;
        }

        /** Builds and displays the modal. */
        public void show() {
            int iW = modalW - 12; // inner width (padding 6 each side)

            // ── Title bar ─────────────────────────────────────────────────────
            TesseraPanel titleBar = TesseraPanel.row(0, 0, iW, 14)
                    .background(TesseraPalette.BG3)
                    .padding(0, 4, 0, 4)
                    .alignItems("center");
            titleBar.add(new TesseraLabel(0, 0, iW - 16, 9, title)
                    .color(TesseraPalette.COPPER_HI).fontSize(7f).fontWeight(700));
            // Close button (×) in title bar
            titleBar.add(new TesseraButton(0, 0, 12, 12)
                    .label("×").bgColor(TesseraPalette.BG3)
                    .labelColor(TesseraPalette.CREAM_DIM).fontSize(8f)
                    .onClick(TesseraModal::close));

            // ── Content area ──────────────────────────────────────────────────
            int contentH = 0;
            if (content != null) {
                content.setSize(iW, content.getHeight());
                content.layout();
                contentH = content.getHeight();
            }

            // ── Button row ────────────────────────────────────────────────────
            int btnW = 60;
            TesseraPanel btnRow = TesseraPanel.row(0, 0, iW, 16)
                    .justifyContent("flex-end")
                    .gap(4)
                    .padding(0, 2, 0, 0);

            if (onCancel != null) {
                final Runnable cancelCb = onCancel;
                btnRow.add(new TesseraButton(0, 0, btnW, 12)
                        .label(cancelLabel)
                        .bgColor(TesseraPalette.BG2)
                        .labelColor(TesseraPalette.CREAM_DIM)
                        .fontSize(6f)
                        .onClick(cancelCb));
            }
            if (onConfirm != null) {
                final Runnable confirmCb = onConfirm;
                btnRow.add(new TesseraButton(0, 0, btnW, 12)
                        .label(confirmLabel)
                        .bgColor(TesseraPalette.COPPER_LO)
                        .labelColor(TesseraPalette.CREAM)
                        .fontSize(6f)
                        .onClick(confirmCb));
            }

            // ── Outer container ───────────────────────────────────────────────
            int totalH = 14 + (contentH > 0 ? contentH + 4 : 0) + 4 + 16 + 8;
            TesseraPanel modal = TesseraPanel.column(0, 0, modalW, totalH)
                    .background(TesseraPalette.BG1)
                    .border(1, TesseraPalette.COPPER_LO)
                    .padding(6)
                    .gap(4);

            modal.add(titleBar);
            if (content != null) modal.add(content);
            modal.add(btnRow);
            modal.layout();

            TesseraModal.show(modal, null);
        }
    }
}
