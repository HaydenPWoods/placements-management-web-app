package hpw7.placements.dto;

import javax.validation.constraints.NotBlank;

/**
 * '<code>ProviderDTO</code>' is a Data Transfer Object (DTO), with simple fields and constraints, used when a user
 * creates a provider through the new provider form. Data held by the DTO can be used in the creation of an actual
 * {@link hpw7.placements.domain.Provider} entity.
 */
public class ProviderDTO {

    /**
     * The unique name of the provider.
     */
    @NotBlank(message = "Name cannot be blank!")
    private String name;

    /**
     * Line 1 of the address of the provider.
     */
    @NotBlank(message = "Address Line 1 cannot be blank!")
    private String addressLine1;

    /**
     * Line 2 of the address of the provider. Optional.
     */
    private String addressLine2;

    /**
     * The city of the address of the provider.
     */
    @NotBlank(message = "City cannot be blank!")
    private String addressCity;

    /**
     * The postcode of the address of the provider.
     */
    @NotBlank(message = "Postcode cannot be blank!")
    private String addressPostcode;

    /**
     * A description of the provider.
     */
    @NotBlank(message = "Provider description must be set!")
    private String description;

    // Constructors

    public ProviderDTO(String name, String addressLine1, String addressLine2, String addressCity, String addressPostcode, String description) {
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressCity = addressCity;
        this.addressPostcode = addressPostcode;
        this.description = description;
    }

    public ProviderDTO(String name, String addressLine1, String addressCity, String addressPostcode, String description) {
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressCity = addressCity;
        this.addressPostcode = addressPostcode;
        this.description = description;
    }

    public ProviderDTO() {
        super();
    }

    // Getters and setters, self-explanatory

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
