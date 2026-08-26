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
 * easyexcel 委托解析器（高质量优先）：XLSX 1:1 结构还原（表格保真）。
 * <p>契约占位：待 {@code io.github.easy4j:easyexcel-core} 发布后激活委托实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@ConditionalOnClass(name = "io.github.easy4j.excel.core.convert.XlsxToMarkdownConverter")
public class EasyexcelDocumentParser implements DocumentParser {

    @Override
    public MediaType supports() {
        return MediaType.XLSX;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Document parse(File file) throws Exception {
        Objects.requireNonNull(file, "file must not be null");
        throw new UnsupportedOperationException(
                "easyexcel delegation pending; ensure io.github.easy4j:easyexcel-core is on classpath");
    }

    @Override
    public Document parse(InputStream in, String filename) throws Exception {
        Objects.requireNonNull(in, "in must not be null");
        // 占位阶段不消费输入流：直接抛未就位，门面可重放到通用兜底
        throw new UnsupportedOperationException(
                "delegation pending; component jar not yet published");
    }
}
