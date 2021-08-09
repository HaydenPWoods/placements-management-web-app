package hpw7.placements.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * An entity to represent an individual scheduled visit for a placement, organised by a tutor. Visits scheduled for a
 * placement are "relevant" to the organiser and all other members of the placement.
 */
@Entity
public class PlacementVisit {

    /**
     * The unique ID of the visit.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * The date and time of the scheduled visit.
     */
    @Column(nullable = false)
    private LocalDateTime visitDateTime;

    /**
     * The organiser of the visit.
     */
    @ManyToOne
    @JoinColumn(nullable = false)
    private AppUser organiser;

    // Constructors

    public PlacementVisit(LocalDateTime visitDateTime, AppUser organiser) {
        this.visitDateTime = visitDateTime;
        this.organiser = organiser;
    }

    public PlacementVisit(LocalDateTime visitDateTime) {
        this.visitDateTime = visitDateTime;
    }

    public PlacementVisit() {
        super();
    }

    // Getters and setters for entity fields, self-explanatory

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getVisitDateTime() {
        return visitDateTime;
    }

    public void setVisitDateTime(LocalDateTime visitDateTime) {
        this.visitDateTime = visitDateTime;
    }

    public AppUser getOrganiser() {
        return organiser;
    }

    public void setOrganiser(AppUser organiser) {
        this.organiser = organiser;
    }
}
