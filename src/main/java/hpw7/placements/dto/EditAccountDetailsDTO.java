package hpw7.placements.dto;

import javax.validation.constraints.Size;

/**
 * '<code>EditAccountDetailsDTO</code>' is a Data Transfer Object (DTO), with simple fields and constraints, used when a
 * user is making changes to their account details from their profile. Data held by the DTO can be applied to the fields
 * of an actual {@link hpw7.placements.domain.AppUser} entity.
 */
public class EditAccountDetailsDTO {

    /**
     * The updated email address of the user. Must be valid and of the correct format (___@___.___).
     */
    private String email;

    /**
     * The full name of the user.
     */
    @Size(max = 255, message = "Full name cannot be longer than 255 characters!")
    private String fullName;

    /**
     * The updated password for the user, used alongside the username to login to the application.
     */
    private String password;

    /**
     * Whether the user accepts emails being sent by the application to their given email. Used for determining whether
     * to send email notifications. If false, permission has not been granted.
     */
    private boolean emailsEnabled;

    // Constructors

    public EditAccountDetailsDTO(String email, String fullName, String password, boolean emailsEnabled) {
        this.email = email;
        this.fullName = fullName;
        this.password = password;
        this.emailsEnabled = emailsEnabled;
    }

    public EditAccountDetailsDTO() {
        super();
    }

    // Getters and setters, self-explanatory

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEmailsEnabled() {
        return emailsEnabled;
    }

    public void setEmailsEnabled(boolean emailsEnabled) {
        this.emailsEnabled = emailsEnabled;
    }
}
