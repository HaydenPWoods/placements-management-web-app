package hpw7.placements.controller;

import hpw7.placements.service.FileService;
import hpw7.placements.SecurityConfig;
import hpw7.placements.domain.AuthorisationRequest;
import hpw7.placements.domain.Placement;
import hpw7.placements.domain.Role;
import hpw7.placements.repository.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.unbescape.csv.CsvEscape;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The <code>ExportController</code> handles requests relating to application statistics, and the export of these
 * statistics, and data stored by the application, as .csv files. Exports of the activity logs are dealt with in the
 * {@link LogEntryController}. All requests in this controller are restricted to administrators, as specified in the
 * {@link SecurityConfig}.
 */
@Controller
public class ExportController {

    private final AppUserRepository uRepo;

    private final AuthorisationRequestRepository arRepo;

    private final FileService fileService;

    private final PlacementApplicationRepository plAppRepo;

    private final PlacementRepository plRepo;

    private final ProviderRepository prRepo;

    public ExportController(AppUserRepository uRepo, AuthorisationRequestRepository arRepo, FileService fileService,
                            PlacementApplicationRepository plAppRepo, PlacementRepository plRepo,
                            ProviderRepository prRepo) {
        this.uRepo = uRepo;
        this.arRepo = arRepo;
        this.fileService = fileService;
        this.plAppRepo = plAppRepo;
        this.plRepo = plRepo;
        this.prRepo = prRepo;
    }

    @GetMapping("/stats")
    public String applicationStats(Model model) {
        // Retrieving entity counts and adding to model
        model.addAttribute("placementsCount", plRepo.count());
        model.addAttribute("requestsCount", arRepo.count());
        model.addAttribute("providersCount", prRepo.count());
        model.addAttribute("applicationsCount", plAppRepo.count());
        model.addAttribute("usersCount", uRepo.count());

        // Retrieving counts of all user types and adding to model (added all together equals the total user count)
        model.addAttribute("adminCount", uRepo.countByRole(Role.ADMINISTRATOR));
        model.addAttribute("mOPCount", uRepo.countByRole(Role.PROVIDER));
        model.addAttribute("tutorCount", uRepo.countByRole(Role.TUTOR));
        model.addAttribute("studentCount", uRepo.countByRole(Role.STUDENT));
        model.addAttribute("graduateCount", uRepo.countByRole(Role.GRADUATE));

        return "admin/stats";
    }

    @GetMapping("/export/stats/placements")
    public Object placementsStatsExport() throws IOException {
        if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "exports"))) {
            fileService.createExportsDirectory();
        } else {
            fileService.cleanExportsDirectory(false);
        }

        String pathString = System.getProperty("user.dir") + fileService.FS + "exports" + fileService.FS + "stats_placements_" + Instant.now().getEpochSecond() + ".csv";
        FileWriter csvWriter = new FileWriter(pathString);

        // Writing placement stats to .csv file
        csvWriter.write("TOTAL PLACEMENTS," + plRepo.count() + "\n\n");
        csvWriter.write("PLACEMENT STATUS,COUNT\n");
        csvWriter.write("OPEN FOR APPLICATIONS," + plRepo.countAllByApplicationDeadlineAfter(LocalDateTime.now()) + '\n');
        csvWriter.write("CLOSED FOR APPLICATIONS," + plRepo.countAllByApplicationDeadlineBefore(LocalDateTime.now()) + '\n');
        csvWriter.close();

        Path path = Paths.get(pathString);
        Resource statsCsv = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + statsCsv.getFilename() + "\"").body(statsCsv);

    }

    @GetMapping("/export/stats/requests")
    public Object requestsStatsExport() throws IOException {
        if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "exports"))) {
            fileService.createExportsDirectory();
        } else {
            fileService.cleanExportsDirectory(false);
        }

        String pathString = System.getProperty("user.dir") + fileService.FS + "exports" + fileService.FS + "stats_requests_" + Instant.now().getEpochSecond() + ".csv";
        FileWriter csvWriter = new FileWriter(pathString);

        // Writing authorisation request stats to .csv file
        csvWriter.write("TOTAL AUTHORISATION REQUESTS," + arRepo.count() + "\n\n");
        csvWriter.write("REQUEST STATUS,COUNT\n");
        csvWriter.write("FULLY APPROVED," + arRepo.countByApprovedByTutorAndApprovedByAdministrator(true, true) + '\n');
        csvWriter.write(CsvEscape.escapeCsv("APPROVED BY TUTOR, AWAITING ADMIN APPROVAL") + ',' + arRepo.countByApprovedByTutorAndApprovedByAdministrator(true, false) + '\n');
        csvWriter.write(CsvEscape.escapeCsv("APPROVED BY ADMIN, AWAITING TUTOR APPROVAL") + ',' + arRepo.countByApprovedByTutorAndApprovedByAdministrator(false, true) + '\n');
        csvWriter.write("AWAITING TUTOR AND ADMIN APPROVAL," + arRepo.countByApprovedByTutorAndApprovedByAdministrator(false, false) + '\n');
        csvWriter.close();

        Path path = Paths.get(pathString);
        Resource statsCsv = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + statsCsv.getFilename() + "\"").body(statsCsv);
    }

    @GetMapping("/export/stats/users")
    public Object usersStatsExport() throws IOException {
        if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "exports"))) {
            fileService.createExportsDirectory();
        } else {
            fileService.cleanExportsDirectory(false);
        }

        String pathString = System.getProperty("user.dir") + fileService.FS + "exports" + fileService.FS + "stats_users_" + Instant.now().getEpochSecond() + ".csv";
        FileWriter csvWriter = new FileWriter(pathString);

        // Writing user stats to .csv file
        csvWriter.write("TOTAL USERS," + uRepo.count() + "\n\n");
        csvWriter.write("ROLE,COUNT\n");
        csvWriter.write("ADMINISTRATOR," + uRepo.countByRole(Role.ADMINISTRATOR) + '\n');
        csvWriter.write("MEMBER OF PROVIDER," + uRepo.countByRole(Role.PROVIDER) + '\n');
        csvWriter.write("TUTOR," + uRepo.countByRole(Role.TUTOR) + '\n');
        csvWriter.write("STUDENT," + uRepo.countByRole(Role.STUDENT) + '\n');
        csvWriter.write("GRADUATE," + uRepo.countByRole(Role.GRADUATE) + '\n');
        csvWriter.close();

        Path path = Paths.get(pathString);
        Resource statsCsv = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + statsCsv.getFilename() + "\"").body(statsCsv);
    }

    @GetMapping("/export/placements")
    public Object placementsExport() throws IOException {
        if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "exports"))) {
            fileService.createExportsDirectory();
        } else {
            fileService.cleanExportsDirectory(false);
        }

        String pathString = System.getProperty("user.dir") + fileService.FS + "exports" + fileService.FS + "placements_" + Instant.now().getEpochSecond() + ".csv";
        FileWriter csvWriter = new FileWriter(pathString);

        // Writing placement records to .csv file
        csvWriter.write("TITLE,ID,PROVIDER,APPLICATION DEADLINE,START DATE,END DATE,MEMBER COUNT,APPLICATION COUNT,VISIT COUNT,DESCRIPTION\n");
        List<Placement> allPlacements = plRepo.findAll(Sort.by("title").ascending());
        for (Placement p : allPlacements) {
            // Defining default values for dates if they happen to be null
            String applicationDeadline = "-";
            String startDate = "-";
            String endDate = "-";

            if (p.getApplicationDeadline() != null) {
                applicationDeadline = p.getApplicationDeadline().toString();
            }
            if (p.getStartDate() != null) {
                startDate = p.getStartDate().toString();
            }
            if (p.getEndDate() != null) {
                endDate = p.getEndDate().toString();
            }

            csvWriter.write(CsvEscape.escapeCsv(p.getTitle()) + ',' +
                    p.getId() + ',' +
                    CsvEscape.escapeCsv(p.getProvider().getName()) + ',' +
                    CsvEscape.escapeCsv(applicationDeadline) + ',' +
                    CsvEscape.escapeCsv(startDate) + ',' +
                    CsvEscape.escapeCsv(endDate) + ',' +
                    p.getMembers().size() + ',' +
                    plAppRepo.countAllByPlacement(p) + ',' +
                    p.getVisits().size() + ',' +
                    CsvEscape.escapeCsv(p.getDescription()) + '\n'
            );
        }
        csvWriter.close();

        Path path = Paths.get(pathString);
        Resource exportCsv = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + exportCsv.getFilename() + "\"").body(exportCsv);
    }

    @GetMapping("/export/requests")
    public Object requestsExport() throws IOException {
        if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "exports"))) {
            fileService.createExportsDirectory();
        } else {
            fileService.cleanExportsDirectory(false);
        }

        String pathString = System.getProperty("user.dir") + fileService.FS + "exports" + fileService.FS + "requests_" + Instant.now().getEpochSecond() + ".csv";
        FileWriter csvWriter = new FileWriter(pathString);

        // Writing authorisation request records to .csv file
        csvWriter.write("ID,STUDENT USERNAME,PLACEMENT TITLE,PLACEMENT START DATE,PLACEMENT END DATE,PROVIDER NAME," +
                "PROVIDER ADDRESS LINE 1,PROVIDER ADDRESS LINE 2,PROVIDER ADDRESS CITY,PROVIDER ADDRESS POSTCODE," +
                "TUTOR USERNAME,TUTOR APPROVED,ADMIN APPROVED,DETAILS\n");
        List<AuthorisationRequest> allRequests = arRepo.findAll(Sort.by("id").ascending());
        for (AuthorisationRequest request : allRequests) {
            String studentUsername = uRepo.findByAuthorisationRequestsContaining(request).getUsername();

            // Defining default values for dates and boolean values
            String startDate = "-";
            String endDate = "-";
            String tutorApproved = "False";
            String adminApproved = "False";

            if (request.getStartDate() != null) {
                startDate = request.getStartDate().toString();
            }
            if (request.getEndDate() != null) {
                endDate = request.getEndDate().toString();
            }
            if (request.isApprovedByTutor()) {
                tutorApproved = "True";
            }
            if (request.isApprovedByAdministrator()) {
                adminApproved = "True";
            }

            csvWriter.write(Integer.toString(request.getId()) + ',' +
                    CsvEscape.escapeCsv(studentUsername) + ',' +
                    CsvEscape.escapeCsv(request.getTitle()) + ',' +
                    startDate + ',' +
                    endDate + ',' +
                    CsvEscape.escapeCsv(request.getProviderName()) + ',' +
                    CsvEscape.escapeCsv(request.getProviderAddressLine1()) + ',' +
                    CsvEscape.escapeCsv(request.getProviderAddressLine2()) + ',' +
                    CsvEscape.escapeCsv(request.getProviderAddressCity()) + ',' +
                    CsvEscape.escapeCsv(request.getProviderAddressPostcode()) + ',' +
                    CsvEscape.escapeCsv(request.getTutor().getUsername()) + ',' +
                    tutorApproved + ',' +
                    adminApproved + ',' +
                    CsvEscape.escapeCsv(request.getDetails()) + '\n');
        }
        csvWriter.close();

        Path path = Paths.get(pathString);
        Resource exportCsv = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + exportCsv.getFilename() + "\"").body(exportCsv);
    }

}
