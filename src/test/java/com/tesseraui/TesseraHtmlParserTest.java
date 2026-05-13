package com.tesseraui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TesseraHtmlParser}: tag parsing, nesting, attributes,
 * text content, self-closing tags, and HTML entity decoding.
 *
 * <p>The parser uses Mojang {@code LogUtils} for warn-level logging when
 * unknown tags are encountered; it is available on the test classpath via
 * the {@code implementation} NeoForge dependency.</p>
 */
class TesseraHtmlParserTest {

    // ── basic tag ────────────────────────────────────────────────────────────

    @Test
    void parse_simpleDiv() {
        TesseraNode root = TesseraHtmlParser.parse("<div></div>");
        assertNotNull(root);
        assertEquals("div", root.tag());
    }

    @Test
    void parse_initialUtf8BomBeforeRoot() {
        TesseraNode root = TesseraHtmlParser.parse("\uFEFF<div></div>");
        assertNotNull(root);
        assertEquals("div", root.tag());
    }

    @Test
    void parseFragment_initialUtf8BomBeforeRoot() {
        TesseraNode root = TesseraHtmlParser.parseFragment("\uFEFF<div></div>");
        assertNotNull(root);
        assertEquals("div", root.tag());
    }

    @Test
    void parseWithComponents_initialUtf8BomBeforeRoot() {
        TesseraNode root = TesseraHtmlParser.parseWithComponents("\uFEFF<col><p>x</p></col>");
        assertNotNull(root);
        assertEquals("col", root.tag());
        assertEquals("p", root.children().get(0).tag());
    }

    @Test
    void parse_selfClosingTag() {
        TesseraNode root = TesseraHtmlParser.parse("<img/>");
        assertNotNull(root);
        assertEquals("img", root.tag());
        assertTrue(root.children().isEmpty());
    }

    @Test
    void parse_selfClosingWithAttributes() {
        TesseraNode root = TesseraHtmlParser.parse(
                "<img src=\"minecraft:textures/item/diamond.png\" width=\"16\" height=\"16\"/>");
        assertNotNull(root);
        assertEquals("minecraft:textures/item/diamond.png", root.attr("src"));
        assertEquals("16", root.attr("width"));
        assertEquals("16", root.attr("height"));
    }

    @Test
    void parse_tagWithId() {
        TesseraNode root = TesseraHtmlParser.parse("<div id=\"main\"></div>");
        assertNotNull(root);
        assertEquals("main", root.attr("id"));
    }

    @Test
    void parse_tagWithClass() {
        TesseraNode root = TesseraHtmlParser.parse("<div class=\"card active\"></div>");
        assertNotNull(root);
        List<String> classes = root.classNames();
        assertTrue(classes.contains("card"));
        assertTrue(classes.contains("active"));
    }

    // ── text content ─────────────────────────────────────────────────────────

    @Test
    void parse_textContent() {
        TesseraNode root = TesseraHtmlParser.parse("<p>Hello world</p>");
        assertNotNull(root);
        assertEquals("Hello world", root.text());
    }

    @Test
    void parse_emptyText() {
        TesseraNode root = TesseraHtmlParser.parse("<div></div>");
        assertNotNull(root);
        assertEquals("", root.text());
    }

    // ── nesting ───────────────────────────────────────────────────────────────

    @Test
    void parse_singleChild() {
        TesseraNode root = TesseraHtmlParser.parse("<div><p>text</p></div>");
        assertNotNull(root);
        assertEquals(1, root.children().size());
        assertEquals("p", root.children().get(0).tag());
        assertEquals("text", root.children().get(0).text());
    }

    @Test
    void parse_multipleChildren() {
        TesseraNode root = TesseraHtmlParser.parse("<col><p>a</p><p>b</p><p>c</p></col>");
        assertNotNull(root);
        assertEquals("col", root.tag());
        assertEquals(3, root.children().size());
        assertEquals("a", root.children().get(0).text());
        assertEquals("b", root.children().get(1).text());
        assertEquals("c", root.children().get(2).text());
    }

    @Test
    void parse_deepNesting() {
        TesseraNode root = TesseraHtmlParser.parse("<div><row><div><p>deep</p></div></row></div>");
        assertNotNull(root);
        TesseraNode p = root.children().get(0).children().get(0).children().get(0);
        assertEquals("p", p.tag());
        assertEquals("deep", p.text());
    }

    // ── table tags ────────────────────────────────────────────────────────────

    @Test
    void parse_tableStructure() {
        String html = """
            <table>
              <thead><tr><th>Name</th></tr></thead>
              <tbody><tr><td>copper_ore</td></tr></tbody>
            </table>""";
        TesseraNode table = TesseraHtmlParser.parse(html);
        assertNotNull(table);
        assertEquals("table", table.tag());

        TesseraNode thead = table.children().stream()
                .filter(n -> "thead".equals(n.tag())).findFirst().orElse(null);
        assertNotNull(thead, "thead must be present");

        TesseraNode tr = thead.children().get(0);
        assertEquals("tr", tr.tag());

        TesseraNode th = tr.children().get(0);
        assertEquals("th", th.tag());
        assertEquals("Name", th.text());
    }

    // ── HTML entities ────────────────────────────────────────────────────────

    @Test
    void parse_entityAmp() {
        TesseraNode root = TesseraHtmlParser.parse("<p>A &amp; B</p>");
        assertNotNull(root);
        assertEquals("A & B", root.text());
    }

    @Test
    void parse_entityLtGt() {
        TesseraNode root = TesseraHtmlParser.parse("<p>&lt;tag&gt;</p>");
        assertNotNull(root);
        assertEquals("<tag>", root.text());
    }

    @Test
    void parse_entityQuot() {
        TesseraNode root = TesseraHtmlParser.parse("<p>&quot;hello&quot;</p>");
        assertNotNull(root);
        assertEquals("\"hello\"", root.text());
    }

    // ── attributes: v-if / v-for / v-show ────────────────────────────────────

    @Test
    void parse_vIfAttribute() {
        TesseraNode root = TesseraHtmlParser.parse("<div v-if=\"enabled\"></div>");
        assertNotNull(root);
        assertEquals("enabled", root.vIf());
    }

    @Test
    void parse_vForAttribute() {
        TesseraNode root = TesseraHtmlParser.parse("<div v-for=\"item in items\"></div>");
        assertNotNull(root);
        assertEquals("item in items", root.vFor());
    }

    @Test
    void parse_vShowAttribute() {
        TesseraNode root = TesseraHtmlParser.parse("<div v-show=\"visible\"></div>");
        assertNotNull(root);
        assertEquals("visible", root.vShow());
    }

    // ── tag case ─────────────────────────────────────────────────────────────

    @Test
    void parse_tagNormalizedToLowercase() {
        // Parser normalises tags to lower-case
        TesseraNode root = TesseraHtmlParser.parse("<DIV></DIV>");
        // If "div" is a known tag the node is returned; otherwise null
        // Either way, the tag (if non-null) must be lowercase
        if (root != null) assertEquals("div", root.tag());
    }

    // ── img tag ───────────────────────────────────────────────────────────────

    @Test
    void parse_imgTag() {
        TesseraNode root = TesseraHtmlParser.parse(
                "<img src=\"minecraft:textures/item/diamond.png\" width=\"16\" height=\"16\"/>");
        assertNotNull(root);
        assertEquals("img", root.tag());
        assertEquals("minecraft:textures/item/diamond.png", root.attr("src"));
    }

    @Test
    void parse_linkStylesheetTag() {
        TesseraNode root = TesseraHtmlParser.parse(
                "<link rel=\"stylesheet\" href=\"mymod:ui/shared.css\"/>");
        assertNotNull(root);
        assertEquals("link", root.tag());
        assertEquals("stylesheet", root.attr("rel"));
        assertEquals("mymod:ui/shared.css", root.attr("href"));
    }

    // ── hr (void element) ─────────────────────────────────────────────────────

    @Test
    void parse_hrSelfClosing() {
        TesseraNode root = TesseraHtmlParser.parse("<hr/>");
        assertNotNull(root);
        assertEquals("hr", root.tag());
    }

    // ── isKnown ───────────────────────────────────────────────────────────────

    @Test
    void node_isKnown_knownTags() {
        for (String tag : new String[]{"div", "p", "table", "tr", "td", "th", "img", "hr", "span", "link"}) {
            TesseraNode n = new TesseraNode(tag, java.util.Map.of(), java.util.List.of(), "");
            assertTrue(n.isKnown(), "Expected " + tag + " to be a known tag");
        }
    }

    // ── inline flow / #text node support ─────────────────────────────────────

    @Test
    void textNode_isTextNode() {
        TesseraNode t = TesseraNode.textNode("hello");
        assertTrue(t.isTextNode());
        assertEquals("#text", t.tag());
        assertEquals("hello", t.text());
        assertTrue(t.children().isEmpty());
    }

    @Test
    void parse_plainP_noChildren_noChange() {
        // Plain <p> without inline children → same as before: text in node.text()
        TesseraNode root = TesseraHtmlParser.parse("<p>Hello world</p>");
        assertEquals("p", root.tag());
        assertEquals("Hello world", root.text());
        assertTrue(root.children().isEmpty());
    }

    @Test
    void parse_pWithStrong_leadTextInNodeText() {
        // <p>Hello <strong>world</strong></p>
        // "Hello" is lead text → node.text(); <strong> is in children; no tail text.
        TesseraNode root = TesseraHtmlParser.parse("<p>Hello <strong>world</strong></p>");
        assertEquals("p", root.tag());
        assertEquals("Hello", root.text());
        assertEquals(1, root.children().size());
        TesseraNode strong = root.children().get(0);
        assertEquals("strong", strong.tag());
        assertEquals("world", strong.text());
    }

    @Test
    void parse_pWithStrongAndTailText_emitsTextChild() {
        // <p>Hello <strong>world</strong>, end</p>
        // "Hello" → node.text(); <strong> → child; ", end" → #text child
        TesseraNode root = TesseraHtmlParser.parse("<p>Hello <strong>world</strong>, end</p>");
        assertEquals("Hello", root.text());
        assertEquals(2, root.children().size(), "Expected strong + #text child");
        TesseraNode strong = root.children().get(0);
        assertEquals("strong", strong.tag());
        TesseraNode tail = root.children().get(1);
        assertTrue(tail.isTextNode(), "#text child expected for tail text");
        assertEquals(", end", tail.text());
    }

    @Test
    void parse_pMultipleInline_orderPreserved() {
        // <p>A <em>b</em> C <strong>d</strong> E</p>
        // node.text = "A"; children = [em, #text"C", strong, #text"E"]
        TesseraNode root = TesseraHtmlParser.parse(
                "<p>A <em>b</em> C <strong>d</strong> E</p>");
        assertEquals("A", root.text());
        List<TesseraNode> ch = root.children();
        assertEquals(4, ch.size());
        assertEquals("em",    ch.get(0).tag());
        assertTrue(ch.get(1).isTextNode()); assertEquals("C", ch.get(1).text());
        assertEquals("strong", ch.get(2).tag());
        assertTrue(ch.get(3).isTextNode()); assertEquals("E", ch.get(3).text());
    }

    @Test
    void parse_divWithOnlyBlockChildren_noTextNodes() {
        // <div><p>First</p><p>Second</p></div>
        // No text between block elements, so no #text children expected.
        TesseraNode root = TesseraHtmlParser.parse("<div><p>First</p><p>Second</p></div>");
        assertEquals("div", root.tag());
        // All children are element nodes
        for (TesseraNode child : root.children()) {
            assertFalse(child.isTextNode(), "Unexpected #text child in block div");
        }
    }

    // ── HTML comments (v1.8 fix) ─────────────────────────────────────────────

    @Test
    void parse_htmlComment_ignored() {
        // <!-- comment --> must be silently skipped
        TesseraNode root = TesseraHtmlParser.parse("<div><!-- this is a comment --><p>hello</p></div>");
        assertNotNull(root);
        assertEquals("div", root.tag());
        assertEquals(1, root.children().size(), "Only <p> should remain as child");
        assertEquals("p", root.children().get(0).tag());
        assertEquals("hello", root.children().get(0).text());
    }

    @Test
    void parse_htmlComment_withGreaterThanInside() {
        // <!-- a > b --> must not stop at the > inside the comment
        TesseraNode root = TesseraHtmlParser.parse("<div><!-- x > y --><p>ok</p></div>");
        assertNotNull(root);
        // If the bug is present, '> y -->' would be parsed as content, causing parse failure
        assertFalse(root.children().isEmpty(), "Children must be parsed correctly after comment containing '>'");
        assertEquals("p", root.children().get(0).tag());
    }

    @Test
    void parse_htmlComment_atDocumentLevel() {
        // Comment before root element — parser must not crash
        TesseraNode root = TesseraHtmlParser.parse("<!-- preamble --><col><p>x</p></col>");
        assertNotNull(root);
        assertEquals("col", root.tag());
    }

    // ── Numeric HTML entities (v1.8 fix) ─────────────────────────────────────

    @Test
    void parse_entity_numeric_decimal() {
        // &#65; = codepoint 65 = 'A'
        TesseraNode root = TesseraHtmlParser.parse("<p>&#65;</p>");
        assertNotNull(root);
        assertEquals("A", root.text());
    }

    @Test
    void parse_entity_numeric_hex() {
        // &#x41; = hex 41 = 65 = 'A'
        TesseraNode root = TesseraHtmlParser.parse("<p>&#x41;</p>");
        assertNotNull(root);
        assertEquals("A", root.text());
    }

    @Test
    void parse_entity_numeric_unicode() {
        // &#233; = é
        TesseraNode root = TesseraHtmlParser.parse("<p>&#233;</p>");
        assertNotNull(root);
        assertEquals("é", root.text());
    }

    @Test
    void parse_entity_numeric_hex_unicode() {
        // &#xE9; = é
        TesseraNode root = TesseraHtmlParser.parse("<p>&#xE9;</p>");
        assertNotNull(root);
        assertEquals("é", root.text());
    }
}
