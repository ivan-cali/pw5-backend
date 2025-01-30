package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Topic;
import Its.incom.pw5.service.TopicService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/topic")
public class TopicResource {

    @Inject
    TopicService topicService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopics(@QueryParam("name") String topicName) {

        if (topicName == null) {
            // Return all the existing topics
            List<Topic> topicList = topicService.getAllTopics();
            return Response.ok().entity(topicList).build();
        } else {
            // Return the topic with the specified name
            Topic topic = topicService.findTopicByName(topicName);
            return Response.ok().entity(topic).build();
        }
    }
}
