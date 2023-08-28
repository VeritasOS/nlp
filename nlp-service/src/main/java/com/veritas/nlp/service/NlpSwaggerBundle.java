package com.veritas.nlp.service;

import com.veritas.nlp.resources.ApiRoot;
import io.dropwizard.core.setup.Environment;
import io.federecio.dropwizard.swagger.ConfigurationHelper;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.apache.commons.lang3.StringUtils;

class NlpSwaggerBundle extends SwaggerBundle<NlpConfiguration> {
    @Override
    protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(NlpConfiguration configuration) {
        return configuration.getSwaggerBundleConfiguration();
    }

    @Override
    public void run(NlpConfiguration configuration, Environment environment) throws Exception {
        SwaggerBundleConfiguration swaggerBundleConfiguration = getSwaggerBundleConfiguration(configuration);
        // Only add swagger if it's configured
        if (swaggerBundleConfiguration.isEnabled() && StringUtils.isNotBlank(swaggerBundleConfiguration.getResourcePackage())) {
            super.run(configuration, environment);
            ConfigurationHelper configurationHelper = new ConfigurationHelper(configuration, swaggerBundleConfiguration);

            // Also expose swagger at the API root in addition to the "/swagger" path
            ApiRoot.configureSwagger(configurationHelper.getUrlPattern(),
                    swaggerBundleConfiguration.getSwaggerViewConfiguration());
        }
    }
}
