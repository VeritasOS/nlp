package com.veritas.nlp.ner;


import com.veritas.nlp.models.NerEntityType;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;


public class ChunkedNerRecognizerTest {

    @Test
    public void canRecognizeName() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        addContent(recognizer, "My name is Joe Bloggs");
        Map<NerEntityType, Set<String>> entities = recognizer.getEntities();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(NerEntityType.PERSON)).containsExactly("Joe Bloggs");
    }

    @Test
    public void canRecognizeNameInTextSplitByCaller() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        addContent(recognizer, "My name is Joe ");
        addContent(recognizer, "Bloggs");
        Map<NerEntityType, Set<String>> entities = recognizer.getEntities();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(NerEntityType.PERSON)).containsExactly("Joe Bloggs");
    }

    @Test
    public void namesAreDeduplicated() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < 100; i++) {
            sb.append("My name is Joe Bloggs. ");
        }
        addContent(recognizer, sb.toString());
        Map<NerEntityType, Set<String>> entities = recognizer.getEntities();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(NerEntityType.PERSON)).containsExactly("Joe Bloggs");
    }

    @Test
    public void noNamesReturnedIfNoNames() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        addContent(recognizer, "It's a nice day today");

        assertThat(recognizer.getEntities()).isEmpty();
    }

    @Test
    public void noEntitiesIfNoContentAdded() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        assertThat(recognizer.getEntities()).isEmpty();
    }

    @Test
    public void noEntitiesIfTextEmpty() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        addContent(recognizer, "");
        assertThat(recognizer.getEntities()).isEmpty();
    }

    @Test(expectedExceptions = TimeoutException.class)
    public void timeoutExceptionIfRecognizerTakesTooLong() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        recognizer.setTimeout(Duration.ofMillis(10));
        for (int i=0; i < 10000; i++) {
            addContent(recognizer, "My name is Joe Bloggs ");
        }

        recognizer.getEntities();
    }

    @Test
    public void chunkOverlapDoesNotBreakIfNoConvenientWhitespaceToBreakAt() throws Exception {
        // Chunk overlap is managed partly through looking for sentence/word boundaries to break on,
        // but nothing bad should happen if there is no sentence/word near to break on.
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, 100000).forEach(i -> sb.append('a'));    // loads of text chunks with no whitespace to break on
        sb.append(" My name is Joe Bloggs ");
        IntStream.range(0, 100000).forEach(i -> sb.append('a'));    // loads of text chunks with no whitespace to break on

        int chunkSize = 1000;
        ChunkedNerRecognizer recognizer = new ChunkedNerRecognizer(EnumSet.of(NerEntityType.PERSON), chunkSize);
        addContent(recognizer, sb.toString());
        Map<NerEntityType, Set<String>> entities = recognizer.getEntities();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(NerEntityType.PERSON)).containsExactly("Joe Bloggs");
    }

    private ChunkedNerRecognizer createPersonRecognizer() {
        return new ChunkedNerRecognizer(EnumSet.of(NerEntityType.PERSON), 32768);
    }

    private void addContent(ChunkedNerRecognizer recognizer, String content) throws Exception {
        recognizer.addContent(content.toCharArray(), 0, content.length());
    }
}