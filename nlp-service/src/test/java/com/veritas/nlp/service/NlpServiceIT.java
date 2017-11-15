package com.veritas.nlp.service;

import com.veritas.nlp.models.NerResult;
import com.veritas.nlp.models.NerEntityType;
import org.glassfish.jersey.media.multipart.Boundary;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class NlpServiceIT {
    private static final String API_URL = TestNlpService.API_URL;
    private static final String ADMIN_URL = TestNlpService.ADMIN_URL;
    private TestNlpService testService;

    @BeforeClass
    public void beforeClass() throws Exception {
        ensureWorkingFolderIsTargetFolder();

        testService = new TestNlpService();
        testService.configOverride("swagger.enabled", "true");
        testService.start();
    }

    @AfterClass
    public void afterClass() throws Exception {
        testService.stop();
    }

    @Test
    public void appPortIsAvailable() throws Exception {
        checkEndpointAvailable(API_URL, testService.getAppPort());
    }

    @Test
    public void adminPortIsAvailable() throws Exception {
        checkEndpointAvailable(ADMIN_URL, testService.getAdminPort());
    }


    @Test
    public void canRecognizeNames_UTF8() throws Exception {
        String content = "My name is Joe Bloggs, and my sister is Jane Bloggs.  We live in Sydney, Australia.";

        Map<NerEntityType, Set<String>> entityMap = extractNames(content, StandardCharsets.UTF_8, null);
        Set<String> people = entityMap.get(NerEntityType.PERSON);
        Set<String> locations = entityMap.get(NerEntityType.LOCATION);

        assertThat(entityMap).hasSize(2);
        assertThat(people).containsExactlyInAnyOrder("Joe Bloggs", "Jane Bloggs");
        assertThat(locations).containsExactlyInAnyOrder("Sydney", "Australia");
    }

    @Test
    public void canRecognizeNames_UTF16LE() throws Exception {
        String content = "My name is Joe Bloggs, and my sister is Jane Bloggs.  We live in Sydney, Australia.";

        Map<NerEntityType, Set<String>> entityMap = extractNames(content, Charset.forName("UnicodeLittle"), null);
        Set<String> people = entityMap.get(NerEntityType.PERSON);
        Set<String> locations = entityMap.get(NerEntityType.LOCATION);

        assertThat(entityMap).hasSize(2);
        assertThat(people).containsExactlyInAnyOrder("Joe Bloggs", "Jane Bloggs");
        assertThat(locations).containsExactlyInAnyOrder("Sydney", "Australia");
    }

    @Test
    public void canRestrictTypeOfNamesReturned() throws Exception {
        String content = "My name is Joe Bloggs, and my sister is Jane Bloggs.  We live in Sydney, Australia.";

        Map<NerEntityType, Set<String>> entityMap = extractNames(content, StandardCharsets.UTF_8, EnumSet.of(NerEntityType.PERSON));
        Set<String> people = entityMap.get(NerEntityType.PERSON);

        assertThat(entityMap).hasSize(1);
        assertThat(people).containsExactlyInAnyOrder("Joe Bloggs", "Jane Bloggs");
    }

    private Map<NerEntityType, Set<String>> extractNames(String content, Charset charset, EnumSet<NerEntityType> entityTypes) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromUri(API_URL)
                .path("v1/names")
                .port(testService.getAppPort());

        Client client = testService.getClient();

        try(InputStream contentStream = new ByteArrayInputStream(content.getBytes(charset))) {
            FormDataMultiPart multipart = new FormDataMultiPart();
            multipart.bodyPart(new StreamDataBodyPart("file", contentStream, "doc.txt", MediaType.TEXT_PLAIN_TYPE));

            NerResult result = client.target(uriBuilder)
                    .queryParam("type", entityTypes == null ? null : entityTypes.toArray())
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(createMultiPartEntityForApacheConnector(multipart), NerResult.class);

            return result.getEntities();
        }
    }

    private void checkEndpointAvailable(String url, int port) {
        UriBuilder uriBuilder = UriBuilder.fromUri(url).port(port);
        Response response = testService.getClient().target(uriBuilder).request().get();
        assertThat(response.getStatus()).isEqualTo(200);
        drainAndCloseResponse(response);
    }

    private static void ensureWorkingFolderIsTargetFolder() {
        String workingFolder = System.getProperty("user.dir").replace(File.separator, "");
        if (!workingFolder.endsWith("target")) {
            throw new IllegalStateException("Working folder must be 'target' for this suite to work.  Check your IDE launch configuration (if appropriate).");
        }
    }

    private static void drainAndCloseResponse(Response response) {
        // For some reason closing the response without reading the response body causes a
        // 'Premature end of Content-Length' exception.
        // Seems like a bug in the http client lib, so should revisit this later to see if it's been fixed.
        response.readEntity(String.class);
        response.close();
    }

    private static Entity<FormDataMultiPart> createMultiPartEntityForApacheConnector(FormDataMultiPart multipart) {
        // Need to explicitly add boundary to work around a limitation in Jersey+MultiPartFeature+ApacheConnector
        // If we don't add it, the MultiPartFeature code will try to add it later (too late) and
        // ApacheConnector won't like it and will refuse.
        //
        // See https://jersey.java.net/documentation/latest/client.html#connectors.warning
        //
        MediaType contentTypeWithBoundary = Boundary.addBoundary(multipart.getMediaType());
        return Entity.entity(multipart, contentTypeWithBoundary);
    }
}