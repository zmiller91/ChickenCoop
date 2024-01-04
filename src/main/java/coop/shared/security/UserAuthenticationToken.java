package coop.shared.security;

import coop.shared.database.table.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@EnableTransactionManagement
@Transactional
public class UserAuthenticationToken extends AbstractAuthenticationToken {

    private final User user;

    public UserAuthenticationToken(User user, List<SimpleGrantedAuthority> authorities) {
        super(authorities);
        this.user = user;
    }

    @Override
    public String getCredentials() {
        return user.getPassword();
    }

    @Override
    public User getPrincipal() {
        return user;
    }

    public static UserAuthenticationToken create(User user) {
        List<SimpleGrantedAuthority> authorities = user.getAuthorities()
                .stream()
                .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
                .toList();

        return new UserAuthenticationToken(user, authorities);
    }

}
