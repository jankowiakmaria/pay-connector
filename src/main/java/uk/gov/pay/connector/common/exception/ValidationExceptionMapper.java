package uk.gov.pay.connector.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.commons.model.ErrorIdentifier;
import uk.gov.pay.connector.common.model.api.ErrorResponse;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationExceptionMapper.class);

    @Override
    public Response toResponse(ValidationException exception) {
        LOGGER.error(exception.getErrors().stream().collect(Collectors.joining("\n")));
        ErrorResponse errorResponse = new ErrorResponse(ErrorIdentifier.GENERIC, exception.getErrors());
        return Response.status(400).entity(errorResponse).type(APPLICATION_JSON).build();
    }
}
