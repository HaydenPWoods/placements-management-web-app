package hpw7.placements.dto;

/**
 * '<code>EditProviderDTO</code>' is a Data Transfer Object (DTO), with simple fields, used when a user is making
 * changes to the details of a provider through the provider edit form. Data held by the DTO can be applied to the
 * fields of an actual {@link hpw7.placements.domain.Provider} entity.
 */
public class EditProviderDTO {

    /**
     * The updated name of the provider.
     */
    private String name;

    /**
     * The updated address line 1 of the provider.
     */
    private String addressLine1;

    /**
     * The updated address line 2 of the provider.
     */
    private String addressLine2;

    /**
     * The updated city of the provider.
     */
    private String addressCity;

    /**
     * The updated postcode of the provider.
     */
    private String addressPostcode;

    /**
     * An updated description of the provider.
     */
    private String description;

    // Constructors

    public EditProviderDTO(String name, String addressLine1, String addressLine2, String addressCity, String addressPostcode, String description) {
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressCity = addressCity;
        this.addressPostcode = addressPostcode;
        this.description = description;
    }

    public EditProviderDTO() {
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
