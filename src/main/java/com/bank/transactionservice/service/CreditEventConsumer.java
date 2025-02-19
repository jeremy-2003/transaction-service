package com.bank.transactionservice.service;
import com.bank.transactionservice.model.Account;
import com.bank.transactionservice.model.Credit;
import com.bank.transactionservice.model.CreditCard;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CreditEventConsumer {
    private final TransactionCacheService transactionCacheService;
    public CreditEventConsumer(TransactionCacheService transactionCacheService) {
        this.transactionCacheService = transactionCacheService;
    }
    @KafkaListener(topics = "credit-created", groupId = "transaction-service-group")
    public void consumeCreditCreated(Credit credit) {
        try {
            transactionCacheService.saveCredit(credit.getId(), credit)
                    .doOnSuccess(unused -> log.info("Credit successfully saved in cache"))
                    .doOnError(error -> log.error("Error saving Credit in cache: {}", error.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error processing Credit event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "credit-updated", groupId = "transaction-service-group")
    public void consumeCreditUpdated(Credit credit) {
        try {
            transactionCacheService.saveCredit(credit.getId(), credit)
                    .doOnSuccess(unused -> log.info("Credit successfully saved in cache"))
                    .doOnError(error -> log.error("Error saving Credit in cache:  {}", error.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error deserializing Credit event: {}", e.getMessage());
        }
    }
}
