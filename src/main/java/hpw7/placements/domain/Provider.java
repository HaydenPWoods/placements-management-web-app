package hpw7.placements.domain;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * '<code>Provider</code>' is an entity for a placement provider involved with the web application. {@link AppUser}s and
 * {@link Placement}s can be assigned to a Provider.
 */
@Entity
public class Provider {
    /**
     * The unique ID of the provider. Automatically generated.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * The name of the provider.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Line 1 of the address of the provider.
     */
    @Column(nullable = false)
    private String addressLine1;

    /**
     * Line 2 of the address of the provider. Optional.
     */
    private String addressLine2;

    /**
     * The city of the address of the provider.
     */
    @Column(nullable = false)
    private String addressCity;

    /**
     * The postcode of the address of the provider.
     */
    @Column(nullable = false)
    private String addressPostcode;

    /**
     * The geocoded latitude of the location of the provider. Determined by passing the given address for the provider
     * to the Google Maps Geocoding API. See {@link hpw7.placements.service.MapService}.
     */
    private double latitude;

    /**
     * The geocoded longitude of the address of the provider. Determined by passing the given address for the provider
     * to the Google Maps Geocoding API. See {@link hpw7.placements.service.MapService}
     */
    private double longitude;

    /**
     * A description of the provider.
     */
    @Column(nullable = false)
    @Lob
    private String description;

    /**
     * A list of all members (e.g. staff) of a placement provider, of entity type {@link AppUser}.
     */
    @OneToMany
    @JoinColumn
    private List<AppUser> members = new ArrayList<>();

    /**
     * A list of driving times from this provider, to other listed providers on the application.
     */
    @OneToMany
    @JoinColumn(name = "pr1_id", nullable = false)
    private List<DurationToProvider> timesToPr2s = new ArrayList<>();

    /**
     * The location of the provider's logo on the local filesystem, as a string. Null if a custom logo hasn't been set.
     */
    private String logo;

    // Constructors

    public Provider(String name, String addressLine1, String addressLine2, String addressCity, String addressPostcode,
                    String description) {
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressCity = addressCity;
        this.addressPostcode = addressPostcode;
        this.description = description;
    }

    public Provider(String name, String addressLine1, String addressCity, String addressPostcode, String description) {
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressCity = addressCity;
        this.addressPostcode = addressPostcode;
        this.description = description;
    }

    public Provider() {
        super();
    }

    // Getters and setters for entity fields, self-explanatory

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public void setAddressPostcode(String addressPostcode) {
        this.addressPostcode = addressPostcode;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<AppUser> getMembers() {
        return members;
    }

    public void setMembers(List<AppUser> members) {
        this.members = members;
    }

    public List<DurationToProvider> getTimesToPr2s() {
        return timesToPr2s;
    }

    public void setTimesToPr2s(List<DurationToProvider> timesToPr2s) {
        this.timesToPr2s = timesToPr2s;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }
}
