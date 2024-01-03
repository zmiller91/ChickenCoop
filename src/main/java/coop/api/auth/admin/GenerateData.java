package coop.api.auth.admin;

import coop.api.auth.CoopService;
import coop.api.auth.CoopStateProvider;
import coop.database.repository.CoopRepository;
import coop.database.repository.MetricRepository;
import coop.database.repository.PiRepository;
import coop.database.table.Coop;
import coop.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private CoopStateProvider coopStateProvider;

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
