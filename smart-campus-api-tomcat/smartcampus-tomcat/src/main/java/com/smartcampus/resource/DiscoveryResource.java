package com.smartcampus.resource;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;
import java.util.logging.Logger;
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {
    private static final Logger logger = Logger.getLogger(DiscoveryResource.class.getName());
    @GET
    public Response discover() {
        logger.info("Discovery endpoint called");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("contact", "admin@smartcampus.ac.uk");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors");
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/api/v1/");
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("_links", links);
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms",   "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        response.put("resources", resources);
        return Response.ok(response).build();
    }
}
