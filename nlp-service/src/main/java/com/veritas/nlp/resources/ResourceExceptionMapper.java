package com.veritas.nlp.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.veritas.nlp.models.ErrorResponse;
import com.veritas.nlp.ner.NerException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

public class ResourceExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceExceptionMapper.class);
    private static final List<String> SENSITIVE_STRINGS = Arrays.asList("stanford", "java", "com.", "org.");

    public ResourceExceptionMapper() {
    }

    public Response toResponse(Exception exception) {
        Response response = mapResponse(exception);
        StatusType statusInfo = response.getStatusInfo();
        if (statusInfo.getFamily() != Family.SUCCESSFUL) {
            LOG.error("{} ({}): {}", statusInfo.getReasonPhrase(), statusInfo.getStatusCode(), statusInfo.getReasonPhrase(), exception);
        }
        sanitiseResponse(response);
        return response;
    }

    private static Response mapResponse(Throwable throwable) {
        if (throwable instanceof ExceptionContext && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        if (throwable instanceof BadRequestException || throwable instanceof IllegalArgumentException) {
            return buildResponse(Status.BAD_REQUEST, throwable.getMessage());
        }
        else if (throwable instanceof JsonProcessingException) {
            // Json exception message may reveal internals, so don't include it.  But at least let the client
            // know that the problem is with the json.
            return buildResponse(Status.BAD_REQUEST, "Invalid json");
        }
        else if (throwable instanceof NotFoundException) {
            return buildResponse(Status.NOT_FOUND);
        }
        else if (throwable instanceof WebApplicationException) {
            WebApplicationException webApplicationException = (WebApplicationException)throwable;
            return buildResponse(webApplicationException.getResponse().getStatusInfo(), webApplicationException.getMessage());
        }
        else if (throwable instanceof NerException) {
            return buildResponse(((NerException)throwable).getCode(),
                    getMessageWithCause(throwable, ErrorCode.ENTITY_RECOGNITION_FAILED.getMessage()));
        }
        else if (throwable instanceof TimeoutException) {
            return buildResponse(ErrorCode.TIMEOUT);
        }
        else {
            return buildResponse(Status.INTERNAL_SERVER_ERROR,
                    getMessageWithCause(throwable, Status.INTERNAL_SERVER_ERROR.getReasonPhrase())
            );
        }
    }

    private static Response buildResponse(StatusType status) {
        return buildResponse(status, status.getReasonPhrase());
    }

    private static Response buildResponse(StatusType status, String reason) {
        return buildResponse(status, getDefaultErrorCodeForStatus(status), reason);
    }

    private static Response buildResponse(ErrorCode errorCode) {
        return buildResponse(errorCode, "");
    }

    private static Response buildResponse(ErrorCode errorCode, String reason) {
        return buildResponse(getStatusCodeForErrorCode(errorCode), errorCode, reason);
    }

    private static Response buildResponse(StatusType status, ErrorCode errorCode, String reason) {
        return Response
                .status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(
                        status.getStatusCode(),
                        errorCode,
                        StringUtils.isBlank(reason) ? errorCode.getMessage() : reason))
                .build();
    }

    private static ErrorCode getDefaultErrorCodeForStatus(StatusType statusType) {
        if (statusType.getFamily() == Family.CLIENT_ERROR) {
            return ErrorCode.CLIENT_ERROR;
        }
        return ErrorCode.SERVER_ERROR;
    }

    private static StatusType getStatusCodeForErrorCode(ErrorCode errorCode) {
        switch (errorCode) {
            case CLIENT_ERROR:
            case CONTENT_TOO_LARGE:
                return Status.BAD_REQUEST;
            default:
                return Status.INTERNAL_SERVER_ERROR;
        }
    }

    private void sanitiseResponse(Response response) {
        if (!(response.getEntity() instanceof ErrorResponse)) {
            return;
        }
        ErrorResponse errorResponse = (ErrorResponse)response.getEntity();
        if (StringUtils.isNotBlank(errorResponse.getMessage())) {
            String message = errorResponse.getMessage().toLowerCase(Locale.ENGLISH);
            if (SENSITIVE_STRINGS.stream().anyMatch(message::contains)) {
                errorResponse.setMessage(errorResponse.getError().getMessage());
            }
        }
    }

    private static String getMessageWithCause(Throwable t, String defaultMessage) {
        String message = StringUtils.isNotBlank(t.getMessage()) ? t.getMessage() : defaultMessage;
        if (t.getCause() != null && StringUtils.isNotBlank(t.getCause().getMessage())) {
            if (StringUtils.isNotBlank(message)) {
                return message + " [" + t.getCause().getMessage() + "]";
            }
            return t.getCause().getMessage();
        }
        return StringUtils.isNotBlank(message) ? message : StringUtils.EMPTY;
    }
}
