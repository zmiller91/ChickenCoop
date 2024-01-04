package coop.shared.api.pub;

import coop.shared.database.repository.AuthorityRepository;
import coop.shared.database.repository.UserRepository;
import coop.shared.database.table.Authority;
import coop.shared.database.table.User;
import coop.shared.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

@RestController
@EnableTransactionManagement
@Transactional
public class Login {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtTokenUtil jwt;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthorityRepository authorityRepository;

    @Transactional
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest loginRequest) {

        Authentication authenticationRequest = UsernamePasswordAuthenticationToken
                .unauthenticated(loginRequest.username(), loginRequest.password());
        Authentication authenticationResponse =  this.authenticationManager.authenticate(authenticationRequest);
        User user = (User) authenticationResponse.getPrincipal();

        return new AuthResponse(jwt.generateToken(user));
    }

    @Transactional
    @PostMapping("/register")
    public String register(@RequestBody RegistrationRequest registration) {

        User user = new User();
        user.setUsername(registration.username);
        user.setPassword(encoder.encode(registration.password()));
        user.setEnabled(true);
        userRepository.persist(user);

        Authority authority = new Authority();
        authority.setUser(user);
        authority.setAuthority("USER");
        authorityRepository.persist(authority);

        userRepository.flush();
        return jwt.generateToken(user);
    }

    public record LoginRequest(String username, String password) {
    }


    public record RegistrationRequest(String username, String password) {}

    public record AuthResponse(String token){};
}
