package Its.incom.pw5.rest;

import Its.incom.pw5.persistence.model.Topic;
import Its.incom.pw5.service.TopicService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

import java.util.List;
import java.util.Map;

@Path("/topic")
public class TopicResource {

    private final TopicService topicService;

    public TopicResource(TopicService topicService) {
        this.topicService = topicService;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = "api_calls_total", description = "Total number of API calls")
    @Timed(name = "api_call_duration", description = "Time taken to process API calls")
    public Response getTopics(@QueryParam("name") String topicName) {

        if (topicName == null) {
            // Return all the existing topics
            List<Topic> topicList = topicService.getAllTopics();

            Map<String, Object> responseBody = Map.of(
                    "message", "Topics retrieved successfully.",
                    "topics", topicList
            );

            return Response.ok(responseBody)
                    .build();
        } else {
            // Return the topic with the specified name
            Topic topic = topicService.findTopicByName(topicName);

            if (topic == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Topic with name " + topicName + " not found."))
                        .build();
            }

            Map<String, Object> responseBody = Map.of(
                    "message", "Topic retrieved successfully.",
                    "topic", topic
            );

            return Response.ok(responseBody)
                    .build();
        }
    }
}