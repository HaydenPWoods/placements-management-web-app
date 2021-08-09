package hpw7.placements.repository;

import hpw7.placements.domain.AppUser;
import hpw7.placements.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Provides methods to query data and return records stored in the message table of the database. See {@link Message}.
 */
public interface MessageRepository extends PagingAndSortingRepository<Message, Integer> {

    /**
     * Finds and retrieves all recipient usernames for messages sent by a given user. Used to determine current
     * "conversations" for a user - i.e. they have sent at least one message to the recipient.
     *
     * @param sender The sender of the messages.
     * @return A list of usernames of users that the sender has sent at least one message to.
     */
    @Query(value = "SELECT DISTINCT message.recipient FROM message WHERE sender LIKE (?1)", nativeQuery = true)
    List<String> recipientsForSender(AppUser sender);

    /**
     * Finds and retrieves all sender usernames for messages received by a given user. Used to determine current
     * "conversations" for a user - i.e. they have received at least one message from the sender.
     *
     * @param recipient The recipient of the messages.
     * @return A list of usernames of users that have sent at least one message to the recipient.
     */
    @Query(value = "SELECT DISTINCT message.sender FROM message WHERE recipient LIKE (?1)", nativeQuery = true)
    List<String> sendersForRecipient(AppUser recipient);

    /**
     * Finds and retrieves the latest message sent between a given sender and recipient pair of users.
     *
     * @param sender    The sender of the message.
     * @param recipient The recipient of the message.
     * @return The latest message sent between sender and recipient.
     */
    Message findTopBySenderAndRecipientOrderByTimestampDesc(AppUser sender, AppUser recipient);

    /**
     * Finds and retrieves all messages sent by the given user.
     *
     * @param sender The sender of the messages.
     * @return A list of all messages sent by the given user.
     */
    List<Message> findAllBySender(AppUser sender);

    /**
     * Finds and retrieves all messages received by the given user.
     *
     * @param recipient The recipient of the messages.
     * @return A list of all messages received by (sent to) the given user.
     */
    List<Message> findAllByRecipient(AppUser recipient);

    /**
     * Finds and retrieves all messages between two given users (sender and recipient pair).
     *
     * @param sender    The sender of the messages.
     * @param recipient The recipient of the messages.
     * @param pageable  The length of page to return, and any other pagination options, including sorting.
     * @return A page of messages of the given size, between the given sender and recipient.
     */
    Page<Message> findAllBySenderAndRecipient(AppUser sender, AppUser recipient, Pageable pageable);

    /**
     * Finds and retrieves all messages between users in a list of senders and recipients. Intended to allow returning
     * a "message chain" between two users (i.e. both can be the sender and the recipient). To retrieve a message chain,
     * include both users in the lists of senders and recipients.
     *
     * @param senders    The list of message sender users.
     * @param recipients The list of message recipient users.
     * @param pageable   The length of page to return, and any other pagination options, including sorting.
     * @return A page of messages of the given size, between the given senders and recipients.
     */
    Page<Message> findAllBySenderInAndRecipientIn(List<AppUser> senders, List<AppUser> recipients, Pageable pageable);

    /**
     * Checks whether at least one message exists with the given sender and recipient, after the given date/time.
     *
     * @param sender    The sender of the message.
     * @param recipient The recipient of the message.
     * @param timestamp The start of the date/time range (the end of the range effectively being the current date/time).
     * @return True if a message exists satisfying the given parameters, false otherwise.
     */
    boolean existsBySenderAndRecipientAndTimestampAfter(AppUser sender, AppUser recipient, LocalDateTime timestamp);

}
