package hpw7.placements.domain;

import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * An entity for an individual message, sent between two users through the application. Message contents are encrypted
 * using the {@link TextEncryptor}. When creating a Message object, the password should be set to the one defined
 * in the application.properties file.
 */
@Entity
public class Message {

    /**
     * The unique ID of the message.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * The timestamp of when the message was sent.
     */
    @Column(nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;

    /**
     * The sender of the message.
     */
    @ManyToOne
    @JoinColumn(name = "sender", nullable = false)
    private AppUser sender;

    /**
     * The intended recipient of the message.
     */
    @ManyToOne
    @JoinColumn(name = "recipient", nullable = false)
    private AppUser recipient;

    /**
     * The (encrypted) message contents. Decrypts with the encryption password when the getContent() method is called.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * The salt value used when encrypting the message contents for storing in the database. This salt needs to be
     * stored so we can later decrypt the message using the password and this randomly generated salt.
     */
    @Column(nullable = false)
    private String salt;

    // Constructors

    public Message(LocalDateTime timestamp, AppUser sender, AppUser recipient, String content, String encryptPassword) {
        this.timestamp = timestamp;
        this.sender = sender;
        this.recipient = recipient;
        this.salt = KeyGenerators.string().generateKey();
        TextEncryptor textEncryptor = Encryptors.delux(encryptPassword, salt);
        this.content = textEncryptor.encrypt(content);
    }

    public Message() {
        super();
    }

    // Getters and setters for entity fields, self-explanatory

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public AppUser getSender() {
        return sender;
    }

    public void setSender(AppUser sender) {
        this.sender = sender;
    }

    public AppUser getRecipient() {
        return recipient;
    }

    public void setRecipient(AppUser recipient) {
        this.recipient = recipient;
    }

    public String getContent(String encryptPassword) {
        TextEncryptor textEncryptor = Encryptors.delux(encryptPassword, salt);
        return textEncryptor.decrypt(content);
    }

    public void setContent(String content, String encryptPassword) {
        TextEncryptor textEncryptor = Encryptors.delux(encryptPassword, salt);
        this.content = textEncryptor.encrypt(content);
    }
}
