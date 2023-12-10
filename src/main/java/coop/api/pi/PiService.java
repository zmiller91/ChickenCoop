package coop.api.pi;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/pi")
public class PiService {

    @PostMapping("/post")
    String home(@RequestBody Test message) {
        return message.message;
    }

    public record Test(String message){};
}
