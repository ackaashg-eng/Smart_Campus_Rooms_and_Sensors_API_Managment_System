package com.smartcampus.resource;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {
    private static final Logger logger = Logger.getLogger(RoomResource.class.getName());
    private final DataStore store = DataStore.getInstance();
    @GET
    public Response getAllRooms() {
        logger.info("GET /rooms");
        return Response.ok(new ArrayList<>(store.getRooms().values())).build();
    }
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getName() == null || room.getName().isBlank())
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Room name is required\"}").build();
        if (room.getId() == null || room.getId().isBlank())
            room.setId("ROOM-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        store.getRooms().put(room.getId(), room);
        logger.info("Created room: " + room.getId());
        URI location = UriBuilder.fromPath("/api/v1/rooms/{id}").build(room.getId());
        return Response.created(location).entity(room).build();
    }
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null)
            return Response.status(Response.Status.NOT_FOUND).entity("{\"message\":\"Room not found: " + roomId + "\"}").build();
        return Response.ok(room).build();
    }
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null)
            return Response.status(Response.Status.NOT_FOUND).entity("{\"message\":\"Room not found: " + roomId + "\"}").build();
        if (!room.getSensorIds().isEmpty())
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        store.getRooms().remove(roomId);
        logger.info("Deleted room: " + roomId);
        return Response.noContent().build();
    }
}
