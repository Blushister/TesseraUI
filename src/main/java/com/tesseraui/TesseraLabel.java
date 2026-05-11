package com.tesseraui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class TesseraLabel extends TesseraElement {

    private String text;
    private int color = TesseraPalette.CREAM;
    private String textAlign = "left";
    private String fontFamily = null;
    private float fontSize = 7f;
    private int fontWeight = 400;
    private String textTransform = null;
    private boolean clipOverflow = false;
    private float opacity = 1f;
    private String textDecoration = null; // null | "none" | "underline" | "line-through"

    /** When true, text wraps at the widget boundary instead of being clipped. */
    private boolean wrap = false;
    /** Cached line split; recomputed whenever wrap=true and the width changes. */
    private List<FormattedCharSequence> wrappedLines = null;

    public TesseraLabel(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        this.text = text;
    }

    public TesseraLabel text(String text) { this.text = text; return this; }
    public TesseraLabel color(int color)  { this.color = color; return this; }
    public TesseraLabel textAlign(String align) { this.textAlign = align; return this; }
    public TesseraLabel font(String fontFamily) { this.fontFamily = fontFamily; return this; }
    public TesseraLabel fontSize(float px) { if (px > 0) this.fontSize = px; return this; }
    public TesseraLabel fontWeight(int w) { if (w > 0) this.fontWeight = w; return this; }
    public TesseraLabel textTransform(String tt) { this.textTransform = tt; return this; }
    public TesseraLabel clipOverflow(boolean clip) { this.clipOverflow = clip; return this; }
    public TesseraLabel opacity(float o) { this.opacity = Math.max(0f, Math.min(1f, o)); return this; }
    public TesseraLabel textDecoration(String td) { this.textDecoration = td; return this; }

    /**
     * Enables text wrapping.  When {@code true}, the label splits its text into
     * multiple lines using {@code font.split()} and expands its height accordingly.
     * The height is recomputed immediately if the current width is already known.
     */
    public TesseraLabel wrap(boolean wrap) {
        this.wrap = wrap;
        if (wrap && width > 0) recomputeLines();
        return this;
    }

    /**
     * Overridden so that when wrap mode is active, changing the width causes the
     * line list (and therefore the reported height) to be recomputed.  Called by the
     * layout engine's pre-pass before it sums child heights.
     */
    @Override
    public void setSize(int w, int h) {
        boolean widthChanged = (w != width);
        super.setSize(w, h);
        if (wrap && widthChanged && w > 0) recomputeLines();
    }

    // ── wrap helpers ─────────────────────────────────────────────────────────

    /**
     * Splits the label text into lines that fit within the current pixel width and
     * updates {@link #height} to match the resulting line count.
     * A minimum of one line height is always preserved.
     */
    private void recomputeLines() {
        if (width <= 0) return;
        var font = Minecraft.getInstance().font;
        float scale = fontSize / TesseraFonts.naturalPx(fontFamily);
        // Convert pixel width → font-native units (font renders at scale=1 natively).
        int maxFontUnits = Math.max(1, (int) (width / scale));
        String displayed = TesseraTextStyling.transform(text != null ? text : "", textTransform);
        var comp = TesseraFonts.component(displayed, fontFamily, fontWeight);
        wrappedLines = font.split(comp, maxFontUnits);
        // Line height in screen pixels: MC font uses 9 natural units per line (8 glyph + 1 gap)
        int lineH = Math.max(1, (int) Math.ceil(9 * scale));
        int lines = wrappedLines.isEmpty() ? 1 : wrappedLines.size();
        height = lines * lineH;
    }

    // ── rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my) {
        var font = Minecraft.getInstance().font;
        float scale = fontSize / TesseraFonts.naturalPx(fontFamily);
        int drawColor = applyOpacity(color, opacity);

        if (wrap) {
            // Ensure lines are computed (may be null if wrap was set before a valid width)
            if (wrappedLines == null && width > 0) recomputeLines();
            if (wrappedLines != null && !wrappedLines.isEmpty()) {
                int lineH = Math.max(1, (int) Math.ceil(9 * scale));
                g.pose().pushPose();
                g.pose().translate(x, y, 0);
                if (Math.abs(scale - 1f) >= 1e-3f) g.pose().scale(scale, scale, 1f);
                for (int i = 0; i < wrappedLines.size(); i++) {
                    // In the scaled pose, each line is 9 natural units below the previous.
                    int lineY = Math.round(i * lineH / scale);
                    g.drawString(font, wrappedLines.get(i), 0, lineY, drawColor, false);
                }
                g.pose().popPose();
                // Text-decoration lines (underline / line-through) per wrapped line
                if (textDecoration != null && !"none".equals(textDecoration)) {
                    int lineH2 = Math.max(1, (int) Math.ceil(9 * scale));
                    int decoColor = ensureAlpha(drawColor);
                    for (int li = 0; li < wrappedLines.size(); li++) {
                        int lw = (int) Math.ceil(font.width(wrappedLines.get(li)) * scale);
                        int lineTopY = y + li * lineH2;
                        if ("underline".equals(textDecoration)) {
                            int decoY = lineTopY + (int) Math.ceil(8 * scale);
                            g.fill(x, decoY, x + lw, decoY + 1, decoColor);
                        } else if ("line-through".equals(textDecoration)) {
                            int decoY = lineTopY + (int) Math.ceil(4 * scale);
                            g.fill(x, decoY, x + lw, decoY + 1, decoColor);
                        }
                    }
                }
                renderStateOverlays(g, mx, my);
                return;
            }
        }

        // ── single-line path (original behaviour) ────────────────────────────
        String displayed = TesseraTextStyling.transform(text, textTransform);

        if (clipOverflow && width > 0) {
            int rawW = (int) Math.ceil(font.width(Component.literal(displayed)) * scale);
            if (rawW > width) {
                String ellipsed = displayed;
                while (!ellipsed.isEmpty()
                        && font.width(Component.literal(ellipsed + "…")) * scale > width) {
                    ellipsed = ellipsed.substring(0, ellipsed.length() - 1);
                }
                displayed = ellipsed + "…";
            }
        }

        var comp = TesseraFonts.component(displayed, fontFamily, fontWeight);

        int textX;
        int textW = (int) Math.ceil(font.width(comp) * scale);
        switch (textAlign) {
            case "center" -> textX = x + (width - textW) / 2;
            case "right"  -> textX = x + width - textW;
            default       -> textX = x;
        }
        int textY = y + (height - (int) Math.ceil(8 * scale)) / 2;

        if (Math.abs(scale - 1f) < 1e-3f) {
            g.drawString(font, comp, textX, textY, drawColor, false);
        } else {
            g.pose().pushPose();
            g.pose().translate(textX, textY, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(font, comp, 0, 0, drawColor, false);
            g.pose().popPose();
        }
        // Text-decoration lines (underline / line-through)
        if (textDecoration != null && !"none".equals(textDecoration)) {
            int decoColor = ensureAlpha(drawColor);
            if ("underline".equals(textDecoration)) {
                int decoY = textY + (int) Math.ceil(8 * scale);
                g.fill(textX, decoY, textX + textW, decoY + 1, decoColor);
            } else if ("line-through".equals(textDecoration)) {
                int decoY = textY + (int) Math.ceil(4 * scale);
                g.fill(textX, decoY, textX + textW, decoY + 1, decoColor);
            }
        }
        renderStateOverlays(g, mx, my);
    }

    static int applyOpacity(int color, float opacity) {
        if (opacity >= 1f) return color;
        int a = (color >>> 24) & 0xFF;
        if (a == 0) a = 255;
        int newA = Math.max(0, Math.min(255, (int) (a * opacity)));
        return (newA << 24) | (color & 0xFFFFFF);
    }

    /** Ensures the packed ARGB color has non-zero alpha (treats missing alpha as fully opaque). */
    private static int ensureAlpha(int color) {
        return (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color;
    }
}
