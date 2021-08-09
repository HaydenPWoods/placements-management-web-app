package hpw7.placements.repository;

import hpw7.placements.domain.AppUser;
import hpw7.placements.domain.AuthorisationRequest;
import hpw7.placements.domain.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

/**
 * Provides methods to query data and return records stored in the authorisation_request table of the database.
 * See: {@link AuthorisationRequest}.
 */
public interface AuthorisationRequestRepository extends CrudRepository<AuthorisationRequest, Integer> {

    /**
     * Counts the number of authorisation requests in the database based on the approval status.
     *
     * @param tutorApproved If the authorisation request is approved by the assigned tutor.
     * @param adminApproved If the authorisation request is approved by an administrator.
     * @return A count of the number of authorisation requests with the given approval status pair.
     */
    long countByApprovedByTutorAndApprovedByAdministrator(boolean tutorApproved, boolean adminApproved);

    /**
     * Retrieves all authorisation requests stored in the database.
     *
     * @return A list of all authorisation requests stored in the database, sorted by ID in ascending order.
     */
    List<AuthorisationRequest> findAll();

    /**
     * Retrieves all authorisation requests stored in the database.
     *
     * @param sort Sort the results by field and direction (ascending or descending.)
     * @return A sorted list of all authorisation requests stored in the database.
     */
    List<AuthorisationRequest> findAll(Sort sort);

    /**
     * Finds and retrieves all authorisation requests where the given user is the assigned tutor.
     *
     * @param tutor The assigned tutor.
     * @return A list of all authorisation requests with the given user as the assigned tutor.
     */
    List<AuthorisationRequest> findAllByTutor(AppUser tutor);

    /**
     * Finds and retrieves all authorisation requests with the given administrator approval status.
     *
     * @param approved Whether the returned authorisation request(s) are approved by the administrator.
     * @return A list of all authorisation requests with the given administrator approval status.
     */
    List<AuthorisationRequest> findAllByApprovedByAdministrator(boolean approved);

    /**
     * Finds and retrieves all authorisation requests with the given tutor approval status.
     *
     * @param approved Whether the returned authorisation request(s) are approved by the assigned tutor.
     * @return A list of all authorisation requests with the given tutor approval status.
     */
    List<AuthorisationRequest> findAllByApprovedByTutor(boolean approved);

    /**
     * Finds and retrieves all authorisation requests with the given approval status pair, by assigned tutor and
     * administrator.
     *
     * @param tutorApproved Whether the returned authorisation request(s) are approved by the assigned tutor.
     * @param adminApproved Whether the returned authorisation request(s) are approved by the administrator.
     * @return A list of all authorisation requests with the given approval status pair.
     */
    List<AuthorisationRequest> findAllByApprovedByTutorAndApprovedByAdministrator(boolean tutorApproved, boolean adminApproved);

    /**
     * Finds and retrieves all authorisation requests with a given user as the assigned tutor, and the current status of
     * tutor approval.
     *
     * @param tutor    The assigned tutor.
     * @param approved Whether the returned authorisation request(s) are approved by the assigned tutor.
     * @return A list of all authorisation requests with the given user as the assigned tutor, and the given tutor
     * approval status.
     */
    List<AuthorisationRequest> findAllByTutorAndApprovedByTutor(AppUser tutor, boolean approved);

    /**
     * Finds and retrieves all authorisation requests with a given user as the assigned tutor, and the current status of
     * administrator approval.
     *
     * @param tutor    The assigned tutor.
     * @param approved Whether the returned authorisation request(s) are approved by an administrator.
     * @return A list of all authorisation requests with the given user as the assigned tutor, and the given
     * administrator approval status.
     */
    List<AuthorisationRequest> findAllByTutorAndApprovedByAdministrator(AppUser tutor, boolean approved);

    /**
     * Finds and retrieves the authorisation request where the given document is listed as a supporting document of that
     * request.
     *
     * @param document The supporting document.
     * @return An Optional object containing an authorisation request if the given document is a supporting document for
     * an authorisation request, or empty otherwise.
     */
    Optional<AuthorisationRequest> findBySupportingDocsContains(Document document);

    // Relies on full_text_search index on the authorisation_request table in the MySQL database
    // Spring does not create this when instantiating the tables for the first time, it must be done manually!

    /**
     * Searches and returns all authorisation requests where there is a match with the given query in the title,
     * provider name, details, and address fields. This relies on full_text_search index on the authorisation_request
     * table in the MySQL database. This has been created for the database in use (departmental MySQL server - hpw7),
     * but Spring does not create this when instantiating the tables for the first time - it must be done manually.
     *
     * @param query The query to search for.
     * @return A list of all authorisation requests with a query match.
     */
    @Query(value = "SELECT * FROM authorisation_request WHERE " +
            "MATCH (title, provider_name, details, provider_address_line1, provider_address_line2, " +
            "provider_address_city, provider_address_postcode) AGAINST (?1)", nativeQuery = true)
    List<AuthorisationRequest> search(String query);

    /**
     * Searches and returns all authorisation requests where there is a match with the given query in the title,
     * provider name, details, and address fields, and where the request is created by a user with the given username.
     * This relies on full_text_search index on the authorisation_request table in the MySQL database. This has been
     * created for the database in use (departmental MySQL server - hpw7), but Spring does not create this when
     * instantiating the tables for the first time - it must be done manually.
     *
     * @param query           The query to search for.
     * @param studentUsername The username of the student as the creator of any authorisation requests.
     * @return A list of all authorisation requests by the given student, and with a query match.
     */
    @Query(value = "SELECT * FROM authorisation_request WHERE " +
            "MATCH (title, provider_name, details, provider_address_line1, provider_address_line2, " +
            "provider_address_city, provider_address_postcode) AGAINST (?1) AND app_user_username = ?2", nativeQuery = true)
    List<AuthorisationRequest> searchStudent(String query, String studentUsername);


    /**
     * Searches and returns all authorisation requests where there is a match with the given query in the title,
     * provider name, details, and address fields, and where the request has an assigned tutor with the given username.
     * This relies on full_text_search index on the authorisation_request table in the MySQL database. This has been
     * created for the database in use (departmental MySQL server - hpw7), but Spring does not create this when
     * instantiating the tables for the first time - it must be done manually.
     *
     * @param query         The query to search for.
     * @param tutorUsername The username of the assigned tutor.
     * @return A list of all authorisation requests with the given assigned tutor, and with a query match.
     */
    @Query(value = "SELECT * FROM authorisation_request WHERE " +
            "MATCH (title, provider_name, details, provider_address_line1, provider_address_line2, " +
            "provider_address_city, provider_address_postcode) AGAINST (?1) AND tutor_username = ?2", nativeQuery = true)
    List<AuthorisationRequest> searchTutor(String query, String tutorUsername);

    /**
     * Searches and returns all authorisation requests where there is a match with the given query in the title,
     * provider name, details, and address fields, and where the request is for a placement with a provider with the
     * given name. This relies on full_text_search index on the authorisation_request table in the MySQL database. This
     * has been created for the database in use (departmental MySQL server - hpw7), but Spring does not create this when
     * instantiating the tables for the first time - it must be done manually.
     *
     * @param query        The query to search for.
     * @param providerName The name of the provider.
     * @return A list of all authorisation requests with a provider name matching the given provider name, and with a
     * query match.
     */
    @Query(value = "SELECT * FROM authorisation_request WHERE " +
            "MATCH (title, provider_name, details, provider_address_line1, provider_address_line2, " +
            "provider_address_city, provider_address_postcode) AGAINST (?1) AND provider_name = ?2", nativeQuery = true)
    List<AuthorisationRequest> searchProvider(String query, String providerName);

}
