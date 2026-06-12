package com.techbleat.bank.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaProducerConfig {
    @Bean
    public NewTopic bankingTopic() {
        return new NewTopic("banking-transactions", 1, (short) 1);
    }
}
