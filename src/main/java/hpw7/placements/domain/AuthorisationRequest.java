package hpw7.placements.domain;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * '<code>AuthorisationRequest</code>' is an entity for storing information related to individual placement
 * authorisation requests. Students can make an placement authorisation request, to be confirmed by the provider and
 * the tutor responsible for the student. {@link Document}s can be uploaded to support a request.
 */
@Entity
public class AuthorisationRequest {

    /**
     * The unique ID of the request. Automatically generated.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * The name of the (tentative) provider.
     */
    @Column(nullable = false)
    private String providerName;

    /**
     * Line 1 of the address of the (tentative) provider.
     */
    @Column(nullable = false)
    private String providerAddressLine1;

    /**
     * Line 2 of the address of the (tentative) provider. Optional.
     */
    private String providerAddressLine2;

    /**
     * The city of the address of the (tentative) provider.
     */
    @Column(nullable = false)
    private String providerAddressCity;

    /**
     * The postcode of the address of the (tentative) provider.
     */
    @Column(nullable = false)
    private String providerAddressPostcode;

    /**
     * The title of the placement the request is for.
     */
    @Column(nullable = false)
    private String title;

    /**
     * The details of the provider and the authorisation request.
     */
    @Column(nullable = false)
    @Lob
    private String details;

    /**
     * The start date of the placement.
     */
    @Column(columnDefinition = "DATE", nullable = false)
    private LocalDate startDate;

    /**
     * The end date of the placement.
     */
    @Column(columnDefinition = "DATE", nullable = false)
    private LocalDate endDate;

    /**
     * A list of all supporting documents for the authorisation request, of type {@link Document}.
     */
    @OneToMany
    @JoinColumn(name = "authorisation_request_id")
    private List<Document> supportingDocs = new ArrayList<>();

    /**
     * The supporting tutor.
     */
    @ManyToOne
    @JoinColumn(nullable = false)
    private AppUser tutor;

    /**
     * Whether the request has been approved by the tutor, or not.
     */
    @Column(nullable = false)
    private boolean approvedByTutor;

    /**
     * Whether the request has been approved by the administrator, or not.
     */
    @Column(nullable = false)
    private boolean approvedByAdministrator;

    // Constructors

    public AuthorisationRequest(String providerName, String providerAddressLine1, String providerAddressLine2,
                                String providerAddressCity, String providerAddressPostcode, String title,
                                LocalDate startDate, LocalDate endDate, String details) {
        this.providerName = providerName;
        this.providerAddressLine1 = providerAddressLine1;
        this.providerAddressLine2 = providerAddressLine2;
        this.providerAddressCity = providerAddressCity;
        this.providerAddressPostcode = providerAddressPostcode;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.details = details;
        this.approvedByTutor = false;
        this.approvedByAdministrator = false;
    }

    public AuthorisationRequest(String providerName, String providerAddressLine1, String providerAddressCity,
                                String providerAddressPostcode, String title, LocalDate startDate,
                                LocalDate endDate,String details) {
        this.providerName = providerName;
        this.providerAddressLine1 = providerAddressLine1;
        this.providerAddressCity = providerAddressCity;
        this.providerAddressPostcode = providerAddressPostcode;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.details = details;
        this.approvedByTutor = false;
        this.approvedByAdministrator = false;
    }

    public AuthorisationRequest() {
        super();
    }

    // Getters and setters for entity fields, self-explanatory

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderAddressLine1() {
        return providerAddressLine1;
    }

    public void setProviderAddressLine1(String providerAddressLine1) {
        this.providerAddressLine1 = providerAddressLine1;
    }

    public String getProviderAddressLine2() {
        return providerAddressLine2;
    }

    public void setProviderAddressLine2(String providerAddressLine2) {
        this.providerAddressLine2 = providerAddressLine2;
    }

    public String getProviderAddressCity() {
        return providerAddressCity;
    }

    public void setProviderAddressCity(String providerAddressCity) {
        this.providerAddressCity = providerAddressCity;
    }

    public String getProviderAddressPostcode() {
        return providerAddressPostcode;
    }

    public void setProviderAddressPostcode(String providerAddressPostcode) {
        this.providerAddressPostcode = providerAddressPostcode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public AppUser getTutor() {
        return tutor;
    }

    public void setTutor(AppUser tutor) {
        this.tutor = tutor;
    }

    public boolean isApprovedByTutor() {
        return approvedByTutor;
    }

    public void setApprovedByTutor(boolean approvedByTutor) {
        this.approvedByTutor = approvedByTutor;
    }

    public boolean isApprovedByAdministrator() {
        return approvedByAdministrator;
    }

    public void setApprovedByAdministrator(boolean approvedByAdministrator) {
        this.approvedByAdministrator = approvedByAdministrator;
    }
}
