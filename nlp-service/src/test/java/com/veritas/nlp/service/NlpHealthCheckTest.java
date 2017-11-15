package com.veritas.nlp.service;

import com.veritas.nlp.ner.NerSettings;
import com.veritas.nlp.resources.NerResource;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class NlpHealthCheckTest {

    @Test
    public void checkSucceeds() throws Exception {
        NlpServiceSettings nlpServiceSettings = new NlpServiceSettings();
        nlpServiceSettings.setNerSettings(new NerSettings());

        NlpHealthCheck check = new NlpHealthCheck(new NerResource(nlpServiceSettings));

        assertThat(check.check().isHealthy()).isTrue();
    }
}