package com.veritas.nlp.text;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.veritas.nlp.text.SmartTextSplitter.splitOnSentenceBoundaries;
import static org.assertj.core.api.Assertions.assertThat;

public class SmartTextSplitterTest {

    @Test
    public void splitEmptyTextIsOneEmptyString() {
        assertThat(splitOnSentenceBoundaries(null, Locale.ENGLISH, 10, 5)).containsExactly("");
        assertThat(splitOnSentenceBoundaries("", Locale.ENGLISH, 10, 5)).containsExactly("");
    }

    @Test
    public void textLessThanOneChunkIsNotSplit() {
        int chunkSize = 1000;
        int maxAdjustment = 50;
        List<String> testStrings = Arrays.asList(
                "hello",
                "hello world",
                "hello world.",
                "Hello world. Goodbye world.");
        for (String testString : testStrings) {
            assertThat(splitOnSentenceBoundaries(testString, Locale.ENGLISH, chunkSize, maxAdjustment)).containsExactly(testString);
        }
    }

    @Test
    public void textIsSplitOnSentenceBoundaryIfOneIsNearby() {
        int chunkSize = 30;
        int maxChunkAdjustment = 20;

        assertThat(splitOnSentenceBoundaries("This is sentence one. This is sentence two.", Locale.ENGLISH, chunkSize, maxChunkAdjustment))
                .containsExactly("This is sentence one. ", "This is sentence two.");

        assertThat(splitOnSentenceBoundaries("This is sentence one. This is sentence two. This is sentence three.", Locale.ENGLISH, chunkSize, maxChunkAdjustment))
                .containsExactly("This is sentence one. ", "This is sentence two. ", "This is sentence three.");
    }

    @Test
    public void textIsSplitOnWordBoundaryIfNoSentenceBoundary() {
        int chunkSize = 30;
        int maxChunkAdjustment = 20;

        assertThat(splitOnSentenceBoundaries("This is sentence one, this is sentence two.", Locale.ENGLISH, chunkSize, maxChunkAdjustment))
                .containsExactly("This is sentence one, this is", " sentence two.");

        assertThat(splitOnSentenceBoundaries("This is sentence one, this is sentence two, this is sentence three.", Locale.ENGLISH, chunkSize, maxChunkAdjustment))
                .containsExactly("This is sentence one, this is", " sentence two, this is ", "sentence three.");
    }

    @Test
    public void textIsSplitOnWordBoundaryIfSentenceBoundaryIsTooFarAway() {
        int chunkSize = 30;
        int maxChunkAdjustment = 7; // enough to find word boundaries, but not enough to find sentence boundaries

        assertThat(splitOnSentenceBoundaries("This is sentence one. This is sentence two.", Locale.ENGLISH, chunkSize, maxChunkAdjustment))
                .containsExactly("This is sentence one. This is", " sentence two.");

        assertThat(splitOnSentenceBoundaries("This is sentence one. This is sentence two. This is sentence three.", Locale.ENGLISH, chunkSize, maxChunkAdjustment))
                .containsExactly("This is sentence one. This is", " sentence two. This is ", "sentence three.");
    }

    @Test
    public void textIsSplitWithinWordsIfSentenceAndWordBoundariesAreTooFarAway() {
        int chunkSize = 36;
        int maxChunkAdjustment = 1; // not enough to find sentence or word boundaries

        assertThat(splitOnSentenceBoundaries("This is sentence one. This is sentence two.", Locale.ENGLISH, chunkSize, maxChunkAdjustment))
                .containsExactly("This is sentence one. This is senten", "ce two.");

        assertThat(splitOnSentenceBoundaries("This is sentence one. This is sentence two. This is sentence three. Finally this is sentence four.", Locale.ENGLISH, chunkSize, maxChunkAdjustment))
                .containsExactly("This is sentence one. This is senten", "ce two. This is sentence three. Fina", "lly this is sentence four.");
    }

    @Test
    public void textWithNoWhitespaceIsSplitOnChunkBoundary() {
        int chunkSize = 10;
        int maxChunkAdjustment = 5;

        assertThat(splitOnSentenceBoundaries("This_is_sentence_one_this_is_sentence_two.", Locale.ENGLISH, chunkSize, maxChunkAdjustment))
                .containsExactly("This_is_se", "ntence_one", "_this_is_s", "entence_tw", "o.");
    }

}