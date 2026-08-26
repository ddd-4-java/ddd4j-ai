package io.ddd4j.ai.cmpt.document.parser;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import io.ddd4j.ai.cmpt.document.Document;
import io.ddd4j.ai.cmpt.document.DocumentParser;
import io.ddd4j.ai.cmpt.document.MediaType;

/**
 * easypdf 委托解析器（高质量优先）：PDF 1:1 结构还原。
 * <p>契约占位：待 {@code io.github.easy4j:easypdf-core} 发布后激活委托实现；
 * 当前 {@link #parse} 抛 {@link UnsupportedOperationException} 触发门面降级到通用兜底。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ConditionalOnClass(name = "io.github.easy4j.pdf.core.convert.HtmlPdfConverter")
public class EasypdfDocumentParser implements DocumentParser {

    @Override
    public MediaType supports() {
        return MediaType.PDF;
    }

    @Override
    public int order() {
        return 10; // 高于通用兜底（0）
    }

    @Override
    public Document parse(File file) throws Exception {
        Objects.requireNonNull(file, "file must not be null");
        throw new UnsupportedOperationException(
                "easypdf delegation pending; ensure io.github.easy4j:easypdf-core is on classpath");
    }

    @Override
    public Document parse(InputStream in, String filename) throws Exception {
        Objects.requireNonNull(in, "in must not be null");
        File tmp = File.createTempFile("dai-eps-", "-" + (filename == null ? ".pdf" : filename));
        try {
            Files.copy(in, tmp.toPath());
            return parse(tmp);
        } finally {
            tmp.delete();
        }
    }
}
