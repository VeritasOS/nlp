package com.veritas.nlp.ner;

import com.veritas.nlp.models.NlpTagSet;
import com.veritas.nlp.models.NlpTagType;
import com.veritas.nlp.resources.NlpRequestParams;
import com.veritas.nlp.text.SmartTextSplitter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Performs named entity recognition by splitting content into chunks.  This helps performance, especially with
 * very large documents and those with 'unnatural' text.
 */
class ChunkedNerRecognizer {
    private static final int SEARCH_SENTENCE_BOUNDARY_MAX_CHARS = 200;
    private final StringBuilder toBeProcessed = new StringBuilder();
    private final Map<NlpTagType, NlpTagSet> entitiesMap = new HashMap<>();
    private final int chunkSize;
    private final NlpRequestParams params;
    private final Instant startTime = Instant.now();
    private Duration timeout;
    private long matchBaseOffset;

    ChunkedNerRecognizer(int chunkSize, NlpRequestParams params) {
        this.chunkSize = chunkSize;
        this.params = params;
        this.timeout = params.getTimeout();
    }

    void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    void addContent(char[] text, int offset, int len) throws Exception {
        toBeProcessed.append(text, offset, len);
        if (toBeProcessed.length() < chunkSize) {
            return;
        }
        processText(false);
    }

    Map<NlpTagType, NlpTagSet> getEntities() throws Exception {
        processText(true);
        return entitiesMap;
    }

    private void processText(boolean finalize) throws Exception {
        List<CharSequence> chunks = SmartTextSplitter.splitOnSentenceBoundaries(
                toBeProcessed, Locale.ENGLISH, chunkSize, SEARCH_SENTENCE_BOUNDARY_MAX_CHARS);

        // Don't extract entities from the last chunk because it might not be complete (unless we're finalizing, in
        // which case we know we won't get any more text to fill out the chunk)
        int chunksToProcess = finalize ? chunks.size() : chunks.size() - 1;

        long matchBaseOffsetForChunk = matchBaseOffset;
        for (int i=0; i < chunksToProcess; i++) {
            extractEntities(chunks.get(i), matchBaseOffsetForChunk);
            matchBaseOffsetForChunk += chunks.get(i).length();
        }

        if (!finalize && chunks.size() >= 2) {
            // The last chunk wasn't processed - it will be the start of the first chunk next time.
            long processedLength = toBeProcessed.length();
            toBeProcessed.setLength(0);
            toBeProcessed.append(chunks.get(chunks.size() - 1));
            processedLength -= toBeProcessed.length();

            matchBaseOffset += processedLength;
        }
    }

    private void extractEntities(CharSequence text, long matchBaseOffsetForChunk) throws Exception {
        checkTimeout();
        StanfordEntityRecogniser recogniser = new StanfordEntityRecogniser(
                entitiesMap, text.toString(), params, matchBaseOffsetForChunk);
        recogniser.extractEntities();
    }

    private void checkTimeout() throws TimeoutException {
        if (startTime.plus(timeout).isBefore(Instant.now())) {
            throw new TimeoutException();
        }
    }
}
