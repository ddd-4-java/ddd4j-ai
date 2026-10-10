package io.ddd4j.ai.extension.document;

import java.beans.ConstructorProperties;

import java.util.Objects;

import java.util.List;

/**
 * 文档表格：表头行 + 数据行（单元格为字符串）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class DocumentTable {

    private static final long serialVersionUID = 0L;

    private final List<List<String>> headers;

    private final List<List<String>> rows;

    /**
 * @param headers 表头（多行表头按行排列）
 * @param rows 数据行
 */

    @ConstructorProperties({ "headers", "rows" })
    public DocumentTable(List<List<String>> headers, List<List<String>> rows) {
        headers = headers == null ? List.of() : headers.stream().map(List::copyOf).toList();
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
        this.headers = headers;
        this.rows = rows;
    }

    public List<List<String>> headers() {
        return headers;
    }

    public List<List<String>> rows() {
        return rows;
    }

    public List<List<String>> getHeaders() {
        return headers();
    }

    public List<List<String>> getRows() {
        return rows();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        DocumentTable other = (DocumentTable) obj;
        return Objects.equals(this.headers, other.headers) && Objects.equals(this.rows, other.rows);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(headers);
        result = 31 * result + Objects.hashCode(rows);
        return result;
    }

    @Override
    public String toString() {
        return "DocumentTable[headers=" + headers + ", rows=" + rows + "]";
    }
}
