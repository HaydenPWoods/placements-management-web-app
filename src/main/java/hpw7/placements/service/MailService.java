package hpw7.placements.service;

import hpw7.placements.domain.*;
import hpw7.placements.repository.LogEntryRepository;
import hpw7.placements.repository.PlacementRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * The <code>MailService</code> deals with sending email notifications to users who have explicitly given permission for
 * the system to send relevant emails to them. Any call to these methods with a user that has not given permission will
 * silently exit, with no email sent. All methods are asynchronous - they do not return anything, and they will not
 * block the function they're called from.
 */
@Service
@Async
public class MailService {

    @Value("${spring.mail.username}")
    private String emailUsername;

    @Value("${spring.textencryptor.password}")
    private String encryptPassword;

    @Value("${spring.mail.action-url}")
    private String actionUrl;

    private final JavaMailSender javaMailSender;

    private final PlacementRepository plRepo;

    private final TemplateEngine templateEngine;

    private final LogEntryRepository lRepo;

    public MailService(@Qualifier("getJavaMailSender") JavaMailSender javaMailSender, PlacementRepository plRepo, TemplateEngine templateEngine, LogEntryRepository lRepo) {
        this.javaMailSender = javaMailSender;
        this.plRepo = plRepo;
        this.templateEngine = templateEngine;
        this.lRepo = lRepo;
    }

    /**
     * Sends an customised email stating that the user's placement application has been fully approved by provider and
     * administrator.
     *
     * @param recipient The user to send the email to
     * @param pApp      The placement application that the email is about
     */
    public void sendApplicationApprovedNotification(AppUser recipient, PlacementApplication pApp) {
        if (recipient.isEmailsEnabled()) {
            try {
                // Create context and add variables to load into email template
                Context context = new Context();
                context.setVariable("recipient", recipient);
                context.setVariable("pApp", pApp);
                context.setVariable("ACTION_URL", actionUrl);

                // Process Thymeleaf email template
                String htmlEmailBody = templateEngine.process("email/application_approved", context);

                // Construct and send email
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper messageHelper = new MimeMessageHelper(message, "UTF-8");
                messageHelper.setFrom(emailUsername, "Placements Management Web App");
                messageHelper.setTo(recipient.getEmail());
                messageHelper.setSubject("Placement Application Approved!");
                messageHelper.setText(htmlEmailBody, true);

                javaMailSender.send(message);

                lRepo.save(new LogEntry(recipient, LocalDateTime.now(), LogAction.EMAIL, LogEntity.APP_USER,
                        "Sent placement application approved email notification to user"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a customised email with a given message to each of the listed recipients. Called by the application, this
     * method can send an email message to all users of a given role (e.g. all students), or all listed users on the
     * application by selecting all users of each role.
     *
     * @param recipients         A list of users to send the message to.
     * @param message            The message text body to send.
     * @param permissionOverride Whether to send the email to the recipient, regardless of their set email permission.
     */
    public void sendGroupEmail(List<AppUser> recipients, String message, boolean permissionOverride) {
        // Create context and add variables to load into email template
        Context context = new Context();
        context.setVariable("message", message);
        context.setVariable("ACTION_URL", actionUrl);

        // Process Thymeleaf email template - template processing is resource intensive, so using a generic greeting to
        // allow the same generated email body to be used for all sent emails.
        String htmlEmailBody = templateEngine.process("email/group_email", context);

        // Construct email
        MimeMessage emailMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper emailMessageHelper = new MimeMessageHelper(emailMessage, "UTF-8");
        try {
            emailMessageHelper.setFrom(emailUsername, "Placements Management Web App");
            emailMessageHelper.setSubject("Message from the Administrators");
            emailMessageHelper.setText(htmlEmailBody, true);
        } catch (Exception e) {
            e.printStackTrace();
            return; // Cannot continue, email construction failed
        }

        for (AppUser recipient : recipients) {
            if (permissionOverride || recipient.isEmailsEnabled()) {
                try {
                    // Set recipient and send email
                    emailMessageHelper.setTo(recipient.getEmail());
                    javaMailSender.send(emailMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sends a customised email stating that the user has received a new message. The email will only be sent if, in
     * addition to the user giving permission for emails to be sent, this method hasn't been called - and an email sent
     * by it - in the last 12 hours, for the recipient.
     *
     * @param message The message that the email is about, with the email to be sent to the recipient of this message
     */
    public void sendMessageNotification(Message message) {
        if (message.getRecipient().isEmailsEnabled() &&
                !lRepo.existsByAppUserAndActionTypeAndEntityTypeAndTimestampAfterAndDescriptionLike(
                        message.getRecipient(),
                        LogAction.EMAIL,
                        LogEntity.APP_USER,
                        LocalDateTime.now().minus(12, ChronoUnit.HOURS),
                        "Sent 'New message received!' email notification to user")) {
            // Passed both tests - we can try to send the email
            try {
                // Create context and add variables to load into email template
                Context context = new Context();
                context.setVariable("message", message);
                context.setVariable("encryptPassword", encryptPassword);
                context.setVariable("ACTION_URL", actionUrl);

                // Process Thymeleaf email template
                String htmlEmailBody = templateEngine.process("email/new_message", context);

                // Construct and send email
                MimeMessage emailMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper emailMessageHelper = new MimeMessageHelper(emailMessage, "UTF-8");
                emailMessageHelper.setFrom(emailUsername, "Placements Management Web App");
                emailMessageHelper.setTo(message.getRecipient().getEmail());
                emailMessageHelper.setSubject("New Message Received!");
                emailMessageHelper.setText(htmlEmailBody, true);

                javaMailSender.send(emailMessage);

                lRepo.save(new LogEntry(message.getRecipient(), LocalDateTime.now(), LogAction.EMAIL, LogEntity.APP_USER,
                        "Sent 'New message received!' email notification to user"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Sends a customised email stating that the user's password has been changed.
     *
     * @param recipient The user to send the email to
     */
    public void sendPasswordChangeNotification(AppUser recipient) {
        if (recipient.isEmailsEnabled()) {
            try {
                // Create context and add variables to load into email template
                Context context = new Context();
                context.setVariable("recipient", recipient);
                context.setVariable("ACTION_URL", actionUrl);

                // Process Thymeleaf email template
                String htmlEmailBody = templateEngine.process("email/changed_password", context);

                // Construct and send email
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper messageHelper = new MimeMessageHelper(message, "UTF-8");
                messageHelper.setFrom(emailUsername, "Placements Management Web App");
                messageHelper.setTo(recipient.getEmail());
                messageHelper.setSubject("Password Change Notification");
                messageHelper.setText(htmlEmailBody, true);

                javaMailSender.send(message);

                lRepo.save(new LogEntry(recipient, LocalDateTime.now(), LogAction.EMAIL, LogEntity.APP_USER,
                        "Sent password change email notification to user"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a customised email stating that the user's authorisation request has been fully approved by tutor and
     * administrator.
     *
     * @param recipient The user to send the email to
     * @param request   The authorisation request that the email is about
     */
    public void sendRequestApprovedNotification(AppUser recipient, AuthorisationRequest request) {
        if (recipient.isEmailsEnabled()) {
            try {
                // Create context and add variables to load into email template
                Context context = new Context();
                context.setVariable("recipient", recipient);
                context.setVariable("request", request);
                context.setVariable("ACTION_URL", actionUrl);

                // Process Thymeleaf email template
                String htmlEmailBody = templateEngine.process("email/request_approved", context);

                // Construct and send email
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper messageHelper = new MimeMessageHelper(message, "UTF-8");
                messageHelper.setFrom(emailUsername, "Placements Management Web App");
                messageHelper.setTo(recipient.getEmail());
                messageHelper.setSubject("Authorisation Request Approved!");
                messageHelper.setText(htmlEmailBody, true);

                javaMailSender.send(message);

                lRepo.save(new LogEntry(recipient, LocalDateTime.now(), LogAction.EMAIL, LogEntity.APP_USER,
                        "Sent authorisation request approved email notification to user"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a customised email to all members of the placement (other than the organiser of the visit), stating that a
     * visit has been scheduled.
     *
     * @param visit     The scheduled visit to send an email for
     * @param placement The placement the visit is for
     */
    public void sendVisitScheduledNotification(PlacementVisit visit, Placement placement) {
        for (AppUser recipient : placement.getMembers()) {
            if (recipient.isEmailsEnabled() && recipient != visit.getOrganiser()) {
                try {
                    // Create context and add variables to load into email template
                    Context context = new Context();
                    context.setVariable("recipient", recipient);
                    context.setVariable("visit", visit);
                    context.setVariable("placement", placement);
                    context.setVariable("ACTION_URL", actionUrl);

                    // Process Thymeleaf email template
                    String htmlEmailBody = templateEngine.process("email/visit_scheduled", context);

                    // Construct and send email
                    MimeMessage message = javaMailSender.createMimeMessage();
                    MimeMessageHelper messageHelper = new MimeMessageHelper(message, "UTF-8");
                    messageHelper.setFrom(emailUsername, "Placements Management Web App");
                    messageHelper.setTo(recipient.getEmail());
                    messageHelper.setSubject("Visit Scheduled for " + placement.getTitle());
                    messageHelper.setText(htmlEmailBody, true);

                    javaMailSender.send(message);

                    lRepo.save(new LogEntry(recipient, LocalDateTime.now(), LogAction.EMAIL, LogEntity.APP_USER,
                            "Sent visit scheduled email notification to user"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sends a customised email to all members of the placement for each visit (other than each organiser), stating that
     * a visit has been scheduled. This method calls the sendVisitScheduledNotification() method and waits 10 seconds
     * between each call, to prevent the application sending too many emails concurrently and hitting service limits.
     * The Transactional annotation is used here to ensure that the database session remains open so members for a
     * placement can be retrieved in the individual method.
     *
     * @param visitList A list of placement visits to send notifications for.
     */
    @Transactional
    public void sendManyVisitScheduledNotifications(List<PlacementVisit> visitList) {
        for (PlacementVisit visit : visitList) {
            sendVisitScheduledNotification(visit, plRepo.findByVisitsContains(visit));
            try {
                Thread.sleep(10000); // 10 seconds
            } catch (InterruptedException e) {
                // Ignore - carry on for other visits.
            }
        }
    }
}
