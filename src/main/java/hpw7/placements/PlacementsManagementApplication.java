package hpw7.placements;

import hpw7.placements.domain.Role;
import hpw7.placements.dto.AppUserDTO;
import hpw7.placements.repository.AppUserRepository;
import hpw7.placements.service.CustomUserDetailsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main application class. When the application is started, and fully initialised, the 'run' method will be called
 * from here.
 */
@SpringBootApplication
public class PlacementsManagementApplication implements ApplicationRunner {

    public PlacementsManagementApplication(AppUserRepository uRepo, CustomUserDetailsService cUDService) {
        this.uRepo = uRepo;
        this.cUDService = cUDService;
    }

    public static void main(String[] args) {
        SpringApplication.run(PlacementsManagementApplication.class, args);
    }

    private final AppUserRepository uRepo;

    private final CustomUserDetailsService cUDService;

    @Override
    public void run(ApplicationArguments args) {
        if (!uRepo.existsByRole(Role.ADMINISTRATOR)) {
            // No administrator account exists, create one and print a password to the console.
            String username = "admin";
            while (uRepo.existsByUsername(username)) {
                username += "_";
            }
            String passwordGen = RandomStringUtils.randomAscii(20);
            AppUserDTO u = new AppUserDTO(username, "Default Administrator", "admin@default.acc",
                    passwordGen, "ADMINISTRATOR");
            cUDService.save(u);
            System.out.println("=========== INFO ===========");
            System.out.println("No existing admin accounts were found in the database.");
            System.out.println("One has been automatically created with the following credentials:");
            System.out.println("    Username: " + username + "\n    Password: " + passwordGen);
            System.out.println("It is recommended to change this password as soon as possible," +
                    " from your profile once logged in.");
        }
    }
}
