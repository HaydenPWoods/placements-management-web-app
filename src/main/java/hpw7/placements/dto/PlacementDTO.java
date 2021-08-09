package hpw7.placements.dto;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * '<code>PlacementDTO</code>' is a Data Transfer Object (DTO), with simple fields and constraints, used when a user
 * creates a placement through the add placement form. Data held by the DTO can be used in the creation of an actual
 * {@link hpw7.placements.domain.Placement} entity.
 */
public class PlacementDTO {

    /**
     * The ID of the provider to which the placement is to be assigned.
     */
    @NotNull(message = "Provider must be set!")
    private int providerId;

    /**
     * The title of the placement.
     */
    @NotBlank(message = "Title cannot be blank!")
    private String title;

    /**
     * A description of the placement offer.
     */
    @NotBlank(message = "Description cannot be blank!")
    private String description;

    /**
     * The closing date for application submissions for this placement offer, as a date string.
     */
    @NotBlank(message = "Application deadline must be set!")
    private String applicationDeadline;

    /**
     * The closing time for application submissions for this placement offer, combined with the date, as a time string.
     */
    @NotBlank(message = "Application deadline time must be set!")
    private String applicationDeadlineTime;

    /**
     * The start date of the placement, as a date string.
     */
    @NotBlank(message = "Start date must be set!")
    private String startDate;

    /**
     * The end date of the placement, as a date string.
     */
    @NotBlank(message = "End date must be set!")
    private String endDate;

    /**
     * Any files that are to be uploaded and assigned to the placement as related documents.
     */
    private MultipartFile[] files;

    // Constructors

    public PlacementDTO(int providerId, String title, String description, String applicationDeadline, String applicationDeadlineTime,
                        String startDate, String endDate) {
        this.providerId = providerId;
        this.title = title;
        this.description = description;
        this.applicationDeadline = applicationDeadline;
        this.applicationDeadlineTime = applicationDeadlineTime;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public PlacementDTO() {
        super();
    }

    // Getters and setters, self-explanatory

    public int getProviderId() {
        return providerId;
    }

    public void setProviderId(int providerId) {
        this.providerId = providerId;
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

    public String getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(String applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public String getApplicationDeadlineTime() {
        return applicationDeadlineTime;
    }

    public void setApplicationDeadlineTime(String applicationDeadlineTime) {
        this.applicationDeadlineTime = applicationDeadlineTime;
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

    public MultipartFile[] getFiles() {
        return files;
    }

    public void setFiles(MultipartFile[] files) {
        this.files = files;
    }
}
