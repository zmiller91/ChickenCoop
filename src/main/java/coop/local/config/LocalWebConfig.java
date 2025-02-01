package coop.local.config;

import coop.local.PiContext;
import coop.local.CoopRunner;
import coop.local.state.DatabaseStateProvider;
import coop.local.comms.Communication;
import coop.local.comms.serial.DevSerialCommunication;
import coop.local.comms.serial.PiSerialCommunication;
import coop.local.comms.serial.SerialCommunication;
import coop.local.mqtt.PiMqttClient;
import coop.local.state.MqttStateProvider;
import coop.shared.pi.StateProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.util.Arrays;

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
    @Qualifier("coop_id")
    public String coopId(@Value("${coop.id}") String coopId) {
        return coopId;
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
            serial = new DevSerialCommunication("COM4");
        }

        return new Communication(serial);
    }

    /**
     * Data can either be stored locally or it can be sent to AWS and stored remotely. Change this function's input
     * parameter type to dictate which should be used.
     */
    @Bean
    @Primary
    public StateProvider stateProvider(MqttStateProvider localStateProvider) {
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
