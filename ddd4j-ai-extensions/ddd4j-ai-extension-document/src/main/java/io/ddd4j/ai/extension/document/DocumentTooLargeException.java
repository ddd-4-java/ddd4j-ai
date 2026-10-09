package io.ddd4j.ai.extension.document;

/**
 * 文档超过配置的大小上限（{@code ddd4j.ai.document.max-file-size-bytes}）。
 * 上层可按类型捕获，与一般解析失败区分给出业务提示。
 *
 * @author ddd4j
 */
public class DocumentTooLargeException extends IllegalArgumentException {

    /**
     * 创建超限异常。
     *
     * @param message 超限说明
     */
    public DocumentTooLargeException(String message) {
        super(message);
    }
}
