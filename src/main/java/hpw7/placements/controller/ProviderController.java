package hpw7.placements.controller;

import com.google.common.net.HttpHeaders;
import com.google.maps.errors.ApiException;
import com.google.maps.model.LatLng;
import hpw7.placements.domain.*;
import hpw7.placements.dto.EditProviderDTO;
import hpw7.placements.dto.ProviderDTO;
import hpw7.placements.repository.*;
import hpw7.placements.service.FileService;
import hpw7.placements.service.MapService;
import org.apache.tika.Tika;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The <code>ProviderController</code> handles requests relating to placement providers, including the creation of
 * providers, and the assigning of users (with role 'PROVIDER') to individual providers.
 */
@Controller
public class ProviderController {

    private final AppUserRepository uRepo;

    private final DurationToProviderRepository dTPRepo;

    private final FileService fileService;

    private final LogEntryRepository lRepo;

    private final MapService mapService;

    private final PlacementRepository plRepo;

    private final ProviderRepository prRepo;

    private final PlacementVisitRepository vRepo;

    public ProviderController(AppUserRepository uRepo, DurationToProviderRepository dTPRepo, FileService fileService, LogEntryRepository lRepo, MapService mapService, PlacementRepository plRepo, ProviderRepository prRepo, PlacementVisitRepository vRepo) {
        this.uRepo = uRepo;
        this.dTPRepo = dTPRepo;
        this.fileService = fileService;
        this.lRepo = lRepo;
        this.mapService = mapService;
        this.plRepo = plRepo;
        this.prRepo = prRepo;
        this.vRepo = vRepo;
    }

    @GetMapping("/providers")
    public String providerList(Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        List<Provider> providers = prRepo.findAll(Sort.by("name").ascending());
        model.addAttribute("providers", providers);

        // Find and count the number of placements for each provider, to show alongside provider in list
        Map<Provider, Integer> providerPlacementsCount = new HashMap<>();
        for (Provider provider : providers) {
            providerPlacementsCount.put(provider, plRepo.findAllByProvider(provider).size());
        }
        model.addAttribute("providerPlacementsCount", providerPlacementsCount);

        // Storing titles, locations, and id's for showing pins on map for listed providers
        List<String> providerNameList = new ArrayList<>();
        List<Double> providerLatList = new ArrayList<>();
        List<Double> providerLngList = new ArrayList<>();
        List<Integer> providerIdList = new ArrayList<>();

        for (Provider provider : providers) {
            providerNameList.add(provider.getName());
            providerLatList.add(provider.getLatitude());
            providerLngList.add(provider.getLongitude());
            providerIdList.add(provider.getId());
        }

        model.addAttribute("providerNameList", providerNameList);
        model.addAttribute("providerLatList", providerLatList);
        model.addAttribute("providerLngList", providerLngList);
        model.addAttribute("providerIdList", providerIdList);

        // Get Maps API key, add to model for use in page JavaScript
        String clientKey = mapService.gmapsAPIClientKey;
        model.addAttribute("clientKey", clientKey);

        return "providers/provider_list";
    }

    @GetMapping("/providers/{providerId}")
    public String provider(@PathVariable int providerId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<Provider> providerOpt = prRepo.findById(providerId);
        if (providerOpt.isEmpty()) {
            return "redirect:/providers?invalidId";
        }
        Provider provider = providerOpt.get();
        model.addAttribute("provider", provider);

        // All valid users that can be assigned to the provider
        List<AppUser> validUsers = uRepo.findByRole(Role.PROVIDER); // Finding all users with "members of provider" role
        model.addAttribute("validUsers", validUsers);

        // Get Maps API key, add to model for use in page JavaScript
        String clientKey = mapService.gmapsAPIClientKey;
        model.addAttribute("clientKey", clientKey);

        return "providers/provider";
    }

    @PostMapping("/providers/{providerId}/add-member")
    public String addMemberToProvider(@PathVariable int providerId, @RequestParam("username") String username,
                                      Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Provider> providerOpt = prRepo.findById(providerId);
        if (providerOpt.isEmpty()) {
            return "redirect:/providers?invalidId";
        }
        Provider provider = providerOpt.get();

        AppUser userAdd = uRepo.findByUsername(username); // The user to add to the provider
        if (userAdd == null) {
            return "redirect:/providers/" + providerId + "/add-member?invalidId"; // No user with given username
        }

        if (userAdd.getRole() != Role.PROVIDER) {
            return "redirect:/providers/" + providerId + "/add-member?invalidRole"; // User isn't a "member of provider"
        }

        // Check if the user already belongs to a provider - if so, clear that relationship before moving them across
        Provider oldProvider = prRepo.findByMembersUsername(userAdd.getUsername());
        if (oldProvider != null) {
            oldProvider.getMembers().remove(userAdd);

            prRepo.save(oldProvider);

            lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.REMOVE_MEMBER, LogEntity.PROVIDER, oldProvider.getId(),
                    "User removed user '" + userAdd.getUsername() + "' from the provider's member list"));
        }

        // Add the user to the member list of the given provider
        provider.getMembers().add(userAdd);

        prRepo.save(provider);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.ADD_MEMBER, LogEntity.PROVIDER, provider.getId(),
                "User added user '" + userAdd.getUsername() + "' to the provider's member list"));

        return "redirect:/providers/" + providerId + "?addSuccess";
    }

    @PostMapping("/providers/{providerId}/remove-member")
    public String removeMemberFromProvider(@PathVariable("providerId") int providerId, @RequestParam("username") String username,
                                           Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Provider> providerOpt = prRepo.findById(providerId);
        if (providerOpt.isEmpty()) {
            return "redirect:/providers?invalidId";
        }
        Provider provider = providerOpt.get();

        AppUser appUser = uRepo.findByUsername(username); // The user to remove from the provider

        if (appUser == null || !provider.getMembers().contains(appUser)) {
            return "redirect:/providers/" + providerId + "?invalidMember"; // User doesn't exist in the members list
        }

        // Remove the suer from the member list of the given provider
        provider.getMembers().remove(appUser);

        prRepo.save(provider);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.REMOVE_MEMBER, LogEntity.PROVIDER, provider.getId(),
                "User added user '" + appUser.getUsername() + "' to the provider's member list"));

        return "redirect:/providers/" + providerId + "?memberRemoved";
    }

    @GetMapping("/providers/{providerId}/logo")
    public ResponseEntity<Resource> providerLogo(@PathVariable("providerId") int providerId) {
        Optional<Provider> providerOpt = prRepo.findById(providerId);

        Resource defaultLogo = new ClassPathResource("/static/images/provider-logo-default.png");

        if (providerOpt.isEmpty() || providerOpt.get().getLogo() == null) {
            // Return 'default' logo
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/png"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + "provider-logo-default.png" + "\"").body(defaultLogo);
        }

        Provider provider = providerOpt.get(); // Get the requested provider

        try {
            // Attempt to load the stored logo for the provider from the filesystem, and serve in place of the default icon
            Path path;
            // Check separator, change if it is not the correct file separator for the current system
            if (!provider.getLogo().substring(0, 1).equals(fileService.FS)) {
                String altPathString = provider.getLogo().replace('\\', fileService.FS.charAt(0));
                path = Paths.get(System.getProperty("user.dir") + altPathString);
            } else {
                path = Paths.get(System.getProperty("user.dir") + provider.getLogo());
            }
            Resource file = new UrlResource(path.toUri());

            // Detect and pass file type so the browser knows how to handle the file
            Tika tika = new Tika();
            String fileType = tika.detect(path);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + file.getFilename() + "\"").body(file);
        } catch (Exception e) {
            // Image can no longer be accessed, probably deleted from local filesystem - void any record of it, return default icon
            provider.setLogo(null);

            prRepo.save(provider);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/png"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + "provider-logo-default.png" + "\"").body(defaultLogo);
        }
    }

    @PostMapping("/providers/{providerId}/setLogo")
    public String setProviderLogo(@PathVariable("providerId") int providerId, @RequestParam("imageFile") MultipartFile imageFile,
                                  Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Provider> providerOpt = prRepo.findById(providerId);
        if (providerOpt.isEmpty()) {
            return "redirect:/providers/?invalidId";
        }
        Provider provider = providerOpt.get();

        if (u.getRole() != Role.ADMINISTRATOR && !provider.getMembers().contains(u)) {
            return "redirect:/providers/" + providerId + "?noPermission";
        }

        fileService.createProviderLogosDirectory(); // Create the directory if it doesn't exist

        if (!fileService.checkFileIsImage(imageFile)) {
            return "redirect:/providers/" + providerId + "?pictureRejected"; // Reject file - not suitable
        }

        provider.setLogo(fileService.uploadProviderLogo(imageFile, provider));

        prRepo.save(provider);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PROVIDER, providerId,
                "User changed the logo of the provider"));

        return "redirect:/providers/" + providerId + "?pictureSet";
    }

    @GetMapping("/providers/{providerId}/resetLogo")
    public String resetProviderLogo(@PathVariable int providerId, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Provider> providerOpt = prRepo.findById(providerId);
        if (providerOpt.isEmpty()) {
            return "redirect:/providers/?invalidId";
        }
        Provider provider = providerOpt.get();

        if (u.getRole() != Role.ADMINISTRATOR && !provider.getMembers().contains(u)) {
            return "redirect:/providers/" + providerId + "?noPermission";
        }

        fileService.deleteProviderLogo(provider); // Delete any previously stored logo ...

        provider.setLogo(null); // ... and delete any record of it in the database

        prRepo.save(provider);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PROVIDER, providerId,
                "User reset the logo of the provider back to the default icon"));

        return "redirect:/providers/" + providerId + "?pictureReset";
    }

    /**
     * Return a single data transfer object. Used when creating a provider so form contents are not lost if the form has
     * to be returned for an error.
     * @return A ProviderDTO object.
     */
    @ModelAttribute("providerDTO")
    public ProviderDTO providerDTO() {
        return new ProviderDTO();
    }

    @GetMapping("/providers/new")
    public String newProviderForm() {
        return "providers/new_provider";
    }

    @PostMapping("/providers/new")
    public String newProvider(@ModelAttribute("providerDTO") @Valid ProviderDTO providerDTO, BindingResult bindingResult, Principal principal) throws InterruptedException, ApiException, IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        if (bindingResult.hasErrors()) {
            return "providers/new_provider"; // One or more fields are invalid according to existing constraints
        } else {
            if (prRepo.existsByName(providerDTO.getName())) {
                bindingResult.rejectValue("name", "name.exists", "A provider with the given name already exists!");
                return "providers/new_provider";
            }

            // Create new provider object with details from form object
            Provider provider = new Provider(providerDTO.getName(), providerDTO.getAddressLine1(),
                    providerDTO.getAddressCity(), providerDTO.getAddressPostcode(), providerDTO.getDescription());

            if (!providerDTO.getAddressLine2().equals("")) {
                provider.setAddressLine2(providerDTO.getAddressLine2());
            }

            // Build address from parts, and pass to mapService to geocode the address to latitude and longitude
            String address = provider.getAddressLine1() + ", ";

            if (provider.getAddressLine2() != null && !provider.getAddressLine2().equals("")) {
                address += provider.getAddressLine2();
            }

            address += provider.getAddressCity() + ", " + provider.getAddressPostcode();

            LatLng latLng = mapService.geocodeAddress(address);
            provider.setLatitude(latLng.lat);
            provider.setLongitude(latLng.lng);

            prRepo.save(provider);

            lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.PROVIDER, provider.getId(),
                    "User created new provider"));

            return "redirect:/providers?newSuccess";
        }
    }

    @GetMapping("/providers/{providerId}/edit")
    public String editProviderForm(@PathVariable int providerId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Provider> providerOpt = prRepo.findById(providerId);
        if (providerOpt.isEmpty()) {
            return "redirect:/providers?invalidId";
        }
        Provider provider = providerOpt.get();
        model.addAttribute("provider", provider);

        if (u.getRole() != Role.ADMINISTRATOR || (u.getRole() == Role.PROVIDER && !provider.getMembers().contains(u))) {
            return "redirect:/providers/" + provider.getId() + "?noEditPermission";
        }

        // Populate form fields with existing provider details
        EditProviderDTO editProviderDTO = new EditProviderDTO(provider.getName(), provider.getAddressLine1(),
                provider.getAddressLine2(), provider.getAddressCity(), provider.getAddressPostcode(),
                provider.getDescription());
        model.addAttribute("editProviderDTO", editProviderDTO);

        return "providers/edit_provider";
    }

    @PostMapping("/providers/{providerId}/edit")
    public String editProvider(@ModelAttribute("editProviderDTO") @Valid EditProviderDTO editProviderDTO, @PathVariable int providerId, BindingResult bindingResult, Principal principal) throws InterruptedException, ApiException, IOException {
        if (bindingResult.hasErrors()) {
            return "providers/edit_provider";
        } else {
            AppUser u = uRepo.findByUsername(principal.getName());

            Optional<Provider> providerOpt = prRepo.findById(providerId);
            if (providerOpt.isEmpty()) {
                return "redirect:/providers?invalidId";
            }
            Provider provider = providerOpt.get();

            if (u.getRole() != Role.ADMINISTRATOR || (u.getRole() == Role.PROVIDER && !provider.getMembers().contains(u))) {
                return "redirect:/providers/" + provider.getId() + "?noEditPermission";
            }

            // Check if form fields differ from existing stored details, and make any log any necessary changes
            if (!editProviderDTO.getName().equals("") && !editProviderDTO.getName().equals(provider.getName())) {
                if (!editProviderDTO.getName().equals(provider.getName()) && prRepo.existsByName(editProviderDTO.getName())) {
                    return "redirect:/providers/" + providerId + "/edit?nameExists";
                }

                String oldName = provider.getName();
                provider.setName(editProviderDTO.getName());

                lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PROVIDER, provider.getId(),
                        "User changed provider name from '" + oldName + "' to '" + provider.getName() + "'"));
            }

            if (!editProviderDTO.getAddressLine1().equals("") && !editProviderDTO.getAddressLine1().equals(provider.getAddressLine1())) {
                String oldAddressLine1 = provider.getAddressLine1();
                provider.setAddressLine1(editProviderDTO.getAddressLine1());

                lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PROVIDER, provider.getId(),
                        "User changed provider address line 1 from '" + oldAddressLine1 + "' to '" + provider.getAddressLine1() + "'"));
            }

            if (!editProviderDTO.getAddressLine2().equals("") && !editProviderDTO.getAddressLine2().equals(provider.getAddressLine2())) {
                String oldAddressLine2 = provider.getAddressLine2();
                provider.setAddressLine2(editProviderDTO.getAddressLine2());

                lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PROVIDER, provider.getId(),
                        "User changed provider address line 2 from '" + oldAddressLine2 + "' to '" + provider.getAddressLine2() + "'"));
            }

            if (!editProviderDTO.getAddressCity().equals("") && !editProviderDTO.getAddressCity().equals(provider.getAddressCity())) {
                String oldCity = provider.getAddressCity();
                provider.setAddressCity(editProviderDTO.getAddressCity());

                lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PROVIDER, provider.getId(),
                        "User changed provider address city from '" + oldCity + "' to '" + provider.getAddressCity() + "'"));
            }

            if (!editProviderDTO.getAddressPostcode().equals("") && !editProviderDTO.getAddressPostcode().equals(provider.getAddressPostcode())) {
                String oldPostcode = provider.getAddressPostcode();
                provider.setAddressPostcode(editProviderDTO.getAddressPostcode());

                lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PROVIDER, provider.getId(),
                        "User changed provider postcode from '" + oldPostcode + "' to '" + provider.getAddressPostcode() + "'"));
            }

            if (!editProviderDTO.getDescription().equals("") && !editProviderDTO.getDescription().equals(provider.getDescription())) {
                provider.setDescription(editProviderDTO.getDescription());

                lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PROVIDER, provider.getId(),
                        "User updated provider description"));
            }

            // Update (or set) latitude and longitude to reflect any address changes
            String address = provider.getAddressLine1() + ", ";

            if (provider.getAddressLine2() != null && !provider.getAddressLine2().equals("")) {
                address += provider.getAddressLine2();
            }

            address += provider.getAddressCity() + ", " + provider.getAddressPostcode();

            LatLng latLng = mapService.geocodeAddress(address);
            provider.setLatitude(latLng.lat);
            provider.setLongitude(latLng.lng);

            // Void any durations from the provider, since they're likely wrong after the address change
            List<DurationToProvider> dTPs = provider.getTimesToPr2s();
            provider.getTimesToPr2s().clear();
            dTPRepo.deleteAll(dTPs);

            // Void any durations stored as part of other providers (i.e. to the current provider)
            for (DurationToProvider dTP : dTPRepo.findAllByPr2(provider)) {
                Optional<Provider> prvdrOpt = prRepo.findByTimesToPr2sContains(dTP);
                if (prvdrOpt.isPresent()) {
                    Provider prvdr = prvdrOpt.get();
                    prvdr.getTimesToPr2s().remove(dTP);

                    dTPRepo.delete(dTP);
                    prRepo.save(prvdr);
                }
            }

            prRepo.save(provider);

            return "redirect:/providers/" + providerId + "?editSuccess";
        }
    }

    @PostMapping("/providers/{providerId}/delete")
    public String deleteProvider(@PathVariable int providerId, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Provider> providerOpt = prRepo.findById(providerId);
        if (providerOpt.isEmpty()) {
            return "redirect:/providers?invalidId";
        }
        Provider provider = providerOpt.get();

        // Delete any placements offered by the provider
        List<Placement> placements = plRepo.findAllByProvider(provider);
        for (Placement placement : placements) {
            // Delete any documents stored for the placement, both from database and the files from the local filesystem
            if (!placement.getDocuments().isEmpty()) {
                List<Document> documents = placement.getDocuments();
                placement.getDocuments().clear();

                for (Document document : documents) {
                    fileService.deleteDocument(document);
                }
            }

            // Delete and visits stored for the placement
            if (!placement.getVisits().isEmpty()) {
                List<PlacementVisit> visits = placement.getVisits();
                placement.getVisits().clear();
                vRepo.deleteAll(visits);
            }

            // Delete the placement
            plRepo.delete(placement);
        }

        // Delete any calculated distances from the provider
        if (!provider.getTimesToPr2s().isEmpty()) {
            List<DurationToProvider> dTPs = provider.getTimesToPr2s();
            dTPRepo.deleteAll(dTPs);
            provider.getTimesToPr2s().clear();
        }

        // Delete any durations stored as part of other providers (i.e. to the current provider)
        for (DurationToProvider dTP : dTPRepo.findAllByPr2(provider)) {
            Optional<Provider> prvdrOpt = prRepo.findByTimesToPr2sContains(dTP);
            if (prvdrOpt.isPresent()) {
                Provider prvdr = prvdrOpt.get();
                prvdr.getTimesToPr2s().remove(dTP);

                dTPRepo.delete(dTP);
                prRepo.save(prvdr);
            }
        }

        // Delete any stored logos for the provider
        fileService.deleteProviderLogo(provider);

        prRepo.delete(provider);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.DELETE, LogEntity.PROVIDER, provider.getId(),
                "User deleted provider, and all related placements"));
        
        return "redirect:/providers?deleteSuccess";
    }

}
