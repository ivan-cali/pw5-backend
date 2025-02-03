package Its.incom.pw5.service;

import Its.incom.pw5.interceptor.GlobalLog;
import Its.incom.pw5.persistence.model.Topic;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.UserDetails;
import Its.incom.pw5.persistence.repository.TopicRepository;
import Its.incom.pw5.service.exception.InvalidTopicNameException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@GlobalLog
@ApplicationScoped
public class TopicService {
    private final TopicRepository topicRepository;
    private final UserService userService;

    public TopicService(TopicRepository topicRepository, UserService userService) {
        this.topicRepository = topicRepository;
        this.userService = userService;
    }

    public Topic findOrCreateTopic(String topicName) {
        if (topicName == null || topicName.trim().isEmpty()) {
            throw new InvalidTopicNameException("Topic name cannot be null or empty");
        }

        // Convert to lower case to standardize
        String lowerCaseName = topicName.toLowerCase();

        Topic existing = topicRepository.findByName(lowerCaseName);
        if (existing == null) {
            Topic newTopic = new Topic();
            newTopic.setName(lowerCaseName);
            topicRepository.persist(newTopic);
            return newTopic;
        }

        return existing;
    }

    public Topic getTopicById(ObjectId topicId) {
        return topicRepository.findTopicById(topicId);
    }

    //Add a topic to user favourite topic list
    public void addFavouriteTopic(User user, Topic topic) {

        //get the user details
        UserDetails userDetails = user.getUserDetails();
        //initialize them if value is null
        if (userDetails == null) {
            userDetails = new UserDetails();
        }

        //get the list of favourite topics of the user
        List<Topic> favouriteTopics = userDetails.getFavouriteTopics();
        //initialize them if the value is null
        if (favouriteTopics == null) {
            favouriteTopics = new ArrayList<>();
        }

        //check if the topic to add in favourite topic list doesn't already exist
        if (topicRepository.isAlreadyAFavourite(favouriteTopics, topic.getId())) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Topic already exists in the favourite list"))
                    .build());
        }

        //update the user with the new favourite topic list
        favouriteTopics.add(topic);
        userDetails.setFavouriteTopics(favouriteTopics);
        user.setUserDetails(userDetails);
        userService.updateUser(user);
    }

    public void removeFavouriteTopic(User user, Topic topic) {
        // Check if the topic provided exist in the user favouriteTopics list
        boolean isAlreadyAFavourite = topicRepository.isAlreadyAFavourite(user.getUserDetails().getFavouriteTopics(), topic.getId());
        if (!isAlreadyAFavourite) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Topic not found in the favourite list"))
                    .build());
        }

        // Update favourite topics list
        List<Topic> userFavouriteTopics = user.getUserDetails().getFavouriteTopics();
        for (Topic t : userFavouriteTopics) {
            if (t.getId().equals(topic.getId())) {
                userFavouriteTopics.remove(t);
                break;
            }
        }
        user.getUserDetails().setFavouriteTopics(userFavouriteTopics);
        userService.updateUser(user);
    }

    public Topic findTopicByName(String topicName) {
        return topicRepository.findByName(topicName);
    }

    public List<Topic> getAllTopics() {
        return topicRepository.getAll();
    }
}
