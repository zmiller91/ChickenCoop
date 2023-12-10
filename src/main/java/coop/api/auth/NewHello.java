package coop.api.auth;

import coop.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NewHello {

    @Autowired
    private AuthContext userContext;

    @RequestMapping("/new")
    String home() {

        return "Hello " + userContext.getCurrentUser().getUsername() +
                ". Are you authenticated? " + userContext.isRequestAuthenticated();
    }
}
