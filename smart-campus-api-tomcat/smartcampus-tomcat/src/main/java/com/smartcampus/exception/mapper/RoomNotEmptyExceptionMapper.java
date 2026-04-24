package com.smartcampus.exception.mapper;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.ErrorResponse;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.util.logging.Logger;
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    private static final Logger logger = Logger.getLogger(RoomNotEmptyExceptionMapper.class.getName());
    @Override
    public Response toResponse(RoomNotEmptyException e) {
        logger.warning("409 Conflict: " + e.getMessage());
        return Response.status(409).entity(new ErrorResponse(409, "Conflict", e.getMessage())).type(MediaType.APPLICATION_JSON).build();
    }
}
