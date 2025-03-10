package com.bank.transactionservice.event;

import com.bank.transactionservice.client.AccountClientService;
import com.bank.transactionservice.client.CustomerClientService;
import com.bank.transactionservice.dto.bootcoinbank.BootCoinBankPurchaseCompleted;
import com.bank.transactionservice.dto.bootcoinbank.BootCoinBankPurchaseRequested;
import com.bank.transactionservice.model.transaction.ProductCategory;
import com.bank.transactionservice.model.transaction.ProductSubType;
import com.bank.transactionservice.model.transaction.Transaction;
import com.bank.transactionservice.model.transaction.TransactionType;
import com.bank.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
@Slf4j
public class BootCoinBankPurchase {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TransactionService transactionService;
    private final AccountClientService accountClientService;
    private final CustomerClientService customerClientService;
    @KafkaListener(topics = "bootcoin.bank.purchase.requested", groupId = "transaction-service-group")
    public void processBootCoinPurchase(BootCoinBankPurchaseRequested event) {
        log.info("Received BootCoinBankPurchaseRequested event: {}", event);
        customerClientService.getCustomerByDocumentNumber(event.getBuyerDocumentNumber())
                .flatMap(customer ->
                        accountClientService.getAccountById(event.getBuyerAccountNumber())
                                .flatMap(account -> {
                                    if (account.getCustomerId().equals(customer.getId())) {
                                        Transaction transaction = new Transaction(
                                                null,
                                                event.getBuyerDocumentNumber(),
                                                event.getBuyerAccountNumber(),
                                                ProductCategory.ACCOUNT,
                                                ProductSubType.BOOT_COIN,
                                                TransactionType.WITHDRAWAL,
                                                event.getTotalAmountInPEN(),
                                                LocalDateTime.now(),
                                                null,
                                                null,
                                                null,
                                                null
                                        );
                                        return transactionService.createTransaction(transaction)
                                                .map(createdTransaction -> {
                                                    BootCoinBankPurchaseCompleted resultEvent =
                                                        BootCoinBankPurchaseCompleted.builder()
                                                            .transactionId(event.getTransactionId())
                                                            .accepted(true)
                                                            .build();
                                                    kafkaTemplate.send("bootcoin.bank.purchase.procesed",
                                                        resultEvent)
                                                        .addCallback(
                                                            result ->
                                                                log.info("Event sent successfully to topic: {}",
                                                                result.getRecordMetadata().topic()),
                                                            ex -> log.error("Failed to send event: {}",
                                                                ex.getMessage(), ex)
                                                        );
                                                    return createdTransaction;
                                                });
                                    } else {
                                        log.error("Account {} does not belong to customer {}",
                                            event.getBuyerAccountNumber(), customer.getId());
                                        BootCoinBankPurchaseCompleted resultEvent =
                                            BootCoinBankPurchaseCompleted.builder()
                                                .transactionId(event.getTransactionId())
                                                .accepted(false)
                                                .build();
                                        kafkaTemplate.send("bootcoin.bank.purchase.procesed", resultEvent);
                                        return Mono.empty();
                                    }
                                })
                )
                .onErrorResume(error -> {
                    log.error("Error processing BootCoin purchase: {}", error.getMessage());
                    BootCoinBankPurchaseCompleted resultEvent = BootCoinBankPurchaseCompleted.builder()
                            .transactionId(event.getTransactionId())
                            .accepted(false)
                            .build();
                    kafkaTemplate.send("bootcoin.bank.purchase.procesed", resultEvent);
                    return Mono.empty();
                })
                .subscribe();
    }
}
