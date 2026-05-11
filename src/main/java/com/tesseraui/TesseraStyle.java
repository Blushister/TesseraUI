package com.tesseraui;

public final class TesseraStyle {

    public static final int    UNSET   = Integer.MIN_VALUE;
    public static final float  UNSET_F = Float.MIN_VALUE;
    public static final TesseraStyle EMPTY = new TesseraStyle();

    public int background  = UNSET;
    public int color       = UNSET;
    public int borderColor = UNSET;

    public int borderTopColor    = UNSET;
    public int borderBottomColor = UNSET;
    public int borderLeftColor   = UNSET;
    public int borderRightColor  = UNSET;

    public int width    = UNSET;
    public int height   = UNSET;
    public int minWidth = UNSET;
    public int maxWidth = UNSET;
    public int minHeight = UNSET;
    public int maxHeight = UNSET;

    public boolean widthPercent     = false;
    public boolean heightPercent    = false;
    public boolean minWidthPercent  = false;
    public boolean maxWidthPercent  = false;
    public boolean minHeightPercent = false;
    public boolean maxHeightPercent = false;

    public int paddingTop    = UNSET;
    public int paddingRight  = UNSET;
    public int paddingBottom = UNSET;
    public int paddingLeft   = UNSET;

    public int     marginTop    = UNSET;
    public int     marginRight  = UNSET;
    public int     marginBottom = UNSET;
    public int     marginLeft   = UNSET;
    public boolean marginTopAuto = false;

    public int gap  = UNSET;
    public int border = UNSET;

    // ── Full flexbox model ────────────────────────────────────────────────────
    /** CSS {@code flex-grow}  — UNSET_F means "not set" (effective default: 0). */
    public float flexGrow   = UNSET_F;
    /** CSS {@code flex-shrink} — UNSET_F means "not set" (effective default: 1). */
    public float flexShrink = UNSET_F;
    /** CSS {@code flex-basis} in px — UNSET means {@code auto} (use content size). */
    public int   flexBasis  = UNSET;
    /** CSS {@code order} — UNSET means "not set" (effective default: 0). */
    public int   order      = UNSET;
    /** CSS {@code z-index} — UNSET means "not set" (effective default: 0). */
    public int   zIndex     = UNSET;

    // ── calc() expressions ────────────────────────────────────────────────────
    /** Raw {@code calc(...)} string for {@code width}; null when width is a plain value. */
    public String widthCalc  = null;
    /** Raw {@code calc(...)} string for {@code height}; null when height is a plain value. */
    public String heightCalc = null;

    public String textAlign = null;
    public String textTransform = null;
    public float  fontSize = UNSET_F;
    public int    fontWeight = UNSET;

    public float  opacity  = UNSET_F;
    public String overflow = null;

    public String display        = null;
    public String flexDirection  = null;
    public String flexWrap       = null;
    public String alignItems     = null;
    public String alignSelf      = null;
    public String justifyContent = null;

    public String[] gridTemplateColumns = null;

    public String boxSizing = "border-box";

    public int cornerDotSize  = UNSET;
    public int cornerDotColor = UNSET;

    public int hoverBackground       = UNSET;
    public int hoverBorderColor      = UNSET;
    public int hoverBorderTopColor    = UNSET;
    public int hoverBorderBottomColor = UNSET;
    public int hoverBorderLeftColor   = UNSET;
    public int hoverBorderRightColor  = UNSET;
    public int hoverColor            = UNSET;

    public String fontFamily      = null;
    public String whiteSpace      = null; // null | "normal" | "nowrap"
    public String textDecoration  = null; // null | "none" | "underline" | "line-through"

    public int borderRadius = UNSET; // px, applied as rounded corners on TesseraPanel

    public String position = null; // null (static) | "relative" | "absolute"
    public int top    = UNSET;
    public int left   = UNSET;
    public int right  = UNSET;
    public int bottom = UNSET;

    /** CSS {@code transition-duration} en ms pour la propriété hover ; 0 = pas d'animation. */
    public int    transitionDurationMs  = 0;
    /** CSS {@code transition-property} : "background" | "opacity" | "border-color" | "all" | null. */
    public String transitionProperty    = null;

    public TesseraStyle() {}

    public int padding() { return paddingTop != UNSET ? paddingTop : UNSET; }
    public int margin()  { return marginTop  != UNSET ? marginTop  : UNSET; }

    public TesseraStyle merge(TesseraStyle other) {
        TesseraStyle r = new TesseraStyle();

        r.background  = other.background  != UNSET ? other.background  : this.background;
        r.color       = other.color       != UNSET ? other.color       : this.color;
        r.borderColor = other.borderColor != UNSET ? other.borderColor : this.borderColor;

        r.borderTopColor    = other.borderTopColor    != UNSET ? other.borderTopColor    : this.borderTopColor;
        r.borderBottomColor = other.borderBottomColor != UNSET ? other.borderBottomColor : this.borderBottomColor;
        r.borderLeftColor   = other.borderLeftColor   != UNSET ? other.borderLeftColor   : this.borderLeftColor;
        r.borderRightColor  = other.borderRightColor  != UNSET ? other.borderRightColor  : this.borderRightColor;

        if (other.width != UNSET)     { r.width = other.width;       r.widthPercent     = other.widthPercent;     } else { r.width = this.width;       r.widthPercent     = this.widthPercent;     }
        if (other.height != UNSET)    { r.height = other.height;     r.heightPercent    = other.heightPercent;    } else { r.height = this.height;     r.heightPercent    = this.heightPercent;    }
        if (other.minWidth != UNSET)  { r.minWidth = other.minWidth; r.minWidthPercent  = other.minWidthPercent;  } else { r.minWidth = this.minWidth; r.minWidthPercent  = this.minWidthPercent;  }
        if (other.maxWidth != UNSET)  { r.maxWidth = other.maxWidth; r.maxWidthPercent  = other.maxWidthPercent;  } else { r.maxWidth = this.maxWidth; r.maxWidthPercent  = this.maxWidthPercent;  }
        if (other.minHeight != UNSET) { r.minHeight= other.minHeight;r.minHeightPercent = other.minHeightPercent; } else { r.minHeight= this.minHeight;r.minHeightPercent = this.minHeightPercent; }
        if (other.maxHeight != UNSET) { r.maxHeight= other.maxHeight;r.maxHeightPercent = other.maxHeightPercent; } else { r.maxHeight= this.maxHeight;r.maxHeightPercent = this.maxHeightPercent; }

        r.paddingTop    = other.paddingTop    != UNSET ? other.paddingTop    : this.paddingTop;
        r.paddingRight  = other.paddingRight  != UNSET ? other.paddingRight  : this.paddingRight;
        r.paddingBottom = other.paddingBottom != UNSET ? other.paddingBottom : this.paddingBottom;
        r.paddingLeft   = other.paddingLeft   != UNSET ? other.paddingLeft   : this.paddingLeft;

        r.marginTop    = other.marginTop    != UNSET ? other.marginTop    : this.marginTop;
        r.marginRight  = other.marginRight  != UNSET ? other.marginRight  : this.marginRight;
        r.marginBottom = other.marginBottom != UNSET ? other.marginBottom : this.marginBottom;
        r.marginLeft   = other.marginLeft   != UNSET ? other.marginLeft   : this.marginLeft;
        r.marginTopAuto = other.marginTopAuto || this.marginTopAuto;

        r.gap    = other.gap    != UNSET ? other.gap    : this.gap;
        r.border = other.border != UNSET ? other.border : this.border;

        r.flexGrow   = other.flexGrow   != UNSET_F ? other.flexGrow   : this.flexGrow;
        r.flexShrink = other.flexShrink != UNSET_F ? other.flexShrink : this.flexShrink;
        r.flexBasis  = other.flexBasis  != UNSET   ? other.flexBasis  : this.flexBasis;
        r.order      = other.order      != UNSET   ? other.order      : this.order;
        r.zIndex     = other.zIndex     != UNSET   ? other.zIndex     : this.zIndex;

        r.widthCalc  = other.widthCalc  != null ? other.widthCalc  : this.widthCalc;
        r.heightCalc = other.heightCalc != null ? other.heightCalc : this.heightCalc;

        r.textAlign     = other.textAlign     != null ? other.textAlign     : this.textAlign;
        r.textTransform = other.textTransform != null ? other.textTransform : this.textTransform;
        r.fontSize      = other.fontSize      != UNSET_F ? other.fontSize : this.fontSize;
        r.fontWeight    = other.fontWeight    != UNSET ? other.fontWeight : this.fontWeight;
        r.opacity       = other.opacity       != UNSET_F ? other.opacity : this.opacity;
        r.overflow      = other.overflow      != null ? other.overflow  : this.overflow;

        r.display        = other.display        != null ? other.display        : this.display;
        r.flexDirection  = other.flexDirection  != null ? other.flexDirection  : this.flexDirection;
        r.flexWrap       = other.flexWrap       != null ? other.flexWrap       : this.flexWrap;
        r.alignItems     = other.alignItems     != null ? other.alignItems     : this.alignItems;
        r.alignSelf      = other.alignSelf      != null ? other.alignSelf      : this.alignSelf;
        r.justifyContent = other.justifyContent != null ? other.justifyContent : this.justifyContent;
        r.fontFamily     = other.fontFamily     != null ? other.fontFamily     : this.fontFamily;
        r.whiteSpace      = other.whiteSpace      != null ? other.whiteSpace      : this.whiteSpace;
        r.textDecoration  = other.textDecoration  != null ? other.textDecoration  : this.textDecoration;

        r.gridTemplateColumns = other.gridTemplateColumns != null ? other.gridTemplateColumns : this.gridTemplateColumns;
        r.boxSizing = other.boxSizing != null ? other.boxSizing : this.boxSizing;

        r.cornerDotSize  = other.cornerDotSize  != UNSET ? other.cornerDotSize  : this.cornerDotSize;
        r.cornerDotColor = other.cornerDotColor != UNSET ? other.cornerDotColor : this.cornerDotColor;

        r.hoverBackground        = other.hoverBackground        != UNSET ? other.hoverBackground        : this.hoverBackground;
        r.hoverBorderColor       = other.hoverBorderColor       != UNSET ? other.hoverBorderColor       : this.hoverBorderColor;
        r.hoverBorderTopColor    = other.hoverBorderTopColor    != UNSET ? other.hoverBorderTopColor    : this.hoverBorderTopColor;
        r.hoverBorderBottomColor = other.hoverBorderBottomColor != UNSET ? other.hoverBorderBottomColor : this.hoverBorderBottomColor;
        r.hoverBorderLeftColor   = other.hoverBorderLeftColor   != UNSET ? other.hoverBorderLeftColor   : this.hoverBorderLeftColor;
        r.hoverBorderRightColor  = other.hoverBorderRightColor  != UNSET ? other.hoverBorderRightColor  : this.hoverBorderRightColor;
        r.hoverColor             = other.hoverColor             != UNSET ? other.hoverColor             : this.hoverColor;

        r.borderRadius = other.borderRadius != UNSET ? other.borderRadius : this.borderRadius;

        r.position = other.position != null ? other.position : this.position;
        r.top    = other.top    != UNSET ? other.top    : this.top;
        r.left   = other.left   != UNSET ? other.left   : this.left;
        r.right  = other.right  != UNSET ? other.right  : this.right;
        r.bottom = other.bottom != UNSET ? other.bottom : this.bottom;

        r.transitionDurationMs = other.transitionDurationMs != 0   ? other.transitionDurationMs  : this.transitionDurationMs;
        r.transitionProperty   = other.transitionProperty   != null ? other.transitionProperty    : this.transitionProperty;

        return r;
    }
}
