package hpw7.placements.repository;

import hpw7.placements.domain.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Provides methods to query data and return records stored in the placement table of the database.
 * See: {@link Placement}.
 */
public interface PlacementRepository extends PagingAndSortingRepository<Placement, Integer> {

    /**
     * Counts the number of placements in the database where the application deadline is after the given date/time.
     *
     * @param localDateTime The lower bound date/time for the application deadline.
     * @return A count of the number of placements where the application deadline is after the given date/time.
     */
    long countAllByApplicationDeadlineAfter(LocalDateTime localDateTime);

    /**
     * Counts the number of placements in the database where the application deadline is before the given date/time.
     *
     * @param localDateTime The upper bound date/time for the application deadline.
     * @return A count of the number of placements where the application deadline is before the given date/time.
     */
    long countAllByApplicationDeadlineBefore(LocalDateTime localDateTime);

    /**
     * Retrieves all placements stored in the database.
     *
     * @return A list of all placements stored in the database, sorted by ID in ascending order.
     */
    List<Placement> findAll();

    /**
     * Retrieves all placements stored in the database.
     *
     * @param sort Sort the results by field and direction (ascending or descending.)
     * @return A sorted list of all placements stored in the database.
     */
    List<Placement> findAll(Sort sort);

    /**
     * Finds and retrieves all placements offered by the given provider.
     *
     * @param provider The provider of the placements to retrieve.
     * @return A list of all placements offered by the given provider, sorted by ID in ascending order.
     */
    List<Placement> findAllByProvider(Provider provider);

    /**
     * Finds and retrieves all placements offered by the given provider.
     *
     * @param provider The provider of the placements to retrieve.
     * @param sort     Sort the results by field and direction (ascending or descending.)
     * @return A sorted list of all placements offered by the given provider.
     */
    List<Placement> findAllByProvider(Provider provider, Sort sort);

    /**
     * Finds and retrieves all placements where the given user is a member.
     *
     * @param appUser The member of the placements to retrieve.
     * @return A list of all placements where the given user is a member, sorted by ID in ascending order.
     */
    List<Placement> findAllByMembersContains(AppUser appUser);

    /**
     * Finds and retrieves all placements where the given user is a member.
     *
     * @param appUser The member of the placements to retrieve.
     * @param sort    Sort the results by field and direction (ascending or descending.)
     * @return A sorted list of all placements where the given user is a member.
     */
    List<Placement> findAllByMembersContains(AppUser appUser, Sort sort);

    /**
     * Finds and retrieves all placements offered by a provider where the given user is a member.
     *
     * @param provider The provider of the placements to consider.
     * @param appUser  The member of the placements to retrieve.
     * @param sort     Sort the results by field and direction (ascending or descending.)
     * @return A sorted list of all placements offered by the given provider, where the given user is a member.
     */
    List<Placement> findAllByProviderAndMembersContains(Provider provider, AppUser appUser, Sort sort);

    /**
     * Finds and retrieves all placements where the application deadline is after the given date/time.
     *
     * @param applicationDeadline The lower bound date/time for the application deadline.
     * @param sort                Sort the results by field and direction (ascending or descending.)
     * @return A sorted list of all placements where the application deadline is after the given date/time.
     */
    List<Placement> findAllByApplicationDeadlineAfter(LocalDateTime applicationDeadline, Sort sort);

    /**
     * Retrieves the placement which the given visit is scheduled for.
     *
     * @param visit The visit to return the placement for.
     * @return The placement for which the given visit is scheduled for.
     */
    Placement findByVisitsContains(PlacementVisit visit);

    /**
     * Finds and retrieves all placements where no visits are scheduled, the end date of the placement is after the
     * given date, and the given user is a member of the placement.
     *
     * @param appUser The member of the placement(s).
     * @param endDate The lower bound date/time for the end date of the placement.
     * @return A list of all placements where no visits are scheduled, the end date of the placement is after the given
     * date, and the given user is a member of the placement.
     */
    List<Placement> findAllByMembersContainsAndEndDateAfterAndVisitsIsNull(AppUser appUser, LocalDate endDate);

    /**
     * Finds and retrieves the placement associated with a document.
     *
     * @param document The document object to check.
     * @return An Optional object containing a placement if the given document is uploaded in relation to a placement,
     * or empty otherwise.
     */
    Optional<Placement> findByDocumentsContains(Document document);

    /**
     * Searches and returns all placements where there is a match with the given query in the placement title or
     * description, or provider name. This relies on full_text_search index on the placement and provider tables
     * in the MySQL database. This has been created for the database in use (departmental MySQL server - hpw7), but
     * Spring does not create this when instantiating the tables for the first time - it must be done manually.
     *
     * @param query The query to search for.
     * @return A list of all placements with a query match.
     */
    @Query(value = "SELECT DISTINCT placement.* FROM placement, provider WHERE MATCH (placement.title, placement.description) AGAINST (?1) " +
            "OR placement.provider_id IN (SELECT DISTINCT provider.id FROM provider WHERE MATCH (provider.name) AGAINST (?1))", nativeQuery = true)
    List<Placement> search(String query);

    /**
     * Searches and returns all placements where there is a match with the given query in the placement title or
     * description, for placements from a given provider. This relies on full_text_search index on the placement and
     * provider tables in the MySQL database. This has been created for the database in use (departmental MySQL server -
     * hpw7), but Spring does not create this when instantiating the tables for the first time - it must be done
     * manually.
     *
     * @param query The query to search for.
     * @param providerId The ID of the provider to search placements for.
     * @return A list of all placements with a query match for the given provider.
     */
    @Query(value = "SELECT * FROM placement WHERE MATCH (title, description) AGAINST (?1) AND provider_id = ?2",
            nativeQuery = true)
    List<Placement> searchProvider(String query, int providerId);

}
