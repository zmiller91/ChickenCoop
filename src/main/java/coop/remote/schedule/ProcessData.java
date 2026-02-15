package coop.remote.schedule;

import com.amazonaws.services.iot.AWSIot;
import com.amazonaws.services.iot.model.*;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import coop.shared.database.repository.*;
import coop.shared.database.table.Coop;
import coop.shared.database.table.Pi;
import coop.shared.pi.events.MetricReceived;
import coop.shared.pi.events.RuleSatisfiedHubEvent;
import coop.shared.projection.InboxMessageProjection;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class ProcessData {

    private static final Gson GSON = new Gson();

    @Autowired
    private AmazonSQS sqs;

    @Autowired
    private AWSIot iot;

    @Autowired
    @Qualifier("metricSqsUrl")
    private String metricSqsUrl;

    @Autowired
    private PiRepository piRepository;

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private InboxMessageRepository inboxRepository;

    @Scheduled(fixedDelay = 5, initialDelay = 0, timeUnit = TimeUnit.MINUTES)
    public void processQueue() {

        System.out.println("\n\n\nProcessing schedule\n\n\n");

        ReceiveMessageRequest request = new ReceiveMessageRequest();
        request.setQueueUrl(metricSqsUrl);
        request.setMaxNumberOfMessages(10);

        ReceiveMessageResult result = sqs.receiveMessage(request);
        while(!result.getMessages().isEmpty()) {
            process(result.getMessages());
            result = sqs.receiveMessage(request);
        }
    }

    private void process(List<Message> messages) {
        for(Message message : messages) {

            IoTMessage iotMessage = GSON.fromJson(message.getBody(), IoTMessage.class);
            Pi pi = piRepository.findByThumbprint(iotMessage.getPrincipal());

            if(pi == null) {
                log.warn("Could not find pi for thumbprint: " + iotMessage.getPrincipal());
            }

            else {

                switch(iotMessage.getEvent().getType()) {
                    case METRIC:

                        MetricReceived metric = GSON.fromJson(iotMessage.getEvent().getPayload(), MetricReceived.class);
                        Coop coop = coopRepository.findById(pi, metric.getCoopId());
                        if(coop != null) {
                            metricRepository.save(
                                    coop,
                                    metric.getComponentId(),
                                    metric.getDt(),
                                    metric.getMetric(),
                                    metric.getValue());
                        }

                        break;

                    case RULE_SATISFIED:
                        saveRuleExecution(pi, GSON.fromJson(iotMessage.getEvent().getPayload(), RuleSatisfiedHubEvent.class));
                        break;
                }
            }


            DeleteMessageRequest delete = new DeleteMessageRequest();
            delete.setQueueUrl(metricSqsUrl);
            delete.setReceiptHandle(message.getReceiptHandle());
            sqs.deleteMessage(delete);
        }
    }

    private void saveRuleExecution(Pi pi, RuleSatisfiedHubEvent ruleExecution) {
        InboxMessageProjection projection = new InboxMessageProjection(coopRepository, ruleRepository);
        projection.from(pi, ruleExecution).forEach(message -> {
            inboxRepository.persist(message);
        });
    }
}
