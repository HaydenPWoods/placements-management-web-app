package hpw7.placements.controller;

import hpw7.placements.service.FileService;
import hpw7.placements.domain.*;
import hpw7.placements.repository.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.unbescape.csv.CsvEscape;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The <code>LogEntryController</code> handles requests relating to the activity log and entries, including retrieval of
 * the activity log for both the whole application, and specific entities, and also the export of activity logs as a
 * .csv file (other exports are handled by the {@link ExportController}.)
 */
@Controller
public class LogEntryController {

    /**
     * The number of log entries to be returned in a single page.
     */
    private final int LOG_PAGE_LENGTH = 20;

    private final LogEntryRepository lRepo;

    private final AppUserRepository uRepo;

    private final AuthorisationRequestRepository arRepo;

    private final FileService fileService;

    private final PlacementRepository plRepo;

    public LogEntryController(LogEntryRepository lRepo, AppUserRepository uRepo, AuthorisationRequestRepository arRepo, FileService fileService, PlacementRepository plRepo) {
        this.lRepo = lRepo;
        this.uRepo = uRepo;
        this.arRepo = arRepo;
        this.fileService = fileService;
        this.plRepo = plRepo;
    }

    @GetMapping("/log")
    public String activityLog(@RequestParam("page") Optional<Integer> page,
                              @RequestParam("action") Optional<String> action,
                              @RequestParam("entity") Optional<String> entity,
                              Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());

        int pageReturn = 1; // By default, return the first page
        if (page.isPresent()) {
            if (page.get() <= 0) {
                return "redirect:/log?invalidPage";
            }
            pageReturn = page.get();
        }

        Page<LogEntry> pageLogEntry;
        long logEntriesCount;

        // By default, return logs for all actions and all entities
        String actionSelect = "ALL";
        String entitySelect = "ALL";

        if (action.isPresent() && !action.get().equalsIgnoreCase("ALL") && entity.isPresent() && !entity.get().equalsIgnoreCase("ALL")) {
            // Custom filters selected for both action and entity
            try {
                actionSelect = action.get().toUpperCase();
                LogAction logAction = LogAction.valueOf(actionSelect);
                entitySelect = entity.get().toUpperCase();
                LogEntity logEntity = LogEntity.valueOf(entitySelect);

                if (u.getRole() == Role.ADMINISTRATOR) {
                    // Full log with given filters applied
                    pageLogEntry = lRepo.findAllByActionTypeAndEntityType(logAction, logEntity, PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
                    logEntriesCount = lRepo.countByActionTypeAndEntityType(logAction, logEntity);
                } else {
                    // Only the user's activities, with given filters applied
                    pageLogEntry = lRepo.findAllByAppUserAndActionTypeAndEntityType(u, logAction, logEntity, PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
                    logEntriesCount = lRepo.countByAppUserAndActionTypeAndEntityType(u, logAction, logEntity);
                }
            } catch (IllegalArgumentException e) {
                return "redirect:/log?invalidFilters"; // Couldn't parse selected action/entity type, doesn't exist
            }
        } else if (action.isPresent() && !action.get().equalsIgnoreCase("ALL")) {
            // Custom filter selected for action, entity filter left as default (all)
            try {
                actionSelect = action.get().toUpperCase();
                LogAction logAction = LogAction.valueOf(actionSelect);

                if (u.getRole() == Role.ADMINISTRATOR) {
                    // Full log with given action filter applied
                    pageLogEntry = lRepo.findAllByActionType(logAction, PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
                    logEntriesCount = lRepo.countByActionType(logAction);
                } else {
                    // Only the user's activities, with given action filter applied
                    pageLogEntry = lRepo.findAllByAppUserAndActionType(u, logAction, PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
                    logEntriesCount = lRepo.countByAppUserAndActionType(u, logAction);
                }
            } catch (IllegalArgumentException e) {
                return "redirect:/log?invalidAction"; // Couldn't parse selected action type, doesn't exist
            }
        } else if (entity.isPresent() && !entity.get().equalsIgnoreCase("ALL")) {
            // Custom filter selected for entity, action filter left as default (all)
            try {
                entitySelect = entity.get().toUpperCase();
                LogEntity logEntity = LogEntity.valueOf(entitySelect);

                if (u.getRole() == Role.ADMINISTRATOR) {
                    // Full log with given entity filter applied
                    pageLogEntry = lRepo.findAllByEntityType(logEntity, PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
                    logEntriesCount = lRepo.countByEntityType(logEntity);
                } else {
                    // Only the user's activities, with given entity filter applied
                    pageLogEntry = lRepo.findAllByAppUserAndEntityType(u, logEntity, PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
                    logEntriesCount = lRepo.countByAppUserAndEntityType(u, logEntity);
                }
            } catch (IllegalArgumentException e) {
                return "redirect:/log?invalidEntity"; // Couldn't parse selected entity type, doesn't exist
            }
        } else {
            // No custom filters - return default (all actions, all entities) based on role permissions
            if (u.getRole() == Role.ADMINISTRATOR) {
                // Full application activity log
                pageLogEntry = lRepo.findAll(PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
                logEntriesCount = lRepo.count();
            } else {
                // Only the user's activities
                pageLogEntry = lRepo.findAllByAppUser(u, PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
                logEntriesCount = lRepo.countByAppUser(u);
            }
        }

        List<LogEntry> logEntries = pageLogEntry.getContent(); // Convert page of log entries into list for easier iteration in template
        int totalPages = pageLogEntry.getTotalPages();

        model.addAttribute("user", u);
        model.addAttribute("logEntries", logEntries);
        model.addAttribute("logEntriesCount", logEntriesCount);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", pageReturn);
        model.addAttribute("actionSelect", actionSelect);
        model.addAttribute("entitySelect", entitySelect);

        return "activity/log";
    }

    /**
     * Given a FileWriter object and a list of log entries, writes all entries to the csv file.
     *
     * @param csvWriter  FileWriter object, writing to a '.csv' (text) file.
     * @param logEntries A list of LogEntry objects
     * @throws IOException Thrown when an attempt to write a log entry field to the file fails
     */
    public void writeLogEntriesToFile(FileWriter csvWriter, List<LogEntry> logEntries) throws IOException {
        csvWriter.write("TIMESTAMP, USER, ACTION, ENTITY, ENTITY ID, DESCRIPTION\n");
        for (LogEntry entry : logEntries) {
            csvWriter.write(CsvEscape.escapeCsv(entry.getTimestamp().toString()) + ',');
            csvWriter.write(CsvEscape.escapeCsv(entry.getAppUser().getUsername()) + ',');
            csvWriter.write(CsvEscape.escapeCsv(entry.getActionType().toString()) + ',');
            csvWriter.write(CsvEscape.escapeCsv(entry.getEntityType().toString()) + ',');
            csvWriter.write(Integer.toString(entry.getEntityId()) + ',');
            csvWriter.write(CsvEscape.escapeCsv(entry.getDescription()) + '\n');
        }
        csvWriter.close();
    }

    @GetMapping("/log/export")
    public ResponseEntity<Resource> activityLogExport(Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "exports"))) {
            fileService.createExportsDirectory();
        } else {
            fileService.cleanExportsDirectory(false);
        }

        String pathString = System.getProperty("user.dir") + fileService.FS + "exports" + fileService.FS + "activity_log_" + Instant.now().getEpochSecond() + ".csv";
        FileWriter csvWriter = new FileWriter(pathString);

        List<LogEntry> logEntries;
        if (u.getRole() == Role.ADMINISTRATOR) {
            // All records
            logEntries = lRepo.findAll(Sort.by("timestamp").descending());
        } else {
            // Records by the member
            logEntries = lRepo.findAllByAppUser(u, Sort.by("timestamp").descending());
        }
        writeLogEntriesToFile(csvWriter, logEntries);

        Path path = Paths.get(pathString);
        Resource logCsv = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + logCsv.getFilename() + "\"").body(logCsv);
    }

    @GetMapping("/placements/{placementId}/log")
    public String placementActivityLog(@PathVariable int placementId, @RequestParam("page") Optional<Integer> page, @RequestParam("action") Optional<String> action, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Placement> pOpt = plRepo.findById(placementId);
        if (pOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement p = pOpt.get();

        if (u.getRole() != Role.ADMINISTRATOR && !p.getMembers().contains(u) && !p.getProvider().getMembers().contains(u)) {
            return "redirect:/placements/" + p.getId() + "?noLogPermission";
        }

        int pageReturn = 1; // By default, return the first page
        if (page.isPresent()) {
            if (page.get() <= 0) {
                return "redirect:/placements/" + p.getId() + "/log?invalidPage";
            }
            pageReturn = page.get();
        }

        Page<LogEntry> pageLogEntry;
        long logEntriesCount;

        // No need for entity filter - the only entity here is PLACEMENT. Defaults to all actions if no filter specified
        String actionSelect = "ALL";

        if (action.isPresent() && !action.get().equalsIgnoreCase("ALL")) {
            // Custom filter selected for action
            try {
                actionSelect = action.get().toUpperCase();
                LogAction logAction = LogAction.valueOf(actionSelect);

                pageLogEntry = lRepo.findAllByActionTypeAndEntityTypeAndEntityId(logAction, LogEntity.PLACEMENT, p.getId(), PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
                logEntriesCount = lRepo.countByActionTypeAndEntityTypeAndEntityId(logAction, LogEntity.PLACEMENT, p.getId());
            } catch (IllegalArgumentException e) {
                return "redirect:/placements/" + placementId + "/log?invalidAction"; // Couldn't parse selected action type, doesn't exist
            }
        } else {
            // No custom filters, return logs for all actions
            pageLogEntry = lRepo.findAllByEntityTypeAndEntityId(LogEntity.PLACEMENT, p.getId(), PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
            logEntriesCount = lRepo.countByEntityTypeAndEntityId(LogEntity.PLACEMENT, p.getId());
        }

        List<LogEntry> logEntries = pageLogEntry.getContent(); // Convert page to list
        int totalPages = pageLogEntry.getTotalPages();

        model.addAttribute("user", u);
        model.addAttribute("logEntries", logEntries);
        model.addAttribute("logEntriesCount", logEntriesCount);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", pageReturn);
        model.addAttribute("actionSelect", actionSelect);

        return "activity/log";
    }

    @GetMapping("/placements/{placementId}/log/export")
    public Object placementActivityLogExport(@PathVariable int placementId, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Placement> pOpt = plRepo.findById(placementId);
        if (pOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement p = pOpt.get();

        if (u.getRole() != Role.ADMINISTRATOR && !p.getMembers().contains(u) && !p.getProvider().getMembers().contains(u)) {
            return "redirect:/placements/" + p.getId() + "?noLogPermission"; // No permission to export log
        }

        if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "exports"))) {
            fileService.createExportsDirectory();
        } else {
            fileService.cleanExportsDirectory(false);
        }

        String pathString = System.getProperty("user.dir") + fileService.FS + "exports" + fileService.FS + "activity_log_placement_" + Instant.now().getEpochSecond() + ".csv";
        FileWriter csvWriter = new FileWriter(pathString);

        List<LogEntry> logEntries = lRepo.findAllByEntityTypeAndEntityId(LogEntity.PLACEMENT, p.getId(), Sort.by("timestamp").descending());
        writeLogEntriesToFile(csvWriter, logEntries);

        Path path = Paths.get(pathString);
        Resource logCsv = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + logCsv.getFilename() + "\"").body(logCsv);
    }

    @GetMapping("/requests/{requestId}/log")
    public String requestActivityLog(@PathVariable int requestId, @RequestParam("page") Optional<Integer> page, @RequestParam("action") Optional<String> action, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<AuthorisationRequest> requestOpt = arRepo.findById(requestId);
        if (requestOpt.isEmpty()) {
            return "redirect:/requests?invalidId";
        }
        AuthorisationRequest request = requestOpt.get();

        if (request.getTutor() != u && u != uRepo.findByAuthorisationRequestsContaining(request) && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/requests/" + request.getId() + "?noLogPermission";
        }

        int pageReturn = 1; // By default, return the first page
        if (page.isPresent()) {
            if (page.get() <= 0) {
                return "redirect:/requests/" + request.getId() + "/log?invalidId";
            }
            pageReturn = page.get();
        }

        Page<LogEntry> pageLogEntry;
        long logEntriesCount;

        // No need for entity filter - the only entity here is AUTHORISATION_REQUEST. Defaults to all actions if no filter specified
        String actionSelect = "ALL";

        if (action.isPresent() && !action.get().equalsIgnoreCase("ALL")) {
            // Custom filter selected for action
            try {
                actionSelect = action.get().toUpperCase();
                LogAction logAction = LogAction.valueOf(actionSelect);
                pageLogEntry = lRepo.findAllByActionTypeAndEntityTypeAndEntityId(logAction, LogEntity.AUTHORISATION_REQUEST, request.getId(), PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
                logEntriesCount = lRepo.countByActionTypeAndEntityTypeAndEntityId(logAction, LogEntity.AUTHORISATION_REQUEST, request.getId());
            } catch (IllegalArgumentException e) {
                return "redirect:/requests/" + request.getId() + "/log?invalidAction"; // Couldn't parse action
            }
        } else {
            // No custom filters, return logs for all actions
            pageLogEntry = lRepo.findAllByEntityTypeAndEntityId(LogEntity.AUTHORISATION_REQUEST, request.getId(), PageRequest.of(pageReturn - 1, LOG_PAGE_LENGTH, Sort.by("timestamp").descending()));
            logEntriesCount = lRepo.countByEntityTypeAndEntityId(LogEntity.AUTHORISATION_REQUEST, request.getId());
        }

        List<LogEntry> logEntries = pageLogEntry.getContent(); // Convert page to list
        int totalPages = pageLogEntry.getTotalPages();

        model.addAttribute("user", u);
        model.addAttribute("logEntries", logEntries);
        model.addAttribute("logEntriesCount", logEntriesCount);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", pageReturn);
        model.addAttribute("actionSelect", actionSelect);

        return "activity/log";
    }

    @GetMapping("/requests/{requestId}/log/export")
    public Object requestActivityLogExport(@PathVariable int requestId, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<AuthorisationRequest> requestOpt = arRepo.findById(requestId);
        if (requestOpt.isEmpty()) {
            return "redirect:/requests?invalidId";
        }
        AuthorisationRequest request = requestOpt.get();

        if (request.getTutor() != u && u != uRepo.findByAuthorisationRequestsContaining(request) && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/requests/" + request.getId() + "?noLogPermission";
        }

        if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "exports"))) {
            fileService.createExportsDirectory();
        } else {
            fileService.cleanExportsDirectory(false);
        }

        String pathString = System.getProperty("user.dir") + fileService.FS + "exports" + fileService.FS + "activity_log_request_" + Instant.now().getEpochSecond() + ".csv";
        FileWriter csvWriter = new FileWriter(pathString);

        List<LogEntry> logEntries = lRepo.findAllByEntityTypeAndEntityId(LogEntity.AUTHORISATION_REQUEST, request.getId(), Sort.by("timestamp").descending());
        writeLogEntriesToFile(csvWriter, logEntries);

        Path path = Paths.get(pathString);
        Resource logCsv = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + logCsv.getFilename() + "\"").body(logCsv);
    }

    @GetMapping("/log/delete")
    public String deleteLogEntries(Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        if (u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/log?noPermission";
        }

        lRepo.deleteAll();
        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.DELETE, LogEntity.LOG_ENTRY,
                "User deleted all previous log entries"));

        return "redirect:/log?deleteSuccess";
    }
}
