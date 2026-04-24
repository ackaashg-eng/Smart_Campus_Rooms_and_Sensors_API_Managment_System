package com.smartcampus.exception.mapper;
import com.smartcampus.model.ErrorResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.util.logging.*;
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger logger = Logger.getLogger(GlobalExceptionMapper.class.getName());
    @Override
    public Response toResponse(Throwable t) {
        if (t instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) t;
            int status = wae.getResponse().getStatus();
            logger.warning("WebApplicationException: " + status + " - " + t.getMessage());
            String reason = Response.Status.fromStatusCode(status) != null ? Response.Status.fromStatusCode(status).getReasonPhrase() : "Error";
            return Response.status(status).entity(new ErrorResponse(status, reason, t.getMessage())).type(MediaType.APPLICATION_JSON).build();
        }
        logger.log(Level.SEVERE, "Unhandled exception: " + t.getMessage(), t);
        return Response.status(500).entity(new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred. Please contact the API administrator.")).type(MediaType.APPLICATION_JSON).build();
    }
}
