package hpw7.placements.repository;

import hpw7.placements.domain.Document;
import hpw7.placements.domain.Placement;
import hpw7.placements.domain.PlacementApplication;
import hpw7.placements.domain.Provider;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

/**
 * Provides methods to query data and return records stored in the placement_application table of the database.
 * See: {@link PlacementApplication}.
 */
public interface PlacementApplicationRepository extends CrudRepository<PlacementApplication, Integer> {

    /**
     * Counts the number of applications for a given placement.
     *
     * @param placement The placement to count the applications for.
     * @return A count of the number of applications made for the given placement.
     */
    long countAllByPlacement(Placement placement);

    /**
     * Finds and retrieves all applications made for a given placement.
     *
     * @param placement The placement to retrieve all applications for.
     * @return A list of all placement applications made for the given placement.
     */
    List<PlacementApplication> findAllByPlacement(Placement placement);

    /**
     * Finds and retrieves all applications with the given administrator approval status.
     *
     * @param approved The administrator approval status (true/approved or false).
     * @return A list of all placement applications with the given administrator approval status.
     */
    List<PlacementApplication> findAllByApprovedByAdministrator(boolean approved);

    /**
     * Finds and retrieves all applications with the given provider approval status.
     *
     * @param approved The provider approval status (true/approved or false).
     * @return A list of all placement applications with the given provider approval status.
     */
    List<PlacementApplication> findAllByApprovedByProvider(boolean approved);

    /**
     * Finds and retrieves all fully approved placement applications (applications that have been approved by both a
     * member of the provider and an administrator).
     *
     * @return A list of all fully approved placement applications.
     */
    @Query("SELECT a FROM PlacementApplication a WHERE a.approvedByAdministrator = true AND a.approvedByProvider = true")
    List<PlacementApplication> findAllApproved();

    /**
     * Finds and retrieves all placement applications for a given provider's placements that are approved by a member of
     * the provider, but are waiting for approval by an administrator.
     *
     * @param provider The provider of the placements to retrieve the applications for.
     * @return A list of all placement applications for the given provider's placements which are approved by a member
     * of the provider, but are not yet approved by an administrator.
     */
    @Query("SELECT a FROM PlacementApplication a WHERE a.placement.provider = ?1 AND a.approvedByProvider = true AND a.approvedByAdministrator = false")
    List<PlacementApplication> findAllForProviderAwaitingAdminApproval(Provider provider);

    /**
     * Finds and retrieves all placement applications for a given provider's placements that are not yet approved by a
     * member of the provider.
     *
     * @param provider The provider of the placements to retrieve the applications for.
     * @return A list of all placement applications for a given provider's placement which have not yet been approved by
     * a member of the provider.
     */
    @Query("SELECT a FROM PlacementApplication a WHERE a.placement.provider = ?1 AND a.approvedByProvider = false")
    List<PlacementApplication> findAllForProviderApproval(Provider provider);

    /**
     * Finds and retrieves the placement application where the given document is listed as a supporting document of that
     * application.
     *
     * @param document The supporting document.
     * @return An Optional object containing a placement application if the given document is a supporting document for
     * a placement application, or empty otherwise.
     */
    Optional<PlacementApplication> findBySupportingDocsContains(Document document);

}
