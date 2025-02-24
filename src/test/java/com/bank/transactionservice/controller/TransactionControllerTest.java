package com.bank.transactionservice.controller;

import com.bank.transactionservice.model.transaction.ProductCategory;
import com.bank.transactionservice.model.transaction.ProductSubType;
import com.bank.transactionservice.model.transaction.Transaction;
import com.bank.transactionservice.model.transaction.TransactionType;
import com.bank.transactionservice.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {
    @Mock
    private TransactionService transactionService;
    @InjectMocks
    private TransactionController transactionController;
    private Transaction mockTransaction;
    private List<Transaction> mockTransactionList;
    @BeforeEach
    void setUp() {
        mockTransaction = new Transaction();
        mockTransaction.setId("1");
        mockTransaction.setCustomerId("customer1");
        mockTransaction.setProductId("product1");
        mockTransaction.setProductCategory(ProductCategory.ACCOUNT);
        mockTransaction.setProductSubType(ProductSubType.SAVINGS);
        mockTransaction.setTransactionType(TransactionType.DEPOSIT);
        mockTransaction.setAmount(new BigDecimal("100.00"));
        mockTransaction.setTransactionDate(LocalDateTime.now());
        mockTransactionList = Arrays.asList(mockTransaction);
    }
    @Test
    void createTransaction_Success() {
        when(transactionService.createTransaction(any(Transaction.class)))
                .thenReturn(Mono.just(mockTransaction));
        StepVerifier.create(transactionController.createTransaction(mockTransaction))
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED.value(), response.getBody().getStatus());
                    assertEquals("Transaction created successfully", response.getBody().getMessage());
                    assertEquals(mockTransaction, response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void createTransaction_ValidationError() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid amount");
        when(transactionService.createTransaction(any(Transaction.class)))
                .thenReturn(Mono.error(exception));
        StepVerifier.create(transactionController.createTransaction(mockTransaction))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    assertEquals("Invalid amount", response.getBody().getMessage());
                    assertNull(response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void createTransaction_InternalError() {
        when(transactionService.createTransaction(any(Transaction.class)))
                .thenReturn(Mono.error(new RuntimeException("Database error")));
        StepVerifier.create(transactionController.createTransaction(mockTransaction))
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
                    assertEquals("Error processing transaction", response.getBody().getMessage());
                    assertNull(response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionsByCustomerId_Success() {
        String customerId = "customer1";
        when(transactionService.getTransactionsByCustomerId(customerId))
                .thenReturn(Flux.fromIterable(mockTransactionList));
        StepVerifier.create(transactionController.getTransactionsByCustomerId(customerId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK.value(), response.getBody().getStatus());
                    assertEquals("Transactions retrieved successfully", response.getBody().getMessage());
                    assertEquals(mockTransactionList, response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionsByCustomerId_NoContent() {
        String customerId = "customer1";
        when(transactionService.getTransactionsByCustomerId(customerId))
                .thenReturn(Flux.empty());
        StepVerifier.create(transactionController.getTransactionsByCustomerId(customerId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.NO_CONTENT.value(), response.getBody().getStatus());
                    assertEquals("No transactions found for customer", response.getBody().getMessage());
                    assertEquals(Collections.emptyList(), response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionsByProductId_Success() {
        String productId = "product1";
        when(transactionService.getTransactionsByProductId(productId))
                .thenReturn(Flux.fromIterable(mockTransactionList));
        StepVerifier.create(transactionController.getTransactionsByProductId(productId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK.value(), response.getBody().getStatus());
                    assertEquals("Transactions retrieved successfully", response.getBody().getMessage());
                    assertEquals(mockTransactionList, response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionById_Success() {
        String transactionId = "1";
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Mono.just(mockTransaction));
        StepVerifier.create(transactionController.getTransactionById(transactionId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK.value(), response.getBody().getStatus());
                    assertEquals("Transaction retrieved successfully", response.getBody().getMessage());
                    assertEquals(mockTransaction, response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionById_NotFound() {
        String transactionId = "nonexistent";
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Mono.empty());
        StepVerifier.create(transactionController.getTransactionById(transactionId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
                    assertEquals("Transaction not found", response.getBody().getMessage());
                    assertNull(response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionsByCustomerIdAndProductId_Success() {
        String customerId = "customer1";
        String productId = "product1";
        when(transactionService.getTransactionsByCustomerIdAndProductId(customerId, productId))
                .thenReturn(Flux.fromIterable(mockTransactionList));
        StepVerifier.create(transactionController.getTransactionsByCustomerIdAndProductId(customerId, productId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK.value(), response.getBody().getStatus());
                    assertEquals("Transactions retrieved successfully", response.getBody().getMessage());
                    assertEquals(mockTransactionList, response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionsByDate_Success() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        when(transactionService.getTrasactionsByDate(startDate, endDate))
                .thenReturn(Flux.fromIterable(mockTransactionList));
        StepVerifier.create(transactionController.getTransactionsByDate(startDate, endDate))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK.value(), response.getBody().getStatus());
                    assertEquals("Transactions retrieved successfully", response.getBody().getMessage());
                    assertEquals(mockTransactionList, response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionsByDate_NoContent() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        when(transactionService.getTrasactionsByDate(startDate, endDate))
                .thenReturn(Flux.empty());
        StepVerifier.create(transactionController.getTransactionsByDate(startDate, endDate))
                .assertNext(response -> {
                    assertEquals(HttpStatus.NO_CONTENT.value(), response.getBody().getStatus());
                    assertEquals("No transactions found for product", response.getBody().getMessage());
                    assertEquals(Collections.emptyList(), response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionsByCustomerId_InternalError() {
        String customerId = "customer1";
        when(transactionService.getTransactionsByCustomerId(customerId))
                .thenReturn(Flux.error(new RuntimeException("Database error")));
        StepVerifier.create(transactionController.getTransactionsByCustomerId(customerId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
                    assertEquals("Error retrieving transactions", response.getBody().getMessage());
                    assertNull(response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionsByProductId_NoContent() {
        String productId = "product1";
        when(transactionService.getTransactionsByProductId(productId))
                .thenReturn(Flux.empty());
        StepVerifier.create(transactionController.getTransactionsByProductId(productId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.NO_CONTENT.value(), response.getBody().getStatus());
                    assertEquals("No transactions found for product", response.getBody().getMessage());
                    assertEquals(Collections.emptyList(), response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionsByProductId_InternalError() {
        String productId = "product1";
        when(transactionService.getTransactionsByProductId(productId))
                .thenReturn(Flux.error(new RuntimeException("Database error")));
        StepVerifier.create(transactionController.getTransactionsByProductId(productId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
                    assertEquals("Error retrieving transactions", response.getBody().getMessage());
                    assertNull(response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionById_InternalError() {
        String transactionId = "1";
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Mono.error(new RuntimeException("Database error")));
        StepVerifier.create(transactionController.getTransactionById(transactionId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
                    assertEquals("Error retrieving transaction", response.getBody().getMessage());
                    assertNull(response.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getTransactionsByCustomerIdAndProductId_NoContent() {
        String customerId = "customer1";
        String productId = "product1";
        when(transactionService.getTransactionsByCustomerIdAndProductId(customerId, productId))
                .thenReturn(Flux.empty());
        StepVerifier.create(transactionController.getTransactionsByCustomerIdAndProductId(customerId, productId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.NO_CONTENT.value(), response.getBody().getStatus());
                    assertEquals("No transactions found for product", response.getBody().getMessage());
                    assertEquals(Collections.emptyList(), response.getBody().getData());
                })
                .verifyComplete();
    }
}
