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

import java.time.*;
import java.time.format.DateTimeFormatter;
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

            Pi pi = piRepository.findByClientId(metric.getClientId());
            if (pi != null) {
                Coop coop = coopRepository.findById(pi, metric.getCoopId());
                if(coop != null) {

                    Instant instant = Instant.ofEpochMilli(metric.getDt());
                    ZonedDateTime zdt = instant.atZone(ZoneId.of("America/Chicago"));
                    DateTimeFormatter yearFormat = DateTimeFormatter.ofPattern("yyyy");
                    DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("yyyyMM");
                    DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
                    DateTimeFormatter hourFormat = DateTimeFormatter.ofPattern("yyyyMMddhh");

                    CoopMetric coopMetric = new CoopMetric();

                    coopMetric.setDt(metric.getDt());
                    coopMetric.setYear(Integer.parseInt(yearFormat.format(zdt)));
                    coopMetric.setMonth(Integer.parseInt(monthFormat.format(zdt)));
                    coopMetric.setDay(Integer.parseInt(dayFormat.format(zdt)));
                    coopMetric.setHour(Integer.parseInt(hourFormat.format(zdt)));

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
