package com.veritas.nlp.ner;

import com.veritas.nlp.models.NerEntityType;
import com.veritas.nlp.resources.ErrorCode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Reads text from a supplied stream and extracts named entities from it.
 */
public class StreamingNerRecognizer {
    private static final int DEFAULT_BUFFER_SIZE_CHARS = 1024 * 1024;
    private final EnumSet<NerEntityType> entityTypes;
    private final int bufferSizeChars;
    private final NerSettings nerSettings;

    public StreamingNerRecognizer(EnumSet<NerEntityType> entityTypes, int bufferSizeChars, NerSettings nerSettings) {
        this.entityTypes = entityTypes;
        this.bufferSizeChars = bufferSizeChars;
        this.nerSettings = nerSettings;
    }

    public StreamingNerRecognizer(EnumSet<NerEntityType> entityTypes, NerSettings nerSettings) {
        this(entityTypes, DEFAULT_BUFFER_SIZE_CHARS, nerSettings);
    }

    @SuppressFBWarnings(value = "OS_OPEN_STREAM", justification = "Caller owns the stream, so is responsible for closing it.")
    public Map<NerEntityType, Set<String>> extractEntities(InputStream textStream, Duration timeout, double minConfidence) throws Exception {
        ChunkedNerRecognizer chunkedNerRecognizer = new ChunkedNerRecognizer(entityTypes, nerSettings.getNerChunkSizeChars(), minConfidence);
        chunkedNerRecognizer.setTimeout(timeout);

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

        return chunkedNerRecognizer.getEntities();
    }

    private void checkContentNotTooLarge(long sizeChars) throws NerException {
        if (sizeChars > nerSettings.getMaxNerContentSizeChars()) {
            throw new NerException(ErrorCode.CONTENT_TOO_LARGE);
        }
    }
}
