package hpw7.placements.controller;

import hpw7.placements.service.FileService;
import hpw7.placements.domain.*;
import hpw7.placements.dto.AuthorisationRequestDTO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * The <code>AuthorisationRequestController</code> handles requests relating to authorisation requests, such as creation
 * by students, approval of requests, and deletion of requests.
 */
@Controller
public class AuthorisationRequestController {

    private final AuthorisationRequestRepository arRepo;

    private final AppUserRepository uRepo;

    private final DocumentRepository dRepo;

    private final FileService fileService;

    private final LogEntryRepository lRepo;

    private final MailService mailService;

    private final ProviderRepository prRepo;

    private final PlacementRepository plRepo;

    public AuthorisationRequestController(AuthorisationRequestRepository arRepo, AppUserRepository uRepo, DocumentRepository dRepo, FileService fileService, LogEntryRepository lRepo, MailService mailService, ProviderRepository prRepo, PlacementRepository plRepo) {
        this.arRepo = arRepo;
        this.uRepo = uRepo;
        this.dRepo = dRepo;
        this.fileService = fileService;
        this.lRepo = lRepo;
        this.mailService = mailService;
        this.prRepo = prRepo;
        this.plRepo = plRepo;
    }

    @GetMapping("/requests")
    public String authorisationRequestList(Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        // Requests the current user needs to approve
        List<AuthorisationRequest> requestsList;

        // Requests approved by the current user, but waiting for the other user to approve (admin or tutor)
        List<AuthorisationRequest> requestsListSecondary;

        // Mapping authorisation requests to the students that created them
        Map<AuthorisationRequest, AppUser> requestByStudent = new HashMap<>();

        switch (u.getRole()) {
            case ADMINISTRATOR:
                // Return all requests in the database, categorised into three lists
                requestsList = arRepo.findAllByApprovedByAdministrator(false);
                model.addAttribute("requestsList", requestsList);

                requestsListSecondary = arRepo.findAllByApprovedByTutor(false);
                requestsListSecondary.removeAll(requestsList); // Remove any duplicate requests between lists
                model.addAttribute("requestsListSecondary", requestsListSecondary);

                // Fully approved authorisation requests
                List<AuthorisationRequest> requestsListTertiary = arRepo.findAllByApprovedByTutorAndApprovedByAdministrator(true, true);
                model.addAttribute("requestsListTertiary", requestsListTertiary);

                // Populate the request/student mapping
                for (AuthorisationRequest request : requestsList) {
                    requestByStudent.put(request, uRepo.findByAuthorisationRequestsContaining(request));
                }
                for (AuthorisationRequest request : requestsListSecondary) {
                    requestByStudent.put(request, uRepo.findByAuthorisationRequestsContaining(request));
                }
                for (AuthorisationRequest request : requestsListTertiary) {
                    requestByStudent.put(request, uRepo.findByAuthorisationRequestsContaining(request));
                }
                model.addAttribute("requestByStudent", requestByStudent);

                break;
            case TUTOR:
                // Return all requests the tutor is assigned to.
                requestsList = arRepo.findAllByTutorAndApprovedByTutor(u, false);
                model.addAttribute("requestsList", requestsList);

                requestsListSecondary = arRepo.findAllByTutorAndApprovedByAdministrator(u, false);
                requestsListSecondary.removeAll(requestsList);
                model.addAttribute("requestsListSecondary", requestsListSecondary);

                // Populate the request/student mapping
                for (AuthorisationRequest request : requestsList) {
                    requestByStudent.put(request, uRepo.findByAuthorisationRequestsContaining(request));
                }
                for (AuthorisationRequest request : requestsListSecondary) {
                    requestByStudent.put(request, uRepo.findByAuthorisationRequestsContaining(request));
                }
                model.addAttribute("requestByStudent", requestByStudent);

                break;
            default: // STUDENT
                // Return all requests the user has created
                requestsList = u.getAuthorisationRequests();
                model.addAttribute("requestsList", requestsList);

                // Populate the request/student mapping (with self)
                for (AuthorisationRequest request : requestsList) {
                    requestByStudent.put(request, u);
                }
                model.addAttribute("requestByStudent", requestByStudent);

                break;
        }

        return "requests/request_list";
    }

    @GetMapping("/requests/{authRequestId}")
    public String authorisationRequest(@PathVariable int authRequestId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<AuthorisationRequest> requestOpt = arRepo.findById(authRequestId);
        if (requestOpt.isEmpty()) {
            return "redirect:/requests?invalidId"; // No request for given id
        }
        AuthorisationRequest request = requestOpt.get();
        model.addAttribute("request", request);

        AppUser requestBy = uRepo.findByAuthorisationRequestsContaining(request);
        model.addAttribute("requestBy", requestBy);

        if (u.getRole() != Role.ADMINISTRATOR && u != requestBy && u != request.getTutor()) {
            return "redirect:/requests?noPermission"; // No permission to access this request
        }

        return "requests/request";

    }

    @PostMapping("/requests/{authRequestId}/delete")
    public String deleteAuthorisationRequest(@PathVariable int authRequestId, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<AuthorisationRequest> requestOpt = arRepo.findById(authRequestId);
        if (requestOpt.isEmpty()) {
            return "redirect:/requests?invalidId";
        }
        AuthorisationRequest request = requestOpt.get();

        if (u.getRole() != Role.ADMINISTRATOR && u != uRepo.findByAuthorisationRequestsContaining(request)) {
            return "redirect:/requests/" + authRequestId + "?noDeletePermission";
        }

        if (!request.getSupportingDocs().isEmpty()) {
            // Clear all documents associated with the authorisation request
            List<Document> documents = request.getSupportingDocs();
            request.getSupportingDocs().clear();
            for (Document document : documents) {
                fileService.deleteDocument(document);
            }
        }

        arRepo.delete(request);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.DELETE, LogEntity.AUTHORISATION_REQUEST, request.getId(),
                "User deleted authorisation request"));

        return "redirect:/requests?deleteSuccess";
    }

    @PostMapping("/requests/{authRequestId}/approve")
    public String approveAuthorisationRequest(@PathVariable int authRequestId, Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<AuthorisationRequest> requestOpt = arRepo.findById(authRequestId);
        if (requestOpt.isEmpty()) {
            return "redirect:/requests?invalidId";
        } else {
            AuthorisationRequest request = requestOpt.get();

            if (u.getRole() == Role.ADMINISTRATOR) {
                // Approve as admin
                request.setApprovedByAdministrator(true);
                arRepo.save(request);
                lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.APPROVE, LogEntity.AUTHORISATION_REQUEST, request.getId(),
                        "User approved authorisation request as an ADMINISTRATOR"));
            } else if ((u.getRole() == Role.TUTOR) && (request.getTutor() == u)) {
                // Approve as assigned tutor
                request.setApprovedByTutor(true);
                arRepo.save(request);
                lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.APPROVE, LogEntity.AUTHORISATION_REQUEST, request.getId(),
                        "User approved authorisation request as a TUTOR"));
            } else {
                return "redirect:/requests/" + authRequestId + "?noPermission";
            }

            if (request.isApprovedByTutor() && request.isApprovedByAdministrator()) {
                // Approved by both, so we can formally create the placement (and provider, if necessary) on the
                // application.
                Optional<Provider> providerOpt = prRepo.findByName(request.getProviderName());
                Provider provider;
                if (providerOpt.isEmpty()) {
                    // The provider doesn't already exist in the database - we need to create it
                    provider = new Provider(request.getProviderName(), request.getProviderAddressLine1(), request.getProviderAddressCity(),
                            request.getProviderAddressPostcode(), "Description for " + request.getProviderName());
                    if (!request.getProviderAddressLine2().equals("")) {
                        provider.setAddressLine2(request.getProviderAddressLine2());
                    }
                    lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.PROVIDER, provider.getId(),
                            "Approval of authorisation request #" + request.getId() + " by user triggered creation of provider"));
                } else {
                    provider = providerOpt.get();
                }

                // Creating the placement entity
                Placement placement = new Placement(request.getTitle(), request.getDetails(), LocalDateTime.now(),
                        request.getStartDate(), request.getEndDate(), provider);
                placement.getMembers().add(uRepo.findByAuthorisationRequestsContaining(request)); // Add Student
                placement.getMembers().add(request.getTutor()); // Add Tutor
                prRepo.save(provider);
                plRepo.save(placement);

                lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.PLACEMENT, placement.getId(),
                        "Approval of authorisation request #" + request.getId() + " by user triggered creation of placement"));
                mailService.sendRequestApprovedNotification(uRepo.findByAuthorisationRequestsContaining(request), request);
            }
            return "redirect:/requests/" + authRequestId + "?approved";
        }

    }

    @ModelAttribute("authorisationRequest")
    public AuthorisationRequestDTO authorisationRequestDTO() {
        return new AuthorisationRequestDTO();
    }

    @GetMapping("/requests/new")
    public String newAuthorisationRequestForm() {
        return "requests/new_request";
    }

    @PostMapping("/requests/new")
    public String newAuthorisationRequest(@ModelAttribute("authorisationRequest") @Valid AuthorisationRequestDTO requestDTO,
                                          @RequestParam("description") Optional<String[]> descriptionsOpt,
                                          Principal principal, BindingResult bindingResult) throws IOException {
        if (bindingResult.hasErrors()) {
            return "requests/new_request";
        } else {
            // Converting placement start and end dates into LocalDateTime objects
            LocalDate startDate = LocalDate.parse(requestDTO.getStartDate());
            LocalDate endDate = LocalDate.parse(requestDTO.getEndDate());

            // Creating the authorisation request entity with the data from the form fields
            AuthorisationRequest request = new AuthorisationRequest(requestDTO.getProviderName(), requestDTO.getProviderAddressLine1(),
                    requestDTO.getProviderAddressCity(), requestDTO.getProviderAddressPostcode(), requestDTO.getTitle(),
                    startDate, endDate, requestDTO.getDetails());
            if (!requestDTO.getProviderAddressLine2().equals("")) {
                request.setProviderAddressLine2(requestDTO.getProviderAddressLine2());
            }
            request.setTutor(uRepo.findByUsername(requestDTO.getTutorUsername()));
            AppUser u = uRepo.findByUsername(principal.getName());
            boolean blockedFile = false;
            if (!requestDTO.getFiles()[0].isEmpty()) {
                if (descriptionsOpt.isEmpty()) {
                    return "requests/new_request"; // Needs to be a description for each file
                }

                String[] descriptions = descriptionsOpt.get();

                // User has uploaded some files - create Document entity for each file, and store the files
                if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "uploads"))) {
                    fileService.createUploadsDirectory();
                }
                int i = 0;
                for (MultipartFile file : requestDTO.getFiles()) {
                    if (fileService.checkFileAcceptable(file)) {
                        Document document = fileService.uploadFile(file, u);
                        document.setDescription(descriptions[i]);
                        dRepo.save(document);
                        request.getSupportingDocs().add(document);
                    } else {
                        blockedFile = true; // File is of some blocked type (executable)
                    }
                    i++;
                }
            }
            u.getAuthorisationRequests().add(request);
            arRepo.save(request);
            uRepo.save(u);

            lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.AUTHORISATION_REQUEST, request.getId(),
                    "User created new authorisation request"));

            if (blockedFile) {
                // Request has been created successfully, but one or more uploaded files had to be blocked
                return "redirect:/requests?requestSuccessBlockedFile";
            }
            return "redirect:/requests?requestSuccess";
        }
    }

}
