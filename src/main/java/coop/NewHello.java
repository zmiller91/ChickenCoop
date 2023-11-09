package coop;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NewHello {

    @RequestMapping("/new")
    String home() {

        ;

        return "Hello " + SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
