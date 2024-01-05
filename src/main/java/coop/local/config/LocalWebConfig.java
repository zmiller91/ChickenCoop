package coop.local.config;

import coop.local.Context;
import coop.local.CoopRunner;
import coop.local.LocalStateProvider;
import coop.local.mqtt.PiMqttClient;
import coop.shared.pi.StateProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class LocalWebConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        factory.setPort(8042);
    }

    @Bean
    public Context context(@Value("${context.path}") String path) {
        return new Context(path);
    }

    @Bean
    public PiMqttClient client(Context context) {

        String clientEndpoint = context.endpoint();
        String clientId = context.clientId();
        String certificateFile = context.certKey();
        String privateKeyFile = context.privateKey();

        return new PiMqttClient(clientEndpoint, clientId, certificateFile, privateKeyFile);
    }

    @Bean
    public StateProvider stateProvider(LocalStateProvider localStateProvider) {
        return localStateProvider;
    }

    @Bean
    @Qualifier("PiRunnerExecutor")
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }

    @Bean
    public CommandLineRunner runner(@Qualifier("PiRunnerExecutor") TaskExecutor executor, CoopRunner coopRunner) {
        return new CommandLineRunner() {

            @Override
            public void run(String... args) throws Exception {
                executor.execute(coopRunner);
            }
        };
    }
}
