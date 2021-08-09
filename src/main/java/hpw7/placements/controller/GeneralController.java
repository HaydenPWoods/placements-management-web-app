package hpw7.placements.controller;

import hpw7.placements.domain.*;
import hpw7.placements.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The <code>GeneralController</code> handles requests that do not fit any other controller. Currently, the controller
 * handles the redirect to the login page from root ('/'), and the search logic.
 */
@Controller
public class GeneralController {

    private final AppUserRepository uRepo;

    private final AuthorisationRequestRepository arRepo;

    private final PlacementApplicationRepository plAppRepo;

    private final PlacementRepository plRepo;

    private final ProviderRepository prRepo;

    public GeneralController(AuthorisationRequestRepository arRepo, PlacementRepository plRepo, ProviderRepository prRepo, AppUserRepository uRepo, PlacementApplicationRepository plAppRepo) {
        this.arRepo = arRepo;
        this.plRepo = plRepo;
        this.prRepo = prRepo;
        this.uRepo = uRepo;
        this.plAppRepo = plAppRepo;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/app-login";
    }

    @GetMapping("/search")
    public String search(@RequestParam("query") String query, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());

        // Getting results and storing in lists to add to model
        List<AuthorisationRequest> requestResults = new ArrayList<>();
        List<Placement> placementResults = new ArrayList<>();

        switch (u.getRole()) {
            case ADMINISTRATOR: {
                // No restrictions for any of the results, admins can see everything
                requestResults = arRepo.search(query);
                placementResults = plRepo.search(query);
                break;
            }
            case TUTOR: {
                // Only returning results specific to the tutor - i.e. they are a member of the entity.
                requestResults = arRepo.searchTutor(query, u.getUsername());
                placementResults = plRepo.search(query);
                // Remove any placements from results where tutor is not a member
                placementResults.removeIf(placement -> !placement.getMembers().contains(u));
                break;
            }
            case PROVIDER: {
                // Only returning results specific to the provider the user is a member of.
                Provider p = prRepo.findByMembersUsername(u.getUsername());
                if (p == null) {
                    return "redirect:/dashboard?noProvider"; // User isn't assigned to a provider, cannot search
                }
                requestResults = arRepo.searchProvider(query, p.getName());
                placementResults = plRepo.searchProvider(query, p.getId());
                break;
            }
            case STUDENT: {
                // Only returning results specific to the student - i.e. they have created the entity, or are a member of it.
                requestResults = arRepo.searchStudent(query, u.getUsername());
                placementResults = plRepo.search(query);
                // Remove any placements from results where student is not a member
                placementResults.removeIf(placement -> !placement.getMembers().contains(u));
                break;
            }
        }

        model.addAttribute("requestResults", requestResults);
        model.addAttribute("placementResults", placementResults);

        Map<AuthorisationRequest, AppUser> requestByStudent = new HashMap<>(); // Mapping requests to the student creator
        for (AuthorisationRequest request : requestResults) {
            requestByStudent.put(request, uRepo.findByAuthorisationRequestsContaining(request));
        }
        model.addAttribute("requestByStudent", requestByStudent);

        return "app/search_results";
    }

    @RequestMapping("/record")
    public String graduateRecord(Principal principal, Model model) {
        // Showing an record of all activity on the application (placements joined, applied to, etc.) in a simpler
        // format to enable it to be printed

        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        // Find all placements a member of
        List<Placement> placements = plRepo.findAllByMembersContains(u, Sort.by("title").ascending());
        model.addAttribute("placements", placements);

        // Applications and requests can be retrieved from the user object directly

        return "users/record";
    }
}
