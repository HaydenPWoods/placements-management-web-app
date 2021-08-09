package hpw7.placements.repository;

import hpw7.placements.domain.DurationToProvider;
import hpw7.placements.domain.Provider;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Provides methods to query data and return records stored in the duration_to_provider table of the database.
 * See: {@link DurationToProvider}.
 */
public interface DurationToProviderRepository extends CrudRepository<DurationToProvider, Integer> {

    /**
     * Finds and retrieves all durations by the destination provider.
     *
     * @param pr2 The destination provider
     * @return A list of all durations to the destination provider.
     */
    List<DurationToProvider> findAllByPr2(Provider pr2);

}
