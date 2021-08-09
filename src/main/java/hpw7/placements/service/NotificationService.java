package hpw7.placements.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import hpw7.placements.domain.AppUser;
import hpw7.placements.repository.AppUserRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * The <code>NotificationService</code> handles the sending of web notifications to users who have enabled these
 * notifications. Notifications are sent through Firebase Cloud Messaging, and are directed to a specific user by
 * passing a specific device token. These notifications are not to be confused with the "email notifications" sent with
 * the {@link MailService}. Notifications sent here are served through the Notifications API. (See:
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/Notifications_API">Notifications API - MDN</a>) All methods
 * are asynchronous - they do not return anything, and they will not block the function they're called from.
 */
@Service
@Async
public class NotificationService {

    private final AppUserRepository uRepo;

    public NotificationService(AppUserRepository uRepo) {
        this.uRepo = uRepo;
    }

    /**
     * Creates an instance of Firebase. Once created, this instance can then be called with
     * <code>FirebaseMessaging.getInstance()</code>.
     *
     * @throws IOException the Firebase app couldn't be initialised. Check the configuration provided in the
     *                     "resources/firebase-admin-sdk-config.json" file
     */
    public void initialiseFirebaseApp() throws IOException {
        // INITIALISE FIREBASE (for message notifications)
        InputStream sdkConfig = new ClassPathResource("firebase-admin-sdk-config.json").getInputStream();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(sdkConfig))
                .build();
        FirebaseApp.initializeApp(options);
    }

    /**
     * Sends a notification, if possible, to the intended recipient of the message. If the recipient hasn't enabled web
     * notifications, or the token is invalid in some way (they might have granted, and later revoked permissions), the
     * notification will not be sent. <br/><br/>
     * <p>
     * NOTE: This utilises the Firebase Cloud Messaging service to send notifications. On the client side, this requires
     * a service worker to be registered, and for the worker to be registered, it has to be served over a secure tunnel
     * (https). Because this application isn't deployed anywhere (yet), there's no domain name to get a certificate for,
     * and so a self-signed certificate is being used, which throws up warnings in browsers, and seems to block the
     * registering of service workers in some cases. <br/><br/>
     * <p>
     * In Firefox, no such problems occur outside of trusting the self-signed certificate. The service worker
     * registers fine, and notifications can be received by the browser. In Chrome, however, the certificate problems
     * prevent the service worker being registered, and notifications cannot be recieved. This would not be a problem
     * if the system is deployed, and a proper certificate is in use. While the self-signed certificate is in use, the
     * problem can be worked around by launching Chrome with certain flags from the command line: <br/><br/>
     * <p>
     * [LOCATION OF CHROME APPLICATION] --ignore-certificate-errors --unsafely-treat-insecure-origin-as-secure=https://localhost:8443 <br/><br/>
     * <p>
     * See: <a href="https://deanhume.com/testing-service-workers-locally-with-self-signed-certificates/">Testing Service Workers Locally with Self-Signed Certificates, Dean Hume (2017)</a>
     *
     * @param message         the Message object to send a notification for
     * @param encryptPassword the password to decrypt the message content (read from application.properties)
     * @throws IOException the Firebase app couldn't be initialised
     */
    public void sendNewMessageNotification(hpw7.placements.domain.Message message, String encryptPassword) throws IOException {
        if (FirebaseApp.getApps().size() == 0) {
            initialiseFirebaseApp();
        }
        if (message.getRecipient().getDeviceToken() != null) { // If recipient hasn't enabled notifications, ignore - we can't send notification.
            // Recipient has enabled notifications, so can send one.
            Message firebaseMessage = Message.builder()
                    .setToken(message.getRecipient().getDeviceToken())
                    .setNotification(Notification.builder()
                            .setTitle("New message received from " + message.getSender().getUsername())
                            .setBody(message.getContent(encryptPassword))
                            .build())
                    .build();
            try {
                FirebaseMessaging.getInstance().send(firebaseMessage);
            } catch (FirebaseMessagingException e) {
                // Void token, since it doesn't work anymore - and even if it is some other error, we will get the token again at some point.
                AppUser u = message.getRecipient();
                u.setDeviceToken(null);
                uRepo.save(u);
            }
        }
    }

    /**
     * Sends a message deletion notification, if possible, to the other user. Used to attempt to propagate a deleted
     * message to the recipient immediately. If notifications aren't enabled, this can't happen - the message will still
     * have been deleted, and this change will propagate when the recipient reloads the page, or it is done
     * automatically, through the checking for new messages, for example.
     *
     * @param message the deleted Message object to send a notification for
     * @throws IOException the Firebase app couldn't be initialised
     * @see #sendNewMessageNotification(hpw7.placements.domain.Message, String)
     */
    public void sendMessageDeleteNotification(hpw7.placements.domain.Message message) throws IOException {
        if (FirebaseApp.getApps().size() == 0) {
            initialiseFirebaseApp();
        }
        if (message.getRecipient().getDeviceToken() != null) {
            // Recipient has enabled notifications, so can send one.
            Message firebaseMessage = Message.builder()
                    .setToken(message.getRecipient().getDeviceToken())
                    .setNotification(Notification.builder()
                            .setTitle("Message deleted from conversation with user " + message.getSender().getUsername())
                            .build())
                    .build();
            try {
                FirebaseMessaging.getInstance().send(firebaseMessage);
            } catch (FirebaseMessagingException e) {
                // Void token
                AppUser u = message.getRecipient();
                u.setDeviceToken(null);
                uRepo.save(u);
            }
        }
    }

}
