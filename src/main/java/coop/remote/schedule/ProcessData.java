package coop.remote.schedule;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.google.gson.Gson;
import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.MetricRepository;
import coop.shared.database.repository.PiRepository;
import coop.shared.database.table.Coop;
import coop.shared.database.table.Pi;
import coop.shared.pi.metric.Metric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
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
    private CoopRepository coopRepository;

    @Autowired
    private MetricRepository metricRepository;

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
        messages.forEach(message -> {
            Metric metric = GSON.fromJson(message.getBody(), Metric.class);

            //TODO: We should probably get this from the principal, since that's derived from the certificates.
            Pi pi = piRepository.findByClientId(metric.getClientId());
            if (pi != null) {
                Coop coop = coopRepository.findById(pi, metric.getCoopId());
                if(coop != null) {
                    metricRepository.save(
                            coop,
                            metric.getComponentId(),
                            metric.getDt(),
                            metric.getMetric(),
                            metric.getValue());
                }
            }

            DeleteMessageRequest delete = new DeleteMessageRequest();
            delete.setQueueUrl(metricSqsUrl);
            delete.setReceiptHandle(message.getReceiptHandle());
            sqs.deleteMessage(delete);
        });
    }

}
