package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * A widget that renders mixed-style inline text with proper word-wrap.
 *
 * <p>Example: {@code <p>Hello <strong>world</strong>, how are you?</p>} becomes three
 * {@link TextRun}s — normal "Hello", bold "world", normal ", how are you?" — laid out
 * left-to-right and wrapped at the widget boundary, just like a browser would.</p>
 *
 * <p>Layout is recomputed whenever the widget width changes via {@link #setSize(int, int)},
 * which the flex layout engine calls during its pre-pass.</p>
 */
public final class TesseraRichLabel extends TesseraElement {

    // ── TextRun ───────────────────────────────────────────────────────────────

    /**
     * A styled segment of inline text.  All fields are final; create a new instance
     * for each differently-styled segment in a paragraph.
     */
    public record TextRun(
            String text,
            int    color,
            int    fontWeight,        // 400 = normal, 700 = bold
            float  fontSize,
            String fontFamily,        // null = Minecraft default
            String textDecoration     // null | "underline" | "line-through"
    ) {}

    // ── PlacedToken ───────────────────────────────────────────────────────────

    /** A single word that has been positioned by {@link #reflow()}. */
    private record PlacedToken(int x, int y, String word, TextRun run) {}

    // ── State ─────────────────────────────────────────────────────────────────

    private final List<TextRun> runs;
    private float opacity = 1f;

    /** Computed by {@link #reflow()}; rebuilt on every width change. */
    private List<PlacedToken> placed = List.of();

    // ── Construction ──────────────────────────────────────────────────────────

    public TesseraRichLabel(int x, int y, int width, int height, List<TextRun> runs) {
        super(x, y, width, height);
        this.runs = List.copyOf(runs);
        if (width > 0) reflow();
    }

    public TesseraRichLabel opacity(float o) {
        this.opacity = Math.max(0f, Math.min(1f, o));
        return this;
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    /**
     * Called by the flex layout engine when the panel assigns us a final width.
     * Re-runs the inline layout so the height is recomputed and reported correctly.
     */
    @Override
    public void setSize(int w, int h) {
        boolean widthChanged = (w != width);
        super.setSize(w, h);
        if (widthChanged && w > 0) reflow();
    }

    /**
     * Tokenizes all runs into words, then places each word left-to-right with
     * line-wrapping.  Updates {@link #height} to the total rendered height.
     */
    private void reflow() {
        var font = Minecraft.getInstance().font;

        // ── tokenise ──────────────────────────────────────────────────────────
        record Token(String word, TextRun run) {}
        List<Token> tokens = new ArrayList<>();
        for (TextRun run : runs) {
            if (run.text() == null || run.text().isBlank()) continue;
            String[] words = run.text().trim().split("\\s+");
            for (String word : words) {
                if (!word.isEmpty()) tokens.add(new Token(word, run));
            }
        }

        if (tokens.isEmpty()) { placed = List.of(); height = 0; return; }

        // ── place ─────────────────────────────────────────────────────────────
        List<PlacedToken> result = new ArrayList<>(tokens.size());
        int cx = 0;    // x-cursor (pixels from widget left)
        int cy = 0;    // y of current line top
        int lineH = 1; // tallest token on the current line

        for (int i = 0; i < tokens.size(); i++) {
            Token tok = tokens.get(i);
            TextRun r = tok.run();
            float scale  = r.fontSize() / TesseraFonts.naturalPx(r.fontFamily());
            var   comp   = TesseraFonts.component(tok.word(), r.fontFamily(), r.fontWeight());
            int   wordW  = (int) Math.ceil(font.width(comp) * scale);
            // Inter-word space: width of a space character at this run's scale.
            // Last token on the line gets no trailing space.
            int   spaceW = (i + 1 < tokens.size())
                         ? (int) Math.ceil(font.width(" ") * scale) : 0;
            int   thisH  = Math.max(1, (int) Math.ceil(9 * scale));

            // Wrap: start a new line when the word doesn't fit, unless we are already
            // at the start of the line (long word that exceeds width — render it anyway).
            if (cx > 0 && cx + wordW > width) {
                cy    += lineH;
                cx     = 0;
                lineH  = 1;
            }

            result.add(new PlacedToken(cx, cy, tok.word(), r));
            cx   += wordW + spaceW;
            lineH = Math.max(lineH, thisH);
        }

        placed = result;
        height  = cy + lineH; // total height = last line top + last line height
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        var font = Minecraft.getInstance().font;

        for (PlacedToken pt : placed) {
            TextRun r     = pt.run();
            float   scale = r.fontSize() / TesseraFonts.naturalPx(r.fontFamily());

            // Apply opacity to the packed ARGB color; ensure alpha is non-zero.
            int raw   = (r.color() & 0xFF000000) == 0 ? (r.color() | 0xFF000000) : r.color();
            int drawC = TesseraLabel.applyOpacity(raw, opacity);

            var comp = TesseraFonts.component(pt.word(), r.fontFamily(), r.fontWeight());
            int px   = x + pt.x();
            int py   = y + pt.y();

            if (Math.abs(scale - 1f) < 1e-3f) {
                g.drawString(font, comp, px, py, drawC, false);
            } else {
                g.pose().pushPose();
                g.pose().translate(px, py, 0);
                g.pose().scale(scale, scale, 1f);
                g.drawString(font, comp, 0, 0, drawC, false);
                g.pose().popPose();
            }

            // Text decoration — underline or line-through per token.
            if (r.textDecoration() != null && !"none".equals(r.textDecoration())) {
                int textW   = (int) Math.ceil(font.width(comp) * scale);
                int decoC   = drawC | 0xFF000000;
                if ("underline".equals(r.textDecoration())) {
                    int decoY = py + (int) Math.ceil(8 * scale);
                    g.fill(px, decoY, px + textW, decoY + 1, decoC);
                } else if ("line-through".equals(r.textDecoration())) {
                    int decoY = py + (int) Math.ceil(4 * scale);
                    g.fill(px, decoY, px + textW, decoY + 1, decoC);
                }
            }
        }

        renderStateOverlays(g, mx, my);
    }
}
