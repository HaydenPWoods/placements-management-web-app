package hpw7.placements.controller;

import com.google.maps.errors.ApiException;
import hpw7.placements.service.FileService;
import hpw7.placements.service.MailService;
import hpw7.placements.service.MapService;
import hpw7.placements.domain.*;
import hpw7.placements.repository.*;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.RandomUidGenerator;
import net.fortuna.ical4j.util.UidGenerator;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.Duration;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * The <code>PlacementVisitController</code> handles requests relating to the scheduling of visits by tutors for
 * students and other members of placements, either manually, or by suggestion from the application. Additionally, the
 * controller handles serving calendar (.ics) files, allowing scheduled visits to be exported and added to an external
 * calendar, such as an Outlook calendar.
 */
@Controller
public class PlacementVisitController {

    private final AppUserRepository uRepo;

    private final FileService fileService;

    private final LogEntryRepository lRepo;

    private final MailService mailService;

    private final MapService mapService;

    private final PlacementRepository plRepo;

    private final PlacementVisitRepository vRepo;

    private final ProviderRepository prRepo;

    public PlacementVisitController(AppUserRepository uRepo, FileService fileService, LogEntryRepository lRepo, MailService mailService, MapService mapService, PlacementRepository plRepo, PlacementVisitRepository vRepo, ProviderRepository prRepo) {
        this.uRepo = uRepo;
        this.fileService = fileService;
        this.lRepo = lRepo;
        this.mailService = mailService;
        this.mapService = mapService;
        this.plRepo = plRepo;
        this.vRepo = vRepo;
        this.prRepo = prRepo;
    }

    @GetMapping("/visits")
    public String visitList(Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        // Find all visits organised by the user (as a tutor)
        List<PlacementVisit> organisedVisits = vRepo.findAllByOrganiser(u);
        model.addAttribute("organisedVisits", organisedVisits);

        // Find all placements the user is a member of, then find any scheduled visits for these placements
        List<Placement> placements = plRepo.findAllByMembersContains(u);
        List<PlacementVisit> placementVisits = new ArrayList<>();
        for (Placement placement : placements) {
            placementVisits.addAll(placement.getVisits());
        }
        model.addAttribute("placementVisits", placementVisits);

        // If the user is a member of a provider, find all visits for placements listed for that provider
        Provider provider = prRepo.findByMembersUsername(u.getUsername());
        List<PlacementVisit> providerPlacementVisits = new ArrayList<>();
        if (provider != null) {
            List<Placement> providerPlacements = plRepo.findAllByProvider(provider);
            for (Placement placement : providerPlacements) {
                providerPlacementVisits.addAll(placement.getVisits());
            }
        }
        model.addAttribute("providerPlacementVisits", providerPlacementVisits);

        // Linking visits back to the placements they are for
        Map<PlacementVisit, Placement> visitPlacementMap = new HashMap<>();
        for (PlacementVisit visit : organisedVisits) {
            visitPlacementMap.put(visit, plRepo.findByVisitsContains(visit));
        }
        for (PlacementVisit visit : placementVisits) {
            visitPlacementMap.put(visit, plRepo.findByVisitsContains(visit));
        }
        for (PlacementVisit visit : providerPlacementVisits) {
            visitPlacementMap.put(visit, plRepo.findByVisitsContains(visit));
        }
        model.addAttribute("visitPlacementMap", visitPlacementMap);

        return "placements/visits/visit_list";
    }

    @GetMapping("/visits/{visitId}")
    public String singleVisitRedirect(@PathVariable int visitId) {
        Optional<PlacementVisit> visitOpt = vRepo.findById(visitId);
        if (visitOpt.isEmpty()) {
            return "redirect:/visits?invalidId";
        }
        PlacementVisit visit = visitOpt.get();

        Placement placement = plRepo.findByVisitsContains(visit);

        return "redirect:/placements/" + placement.getId() + "/visits/" + visit.getId(); // Redirecting to actual location
    }

    @GetMapping("/visits/suggested")
    public String suggestedVisits(Principal principal, Model model) throws InterruptedException, ApiException, IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        if (u.getRole() != Role.TUTOR) {
            return "redirect:/visits?noPermission";
        }

        // Get current placements (haven't surpassed the end date) where no visits are scheduled
        List<Placement> placements = plRepo.findAllByMembersContainsAndEndDateAfterAndVisitsIsNull(u, LocalDate.now());

        // Working out driving time between placements, so close placement pairs can be grouped together for a visit
        MultiKeyMap<Placement, Duration> placementToPlacement = new MultiKeyMap<>(); // Driving time from p1 to p2
        for (Placement p1 : placements) {
            for (Placement p2 : placements) {
                if (p1 != p2) {
                    placementToPlacement.put(p1, p2, mapService.drivingTimeBetween(p1.getProvider(), p2.getProvider()));
                }
            }
        }

        List<Placement> visited = new ArrayList<>(); // Keeping track of placements we've paired up

        Map<Placement, Placement> placementLinks = new HashMap<>(); // Linking p1 to p2 (can be visited on the same day)
        List<Placement> placementLinksKeys = new ArrayList<>(); // p1's that have a p2 in placementLinks

        for (Placement p1 : placements) {
            for (Placement p2 : placements) {
                if (p1 != p2 && !visited.contains(p1) && !visited.contains(p2)) {
                    Duration duration = placementToPlacement.get(p1, p2); // Driving time

                    // If within 2 hours of each other, link together
                    if (duration.getSeconds() <= 60 * 120 &&
                            (p1.getStartDate().isBefore(p2.getEndDate()) || p1.getStartDate().isEqual(p2.getEndDate())) &&
                            (p1.getEndDate().isAfter(p2.getStartDate()) || p1.getEndDate().isEqual(p2.getStartDate()))) {
                        placementLinks.put(p1, p2);
                        placementLinksKeys.add(p1);

                        visited.add(p1);
                        visited.add(p2);

                        break; // We have a link from p1 to p2
                    }
                }
            }
        }

        // Attempt to suggest pairs of visits on the same day for two placements, where possible
        Map<Placement, LocalDateTime> possibleVisits = new HashMap<>();
        List<Placement> placementsWithSuggestions = new ArrayList<>();
        Set<LocalDate> suggestedDates = new HashSet<>();

        for (Placement p1 : placementLinksKeys) {
            Placement p2 = placementLinks.get(p1);

            // Acceptable driving duration between p1 and p2, and dates overlap. Get intersecting date range
            LocalDate rangeStartDate;
            LocalDate rangeEndDate;

            if (LocalDate.now().isAfter(p1.getStartDate()) && LocalDate.now().isAfter(p2.getStartDate())) {
                rangeStartDate = LocalDate.now();
            } else if (p1.getStartDate().isAfter(p2.getStartDate())) {
                rangeStartDate = p1.getStartDate();
            } else {
                rangeStartDate = p2.getStartDate();
            }

            if (p1.getEndDate().isBefore(p2.getEndDate())) {
                rangeEndDate = p1.getEndDate();
            } else {
                rangeEndDate = p2.getEndDate();
            }

            // Getting valid date range where we can schedule visits on the same day for both placements
            long daysFromStartToEnd = rangeStartDate.until(rangeEndDate, ChronoUnit.DAYS);

            LocalDate possibleDate;

            // Choose a strategy, based on how many days are in the valid range
            if (daysFromStartToEnd >= 3) {
                // Try head for the middle date, if it's a weekend day, there's still space to move the date forward
                int dayFromStart = (int) daysFromStartToEnd / 2;
                possibleDate = rangeStartDate.plus(dayFromStart, ChronoUnit.DAYS);

                if (possibleDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
                    possibleDate = possibleDate.plus(2, ChronoUnit.DAYS);
                } else if (possibleDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    possibleDate = possibleDate.plus(1, ChronoUnit.DAYS);
                }

                if (possibleDate.isAfter(rangeEndDate)) { // Gone outside valid range, fallback to first valid date
                    if (rangeStartDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
                        possibleDate = rangeStartDate.plus(2, ChronoUnit.DAYS);
                    } else if (rangeStartDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        possibleDate = rangeStartDate.plus(1, ChronoUnit.DAYS);
                    } else {
                        possibleDate = rangeStartDate;
                    }
                }
            } else {
                // Too few valid dates, go for the first one - trying to avoid weekend, but not always possible
                possibleDate = rangeStartDate;

                if (possibleDate.getDayOfWeek() == DayOfWeek.SUNDAY && daysFromStartToEnd >= 2) {
                    possibleDate = possibleDate.plus(1, ChronoUnit.DAYS); // Possible to shift over to the Monday
                }
            }

            // Avoid dates with visits already scheduled by the organiser, or previously suggested
            while (vRepo.existsByOrganiserAndVisitDateTimeBetween(u, LocalDateTime.of(possibleDate, LocalTime.MIN),
                    LocalDateTime.of(possibleDate, LocalTime.MAX)) || suggestedDates.contains(possibleDate)) {
                if (possibleDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
                    possibleDate = possibleDate.plus(2, ChronoUnit.DAYS);
                } else {
                    possibleDate = possibleDate.plus(1, ChronoUnit.DAYS);
                }

                if (possibleDate.isAfter(rangeEndDate)) { // Gone outside valid range, fallback to first valid date
                    if (rangeStartDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
                        possibleDate = rangeStartDate.plus(2, ChronoUnit.DAYS);
                    } else if (rangeStartDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        possibleDate = rangeStartDate.plus(1, ChronoUnit.DAYS);
                    } else {
                        possibleDate = rangeStartDate;
                    }
                    break;
                }
            }

            // A possible date has been determined - create possible visits for the two placements
            LocalDateTime p1PossibleVisit = LocalDateTime.of(possibleDate, LocalTime.of(11, 0));
            possibleVisits.put(p1, p1PossibleVisit);

            LocalDateTime p2PossibleVisit = LocalDateTime.of(possibleDate, LocalTime.of(14, 0));
            possibleVisits.put(p2, p2PossibleVisit);

            placementsWithSuggestions.add(p1);
            placementsWithSuggestions.add(p2);

            // Keep a log that we have suggested this date, so we do not suggest any others for the same date
            suggestedDates.add(possibleDate);
        }

        // Suggesting visits for placements that couldn't be paired up
        List<Placement> individualPlacements = new ArrayList<>();

        for (Placement p : placements) {
            if (!placementsWithSuggestions.contains(p)) {
                // Getting valid date range for the placement
                long daysFromStartToEnd = p.getStartDate().until(p.getEndDate(), ChronoUnit.DAYS);

                LocalDate possibleDate;

                if (daysFromStartToEnd >= 3) {
                    // Try head for the middle, if it's a weekend day we have room to move the date
                    int daysFromStart = (int) daysFromStartToEnd / 2;
                    possibleDate = p.getStartDate().plus(daysFromStart, ChronoUnit.DAYS);

                    if (possibleDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
                        possibleDate = possibleDate.plus(2, ChronoUnit.DAYS);
                    } else if (possibleDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        possibleDate = possibleDate.plus(1, ChronoUnit.DAYS);
                    }
                } else {
                    // Too few valid dates, go for the first one - trying to avoid weekend, but not always possible
                    possibleDate = p.getStartDate();

                    if (possibleDate.getDayOfWeek() == DayOfWeek.SUNDAY && daysFromStartToEnd >= 2) {
                        possibleDate = possibleDate.plus(1, ChronoUnit.DAYS); // Possible to shift over to the Monday
                    }
                }

                // Avoid dates with visits already scheduled by the organiser, or previously suggested
                while (vRepo.existsByOrganiserAndVisitDateTimeBetween(u, LocalDateTime.of(possibleDate, LocalTime.MIN),
                        LocalDateTime.of(possibleDate, LocalTime.MAX)) || suggestedDates.contains(possibleDate)) {
                    if (possibleDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
                        possibleDate = possibleDate.plus(2, ChronoUnit.DAYS);
                    } else {
                        possibleDate = possibleDate.plus(1, ChronoUnit.DAYS);
                    }

                    if (possibleDate.isAfter(p.getEndDate())) { // Gone outside valid range, fallback to first valid date
                        if (p.getStartDate().getDayOfWeek() == DayOfWeek.SATURDAY) {
                            possibleDate = p.getStartDate().plus(2, ChronoUnit.DAYS);
                        } else if (p.getStartDate().getDayOfWeek() == DayOfWeek.SUNDAY) {
                            possibleDate = p.getStartDate().plus(1, ChronoUnit.DAYS);
                        } else {
                            possibleDate = p.getStartDate();
                        }
                        break;
                    }
                }

                // A possible date has been determined - create possible visit at 11:00
                LocalDateTime possibleVisit = LocalDateTime.of(possibleDate, LocalTime.of(11, 0));
                possibleVisits.put(p, possibleVisit);

                individualPlacements.add(p);
                placementsWithSuggestions.add(p);
                suggestedDates.add(possibleDate);
            }
        }

        model.addAttribute("placementLinks", placementLinks);
        model.addAttribute("placementLinksKeys", placementLinksKeys);
        model.addAttribute("placementToPlacement", placementToPlacement);
        model.addAttribute("possibleVisits", possibleVisits);
        model.addAttribute("placementsWithSuggestions", placementsWithSuggestions);
        model.addAttribute("individualPlacements", individualPlacements);

        return "placements/visits/suggested_list";
    }

    @PostMapping("/visits/suggested/acceptOne")
    public String acceptSuggestedVisit(@RequestParam("placementId") int placementId,
                                       @RequestParam("visitDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDate,
                                       @RequestParam("visitTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime visitTime,
                                       Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        if (u.getRole() != Role.TUTOR) {
            return "redirect:/visits?noPermission";
        }

        Optional<Placement> placementOpt = plRepo.findById(placementId);
        if (placementOpt.isEmpty()) {
            return "redirect:/visits/suggested?invalidId";
        }
        Placement placement = placementOpt.get();

        if (!placement.getMembers().contains(u)) {
            return "redirect:/visits/suggested?noPermission";
        }

        // Create and store visit with the suggested date and time
        LocalDateTime visitDateTime = LocalDateTime.of(visitDate, visitTime);
        PlacementVisit newVisit = new PlacementVisit(visitDateTime, u);
        placement.getVisits().add(newVisit);

        vRepo.save(newVisit);

        plRepo.save(placement);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.PLACEMENT_VISIT, newVisit.getId(),
                "User accepted suggested visit for placement #" + placement.getId()));

        // Attempt to send newly scheduled visit email notification to all relevant users
        mailService.sendVisitScheduledNotification(newVisit, placement);

        return "redirect:/visits?suggestedAcceptedOne";
    }

    @PostMapping("/visits/suggested/acceptMany")
    public String acceptManySuggestedVisits(@RequestParam("placementId") int[] placementIds,
                                            @RequestParam("visitDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate[] visitDates,
                                            @RequestParam("visitTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime[] visitTimes,
                                            Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        if (u.getRole() != Role.TUTOR) {
            return "redirect:/visits?noPermission";
        }

        List<PlacementVisit> newVisitList = new ArrayList<>();

        for (int i = 0; i < placementIds.length; i++) {
            Optional<Placement> placementOpt = plRepo.findById(placementIds[i]);
            if (placementOpt.isEmpty()) {
                return "redirect:/visits/suggested?invalidId";
            }
            Placement placement = placementOpt.get();

            if (!placement.getMembers().contains(u)) {
                return "redirect:/visits/suggested?noPermission";
            }

            // Create and store visit with the suggested date and time
            LocalDateTime visitDateTime = LocalDateTime.of(visitDates[i], visitTimes[i]);
            PlacementVisit newVisit = new PlacementVisit(visitDateTime, u);
            placement.getVisits().add(newVisit);

            newVisitList.add(newVisit);

            vRepo.save(newVisit);

            plRepo.save(placement);

            lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.PLACEMENT_VISIT, newVisit.getId(),
                    "User accepted suggested visit for placement #" + placement.getId()));
        }

        // Attempt to send newly scheduled visits email notifications to all relevant users
        mailService.sendManyVisitScheduledNotifications(newVisitList);

        return "redirect:/visits?suggestedAcceptedMany";
    }

    @PostMapping("/visits/{visitId}/delete")
    public String deleteVisit(@PathVariable int visitId, Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<PlacementVisit> visitOpt = vRepo.findById(visitId);
        if (visitOpt.isEmpty()) {
            return "redirect:/visits?invalidId";
        }
        PlacementVisit visit = visitOpt.get();

        if (u.getRole() != Role.ADMINISTRATOR && u != visit.getOrganiser()) {
            return "redirect:/visits/?noDeletePermission";
        }

        Placement placement = plRepo.findByVisitsContains(visit);
        placement.getVisits().remove(visit);

        plRepo.save(placement);

        vRepo.delete(visit);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.DELETE, LogEntity.PLACEMENT_VISIT, visit.getId(),
                "User deleted visit for placement #" + placement.getId()));

        return "redirect:/visits?deleteSuccess";
    }

    @GetMapping("/placements/{placementId}/visits")
    public String placementVisitList(@PathVariable int placementId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<Placement> placementOpt = plRepo.findById(placementId);
        if (placementOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement placement = placementOpt.get();
        model.addAttribute("placement", placement);

        if (!placement.getMembers().contains(u) && !placement.getProvider().getMembers().contains(u) && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/placements/" + placementId + "?noVisitPermission";
        }

        return "placements/visits/placement_visit_list";
    }

    @GetMapping("/placements/{placementId}/visits/{visitId}")
    public String placementVisit(@PathVariable int placementId, @PathVariable int visitId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<Placement> placementOpt = plRepo.findById(placementId);
        if (placementOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement placement = placementOpt.get();
        model.addAttribute("placement", placement);

        Optional<PlacementVisit> placementVisitOpt = vRepo.findById(visitId);
        if (placementVisitOpt.isEmpty()) {
            return "redirect:/placements/"+ placementId + "/visits?invalidId";
        }
        PlacementVisit visit = placementVisitOpt.get();
        model.addAttribute("visit", visit);

        if (!placement.getMembers().contains(u) && !placement.getProvider().getMembers().contains(u) && u.getRole() != Role.ADMINISTRATOR) {
            return "redirect:/placements/" + placementId + "?noVisitPermission";
        }

        // Generating link to add to Google Calendar, according to the specific link format.
        // Date values (day/month) and time values (hours/minutes) need to be two-digit - add a 0 where only one digit
        String monthValue = String.valueOf(visit.getVisitDateTime().getMonthValue());
        if (visit.getVisitDateTime().getMonthValue() < 10) {
            monthValue = "0" + visit.getVisitDateTime().getMonthValue();
        }

        String dayValue = String.valueOf(visit.getVisitDateTime().getDayOfMonth());
        if (visit.getVisitDateTime().getDayOfMonth() < 10) {
            dayValue = "0" + visit.getVisitDateTime().getDayOfMonth();
        }

        String hourValue = String.valueOf(visit.getVisitDateTime().toLocalTime().getHour());
        if (visit.getVisitDateTime().toLocalTime().getHour() < 10) {
            hourValue = "0" + visit.getVisitDateTime().toLocalTime().getHour();
        }

        String minuteValue = String.valueOf(visit.getVisitDateTime().toLocalTime().getMinute());
        if (visit.getVisitDateTime().toLocalTime().getMinute() < 10) {
            minuteValue = "0" + visit.getVisitDateTime().toLocalTime().getMinute();
        }

        // Build the Google Calendar link
        String googleCalendarURL = "https://www.google.com/calendar/event?" +
                "action=TEMPLATE" +
                "&text=Visit for Placement '" + placement.getTitle() + "'" +
                "&dates=" + visit.getVisitDateTime().getYear() + monthValue + dayValue +
                "T" + hourValue + minuteValue + "00" +
                "/" + visit.getVisitDateTime().getYear() + monthValue + dayValue +
                "T" + hourValue + minuteValue + "00" +
                "&details=Visit organised by " + visit.getOrganiser().getFullName() + " for placement " + placement.getTitle() +
                "&location=" + placement.getProvider().getAddressPostcode();

        model.addAttribute("googleCalendarURL", googleCalendarURL);

        return "placements/visits/visit";
    }

    @GetMapping("/placements/{placementId}/visits/{visitId}/ics")
    public ResponseEntity<Resource> icsForVisit(@PathVariable int placementId, @PathVariable int visitId, Principal principal) throws IOException {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Placement> placementOpt = plRepo.findById(placementId);
        if (placementOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Placement placement = placementOpt.get();

        Optional<PlacementVisit> placementVisitOpt = vRepo.findById(visitId);
        if (placementVisitOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        PlacementVisit visit = placementVisitOpt.get();

        if (!placement.getMembers().contains(u) && !placement.getProvider().getMembers().contains(u) && u.getRole() != Role.ADMINISTRATOR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        // Generating .ics file using iCal4j
        if (!Files.isDirectory(Path.of(System.getProperty("user.dir") + fileService.FS + "events"))) {
            fileService.createEventsDirectory();
        } else {
            fileService.cleanEventsDirectory(false);
        }

        FileOutputStream ics = new FileOutputStream(System.getProperty("user.dir") + fileService.FS + "events" + fileService.FS + "event" + visit.getId() + ".ics");

        // Defining the calendar used for the event
        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//Placements Management Web App//iCal4j//EN"));
        calendar.getProperties().add(CalScale.GREGORIAN);
        calendar.getProperties().add(Version.VERSION_2_0);

        // Defining event details according to placement and visit details
        DateTime start = new DateTime(Date.from(visit.getVisitDateTime().toInstant(ZoneOffset.of("+1"))));
        DateTime end = new DateTime(Date.from(visit.getVisitDateTime().plusHours(1).toInstant(ZoneOffset.of("+1"))));
        VEvent event = new VEvent(start, end, "Visit for placement '" + placement.getTitle() + "'");

        // Giving the event a unique id
        UidGenerator uidG = new RandomUidGenerator();
        Uid uid = uidG.generateUid();
        event.getProperties().add(uid);

        // Adding all members of the placement as "attendees" of the event, with linked email
        for (AppUser member: placement.getMembers()) {
            Attendee attendee = new Attendee(URI.create("mailto:" + member.getEmail()));
            attendee.getParameters().add(new Cn(member.getFullName()));

            // Students and tutors are required to attend the visit, others optionally
            if (member.getRole() == Role.STUDENT || member.getRole() == Role.TUTOR) {
                attendee.getParameters().add(net.fortuna.ical4j.model.parameter.Role.REQ_PARTICIPANT);
            } else {
                attendee.getParameters().add(net.fortuna.ical4j.model.parameter.Role.OPT_PARTICIPANT);
            }

            event.getProperties().add(attendee);
        }

        // Add the event to the calendar, and output as .ics
        calendar.getComponents().add(event);
        CalendarOutputter outputter = new CalendarOutputter();
        outputter.output(calendar, ics);

        Resource file = new UrlResource(Paths.get(System.getProperty("user.dir") + fileService.FS + "events" + fileService.FS + "event" + visit.getId() + ".ics").toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"event" + visit.getId() + ".ics\"").body(file);
    }

    @GetMapping("/placements/{placementId}/visits/{visitId}/edit")
    public String editVisitForm(@PathVariable int placementId, @PathVariable int visitId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<Placement> placementOpt = plRepo.findById(placementId);
        if (placementOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement placement = placementOpt.get();
        model.addAttribute("placement", placement);

        Optional<PlacementVisit> placementVisitOpt = vRepo.findById(visitId);
        if (placementVisitOpt.isEmpty()) {
            return "redirect:/placements/" + placementId + "/visits?invalidId";
        }
        PlacementVisit visit = placementVisitOpt.get();
        model.addAttribute("visit", visit);

        if (!(visit.getOrganiser() == u) && !(u.getRole() == Role.ADMINISTRATOR)) {
            return "redirect:/placements/" + placementId + "/visits/" + visitId + "?noEditPermission";
        }

        return "placements/visits/edit_visit";
    }

    @PostMapping("/placements/{placementId}/visits/{visitId}/edit")
    public String editVisit(@PathVariable int placementId, @PathVariable int visitId,
                            @RequestParam("visitDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDate,
                            @RequestParam("visitTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime visitTime,
                            Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        if (plRepo.findById(placementId).isEmpty()) {
            return "redirect:/placements?invalidId";
        }

        Optional<PlacementVisit> placementVisitOpt = vRepo.findById(visitId);
        if (placementVisitOpt.isEmpty()) {
            return "redirect:/placements/" + placementId + "/visits?invalidId";
        }

        PlacementVisit visit = placementVisitOpt.get();
        if (!(visit.getOrganiser() == u) && !(u.getRole() == Role.ADMINISTRATOR)) {
            return "redirect:/placements/" + placementId + "/visits/" + visitId + "?noEditPermission";
        }

        // Replace old visit date and time with new changes
        LocalDateTime oldDateTime = visit.getVisitDateTime();
        visit.setVisitDateTime(LocalDateTime.of(visitDate, visitTime));

        vRepo.save(visit);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.UPDATE, LogEntity.PLACEMENT_VISIT, visit.getId(),
                "User changed visit date and time from '" + oldDateTime.toString() + "' to '" +
                        LocalDateTime.of(visitDate, visitTime).toString() + "'"));

        return "redirect:/placements/" + placementId + "/visits/" + visitId + "?editSuccess";
    }

    @GetMapping("/placements/{placementId}/visits/new")
    public String newVisitForm(@PathVariable int placementId, Principal principal, Model model) {
        AppUser u = uRepo.findByUsername(principal.getName());
        model.addAttribute("user", u);

        Optional<Placement> placementOpt = plRepo.findById(placementId);
        if (placementOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement placement = placementOpt.get();
        model.addAttribute("placement", placement);

        if ((u.getRole() != Role.ADMINISTRATOR && u.getRole() != Role.TUTOR) || (u.getRole() == Role.TUTOR && !(placement.getMembers().contains(u)))) {
            return "redirect:/placements/" + placementId + "?noVisitPermission";
        }

        return "placements/visits/new_visit";
    }

    @PostMapping("/placements/{placementId}/visits/new")
    public String newVisit(@PathVariable int placementId,
                           @RequestParam("visitDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDate,
                           @RequestParam("visitTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime visitTime,
                           Principal principal) {
        AppUser u = uRepo.findByUsername(principal.getName());

        Optional<Placement> placementOpt = plRepo.findById(placementId);
        if (placementOpt.isEmpty()) {
            return "redirect:/placements?invalidId";
        }
        Placement placement = placementOpt.get();

        if ((u.getRole() != Role.ADMINISTRATOR && u.getRole() != Role.TUTOR) || (u.getRole() == Role.TUTOR && !(placement.getMembers().contains(u)))) {
            return "redirect:/placements/" + placementId + "?noVisitPermission";
        }

        // Create visit with given details, set the current user as the organiser
        PlacementVisit visit = new PlacementVisit(LocalDateTime.of(visitDate, visitTime));
        visit.setOrganiser(u);
        placement.getVisits().add(visit);

        vRepo.save(visit);

        plRepo.save(placement);

        lRepo.save(new LogEntry(u, LocalDateTime.now(), LogAction.CREATE, LogEntity.PLACEMENT_VISIT, visit.getId(),
                "User created new visit for placement #" + placement.getId()));

        // Attempt to send newly scheduled visit email notification to all relevant users
        mailService.sendVisitScheduledNotification(visit, placement);

        return "redirect:/placements/" + placementId + "?visitSuccess";
    }

}
