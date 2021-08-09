package hpw7.placements.dto;

/**
 * '<code>EditPlacementDTO</code>' is a Data Transfer Object (DTO), with simple fields, used when a user is making
 * changes to the details of a placement through the placement edit form. Data held by the DTO can be applied to the
 * fields of an actual {@link hpw7.placements.domain.Placement} entity.
 */
public class EditPlacementDTO {

    /**
     * The updated title of the placement.
     */
    private String title;

    /**
     * An updated description of the placement offer.
     */
    private String description;

    /**
     * An updated closing date for application submissions, as a date string.
     */
    private String applicationDeadline;

    /**
     * An updated closing time for application submissions, combined with the closing date, as a time string.
     */
    private String applicationDeadlineTime;

    /**
     * An updated start date of the placement, as a date string.
     */
    private String startDate;

    /**
     * An updated end date of the placement, as a date string.
     */
    private String endDate;

    // Constructors

    public EditPlacementDTO(String title, String description, String applicationDeadline, String applicationDeadlineTime,
                            String startDate, String endDate) {
        this.title = title;
        this.description = description;
        this.applicationDeadline = applicationDeadline;
        this.applicationDeadlineTime = applicationDeadlineTime;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public EditPlacementDTO() {
        super();
    }

    // Getters and setters, self-explanatory

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
}
