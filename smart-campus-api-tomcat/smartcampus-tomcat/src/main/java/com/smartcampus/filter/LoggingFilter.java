package com.smartcampus.filter;
import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger logger = Logger.getLogger(LoggingFilter.class.getName());
    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        logger.info(String.format(">>> REQUEST  | %-6s | %s", req.getMethod(), req.getUriInfo().getRequestUri()));
    }
    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        logger.info(String.format("<<< RESPONSE | %-6s | %-45s | %d", req.getMethod(), req.getUriInfo().getRequestUri(), res.getStatus()));
    }
}
