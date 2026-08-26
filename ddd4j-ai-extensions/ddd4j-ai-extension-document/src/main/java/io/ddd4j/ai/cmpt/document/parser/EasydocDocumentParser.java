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
 * easydoc 委托解析器（高质量优先）：DOCX 1:1 结构还原。
 * <p>契约占位：待 {@code io.github.easy4j:easydoc-core} 发布后激活委托实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ConditionalOnClass(name = "io.github.easy4j.doc.xhtml.markdown.DocxToMarkdownConverter")
public class EasydocDocumentParser implements DocumentParser {

    @Override
    public MediaType supports() {
        return MediaType.DOCX;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Document parse(File file) throws Exception {
        Objects.requireNonNull(file, "file must not be null");
        throw new UnsupportedOperationException(
                "easydoc delegation pending; ensure io.github.easy4j:easydoc-core is on classpath");
    }

    @Override
    public Document parse(InputStream in, String filename) throws Exception {
        Objects.requireNonNull(in, "in must not be null");
        File tmp = File.createTempFile("dai-esd-", "-" + (filename == null ? ".docx" : filename));
        try {
            Files.copy(in, tmp.toPath());
            return parse(tmp);
        } finally {
            tmp.delete();
        }
    }
}
