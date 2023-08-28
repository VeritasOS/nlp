package com.veritas.nlp.resources;

import io.federecio.dropwizard.swagger.SwaggerOAuth2Configuration;
import io.federecio.dropwizard.swagger.SwaggerResource;
import io.federecio.dropwizard.swagger.SwaggerViewConfiguration;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class ApiRoot {
    private static final Logger LOG = LoggerFactory.getLogger(ApiRoot.class);
    private static String swaggerUrlPattern;
    private static SwaggerViewConfiguration swaggerViewConfiguration;

    // This resource is the root for authentication and authorisation filter mappings and must
    // exist in order for token based auth to work with browsers whereby the browser can make
    // a scripted HEAD request to this resource, specifying an auth token, therefore causing
    // the auth filter(s) to fire and the auth token cookie to be set for subsequent browser
    // requests. Its HEAD method also acts as the cheapest possible request to trigger the
    // Windows negotiate challenge-response sequence without anything additional needing to
    // sent, re-sent, or processed during the multiple request-response sequences.
    @HEAD
    public Response head() {
        LOG.debug("HEAD request received");
        return Response.ok().build(); // Explicit 200 (ok) to avoid automatic 204 (no content)
    }

    // Shortcut to Swagger.
    // Not auth'd (auth filters code ignores)
    @GET
    public Response get() {
        LOG.debug("GET request received");
        if (swaggerUrlPattern == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(new SwaggerResource(swaggerUrlPattern, swaggerViewConfiguration, new SwaggerOAuth2Configuration()).get()).build();
    }

    public static void configureSwagger(String urlPattern, SwaggerViewConfiguration viewConfiguration) {
        swaggerUrlPattern = urlPattern;
        swaggerViewConfiguration = viewConfiguration;
    }
}
