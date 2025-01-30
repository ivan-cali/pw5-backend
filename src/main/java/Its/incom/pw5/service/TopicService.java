package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Topic;
import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.UserDetails;
import Its.incom.pw5.persistence.repository.TopicRepository;
import Its.incom.pw5.service.exception.InvalidTopicNameException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TopicService {

    @Inject
    TopicRepository topicRepository;
    @Inject
    UserService userService;

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
            throw new IllegalArgumentException(topic.getName() + " is already a favourite topic.");
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
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Topic not found in the list").build());
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

}
