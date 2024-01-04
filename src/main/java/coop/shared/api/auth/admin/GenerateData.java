package coop.shared.api.auth.admin;

import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.MetricRepository;
import coop.shared.database.repository.PiRepository;
import coop.shared.database.table.Coop;
import coop.shared.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableTransactionManagement
@Transactional
@RestController
@RequestMapping(value = "/generate")
public class GenerateData {

    private static final String COOP_ID = "4028b2698ca7ff21018ca8004f970000";

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private PiRepository piRepository;

    @Autowired
    private AuthContext userContext;

    @Autowired
    MetricRepository metricRepository;

    @GetMapping("/data")
    public GenerateResponse generate() {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), COOP_ID);
        PublishWeather publishWeather = new PublishWeather(metricRepository, "C:\\Users\\zmiller\\IdeaProjects\\ChickenCoop\\weatherdata.json");
        publishWeather.execute(coop);
        return new GenerateResponse();
    }

    public record GenerateResponse(){};
}
