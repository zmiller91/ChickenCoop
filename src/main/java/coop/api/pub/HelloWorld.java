package coop.api.pub;

import coop.database.table.User;
import coop.database.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@EnableTransactionManagement
public class HelloWorld {

    @Autowired
    UserRepository userRepository;

    @Transactional
    @RequestMapping("/pub/hibernate")
    List<String> home() {
        return userRepository.list().stream().map(u -> u.getUsername()).collect(Collectors.toList());
    }
}
