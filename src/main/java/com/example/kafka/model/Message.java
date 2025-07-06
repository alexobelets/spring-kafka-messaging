package com.example.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Represents a Kafka message.
 *
 * <p>This class encapsulates the structure of messages transmitted through Kafka topics. Each
 * message contains a unique trace ID and a payload containing the actual data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {
  /**
   * Unique identifier for tracing the message through the system. Used for correlating messages
   * across different services and topics.
   */
  @NonNull private String traceId;

  /**
   * The actual message content/data being transmitted. Can contain any string-based payload data.
   */
  @NonNull private String payload;
}
