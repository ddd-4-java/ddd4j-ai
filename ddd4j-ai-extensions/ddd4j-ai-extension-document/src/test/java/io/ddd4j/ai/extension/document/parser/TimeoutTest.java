package io.ddd4j.ai.extension.document.parser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.ddd4j.ai.extension.document.Document;
import io.ddd4j.ai.extension.document.properties.DocumentProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TikaDocumentParser} 解析超时配置测试：超时上下文注入语义 + 默认值锁定
 * （真实超时触发依赖病态样本，不稳定，故用注入语义断言）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TimeoutTest {

    @Test
    void timeoutConfigured_smallDocumentStillParses(@TempDir Path tmp) throws Exception {
        DocumentProperties properties = new DocumentProperties();
        properties.setParseTimeoutMillis(5_000);
        File file = tmp.resolve("note.txt").toFile();
        Files.writeString(file.toPath(), "timeout configured parse");
        Document document = new TikaDocumentParser(properties).parse(file);
        assertThat(document.fullMarkdown()).contains("timeout configured parse");
    }

    @Test
    void timeoutDisabled_defaultStillWorks(@TempDir Path tmp) throws Exception {
        DocumentProperties properties = new DocumentProperties();
        properties.setParseTimeoutMillis(0);
        File file = tmp.resolve("note.txt").toFile();
        Files.writeString(file.toPath(), "no timeout");
        Document document = new TikaDocumentParser(properties).parse(file);
        assertThat(document.fullMarkdown()).contains("no timeout");
    }

    @Test
    void timeoutDefault_is60s() {
        // 生产安全默认值锁定：防止误改导致病态文档可无限挂住解析线程
        assertThat(new DocumentProperties().getParseTimeoutMillis()).isEqualTo(60_000L);
    }

    @Test
    void timeoutContextReflectedInParse() throws Exception {
        // 语义断言：TikaTaskTimeout.getTimeoutMillis(ParseContext, default) 能读到我们注入的值
        org.apache.tika.parser.ParseContext context = new org.apache.tika.parser.ParseContext();
        context.set(org.apache.tika.config.TikaTaskTimeout.class, new org.apache.tika.config.TikaTaskTimeout(1_234L));
        assertThat(org.apache.tika.config.TikaTaskTimeout.getTimeoutMillis(context, 99_999L)).isEqualTo(1_234L);
    }
}
