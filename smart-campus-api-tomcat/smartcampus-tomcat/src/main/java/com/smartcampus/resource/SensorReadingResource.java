package com.smartcampus.resource;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
public class SensorReadingResource {
    private static final Logger logger = Logger.getLogger(SensorReadingResource.class.getName());
    private final DataStore store = DataStore.getInstance();
    private final String sensorId;
    public SensorReadingResource(String sensorId) { this.sensorId = sensorId; }
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadings() {
        logger.info("GET /sensors/" + sensorId + "/readings");
        return Response.ok(store.getReadingsForSensor(sensorId)).build();
    }
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (!"ACTIVE".equalsIgnoreCase(sensor.getStatus()))
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        if (reading.getId() == null || reading.getId().isBlank())
            reading.setId(UUID.randomUUID().toString());
        if (reading.getTimestamp() == 0)
            reading.setTimestamp(System.currentTimeMillis());
        store.getReadingsForSensor(sensorId).add(reading);
        sensor.setCurrentValue(reading.getValue());
        logger.info("Reading added to " + sensorId + ", currentValue=" + reading.getValue());
        URI location = UriBuilder.fromPath("/api/v1/sensors/{sensorId}/readings/{readingId}").build(sensorId, reading.getId());
        return Response.created(location).entity(reading).build();
    }
}
