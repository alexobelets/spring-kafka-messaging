package com.example.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Kafka Mock Spring Boot application.
 *
 * <p>This application demonstrates Kafka messaging capabilities using Spring Boot and Spring Kafka.
 * It provides producer, consumer, and forwarding services for testing Kafka message flow in
 * automation scenarios.
 */
@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
