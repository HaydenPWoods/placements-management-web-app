package hpw7.placements.domain;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An entity to represent an individual application made for a placement by a student. Placement applications must be
 * approved by the provider of the placement, and an administrator, before it becomes "fully approved" and the student
 * is assigned to the placement.
 */
@Entity
public class PlacementApplication {

    /**
     * A unique ID for the placement application.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * The details of the application.
     */
    @Column(nullable = false)
    @Lob
    private String details;

    /**
     * Any supporting documents related to the application (e.g. the student's CV).
     */
    @OneToMany
    @JoinColumn(name = "placement_application_id")
    private List<Document> supportingDocs = new ArrayList<>();

    /**
     * Whether the application has been approved by the provider of the placement.
     */
    @Column(nullable = false)
    private boolean approvedByProvider = false;

    /**
     * Whether the application has been approved by an administrator.
     */
    @Column(nullable = false)
    private boolean approvedByAdministrator = false;

    /**
     * The placement which the application is being made for.
     */
    @ManyToOne
    @JoinColumn(nullable = false)
    private Placement placement;

    // Constructors

    public PlacementApplication(String details, Placement placement) {
        this.details = details;
        this.placement = placement;
    }

    public PlacementApplication(String details) {
        this.details = details;
    }

    public PlacementApplication() {
        super();
    }

    // Getters and setters for entity fields, self-explanatory

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<Document> getSupportingDocs() {
        return supportingDocs;
    }

    public void setSupportingDocs(List<Document> supportingDocs) {
        this.supportingDocs = supportingDocs;
    }

    public boolean isApprovedByProvider() {
        return approvedByProvider;
    }

    public void setApprovedByProvider(boolean approvedByProvider) {
        this.approvedByProvider = approvedByProvider;
    }

    public boolean isApprovedByAdministrator() {
        return approvedByAdministrator;
    }

    public void setApprovedByAdministrator(boolean approvedByAdministrator) {
        this.approvedByAdministrator = approvedByAdministrator;
    }

    public Placement getPlacement() {
        return placement;
    }

    public void setPlacement(Placement placement) {
        this.placement = placement;
    }
}
