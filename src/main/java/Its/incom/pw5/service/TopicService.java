package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.Topic;
import Its.incom.pw5.persistence.repository.TopicRepository;
import Its.incom.pw5.service.exception.InvalidTopicNameException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TopicService {

    @Inject
    TopicRepository topicRepository;

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
}
