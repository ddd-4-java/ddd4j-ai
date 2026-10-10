# javadoc 中文补齐进度（feature/1.0.x / 2026-10-09）

- 基线盘点：136 个 src/main/java 文件，95 个存在缺口，合计 340 处
  （NO_COMMENT 234 / MISSING_PARAM 77 / MISSING_RETURN 29）
- 自查工具：target/javadoc_scan_v2.py（规则按 javadoc 17 `-Xdoclint:missing` 实测校准：
  public/protected 类型需注释；方法需注释 + 全部值形参 @param + 方法类型形参 @param + 非 void @return；
  record 规范构造器需注释 + 组件 @param；public/protected 字段与 enum 常量需注释；@throws 非强制）
- 批次规则：每批 ≤30 个文件，完成后追加记录（批号、文件清单、剩余量），防止中断丢失。

## 批次 0（盘点）
- 状态：完成
- 剩余量：95 文件 / 340 处

## 批次 1（文件 1-30）
- 状态：完成（30/30 文件经 target/javadoc_scan_v2.py 复扫为 0 缺口）
- 文件清单（30）：
  1. ddd4j-ai-core/.../AiRequest.java
  2. ddd4j-ai-core/.../AiResponse.java
  3. ddd4j-ai-extension-agent/.../agent/AgentExecutionException.java
  4. ddd4j-ai-extension-agent/.../agent/AgentScopeAgentAdapter.java
  5. ddd4j-ai-extension-agent/.../agent/SpringAiToolkitBuilder.java
  6. ddd4j-ai-extension-agent/.../autoconfigure/AgentAutoConfiguration.java
  7. ddd4j-ai-extension-agent/.../dispatch/AgentDispatchTask.java
  8. ddd4j-ai-extension-agent/.../dispatch/InMemoryAgentDispatchTaskRepository.java
  9. ddd4j-ai-extension-agent/.../dispatch/AgentPlanOrchestrator.java
  10. ddd4j-ai-extension-agent/.../properties/AgentProperties.java
  11. ddd4j-ai-extension-agent/.../service/AgentResult.java
  12. ddd4j-ai-extension-agent/.../service/AgentStep.java
  13. ddd4j-ai-extension-agent/.../service/AgentTask.java
  14. ddd4j-ai-extension-asr/.../autoconfigure/AsrAutoConfiguration.java
  15. ddd4j-ai-extension-asr/.../properties/AsrProperties.java
  16. ddd4j-ai-extension-asr/.../service/AudioFormat.java
  17. ddd4j-ai-extension-asr/.../service/impl/WavConverter.java
  18. ddd4j-ai-extension-asr/.../service/impl/WhisperCppAsrService.java
  19. ddd4j-ai-extension-chat/.../autoconfigure/ChatAutoConfiguration.java
  20. ddd4j-ai-extension-chat/.../properties/ChatProperties.java
  21. ddd4j-ai-extension-chat/.../service/impl/ChatClientAdapter.java
  22. ddd4j-ai-extension-document/.../Document.java
  23. ddd4j-ai-extension-document/.../DocumentReader.java
  24. ddd4j-ai-extension-document/.../DocumentSection.java
  25. ddd4j-ai-extension-document/.../DocumentTable.java
  26. ddd4j-ai-extension-document/.../DocumentTooLargeException.java
  27. ddd4j-ai-extension-document/.../MediaType.java
  28. ddd4j-ai-extension-document/.../autoconfigure/DocumentAutoConfiguration.java
  29. ddd4j-ai-extension-document/.../parser/EasydocDocumentParser.java
  30. ddd4j-ai-extension-document/.../parser/EasyexcelDocumentParser.java
- 复扫结果：全仓 136 文件中 65 文件仍有缺口，合计 244 处（批次 1 之前为 86 文件 / 319 处）
- 校准补充（javadoc 17 实测）：构造器 javadoc 无需 @return（扫描器已修正误报）；
  但构造器/record 规范构造器 javadoc 必须为每个值形参提供 @param，record 类级 @param 不能豁免构造器

## 批次 2（文件 1-30，排序剩余清单）
- 状态：完成（30/30 文件复扫 0 缺口）
- 文件清单（30）：
  1. ddd4j-ai-extension-document/.../parser/EasyodfDocumentParser.java
  2. ddd4j-ai-extension-document/.../parser/EasypdfDocumentParser.java
  3. ddd4j-ai-extension-document/.../parser/TikaDocumentParser.java
  4. ddd4j-ai-extension-document/.../properties/DocumentProperties.java
  5. ddd4j-ai-extension-embedding/.../autoconfigure/EmbeddingAutoConfiguration.java
  6. ddd4j-ai-extension-embedding/.../properties/EmbeddingProperties.java
  7. ddd4j-ai-extension-embedding/.../service/impl/EmbeddingModelAdapter.java
  8. ddd4j-ai-extension-flow/.../autoconfigure/FlowAutoConfiguration.java
  9. ddd4j-ai-extension-flow/.../service/FlowDefinition.java
  10. ddd4j-ai-extension-flow/.../service/FlowEdge.java
  11. ddd4j-ai-extension-flow/.../service/FlowNodeSpec.java
  12. ddd4j-ai-extension-flow/.../service/impl/GraphFlowService.java
  13. ddd4j-ai-extension-flow/.../properties/FlowProperties.java
  14. ddd4j-ai-extension-mcp/.../autoconfigure/McpAutoConfiguration.java
  15. ddd4j-ai-extension-mcp/.../service/ToolDefinition.java
  16. ddd4j-ai-extension-mcp/.../service/impl/SpringAiMcpToolProvider.java
  17. ddd4j-ai-extension-memory/.../autoconfigure/MemoryAutoConfiguration.java
  18. ddd4j-ai-extension-memory/.../properties/MemoryProperties.java
  19. ddd4j-ai-extension-memory/.../service/impl/WindowMemoryService.java
  20. ddd4j-ai-extension-ocr/.../autoconfigure/OcrAutoConfiguration.java
  21. ddd4j-ai-extension-ocr/.../properties/OcrProperties.java
  22. ddd4j-ai-extension-ocr/.../service/impl/CompositeOcrService.java
  23. ddd4j-ai-extension-ocr/.../service/impl/PdfBoxOcrService.java
  24. ddd4j-ai-extension-ocr/.../service/impl/TikaOcrService.java
  25. ddd4j-ai-extension-rag/.../autoconfigure/RagAutoConfiguration.java
  26. ddd4j-ai-extension-rag/.../properties/RagProperties.java
  27. ddd4j-ai-extension-rag/.../service/NoopReranker.java
  28. ddd4j-ai-extension-rag/.../service/impl/RagPipeline.java
  29. ddd4j-ai-extension-router/.../autoconfigure/RouterAutoConfiguration.java
  30. ddd4j-ai-extension-router/.../properties/RouterProperties.java
- 复扫结果：全仓 136 文件中 35 文件仍有缺口，合计 154 处
- 批次 3 范围（剩余清单 1-30）：router impls 4 + sst 11 + tts 3 + vectordb 3 + samples 9（AgentSample、ChatSample、EmbeddingSample、ChatMemorySseSample、DocUnderstandingSample、KnowledgeBaseSample、MultiModelRouterSample、OrchestrationSample、RagPipelineSample）
- 批次 4 范围（剩余清单 31-35）：SmartAgentSample、MemorySample、RagSample、SstSample、VectorDbSample

