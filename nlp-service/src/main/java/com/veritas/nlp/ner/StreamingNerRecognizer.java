package com.veritas.nlp.ner;

import com.veritas.nlp.resources.ErrorCode;
import com.veritas.nlp.resources.NlpRequestParams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Reads text from a supplied stream and extracts named entities from it.
 */
public class StreamingNerRecognizer {
    private static final int DEFAULT_BUFFER_SIZE_CHARS = 1024 * 1024;
    private final int bufferSizeChars;
    private final NerSettings nerSettings;

    public StreamingNerRecognizer(int bufferSizeChars, NerSettings nerSettings) {
        this.bufferSizeChars = bufferSizeChars;
        this.nerSettings = nerSettings;
    }

    public StreamingNerRecognizer(NerSettings nerSettings) {
        this(DEFAULT_BUFFER_SIZE_CHARS, nerSettings);
    }

    @SuppressFBWarnings(value = "OS_OPEN_STREAM", justification = "Caller owns the stream, so is responsible for closing it.")
    public NerRecognizerResult extractEntities(
            InputStream textStream, NlpRequestParams params) throws Exception {

        ChunkedNerRecognizer chunkedNerRecognizer = new ChunkedNerRecognizer(nerSettings.getNerChunkSizeChars(), params);

        // NOTE: BOMInputStream will detect the charset from the BOM and then (by default) skip the BOM.
        // WARNING! BOMInputStream sorts the supplied array of BOMs, so DO NOT pass in a static array, or you may
        //          hit a nasty race condition with multiple threads!
        BOMInputStream bomInputStream = new BOMInputStream(textStream, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE,
                ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_8);
        String charset = bomInputStream.getBOMCharsetName() != null ? bomInputStream.getBOMCharsetName() : StandardCharsets.UTF_8.name();

        InputStreamReader inputStreamReader = new InputStreamReader(bomInputStream, charset);
        char[] buffer = new char[bufferSizeChars];
        long totalRead = 0;
        int read;

        while ((read = inputStreamReader.read(buffer)) != -1) {
            if (read > 0) {
                totalRead += read;
                checkContentNotTooLarge(totalRead);
                chunkedNerRecognizer.addContent(buffer, 0, read);
            }
        }

        return chunkedNerRecognizer.finalizeNer();
    }

    private void checkContentNotTooLarge(long sizeChars) throws NerException {
        if (sizeChars > nerSettings.getMaxNerContentSizeChars()) {
            throw new NerException(ErrorCode.CONTENT_TOO_LARGE);
        }
    }
}
