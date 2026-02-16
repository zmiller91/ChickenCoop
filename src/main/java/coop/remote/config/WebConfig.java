package coop.remote.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIot;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.AWSIotDataClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;


@Configuration
public class WebConfig {

    @Bean
    public AWSIotData awsIotData() {
        return AWSIotDataClientBuilder.defaultClient();
    }

    @Bean
    public AWSIot awsIot() {
        return AWSIotClient.builder()
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    @Bean
    public AmazonSQS awsSqs() {
        return AmazonSQSClientBuilder.defaultClient();
    }

    @Bean
    public SesClient ses() {
        return SesClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @Bean(name = "metricSqsUrl")
    public String metricSqsUrl() {
        return "https://sqs.us-east-1.amazonaws.com/547228847576/HubEventQueue";
    }
}
