package Its.incom.pw5.service;

import Its.incom.pw5.persistence.model.User;
import Its.incom.pw5.persistence.model.UserDetails;
import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.UserStatus;
import Its.incom.pw5.persistence.repository.AuthRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;

@ApplicationScoped
public class AuthService {
    private final AuthRepository authRepository;
    private final HashCalculator hashCalculator;

    public AuthService(AuthRepository authRepository, HashCalculator hashCalculator) {
        this.authRepository = authRepository;
        this.hashCalculator = hashCalculator;
    }


    public void checkNewUserCredentials(User user) {
        String hashedPsw = hashCalculator.calculateHash(user.getHashedPsw());

        if (user.getFirstName() == null || user.getLastName().isBlank() || user.getLastName() == null || user.getFirstName().isBlank() || user.getEmail() == null || user.getEmail().isBlank() || hashedPsw == null || hashedPsw.isBlank()) {
            throw new IllegalArgumentException("Missing required fields");
        }

        User newUser = new User();
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setEmail(user.getEmail());
        newUser.setHashedPsw(hashedPsw);
        newUser.setStatus(UserStatus.UNVERIFIED);
        newUser.setRole(Role.USER);
        UserDetails newUserDetails = new UserDetails();
        newUserDetails.setArchivedEvents(new ArrayList<>());
        newUserDetails.setBookedEvents(new ArrayList<>());
        newUserDetails.setFavouriteTopics(new ArrayList<>());
        newUser.setUserDetails(newUserDetails);

        authRepository.register(newUser);
    }

    public User checkUserCredentials(User user) {
        String hashedPsw = hashCalculator.calculateHash(user.getHashedPsw());
        if (user.getEmail() == null || user.getEmail().isBlank() || hashedPsw == null || hashedPsw.isBlank()) {
            throw new IllegalArgumentException("Missing required fields");
        }

        authRepository.login(user.getEmail(), hashedPsw);
        return user;
    }
}
