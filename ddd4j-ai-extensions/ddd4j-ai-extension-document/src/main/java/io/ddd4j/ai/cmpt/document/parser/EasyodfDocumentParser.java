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
 * easyodf 委托解析器（高质量优先）：ODF/OFD 1:1 结构还原。
 * <p>契约占位：待 {@code io.github.easy4j:easyodf-core} 发布后激活委托实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ConditionalOnClass(name = "io.github.easy4j.odf.core.convert.OdfToMarkdownConverter")
public class EasyodfDocumentParser implements DocumentParser {

    @Override
    public MediaType supports() {
        return MediaType.UNKNOWN; // ODF/OFD 后缀未纳入 MediaType 枚举，按 UNKNOWN 兜底路由
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Document parse(File file) throws Exception {
        Objects.requireNonNull(file, "file must not be null");
        throw new UnsupportedOperationException(
                "easyodf delegation pending; ensure io.github.easy4j:easyodf-core is on classpath");
    }

    @Override
    public Document parse(InputStream in, String filename) throws Exception {
        Objects.requireNonNull(in, "in must not be null");
        File tmp = File.createTempFile("dai-eodf-", "-" + (filename == null ? ".odf" : filename));
        try {
            Files.copy(in, tmp.toPath());
            return parse(tmp);
        } finally {
            tmp.delete();
        }
    }
}
