package coop.remote;

import coop.shared.pi.StateProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RemoteWebConfig {

    @Bean
    public StateProvider stateProvider() {
        return new RemoteStateProvider();
    }

}
