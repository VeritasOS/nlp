package com.veritas.nlp.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import io.dropwizard.util.Duration;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

class HttpClientBuilder {
    private static final int CONNECT_TIMEOUT_MS = 60000;

    static Client newClient(Duration timeout) {
        ClientConfig clientConfig = new ClientConfig()
                .connectorProvider(new ApacheConnectorProvider())
                .property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT_MS)
                .property(ClientProperties.READ_TIMEOUT, (int)timeout.toMilliseconds())
                // Request bodies can be very large so use chunked request entity processing
                // with the downside that requests are not repeatable
                .property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED)
                .property(ClientProperties.CHUNKED_ENCODING_SIZE, 64 * 1024);

        return ClientBuilder.newClient(clientConfig)
                .register(JacksonFeature.class)
                .register(MultiPartFeature.class)
                .register(new JacksonJsonProvider().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
    }
}
