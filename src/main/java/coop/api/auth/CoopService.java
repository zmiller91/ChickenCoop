package coop.api.auth;

import coop.database.repository.PiRepository;
import coop.database.table.Pi;
import coop.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@EnableTransactionManagement
@Transactional
@RestController
@RequestMapping(value = "/coops")
public class CoopService {

    @Autowired
    private PiRepository piRepository;

    @Autowired
    private AuthContext userContext;

    @PostMapping("/create")
    public CreateCoopResponse create(@RequestBody  CreateCoopRequest request) {
        Pi coop = piRepository.create(userContext.getCurrentUser(), request.name);
        return new CreateCoopResponse(new CoopDAO(coop.getId(), coop.getName()));
    }

    @GetMapping("/list")
    public ListCoopResponse list() {
         List<CoopDAO> coops = piRepository.list(userContext.getCurrentUser())
                 .stream()
                 .map(c -> new CoopDAO(c.getId(), c.getName()))
                 .toList();

         return new ListCoopResponse(coops);
    }

    public record CreateCoopRequest(String name) {}
    public record CreateCoopResponse(CoopDAO coop){}


    public record ListCoopRequest() {}
    public record ListCoopResponse(List<CoopDAO> coops){}

    public record CoopDAO(String id, String name){};
}
