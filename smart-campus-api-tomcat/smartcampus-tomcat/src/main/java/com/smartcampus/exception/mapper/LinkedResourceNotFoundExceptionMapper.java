package com.smartcampus.exception.mapper;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.ErrorResponse;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.util.logging.Logger;
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    private static final Logger logger = Logger.getLogger(LinkedResourceNotFoundExceptionMapper.class.getName());
    @Override
    public Response toResponse(LinkedResourceNotFoundException e) {
        logger.warning("422 Unprocessable Entity: " + e.getMessage());
        return Response.status(422).entity(new ErrorResponse(422, "Unprocessable Entity", e.getMessage())).type(MediaType.APPLICATION_JSON).build();
    }
}
