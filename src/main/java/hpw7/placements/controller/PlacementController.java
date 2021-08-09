package hpw7.placements.controller;

import hpw7.placements.domain.*;
import hpw7.placements.dto.EditPlacementDTO;
import hpw7.placements.dto.PlacementDTO;
import hpw7.placements.repository.*;
import hpw7.placements.service.FileService;
import hpw7.placements.service.MapService;
import org.springframework.data.domain.Sort;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The <code>PlacementController</code> handles requests relating to placements, including the creation of placements by
 * a provider, the listing of placements open for applications for students, and the management of users on these
 * placements.
 */
@Controller
public class PlacementController {

    private final AppUserRepository uRepo;

    private final DocumentRepository dRepo;

    private final FileService fileService;

    private final LogEntryRepository lRepo;

    private final MapService mapService;

    private final PlacementRepository plRepo;

    private final PlacementVisitRepository vRepo;

    private final ProviderRepository prRepo;

    public PlacementController(AppUserRepository uRepo, DocumentRepository dRepo, FileService fileService, LogEntryRepository lRepo, MapService mapService, PlacementRepository plRepo, ProviderRepository prRepo, PlacementVisitRepository vRepo) {
        this.uRepo = uRepo;
        this.dRepo = dRepo;
        this.fileService = fileService;
        this.lRepo = lRepo;
        this.mapService = mapService;
        this.plRepo = plRepo;
        this.prRepo = prRepo;
        this.vRepo = vRepo;
    }

    @GetMapping("/placements")
    public String placementList(Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        List<Placement> placements;

        switch (u.getRole()) {
            case ADMINISTRATOR: {
                // Return all stored placements
                placements = plRepo.findAll(Sort.by("title").ascending());
                model.addAttribute("placements", placements);
                break;
            }
            case PROVIDER: {
                // Return all placements for the provider the user is a member of
                placements = plRepo.findAllByProvider(prRepo.findByMembersUsername(u.getUsername()), Sort.by("title").ascending());
                model.addAttribute("placements", placements);
                break;
            }
            case TUTOR: {
                // Return all placements where the tutor is a member
                placements = plRepo.findAllByMembersContains(u, Sort.by("title").ascending());
                model.addAttribute("placements", placements);
                break;
            }
            default: { // Student
                // Return all placements where the student is a member
                placements = plRepo.findAllByMembersContains(u, Sort.by("title").ascending());
                model.addAttribute("placements", placements);

                // Return all placements open for applications
                List<Placement> placementsOpen = plRepo.findAllByApplicationDeadlineAfter(LocalDateTime.now(), Sort.by("title").ascending());
                placementsOpen.removeAll(placements);
                model.addAttribute("placementsOpen", placementsOpen);

                // Storing titles, locations, and id's for showing pins on map for open placements
                List<String> placementsOpenTitleList = new ArrayList<>();
                List<Double> placementsOpenLatList = new ArrayList<>();
                List<Double> placementsOpenLngList = new ArrayList<>();
                List<Integer> placementsOpenIdList = new ArrayList<>();

                for (Placement placement : placementsOpen) {
                    placementsOpenTitleList.add(placement.getTitle());
                    placementsOpenLatList.add(placement.getProvider().getLatitude());
                    placementsOpenLngList.add(placement.getProvider().getLongitude());
                    placementsOpenIdList.add(placement.getId());
                }

                model.addAttribute("placementsOpenTitleList", placementsOpenTitleList);
                model.addAttribute("placementsOpenLatList", placementsOpenLatList);
                model.addAttribute("placementsOpenLngList", placementsOpenLngList);
                model.addAttribute("placementsOpenIdList", placementsOpenIdList);

                break;
            }
        }

        // Storing titles, locations, and id's for showing pins on map for listed placements
        List<String> placementTitleList = new ArrayList<>();
        List<Double> placementLatList = new ArrayList<>();
        List<Double> placementLngList = new ArrayList<>();
        List<Integer> placementIdList = new ArrayList<>();

        for (Placement placement : placements) {
            placementTitleList.add(placement.getTitle());
            placementLatList.add(placement.getProvider().getLatitude());
            placementLngList.add(placement.getProvider().getLongitude());
            placementIdList.add(placement.getId());
        }

        model.addAttribute("placementTitleList", placementTitleList);
        model.addAttribute("placementLatList", placementLatList);
        model.addAttribute("placementLngList", placementLngList);
        model.addAttribute("placementIdList", placementIdList);

        // Get Maps API key, add to model for use in page JavaScript
        String clientKey = mapService.gmapsAPIClientKey;
        model.addAttribute("clientKey", clientKey);

        return "placements/placement_list";
    }

    @GetMapping("/placements/{placementId}")
    public String onePlacement(@PathVariable("placementId") int placementId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<Placement> pOpt = plRepo.findById(placementId);
        if (pOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement p = pOpt.get();
        model.addAttribute("placement", p);

        // Logic to determine if the user has permission to edit the placement details
        boolean editPermissions = (u.getRole() == Role.ADMINISTRATOR || (u.getRole() == Role.TUTOR && p.getMembers().contains(u))
                || (u.getRole() == Role.PROVIDER && p.getProvider() == prRepo.findByMembersUsername(u.getUsername())));
        model.addAttribute("editPermissions", editPermissions);

        // Get current time, can then check during template processing if deadline has surpassed
        LocalDateTime now = LocalDateTime.now();
        model.addAttribute("now", now);

        // Get all valid students and tutors that can be added to the placement (not including existing members)
        List<AppUser> students = uRepo.findByRole(Role.STUDENT);
        students.removeAll(p.getMembers());
        model.addAttribute("students", students);

        List<AppUser> tutors = uRepo.findByRole(Role.TUTOR);
        tutors.removeAll(p.getMembers());
        model.addAttribute("tutors", tutors);

        return "placements/placement";
    }

    @GetMapping("/placements/{placementId}/edit")
    public String editPlacementForm(@PathVariable("placementId") int placementId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<Placement> pOpt = plRepo.findById(placementId);
        if (pOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement p = pOpt.get();
        model.addAttribute("placement", p);

        // Populate edit form with existing placement details
        EditPlacementDTO editPlacementDTO = new EditPlacementDTO(p.getTitle(), p.getDescription(),
                p.getApplicationDeadline().toLocalDate().toString(),
                p.getApplicationDeadline().toLocalTime().toString(),
                p.getStartDate().toString(),
                p.getEndDate().toString());
        model.addAttribute("editPlacementDTO", editPlacementDTO);

        return "placements/edit_placement";
    }

    @PostMapping("/placements/{placementId}/edit")
    public String editPlacement(@ModelAttribute("editPlacementDTO") @Valid EditPlacementDTO editPlacementDTO,
                                @PathVariable("placementId") int placementId, BindingResult bindingResult,
                                Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        if (bindingResult.hasErrors()) {
            return "placements/edit_placement";
        } else {
            Optional<Placement> pOpt = plRepo.findById(placementId);
            if (pOpt.isEmpty()) {
                return "redirect:/placements?invalidId";
            }
            Placement p = pOpt.get();

            // Logic to determine if the user has permission to edit the placement details
            boolean editPermissions = (u.getRole() == Role.ADMINISTRATOR || (u.getRole() == Role.TUTOR && p.getMembers().contains(u))
                    || (u.getRole() == Role.PROVIDER && p.getProvider() == prRepo.findByMembersUsername(u.getUsername())));

            if (!editPermissions) {
                return "redirect:/placements/" + p.getId() + "?noEditPermission";
            }

            List<LogEntry> logEntries = new ArrayList<>(); // Storing created log entries, saving to database if changes are successful

            // Check each field - if changes made, apply the changes (if valid) and log the change
            if (editPlacementDTO.getTitle() != null && !editPlacementDTO.getTitle().equals("") && !p.getTitle().equals(editPlacementDTO.getTitle())) {
                String oldTitle = p.getTitle();
                p.setTitle(editPlacementDTO.getTitle());

                logEntries.add(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PLACEMENT, p.getId(),
                        "User changed placement title from '" + oldTitle + "' to '" + p.getTitle() + "'"));
            }

            if (editPlacementDTO.getDescription() != null && !editPlacementDTO.getDescription().equals("") && !p.getDescription().equals(editPlacementDTO.getDescription())) {
                p.setDescription(editPlacementDTO.getDescription());

                logEntries.add(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PLACEMENT, p.getId(),
                        "User changed placement description"));
            }

            if (editPlacementDTO.getApplicationDeadline() != null && editPlacementDTO.getApplicationDeadlineTime() != null &&
                    (!editPlacementDTO.getApplicationDeadline().equals("") ||
                            !editPlacementDTO.getApplicationDeadlineTime().equals(""))) {
                LocalDateTime oldDateTime = p.getApplicationDeadline();

                LocalDate date = p.getApplicationDeadline().toLocalDate(); // New date
                if (!editPlacementDTO.getApplicationDeadline().equals("")) {
                    // Date set, use new date
                    date = LocalDate.parse(editPlacementDTO.getApplicationDeadline());
                }

                LocalTime time = p.getApplicationDeadline().toLocalTime();
                if (!editPlacementDTO.getApplicationDeadlineTime().equals("")) {
                    // Time set, use new time
                    time = LocalTime.parse(editPlacementDTO.getApplicationDeadlineTime());
                }

                LocalDateTime dateTime = LocalDateTime.of(date, time);

                if (dateTime.isBefore(LocalDateTime.now())) {
                    bindingResult.rejectValue("applicationDeadline", "applicationDeadline.beforenow",
                            "Application deadline has already surpassed!");
                    return "placements/edit_placement";
                }
                p.setApplicationDeadline(dateTime);
                logEntries.add(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PLACEMENT, p.getId(),
                        "User changed placement application deadline from '" + oldDateTime + "' to '" + dateTime + "'"));
            }

            if (!editPlacementDTO.getStartDate().equals("")) {
                LocalDate oldDate = p.getStartDate();

                LocalDate startDate = LocalDate.parse(editPlacementDTO.getStartDate());
                p.setStartDate(startDate);

                logEntries.add(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PLACEMENT, p.getId(),
                        "User changed placement start date from '" + oldDate + "' to '" + startDate + "'"));
            }

            if (!editPlacementDTO.getEndDate().equals("")) {
                LocalDate oldDate = p.getEndDate();

                LocalDate endDate = LocalDate.parse(editPlacementDTO.getEndDate());
                p.setEndDate(endDate);

                logEntries.add(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PLACEMENT, p.getId(),
                        "User changed placement end date from '" + oldDate + "' to '" + endDate + "'"));
            }

            plRepo.save(p);
            lRepo.saveAll(logEntries);

            return "redirect:/placements/" + placementId + "?editSuccess";
        }
    }

    @PostMapping("/placements/{placementId}/delete")
    public String deletePlacement(@PathVariable int placementId, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Placement> pOpt = plRepo.findById(placementId);
        if (pOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement p = pOpt.get();

        if (!p.getDocuments().isEmpty()) {
            // Remove any documents, delete from local filesystem
            List<Document> documents = p.getDocuments();
            p.getDocuments().clear();

            for (Document document : documents) {
                fileService.deleteDocument(document);
            }
        }

        if (!p.getVisits().isEmpty()) {
            // Stored visits no longer relevant, delete them
            List<PlacementVisit> visits = p.getVisits();
            p.getVisits().clear();
            vRepo.deleteAll(visits);
        }

        plRepo.delete(p);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.DELETE, LogEntity.PLACEMENT, p.getId(),
                "User deleted placement, and all related documents and visits"));

        return "redirect:/placements?deleteSuccess";
    }

    @PostMapping("/placements/{placementId}/assign-user")
    public String assignUserToPlacement(@PathVariable("placementId") int placementId, @RequestParam("username") String username, Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        AppUser appUser = uRepo.findByUsername(username); // User to be added

        Optional<Placement> pOpt = plRepo.findById(placementId);
        if (pOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement p = pOpt.get();

        if (!p.getMembers().contains(appUser)) {
            List<AppUser> members = p.getMembers();
            members.add(appUser);
            p.setMembers(members);
            plRepo.save(p);
        }

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.ADD_MEMBER, LogEntity.PLACEMENT, p.getId(),
                "User assigned '" + appUser.getUsername() + "' (" + appUser.getRole().name() + ") as a member of the placement"));

        return "redirect:/placements/" + placementId + "?addSuccess";
    }

    @PostMapping("/placements/{placementId}/remove-user")
    public String removeUserFromPlacement(@PathVariable("placementId") int placementId,
                                          @RequestParam("username") String username, Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        AppUser appUser = uRepo.findByUsername(username); // User to be removed

        Optional<Placement> pOpt = plRepo.findById(placementId);
        if (pOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement p = pOpt.get();

        // Logic to determine if the user has permission to edit the placement details
        boolean editPermissions = (u.getRole() == Role.ADMINISTRATOR || (u.getRole() == Role.TUTOR && p.getMembers().contains(u))
                || (u.getRole() == Role.PROVIDER && p.getProvider() == prRepo.findByMembersUsername(u.getUsername())));

        if (!editPermissions) {
            return "redirect:/placements/" + p.getId() + "?noPermission";
        }

        p.getMembers().remove(appUser);

        plRepo.save(p);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.REMOVE_MEMBER, LogEntity.PLACEMENT, p.getId(),
                "User removed '" + appUser.getUsername() + "' (" + appUser.getRole().name() + ") from the placement"));

        return "redirect:/placements/" + placementId + "?userRemoved";
    }

    @GetMapping("/providers/{providerId}/placements")
    public String placementListOfProvider(@PathVariable("providerId") int providerId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<Provider> providerOpt = prRepo.findById(providerId);
        if (providerOpt.isEmpty()) {
            return "redirect:/providers?invalidId";
        }
        Provider provider = providerOpt.get();

        List<Placement> placements; // Placements offered by the provider
        if (u.getRole() == Role.ADMINISTRATOR || (u.getRole() == Role.PROVIDER && prRepo.findByMembersUsername(u.getUsername()).getId() == providerId)) {
            // Return all the provider's placements
            placements = plRepo.findAllByProvider(provider, Sort.by("title").ascending());
        } else {
            // Return all the provider's placements where the user is a member
            placements = plRepo.findAllByProviderAndMembersContains(provider, u, Sort.by("title").ascending());
        }

        // Storing titles, locations, and id's for showing pins on map for listed placements
        List<String> placementTitleList = new ArrayList<>();
        List<Double> placementLatList = new ArrayList<>();
        List<Double> placementLngList = new ArrayList<>();
        List<Integer> placementIdList = new ArrayList<>();

        for (Placement placement : placements) {
            placementTitleList.add(placement.getTitle());
            placementLatList.add(placement.getProvider().getLatitude());
            placementLngList.add(placement.getProvider().getLongitude());
            placementIdList.add(placement.getId());
        }

        model.addAttribute("placements", placements);
        model.addAttribute("placementTitleList", placementTitleList);
        model.addAttribute("placementLatList", placementLatList);
        model.addAttribute("placementLngList", placementLngList);
        model.addAttribute("placementIdList", placementIdList);

        return "placements/placement_list";
    }

    /**
     * Populating the provider selection list for the new placement form with valid providers, according to the user's
     * role.
     *
     * @param principal Details of the authenticated user. Can get the name of the principal to retrieve the current user.
     * @param model     Storing attributes that can be accessed during template processing.
     * @return A String location of the "add placement" form template, with providers loaded into the model.
     */
    private String providersPopulate(Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());

        if (u.getRole().equals(Role.PROVIDER)) {
            // Can only choose the provider they are a member of
            List<Provider> memberOfProvider = new ArrayList<>();
            memberOfProvider.add(prRepo.findByMembersUsername(u.getUsername()));
            model.addAttribute("providers", memberOfProvider);
        } else {
            // (Admins) Can choose any listed provider
            List<Provider> providers = prRepo.findAll(Sort.by("name").ascending());
            model.addAttribute("providers", providers);
        }

        return "placements/add_placement";
    }

    @GetMapping("/placements/add")
    public String addPlacementForm(Principal principal, Model model) {
        model.addAttribute("placementDTO", new PlacementDTO());

        return providersPopulate(principal, model);
    }

    @PostMapping("/placements/add")
    public String addPlacement(@ModelAttribute("placementDTO") @Valid PlacementDTO placementDTO,
                               @RequestParam("description") String[] descriptions, BindingResult bindingResult,
                               Principal principal, Model model) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        LocalDateTime applicationDeadline = null;
        if (!placementDTO.getApplicationDeadline().equals("") || !placementDTO.getApplicationDeadlineTime().equals("")) {
            LocalDate date = LocalDate.parse(placementDTO.getApplicationDeadline());
            LocalTime time = LocalTime.parse(placementDTO.getApplicationDeadlineTime());
            applicationDeadline = LocalDateTime.of(date, time);

            if (applicationDeadline.isBefore(LocalDateTime.now())) {
                bindingResult.rejectValue("applicationDeadline", "applicationDeadline.beforeNow",
                        "Application deadline has already surpassed!"); // Invalid application deadline
            }
        }

        if (bindingResult.hasErrors()) {
            return providersPopulate(principal, model); // Return back to the form, no placement added
        } else {
            LocalDate startDate = LocalDate.parse(placementDTO.getStartDate());
            LocalDate endDate = LocalDate.parse(placementDTO.getEndDate());

            Placement placement = new Placement(placementDTO.getTitle(), placementDTO.getDescription(),
                    applicationDeadline, startDate, endDate);

            Provider provider = prRepo.findById(placementDTO.getProviderId()).get();
            placement.setProvider(provider);

            boolean blockedFile = false; // Keeping a record if any files need to be blocked (are of a disallowed type)
            if (!placementDTO.getFiles()[0].isEmpty()) {
                if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "uploads"))) {
                    fileService.createUploadsDirectory();
                }

                int i = 0; // Accessing descriptions for each file
                for (MultipartFile file : placementDTO.getFiles()) {
                    if (fileService.checkFileAcceptable(file)) {
                        Document document = fileService.uploadFile(file, u);
                        document.setDescription(descriptions[i]);
                        dRepo.save(document);
                        placement.getDocuments().add(document);
                    } else {
                        blockedFile = true;
                    }
                    i++;
                }
            }

            plRepo.save(placement);

            prRepo.save(provider);

            lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.PLACEMENT, placement.getId(),
                    "User created new placement for provider '" + provider.getName() + "' (#" + provider.getId() + ")"));

            if (blockedFile) {
                return "redirect:/placements?addPlacementSuccessBlockedFile";
            } else {
                return "redirect:/placements?addPlacementSuccess";
            }
        }
    }
}
