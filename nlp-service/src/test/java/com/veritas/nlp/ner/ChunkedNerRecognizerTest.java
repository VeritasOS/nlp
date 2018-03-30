package com.veritas.nlp.ner;


import com.veritas.nlp.models.NlpMatch;
import com.veritas.nlp.models.NlpMatchCollection;
import com.veritas.nlp.models.NlpTagSet;
import com.veritas.nlp.models.NlpTagType;
import com.veritas.nlp.resources.NlpRequestParams;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ChunkedNerRecognizerTest {
    private static final int DEFAULT_CHUNK_SIZE = 32768;
    private NlpRequestParams params;

    @BeforeMethod
    public void beforeMethod() {
        this.params = new NlpRequestParams();
        params.setTagTypes(EnumSet.of(NlpTagType.PERSON));
    }

    @Test
    public void canRecognizeName() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        addContent(recognizer, "My name is Joe Bloggs");
        Map<NlpTagType, NlpTagSet> entities = recognizer.finalizeNer().getEntities();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(NlpTagType.PERSON).getTags()).containsExactly("Joe Bloggs");
        assertThat(entities.get(NlpTagType.PERSON).getMatchCollection()).isNull();
    }

    @Test
    public void canRecognizeNameInTextSplitByCaller() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        addContent(recognizer, "My name is Joe ");
        addContent(recognizer, "Bloggs");
        Map<NlpTagType, NlpTagSet> entities = recognizer.finalizeNer().getEntities();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(NlpTagType.PERSON).getTags()).containsExactly("Joe Bloggs");
    }

    @Test
    public void namesAreDeduplicated() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < 100; i++) {
            sb.append("My name is Joe Bloggs. ");
        }
        addContent(recognizer, sb.toString());
        Map<NlpTagType, NlpTagSet> entities = recognizer.finalizeNer().getEntities();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(NlpTagType.PERSON).getTags()).containsExactly("Joe Bloggs");
    }

    @Test
    public void noNamesReturnedIfNoNames() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        addContent(recognizer, "It's a nice day today");

        assertThat(recognizer.finalizeNer().getEntities()).isEmpty();
    }

    @Test
    public void noEntitiesIfNoContentAdded() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        assertThat(recognizer.finalizeNer().getEntities()).isEmpty();
    }

    @Test
    public void noEntitiesIfTextEmpty() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        addContent(recognizer, "");
        assertThat(recognizer.finalizeNer().getEntities()).isEmpty();
    }

    @Test(expectedExceptions = TimeoutException.class)
    public void timeoutExceptionIfRecognizerTakesTooLong() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizer();
        recognizer.setTimeout(Duration.ofMillis(10));
        for (int i=0; i < 10000; i++) {
            addContent(recognizer, "My name is Joe Bloggs ");
        }

        recognizer.finalizeNer();
    }

    @Test
    public void chunkOverlapDoesNotBreakIfNoConvenientWhitespaceToBreakAt() throws Exception {
        // Chunk overlap is managed partly through looking for sentence/word boundaries to break on,
        // but nothing bad should happen if there is no sentence/word near to break on\.
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, 100000).forEach(i -> sb.append('a'));    // loads of text chunks with no whitespace to break on
        sb.append(" My name is Joe Bloggs ");
        IntStream.range(0, 100000).forEach(i -> sb.append('a'));    // loads of text chunks with no whitespace to break on

        int chunkSize = 1000;
        ChunkedNerRecognizer recognizer = new ChunkedNerRecognizer(chunkSize, params);
        addContent(recognizer, sb.toString());
        Map<NlpTagType, NlpTagSet> entities = recognizer.finalizeNer().getEntities();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(NlpTagType.PERSON).getTags()).containsExactly("Joe Bloggs");
    }

    @Test
    public void canGetMatchDetailsForName() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizerWithMatches();
        addContent(recognizer, "My name is the amazing Joe Bloggs and I am a fairly anonymous person.");
        Map<NlpTagType, NlpTagSet> entities = recognizer.finalizeNer().getEntities();

        NlpMatchCollection matchCollection = entities.get(NlpTagType.PERSON).getMatchCollection();
        assertThat(matchCollection.getTotal()).isEqualTo(1);
        assertThat(matchCollection.getMatches()).hasSize(1);

        NlpMatch matchForJoe = matchCollection.getMatches().get(0);
        assertThat(matchForJoe.getContent()).isEqualTo("Joe Bloggs");
        assertThat(matchForJoe.getOffset()).isEqualTo(23);
        assertThat(matchForJoe.getLength()).isEqualTo(10);
        assertThat(matchForJoe.getContext()).isEqualTo("My name is the amazing Joe Bloggs and I am a fairly anonymous person.");
        assertThat(matchForJoe.getContextOffset()).isEqualTo(23);
    }

    @Test
    public void canGetMatchDetailsForNameWhenMatchIsNotInFirstChunk() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizerWithMatches();
        int charsAdded = 0;
        String fillerTextPart = "This is some text. ";
        while (charsAdded <= DEFAULT_CHUNK_SIZE * 3.5) {
            addContent(recognizer, fillerTextPart);
            charsAdded += fillerTextPart.length();
        }
        addContent(recognizer, "My name is the amazing Joe Bloggs and I am a fairly anonymous person.");
        Map<NlpTagType, NlpTagSet> entities = recognizer.finalizeNer().getEntities();

        NlpMatchCollection matchCollection = entities.get(NlpTagType.PERSON).getMatchCollection();
        assertThat(matchCollection.getTotal()).isEqualTo(1);
        assertThat(matchCollection.getMatches()).hasSize(1);

        NlpMatch matchForJoe = matchCollection.getMatches().get(0);
        assertThat(matchForJoe.getContent()).isEqualTo("Joe Bloggs");
        assertThat(matchForJoe.getOffset()).isEqualTo(charsAdded + 23);
        assertThat(matchForJoe.getLength()).isEqualTo(10);
        assertThat(matchForJoe.getContext()).contains("My name is the amazing Joe Bloggs and I am a fairly anonymous person.");
        assertThat(matchForJoe.getContextOffset()).isEqualTo(matchForJoe.getContext().indexOf("Joe"));
    }

    @Test
    public void multipleMatchesForSingleNameAreReturnedAndAreCaseInsensitive() throws Exception {
        ChunkedNerRecognizer recognizer = createPersonRecognizerWithMatches();
        addContent(recognizer, "My name is Joe Bloggs and my dad is also called Joe Bloggs.  My grandfather was also Joe bloggs.");
        Map<NlpTagType, NlpTagSet> entities = recognizer.finalizeNer().getEntities();

        assertThat(entities.get(NlpTagType.PERSON).getMatchCollection().getMatches()).hasSize(3);
        assertThat(entities.get(NlpTagType.PERSON).getMatchCollection().getTotal()).isEqualTo(3);
    }

    private ChunkedNerRecognizer createPersonRecognizer() {
        return createPersonRecognizer(false);
    }

    private ChunkedNerRecognizer createPersonRecognizerWithMatches() {
        return createPersonRecognizer(true);
    }

    private ChunkedNerRecognizer createPersonRecognizer(boolean includeMatches) {
        params.setIncludeMatches(includeMatches);
        return new ChunkedNerRecognizer(DEFAULT_CHUNK_SIZE, params);
    }

    private void addContent(ChunkedNerRecognizer recognizer, String content) throws Exception {
        recognizer.addContent(content.toCharArray(), 0, content.length());
    }
}