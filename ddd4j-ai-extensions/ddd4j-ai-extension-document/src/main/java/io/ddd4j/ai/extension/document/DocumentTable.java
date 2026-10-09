package io.ddd4j.ai.extension.document;

import java.util.List;

/**
 * 文档表格：表头行 + 数据行（单元格为字符串）。
 *
 * @param headers 表头（多行表头按行排列）
 * @param rows    数据行
 * @author ddd4j
 */
public record DocumentTable(List<List<String>> headers, List<List<String>> rows) {

    /**
     * 紧凑构造器：表头与数据行做防御性复制，{@code null} 时以空列表替代。
     */
    public DocumentTable {
        headers = headers == null ? List.of() : headers.stream().map(List::copyOf).toList();
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
    }
}
