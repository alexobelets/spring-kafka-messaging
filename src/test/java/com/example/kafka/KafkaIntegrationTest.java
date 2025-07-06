package com.example.kafka;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.kafka.model.Message;
import com.example.kafka.service.KafkaConsumerService;
import com.example.kafka.service.KafkaProducerService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/** Integration tests for Kafka message processing. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka
public class KafkaIntegrationTest {

  @Autowired private KafkaProducerService producerService;

  @Autowired private KafkaTemplate<String, Message> kafkaTemplate;

  @Autowired private KafkaConsumerService consumerService;

  @Value("${kafka.topics.sender}")
  private String senderTopic;

  @BeforeEach
  void setUp() {
    consumerService.clearReceivedData();
  }

  @Test
  void testMessageIsForwardedFromSenderToReceiverWithHeader() {
    // Prepare test data
    final String traceId = UUID.randomUUID().toString();
    final String payload = "Test Payload";
    Message message = Message.builder().traceId(traceId).payload(payload).build();

    // Act
    producerService.send(senderTopic, message);

    // Verify
    await()
        .atMost(10, SECONDS)
        .untilAsserted(
            () -> {
              assertThat(consumerService.hasMessageWithTraceId(traceId, payload))
                  .as(
                      "Message with traceId '%s' and payload '%s' should be received",
                      traceId, payload)
                  .isTrue();
              assertThat(consumerService.hasHeaderTraceId(traceId))
                  .as("Trace ID '%s' should be present in message headers", traceId)
                  .isTrue();
            });
  }

  @Test
  void testNullMessageHandling() {
    // Act
    kafkaTemplate.send(senderTopic, null);

    // Verify
    await()
        .pollDelay(2, SECONDS)
        .atMost(5, SECONDS)
        .untilAsserted(
            () ->
                assertThat(consumerService.getReceivedMessageCount())
                    .as("No messages should be received when null message is sent")
                    .isEqualTo(0));
  }
}
