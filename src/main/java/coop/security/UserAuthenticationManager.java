package coop.security;

import coop.database.repository.UserRepository;
import coop.database.table.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserAuthenticationManager implements AuthenticationManager {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String username = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();

        User user = userRepository.findByUsername(username);
        if (user == null || !user.isEnabled()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if(!encoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        Authentication userAuthentication =  UserAuthenticationToken.create(user);
        userAuthentication.setAuthenticated(true);
        return userAuthentication;
    }
}
