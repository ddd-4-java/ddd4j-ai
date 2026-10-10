package io.ddd4j.ai.extension.document;

import java.beans.ConstructorProperties;

import java.util.Objects;

/**
 * 文档图片：替代文本 + 内容地址（base64 data URL 或 URL）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class DocumentImage {

    private static final long serialVersionUID = 0L;

    private final String alt;

    private final String src;

    /**
 * @param alt 替代文本
 * @param src 图片地址
 */

    @ConstructorProperties({ "alt", "src" })
    public DocumentImage(String alt, String src) {
        this.alt = alt;
        this.src = src;
    }

    public String alt() {
        return alt;
    }

    public String src() {
        return src;
    }

    public String getAlt() {
        return alt();
    }

    public String getSrc() {
        return src();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        DocumentImage other = (DocumentImage) obj;
        return Objects.equals(this.alt, other.alt) && Objects.equals(this.src, other.src);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(alt);
        result = 31 * result + Objects.hashCode(src);
        return result;
    }

    @Override
    public String toString() {
        return "DocumentImage[alt=" + alt + ", src=" + src + "]";
    }
}
