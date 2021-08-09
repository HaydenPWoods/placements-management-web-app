package hpw7.placements.repository;

import hpw7.placements.domain.AppUser;
import hpw7.placements.domain.PlacementVisit;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Provides methods to query data and return records stored in the placement_visit table of the database.
 * See: {@link PlacementVisit}.
 */
public interface PlacementVisitRepository extends CrudRepository<PlacementVisit, Integer> {

    /**
     * Finds and retrieves all placement visits organised by the given user.
     *
     * @param organiser The user organising the visits to return.
     * @return A list of all visits organised by the given user.
     */
    List<PlacementVisit> findAllByOrganiser(AppUser organiser);

    /**
     * Checks whether a placement visit exists in the database with the given organiser, and with a visit date/time
     * between the given date/times.
     *
     * @param appUser   The organising user.
     * @param dateTimeA The start date/time of the range to check.
     * @param dateTimeB The end date/time of the range to check.
     * @return True if a visit exists satisfying the given parameters, false otherwise.
     */
    boolean existsByOrganiserAndVisitDateTimeBetween(AppUser appUser, LocalDateTime dateTimeA, LocalDateTime dateTimeB);

}
