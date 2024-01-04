package coop.shared.config;

import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.AWSIotDataClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class WebConfig {

    @Bean
    public AWSIotData awsIotData() {
        return AWSIotDataClientBuilder.defaultClient();
    }

    @Bean
    public AmazonSQS awsSqs() {
        return AmazonSQSClientBuilder.defaultClient();
    }

    @Bean(name = "metricSqsUrl")
    public String metricSqsUrl() {
        return "https://sqs.us-east-1.amazonaws.com/547228847576/CoopDataQueue";
    }
}
