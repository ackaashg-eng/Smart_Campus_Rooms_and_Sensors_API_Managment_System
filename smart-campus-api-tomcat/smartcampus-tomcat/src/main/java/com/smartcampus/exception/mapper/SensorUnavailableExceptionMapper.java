package com.smartcampus.exception.mapper;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.ErrorResponse;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.util.logging.Logger;
@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    private static final Logger logger = Logger.getLogger(SensorUnavailableExceptionMapper.class.getName());
    @Override
    public Response toResponse(SensorUnavailableException e) {
        logger.warning("403 Forbidden: " + e.getMessage());
        return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(403, "Forbidden", e.getMessage())).type(MediaType.APPLICATION_JSON).build();
    }
}
