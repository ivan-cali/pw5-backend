package Its.incom.pw5.persistence.model;

import Its.incom.pw5.persistence.model.enums.Role;
import Its.incom.pw5.persistence.model.enums.UserStatus;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

@MongoEntity(collection = "user")
public class User {
    private ObjectId id;
    private String firstName;
    private String lastName;
    private String email;
    private String hashedPsw;
    private UserStatus status;
    private Role role;
    private UserDetails userDetails;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHashedPsw() {
        return hashedPsw;
    }

    public void setHashedPsw(String hashedPsw) {
        this.hashedPsw = hashedPsw;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserDetails getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }


}
