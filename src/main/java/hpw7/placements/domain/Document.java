package hpw7.placements.domain;

import javax.persistence.*;

/**
 * '<code>Document</code>' is an entity for a file uploaded by a {@link AppUser}, associated with a {@link Placement}.
 */
@Entity
public class Document {

    /**
     * The unique ID of the document. Automatically generated.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * The title of the document.
     */
    @Column(nullable = false)
    private String name;

    /**
     * A description of the document.
     */
    @Column(nullable = false)
    private String description;

    /**
     * The location of the file as a String. / The document file itself, in a suitable and readable format.
     */
    @Column(nullable = false)
    private String file;

    /**
     * The owner of the document.
     */
    @ManyToOne
    @JoinColumn(nullable = false)
    private AppUser owner;

    // Constructors

    public Document(String name, String description, String file) {
        this.name = name;
        this.description = description;
        this.file = file;
    }

    public Document() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }
}
