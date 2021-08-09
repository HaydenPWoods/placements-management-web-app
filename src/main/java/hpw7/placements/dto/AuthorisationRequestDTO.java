package hpw7.placements.dto;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;

/**
 * '<code>AuthorisationRequestDTO</code>' is a Data Transfer Object (DTO), with simple fields and constraints, used when
 * a student creates an authorisation request through the form. Data held by the DTO can be used in the creation of an
 * actual {@link hpw7.placements.domain.AuthorisationRequest} entity.
 */
public class AuthorisationRequestDTO {

    /**
     * The name of the (tentative) provider.
     */
    @NotBlank(message = "Provider name cannot be blank!")
    private String providerName;

    /**
     * Line 1 of the address of the (tentative) provider.
     */
    @NotBlank(message = "Provider address cannot be blank!")
    private String providerAddressLine1;

    /**
     * Line 2 of the address of the (tentative) provider. Optional.
     */
    private String providerAddressLine2;

    /**
     * The city of the address of the (tentative) provider.
     */
    @NotBlank(message = "Provider city cannot be blank!")
    private String providerAddressCity;

    /**
     * The postcode of the address of the (tentative) provider.
     */
    @NotBlank(message = "Provider postcode cannot be blank!")
    private String providerAddressPostcode;

    /**
     * The title of the placement the request is for.
     */
    @NotBlank(message = "Placement title cannot be blank!")
    private String title;

    /**
     * The start date of the placement, given as a date string.
     */
    @NotBlank(message = "Placement start date must be set!")
    private String startDate;

    /**
     * The end date of the placement, given as a date string.
     */
    @NotBlank(message = "Placement end date must be set!")
    private String endDate;

    /**
     * The details of the provider and the authorisation request.
     */
    @NotBlank(message = "Details cannot be blank!")
    private String details;

    /**
     * The username of the supporting tutor.
     */
    @NotBlank(message = "Tutor username must be provided!")
    private String tutorUsername;

    /**
     * Any files that are to be uploaded and assigned to the authorisation request as supporting documents.
     */
    private MultipartFile[] files;

    // Constructors

    public AuthorisationRequestDTO(String providerName, String providerAddressLine1, String providerAddressLine2,
                                   String providerAddressCity, String providerAddressPostcode, String title,
                                   String startDate, String endDate, String details, String tutorUsername) {
        this.providerName = providerName;
        this.providerAddressLine1 = providerAddressLine1;
        this.providerAddressLine2 = providerAddressLine2;
        this.providerAddressCity = providerAddressCity;
        this.providerAddressPostcode = providerAddressPostcode;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.details = details;
        this.tutorUsername = tutorUsername;
    }

    public AuthorisationRequestDTO(String providerName, String providerAddressLine1, String providerAddressCity,
                                   String providerAddressPostcode, String title, String startDate, String endDate,
                                   String details, String tutorUsername) {
        this.providerName = providerName;
        this.providerAddressLine1 = providerAddressLine1;
        this.providerAddressCity = providerAddressCity;
        this.providerAddressPostcode = providerAddressPostcode;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.details = details;
        this.tutorUsername = tutorUsername;
    }

    public AuthorisationRequestDTO() {
        super();
    }

    // Getters and setters, self-explanatory

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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public MultipartFile[] getFiles() {
        return files;
    }

    public void setFiles(MultipartFile[] files) {
        this.files = files;
    }

    public String getTutorUsername() {
        return tutorUsername;
    }

    public void setTutorUsername(String tutorUsername) {
        this.tutorUsername = tutorUsername;
    }
}
