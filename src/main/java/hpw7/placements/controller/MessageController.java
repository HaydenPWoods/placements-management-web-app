package hpw7.placements.controller;

import hpw7.placements.service.MailService;
import hpw7.placements.service.NotificationService;
import hpw7.placements.domain.*;
import hpw7.placements.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * The <code>MessageController</code> handles requests relating to private messages sent through the application,
 * between two users. It offers the ability for sending and deleting messages, and checking for new ones. With the
 * {@link NotificationService} and Firebase Cloud Messaging, the application can store a device token for the user, and
 * can then send web notifications for new messages, where that user has enabled notifications and has their registered
 * browser (device) running.
 */
@Controller
public class MessageController {

    private final AppUserRepository uRepo;

    private final AuthorisationRequestRepository arRepo;

    private final MailService mailService;

    private final MessageRepository mRepo;

    private final NotificationService notificationService;

    private final PlacementApplicationRepository plAppRepo;

    private final PlacementRepository plRepo;

    private final ProviderRepository prRepo;

    /**
     * The password used to encrypt and decrypt message contents when storing and retrieving from the database, as
     * defined in application.properties. If changed, previous messages cannot be decrypted.
     */
    @Value("${spring.textencryptor.password}")
    private String encryptPassword;

    public MessageController(AppUserRepository uRepo, AuthorisationRequestRepository arRepo, MailService mailService, MessageRepository mRepo, NotificationService notificationService, PlacementApplicationRepository plAppRepo, PlacementRepository plRepo, ProviderRepository prRepo) {
        this.uRepo = uRepo;
        this.arRepo = arRepo;
        this.mailService = mailService;
        this.mRepo = mRepo;
        this.notificationService = notificationService;
        this.plAppRepo = plAppRepo;
        this.plRepo = plRepo;
        this.prRepo = prRepo;
    }

    @GetMapping("/messaging")
    public String allConversationsOverview(Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());

        // Work out who the user has sent at least one message to / received at least one message from
        List<String> recipientsForSender = mRepo.recipientsForSender(u);
        List<String> sendersForRecipient = mRepo.sendersForRecipient(u);

        List<Message> latestMessageList = new ArrayList<>(); // Getting the most recent message for each conversation

        for (String user : recipientsForSender) {
            AppUser u2 = uRepo.findByUsername(user);
            Message latestMessage = mRepo.findTopBySenderAndRecipientOrderByTimestampDesc(u, u2); // Most recent message from u to u2
            if (sendersForRecipient.contains(user)) {
                // Reverse and see if a message from u2 to u is more recent than the current latestMessage
                Message reverseLatestMessage = mRepo.findTopBySenderAndRecipientOrderByTimestampDesc(u2,u);
                if (reverseLatestMessage.getTimestamp().isAfter(latestMessage.getTimestamp())) {
                    latestMessage = reverseLatestMessage;
                }
                sendersForRecipient.remove(user);
            }
            latestMessageList.add(latestMessage);
        }

        for (String user : sendersForRecipient) { // Catch any not dealt with yet
            AppUser u2 = uRepo.findByUsername(user);
            Message latestMessage = mRepo.findTopBySenderAndRecipientOrderByTimestampDesc(u2, u);
            latestMessageList.add(latestMessage);
        }

        latestMessageList.sort(Comparator.comparing(Message::getTimestamp).reversed()); // Latest messages first

        model.addAttribute("user", u);
        model.addAttribute("latestMessageList", latestMessageList);
        model.addAttribute("encryptPassword", encryptPassword); // To decrypt messages when parsing template

        return "messaging/overview";
    }

    @GetMapping("/messaging/new")
    public String newConversationForm(Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());

        // Get all recipients relevant to the user
        Set<AppUser> possibleRecipients = new HashSet<>();

        switch (u.getRole()) {
            case ADMINISTRATOR: {
                // Admin can send a message to all users
                possibleRecipients.addAll(uRepo.findAll(Sort.by("username").ascending()));
                break;
            }
            case PROVIDER: {
                // Member of provider can send a message to all other members of the provider,members of their
                // placements, and those applying to their placements.
                Provider provider = prRepo.findByMembersUsername(u.getUsername());
                possibleRecipients.addAll(provider.getMembers());
                for (Placement placement : plRepo.findAllByProvider(provider)) {
                    possibleRecipients.addAll(placement.getMembers());
                    for (PlacementApplication pApp : plAppRepo.findAllByPlacement(placement)) {
                        possibleRecipients.add(uRepo.findByPlacementApplicationsContaining(pApp));
                    }
                }
                break;
            }
            case TUTOR: {
                // Tutor can reach other tutors, members of placements they are a member of, and students who have
                // created an authorisation requests where they are listed as the assigned tutor.
                possibleRecipients.addAll(uRepo.findByRole(Role.TUTOR));
                for (Placement placement : plRepo.findAllByMembersContains(u)) {
                    possibleRecipients.addAll(placement.getMembers());
                }
                for (AuthorisationRequest request : arRepo.findAllByTutor(u)) {
                    possibleRecipients.add(uRepo.findByAuthorisationRequestsContaining(request));
                }
                break;
            }
            default: { // Student
                // Student can reach members of placements they are a member of, and assigned tutors for authorisation
                // requests they have created.
                for (Placement placement : plRepo.findAllByMembersContains(u)) {
                    possibleRecipients.addAll(placement.getMembers());
                }
                for (AuthorisationRequest request : u.getAuthorisationRequests()) {
                    possibleRecipients.add(request.getTutor());
                }
                break;
            }
        }

        possibleRecipients.addAll(uRepo.findByRole(Role.ADMINISTRATOR)); // All users can contact all administrators
        possibleRecipients.remove(u); // Remove self

        List<AppUser> possibleRecipientsList = new ArrayList<>(possibleRecipients); // Convert set to list
        possibleRecipientsList.sort(Comparator.comparing(AppUser::getUsername));

        model.addAttribute("user", u);
        model.addAttribute("possibleRecipientsList", possibleRecipientsList);

        return "messaging/new_message";
    }

    @PostMapping("/messaging/new")
    public String sendMessage(@RequestParam("recipient") String recipientUsername, @RequestParam("content") String content,
                              Principal principal) throws IOException {
        AppUser sender = uRepo.findByUsername(principal.getName());

        if (!uRepo.existsByUsername(recipientUsername)) {
            return "redirect:/messaging/new?invalidRecipient";
        }
        AppUser recipient = uRepo.findByUsername(recipientUsername);

        Message message = new Message(LocalDateTime.now(), sender, recipient, content, encryptPassword);
        mRepo.save(message);

        notificationService.sendNewMessageNotification(message, encryptPassword); // Attempt to send web notification to recipient
        mailService.sendMessageNotification(message); // Attempt to send email notification to recipient

        return "redirect:/messaging/user/" + recipient.getUsername() + "?sendSuccess";
    }

    @GetMapping("/messaging/user/{u2Username}")
    public String conversation(@PathVariable String u2Username, @RequestParam("page") Optional<Integer> page, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());

        if (!uRepo.existsByUsername(u2Username)) {
            return "redirect:/messaging?invalidId";
        }
        AppUser u2 = uRepo.findByUsername(u2Username);

        int pageReturn = 1; // By default, return the first page
        if (page.isPresent()) {
            if (page.get() <= 0) {
                return "redirect:/messaging/user/" + u2Username + "?invalidPage";
            }
            pageReturn = page.get();
        }

        Page<Message> messageChain = mRepo.findAllBySenderInAndRecipientIn(List.of(u, u2), List.of(u, u2),
                PageRequest.of(pageReturn - 1, 10, Sort.by("timestamp").descending()));
        int totalPages = messageChain.getTotalPages();

        List<Message> messages = messageChain.getContent();
        List<Message> messagesLogical = new ArrayList<>(messages);
        messagesLogical.sort(Comparator.comparing(Message::getTimestamp)); // Logical sorting for conversation structure

        model.addAttribute("messages", messagesLogical);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("user", u);
        model.addAttribute("u2", u2);
        model.addAttribute("currentPage", pageReturn);
        model.addAttribute("encryptPassword", encryptPassword);

        return "messaging/conversation";
    }

    @PostMapping("/messaging/delete")
    public String deleteMessage(@RequestParam("messageId") int messageId, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Message> messageOpt = mRepo.findById(messageId);
        if (messageOpt.isEmpty()) {
            return "redirect:/messaging?invalidId";
        }
        Message message = messageOpt.get();

        if (message.getSender() != u) {
            return "redirect:/messaging/user/" + message.getRecipient().getUsername() + "?noDeletePermission";
        }

        // Attempt to send web notification to recipient (if they have the conversation page open, this will attempt to
        // delete the message immediately on their end)
        notificationService.sendMessageDeleteNotification(message);

        mRepo.delete(message);

        return "redirect:/messaging/user/" + message.getRecipient().getUsername() + "?deleteSuccess";
    }

    @GetMapping("/messaging/user/{u2Username}/check")
    @ResponseBody
    public boolean checkForNewMessages(@PathVariable String u2Username, Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());
        AppUser u2 = uRepo.findByUsername(u2Username);

        LocalDateTime since = LocalDateTime.now().minus(30, ChronoUnit.SECONDS); // In the last 30 seconds

        return mRepo.existsBySenderAndRecipientAndTimestampAfter(u, u2, since) || mRepo.existsBySenderAndRecipientAndTimestampAfter(u2, u, since);
    }

    @PostMapping("/messaging/token-store")
    @ResponseBody
    public boolean storeNotificationsDeviceToken(@RequestBody String token, Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        String tokenClean = URLDecoder.decode(token, StandardCharsets.UTF_8);
        tokenClean = tokenClean.substring(1, tokenClean.length() - 2);

        if (u.getDeviceToken() == null || !u.getDeviceToken().equals(tokenClean)) {
            // Set the token
            u.setDeviceToken(tokenClean);
            uRepo.save(u);
        }

        return true; // A token must exist - we can send notifications to this user.
    }

}
