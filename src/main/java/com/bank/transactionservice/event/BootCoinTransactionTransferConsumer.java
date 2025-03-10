package com.bank.transactionservice.event;

import com.bank.transactionservice.client.CustomerClientService;
import com.bank.transactionservice.dto.bootcoinpurchase.TransactionEvent;
import com.bank.transactionservice.dto.bootcoinpurchase.TransactionResponse;
import com.bank.transactionservice.model.customer.Customer;
import com.bank.transactionservice.model.transaction.ProductCategory;
import com.bank.transactionservice.model.transaction.Transaction;
import com.bank.transactionservice.model.transaction.TransactionType;
import com.bank.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootCoinTransactionTransferConsumer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TransactionService transactionService;
    private final CustomerClientService customerClientService;
    @KafkaListener(topics = "bootcoin.transaction.transfer.requested", groupId = "transaction-service-group")
    public void processTransferEvent(TransactionEvent event) {
        log.info("Received BootCoin transfer request: {}", event);
        customerClientService.getCustomerByDocumentNumber(event.getBuyerDocumentNumber())
            .map(Customer::getId)
            .flatMap(customerId -> {
                Transaction transaction = Transaction.builder()
                    .customerId(customerId)
                    .productId(event.getBuyerAccountNumber())
                    .productCategory(ProductCategory.ACCOUNT)
                    .transactionType(TransactionType.TRANSFER)
                    .amount(event.getTotalAmountInPEN())
                    .destinationAccountId(event.getSellerAccountNumber())
                    .transactionDate(LocalDateTime.now())
                    .build();
                return transactionService.createTransaction(transaction);
            })
            .subscribe(
                savedTransaction -> {
                    log.info("Transfer transaction completed for purchaseId: {}", event.getPurchaseId());
                    TransactionResponse response = TransactionResponse.builder()
                        .transactionId(event.getPurchaseId())
                        .success(true)
                        .message("Transaction successful")
                        .build();
                    kafkaTemplate.send("bootcoin.transaction.processed",
                        response);
                },
                error -> {
                    log.error("Transfer transaction failed for purchaseId {}: {}",
                        event.getPurchaseId(), error.getMessage());
                    TransactionResponse response = TransactionResponse.builder()
                        .transactionId(event.getPurchaseId())
                        .success(false)
                        .message(error.getMessage())
                        .build();
                    kafkaTemplate.send("bootcoin.transaction.processed", response);
                }
            );
    }
}
