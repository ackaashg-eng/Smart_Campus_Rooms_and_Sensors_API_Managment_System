package com.smartcampus.resource;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {
    private static final Logger logger = Logger.getLogger(SensorResource.class.getName());
    private final DataStore store = DataStore.getInstance();
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        logger.info("GET /sensors — type: " + type);
        List<Sensor> result = new ArrayList<>(store.getSensors().values());
        if (type != null && !type.isBlank())
            result = result.stream().filter(s -> type.equalsIgnoreCase(s.getType())).collect(Collectors.toList());
        return Response.ok(result).build();
    }
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getRoomId() == null || sensor.getRoomId().isBlank())
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"roomId is required\"}").build();
        if (!store.getRooms().containsKey(sensor.getRoomId()))
            throw new LinkedResourceNotFoundException("Room", sensor.getRoomId());
        if (sensor.getId() == null || sensor.getId().isBlank())
            sensor.setId("SENS-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        if (sensor.getStatus() == null || sensor.getStatus().isBlank())
            sensor.setStatus("ACTIVE");
        store.getSensors().put(sensor.getId(), sensor);
        store.getRooms().get(sensor.getRoomId()).addSensorId(sensor.getId());
        logger.info("Created sensor: " + sensor.getId());
        URI location = UriBuilder.fromPath("/api/v1/sensors/{id}").build(sensor.getId());
        return Response.created(location).entity(sensor).build();
    }
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null)
            return Response.status(Response.Status.NOT_FOUND).entity("{\"message\":\"Sensor not found: " + sensorId + "\"}").build();
        return Response.ok(sensor).build();
    }
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        if (!store.getSensors().containsKey(sensorId))
            throw new NotFoundException("Sensor not found: " + sensorId);
        return new SensorReadingResource(sensorId);
    }
}
