package com.example.kafka.service;

import com.example.kafka.model.Message;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/** Service for producing Kafka messages. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

  private static final String TRACE_ID_HEADER = "traceId";

  private final KafkaTemplate<String, Message> kafkaTemplate;

  /**
   * Sends a message to the specified Kafka topic.
   *
   * <p>The message is sent with a custom "traceId" header that matches the traceId field in the
   * message payload. This allows for proper message correlation across the system.
   *
   * @param topic the Kafka topic to send the message to
   * @param message the message to send, must not be null
   * @throws IllegalArgumentException if topic or message is null/empty
   */
  public void send(String topic, Message message) {
    Assert.hasText(topic, "Topic must not be null or empty");
    log.debug("Sending message to topic '{}' with traceId: {}", topic, message.getTraceId());

    CompletableFuture<SendResult<String, Message>> future =
        kafkaTemplate.send(
            MessageBuilder.withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(TRACE_ID_HEADER, message.getTraceId())
                .build());

    future.whenComplete(
        (result, throwable) -> {
          if (throwable != null) {
            log.error(
                "Failed to send message to topic '{}' with traceId: {}",
                topic,
                message.getTraceId(),
                throwable);
          } else {
            log.debug(
                "Successfully sent message to topic '{}' with traceId: {} at offset: {}",
                topic,
                message.getTraceId(),
                result.getRecordMetadata().offset());
          }
        });
  }
}
