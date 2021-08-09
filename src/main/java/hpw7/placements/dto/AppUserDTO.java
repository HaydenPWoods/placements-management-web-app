package hpw7.placements.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * '<code>AppUserDTO</code>' is a Data Transfer Object (DTO), with simple fields and constraints, used when accounts are
 * registered through the Register form. Data held by the DTO can be used in the creation of an actual
 * {@link hpw7.placements.domain.AppUser} entity.
 */
public class AppUserDTO {

    /**
     * A unique username to login with and identify the user. Used as the ID.
     */
    @NotBlank(message = "Username cannot be blank!")
    @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters!")
    private String username;

    /**
     * The full name of the user.
     */
    @NotBlank(message = "Full name cannot be blank!")
    @Size(max = 255, message = "Full name cannot be longer than 255 characters!")
    private String fullName;

    /**
     * The email address of the user. Must be valid and of the correct format (___@___.___).
     */
    @NotBlank(message = "Email cannot be blank!")
    private String email;

    /**
     * The password for the user, used alongside the username to login to the application.
     */
    @NotBlank(message = "Password cannot be blank!")
    @Size(min = 8, message = "Password must be a minimum of 8 characters long!")
    private String password;

    /**
     * The role assigned to the user to designate permissions and access to different areas of the application. Must be
     * the name of a role, as defined in {@link hpw7.placements.domain.Role} (excluding ADMINISTRATOR and GRADUATE).
     */
    @NotBlank(message = "Role must be selected!")
    private String role;

    // Constructors

    public AppUserDTO(String username, String fullName, String email, String password, String role) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public AppUserDTO() {
        super();
    }

    // Getters and setters, self-explanatory

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
