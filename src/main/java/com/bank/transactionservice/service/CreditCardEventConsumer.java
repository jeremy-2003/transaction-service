package com.bank.transactionservice.service;
import com.bank.transactionservice.model.Account;
import com.bank.transactionservice.model.Credit;
import com.bank.transactionservice.model.CreditCard;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CreditCardEventConsumer {
    private final TransactionCacheService transactionCacheService;
    public CreditCardEventConsumer(TransactionCacheService transactionCacheService) {
        this.transactionCacheService = transactionCacheService;
    }
    @KafkaListener(topics = "creditcard-created", groupId = "transaction-service-group")
    public void consumeCreditCardCreated(String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            CreditCard creditCard = objectMapper.readValue(message, CreditCard.class);
            transactionCacheService.saveCreditCard(creditCard.getId(), creditCard)
                    .doOnSuccess(unused -> log.info("CreditCard successfully saved in cache"))
                    .doOnError(error -> log.error("Error saving CreditCard in cache: {}", error.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error deserializing CreditCard event: {}", e.getMessage());
        }
    }
    @KafkaListener(topics = "creditcard-updated", groupId = "transaction-service-group")
    public void consumeCreditCardUpdated(CreditCard creditCard) {
        try {
            transactionCacheService.saveCreditCard(creditCard.getId(), creditCard)
                    .doOnSuccess(unused -> log.info("CreditCard successfully saved in cache"))
                    .doOnError(error -> log.error("Error saving CreditCard in cache: {}", error.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error processing CreditCard event: {}", e.getMessage());
        }
    }

}
