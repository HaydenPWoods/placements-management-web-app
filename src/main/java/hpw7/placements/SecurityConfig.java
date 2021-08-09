package hpw7.placements;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Configuring basic access permissions and security policies. "Simpler" permission controls are included here (for
 * example, permitting only students the ability to create an authorisation request or apply for a placement). Where
 * determining if a user is allowed to perform an action is more complex (for example, only administrators and members
 * of the provider can add new placements for that provider), this access control is handled at the controller level.
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/", "/app-register", "/register", "/css/*", "/images/*", "/js/*", "/firebase-messaging-sw.js", "/favicon.ico").permitAll()
                .antMatchers(HttpMethod.POST, "/ajax/email-check").permitAll()
                .antMatchers(HttpMethod.POST, "/ajax/username-check").permitAll()
                .antMatchers(HttpMethod.POST, "/ajax/tutor-check").permitAll()
                .mvcMatchers(HttpMethod.GET, "/requests/new", "/placements/*/apply").hasRole("STUDENT")
                .mvcMatchers("/requests", "/requests/*").hasAnyRole("ADMINISTRATOR", "TUTOR", "STUDENT", "GRADUATE")
                .mvcMatchers(HttpMethod.POST, "/requests/*/approve").hasAnyRole("ADMINISTRATOR", "TUTOR")
                .mvcMatchers(HttpMethod.GET, "/admin", "/admin/send-group-email", "/users", "/users/*/delete", "/stats", "/export/*", "/export/*/*", "/providers/*/add-member", "/providers/new").hasRole("ADMINISTRATOR")
                .mvcMatchers(HttpMethod.POST, "/users/*/changeRole", "/admin/send-group-email", "/placements/*/delete", "/providers/*/add-member", "/providers/*/remove-member", "/providers/new", "/providers/*/delete").hasRole("ADMINISTRATOR")
                .mvcMatchers("/placements/*/edit", "/placements/*/assign-user", "/placements/*/remove-user").hasAnyRole("ADMINISTRATOR", "PROVIDER", "TUTOR")
                .mvcMatchers("/placements/add", "/providers/*/edit").hasAnyRole("ADMINISTRATOR", "PROVIDER")
                .mvcMatchers("/record").hasRole("GRADUATE")
                .mvcMatchers("/messaging", "/messaging/new", "/messaging/user/*", "/visits").hasAnyRole("ADMINISTRATOR", "PROVIDER", "TUTOR", "STUDENT")
                .anyRequest().authenticated()
                .and().formLogin()
                .loginPage("/app-login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/app-login?invalid")
                .permitAll()
                .and().headers().frameOptions().sameOrigin() // Allowing PDFs to be embedded in an iframe on the document page
                .and().logout()
                .invalidateHttpSession(true)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/app-login").permitAll();
    }

    private final PasswordEncoder pEncoder;

    private final UserDetailsService userDetailsService;

    public SecurityConfig(PasswordEncoder pEncoder, @Qualifier("CustomUserDetailsService") UserDetailsService userDetailsService) {
        this.pEncoder = pEncoder;
        this.userDetailsService = userDetailsService;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(pEncoder);
    }
}
