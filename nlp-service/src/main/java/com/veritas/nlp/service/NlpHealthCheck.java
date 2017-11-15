package com.veritas.nlp.service;

import com.codahale.metrics.health.HealthCheck;
import com.veritas.nlp.models.NerEntityType;
import com.veritas.nlp.resources.NerResource;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

class NlpHealthCheck extends HealthCheck {
    private static final Logger LOG = LoggerFactory.getLogger(NlpHealthCheck.class);
    private static final int NER_TIMEOUT_SECONDS = 300;
    private final NerResource nerResource;

    NlpHealthCheck(NerResource nerResource) {
        this.nerResource = nerResource;
    }

    @Override
    protected Result check() {
        try {
            try (InputStream content = new ByteArrayInputStream("My name is Sue Smith".getBytes(StandardCharsets.UTF_8))) {
                Response response = nerResource.extractEntities(content, null, EnumSet.of(NerEntityType.PERSON), NER_TIMEOUT_SECONDS);
                if (response.getStatus() != HttpStatus.OK_200) {
                    return Result.unhealthy("Named entity recognition failed with status " + response.getStatus());
                }
                LOG.info("Healthy");
                return Result.healthy();
            }
        } catch (Exception e) {
            LOG.error("Unhealthy", e);
            return Result.unhealthy(e);
        }
    }
}
