package hpw7.placements.domain;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * '<code>Placement</code>' is an entity for an individual placement offering from a {@link Provider}. {@link AppUser}s
 * associated with a placement can communicate and interact with one-another.
 */
@Entity
public class Placement {

    /**
     * The unique ID of the placement. Automatically generated.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * The title of the placement.
     */
    @Column(nullable = false)
    private String title;

    /**
     * A description of the placement offer.
     */
    @Column(nullable = false)
    @Lob
    private String description;

    /**
     * The closing time for application submissions for this placement offer.
     */
    @Column(nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime applicationDeadline;

    /**
     * The start date of the placement.
     */
    @Column(nullable = false, columnDefinition = "DATE")
    private LocalDate startDate;

    /**
     * The end date of the placement.
     */
    @Column(nullable = false, columnDefinition = "DATE")
    private LocalDate endDate;

    /**
     * A list of {@link AppUser}s associated with the placement, such as members of the placement provider
     * organisation, and a student on the placement.
     */
    @ManyToMany
    @JoinColumn
    private List<AppUser> members = new ArrayList<>();

    /**
     * A list of {@link Document}s related to the placement, uploaded by users associated with the placement.
     */
    @OneToMany
    @JoinColumn(name = "placement_id")
    private List<Document> documents = new ArrayList<>();

    /**
     * A list of {@link PlacementVisit}s scheduled for the placement.
     */
    @OneToMany
    @JoinColumn(name = "placement_id", nullable = false)
    private List<PlacementVisit> visits = new ArrayList<>();

    /**
     * The {@link Provider} which the placement belongs to.
     */
    @ManyToOne
    @JoinColumn(nullable = false)
    private Provider provider;

    // Constructors

    public Placement(String title, String description, LocalDateTime applicationDeadline, LocalDate startDate,
                     LocalDate endDate, Provider provider) {
        this.title = title;
        this.description = description;
        this.applicationDeadline = applicationDeadline;
        this.startDate = startDate;
        this.endDate = endDate;
        this.provider = provider;
    }

    public Placement(String title, String description, LocalDateTime applicationDeadline, LocalDate startDate,
                     LocalDate endDate) {
        this.title = title;
        this.description = description;
        this.applicationDeadline = applicationDeadline;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Placement() {
        super();
    }

    // Getters and setters for entity fields, self-explanatory

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDateTime applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<AppUser> getMembers() {
        return members;
    }

    public void setMembers(List<AppUser> members) {
        this.members = members;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public List<PlacementVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<PlacementVisit> visits) {
        this.visits = visits;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }
}
