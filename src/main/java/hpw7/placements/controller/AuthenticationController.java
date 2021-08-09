package hpw7.placements.controller;

import hpw7.placements.repository.ProviderRepository;
import hpw7.placements.service.MailService;
import hpw7.placements.domain.AppUser;
import hpw7.placements.domain.LogAction;
import hpw7.placements.domain.LogEntity;
import hpw7.placements.domain.LogEntry;
import hpw7.placements.dto.AppUserDTO;
import hpw7.placements.dto.EditAccountDetailsDTO;
import hpw7.placements.repository.AppUserRepository;
import hpw7.placements.repository.LogEntryRepository;
import hpw7.placements.service.CustomUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>AuthenticationController</code> handles requests relating to logging-in to the application, registering an
 * account, and updating an account's details. Further user request handling is dealt with in the
 * {@link AppUserController}.
 */
@Controller
public class AuthenticationController {

    private final AppUserRepository uRepo;

    private final CustomUserDetailsService cudService;

    private final LogEntryRepository lRepo;

    private final MailService mailService;

    private final PasswordEncoder pEncoder;

    private final ProviderRepository prRepo;

    public AuthenticationController(AppUserRepository uRepo, CustomUserDetailsService cudService, LogEntryRepository lRepo, MailService mailService, PasswordEncoder pEncoder, ProviderRepository prRepo) {
        this.uRepo = uRepo;
        this.cudService = cudService;
        this.lRepo = lRepo;
        this.mailService = mailService;
        this.pEncoder = pEncoder;
        this.prRepo = prRepo;
    }

    @ModelAttribute("user")
    public AppUserDTO appUserDTO() {
        return new AppUserDTO();
    }

    @GetMapping("/app-login")
    public String login(Principal principal) {
        if (principal == null) {
            return "authentication/login";
        } else if (uRepo.existsByUsername(principal.getName())) {
            return "redirect:/dashboard"; // User is already authenticated
        } else {
            return "authentication/login";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        // Showing message on dashboard if user has 'member of provider' role, but isn't a member of any provider
        boolean memberOfNoProvider = prRepo.findProviderByMembersContains(u).isEmpty();
        model.addAttribute("memberOfNoProvider", memberOfNoProvider);

        return "app/dashboard";
    }

    @GetMapping("/app-register")
    public String registerPage(Principal principal) {
        if (principal == null) {
            return "authentication/register";
        } else if (uRepo.existsByUsername(principal.getName())) {
            return "redirect:/dashboard"; // User is already authenticated
        } else {
            return "authentication/register";
        }
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") @Valid AppUserDTO appUserDTO, BindingResult bindingResult) {
        // Users cannot have duplicate usernames and emails, check and reject here if AJAX validation failed
        if (uRepo.existsByUsername(appUserDTO.getUsername())) {
            bindingResult.rejectValue("username", "username.exists",
                    "A user exists with the given username!");
        }
        if (uRepo.existsByEmail(appUserDTO.getEmail())) {
            bindingResult.rejectValue("email", "email.exists",
                    "Specified email has already been used");
        }
        if (bindingResult.hasErrors()) {
            return "authentication/register";
        } else {
            AppUser u = cudService.save(appUserDTO);
            lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.REGISTER, LogEntity.APP_USER,
                    "User registered their account."));
            return "authentication/registered";
        }
    }

    @GetMapping("/account/edit")
    public String editAccountDetailsPage(Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);
        EditAccountDetailsDTO editAccountDetailsDTO = new EditAccountDetailsDTO();
        editAccountDetailsDTO.setEmail(u.getEmail());
        editAccountDetailsDTO.setFullName(u.getFullName());
        editAccountDetailsDTO.setEmailsEnabled(u.isEmailsEnabled());
        model.addAttribute("editAccountDetailsDTO", editAccountDetailsDTO);
        return "authentication/edit_details";
    }

    @PostMapping("/account/edit")
    public String editAccountDetails(@ModelAttribute("editAccountDetailsDTO") @Valid EditAccountDetailsDTO editAccountDetailsDTO,
                                     BindingResult bindingResult, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        List<LogEntry> logEntries = new ArrayList<>(); // Creating log entry for each user change made

        if (!editAccountDetailsDTO.getEmail().equals("") && !editAccountDetailsDTO.getEmail().equals(u.getEmail())) {
            if (uRepo.existsByEmail(editAccountDetailsDTO.getEmail())) {
                bindingResult.rejectValue("email", "email.exists",
                        "Specified email has already been used");
            } else {
                String oldEmail = u.getEmail();
                u.setEmail(editAccountDetailsDTO.getEmail());
                logEntries.add(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.APP_USER,
                        "User changed their email from '" + oldEmail + "' to '" + editAccountDetailsDTO.getEmail() + "'"));
            }
        }

        if (!editAccountDetailsDTO.isEmailsEnabled() == u.isEmailsEnabled()) {
            u.setEmailsEnabled(editAccountDetailsDTO.isEmailsEnabled());
            logEntries.add(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.APP_USER,
                    "User changed their system emails permission to '" + editAccountDetailsDTO.isEmailsEnabled() + "'"));
        }


        if (!editAccountDetailsDTO.getFullName().equals("") && !editAccountDetailsDTO.getFullName().equals(u.getFullName())) {
            String oldName = u.getFullName();
            u.setFullName(editAccountDetailsDTO.getFullName());
            logEntries.add(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.APP_USER,
                    "User changed their name from '" + oldName + "' to '" + editAccountDetailsDTO.getFullName() + "'"));
        }

        if (!editAccountDetailsDTO.getPassword().equals("")) {
            u.setPassword(pEncoder.encode(editAccountDetailsDTO.getPassword()));
            logEntries.add(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.APP_USER,
                    "User updated their password."));
        }

        if (bindingResult.hasErrors()) {
            return "authentication/edit_details";
        } else {
            uRepo.save(u); // Save changes

            if (!logEntries.isEmpty()) {
                lRepo.saveAll(logEntries);
            }

            if (!editAccountDetailsDTO.getPassword().equals("")) {
                // Password was changed, change is successful, send notification.
                mailService.sendPasswordChangeNotification(u);
            }

            return "redirect:/users/" + u.getUsername() + "?detailssuccess";
        }
    }

}
