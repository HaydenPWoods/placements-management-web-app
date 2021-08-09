package hpw7.placements.controller;

import com.google.common.net.HttpHeaders;
import hpw7.placements.service.FileService;
import hpw7.placements.domain.*;
import hpw7.placements.repository.*;
import org.apache.tika.Tika;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The <code>AppUserController</code> handles requests relating to users and user management, including the users list,
 * and the ability to change roles and delete users.
 */
@Controller
public class AppUserController {

    private final AppUserRepository uRepo;

    private final AuthorisationRequestRepository arRepo;

    private final DocumentRepository dRepo;

    private final FileService fileService;

    private final LogEntryRepository lRepo;

    private final MessageRepository mRepo;

    private final PlacementRepository plRepo;

    private final PlacementApplicationRepository plAppRepo;

    private final ProviderRepository prRepo;

    public AppUserController(AppUserRepository uRepo, AuthorisationRequestRepository arRepo, DocumentRepository dRepo, FileService fileService, LogEntryRepository lRepo, MessageRepository mRepo, PlacementRepository plRepo, PlacementApplicationRepository plAppRepo, ProviderRepository prRepo) {
        this.uRepo = uRepo;
        this.arRepo = arRepo;
        this.dRepo = dRepo;
        this.fileService = fileService;
        this.lRepo = lRepo;
        this.mRepo = mRepo;
        this.plRepo = plRepo;
        this.plAppRepo = plAppRepo;
        this.prRepo = prRepo;
    }

    @GetMapping("/users")
    public String usersList(@RequestParam("page") Optional<Integer> page, Model model) {
        int pageReturn = 1;
        if (page.isPresent()) {
            if (page.get() <= 0) {
                return "redirect:/users?invalidPage";
            }
            pageReturn = page.get();
        }
        Page<AppUser> userList = uRepo.findAll(PageRequest.of(pageReturn - 1, 10, Sort.by("username")));
        model.addAttribute("userList", userList);
        model.addAttribute("totalUsers", userList.getTotalElements());
        model.addAttribute("totalPages", userList.getTotalPages());
        model.addAttribute("currentPage", pageReturn);
        return "users/list";
    }

    @GetMapping("/profile")
    public String profileRedirect(Principal principal) {
        return "redirect:/users/" + principal.getName();
    }

    @GetMapping("/users/{username}")
    public String userProfile(@PathVariable String username, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        AppUser profileUser = uRepo.findByUsername(username);
        if (profileUser == null) {
            if (u.getRole() == Role.ADMINISTRATOR) {
                return "redirect:/users?invalidId";
            } else {
                return "redirect:/?invalidId";
            }
        }
        Optional<Provider> providerOpt = prRepo.findProviderByMembersContains(profileUser);
        List<Placement> placementList = plRepo.findAllByMembersContains(profileUser, Sort.by("title").ascending());
        model.addAttribute("user", u);
        model.addAttribute("profileUser", profileUser);
        model.addAttribute("providerOpt", providerOpt);
        model.addAttribute("placementList", placementList);
        return "users/profile";
    }

    @GetMapping("/users/{username}/profilePicture")
    public ResponseEntity<Resource> userProfilePicture(@PathVariable String username) {
        AppUser u = uRepo.findByUsername(username);
        Resource defaultProfilePicture = new ClassPathResource("/static/images/profile-default.png");
        if (u == null || u.getProfilePicture() == null) {
            // Return default profile picture
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/png"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + "profile-default.png" + "\"").body(defaultProfilePicture);
        }

        try {
            Path path;
            // Check separator, change if it is not the correct file separator for the current system
            if (!u.getProfilePicture().substring(0, 1).equals(fileService.FS)) {
                String altPathString = u.getProfilePicture().replace('\\', fileService.FS.charAt(0));
                path = Paths.get(System.getProperty("user.dir") + altPathString);
            } else {
                path = Paths.get(System.getProperty("user.dir") + u.getProfilePicture());
            }
            Resource file = new UrlResource(path.toUri());
            Tika tika = new Tika();
            String fileType = tika.detect(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + file.getFilename() + "\"").body(file);
        } catch (Exception e) {
            // Image can no longer be accessed, probably deleted from local filesystem - void any record of it
            u.setProfilePicture(null);
            uRepo.save(u);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/png"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + "profile-default.png" + "\"").body(defaultProfilePicture);
        }
    }

    @PostMapping("/users/{username}/changeRole")
    public String changeUserRole(@PathVariable String username, @RequestParam("role") String roleString, Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());
        AppUser profileUser = uRepo.findByUsername(username);
        if (profileUser == null) {
            return "redirect:/users?invalidId";
        } else if (profileUser == u) {
            return "redirect:/users/" + username + "?noPermissionSelf";
        }
        Role newRole;
        try {
            newRole = Role.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException iAE) {
            return "redirect:/users/" + username + "?invalidRole"; // Given role doesn't exist
        }
        profileUser.setRole(newRole);
        if (newRole != Role.PROVIDER) {
            Optional<Provider> providerOpt = prRepo.findProviderByMembersContains(profileUser);
            if (providerOpt.isPresent()) {
                // profileUser no longer has the (member of) PROVIDER role, so remove the remaining provider membership
                Provider provider = providerOpt.get();
                provider.getMembers().remove(profileUser);
                prRepo.save(provider);
            }
        }
        uRepo.save(profileUser);
        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.APP_USER,
                "User changed role of user '" + username + "' to " + profileUser.getRole().toString()));
        return "redirect:/users/" + username + "?roleChangeSuccess";
    }

    @PostMapping("/users/{username}/changeProfilePicture")
    public String changeProfilePicture(@PathVariable String username, @RequestParam("imageFile") MultipartFile imageFile,
                                       Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());
        AppUser profileUser = uRepo.findByUsername(username);
        if (profileUser == null) {
            return "redirect:/users?invalidId";
        } else if (profileUser != u) {
            return "redirect:/users/" + username + "?noPermission";
        }
        fileService.createProfilePicturesDirectory(); // Create the directory if it doesn't already exist
        if (!fileService.checkFileIsImage(imageFile)) {
            return "redirect:/users/" + username + "?pictureRejected"; // Reject file - not suitable
        }
        u.setProfilePicture(fileService.uploadProfilePicture(imageFile, u));
        uRepo.save(u);
        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.APP_USER,
                "User changed their profile picture"));
        return "redirect:/users/" + username + "?pictureSet";
    }

    @GetMapping("/users/{username}/resetProfilePicture")
    public String resetProfilePicture(@PathVariable String username, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());
        AppUser profileUser = uRepo.findByUsername(username);
        if (profileUser == null) {
            return "redirect:/users?invalidId";
        } else if (profileUser != u && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/users/" + username + "?noPermission";
        }
        fileService.deleteProfilePicture(profileUser);
        profileUser.setProfilePicture(null);
        uRepo.save(profileUser);
        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.APP_USER,
                "User reset their profile picture back to default"));
        return "redirect:/users/" + username + "?pictureReset";
    }

    @GetMapping("/users/{username}/delete")
    public String deleteUser(@PathVariable String username, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());
        AppUser profileUser = uRepo.findByUsername(username);
        if (profileUser == null) {
            return "redirect:/users?invalidId";
        } else if (profileUser == u) {
            return "redirect:/users/" + username + "?noPermissionDeleteSelf";
        }

        // Delete all documents uploaded by the user
        List<Document> documentList = dRepo.findAllByOwner(profileUser);
        for (Document document : documentList) {
            Optional<AuthorisationRequest> arOpt = arRepo.findBySupportingDocsContains(document);
            if (arOpt.isPresent()) {
                AuthorisationRequest ar = arOpt.get();
                ar.getSupportingDocs().remove(document);
                arRepo.save(ar);
            }
            Optional<PlacementApplication> pAppOpt = plAppRepo.findBySupportingDocsContains(document);
            if (pAppOpt.isPresent()) {
                PlacementApplication pApp = pAppOpt.get();
                pApp.getSupportingDocs().remove(document);
                plAppRepo.save(pApp);
            }
            Optional<Placement> pOpt = plRepo.findByDocumentsContains(document);
            if (pOpt.isPresent()) {
                Placement p = pOpt.get();
                p.getDocuments().remove(document);
                plRepo.save(p);
            }
            fileService.deleteDocument(document);
        }

        // Delete all placement applications created by the user
        List<PlacementApplication> deletePApps = profileUser.getPlacementApplications();
        plAppRepo.deleteAll(deletePApps);
        profileUser.getPlacementApplications().clear();

        // Delete all authorisation requests created by the user
        List<AuthorisationRequest> deleteARs = profileUser.getAuthorisationRequests();
        arRepo.deleteAll(deleteARs);
        profileUser.getAuthorisationRequests().clear();

        // Remove the user from authorisation requests where they have been listed as the tutor
        List<AuthorisationRequest> tutorARs = arRepo.findAllByTutor(profileUser);
        for (AuthorisationRequest request : tutorARs) {
            request.setTutor(null);
            arRepo.save(request);
        }

        // Delete any messages sent to and from the user
        mRepo.deleteAll(mRepo.findAllBySender(profileUser));
        mRepo.deleteAll(mRepo.findAllByRecipient(profileUser));

        // Remove the user from any provider they are a member of
        Provider provider = prRepo.findByMembersUsername(profileUser.getUsername());
        if (provider != null) {
            provider.getMembers().remove(profileUser);
            prRepo.save(provider);
        }

        // Remove the user from any placements they are a member of
        List<Placement> placementList = plRepo.findAllByMembersContains(profileUser);
        for (Placement placement : placementList) {
            placement.getMembers().remove(profileUser);
            plRepo.save(placement);
        }

        // Delete any log entries stored for the user
        lRepo.deleteAll(lRepo.findAllByAppUser(profileUser));

        // Delete any profile picture stored for the user
        fileService.deleteProfilePicture(profileUser);

        // Delete the user
        uRepo.delete(profileUser);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.DELETE, LogEntity.APP_USER,
                "User deleted user '" + username + "'"));

        return "redirect:/users/?deleteSuccess";
    }

}
