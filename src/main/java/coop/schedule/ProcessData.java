package coop.schedule;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.google.gson.Gson;
import coop.database.repository.CoopRepository;
import coop.database.repository.MetricRepository;
import coop.database.repository.PiRepository;
import coop.database.table.Coop;
import coop.database.table.CoopMetric;
import coop.database.table.Pi;
import coop.pi.metric.Metric;
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

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
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

            Pi pi = piRepository.findByClientId(metric.getClientId());
            if (pi != null) {
                Coop coop = coopRepository.findById(pi, metric.getCoopId());
                if(coop != null) {
                    CoopMetric coopMetric = new CoopMetric();
                    coopMetric.setDt(metric.getDt());
                    coopMetric.setCoop(coop);
                    coopMetric.setComponentId(metric.getComponentId());
                    coopMetric.setMetric(metric.getMetric());
                    coopMetric.setValue(metric.getValue());
                    metricRepository.persist(coopMetric);
                }
            }

            DeleteMessageRequest delete = new DeleteMessageRequest();
            delete.setQueueUrl(metricSqsUrl);
            delete.setReceiptHandle(message.getReceiptHandle());
            sqs.deleteMessage(delete);
        });
    }

}
