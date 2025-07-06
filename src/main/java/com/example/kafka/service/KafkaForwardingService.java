package com.example.kafka.service;

import com.example.kafka.model.Message;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Service for forwarding messages between Kafka topics.
 *
 * <p>This service listens to the sender topic and automatically forwards received messages to the
 * receiver topic. It preserves trace ID headers during the forwarding process to maintain message
 * correlation across topics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaForwardingService {

  private static final String TRACE_ID_HEADER = "traceId";

  private final KafkaTemplate<String, Message> kafkaTemplate;

  @Value("${kafka.topics.receiver}")
  private String receiverTopic;

  /**
   * Kafka listener that forwards messages from sender to receiver topic.
   *
   * <p>This method is automatically invoked when messages arrive on the sender topic. It extracts
   * the message payload and trace ID header, then forwards the complete message to the receiver
   * topic while preserving the correlation information.
   *
   * @param consumerRecord the consumed message record containing payload and headers
   */
  @KafkaListener(topics = "${kafka.topics.sender}", groupId = "${spring.kafka.consumer.group-id}")
  public void forward(ConsumerRecord<String, Message> consumerRecord) {
    try {
      Message message = consumerRecord.value();
      String traceId = extractTraceId(consumerRecord);
      String payload = message.getPayload();
      log.info(
          "Forwarding message with traceId='{}' and payload='{}' from sender to receiver topic",
          traceId,
          payload);

      kafkaTemplate
          .send(
              MessageBuilder.withPayload(message)
                  .setHeader(KafkaHeaders.TOPIC, receiverTopic)
                  .setHeader(TRACE_ID_HEADER, traceId)
                  .build())
          .whenComplete(
              (result, throwable) -> {
                if (throwable != null) {
                  log.error(
                      "Failed to forward message with traceId='{}' to receiver topic",
                      traceId,
                      throwable);
                } else {
                  log.debug(
                      "Successfully forwarded message with traceId='{}' to receiver topic at offset: {}",
                      traceId,
                      result.getRecordMetadata().offset());
                }
              });

    } catch (Exception e) {
      log.error("Unexpected error during message forwarding", e);
    }
  }

  /**
   * Extracts the trace ID from the message record headers.
   *
   * @param consumerRecord the consumer record containing the headers
   * @return the trace ID as a string, or null if not found or malformed
   */
  private String extractTraceId(ConsumerRecord<String, Message> consumerRecord) {
    try {
      var traceIdHeader = consumerRecord.headers().lastHeader(TRACE_ID_HEADER);
      if (traceIdHeader != null && traceIdHeader.value() != null) {
        return new String(traceIdHeader.value(), StandardCharsets.UTF_8);
      }
    } catch (Exception e) {
      log.warn("Failed to extract trace ID from message headers", e);
    }
    return null;
  }
}
