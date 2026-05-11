package com.tesseraui;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TesseraHtmlParser {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final String src;
    private int pos;

    private TesseraHtmlParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static TesseraNode parse(String html) {
        return new TesseraHtmlParser(html.trim()).parseDocument();
    }

    public static TesseraNode parse(InputStream is) throws IOException {
        return parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
    }

    private TesseraNode parseDocument() {
        skipWhitespace();
        while (pos < src.length() && peek("<") && (peekAt(1) == '?' || peekAt(1) == '!')) {
            if (peek("<!--")) {
                pos += 4; // skip past <!--
                consumeUntil("-->");
                if (src.startsWith("-->", pos)) pos += 3;
            } else {
                consumeUntil('>');
                pos++;
            }
            skipWhitespace();
        }
        return parseElement();
    }

    private TesseraNode parseElement() {
        if (!peek("<") || peek("</")) return null;
        pos++;
        String tag = parseName().toLowerCase(java.util.Locale.ROOT);

        Map<String, String> attrs = parseAttrs();
        skipWhitespace();

        if (peek("/>")) {
            pos += 2;
            return makeNode(tag, attrs, List.of(), "");
        }

        if (!peek(">")) {
            consumeUntil('>');
            pos++;
            return null;
        }
        pos++;

        List<TesseraNode> children = new ArrayList<>();
        StringBuilder leadText = new StringBuilder(); // text before the first child element
        boolean seenElement = false;

        while (pos < src.length()) {
            skipWhitespace();
            if (peek("</" + tag)) {
                consumeUntil('>');
                pos++;
                break;
            }
            if (peek("</")) break;
            if (peek("<!--")) {
                pos += 4; // skip past <!--
                consumeUntil("-->");
                if (src.startsWith("-->", pos)) pos += 3;
                continue;
            }
            if (peek("<")) {
                TesseraNode child = parseElement();
                if (child != null) {
                    children.add(child);
                    seenElement = true;
                }
            } else {
                int start = pos;
                while (pos < src.length() && src.charAt(pos) != '<') pos++;
                String chunk = decodeEntities(src.substring(start, pos));
                if (!seenElement) {
                    // Text before the first child element — stored in node.text() as before.
                    leadText.append(chunk);
                } else {
                    // Tail text after a child element — emitted as a #text child to preserve
                    // inter-element ordering (required for correct inline flow rendering).
                    String trimmed = chunk.strip();
                    if (!trimmed.isEmpty()) {
                        children.add(TesseraNode.textNode(trimmed));
                    }
                }
            }
        }

        return makeNode(tag, attrs, children, leadText.toString().trim());
    }

    private TesseraNode makeNode(String tag, Map<String, String> attrs, List<TesseraNode> children, String text) {
        if (!TesseraNode.KNOWN_TAGS.contains(tag)) {
            LOGGER.warn("[TesseraUI] Unknown HTML tag ignored: <{}>", tag);
            return null;
        }
        Map<String, String> decodedAttrs = new LinkedHashMap<>();
        attrs.forEach((k, v) -> decodedAttrs.put(k, decodeEntities(v)));
        return new TesseraNode(tag, decodedAttrs, children, text);
    }

    private static final Pattern ENTITY_HEX = Pattern.compile("&#x([0-9a-fA-F]+);");
    private static final Pattern ENTITY_DEC = Pattern.compile("&#(\\d+);");

    private static String decodeEntities(String s) {
        // Named entities
        s = s.replace("&amp;",  "&")
             .replace("&lt;",   "<")
             .replace("&gt;",   ">")
             .replace("&quot;", "\"")
             .replace("&apos;", "'")
             .replace("&nbsp;", " ");

        // Numeric hex entities: &#xHH; or &#xHHHH;
        Matcher mHex = ENTITY_HEX.matcher(s);
        StringBuffer sbHex = new StringBuffer();
        while (mHex.find()) {
            int codePoint = Integer.parseInt(mHex.group(1), 16);
            mHex.appendReplacement(sbHex, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
        }
        mHex.appendTail(sbHex);
        s = sbHex.toString();

        // Numeric decimal entities: &#DD;
        Matcher mDec = ENTITY_DEC.matcher(s);
        StringBuffer sbDec = new StringBuffer();
        while (mDec.find()) {
            int codePoint = Integer.parseInt(mDec.group(1), 10);
            mDec.appendReplacement(sbDec, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
        }
        mDec.appendTail(sbDec);
        return sbDec.toString();
    }

    private Map<String, String> parseAttrs() {
        Map<String, String> attrs = new LinkedHashMap<>();
        while (pos < src.length()) {
            skipWhitespace();
            if (peek(">") || peek("/>") || peek("<") || pos >= src.length()) break;
            String name = parseName();
            if (name.isEmpty()) { pos++; continue; }
            skipWhitespace();
            if (peek("=")) {
                pos++;
                skipWhitespace();
                attrs.put(name, parseAttrValue());
            } else {
                attrs.put(name, name);
            }
        }
        return attrs;
    }

    private String parseAttrValue() {
        if (pos >= src.length()) return "";
        char quote = src.charAt(pos);
        if (quote == '"' || quote == '\'') {
            pos++;
            int start = pos;
            while (pos < src.length() && src.charAt(pos) != quote) pos++;
            String value = src.substring(start, pos);
            if (pos < src.length()) pos++;
            return value;
        }
        int start = pos;
        while (pos < src.length() && !Character.isWhitespace(src.charAt(pos))
               && src.charAt(pos) != '>' && src.charAt(pos) != '/') pos++;
        return src.substring(start, pos);
    }

    private String parseName() {
        int start = pos;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ':') pos++;
            else break;
        }
        return src.substring(start, pos);
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private boolean peek(String s) { return src.startsWith(s, pos); }

    private char peekAt(int offset) {
        int idx = pos + offset;
        return idx < src.length() ? src.charAt(idx) : 0;
    }

    private void consumeUntil(char c) {
        while (pos < src.length() && src.charAt(pos) != c) pos++;
    }

    private void consumeUntil(String s) {
        while (pos < src.length() && !src.startsWith(s, pos)) pos++;
    }
}
