package com.veritas.nlp.resources;

import java.io.*;
import java.time.Duration;
import java.util.*;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.veritas.nlp.models.ErrorResponse;
import com.veritas.nlp.models.NerResult;
import com.veritas.nlp.models.NlpTagSet;
import com.veritas.nlp.models.NlpTagType;
import com.veritas.nlp.ner.StreamingNerRecognizer;
import com.veritas.nlp.service.NlpServiceSettings;
import io.swagger.annotations.*;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;


@Path("/v1")
@Api(value = "Named Entity Recognition")
public class NerResource {
    private final NlpServiceSettings settings;

    public NerResource(NlpServiceSettings settings) {
        this.settings = settings;
    }

    @POST
    @Path("names")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Extract named entities from the supplied text",
            response = NerResult.class,
            responseContainer = "Map",
            notes = ResourceStrings.ENTITIES_OPERATION_NOTES)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = NerResult.class),
            @ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized", response = ErrorResponse.class),
            @ApiResponse(code = 403, message = "Forbidden", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Not found", response = ErrorResponse.class),
            @ApiResponse(code = 422, message = "Unprocessable entity", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal server error", response = ErrorResponse.class),
            @ApiResponse(code = 503, message = "Service unavailable", response = ErrorResponse.class)
    })
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
    public Response extractEntities(
            @ApiParam(value = ResourceStrings.ENTITIES_DOCUMENT) @FormDataParam("file") InputStream documentStream,
            @FormDataParam("file") FormDataContentDisposition fileMetaData,
            @ApiParam(value = ResourceStrings.ENTITIES_TYPES) @QueryParam("type") Set<NlpTagType> types,
            @DefaultValue("300") @QueryParam("timeoutSeconds") int timeoutSeconds,
            @ApiParam(value = ResourceStrings.ENTITIES_MIN_CONFIDENCE_PERCENTAGE) @DefaultValue("90") @QueryParam("minConfidencePercentage") int minConfidencePercentage,
            @ApiParam(value = ResourceStrings.ENTITIES_INCLUDE_MATCHES) @QueryParam("includeMatches") boolean includeMatches,
            @ApiParam(value = ResourceStrings.ENTITIES_MAX_CONTENT_MATCHES) @QueryParam("maxContentMatches") Integer maxContentMatches
            ) throws Exception {

        NlpRequestParams params = new NlpRequestParams()
            .setIncludeMatches(includeMatches)
            .setMaxContentMatches(maxContentMatches)
            .setMinConfidencePercentage(minConfidencePercentage)
            .setTagTypes(types)
            .setTimeout(Duration.ofSeconds(timeoutSeconds));

        StreamingNerRecognizer nerRecognizer = new StreamingNerRecognizer(settings.getNerSettings());
        Map<NlpTagType, NlpTagSet> tagSets = nerRecognizer.extractEntities(documentStream, params);

        return Response.ok(new NerResult(tagSets))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

}
