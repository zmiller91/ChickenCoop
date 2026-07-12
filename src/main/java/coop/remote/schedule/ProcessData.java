package coop.remote.schedule;

import com.amazonaws.services.iot.model.*;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import coop.shared.database.repository.*;
import coop.shared.database.table.Pi;
import coop.shared.pi.events.MetricReceived;
import coop.shared.pi.events.RuleSatisfiedHubEvent;
import coop.shared.pi.events.PortActionHubEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log4j2
public class ProcessData {

    private static final Gson GSON = new Gson();

    @Autowired
    private AmazonSQS sqs;

    @Autowired
    @Qualifier("metricSqsUrl")
    private String metricSqsUrl;

    @Autowired
    private PiRepository piRepository;

    @Autowired
    private RuleSatisfiedProcessor ruleSatisfiedProcessor;

    @Autowired
    private MetricProcessor metricProcessor;

    @Autowired
    private PortActionProcessor portActionProcessor;

    // fixedDelay = 0 means the next poll starts immediately once the previous one returns - combined with the
    // 20s long poll below, this keeps a connection to SQS effectively always open instead of sitting idle
    // between short-polls, so a message shows up in the UI within ~20s instead of up to 5 minutes.
    @Scheduled(fixedDelay = 0)
    public void processQueue() {

        ReceiveMessageRequest request = new ReceiveMessageRequest();
        request.setQueueUrl(metricSqsUrl);
        request.setMaxNumberOfMessages(10);
        request.setWaitTimeSeconds(20);

        ReceiveMessageResult result = sqs.receiveMessage(request);
        while(!result.getMessages().isEmpty()) {
            process(result.getMessages());
            result = sqs.receiveMessage(request);
        }
    }

    private void process(List<Message> messages) {
        for(Message message : messages) {

            try {

                process(message);

                DeleteMessageRequest delete = new DeleteMessageRequest();
                delete.setQueueUrl(metricSqsUrl);
                delete.setReceiptHandle(message.getReceiptHandle());
                sqs.deleteMessage(delete);

            } catch (Throwable t) {
                log.error("Error processing message.", t);
            }
        }
    }

    private void process(Message message) {

        IoTMessage iotMessage = GSON.fromJson(message.getBody(), IoTMessage.class);
        Pi pi = piRepository.findByThumbprint(iotMessage.getPrincipal());
        if(pi == null) {
            log.warn("Could not find pi for thumbprint: " + iotMessage.getPrincipal());
        }

        else {

            JsonElement payload = iotMessage.getEvent().getPayload();
            switch(iotMessage.getEvent().getType()) {
                case METRIC:

                    MetricReceived metric = GSON.fromJson(payload, MetricReceived.class);
                    metricProcessor.process(pi, metric);
                    break;

                case RULE_SATISFIED:

                    RuleSatisfiedHubEvent event = GSON.fromJson(payload, RuleSatisfiedHubEvent.class);
                    ruleSatisfiedProcessor.process(pi, event);
                    break;

                case PORT_ACTION:

                    PortActionHubEvent portEvent = GSON.fromJson(payload, PortActionHubEvent.class);
                    portActionProcessor.process(pi, portEvent);
                    break;
            }
        }
    }
}
