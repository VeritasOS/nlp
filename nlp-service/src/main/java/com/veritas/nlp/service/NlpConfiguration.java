package com.veritas.nlp.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

public class NlpConfiguration extends Configuration {
    @JsonProperty("swagger")
    private SwaggerBundleConfiguration swaggerBundleConfiguration = new SwaggerBundleConfiguration();

    @JsonProperty("nlpService")
    private NlpServiceSettings nlpServiceSettings = new NlpServiceSettings();

    public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
        return swaggerBundleConfiguration;
    }

    public void setSwaggerBundleConfiguration(SwaggerBundleConfiguration swaggerBundleConfiguration) {
        if (swaggerBundleConfiguration != null) {
            this.swaggerBundleConfiguration = swaggerBundleConfiguration;
        }
    }

    public NlpServiceSettings getNlpServiceSettings() {
        return nlpServiceSettings;
    }

    public void setNlpServiceSettings(NlpServiceSettings settings) {
        if (settings != null) {
            this.nlpServiceSettings = settings;
        }
    } 
}
