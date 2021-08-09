package hpw7.placements.repository;

import hpw7.placements.domain.AppUser;
import hpw7.placements.domain.DurationToProvider;
import hpw7.placements.domain.Provider;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

/**
 * Provides methods to query data and return records stored in the provider table of the database.
 * See: {@link Provider}.
 */
public interface ProviderRepository extends PagingAndSortingRepository<Provider, Integer> {

    /**
     * Finds and retrieves the provider where the members list contains the specified user.
     *
     * @param appUser The user.
     * @return An Optional object containing the provider if the given user was a member of a provider, or empty
     * otherwise.
     */
    Optional<Provider> findProviderByMembersContains(AppUser appUser);

    /**
     * Finds and retrieves the provider where the members list contains a user with the given username.
     *
     * @param username The username of the member.
     * @return A Provider object where a member with the given username exists.
     */
    Provider findByMembersUsername(String username);

    /**
     * Checks if a provider exists with the given name in the database.
     *
     * @param name The provider name string to check.
     * @return True if a provider exists with the given name, false otherwise.
     */
    boolean existsByName(String name);

    /**
     * Finds and retrieves the provider with the given name, if a provider with the given name exists.
     *
     * @param name The provider name to find.
     * @return An Optional object containing the provider with the given name if found, or empty otherwise.
     */
    Optional<Provider> findByName(String name);

    /**
     * Retrieves all providers stored in the database.
     *
     * @return A list of all providers stored in the database, sorted by ID in ascending order.
     */
    List<Provider> findAll();

    /**
     * Retrieves all providers stored in the database.
     *
     * @param sort Sort the results by field and direction (ascending or descending.)
     * @return A sorted list of all providers stored in the database.
     */
    List<Provider> findAll(Sort sort);

    /**
     * Finds and retrieves the provider where the TimesToPr2s contains the given DurationToProvider (i.e, gets the
     * provider by the duration to the other provider (pr2) - the provider returned is pr1).
     *
     * @param timeToPr2 The DurationToProvider object to the other provider (pr2).
     * @return An Optional object containing the provider if found, or empty otherwise if there is no match.
     */
    Optional<Provider> findByTimesToPr2sContains(DurationToProvider timeToPr2);

}
