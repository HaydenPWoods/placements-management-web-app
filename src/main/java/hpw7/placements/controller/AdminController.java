package hpw7.placements.controller;

import hpw7.placements.domain.*;
import hpw7.placements.repository.AppUserRepository;
import hpw7.placements.repository.LogEntryRepository;
import hpw7.placements.service.MailService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The <code>AdminController</code> handles requests relating to admin-specific activities, such as access to the admin
 * panel, allowing access to areas such as application stats and user management.
 */
@Controller
public class AdminController {

    private final AppUserRepository uRepo;

    private final LogEntryRepository lRepo;

    private final MailService mailService;

    public AdminController(AppUserRepository uRepo, LogEntryRepository lRepo, MailService mailService) {
        this.uRepo = uRepo;
        this.lRepo = lRepo;
        this.mailService = mailService;
    }

    @GetMapping("/admin")
    public String adminPanel() {
        return "admin/panel";
    }

    @GetMapping("/admin/send-group-email")
    public String sendGroupEmailForm() {
        return "admin/send_group_email";
    }

    @PostMapping("/admin/send-group-email")
    public String sendGroupEmail(@RequestParam("message") String message,
                                @RequestParam("toStudents") Optional<Boolean> toStudents,
                                @RequestParam("toTutors") Optional<Boolean> toTutors,
                                @RequestParam("toProviders") Optional<Boolean> toProviders,
                                @RequestParam("toAdministrators") Optional<Boolean> toAdministrators,
                                @RequestParam("permissionOverride") Optional<Boolean> permissionOverride,
                                Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        if (toStudents.isEmpty() && toTutors.isEmpty() && toProviders.isEmpty() && toAdministrators.isEmpty()) {
            // No recipients
            return "redirect:/admin/send-group-email?noRecipients";
        }

        // Get all recipients based on "to..." selection
        List<AppUser> recipients = new ArrayList<>();
        if (toStudents.isPresent()) {
            recipients.addAll(uRepo.findByRole(Role.STUDENT));
        }
        if (toTutors.isPresent()) {
            recipients.addAll(uRepo.findByRole(Role.TUTOR));
        }
        if (toProviders.isPresent()) {
            recipients.addAll(uRepo.findByRole(Role.PROVIDER));
        }
        if (toAdministrators.isPresent()) {
            recipients.addAll(uRepo.findByRole(Role.ADMINISTRATOR));
        }

        // Whether to send emails regardless of a recipient's set permission
        boolean override = false;
        if (permissionOverride.isPresent()) {
            override = true;
        }

        // Attempt to send email through the MailService
        mailService.sendGroupEmail(recipients, message, override);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.EMAIL, LogEntity.APP_USER,
                "User sent a group email to users of the application."));

        return "redirect:/admin/send-group-email?mailSuccess";
    }
}
