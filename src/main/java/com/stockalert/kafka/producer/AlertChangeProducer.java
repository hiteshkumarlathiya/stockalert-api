package com.stockalert.kafka.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.stockalert.common.AlertChangeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertChangeProducer {

    private final KafkaTemplate<String, AlertChangeEvent> kafkaTemplate;

    @Value("${confluent.topic.alert.changes}")
    private String topic;

    public void publish(AlertChangeEvent event) {
        String key = event.getSymbol();
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish alert change: {}", event, ex);
            } else {
                log.info("Published alert change: topic={}, partition={}, offset={}, key={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        key);
            }
        });
    }
}