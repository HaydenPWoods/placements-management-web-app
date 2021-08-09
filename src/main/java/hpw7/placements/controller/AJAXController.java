package hpw7.placements.controller;

import hpw7.placements.domain.AppUser;
import hpw7.placements.domain.Role;
import hpw7.placements.repository.AppUserRepository;
import hpw7.placements.repository.ProviderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * The <code>AJAXController</code> handles AJAX requests, typically for immediate validation, such as checking if a
 * username or email already exists at registration. Each mapping returns a 200 OK response, with a relevant message
 * that can be used to get the outcome of the check and run JavaScript code when relevant, such as disabling form
 * submission if a check fails.
 */
@RestController
public class AJAXController {

    private final AppUserRepository uRepo;

    private final ProviderRepository prRepo;

    public AJAXController(AppUserRepository uRepo, ProviderRepository prRepo) {
        this.uRepo = uRepo;
        this.prRepo = prRepo;
    }

    /**
     * Given an email string, decodes any special characters lost through UTF_8 encoding and checks for a user that
     * exists with the given email.
     *
     * @param email The email string to check
     * @return 200 OK, with the "Exists" message if the email exists, or "Ok" otherwise.
     */
    @PostMapping("/ajax/email-check")
    public ResponseEntity<String> ajaxEmailCheck(@RequestBody String email) {
        email = URLDecoder.decode(email, StandardCharsets.UTF_8);
        email = email.substring(0, email.length() - 1); // Remove the trailing '='
        String message = "Ok";
        if (uRepo.existsByEmail(email)) {
            message = "Exists";
        }
        return ResponseEntity.ok(message);
    }

    /**
     * Given a username string, decodes the UTF_8 string and checks both if a user exists with the given username, and
     * if they have the (member of) "PROVIDER" role. Additionally checks if this user is already a member of another
     * provider.
     *
     * @param username The username string to check
     * @return 200 OK, with a message:
     * <li> "Ok" if a user with the given username exists, has the "PROVIDER" role, and is not already assigned
     * to a provider.
     * <li> "MEMBER: xxxxxxx" if a user with the given username exists, has the "PROVIDER" role, but is already
     * assigned to another provider. Returns the name of this other provider in the string.
     * <li> "Not found" otherwise.
     */
    @PostMapping("/ajax/provider-member-check")
    public ResponseEntity<String> ajaxProviderMemberCheck(@RequestBody String username) {
        username = URLDecoder.decode(username, StandardCharsets.UTF_8);
        username = username.substring(0, username.length() - 1); // Remove the trailing '='
        String message = "Not found";
        if (uRepo.existsByUsername(username)) {
            if (prRepo.findByMembersUsername(username) != null) {
                // Member of another provider
                message = "MEMBER: " + prRepo.findByMembersUsername(username).getName();
            } else {
                message = "Ok";
            }
        }
        return ResponseEntity.ok(message);
    }

    /**
     * Given a provider name string, decodes the UTF_8 string and checks if a provider exists with the given name.
     *
     * @param providerName The provider name string to check
     * @return 200 OK, with the "Exists" message if a provider with the name exists, or "Ok" otherwise.
     */
    @PostMapping("/ajax/provider-name-check")
    public ResponseEntity<String> ajaxProviderNameCheck(@RequestBody String providerName) {
        providerName = URLDecoder.decode(providerName, StandardCharsets.UTF_8);
        providerName = providerName.substring(0, providerName.length() - 1); // Remove the trailing '='
        String message = "Ok";
        if (prRepo.existsByName(providerName)) {
            message = "Exists";
        }
        return ResponseEntity.ok(message);
    }

    /**
     * Given a username string, decodes the UTF_8 string and checks both if a user exists with the given username, and
     * if they have the "TUTOR" role.
     *
     * @param username The username string to check
     * @return 200 OK, with a message:
     * <li> "Is tutor" if a user with the given username exists, and they have the "TUTOR" role.
     * <li> "Is not tutor" if a user with the given username exists, but they do not have the "TUTOR" role.
     * <li> "Not found" if no user exists with the given username.
     */
    @PostMapping("/ajax/tutor-check")
    public ResponseEntity<String> ajaxTutorCheck(@RequestBody String username) {
        username = URLDecoder.decode(username, StandardCharsets.UTF_8);
        username = username.substring(0, username.length() - 1); // Remove the trailing '='
        String message = "Not found";
        if (uRepo.existsByUsername(username)) {
            AppUser u = uRepo.findByUsername(username);
            if (u.getRole() == Role.TUTOR) {
                message = "Is tutor";
            } else {
                message = "Is not tutor";
            }
        }
        return ResponseEntity.ok(message);
    }

    /**
     * Given a username string, decodes the UTF_8 string and checks if a user exists with the given username (and hence,
     * this username cannot be used to register a new account).
     *
     * @param username The username string to check
     * @return 200 OK, with the "Exists" message if a user with the given username exists, or "Ok" otherwise.
     */
    @PostMapping("/ajax/username-check")
    public ResponseEntity<String> ajaxUsernameCheck(@RequestBody String username) {
        username = URLDecoder.decode(username, StandardCharsets.UTF_8);
        username = username.substring(0, username.length() - 1); // Remove the trailing '='
        String message = "Ok";
        if (uRepo.existsByUsername(username)) {
            message = "Exists";
        }
        return ResponseEntity.ok(message);
    }
}
