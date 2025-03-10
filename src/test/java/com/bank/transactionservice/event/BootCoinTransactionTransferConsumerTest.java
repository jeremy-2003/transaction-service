package com.bank.transactionservice.event;

import com.bank.transactionservice.client.CustomerClientService;
import com.bank.transactionservice.dto.bootcoinpurchase.TransactionEvent;
import com.bank.transactionservice.dto.bootcoinpurchase.TransactionResponse;
import com.bank.transactionservice.model.customer.Customer;
import com.bank.transactionservice.model.transaction.ProductCategory;
import com.bank.transactionservice.model.transaction.Transaction;
import com.bank.transactionservice.model.transaction.TransactionType;
import com.bank.transactionservice.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
@ExtendWith(MockitoExtension.class)
public class BootCoinTransactionTransferConsumerTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private TransactionService transactionService;
    @Mock
    private CustomerClientService customerClientService;
    @InjectMocks
    private BootCoinTransactionTransferConsumer consumer;
    @Captor
    private ArgumentCaptor<TransactionResponse> responseCaptor;
    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;
    private TransactionEvent transactionEvent;
    private Customer customer;
    private Transaction transaction;
    @BeforeEach
    void setUp() {
        transactionEvent = TransactionEvent.builder()
                .purchaseId("purchase-123")
                .buyerDocumentNumber("12345678")
                .sellerDocumentNumber("87654321")
                .amount(new BigDecimal("2.5"))
                .totalAmountInPEN(new BigDecimal("250"))
                .sellerAccountNumber("SELLER-ACC-001")
                .buyerAccountNumber("BUYER-ACC-001")
                .transactionType("TRANSFER")
                .build();
        customer = new Customer();
        customer.setId("customer-001");
        customer.setDocumentNumber("12345678");
        transaction = Transaction.builder()
                .id("tx-123")
                .customerId("customer-001")
                .productId("BUYER-ACC-001")
                .productCategory(ProductCategory.ACCOUNT)
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("250"))
                .destinationAccountId("SELLER-ACC-001")
                .transactionDate(LocalDateTime.now())
                .build();
    }
    @Test
    void processTransferEvent_Success() {
        when(customerClientService.getCustomerByDocumentNumber(transactionEvent.getBuyerDocumentNumber()))
                .thenReturn(Mono.just(customer));
        when(transactionService.createTransaction(any(Transaction.class)))
                .thenReturn(Mono.just(transaction));
        consumer.processTransferEvent(transactionEvent);
        verify(customerClientService).getCustomerByDocumentNumber(transactionEvent.getBuyerDocumentNumber());
        verify(transactionService).createTransaction(transactionCaptor.capture());
        verify(kafkaTemplate).send(eq("bootcoin.transaction.processed"), responseCaptor.capture());
        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals(customer.getId(), capturedTransaction.getCustomerId());
        assertEquals(transactionEvent.getBuyerAccountNumber(), capturedTransaction.getProductId());
        assertEquals(ProductCategory.ACCOUNT, capturedTransaction.getProductCategory());
        assertEquals(TransactionType.TRANSFER, capturedTransaction.getTransactionType());
        assertEquals(transactionEvent.getTotalAmountInPEN(), capturedTransaction.getAmount());
        assertEquals(transactionEvent.getSellerAccountNumber(), capturedTransaction.getDestinationAccountId());
        TransactionResponse response = responseCaptor.getValue();
        assertEquals(transactionEvent.getPurchaseId(), response.getTransactionId());
        assertTrue(response.isSuccess());
        assertEquals("Transaction successful", response.getMessage());
    }
    @Test
    void processTransferEvent_CustomerNotFound() {
        when(customerClientService.getCustomerByDocumentNumber(transactionEvent.getBuyerDocumentNumber()))
                .thenReturn(Mono.error(new RuntimeException("Customer not found")));
        consumer.processTransferEvent(transactionEvent);
        verify(customerClientService).getCustomerByDocumentNumber(transactionEvent.getBuyerDocumentNumber());
        verify(kafkaTemplate).send(eq("bootcoin.transaction.processed"), responseCaptor.capture());
        TransactionResponse response = responseCaptor.getValue();
        assertEquals(transactionEvent.getPurchaseId(), response.getTransactionId());
        assertFalse(response.isSuccess());
        assertEquals("Customer not found", response.getMessage());
    }
    @Test
    void processTransferEvent_TransactionCreationFails() {
        when(customerClientService.getCustomerByDocumentNumber(transactionEvent.getBuyerDocumentNumber()))
                .thenReturn(Mono.just(customer));
        when(transactionService.createTransaction(any(Transaction.class)))
                .thenReturn(Mono.error(new RuntimeException("Failed to create transaction")));
        consumer.processTransferEvent(transactionEvent);
        verify(customerClientService).getCustomerByDocumentNumber(transactionEvent.getBuyerDocumentNumber());
        verify(transactionService).createTransaction(any(Transaction.class));
        verify(kafkaTemplate).send(eq("bootcoin.transaction.processed"), responseCaptor.capture());
        TransactionResponse response = responseCaptor.getValue();
        assertEquals(transactionEvent.getPurchaseId(), response.getTransactionId());
        assertFalse(response.isSuccess());
        assertEquals("Failed to create transaction", response.getMessage());
    }
    @Test
    void processTransferEvent_CustomerServiceThrowsException() {
        String errorMessage = "Service unavailable";
        when(customerClientService.getCustomerByDocumentNumber(anyString()))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));
        consumer.processTransferEvent(transactionEvent);
        verify(customerClientService).getCustomerByDocumentNumber(transactionEvent.getBuyerDocumentNumber());
        verify(kafkaTemplate).send(eq("bootcoin.transaction.processed"), responseCaptor.capture());
        TransactionResponse response = responseCaptor.getValue();
        assertEquals(transactionEvent.getPurchaseId(), response.getTransactionId());
        assertFalse(response.isSuccess());
        assertEquals(errorMessage, response.getMessage());
    }
}