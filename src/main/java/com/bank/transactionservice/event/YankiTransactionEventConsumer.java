package com.bank.transactionservice.event;

import com.bank.transactionservice.client.AccountClientService;
import com.bank.transactionservice.client.DebitCardClientService;
import com.bank.transactionservice.dto.YankiTransactionEvent;
import com.bank.transactionservice.dto.YankiTransactionProcessedEvent;
import com.bank.transactionservice.model.account.Account;
import com.bank.transactionservice.model.debitcard.DebitCard;
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

import java.time.Instant;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class YankiTransactionEventConsumer {
    private final TransactionService transactionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DebitCardClientService debitCardClientService;
    private final AccountClientService accountClientService;

    @KafkaListener(topics = "yanki.transaction.requested", groupId = "transaction-service-group")
    public void processYankiTransaction(YankiTransactionEvent event) {
        log.info("Received Yanki transaction with debit cards: {}", event);
        boolean senderHasCard = event.getSenderCard() != null;
        boolean receiverHasCard = event.getReceiverCard() != null;
        try {
            if (!senderHasCard && !receiverHasCard) {
                log.info("Both users are Yanki only. Transaction processed internally.");
                publishProcessedEvent(event, "SUCCESS", null);
                return;
            }
            if (senderHasCard && !receiverHasCard) {
                // Only the sender has a card → Debit bank account
                debitCardClientService.getDebitCardByCardNumber(event.getSenderCard())
                    .flatMap(debitCard ->
                        accountClientService.getAccountById(debitCard.getPrimaryAccountId())
                            .flatMap(account -> {
                                Transaction transaction = new Transaction(
                                    null,
                                    debitCard.getCustomerId(),
                                    debitCard.getPrimaryAccountId(),
                                    ProductCategory.ACCOUNT,
                                    ProductSubType.YANKI,
                                    TransactionType.WITHDRAWAL,
                                    event.getAmount(),
                                    LocalDateTime.now(),
                                    null,
                                    null,
                                    null,
                                    null
                                );
                                return transactionService.createTransaction(transaction);
                            })
                    )
                    .subscribe(
                        result -> {
                            log.info("Successful debit from sender's account: {}", result.getId());
                            publishProcessedEvent(event, "SUCCESS", null);
                        },
                        error -> {
                            log.error("Error debiting sender's account: {}", error.getMessage());
                                publishProcessedEvent(event,
                                    "FAILED",
                                    "Could not debit sender's" +
                                        " account: " + error.getMessage());
                        }
                    );
                return;
            }
            if (!senderHasCard && receiverHasCard) {
                //Only the receiver has a card → Yanki debits internally and credits receiver's bank account
                log.info("Internal debit in Yanki. " +
                    "Crediting receiver's account...");
                debitCardClientService.getDebitCardByCardNumber(
                    event.getReceiverCard())
                        .flatMap(debitCard ->
                                accountClientService.getAccountById(debitCard.getPrimaryAccountId())
                                        .flatMap(account -> {
                                            Transaction transaction = new Transaction(
                                                    null,
                                                    debitCard.getCustomerId(),
                                                    debitCard.getPrimaryAccountId(),
                                                    ProductCategory.ACCOUNT,
                                                    ProductSubType.YANKI,
                                                    TransactionType.DEPOSIT,
                                                    event.getAmount(),
                                                    LocalDateTime.now(),
                                                    null,
                                                    null,
                                                    null,
                                                    null
                                            );
                                            return transactionService.createTransaction(transaction);
                                        })
                        )
                        .subscribe(
                            result -> {
                                log.info("Successful credit to receiver's account: {}", result.getId());
                                publishProcessedEvent(event, "SUCCESS", null);
                            },
                            error -> {
                                log.error("Error crediting receiver's" +
                                    " account: {}", error.getMessage());
                                publishProcessedEvent(event, "FAILED",
                                    "Could not credit receiver's account: " + error.getMessage());
                            });
                return;
            }
            if (senderHasCard && receiverHasCard) {
                // Both have cards → Debit and credit in bank accounts
                Mono<DebitCard> senderCardMono = debitCardClientService
                    .getDebitCardByCardNumber(event.getSenderCard());
                Mono<DebitCard> receiverCardMono = debitCardClientService
                    .getDebitCardByCardNumber(event.getReceiverCard());
                Mono.zip(senderCardMono, receiverCardMono)
                        .flatMap(tuple -> {
                            DebitCard senderCard = tuple.getT1();
                            DebitCard receiverCard = tuple.getT2();
                            Mono<Account> senderAccountMono = accountClientService
                                .getAccountById(senderCard.getPrimaryAccountId());
                            Mono<Account> receiverAccountMono = accountClientService
                                .getAccountById(receiverCard.getPrimaryAccountId());
                            return Mono.zip(senderAccountMono, receiverAccountMono)
                                    .flatMap(accounts -> {
                                        Account senderAccount = accounts.getT1();
                                        Account receiverAccount = accounts.getT2();

                                        Transaction transfer = new Transaction(
                                                null,
                                                senderAccount.getCustomerId(),
                                                senderAccount.getId(),
                                                ProductCategory.ACCOUNT,
                                                ProductSubType.YANKI,
                                                TransactionType.TRANSFER,
                                                event.getAmount(),
                                                LocalDateTime.now(),
                                                receiverAccount.getId(),
                                                null,
                                                null,
                                                null
                                        );
                                        return transactionService.createTransaction(transfer);
                                    });
                        })
                        .subscribe(
                            result -> {
                                log.info("Successful transfer in bank accounts.");
                                publishProcessedEvent(event, "SUCCESS", null);
                            },
                            error -> {
                                log.error("Error in transfer of bank accounts: {}", error.getMessage());
                                publishProcessedEvent(event,
                                    "FAILED",
                                    "Error in transfer of bank " +
                                        "accounts: " + error.getMessage());
                            });
                return;
            }
        } catch (Exception e) {
            log.error("Error processing transaction: {}", e.getMessage());
            publishProcessedEvent(event, "FAILED", e.getMessage());
        }
    }
    private void publishProcessedEvent(YankiTransactionEvent event, String status, String reason) {
        YankiTransactionProcessedEvent processedEvent = new YankiTransactionProcessedEvent(
                event.getTransactionId(),
                event.getSenderPhoneNumber(),
                event.getReceiverPhoneNumber(),
                event.getAmount(),
                status,
                reason,
                Instant.now()
        );
        kafkaTemplate.send("yanki.transaction.processed", processedEvent);
        log.info("Sent transaction event processed: {}", processedEvent);
    }
}
