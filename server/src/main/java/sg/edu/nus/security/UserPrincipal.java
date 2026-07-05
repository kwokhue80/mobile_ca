package sg.edu.nus.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import sg.edu.nus.features.user.account.User;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final String id;
    private final String emailAddress;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    // Map of database User entity to this Principal
    public UserPrincipal(User user) {
        this(user, DEFAULT_ROLE);
    }

    public UserPrincipal(User user, String role) {
        this.id = user.getId().toString();
        this.emailAddress = user.getEmailAddress();
        this.password = user.getPasswordHash();
        this.enabled = user.getEnabled();
        String effectiveRole = (role == null || role.isBlank()) ? DEFAULT_ROLE : role;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(effectiveRole));
    }

    /**
     * Custom Method Required by Controllers Layers (e.g., WellnessController)
     * to fetch the UUID for database foreign key relations.
     */
    public String getId() {
        return id;
    }

    // --- Spring Security UserDetails Overrides ---

    @Override
    public String getUsername() {
        // The Email Address serves as the Username.
        return emailAddress;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Not implemented in current DB schema, default to true
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Not implemented in current DB schema, default to true
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Not implemented in current DB schema, default to true
    }
}