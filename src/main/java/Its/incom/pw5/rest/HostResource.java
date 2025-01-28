package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Host;
import Its.incom.pw5.persistence.model.enums.Type;
import Its.incom.pw5.service.HostService;
import Its.incom.pw5.service.exception.HostAlreadyExistsException;
import Its.incom.pw5.service.exception.HostCreationException;
import Its.incom.pw5.service.exception.HostNotFoundException;
import Its.incom.pw5.service.exception.HostUpdateException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.Map;
import java.util.UUID;


@Path("/host")
public class HostResource {
    //class injection
    @Inject
    HostService hostService;

    //get all hosts
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(){
        try {
            return Response.ok(hostService.getAll()).build();
        } catch (HostNotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }
    //create a new host

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Type type, String hostname, String email, String description){
        try {

            String generatedPassword = UUID.randomUUID().toString().replace("-", "");

            //email verification


            Host host = hostService.create(type, hostname, email, generatedPassword,  description);

            //session creation


            return Response.status(Response.Status.CREATED).entity(host).build();
        } catch (HostAlreadyExistsException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        } catch (HostCreationException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }




    //update an exsisting host
    @PATCH
    @Path("/{id}/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@CookieParam("SESSION_ID") Cookie sessionoCookie, @PathParam("id") ObjectId id, Map<String, Object> updates){
        try{
            String sessionId = sessionoCookie.getValue();
            Host host = hostService.update(sessionId, id, updates);
            return Response.ok().entity(host).build();
        } catch (HostUpdateException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }


    //delete an host
    /*@DELETE
    @Path("{Id}/delete")
    public Response delete(@CookieParam("SESSION_ID") Cookie sessionCookie){
        try {

        } catch ()
    }*/


}
