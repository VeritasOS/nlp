package com.veritas.nlp.ner;

import com.veritas.nlp.models.NerEntityType;
import com.veritas.nlp.text.SmartTextSplitter;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

/**
 * Performs named entity recognition by splitting content into chunks.  This helps performance, especially with
 * very large documents and those with 'unnatural' text.
 */
class ChunkedNerRecognizer {
    private static final int SEARCH_SENTENCE_BOUNDARY_MAX_CHARS = 200;
    private final StringBuilder toBeProcessed = new StringBuilder();
    private final Map<NerEntityType, Set<String>> entities = new HashMap<>();
    private final EnumSet<NerEntityType> entityTypes;
    private final int chunkSize;
    private final StanfordNLP stanfordNLP = new StanfordNLP();
    private final Instant startTime = Instant.now();
    private Duration timeout = Duration.ofSeconds(30);

    ChunkedNerRecognizer(EnumSet<NerEntityType> entityTypes, int chunkSize) {
        this.entityTypes = entityTypes;
        this.chunkSize = chunkSize;
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

    Map<NerEntityType, Set<String>> getEntities() throws Exception {
        processText(true);
        return entities;
    }

    private void processText(boolean finalize) throws Exception {
        List<CharSequence> chunks = SmartTextSplitter.splitOnSentenceBoundaries(
                toBeProcessed, Locale.ENGLISH, chunkSize, SEARCH_SENTENCE_BOUNDARY_MAX_CHARS);

        // Don't extract entities from the last chunk because it might not be complete (unless we're finalizing, in
        // which case we know we won't get any more text to fill out the chunk)
        int chunksToProcess = finalize ? chunks.size() : chunks.size() - 1;

        for (int i=0; i < chunksToProcess; i++) {
            Map<NerEntityType, Set<String>> entities = extractEntities(chunks.get(i));
            storeEntities(entities);
        }

        if (!finalize && chunks.size() >= 2) {
            // The last chunk wasn't processed - it will be the start of the first chunk next time.
            toBeProcessed.setLength(0);
            toBeProcessed.append(chunks.get(chunks.size() - 1));
        }
    }

    private Map<NerEntityType, Set<String>> extractEntities(CharSequence text) throws Exception {
        checkTimeout();
        return stanfordNLP.getEntities(text.toString(), entityTypes);
    }

    private void checkTimeout() throws TimeoutException {
        if (startTime.plus(timeout).isBefore(Instant.now())) {
            throw new TimeoutException();
        }
    }

    private void storeEntities(Map<NerEntityType, Set<String>> entitiesToStore) {
        entitiesToStore.forEach((type, values) -> {
            entities.computeIfAbsent(type, t -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).addAll(values);
        });
    }

}
