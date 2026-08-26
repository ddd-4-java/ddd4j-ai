package io.ddd4j.ai.cmpt.document;

/**
 * 文档超过配置的大小上限（{@code ddd4j.ai.document.max-file-size-bytes}）。
 * 上层可按类型捕获，与一般解析失败区分给出业务提示。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class DocumentTooLargeException extends IllegalArgumentException {

    public DocumentTooLargeException(String message) {
        super(message);
    }
}
