package hpw7.placements.controller;

import hpw7.placements.service.FileService;
import hpw7.placements.domain.*;
import hpw7.placements.repository.*;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The <code>DocumentController</code> handles requests relating to document objects, and the associated files, for
 * authorisation requests, placements, and placement applications. This includes uploads, document retrieval and
 * downloading, and deletion. Calls upon {@link FileService} for file-specific operations, like storing the file on
 * the local filesystem.
 */
@Controller
public class DocumentController {

    /**
     * Commonly used image extensions, supported by <img>. Excluding .svg.
     */
    private final List<String> IMAGE_EXTENSIONS = List.of("apng", "avif", "gif", "jpg", "jpeg", "jfif", "pjpeg", "pjp",
            "png", "webp");

    private final AppUserRepository uRepo;

    private final AuthorisationRequestRepository arRepo;

    private final DocumentRepository dRepo;

    private final FileService fileService;

    private final LogEntryRepository lRepo;

    private final PlacementRepository plRepo;

    private final PlacementApplicationRepository plAppRepo;

    private final ProviderRepository prRepo;

    public DocumentController(AppUserRepository uRepo, AuthorisationRequestRepository arRepo, DocumentRepository dRepo, FileService fileService, LogEntryRepository lRepo, PlacementRepository plRepo, PlacementApplicationRepository plAppRepo, ProviderRepository prRepo) {
        this.uRepo = uRepo;
        this.arRepo = arRepo;
        this.dRepo = dRepo;
        this.fileService = fileService;
        this.lRepo = lRepo;
        this.plRepo = plRepo;
        this.plAppRepo = plAppRepo;
        this.prRepo = prRepo;
    }

    @GetMapping("/documents/{fileName:.+}")
    public Object downloadDocument(@PathVariable String fileName, Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Document> documentOpt = dRepo.findByFile("\\uploads\\" + fileName);
        if (documentOpt.isEmpty()) {
            return "redirect:/dashboard?invalidDocumentId"; // File with the given name doesn't exist
        }

        boolean ok = false; // Whether the user has permission to access the file
        Document document = documentOpt.get();

        // Checking what the document is for, and checking if the user has permission to access it. Admins access all
        // areas. File owners can access their own documents, regardless of memberships.
        if (u.getRole() != Role.ADMINISTRATOR && document.getOwner() != u) {
            Optional<PlacementApplication> plAppOpt = plAppRepo.findBySupportingDocsContains(document);
            Optional<AuthorisationRequest> requestOpt = arRepo.findBySupportingDocsContains(document);
            Optional<Placement> placementOpt = plRepo.findByDocumentsContains(document);

            if (plAppOpt.isPresent()) {
                PlacementApplication plApp = plAppOpt.get();
                if (plApp.getPlacement().getProvider().getMembers().contains(u)) {
                    ok = true;
                }
            } else if (requestOpt.isPresent()) {
                AuthorisationRequest request = requestOpt.get();
                if (request.getTutor() == u) {
                    ok = true;
                } else {
                    Optional<Provider> providerOpt = prRepo.findByName(request.getProviderName());
                    if (providerOpt.isPresent() && providerOpt.get().getMembers().contains(u)) {
                        ok = true;
                    }
                }
            } else if (placementOpt.isPresent()) {
                Placement placement = placementOpt.get();
                if (placement.getMembers().contains(u) || placement.getProvider().getMembers().contains(u)) {
                    ok = true;
                }
            }

            if (!ok) { // No permission to access the requested file
                lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.DOWNLOAD, LogEntity.DOCUMENT,
                        "User unsuccessfully attempted to request file '" + document.getName() + "'"));
                return "redirect:/dashboard?noDocumentPermission";
            }
        }

        Path path = Paths.get(System.getProperty("user.dir") + fileService.FS + "uploads" + fileService.FS + fileName);

        Resource file = null;
        String fileType = "";
        try {
            file = new UrlResource(path.toUri());
            Tika tika = new Tika();
            fileType = tika.detect(path); // Parsing file type, so browser knows how to best handle the file
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert file != null;
        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.DOWNLOAD, LogEntity.DOCUMENT,
                "User successfully requested file '" + file.getFilename() + "'"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @GetMapping("/documents/id/{documentId}")
    public String documentRedirect(@PathVariable int documentId) {
        // Documents pages are accessed under their relevant entity, e.g. "/applications/1/documents/1". This will
        // attempt to find the entity that the document is associated with, and then redirect the user to the correct
        // location, without needing to know this information initially. Specifically implemented to allow linking to
        // documents from log entries, with just the document id, without needing to keep track of the associated
        // entity.

        Optional<Document> documentOpt = dRepo.findById(documentId);
        if (documentOpt.isEmpty()) {
            return "redirect:/dashboard?invalidDocumentId";
        }
        Document document = documentOpt.get();

        Optional<PlacementApplication> pAppOpt = plAppRepo.findBySupportingDocsContains(document);
        if (pAppOpt.isPresent()) {
            return "redirect:/applications/" + pAppOpt.get().getId() + "/documents/" + document.getId();
        }

        Optional<Placement> placementOpt = plRepo.findByDocumentsContains(document);
        if (placementOpt.isPresent()) {
            return "redirect:/placements/" + placementOpt.get().getId() + "/documents/" + document.getId();
        }

        Optional<AuthorisationRequest> requestOpt = arRepo.findBySupportingDocsContains(document);
        if (requestOpt.isPresent()) {
            return "redirect:/requests/" + requestOpt.get().getId() + "/documents/" + document.getId();
        }

        return "redirect:/dashboard?documentNoRedirect"; // Couldn't find the related entity
    }

    // FILE UPLOAD FORMS
    @GetMapping("/applications/{applicationId}/upload")
    public String uploadFilesForApplicationForm(@PathVariable int applicationId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<PlacementApplication> pAppOpt = plAppRepo.findById(applicationId);
        if (pAppOpt.isEmpty()) {
            return "redirect:/applications?invalidId";
        }
        PlacementApplication pApp = pAppOpt.get();

        if (!(uRepo.findByPlacementApplicationsContaining(pApp) == u) && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/applications/" + applicationId + "?noUploadPermission";
        }

        model.addAttribute("postLocation", "/applications/" + applicationId + "/upload");

        return "documents/upload_document";
    }

    @GetMapping("/placements/{placementId}/upload")
    public String uploadFilesForPlacementForm(@PathVariable int placementId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<Placement> placementOpt = plRepo.findById(placementId);
        if (placementOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement placement = placementOpt.get();

        if (!(plRepo.findAllByMembersContains(u).contains(placement)) && !placement.getProvider().getMembers().contains(u)
                && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/placements/" + placementId + "?noUploadPermission";
        }

        model.addAttribute("postLocation", "/placements/" + placementId + "/upload");

        return "documents/upload_document";
    }

    @GetMapping("/requests/{authRequestId}/upload")
    public String uploadFilesForRequestForm(@PathVariable int authRequestId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<AuthorisationRequest> requestOpt = arRepo.findById(authRequestId);
        if (requestOpt.isEmpty()) {
            return "redirect:/requests?invalidId";
        }
        AuthorisationRequest request = requestOpt.get();

        if (!(uRepo.findByAuthorisationRequestsContaining(request) == u) && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/requests/" + authRequestId + "?noUploadPermission";
        }

        model.addAttribute("postLocation", "/requests/" + authRequestId + "/upload");

        return "documents/upload_document";
    }

    // FILE UPLOAD
    @PostMapping("/applications/{applicationId}/upload")
    public String uploadFilesForApplication(@PathVariable int applicationId,
                                            @RequestParam("files") MultipartFile[] files,
                                            @RequestParam("description") String[] descriptions,
                                            Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<PlacementApplication> pAppOpt = plAppRepo.findById(applicationId);
        if (pAppOpt.isEmpty()) {
            return "redirect:/applications?invalidId";
        }
        PlacementApplication pApp = pAppOpt.get();

        if (!(uRepo.findByPlacementApplicationsContaining(pApp) == u) && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/applications/" + applicationId + "?noUploadPermission";
        }

        boolean blockedFile = false; // Keep a record if a file has to be blocked (is of a disallowed file type)

        if (!files[0].isEmpty()) {
            if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "uploads"))) {
                fileService.createUploadsDirectory();
            }

            int i = 0; // Iterator to access descriptions for each submitted file

            for (MultipartFile file : files) {
                if (fileService.checkFileAcceptable(file)) {
                    Document document = fileService.uploadFile(file, u);
                    document.setDescription(descriptions[i]);
                    dRepo.save(document);
                    pApp.getSupportingDocs().add(document);
                    lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.DOCUMENT, document.getId(),
                            "User uploaded new document related to placement application #" + pApp.getId()));
                } else {
                    blockedFile = true;
                    lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.DOCUMENT,
                            "User attempted to upload a document of a blocked file type for placement application #" + pApp.getId()));
                }
                i++;
            }
        }

        plAppRepo.save(pApp);

        if (blockedFile) {
            return "redirect:/applications/" + applicationId + "?uploadSuccessBlockedFile";
        } else {
            return "redirect:/applications/" + applicationId + "?uploadSuccess";
        }
    }

    @PostMapping("/placements/{placementId}/upload")
    public String uploadFilesForPlacement(@PathVariable int placementId,
                                          @RequestParam("files") MultipartFile[] files,
                                          @RequestParam("description") String[] descriptions,
                                          Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Placement> placementOpt = plRepo.findById(placementId);
        if (placementOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement placement = placementOpt.get();

        if (!(plRepo.findAllByMembersContains(u).contains(placement)) && !placement.getProvider().getMembers().contains(u)
                && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/placements/" + placementId + "?noUploadPermission";
        }

        boolean blockedFile = false; // Keep a record if a file has to be blocked (is of a disallowed file type)

        if (!files[0].isEmpty()) {
            if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "uploads"))) {
                fileService.createUploadsDirectory();
            }

            int i = 0; // Iterator to access descriptions for each submitted file

            for (MultipartFile file : files) {
                if (fileService.checkFileAcceptable(file)) {
                    Document document = fileService.uploadFile(file, u);
                    document.setDescription(descriptions[i]);
                    dRepo.save(document);
                    placement.getDocuments().add(document);
                    lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.DOCUMENT, document.getId(),
                            "User uploaded new document related to placement #" + placement.getId()));
                } else {
                    blockedFile = true;
                    lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.DOCUMENT,
                            "User attempted to upload a document of a blocked file type for placement #" + placement.getId()));
                }
                i++;
            }
        }

        plRepo.save(placement);

        if (blockedFile) {
            return "redirect:/placements/" + placementId + "?uploadSuccessBlockedFile";
        } else {
            return "redirect:/placements/" + placementId + "?uploadSuccess";
        }
    }

    @PostMapping("/requests/{authRequestId}/upload")
    public String uploadFilesForRequest(@PathVariable int authRequestId,
                                        @RequestParam("files") MultipartFile[] files,
                                        @RequestParam("description") String[] descriptions,
                                        Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<AuthorisationRequest> requestOpt = arRepo.findById(authRequestId);
        if (requestOpt.isEmpty()) {
            return "redirect:/requests?invalidId";
        }
        AuthorisationRequest request = requestOpt.get();

        if (!(uRepo.findByAuthorisationRequestsContaining(request) == u) && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/requests/" + authRequestId + "?noUploadPermission";
        }

        boolean blockedFile = false; // Keep a record if a file has to be blocked (is of a disallowed file type)

        if (!files[0].isEmpty()) {
            if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "uploads"))) {
                fileService.createUploadsDirectory();
            }

            int i = 0; // Iterator to access descriptions for each submitted file

            for (MultipartFile file : files) {
                if (fileService.checkFileAcceptable(file)) {
                    Document document = fileService.uploadFile(file, u);
                    document.setDescription(descriptions[i]);
                    dRepo.save(document);
                    request.getSupportingDocs().add(document);
                    lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.DOCUMENT, document.getId(),
                            "User uploaded new document related to authorisation request #" + request.getId()));
                } else {
                    blockedFile = true;
                    lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.DOCUMENT,
                            "User attempted to upload a document of a blocked file type for authorisation request #" + request.getId()));
                }
                i++;
            }
        }

        arRepo.save(request);

        if (blockedFile) {
            return "redirect:/requests/" + authRequestId + "?uploadSuccessBlockedFile";
        } else {
            return "redirect:/requests/" + authRequestId + "?uploadSuccess";
        }
    }

    // RETRIEVE SINGLE DOCUMENT FOR ENTITY

    /**
     * Checking if a 'de-sync' has occurred - a situation where the document remains, but the file associated with it
     * has been deleted from the local filesystem, or doesn't exist for some reason - outside the control of the
     * application.
     *
     * @param document The Document object to check for a de-sync.
     * @return <code>true</code> if the associated file doesn't exist, <code>false</code> otherwise.
     */
    public boolean documentDeSync(Document document) {
        // Check separator, change if it is not the correct file separator for the current system
        if (!document.getFile().substring(0, 1).equals(fileService.FS)) {
            String altFilePath = document.getFile().replace('\\', fileService.FS.charAt(0));
            return Files.notExists(Path.of(System.getProperty("user.dir") + altFilePath));
        } else {
            return Files.notExists(Path.of(System.getProperty("user.dir") + document.getFile()));
        }
    }

    @GetMapping("/applications/{applicationId}/documents/{documentId}")
    public String applicationSingleDocument(@PathVariable int applicationId,
                                            @PathVariable int documentId,
                                            Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<PlacementApplication> pAppOpt = plAppRepo.findById(applicationId);
        if (pAppOpt.isEmpty()) {
            return "redirect:/applications?invalidId";
        }
        PlacementApplication pApp = pAppOpt.get();
        model.addAttribute("pApp", pApp);

        if (u.getRole() != Role.ADMINISTRATOR
                && u != uRepo.findByPlacementApplicationsContaining(pApp)
                && !pApp.getPlacement().getProvider().getMembers().contains(u)) {
            return "redirect:/applications?noPermission"; // No permission to view the application + related documents
        }

        Optional<Document> documentOpt = dRepo.findById(documentId);
        if (documentOpt.isEmpty() || !pApp.getSupportingDocs().contains(documentOpt.get())) {
            return "redirect:/applications/" + applicationId + "?invalidId";
        }
        Document document = documentOpt.get();
        model.addAttribute("document", document);

        if (documentDeSync(document)) {
            // Invalid document, remove it and tell the user
            pApp.getSupportingDocs().remove(document);
            dRepo.delete(document);
            return "redirect:/applications/" + applicationId + "?documentDeSync";
        }

        String[] documentSplit = document.getFile().split("\\."); // Split file name on '.' to get extension
        String fileExtension = documentSplit[documentSplit.length - 1];
        model.addAttribute("fileExtension", fileExtension);

        model.addAttribute("IMAGE_EXTENSIONS", IMAGE_EXTENSIONS);
        model.addAttribute("forApplication", true);
        model.addAttribute("forRequest", false);
        model.addAttribute("forPlacement", false);

        return "documents/document";
    }

    @GetMapping("/placements/{placementId}/documents/{documentId}")
    public String placementSingleDocument(@PathVariable int placementId,
                                          @PathVariable int documentId,
                                          Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<Placement> placementOpt = plRepo.findById(placementId);
        if (placementOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement placement = placementOpt.get();
        model.addAttribute("placement", placement);

        if (u.getRole() != Role.ADMINISTRATOR
                && !placement.getMembers().contains(u)
                && !placement.getProvider().getMembers().contains(u)) {
            return "redirect:/placements?noPermission"; // No permission to view the placement's documents
        }

        Optional<Document> documentOpt = dRepo.findById(documentId);
        if (documentOpt.isEmpty() || !placement.getDocuments().contains(documentOpt.get())) {
            return "redirect:/placements/" + placementId + "?invalidId";
        }
        Document document = documentOpt.get();

        if (documentDeSync(document)) {
            // Invalid document, remove it and tell the user
            placement.getDocuments().remove(document);
            dRepo.delete(document);
            return "redirect:/placements/" + placementId + "?documentDeSync";
        }

        String[] documentSplit = document.getFile().split("\\."); // Split file name on '.' to get extension
        String fileExtension = documentSplit[documentSplit.length - 1];
        model.addAttribute("fileExtension", fileExtension);

        model.addAttribute("document", document);
        model.addAttribute("IMAGE_EXTENSIONS", IMAGE_EXTENSIONS);
        model.addAttribute("forApplication", false);
        model.addAttribute("forRequest", false);
        model.addAttribute("forPlacement", true);

        return "documents/document";
    }

    @GetMapping("/requests/{authRequestId}/documents/{documentId}")
    public String requestSingleDocument(@PathVariable int authRequestId,
                                        @PathVariable int documentId,
                                        Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<AuthorisationRequest> requestOpt = arRepo.findById(authRequestId);
        if (requestOpt.isEmpty()) {
            return "redirect:/requests?invalidId";
        }
        AuthorisationRequest request = requestOpt.get();
        model.addAttribute("request", request);

        if (u.getRole() != Role.ADMINISTRATOR
                && u != uRepo.findByAuthorisationRequestsContaining(request)
                && u != request.getTutor()) {
            return "redirect:/requests?noPermission"; // No permission to view the request + related documents
        }

        Optional<Document> documentOpt = dRepo.findById(documentId);
        if (documentOpt.isEmpty() || !request.getSupportingDocs().contains(documentOpt.get())) {
            return "redirect:/requests/" + authRequestId + "?invalidId";
        }
        Document document = documentOpt.get();

        if (documentDeSync(document)) {
            // Invalid document, remove it and tell the user
            request.getSupportingDocs().remove(document);
            dRepo.delete(document);
            return "redirect:/requests/" + authRequestId + "?documentDeSync";
        }

        String[] documentSplit = document.getFile().split("\\."); // Split file name on '.' to get extension
        String fileExtension = documentSplit[documentSplit.length - 1];
        model.addAttribute("fileExtension", fileExtension);

        model.addAttribute("document", document);
        model.addAttribute("IMAGE_EXTENSIONS", IMAGE_EXTENSIONS);
        model.addAttribute("forApplication", false);
        model.addAttribute("forRequest", true);
        model.addAttribute("forPlacement", false);

        return "documents/document";
    }

    // DELETE DOCUMENT FOR ENTITY
    @PostMapping("/applications/{applicationId}/documents/{documentId}/delete")
    public String applicationDeleteDocument(@PathVariable int applicationId,
                                            @PathVariable int documentId,
                                            Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<PlacementApplication> pAppOpt = plAppRepo.findById(applicationId);
        if (pAppOpt.isEmpty()) {
            return "redirect:/applications?invalidId";
        }
        PlacementApplication pApp = pAppOpt.get();

        Optional<Document> documentOpt = dRepo.findById(documentId);
        if (documentOpt.isEmpty() || !pApp.getSupportingDocs().contains(documentOpt.get())) {
            return "redirect:/applications/" + applicationId + "?invalidId";
        }
        Document document = documentOpt.get();

        if (document.getOwner() != u && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/applications/" + applicationId + "/documents/" + documentId + "?noDeletePermission";
        }

        pApp.getSupportingDocs().remove(document);

        plAppRepo.save(pApp);

        fileService.deleteDocument(document);

        return "redirect:/applications/" + applicationId + "?deleteSuccess";
    }

    @PostMapping("/placements/{placementId}/documents/{documentId}/delete")
    public String placementDeleteDocument(@PathVariable int placementId,
                                          @PathVariable int documentId,
                                          Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Placement> placementOpt = plRepo.findById(placementId);
        if (placementOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement placement = placementOpt.get();

        Optional<Document> documentOpt = dRepo.findById(documentId);
        if (documentOpt.isEmpty() || !placement.getDocuments().contains(documentOpt.get())) {
            return "redirect:/placements/" + placementId + "?invalidId";
        }
        Document document = documentOpt.get();

        if (document.getOwner() != u && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/placements/" + placementId + "/documents/" + documentId + "?noDeletePermission";
        }

        placement.getDocuments().remove(document);

        plRepo.save(placement);

        fileService.deleteDocument(document);

        return "redirect:/placements/" + placementId + "?deleteSuccess";
    }

    @PostMapping("/requests/{authRequestId}/documents/{documentId}/delete")
    public String requestDeleteDocument(@PathVariable int authRequestId,
                                        @PathVariable int documentId,
                                        Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<AuthorisationRequest> requestOpt = arRepo.findById(authRequestId);
        if (requestOpt.isEmpty()) {
            return "redirect:/requests?invalidId";
        }
        AuthorisationRequest request = requestOpt.get();

        Optional<Document> documentOpt = dRepo.findById(documentId);
        if (documentOpt.isEmpty() || !request.getSupportingDocs().contains(documentOpt.get())) {
            return "redirect:/requests/" + authRequestId + "?invalidId";
        }
        Document document = documentOpt.get();

        if (document.getOwner() != u && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/requests/" + authRequestId + "/documents/" + documentId + "?noDeletePermission";
        }

        request.getSupportingDocs().remove(document);

        arRepo.save(request);

        fileService.deleteDocument(document);

        return "redirect:/requests/" + authRequestId + "?deleteSuccess";
    }

}
