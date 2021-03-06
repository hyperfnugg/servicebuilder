package jante.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import jante.model.LogLevel;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Slf4j
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {
    final private GenericExceptionHandler genericExceptionHandler;

    @Inject
    public JsonProcessingExceptionMapper(GenericExceptionHandler genericExceptionHandler) {
        this.genericExceptionHandler = genericExceptionHandler;
    }

    @Override
    public Response toResponse(JsonProcessingException exception) {
        return genericExceptionHandler.handle(exception, cfg -> cfg
                .status(BAD_REQUEST.getStatusCode())
                .logLevel(LogLevel.ERROR)
                .detail("Det har oppstått en intern feil")
                .logger(log)
        );
    }
}
