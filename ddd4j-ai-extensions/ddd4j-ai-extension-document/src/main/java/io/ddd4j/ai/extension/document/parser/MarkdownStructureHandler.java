package io.ddd4j.ai.extension.document.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import io.ddd4j.ai.extension.document.DocumentImage;
import io.ddd4j.ai.extension.document.DocumentSection;
import io.ddd4j.ai.extension.document.DocumentTable;

/**
 * Tika SAX 事件 → {@link Document} 结构化字段的监听器（对齐 markitdown 的结构化逻辑）：
 * <ul>
 *   <li>{@code h1..h6} 标题 → {@link DocumentSection} 层级树</li>
 *   <li>{@code table/thead/tr/td/th} → {@link DocumentTable}（表头/数据行）</li>
 *   <li>{@code img} → {@link DocumentImage}（alt/src）</li>
 * </ul>
 * 与 {@link org.apache.tika.sax.ToMarkdownContentHandler} 经 TeeContentHandler 并行消费同一事件流。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
final class MarkdownStructureHandler extends DefaultHandler {

    private final List<MutableSection> roots = new ArrayList<>();
    private final Deque<MutableSection> stack = new ArrayDeque<>();
    private final List<DocumentTable> tables = new ArrayList<>();
    private final List<DocumentImage> images = new ArrayList<>();

    private final StringBuilder text = new StringBuilder();
    private String headingTag;

    private boolean inTable;
    private boolean inHeaderRow;
    private final List<List<String>> headerRows = new ArrayList<>();
    private final List<List<String>> dataRows = new ArrayList<>();
    private List<String> row;
    private StringBuilder cell;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        String name = localName.toLowerCase(Locale.ROOT);
        if (isHeading(name)) {
            flushText();
            headingTag = name;
            return;
        }
        if ("table".equals(name)) {
            flushText();
            inTable = true;
            headerRows.clear();
            dataRows.clear();
            return;
        }
        if ("tr".equals(name)) {
            row = new ArrayList<>();
            return;
        }
        if ("td".equals(name) || "th".equals(name)) {
            cell = new StringBuilder();
            return;
        }
        if ("img".equals(name)) {
            flushText();
            images.add(new DocumentImage(attributes.getValue("alt"), attributes.getValue("src")));
            return;
        }
        if ("thead".equals(name)) {
            inHeaderRow = true;
        }
        flushText();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        text.append(ch, start, length);
        if (cell != null) {
            cell.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String name = localName.toLowerCase(Locale.ROOT);
        if (isHeading(name)) {
            String title = text.toString().strip();
            text.setLength(0);
            pushSection(title, headingLevel(name));
            headingTag = null;
            return;
        }
        if ("td".equals(name) || "th".equals(name)) {
            if (row != null) {
                row.add(cell == null ? "" : cell.toString().strip());
            }
            cell = null;
            return;
        }
        if ("tr".equals(name)) {
            if (row != null) {
                if (inHeaderRow) {
                    headerRows.add(row);
                } else {
                    dataRows.add(row);
                }
                row = null;
            }
            return;
        }
        if ("thead".equals(name)) {
            inHeaderRow = false;
            return;
        }
        if ("table".equals(name)) {
            tables.add(new DocumentTable(List.copyOf(headerRows), List.copyOf(dataRows)));
            inTable = false;
            inHeaderRow = false;
            return;
        }
        flushText();
    }

    @Override
    public void endDocument() throws SAXException {
        flushText();
    }

    List<DocumentSection> sections() {
        return roots.stream().map(MutableSection::toImmutable).toList();
    }

    List<DocumentTable> tables() {
        return tables;
    }

    List<DocumentImage> images() {
        return images;
    }

    private void pushSection(String title, int level) {
        while (!stack.isEmpty() && stack.peek().level >= level) {
            stack.pop();
        }
        MutableSection section = new MutableSection(title, level);
        if (stack.isEmpty()) {
            roots.add(section);
        } else {
            stack.peek().children.add(section);
        }
        stack.push(section);
    }

    private void flushText() {
        String content = text.toString().strip();
        text.setLength(0);
        if (content.isEmpty()) {
            return;
        }
        if (!stack.isEmpty()) {
            stack.peek().content.append(content).append('\n');
        }
    }

    private static boolean isHeading(String name) {
        return name.length() == 2 && name.charAt(0) == 'h'
                && name.charAt(1) >= '1' && name.charAt(1) <= '6';
    }

    private static int headingLevel(String name) {
        return name.charAt(1) - '0';
    }

    /** 构建期可变 section（DocumentSection 为不可变 record）。 */
    private static final class MutableSection {

        private final String title;
        private final int level;
        private final StringBuilder content = new StringBuilder();
        private final List<MutableSection> children = new ArrayList<>();

        private MutableSection(String title, int level) {
            this.title = title;
            this.level = level;
        }

        private DocumentSection toImmutable() {
            return new DocumentSection(title, level, content.toString().strip(),
                    children.stream().map(MutableSection::toImmutable).toList(),
                    List.of(), List.of());
        }
    }
}
