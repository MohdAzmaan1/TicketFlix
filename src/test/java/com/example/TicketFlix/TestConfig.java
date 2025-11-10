package com.example.TicketFlix;

import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Test configuration to mock external dependencies
 */
@TestConfiguration
public class TestConfig {

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private JavaMailSender javaMailSender;
}