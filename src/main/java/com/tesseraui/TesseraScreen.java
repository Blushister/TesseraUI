package com.tesseraui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class for screens built with TesseraUI.
 *
 * <p>Extend this instead of {@link Screen} to get un rendu propre par défaut :</p>
 * <ul>
 *   <li>Pas de shader Gaussian blur (ajouté automatiquement par MC 1.21 derrière
 *       tous les {@code Screen}).</li>
 *   <li>Pas d'overlay sombre — les {@link TesseraPanel} gèrent leur propre fond.</li>
 *   <li>{@link #mouseDragged} forwardé au panel pour que les {@link TesseraSlider}
 *       fonctionnent sans code supplémentaire.</li>
 * </ul>
 *
 * <p>Exemple minimal :</p>
 * <pre>{@code
 * public class MyScreen extends TesseraScreen {
 *
 *     private TesseraPanel ui;
 *
 *     public MyScreen() { super(Component.literal("My Screen")); }
 *
 *     @Override
 *     protected void init() {
 *         TesseraTemplate tpl = TesseraTemplate.load("mymod:ui/my_screen");
 *         ui = TesseraTemplateRenderer.build(tpl, model, handlers,
 *                  (width - 200) / 2, (height - 120) / 2, 200, 120);
 *     }
 *
 *     @Override
 *     public void render(GuiGraphics g, int mx, int my, float delta) {
 *         super.render(g, mx, my, delta);   // appelle renderBackground propre
 *         ui.render(g, mx, my);
 *     }
 *
 *     @Override protected TesseraPanel tesseraRoot() { return ui; }
 * }
 * }</pre>
 */
public abstract class TesseraScreen extends Screen {

    protected TesseraScreen(Component title) {
        super(title);
    }

    // ── Blur supprimé ─────────────────────────────────────────────────────────

    /**
     * Supprime le shader de flou Gaussian de MC 1.21 et n'ajoute aucun overlay
     * sombre. Les panels TesseraUI fournissent leur propre fond via
     * {@link TesseraPanel#background(int)}.
     *
     * <p>Pour ajouter un fond semi-transparent derrière votre UI :</p>
     * <pre>{@code
     * @Override
     * public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
     *     super.renderBackground(g, mx, my, delta);          // no-op (pas de blur)
     *     g.fill(0, 0, width, height, 0x80000000);           // overlay custom
     * }
     * }</pre>
     */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
        // no-op : pas de blur, pas d'overlay — votre TesseraPanel s'en charge.
    }

    // ── Forwarding souris ─────────────────────────────────────────────────────

    /**
     * Retourne le panel racine de cet écran, utilisé pour le forwarding automatique
     * de {@link #mouseDragged} (nécessaire pour {@link TesseraSlider}).
     *
     * <p>Retournez simplement votre variable {@code root} / {@code ui}.</p>
     *
     * @return le panel racine, ou {@code null} si pas encore initialisé
     */
    protected abstract TesseraPanel tesseraRoot();

    /**
     * Forwarde le drag au panel racine, ce qui permet aux {@link TesseraSlider}
     * de mettre à jour leur poignée pendant le drag sans que vous ayez à l'écrire
     * dans chaque sous-classe.
     */
    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        TesseraPanel root = tesseraRoot();
        if (root != null) root.mouseDragged(mx, my, btn);
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    // ── Hot reload ────────────────────────────────────────────────────────────

    /**
     * Polls {@link TesseraHotReload} once per tick.  When any template has been
     * invalidated (file changed on disk or F3+T pressed), calls {@link #init()} so the
     * screen rebuilds with the fresh template — no client restart required.
     *
     * <p>Only active when hot reload is enabled; zero overhead in production.</p>
     */
    @Override
    public void tick() {
        super.tick();
        if (TesseraHotReload.isActive() && TesseraHotReload.consumeDirty()) {
            init();
        }
    }
}
