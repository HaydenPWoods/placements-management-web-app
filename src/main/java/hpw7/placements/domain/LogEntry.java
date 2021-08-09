package hpw7.placements.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * An entity to keep a record for individual actions completed or otherwise prompted by an individual user. Identifiable
 * by an action and entity pair. An entity ID can be stored in a log entry, related to the entity type, but this can be
 * left blank in situations where an entity ID is irrelevant or doesn't exist.
 */
@Entity
public class LogEntry {

    /**
     * The unique ID of the log entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * The user whose action is being logged.
     */
    @ManyToOne
    @JoinColumn(name = "app_user", nullable = false)
    private AppUser appUser;

    /**
     * The timestamp of the log. Should be set to the current time (LocalDateTime.now()) when creating a new log entry.
     */
    @Column(nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;

    /**
     * The type of action being logged, as defined in {@link LogAction}.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogAction actionType;

    /**
     * The entity type that the action is related to, as defined in {@link LogEntity}.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogEntity entityType;

    /**
     * The ID of the entity. Can be used to identify the specific entity the log entry regards when paired with the
     * entityType. Optional.
     */
    private int entityId;

    /**
     * A short description of the activity being logged.
     */
    @Column(nullable = false)
    private String description;

    // Constructors

    public LogEntry(AppUser appUser, LocalDateTime timestamp, LogAction actionType, LogEntity entityType, int entityId, String description) {
        this.appUser = appUser;
        this.timestamp = timestamp;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
    }

    public LogEntry(AppUser appUser, LocalDateTime timestamp, LogAction actionType, LogEntity entityType, String description) {
        this.appUser = appUser;
        this.timestamp = timestamp;
        this.actionType = actionType;
        this.entityType = entityType;
        this.description = description;
    }

    public LogEntry() {
        super();
    }

    // Getters and setters for entity fields, self-explanatory

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public AppUser getAppUser() {
        return appUser;
    }

    public void setAppUser(AppUser appUser) {
        this.appUser = appUser;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LogAction getActionType() {
        return actionType;
    }

    public void setActionType(LogAction actionType) {
        this.actionType = actionType;
    }

    public LogEntity getEntityType() {
        return entityType;
    }

    public void setEntityType(LogEntity entityType) {
        this.entityType = entityType;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
