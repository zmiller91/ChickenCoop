package coop.local.config;

import coop.local.PiContext;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway webFlyway(PiContext context) {
        return Flyway.configure()
                .dataSource("jdbc:mysql://localhost/local_pi", context.dbUser(), context.dbPassword())
                .locations("classpath:sql/shared")
                .load();
    }

    @Bean(initMethod = "migrate")
    public Flyway stateFlyway(PiContext context) {
        return Flyway.configure()
                .dataSource("jdbc:mysql://localhost/pi_state", context.dbUser(), context.dbPassword())
                .locations("classpath:sql/local")
                .load();
    }
}