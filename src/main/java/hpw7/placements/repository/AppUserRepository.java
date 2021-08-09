package hpw7.placements.repository;

import hpw7.placements.domain.AppUser;
import hpw7.placements.domain.AuthorisationRequest;
import hpw7.placements.domain.PlacementApplication;
import hpw7.placements.domain.Role;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * Provides methods to query data and return records stored in the app_user table of the database. See: {@link AppUser}.
 */
public interface AppUserRepository extends PagingAndSortingRepository<AppUser, Integer> {

    /**
     * Returns all users stored in the database.
     *
     * @param sort Sort the results by field and direction (ascending or descending.)
     * @return A sorted list of all users stored in the database.
     */
    List<AppUser> findAll(Sort sort);

    /**
     * Checks for a user stored in the database with the given username, and returns this user if present.
     *
     * @param username The username of the user to find.
     * @return The AppUser with the given username, or null if no user was found.
     */
    AppUser findByUsername(String username);

    /**
     * Checks if a user exists in the database with the given username.
     *
     * @param username The username of the user to find.
     * @return True if a user exists with the given username, false otherwise.
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user exists in the database with the given email.
     *
     * @param email The email of the user to find.
     * @return True if a user exists with the given email, false otherwise.
     */
    boolean existsByEmail(String email);

    /**
     * Checks if any users exist with the given role.
     *
     * @param role The role to find.
     * @return True if at least one user exists with the given role, false otherwise.
     */
    boolean existsByRole(Role role);

    /**
     * Given an authorisation request, finds and retrieves the user that created it.
     *
     * @param authorisationRequest The authorisation request to find and retrieve the creator of.
     * @return The AppUser who created the authorisation request.
     */
    AppUser findByAuthorisationRequestsContaining(AuthorisationRequest authorisationRequest);

    /**
     * Given a placement application, finds and retrieves the user that created it.
     *
     * @param placementApplication The placement application to find and retrieve the creator of.
     * @return The AppUser who created the placement application.
     */
    AppUser findByPlacementApplicationsContaining(PlacementApplication placementApplication);

    /**
     * Retrieves all users with the given role.
     *
     * @param role The role of users to retrieve.
     * @return A list of all users with the given role, or an empty list if no users exist with the given role.
     */
    List<AppUser> findByRole(Role role);

    /**
     * Counts the number of users in the database with the given role.
     *
     * @param role The role to count.
     * @return A count of the number of users with the given role.
     */
    long countByRole(Role role);

}
