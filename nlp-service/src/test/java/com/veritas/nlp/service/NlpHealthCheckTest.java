package com.veritas.nlp.service;

import com.codahale.metrics.health.HealthCheck;
import com.veritas.nlp.models.NlpTagType;
import com.veritas.nlp.ner.NerException;
import com.veritas.nlp.resources.NerResource;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@Listeners(MockitoTestNGListener.class)
public class NlpHealthCheckTest {
    @Mock
    NerResource nerResource;

    @Mock
    Response response;

    @InjectMocks
    NlpHealthCheck nlpHealthCheck;

    @BeforeMethod
    public void beforeMethod() {
    }

    @AfterMethod
    public void afterMethod() {
        nlpHealthCheck = null;
    }

    @Test
    public void checkSucceeds() throws Exception {
        when(nerResource.extractEntities(any(InputStream.class), isNull(), eq(EnumSet.of(NlpTagType.PERSON)),
                eq(300), eq(90), eq(false), eq(0)))
                .thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.OK_200);

        assertThat(nlpHealthCheck.check().isHealthy()).isTrue();
    }

    @Test
    public void checkFails() throws Exception {
        when(nerResource.extractEntities(any(InputStream.class), isNull(), eq(EnumSet.of(NlpTagType.PERSON)),
                eq(300), eq(90), eq(false), eq(0)))
                .thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.NOT_FOUND_404).thenReturn(HttpStatus.NOT_FOUND_404);

        HealthCheck.Result result = nlpHealthCheck.check();

        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Named entity recognition failed with status 404");
    }

    @Test
    public void checkFailsWithException() throws Exception {
        when(nerResource.extractEntities(any(InputStream.class), isNull(), eq(EnumSet.of(NlpTagType.PERSON)),
                eq(300), eq(90), eq(false), eq(0)))
                .thenThrow(new NerException("Extract entities failed."));

        HealthCheck.Result result = nlpHealthCheck.check();

        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getError()).isInstanceOf(NerException.class).hasMessageContaining("Extract entities failed.");
        assertThat(result.getMessage()).contains("Extract entities failed.");
    }
}