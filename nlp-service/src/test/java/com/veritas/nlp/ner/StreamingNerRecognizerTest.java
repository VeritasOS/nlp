package com.veritas.nlp.ner;

import com.veritas.nlp.models.NlpTagSet;
import com.veritas.nlp.models.NlpTagType;
import com.veritas.nlp.resources.ErrorCode;
import com.veritas.nlp.resources.NlpRequestParams;
import com.veritas.nlp.utils.ThrowingRunnable;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;


public class StreamingNerRecognizerTest {
    private static final NerSettings DEFAULT_NER_SETTINGS = new NerSettings();

    private NlpRequestParams params;

    @BeforeMethod
    public void beforeMethod() {
        this.params = new NlpRequestParams();
        params.setTagTypes(EnumSet.of(NlpTagType.PERSON));
    }

    @Test
    public void canExtractEntitiesFromStream() throws Exception {
        // NOTE: Must use charsets that add a BOM where necessary, e.g. UnicodeLittle rather than UTF-16LE.
        for (String charsetName : Arrays.asList("UTF-8", "UnicodeLittle", "UnicodeBig", "UTF_32BE_BOM", "UTF_32LE_BOM")) {
            StreamingNerRecognizer recognizer = new StreamingNerRecognizer(100, DEFAULT_NER_SETTINGS);

            Map<NlpTagType, NlpTagSet> entities = recognizer.extractEntities(
                    new ByteArrayInputStream("My name is Sue Jones.".getBytes(charsetName)), params);

            assertThat(entities.get(NlpTagType.PERSON).getTags()).containsExactly("Sue Jones");
        }
    }

    @Test
    public void canExtractEntitiesFromSlowlyPopulatedStream() throws Exception {
        StreamingNerRecognizer recognizer = new StreamingNerRecognizer(100, DEFAULT_NER_SETTINGS);

        // First, get the recognizer warmed up, so first-time-load delays don't influence our test.
        recognizer.extractEntities(new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)), params);

        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputStream = new PipedOutputStream(inputStream);
        Charset charset = StandardCharsets.UTF_8;

        // Write to the stream on a background thread (slowly).  Our recognizer should read from the stream until
        // it is closed, so the drip feed of text into the stream should not affect the recognizer functionality
        // (other than slow it down).
        runOnNewThread(() -> {
            // write the original OutputStream to the PipedOutputStream
            IOUtils.write("My name is ", outputStream, charset);
            Thread.sleep(500);
            IOUtils.write("Joe Bloggs.", outputStream, charset);
            Thread.sleep(500);
            outputStream.close();
        });

        Map<NlpTagType, NlpTagSet> entities = recognizer.extractEntities(inputStream, params);
        assertThat(entities.get(NlpTagType.PERSON).getTags()).containsExactly("Joe Bloggs");
    }

    @Test
    public void recognizerThrowsIfTakesTooLong() throws Exception {
        StreamingNerRecognizer recognizer = new StreamingNerRecognizer(100, DEFAULT_NER_SETTINGS);

        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputStream = new PipedOutputStream(inputStream);
        Charset charset = StandardCharsets.UTF_8;

        runOnNewThread(() -> {
            IOUtils.write("My name is ", outputStream, charset);
            Thread.sleep(500);
            outputStream.close();
        });

        params.setTimeout(Duration.ofMillis(300));
        assertThatThrownBy(() -> recognizer.extractEntities(inputStream, params)).isInstanceOf(TimeoutException.class);
    }

    @Test
    public void recognizerThrowsIfContentTooLarge() throws Exception {
        NerSettings nerSettings = new NerSettings();
        nerSettings.setMaxNerContentSizeChars(10);
        StreamingNerRecognizer recognizer = new StreamingNerRecognizer(100, nerSettings);

        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputStream = new PipedOutputStream(inputStream);
        Charset charset = StandardCharsets.UTF_8;

        runOnNewThread(() -> {
            IOUtils.write("This is some text that is more than 10 chars long.", outputStream, charset);
            outputStream.close();
        });

        Throwable thrown = catchThrowable(() -> recognizer.extractEntities(inputStream, params));
        assertThat(thrown).isInstanceOf(NerException.class);
        assertThat(((NerException)thrown).getCode()).isEqualTo(ErrorCode.CONTENT_TOO_LARGE);
    }

    private void runOnNewThread(ThrowingRunnable runnable) {
        new Thread(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}