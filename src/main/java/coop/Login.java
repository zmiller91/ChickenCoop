package coop;

import coop.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.web.bind.annotation.*;

@RestController
public class Login {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtTokenUtil jwt;

    @Autowired
    UserDetailsService userDetailsService;

    @RequestMapping("/login")
    String home() {
        return "You must login!!";
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest loginRequest) {

        Authentication authenticationRequest = UsernamePasswordAuthenticationToken
                .unauthenticated(loginRequest.username(), loginRequest.password());
        Authentication authenticationResponse =  this.authenticationManager.authenticate(authenticationRequest);
        User user = (User) authenticationResponse.getPrincipal();

        return new AuthResponse(jwt.generateToken(user));
    }

    @PostMapping("/register")
    public String register(@RequestBody RegistrationRequest registration) {

        UserDetails details = User.builder()
                .username(registration.username())
                .password(encoder.encode(registration.password()))
                .disabled(false)
                .authorities("USER")
                .build();

        ((JdbcUserDetailsManager) userDetailsService).createUser(details);
        return jwt.generateToken(details);
    }

    public record LoginRequest(String username, String password) {
    }


    public record RegistrationRequest(String username, String password) {}

    public record AuthResponse(String token){};
}
