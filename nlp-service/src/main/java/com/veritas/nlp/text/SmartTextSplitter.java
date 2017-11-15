package com.veritas.nlp.text;

import org.apache.commons.lang3.StringUtils;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SmartTextSplitter {

    /**
     * Split text into chunks, but tries to align the chunks on sentence boundaries.  Where this is not possible,
     * word boundaries are used, and if that is not possible words will be split.
     */
    public static List<CharSequence> splitOnSentenceBoundaries(
            CharSequence text,
            Locale textLanguage,
            int chunkSize,
            int maxChunkAdjustment
            ) {

        if (StringUtils.isEmpty(text)) {
            return Collections.singletonList("");
        }

        int chunkStart = 0;
        List<CharSequence> chunks = new ArrayList<>();

        // ISSUE: BreakIterator.setText takes a String not a CharSequence - how do we avoid string copy?
        String textString = text.toString();
        BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(textLanguage);
        BreakIterator wordIterator = BreakIterator.getWordInstance(textLanguage);
        sentenceIterator.setText(textString);
        wordIterator.setText(textString);

        while (chunkStart < text.length()) {
            int chunkEnd = chunkStart + chunkSize;
            if (chunkEnd > text.length()) {
                chunkEnd = text.length();
            }

            if (chunkEnd != text.length()) {
                // Chunk might end in the middle of a sentence - if so, go back to the start of the sentence.
                int lastSentenceStart = sentenceIterator.preceding(chunkEnd);
                if (lastSentenceStart == BreakIterator.DONE) {
                    lastSentenceStart = chunkEnd;
                }

                boolean sentenceSearchWentTooFar = lastSentenceStart <= chunkStart
                        || (chunkEnd - lastSentenceStart) > maxChunkAdjustment;

                if (!sentenceSearchWentTooFar) {
                    chunkEnd = lastSentenceStart;
                } else {
                    // Didn't find a sentence boundary within a reasonable range.  We'll fall back to looking for a word boundary.
                    // TODO: Do something smarter by looking for sentence-part breaks, e.g. commas, colons and the like?
                    int wordStart = wordIterator.preceding(chunkEnd);
                    if (wordStart == BreakIterator.DONE) {
                        wordStart = chunkEnd;
                    }

                    boolean wordSearchWentTooFar = wordStart <= chunkStart || (chunkEnd - wordStart) > maxChunkAdjustment;
                    if (!wordSearchWentTooFar) {
                        chunkEnd = wordStart;
                    }
                }
            }

            chunks.add(text.subSequence(chunkStart, chunkEnd));
            chunkStart = chunkEnd;
        }

        return chunks;
    }
}
