package com.tesseraui.dev;

import com.tesseraui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Developer test screen for TesseraUI v1.8 features:
 * <ul>
 *   <li>Tooltips — hover a button to see a tooltip rendered by the panel</li>
 *   <li>TesseraReactiveModel — counter that updates without full screen rebuild</li>
 *   <li>TesseraToast — success / error / info toast notifications</li>
 * </ul>
 *
 * <p>Open with: {@code /tessera test-v18}</p>
 *
 * <p>Note: to register this command, add the following entry in {@code TesseraDevCommand}:
 * <pre>
 *   .then(Commands.literal("test-v18")
 *       .executes(ctx -> {
 *           Minecraft.getInstance().execute(
 *               () -> Minecraft.getInstance().setScreen(new TesseraTestScreenV18()));
 *           return 1;
 *       })
 *   )
 * </pre>
 * </p>
 */
public final class TesseraTestScreenV18 extends TesseraScreen {

    private TesseraPanel root;

    /** Reactive model holding the counter value. */
    private final TesseraReactiveModel model = TesseraReactiveModel.of(Map.of("count", "0"));

    public TesseraTestScreenV18() {
        super(Component.literal("TesseraUI v1.8 — Tooltips · Reactive · Toasts"));
    }

    @Override
    protected void init() {
        int pw = Math.min(width,  360);
        int ph = Math.min(height, 240);
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;

        // Outer wrapper panel — its children will be swapped reactively
        root = TesseraPanel.column(px, py, pw, ph)
                .background(TesseraPalette.BG0)
                .border(1, TesseraPalette.COPPER_LO)
                .padding(0).gap(0);

        int iW = pw;
        int iH = ph;

        // Handlers — declared before watchModel so the lambda captures them
        Map<String, Runnable> handlers = Map.of(
            "noop",        () -> {},
            "increment",   () -> {
                int v = parseInt(model.resolve("count"));
                model.set("count", String.valueOf(v + 1));
            },
            "decrement",   () -> {
                int v = parseInt(model.resolve("count"));
                model.set("count", String.valueOf(v - 1));
            },
            "toastSuccess", () -> TesseraToast.success("Operation successful!"),
            "toastError",   () -> TesseraToast.error("Something went wrong."),
            "toastInfo",    () -> TesseraToast.show("This is an info message.")
        );

        // Build the initial template panel and add it as the sole child of root
        TesseraTemplate template = TesseraTemplate.load("tesseraui:ui/test_v18");
        TesseraPanel inner = TesseraTemplateRenderer.build(template, model, handlers, 0, 0, iW, iH);
        root.add(inner, 0f, 0f, 0, 0, 0, null, false, 0, 0, 0, 0);
        root.layout();

        // Watch the model: when count changes, rebuild the inner template panel
        // and swap root's children to the fresh one
        final int fW = iW, fH = iH;
        root.watchModel(model, () -> {
            TesseraPanel fresh = TesseraTemplateRenderer.build(template, model, handlers, 0, 0, fW, fH);
            // Return a wrapper whose children = [fresh] so swapChildren replaces root's child
            TesseraPanel wrapper = TesseraPanel.column(0, 0, fW, fH);
            wrapper.add(fresh, 0f, 0f, 0, 0, 0, null, false, 0, 0, 0, 0);
            return wrapper;
        });
    }

    private static int parseInt(String s) {
        if (s == null) return 0;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    // ── TesseraScreen boilerplate ─────────────────────────────────────────────

    @Override
    protected TesseraPanel tesseraRoot() { return root; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        if (root != null) root.render(g, mx, my);
        TesseraToast.render(g, width, height);
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
