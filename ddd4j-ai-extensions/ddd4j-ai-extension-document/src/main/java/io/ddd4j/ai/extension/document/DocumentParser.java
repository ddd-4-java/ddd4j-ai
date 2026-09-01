package io.ddd4j.ai.extension.document;

import java.io.File;
import java.io.InputStream;

/**
 * 文档解析器 SPI：按媒体类型路由，{@code order()} 决定优先级（数值大者优先）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface DocumentParser {

    /**
     * 本解析器负责的媒体类型；{@link MediaType#UNKNOWN} 表示通用兜底（匹配任意类型）。
     */
    MediaType supports();

    /**
     * 优先级：数值大者优先尝试；默认 0（通用兜底）。
     */
    default int order() {
        return 0;
    }

    /**
     * 解析文件为统一文档模型。
     *
     * @throws Exception 解析失败 / 委托未就位（{@link UnsupportedOperationException} 触发降级）
     */
    Document parse(File file) throws Exception;

    /**
     * 解析输入流为统一文档模型（文件名用于媒体类型判定）。
     *
     * @throws Exception 解析失败 / 委托未就位
     */
    Document parse(InputStream in, String filename) throws Exception;
}
