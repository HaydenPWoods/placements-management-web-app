package hpw7.placements.controller;

import hpw7.placements.service.FileService;
import hpw7.placements.domain.*;
import hpw7.placements.dto.PlacementApplicationDTO;
import hpw7.placements.repository.*;
import hpw7.placements.service.MailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The <code>PlacementApplicationController</code> handles requests relating to management of placement applications
 * through the application, including the application process for students, and the approval process for a member of
 * the relevant provider, and an administrator.
 */
@Controller
public class PlacementApplicationController {

    private final AppUserRepository uRepo;

    private final DocumentRepository dRepo;

    private final FileService fileService;

    private final LogEntryRepository lRepo;

    private final MailService mailService;

    private final PlacementRepository plRepo;

    private final PlacementApplicationRepository plAppRepo;

    private final ProviderRepository prRepo;

    public PlacementApplicationController(AppUserRepository uRepo, DocumentRepository dRepo, FileService fileService, LogEntryRepository lRepo, MailService mailService, PlacementRepository plRepo, PlacementApplicationRepository plAppRepo, ProviderRepository prRepo) {
        this.uRepo = uRepo;
        this.dRepo = dRepo;
        this.fileService = fileService;
        this.lRepo = lRepo;
        this.mailService = mailService;
        this.plRepo = plRepo;
        this.plAppRepo = plAppRepo;
        this.prRepo = prRepo;
    }

    @GetMapping("/placements/{placementId}/apply")
    public String placementApplicationForm(Principal principal, Model model, @PathVariable int placementId) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<Placement> pOpt = plRepo.findById(placementId); // The placement being applied to
        if (pOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement placement = pOpt.get();

        if (placement.getApplicationDeadline().isBefore(LocalDateTime.now())) {
            return "redirect:/placements/" + placementId + "?deadlinePassed"; // Not accepting applications
        }

        model.addAttribute("placement", placement);

        PlacementApplicationDTO placementApplicationDTO = new PlacementApplicationDTO(); // Storing application details
        model.addAttribute("placementApplicationDTO", placementApplicationDTO);

        return "placements/applications/new_application";
    }

    @PostMapping("/placements/{placementId}/apply")
    public String placementApplication(@PathVariable int placementId,
                                       @ModelAttribute("placementApplicationDTO") @Valid PlacementApplicationDTO
                                               placementApplicationDTO,
                                       @RequestParam("description") Optional<String[]> descriptionsOpt,
                                       BindingResult bindingResult, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Placement> pOpt = plRepo.findById(placementId);
        if (pOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }

        if (bindingResult.hasErrors()) {
            return "placements/applications/new_application";
        } else {
            Placement p = pOpt.get();
            PlacementApplication pApp = new PlacementApplication(placementApplicationDTO.getDetails());
            pApp.setPlacement(p);

            boolean blockedFile = false; // Keeping a record if any files need to be blocked (are of a disallowed type)
            if (!placementApplicationDTO.getFiles()[0].isEmpty()) {
                if (descriptionsOpt.isEmpty()) {
                    return "redirect:/placements/" + placementId + "/apply?invalid"; // Needs to be a description for each file
                }
                String[] descriptions = descriptionsOpt.get();

                if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "uploads"))) {
                    fileService.createUploadsDirectory();
                }

                int i = 0; // Accessing description for each file
                for (MultipartFile file : placementApplicationDTO.getFiles()) {
                    if (fileService.checkFileAcceptable(file)) {
                        Document document = fileService.uploadFile(file, u);
                        document.setDescription(descriptions[i]);
                        dRepo.save(document);
                        pApp.getSupportingDocs().add(document);
                    } else {
                        blockedFile = true;
                    }
                    i++;
                }
            }

            plAppRepo.save(pApp);
            u.getPlacementApplications().add(pApp);
            uRepo.save(u);

            lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.PLACEMENT_APPLICATION, pApp.getId(),
                    "User created placement application"));

            if (blockedFile) {
                return "redirect:/placements/" + placementId + "?applySuccessBlockedFile";
            } else {
                return "redirect:/placements/" + placementId + "?applySuccess";
            }
        }
    }

    @GetMapping("/applications")
    public String placementApplicationList(Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        // Three lists of placement applications - different categories:
        List<PlacementApplication> applicationList; // Current user needs to approve (or, for student, requests they have made)
        List<PlacementApplication> applicationListSecondary; // Waiting for others approval
        List<PlacementApplication> applicationListTertiary; // Approved applications (admin)

        Map<PlacementApplication, AppUser> applicationByStudent = new HashMap<>();

        switch (u.getRole()) {
            case ADMINISTRATOR:
                // Return all applications, sorted into the different categories
                applicationList = plAppRepo.findAllByApprovedByAdministrator(false);
                model.addAttribute("applicationList", applicationList);

                applicationListSecondary = plAppRepo.findAllByApprovedByProvider(false);
                applicationListSecondary.removeAll(applicationList); // Remove duplicates
                model.addAttribute("applicationListSecondary", applicationListSecondary);

                applicationListTertiary = plAppRepo.findAllApproved();
                model.addAttribute("applicationListTertiary", applicationListTertiary);

                // Populate application <-> student mapping, for easy retrieval of who created an application
                for (PlacementApplication application : applicationList) {
                    applicationByStudent.put(application, uRepo.findByPlacementApplicationsContaining(application));
                }
                for (PlacementApplication application : applicationListSecondary) {
                    applicationByStudent.put(application, uRepo.findByPlacementApplicationsContaining(application));
                }
                for (PlacementApplication application : applicationListTertiary) {
                    applicationByStudent.put(application, uRepo.findByPlacementApplicationsContaining(application));
                }
                model.addAttribute("applicationByStudent", applicationByStudent);

                return "placements/applications/application_list";
            case PROVIDER:
                // Return all applications for placements of the provider the user is a member of, sorted by approval status
                applicationList = plAppRepo.findAllForProviderApproval(prRepo.findByMembersUsername(u.getUsername()));
                model.addAttribute("applicationList", applicationList);

                applicationListSecondary = plAppRepo.findAllForProviderAwaitingAdminApproval(prRepo.findByMembersUsername(u.getUsername()));
                model.addAttribute("applicationListSecondary", applicationListSecondary);

                // Populate application <-> student mapping
                for (PlacementApplication application : applicationList) {
                    applicationByStudent.put(application, uRepo.findByPlacementApplicationsContaining(application));
                }
                for (PlacementApplication application : applicationListSecondary) {
                    applicationByStudent.put(application, uRepo.findByPlacementApplicationsContaining(application));
                }
                model.addAttribute("applicationByStudent", applicationByStudent);

                return "placements/applications/application_list";
            case TUTOR:
                // Tutors do not need access to placement applications - deny, and redirect
                return "redirect:/dashboard?applicationsNoPermission";
            default: // Student
                // Retrieve the student's own placement applications
                applicationList = u.getPlacementApplications();
                applicationListSecondary = new ArrayList<>(); // Empty
                model.addAttribute("applicationList", applicationList);
                model.addAttribute("applicationListSecondary", applicationListSecondary);

                // Populate application <-> student mapping (with self)
                for (PlacementApplication application : applicationList) {
                    applicationByStudent.put(application, u);
                }
                model.addAttribute("applicationByStudent", applicationByStudent);

                return "placements/applications/application_list";
        }
    }

    @GetMapping("/applications/{applicationId}")
    public String singleApplication(@PathVariable int applicationId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<PlacementApplication> pAppOpt = plAppRepo.findById(applicationId);
        if (pAppOpt.isEmpty()) {
            return "redirect:/applications?invalidId";
        }
        PlacementApplication pApp = pAppOpt.get();
        model.addAttribute("pApp", pApp);

        AppUser student = uRepo.findByPlacementApplicationsContaining(pApp);
        model.addAttribute("student", student);

        if (u.getRole() != Role.ADMINISTRATOR
                && u != student
                && !pApp.getPlacement().getProvider().getMembers().contains(u)) {
            return "redirect:/applications?noPermission"; // No permission to view the application + related documents
        }

        return "placements/applications/application";
    }

    @PostMapping("/applications/{applicationId}/approve")
    public String approveApplication(@PathVariable int applicationId, Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<PlacementApplication> pAppOpt = plAppRepo.findById(applicationId);
        if (pAppOpt.isEmpty()) {
            return "redirect:/applications?invalidId";
        }
        PlacementApplication pApp = pAppOpt.get();

        if (u.getRole() == Role.ADMINISTRATOR) {
            // Approve as admin
            pApp.setApprovedByAdministrator(true);
            plAppRepo.save(pApp);

            lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.APPROVE, LogEntity.PLACEMENT_APPLICATION, pApp.getId(),
                    "User approved placement application as an ADMINISTRATOR"));
        } else if (u.getRole() == Role.PROVIDER && prRepo.findByMembersUsername(u.getUsername()) == pApp.getPlacement().getProvider()) {
            // Approve as member of provider
            pApp.setApprovedByProvider(true);
            plAppRepo.save(pApp);

            lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.APPROVE, LogEntity.PLACEMENT_APPLICATION, pApp.getId(),
                    "User approved placement application as a member of PROVIDER"));
        } else {
            return "redirect:/applications/" + applicationId + "?noPermission"; // User cannot approve this application
        }

        if (pApp.isApprovedByAdministrator() && pApp.isApprovedByProvider()) {
            // Application is fully approved, add student to placement
            Placement p = pApp.getPlacement();
            p.getMembers().add(uRepo.findByPlacementApplicationsContaining(pApp));
            plRepo.save(p);

            lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.ADD_MEMBER, LogEntity.PLACEMENT, p.getId(),
                    "User's approval of placement application #" + pApp.getId() + " caused the application " +
                            "to become fully approved, and triggered the student to be added to the placement"));

            // Attempt to notify student by email
            mailService.sendApplicationApprovedNotification(uRepo.findByPlacementApplicationsContaining(pApp), pApp);

            return "redirect:/placements/" + p.getId() + "?appApproved";
        }

        return "redirect:/applications/" + applicationId + "?approved";
    }

    @PostMapping("/applications/{applicationId}/delete")
    public String deleteApplication(@PathVariable int applicationId, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<PlacementApplication> pAppOpt = plAppRepo.findById(applicationId);
        if (pAppOpt.isEmpty()) {
            return "redirect:/applications?invalidId";
        }
        PlacementApplication pApp = pAppOpt.get();

        if (u.getRole() != Role.ADMINISTRATOR && u != uRepo.findByPlacementApplicationsContaining(pApp)) {
            return "redirect:/applications/" + applicationId + "?noDeletePermission";
        }

        if (!pApp.getSupportingDocs().isEmpty()) {
            // Remove any related documents, delete from local filesystem
            List<Document> documents = pApp.getSupportingDocs();
            pApp.getSupportingDocs().clear();

            for (Document document : documents) {
                fileService.deleteDocument(document);
            }
        }

        plAppRepo.delete(pApp);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.DELETE, LogEntity.PLACEMENT_APPLICATION, pApp.getId(),
                "User deleted placement application"));

        return "redirect:/applications?deleteSuccess";
    }

}
