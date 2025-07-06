package com.example.kafka.service;

import com.example.kafka.model.Message;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/** Service for consuming messages from Kafka. */
@Slf4j
@Service
public class KafkaConsumerService {

  private final List<Message> receivedMessages = new CopyOnWriteArrayList<>();

  private final List<String> receivedTraceIdsFromHeader = new CopyOnWriteArrayList<>();

  /**
   * Kafka listener method that automatically invoked when messages arrive on the configured
   * receiver topic.
   *
   * @param message the received message payload
   * @param traceIdHeader the trace ID extracted from the message header
   */
  @KafkaListener(topics = "${kafka.topics.receiver}", groupId = "${spring.kafka.consumer.group-id}")
  public void listen(Message message, @Header("traceId") String traceIdHeader) {
    final String payload = message.getPayload();
    log.info(
        "Received message with traceId from header: {}, payload: '{}'", traceIdHeader, payload);
    receivedTraceIdsFromHeader.add(traceIdHeader);
    receivedMessages.add(message);
  }

  /**
   * Checks if a message with the specified trace ID and payload has been received.
   *
   * <p>This method searches through all received messages to find a match based on both the trace
   * ID and payload content.
   *
   * @param traceId the trace ID to search for
   * @param payload the payload content to search for
   * @return true if a matching message was found
   * @throws IllegalArgumentException if traceId or payload is null
   */
  public boolean hasMessageWithTraceId(String traceId, String payload) {
    Assert.notNull(traceId, "TraceId must not be null");
    Assert.notNull(payload, "Payload must not be null");

    return receivedMessages.stream()
        .anyMatch(
            msg ->
                msg != null
                    && traceId.equals(msg.getTraceId())
                    && payload.equals(msg.getPayload()));
  }

  /**
   * Checks if a trace ID has been received in message headers.
   *
   * <p>This method searches through all received trace IDs from message headers to find a match.
   *
   * @param traceId the trace ID to search for, must not be null
   * @return true if the trace ID was found in headers, false otherwise
   * @throws IllegalArgumentException if traceId is null
   */
  public boolean hasHeaderTraceId(String traceId) {
    Assert.notNull(traceId, "TraceId must not be null");

    return receivedTraceIdsFromHeader.contains(traceId);
  }

  /**
   * Returns the total number of messages received.
   *
   * @return the count of received messages
   */
  public int getReceivedMessageCount() {
    return receivedMessages.size();
  }

  /**
   * Returns the total number of trace IDs received in headers.
   *
   * @return the count of received trace IDs
   */
  public int getReceivedTraceIdCount() {
    return receivedTraceIdsFromHeader.size();
  }

  /** Clears all stored messages and trace IDs. Useful for cleaning up between tests. */
  public void clearReceivedData() {
    log.debug("Clearing all received messages and trace IDs");
    receivedMessages.clear();
    receivedTraceIdsFromHeader.clear();
  }
}
