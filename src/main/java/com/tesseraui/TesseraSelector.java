package com.tesseraui;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * CSS selector model. Supports compound selectors (class + tag), descendant combinators
 * ({@code .a .b}) and direct child combinators ({@code .a > .b}).
 */
public final class TesseraSelector {

    public static final class Segment {
        public final String tag;
        public final List<String> classes;
        /**
         * Parsed {@code :nth-child} expression: {@code "odd"}, {@code "even"}, or a
         * positive integer string. {@code null} means no {@code :nth-child} constraint.
         */
        public final String nthChild;

        public Segment(String tag, List<String> classes) {
            this(tag, classes, null);
        }

        public Segment(String tag, List<String> classes, String nthChild) {
            this.tag = tag;
            this.classes = classes;
            this.nthChild = nthChild;
        }

        public boolean matches(TesseraNode node) {
            if (tag != null && !tag.equals(node.tag())) return false;
            if (!classes.isEmpty()) {
                List<String> nodeClasses = node.classNames();
                for (String c : classes) if (!nodeClasses.contains(c)) return false;
            }
            // nth-child: compare against __nth-index injected by TesseraTemplateRenderer
            if (nthChild != null) {
                String idxAttr = node.attr("__nth-index");
                if (idxAttr.isBlank()) return false;
                try {
                    int n = Integer.parseInt(idxAttr);
                    if (!matchesNth(nthChild, n)) return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }

        private static boolean matchesNth(String expr, int n) {
            return switch (expr.toLowerCase(java.util.Locale.ROOT)) {
                case "odd"  -> (n % 2) != 0;
                case "even" -> (n % 2) == 0;
                default -> {
                    try { yield Integer.parseInt(expr) == n; }
                    catch (NumberFormatException e) { yield false; }
                }
            };
        }

        public int specificity() {
            int s = classes.size() * 10;
            if (tag != null) s += 1;
            if (nthChild != null) s += 10; // :nth-child has same weight as a class
            return s;
        }
    }

    public enum Combinator { DESCENDANT, CHILD }

    public final List<Segment> segments;
    public final List<Combinator> combinators;
    public final String raw;

    public TesseraSelector(List<Segment> segments, List<Combinator> combinators, String raw) {
        this.segments = List.copyOf(segments);
        this.combinators = List.copyOf(combinators);
        this.raw = raw;
    }

    public boolean isUniversal() { return segments.isEmpty(); }

    public int specificity() {
        int s = 0;
        for (Segment seg : segments) s += seg.specificity();
        return s;
    }

    public boolean matches(TesseraNode node, Deque<TesseraNode> ancestors) {
        if (segments.isEmpty()) return true;
        Segment last = segments.get(segments.size() - 1);
        if (!last.matches(node)) return false;
        if (segments.size() == 1) return true;

        // Walk ancestors from closest to farthest, matching segments right-to-left.
        // For each segment[i] (from N-2 down to 0):
        //   - DESCENDANT combinator: advance iterator until an ancestor matches (or exhaust).
        //   - CHILD combinator: the very next ancestor must match (exactly one step).
        java.util.Iterator<TesseraNode> it = ancestors.iterator();
        for (int segIdx = segments.size() - 2; segIdx >= 0; segIdx--) {
            // combinators[segIdx] is the combinator between segment[segIdx] and segment[segIdx+1]
            Combinator comb = combinators.get(segIdx);
            Segment seg = segments.get(segIdx);
            if (comb == Combinator.CHILD) {
                // The very next ancestor must be the direct parent.
                if (!it.hasNext()) return false;
                TesseraNode anc = it.next();
                if (!seg.matches(anc)) return false;
            } else {
                // DESCENDANT: scan forward until we find a matching ancestor.
                boolean found = false;
                while (it.hasNext()) {
                    TesseraNode anc = it.next();
                    if (seg.matches(anc)) { found = true; break; }
                }
                if (!found) return false;
            }
        }
        return true;
    }

    public static TesseraSelector parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        List<String> tokens = new ArrayList<>();
        List<Combinator> combs = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean nextIsChild = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '>') {
                if (cur.length() > 0) { tokens.add(cur.toString()); cur.setLength(0); }
                // If a DESCENDANT combinator was just added for the previous token
                // (from the space before this '>'), upgrade it to CHILD in place.
                if (!combs.isEmpty() && combs.size() == tokens.size()) {
                    combs.set(combs.size() - 1, Combinator.CHILD);
                } else {
                    // No combinator added yet ('>'' immediately follows the token, no space)
                    nextIsChild = true;
                }
            } else if (Character.isWhitespace(c)) {
                if (cur.length() > 0) {
                    tokens.add(cur.toString()); cur.setLength(0);
                    combs.add(nextIsChild ? Combinator.CHILD : Combinator.DESCENDANT);
                    nextIsChild = false;
                }
            } else {
                if (nextIsChild && combs.size() < tokens.size()) {
                    // '>' appeared immediately after previous token with no space before next
                    combs.add(Combinator.CHILD);
                    nextIsChild = false;
                }
                cur.append(c);
            }
        }
        if (cur.length() > 0) tokens.add(cur.toString());
        // Guard: empty token list → nothing to match; avoid underflow in the while below.
        if (tokens.isEmpty()) return null;
        while (combs.size() > tokens.size() - 1) combs.remove(combs.size() - 1);

        List<Segment> segs = new ArrayList<>();
        for (String tok : tokens) segs.add(parseSegment(tok));
        if (segs.isEmpty()) return null;
        return new TesseraSelector(segs, combs, raw);
    }

    private static final java.util.regex.Pattern NTH_CHILD =
            java.util.regex.Pattern.compile(":nth-child\\(([^)]+)\\)");

    private static Segment parseSegment(String tok) {
        // Extract :nth-child(...) before splitting on dots
        String nthChild = null;
        java.util.regex.Matcher nthMatcher = NTH_CHILD.matcher(tok);
        if (nthMatcher.find()) {
            nthChild = nthMatcher.group(1).trim();
            tok = tok.substring(0, nthMatcher.start()) + tok.substring(nthMatcher.end());
        }

        String tag = null;
        List<String> classes = new ArrayList<>();
        int firstDot = tok.indexOf('.');
        String tagPart;
        String rest;
        if (firstDot < 0) { tagPart = tok; rest = ""; }
        else { tagPart = tok.substring(0, firstDot); rest = tok.substring(firstDot); }
        if (!tagPart.isEmpty() && !tagPart.equals("*")) tag = tagPart;
        if (!rest.isEmpty()) {
            for (String c : rest.split("\\.")) {
                if (!c.isEmpty()) classes.add(c);
            }
        }
        return new Segment(tag, classes, nthChild);
    }

    @Override public String toString() { return "TesseraSelector(" + raw + ")"; }
}
