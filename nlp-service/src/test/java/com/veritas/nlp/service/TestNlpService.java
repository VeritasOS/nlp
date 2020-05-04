package com.veritas.nlp.service;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An in-process instance of the Nlp service, for use by integration tests.
 */
class TestNlpService {
    private static final String DEFAULT_BASE_URL = "http://localhost/nlp";
    private static final String DEFAULT_API_URL = DEFAULT_BASE_URL + "/api";
    private static final String DEFAULT_ADMIN_URL = DEFAULT_BASE_URL;
    private static final Duration DEFAULT_TIMEOUT = Duration.seconds(60);

    static final String API_URL = DEFAULT_API_URL;
    static final String ADMIN_URL = DEFAULT_ADMIN_URL;

    private static final Logger LOG = LoggerFactory.getLogger(TestNlpService.class);
    private static final String configPath = "nlp-default-config.yml";
    private final List<ConfigOverride> configOverrides = new ArrayList<>();

    private DropwizardTestSupport<NlpConfiguration> testSupport;
    private int appPort;
    private int adminPort;

    TestNlpService() {
        configOverrides.addAll(Arrays.asList(
            ConfigOverride.config("logging.appenders[0].currentLogFilename", "./nlp-logs/nlp-integration-tests.log"),
            ConfigOverride.config("logging.appenders[0].archivedLogFilenamePattern", "./nlp-logs/nlp-integration-tests%i.log.zip"),
            ConfigOverride.config("server.applicationConnectors[0].port", "0"),
            ConfigOverride.config("server.adminConnectors[0].port", "0")
        ));
    }

    void start() throws Exception {
        testSupport = new DropwizardTestSupport<>(
                NlpMicroService.class, configPath, configOverrides.toArray(new ConfigOverride[0]));
        testSupport.before();
        appPort = testSupport.getLocalPort();
        adminPort = testSupport.getAdminPort();
        LOG.info("appPort:[{}], adminPort:[{}]", appPort, adminPort);
    }

    void stop() {
        if (testSupport != null) {
            testSupport.after();
        }
    }

    void configOverride(String key, String value) {
        configOverrides.add(ConfigOverride.config(key, value));
    }

    int getAppPort() {
        return appPort;
    }

    int getAdminPort() {
        return adminPort;
    }

    Client getClient() {
        return HttpClientBuilder.newClient(DEFAULT_TIMEOUT);
    }
}
