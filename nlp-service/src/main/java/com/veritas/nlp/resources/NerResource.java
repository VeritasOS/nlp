package com.veritas.nlp.resources;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jakarta.rs.annotation.JacksonFeatures;
import com.veritas.nlp.models.ErrorResponse;
import com.veritas.nlp.models.NerResult;
import com.veritas.nlp.models.NlpTagSet;
import com.veritas.nlp.models.NlpTagType;
import com.veritas.nlp.ner.StreamingNerRecognizer;
import com.veritas.nlp.service.NlpServiceSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

@Path("/v1")
@Tag(name = "Named Entity Recognition")
public class NerResource {
    private final NlpServiceSettings settings;

    public NerResource(NlpServiceSettings settings) {
        this.settings = settings;
    }

    @POST
    @Path("names")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Extract named entities from the supplied text",
            description = ResourceStrings.ENTITIES_OPERATION_NOTES,
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Success",
                            content = @Content(schema = @Schema(implementation = NerResult.class))),
                    @ApiResponse(responseCode = "400",
                            description = "Bad request",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403",
                            description = "Forbidden",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404",
                            description = "Not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "422",
                            description = "Unprocessable entity",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "503",
                            description = "Service unavailable",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
    public Response extractEntities(
            @Parameter(description = ResourceStrings.ENTITIES_DOCUMENT) @FormDataParam("file") InputStream documentStream,
            @FormDataParam("file") FormDataContentDisposition fileMetaData,
            @Parameter(description = ResourceStrings.ENTITIES_TYPES) @QueryParam("type") Set<NlpTagType> types,
            @DefaultValue("300") @QueryParam("timeoutSeconds") int timeoutSeconds,
            @Parameter(description = ResourceStrings.ENTITIES_MIN_CONFIDENCE_PERCENTAGE) @DefaultValue("90") @QueryParam("minConfidencePercentage") int minConfidencePercentage,
            @Parameter(description = ResourceStrings.ENTITIES_INCLUDE_MATCHES) @QueryParam("includeMatches") boolean includeMatches,
            @Parameter(description = ResourceStrings.ENTITIES_MAX_CONTENT_MATCHES) @QueryParam("maxContentMatches") Integer maxContentMatches
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
