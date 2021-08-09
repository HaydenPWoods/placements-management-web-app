package hpw7.placements.dto;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;

/**
 * '<code>PlacementApplicationDTO</code>' is a Data Transfer Object (DTO), with simple fields and constraints, used when
 * a student creates a placement application through the application form. Data held by the DTO can be used in the
 * creation of an actual {@link hpw7.placements.domain.PlacementApplication} entity.
 */
public class PlacementApplicationDTO {

    /**
     * The details of the application.
     */
    @NotBlank(message = "Application details cannot be blank!")
    private String details;

    /**
     * Any files that are to be uploaded and assigned to the placement application as supporting documents.
     */
    private MultipartFile[] files;

    // Constructors

    public PlacementApplicationDTO(String details) {
        this.details = details;
    }

    public PlacementApplicationDTO() {
        super();
    }

    // Getters and setters, self-explanatory

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
}
