package hpw7.placements.domain;

import javax.persistence.*;
import java.time.Duration;

/**
 * An entity to store the 'driving duration' between two different {@link Provider}s, as calculated by the Google Maps
 * API. Stored to reduce unnecessary calls to the API.
 */
@Entity
public class DurationToProvider {

    /**
     * The unique ID for the stored duration.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * The 'to' provider.
     */
    @ManyToOne
    @JoinColumn(nullable = false)
    private Provider pr2;

    /**
     * The duration of time to drive from pr1 to pr2.
     */
    @Column(nullable = false)
    private Duration duration;

    // Constructors

    public DurationToProvider(Provider pr2, Duration duration) {
        this.pr2 = pr2;
        this.duration = duration;
    }

    public DurationToProvider() {
        super();
    }

    // Getters and setters for entity fields, self-explanatory

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Provider getPr2() {
        return pr2;
    }

    public void setPr2(Provider pr2) {
        this.pr2 = pr2;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }
}
