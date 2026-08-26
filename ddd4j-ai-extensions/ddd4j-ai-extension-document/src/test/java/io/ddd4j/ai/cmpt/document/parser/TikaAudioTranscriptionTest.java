package io.ddd4j.ai.cmpt.document.parser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.ddd4j.ai.cmpt.asr.service.AsrService;
import io.ddd4j.ai.cmpt.asr.service.AudioFormat;
import io.ddd4j.ai.cmpt.document.Document;
import io.ddd4j.ai.cmpt.document.properties.DocumentProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link TikaDocumentParser} 音频语音转写测试（mock AsrService，不依赖 whisper native）：
 * 对齐 markitdown 的音频转写能力，Tika 底座负责音频元数据、asr 组件负责语音文本。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TikaAudioTranscriptionTest {

    /** 最小 WAV 头 + 少量 16-bit PCM 采样（16kHz mono）。 */
    private static byte[] wavBytes() {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] header = {
                0x52, 0x49, 0x46, 0x46, 36, 0, 0, 0, 0x57, 0x41, 0x56, 0x45,
                0x66, 0x6d, 0x74, 0x20, 16, 0, 0, 0, 1, 0, 1, 0,
                0x40, 0x1F, 0, 0, (byte) 0x80, 0x3E, 0, 0, 2, 0, 16, 0,
                0x64, 0x61, 0x74, 0x61, 8, 0, 0, 0,
                0, 0, 0, 0, (byte) 0xFF, 0x7F, 0, 0, (byte) 0xFF, 0x7F, 0, 0
        };
        out.write(header, 0, header.length);
        return out.toByteArray();
    }

    @Test
    void audio_withAsrService_appendsTranscription(@TempDir Path tmp) throws Exception {
        AsrService asrService = mock(AsrService.class);
        when(asrService.transcribe(any(byte[].class), any(AudioFormat.class))).thenReturn("spoken text");
        TikaDocumentParser parser = new TikaDocumentParser(new DocumentProperties(), asrService);
        File file = tmp.resolve("speech.wav").toFile();
        Files.write(file.toPath(), wavBytes());

        Document document = parser.parse(file);

        assertThat(document.sections()).extracting(io.ddd4j.ai.cmpt.document.DocumentSection::title)
                .contains("Transcription");
        assertThat(document.fullMarkdown()).contains("spoken text");
    }

    @Test
    void audio_withoutAsrService_metadataOnly(@TempDir Path tmp) throws Exception {
        TikaDocumentParser parser = new TikaDocumentParser(new DocumentProperties(), null);
        File file = tmp.resolve("speech.wav").toFile();
        Files.write(file.toPath(), wavBytes());

        Document document = parser.parse(file);

        assertThat(document.sections()).extracting(io.ddd4j.ai.cmpt.document.DocumentSection::title)
                .doesNotContain("Transcription");
    }

    @Test
    void audio_asrFailure_fallsBackToMetadataOnly(@TempDir Path tmp) throws Exception {
        AsrService asrService = mock(AsrService.class);
        when(asrService.transcribe(any(byte[].class), any(AudioFormat.class)))
                .thenThrow(new IllegalStateException("engine down"));
        TikaDocumentParser parser = new TikaDocumentParser(new DocumentProperties(), asrService);
        File file = tmp.resolve("speech.wav").toFile();
        Files.write(file.toPath(), wavBytes());

        Document document = parser.parse(file);

        assertThat(document.source()).isEqualTo(io.ddd4j.ai.cmpt.document.SourceType.TIKA_FALLBACK);
        assertThat(document.sections()).extracting(io.ddd4j.ai.cmpt.document.DocumentSection::title)
                .doesNotContain("Transcription");
    }
}
