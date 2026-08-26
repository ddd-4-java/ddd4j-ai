package io.ddd4j.ai.cmpt.document;

import java.util.List;

/**
 * 文档表格：表头行 + 数据行（单元格为字符串）。
 *
 * @param headers 表头（多行表头按行排列）
 * @param rows    数据行
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record DocumentTable(List<List<String>> headers, List<List<String>> rows) {

    public DocumentTable {
        headers = headers == null ? List.of() : headers.stream().map(List::copyOf).toList();
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
    }
}
