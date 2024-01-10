package coop.local.config;

import coop.local.PiContext;
import coop.local.CoopRunner;
import coop.local.LocalStateProvider;
import coop.local.comms.Communication;
import coop.local.comms.DevSerialCommunication;
import coop.local.comms.PiSerialCommunication;
import coop.local.comms.SerialCommunication;
import coop.local.mqtt.PiMqttClient;
import coop.shared.pi.StateProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.io.Serial;
import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
public class LocalWebConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {


    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        factory.setPort(8042);
    }

    @Bean
    public PiContext context(@Value("${context.path}") String path) {
        return new PiContext(path);
    }

    @Bean
    public PiMqttClient client(PiContext piContext) {

        String clientEndpoint = piContext.endpoint();
        String clientId = piContext.clientId();
        String certificateFile = piContext.certKey();
        String privateKeyFile = piContext.privateKey();

        return new PiMqttClient(clientEndpoint, clientId, certificateFile, privateKeyFile);
    }

    @Bean
    public Communication serialCommunication(Environment env) {

        SerialCommunication serial;
        boolean isPi = Arrays.asList(env.getActiveProfiles()).contains("pi");

        if (isPi) {
            serial = new PiSerialCommunication("/dev/ttyAMA0");
        } else {
            serial = new DevSerialCommunication("COM6");
        }

        return new Communication(serial);
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
