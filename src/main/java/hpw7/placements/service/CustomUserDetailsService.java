package hpw7.placements.service;

import hpw7.placements.domain.AppUser;
import hpw7.placements.domain.Role;
import hpw7.placements.dto.AppUserDTO;
import hpw7.placements.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>CustomUserDetailsService</code> enables users to be stored, and loaded with the correct permissions when
 * authenticated.
 */
@Service
@Qualifier("CustomUserDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository uRepo;

    private final PasswordEncoder pEncoder;

    public CustomUserDetailsService(AppUserRepository uRepo, PasswordEncoder pEncoder) {
        this.uRepo = uRepo;
        this.pEncoder = pEncoder;
    }

    /**
     * Given a username, loads the user with that username in the database, and returns a Spring Security {@link User}.
     *
     * @param username The username of the user to load.
     * @return An enabled {@link User} with the role set as assigned to the user.
     * @throws UsernameNotFoundException A user with the given username was not found.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = uRepo.findByUsername(username);
        if (u == null) {
            throw new UsernameNotFoundException(username + "not found!");
        } else {
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + u.getRole()));
            return new User(u.getUsername(), u.getPassword(), true, true, true,
                    true, authorities);
        }
    }

    /**
     * Creates and saves a user in the database with the given details, and the password encoded.
     *
     * @param appUserDTO An {@link AppUserDTO} object with the user details.
     * @return A new {@link AppUser} with the provided details.
     */
    public AppUser save(AppUserDTO appUserDTO) {
        AppUser u = new AppUser(appUserDTO.getUsername(), appUserDTO.getFullName(), appUserDTO.getEmail(),
                pEncoder.encode(appUserDTO.getPassword()), Role.valueOf(appUserDTO.getRole()));
        u = uRepo.save(u);
        return u;
    }
}
