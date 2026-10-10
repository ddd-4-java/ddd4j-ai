package io.ddd4j.ai.extension.document;

import java.util.List;

/**
 * 文档表格：表头行 + 数据行（单元格为字符串）。
 *
 * @param headers 表头（多行表头按行排列）
 * @param rows    数据行
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record DocumentTable(List<List<String>> headers, List<List<String>> rows) {

    /**
     * 规范构造器：表头/数据行逐行固化为不可变列表，{@code null} 归一为空列表。
     *
     * @param headers 表头（多行表头按行排列）
     * @param rows    数据行
     */
    public DocumentTable {
        headers = headers == null ? List.of() : headers.stream().map(List::copyOf).toList();
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
    }
}
