package hpw7.placements.repository;

import hpw7.placements.domain.AppUser;
import hpw7.placements.domain.LogAction;
import hpw7.placements.domain.LogEntity;
import hpw7.placements.domain.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Provides methods to query data and return records stored in the log_entry table of the database.
 * See: {@link LogEntry}.
 */
public interface LogEntryRepository extends PagingAndSortingRepository<LogEntry, Integer> {

    /**
     * Retrieves all log entries stored in the database.
     *
     * @param sort Sort the results by field and direction (ascending or descending.)
     * @return A sorted list of all log entries stored in the database.
     */
    List<LogEntry> findAll(Sort sort);

    /**
     * Finds and retrieves all log entries for the given user.
     *
     * @param user The user to return all log entries for.
     * @return A list of all log entries assigned to the given user, sorted by ID in ascending order.
     */
    List<LogEntry> findAllByAppUser(AppUser user);

    /**
     * Finds and retrieves all log entries for the given user.
     *
     * @param user The user to return all log entries for.
     * @param sort Sort the results by field and direction (ascending or descending.)
     * @return A sorted list of all log entries assigned to the given user.
     */
    List<LogEntry> findAllByAppUser(AppUser user, Sort sort);

    /**
     * Finds and retrieves a page of log entries for the given user.
     *
     * @param user     The user to return all log entries for.
     * @param pageable The length of page to return, and any other pagination options, including sorting.
     * @return A page (optionally sorted) of log entries assigned to the given user.
     */
    Page<LogEntry> findAllByAppUser(AppUser user, Pageable pageable);

    /**
     * Finds and retrieves all log entries matching the given entity type and entity ID.
     *
     * @param entityType The type of entity, as {@link LogEntity}.
     * @param entityId   The ID of the entity to return log entries for.
     * @param sort       Sort the results by field and direction (ascending or descending.)
     * @return A sorted list of all log entries for the given entity identified by type and ID.
     */
    List<LogEntry> findAllByEntityTypeAndEntityId(LogEntity entityType, int entityId, Sort sort);

    /**
     * Finds and retrieves a page of log entries matching the given entity type and entity ID.
     *
     * @param entityType The type of entity, as {@link LogEntity}.
     * @param entityId   The ID of the entity to return log entries for.
     * @param pageable   The length of page to return, and any other pagination options, including sorting.
     * @return A page (optionally sorted) of log entries for the given entity identified by type and ID.
     */
    Page<LogEntry> findAllByEntityTypeAndEntityId(LogEntity entityType, int entityId, Pageable pageable);

    /**
     * Finds and retrieves a page of log entries with the given action type.
     *
     * @param logAction The type of action, as {@link LogAction}.
     * @param pageable  The length of page to return, and any other pagination options, including sorting.
     * @return A page (optionally sorted) of log entries with the given action type.
     */
    Page<LogEntry> findAllByActionType(LogAction logAction, Pageable pageable);

    /**
     * Finds and retrieves a page of log entries with the given entity type.
     *
     * @param logEntity The type of entity, as {@link LogEntity}.
     * @param pageable  The length of page to return, and any other pagination options, including sorting.
     * @return A page (optionally sorted) of log entries with the given entity type.
     */
    Page<LogEntry> findAllByEntityType(LogEntity logEntity, Pageable pageable);

    /**
     * Finds and retrieves a page of log entries for the given user with the given action type.
     *
     * @param user      The user to return log entries for.
     * @param logAction The type of action, as {@link LogAction}.
     * @param pageable  The length of page to return, and any other pagination options, including sorting.
     * @return A page (optionally sorted) of log entries assigned to the given user with the given action type.
     */
    Page<LogEntry> findAllByAppUserAndActionType(AppUser user, LogAction logAction, Pageable pageable);

    /**
     * Finds and retrieves a page of log entries for the given user with the given entity type.
     *
     * @param user      The user to return log entries for.
     * @param logEntity The type of entity, as {@link LogEntity}.
     * @param pageable  The length of page to return, and any other pagination options, including sorting.
     * @return A page (optionally sorted) of log entries assigned to the given user with the given entity type.
     */
    Page<LogEntry> findAllByAppUserAndEntityType(AppUser user, LogEntity logEntity, Pageable pageable);

    /**
     * Finds and retrieves a page of log entries with the given action type and entity type.
     *
     * @param logAction The type of action, as {@link LogAction}.
     * @param logEntity The type of entity, as {@link LogEntity}.
     * @param pageable  The length of page to return, and any other pagination options, including sorting.
     * @return A page (optionally sorted) of log entries with the given action type and entity type.
     */
    Page<LogEntry> findAllByActionTypeAndEntityType(LogAction logAction, LogEntity logEntity, Pageable pageable);

    /**
     * Finds and retrieves a page of log entries for the given user with the given action type and entity type.
     *
     * @param user      The user to return log entries for.
     * @param logAction The type of action, as {@link LogAction}.
     * @param logEntity The type of entity, as {@link LogEntity}.
     * @param pageable  The length of page to return, and any other pagination options, including sorting.
     * @return A page (optionally sorted) of log entries assigned to the given user with the given action type and
     * entity type.
     */
    Page<LogEntry> findAllByAppUserAndActionTypeAndEntityType(AppUser user, LogAction logAction, LogEntity logEntity, Pageable pageable);

    /**
     * Finds and retrieves a page of log entries with the given action type, entity type, and entity ID.
     *
     * @param logAction  The type of action, as {@link LogAction}.
     * @param entityType The type of entity, as {@link LogEntity}.
     * @param entityId   The ID of the entity to return log entries for.
     * @param pageable   The length of page to return, and any other pagination options, including sorting.
     * @return A page (optionally sorted) of log entries with the given action type, entity type, and entity ID.
     */
    Page<LogEntry> findAllByActionTypeAndEntityTypeAndEntityId(LogAction logAction, LogEntity entityType, int entityId, Pageable pageable);

    /**
     * Counts the number of log entries assigned to a given user.
     *
     * @param user The user to count log entries for.
     * @return A count of the number of log entries assigned to the given user.
     */
    long countByAppUser(AppUser user);

    /**
     * Counts the number of log entries with the given action type.
     *
     * @param actionType The type of action, as {@link LogAction}.
     * @return A count of the number of log entries with the given action type.
     */
    long countByActionType(LogAction actionType);

    /**
     * Counts the number of log entries with the given entity type.
     *
     * @param entityType The type of entity, as {@link LogEntity}.
     * @return A count of the number of log entries with the given entity type.
     */
    long countByEntityType(LogEntity entityType);

    /**
     * Counts the number of log entries with the given action type and entity type.
     *
     * @param logAction The type of action, as {@link LogAction}.
     * @param logEntity The type of entity, as {@link LogEntity}.
     * @return A count of the number of log entries with the given action type and entity type.
     */
    long countByActionTypeAndEntityType(LogAction logAction, LogEntity logEntity);

    /**
     * Counts the number of log entries with the given entity type, matching the given entity ID.
     *
     * @param entityType The type of entity, as {@link LogEntity}.
     * @param entityId   The ID of the entity to count log entries for.
     * @return A count of the number of log entries with the given entity type and matching entity ID.
     */
    long countByEntityTypeAndEntityId(LogEntity entityType, int entityId);

    /**
     * Counts the number of log entries assigned to the given user with the given action type.
     *
     * @param user       The user to count log entries for.
     * @param actionType The type of action, as {@link LogAction}.
     * @return A count of the number of log entries assigned to the given user with the given action type.
     */
    long countByAppUserAndActionType(AppUser user, LogAction actionType);

    /**
     * Counts the number of log entries assigned to the given user with the given entity type.
     *
     * @param user      The user to count log entries for.
     * @param logEntity The type of entity, as {@link LogEntity}.
     * @return A count of the number of log entries assigned to the given user with the given entity type.
     */
    long countByAppUserAndEntityType(AppUser user, LogEntity logEntity);

    /**
     * Counts the number of log entries assigned to the given user with the given action type and entity type.
     *
     * @param user      The user to count log entries for.
     * @param logAction The type of action, as {@link LogAction}.
     * @param logEntity The type of entity, as {@link LogEntity}.
     * @return A count of the number of log entries assigned to the given user with the given action type and entity
     * type.
     */
    long countByAppUserAndActionTypeAndEntityType(AppUser user, LogAction logAction, LogEntity logEntity);

    /**
     * Counts the number of log entries with the given action and entity types, matching the given entity ID.
     *
     * @param actionType The type of action, as {@link LogAction}.
     * @param entityType The type of entity, as {@link LogEntity}.
     * @param entityId   The ID of the entity to count log entries for.
     * @return A count of the number of log entries with the given action and entity types, and matching entity ID.
     */
    long countByActionTypeAndEntityTypeAndEntityId(LogAction actionType, LogEntity entityType, int entityId);

    /**
     * Counts the number of log entries assigned to the given user with the given action and entity types, matching the
     * given entity ID.
     *
     * @param user       The user to count log entries for.
     * @param actionType The type of action, as {@link LogAction}.
     * @param entityType The type of entity, as {@link LogEntity}.
     * @param entityId   The ID of the entity to count log entries for.
     * @return A count of the number of log entries assigned to the given user with the given action and entity types,
     * and matching entity ID.
     */
    long countByAppUserAndActionTypeAndEntityTypeAndEntityId(AppUser user, LogAction actionType, LogEntity entityType, int entityId);

    /**
     * Checks whether a log entry exists assigned to the given user with a given action and entity type, matching a
     * given description, after a given time. (This is specifically used to check if a 'New message received!' email
     * notification was sent to the user in the last 12 hours, and block sending the message if so.)
     *
     * @param user        The user to check log entries for.
     * @param action      The type of action, as {@link LogAction}.
     * @param entity      The type of entity, as {@link LogEntity}.
     * @param timestamp   Checking log entries after the given time, as {@link LocalDateTime}.
     * @param description The description of the log entry.
     * @return True if a log entry exists satisfying the given parameters, false otherwise.
     */
    boolean existsByAppUserAndActionTypeAndEntityTypeAndTimestampAfterAndDescriptionLike(AppUser user, LogAction action, LogEntity entity, LocalDateTime timestamp, String description);

}
