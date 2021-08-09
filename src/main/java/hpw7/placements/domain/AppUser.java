package hpw7.placements.domain;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * '<code>AppUser</code>' is an entity for all the different users of the web application. <code>AppUser</code>s are
 * able to login to the application with their unique <code>username</code> and <code>password</code>, which will allow
 * them to access information relevant to themselves, according to their <code>role</code> and whether they are a member
 * of a {@link Provider} or a {@link Placement}.
 */
@Entity
public class AppUser {

    /**
     * A unique username to login with and identify the user. Used as the ID.
     */
    @Id
    private String username;

    /**
     * The full name of the user.
     */
    @Column(nullable = false)
    private String fullName;

    /**
     * The email address of the user. Must be valid and of the correct format (___@___.___).
     */
    @Column(nullable = false)
    private String email;

    /**
     * The password for the user, used alongside the username to login to the application.
     */
    @Column(nullable = false)
    private String password;

    /**
     * The role assigned to the user to designate permissions and access to different areas of the application. Must be
     * a role as defined in {@link Role}.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * The authorisation requests that the user (as a student) has created.
     */
    @OneToMany
    @JoinColumn(name = "app_user_username", nullable = false)
    private List<AuthorisationRequest> authorisationRequests = new ArrayList<>();

    /**
     * The placement applications that the user (as a student) has created.
     */
    @OneToMany
    @JoinColumn(name = "app_user_username", nullable = false)
    private List<PlacementApplication> placementApplications = new ArrayList<>();

    /**
     * When the user accepts notifications for new messages, a device token is stored for the user. This is null,
     * otherwise. Used for Firebase Cloud Messaging.
     */
    private String deviceToken;

    /**
     * The location of the user's profile picture on the local filesystem, as a string. Null if the user hasn't set a
     * custom image (a default will be used).
     */
    private String profilePicture;

    /**
     * Whether the user accepts emails being sent by the application to their given email. Used for determining whether
     * to send email notifications. If false, permission has not been granted.
     */
    @Column(nullable = false)
    private boolean emailsEnabled;

    // Constructors

    public AppUser() {
        super();
    }

    public AppUser(String username, String fullName, String email, String password, Role role) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.emailsEnabled = true;
    }

    // Getters and setters for entity fields, self-explanatory

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<AuthorisationRequest> getAuthorisationRequests() {
        return authorisationRequests;
    }

    public void setAuthorisationRequests(List<AuthorisationRequest> authorisationRequests) {
        this.authorisationRequests = authorisationRequests;
    }

    public List<PlacementApplication> getPlacementApplications() {
        return placementApplications;
    }

    public void setPlacementApplications(List<PlacementApplication> placementApplications) {
        this.placementApplications = placementApplications;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public boolean isEmailsEnabled() {
        return emailsEnabled;
    }

    public void setEmailsEnabled(boolean emailsEnabled) {
        this.emailsEnabled = emailsEnabled;
    }
}
