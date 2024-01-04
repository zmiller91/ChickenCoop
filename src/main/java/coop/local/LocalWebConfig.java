package coop.local;

import coop.local.mqtt.PiMqttClient;
import coop.shared.pi.StateProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class LocalWebConfig {

    @Bean
    public PiMqttClient client() {
        String clientEndpoint = Context.getInstance().endpoint();
        String clientId = Context.getInstance().clientId();
        String certificateFile = Context.getInstance().certKey();
        String privateKeyFile = Context.getInstance().privateKey();

        return new PiMqttClient(clientEndpoint, clientId, certificateFile, privateKeyFile);
    }

    @Bean
    public StateProvider stateProvider(LocalStateProvider localStateProvider) {
        return localStateProvider;
    }

    @Bean
    @Qualifier("PiRunnerExecutor")
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor(); // Or use another one of your liking
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
