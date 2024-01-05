package coop.local.mqtt;

import coop.local.Context;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

public enum ShadowTopic {
    GET_ACCEPTED("$aws/things/%s/shadow/get/accepted"),
    GET_REJECTED("$aws/things/%s/shadow/get/rejected"),
    UPDATE_DELTA("$aws/things/%s/shadow/update/delta"),
    UPDATE_ACCEPTED("$aws/things/%s/shadow/update/accepted"),
    UPDATE_DOCUMENTS("$aws/things/%s/shadow/update/documents"),
    UPDATE_REJECTED("$aws/things/%s/shadow/update/rejected"),
    DELETE_ACCEPTED("$aws/things/%s/shadow/accepted"),
    DELETE_REJECTED("$aws/things/%s/shadow/rejected"),

    GET("$aws/things/%s/shadow/get"),
    UPDATE("$aws/things/%s/shadow/update"),
    DELETE("$aws/things/%s/shadow/delete"),
    METRIC("datatopic");

    private String shadowName;
    private final String topic;

    ShadowTopic(String topic) {
        this.topic = topic;
    }

    public String topic() {
        return String.format(topic, shadowName);
    }

    private void setShadowName(String shadowName) {
        this.shadowName = shadowName;
    }

    @Component
    public static class ShadowTopicInjector {

        @Autowired
        private Context context;

        @PostConstruct
        public void postConstruct() {
            for (ShadowTopic st : EnumSet.allOf(ShadowTopic.class))
                st.setShadowName(context.shadowName());
        }
    }
}
