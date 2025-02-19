package com.bank.transactionservice.controller;

import com.bank.transactionservice.dto.BaseResponse;
import com.bank.transactionservice.model.Transaction.Transaction;
import com.bank.transactionservice.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;


@RestController
@RequestMapping("/api/transactions")
@Slf4j
public class TransactionController {
    private final TransactionService transactionService;
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
    @PostMapping
    public Mono<ResponseEntity<BaseResponse<Transaction>>> createTransaction(@RequestBody Transaction transaction) {
        return transactionService.createTransaction(transaction)
                .map(saved -> ResponseEntity.ok(BaseResponse.<Transaction>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Transaction created successfully")
                        .data(saved)
                        .build()))
                .onErrorResume(IllegalArgumentException.class, e -> Mono.just(
                        ResponseEntity.badRequest().body(BaseResponse.<Transaction>builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .message(e.getMessage())
                                .build())))
                .onErrorResume(Exception.class, e -> {
                    log.error("Error creating transaction", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BaseResponse.<Transaction>builder()
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .message("Error processing transaction")
                                    .build()));
                })
                .doOnSuccess(response -> log.info("Transaction processed with status: {}", response.getStatusCode()));
    }
    @GetMapping("/customer/{customerId}")
    public Mono<ResponseEntity<BaseResponse<List<Transaction>>>> getTransactionsByCustomerId(
            @PathVariable String customerId) {
        return transactionService.getTransactionsByCustomerId(customerId)
                .collectList()
                .map(transactions -> {
                    if (transactions.isEmpty()) {
                        return ResponseEntity.ok(BaseResponse.<List<Transaction>>builder()
                                .status(HttpStatus.NO_CONTENT.value())
                                .message("No transactions found for customer")
                                .data(Collections.emptyList())
                                .build());
                    }
                    return ResponseEntity.ok(BaseResponse.<List<Transaction>>builder()
                            .status(HttpStatus.OK.value())
                            .message("Transactions retrieved successfully")
                            .data(transactions)
                            .build());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error retrieving customer transactions", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BaseResponse.<List<Transaction>>builder()
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .message("Error retrieving transactions")
                                    .build()));
                })
                .doOnSuccess(response -> log.info("Retrieved transactions for customer: {} with status: {}",
                        customerId, response.getStatusCode()));
    }
    @GetMapping("/product/{productId}")
    public Mono<ResponseEntity<BaseResponse<List<Transaction>>>> getTransactionsByProductId(
            @PathVariable String productId) {
        return transactionService.getTransactionsByProductId(productId)
                .collectList()
                .map(transactions -> {
                    if (transactions.isEmpty()) {
                        return ResponseEntity.ok(BaseResponse.<List<Transaction>>builder()
                                .status(HttpStatus.NO_CONTENT.value())
                                .message("No transactions found for product")
                                .data(Collections.emptyList())
                                .build());
                    }
                    return ResponseEntity.ok(BaseResponse.<List<Transaction>>builder()
                            .status(HttpStatus.OK.value())
                            .message("Transactions retrieved successfully")
                            .data(transactions)
                            .build());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error retrieving product transactions", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BaseResponse.<List<Transaction>>builder()
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .message("Error retrieving transactions")
                                    .build()));
                })
                .doOnSuccess(response -> log.info("Retrieved transactions for product: {} with status: {}",
                        productId, response.getStatusCode()));
    }
    @GetMapping("customer/{customerId}/product/{productId}")
    public Mono<ResponseEntity<BaseResponse<List<Transaction>>>> getTransactionsByCustomerIdAndProductId(
            @PathVariable String customerId,
            @PathVariable String productId) {
                    return transactionService.getTransactionsByCustomerIdAndProductId(customerId, productId)
                            .collectList()
                            .map(transactions -> {
                                if (transactions.isEmpty()) {
                                    return ResponseEntity.ok(BaseResponse.<List<Transaction>>builder()
                                            .status(HttpStatus.NO_CONTENT.value())
                                            .message("No transactions found for product")
                                            .data(Collections.emptyList())
                                            .build());
                                }
                                return ResponseEntity.ok(BaseResponse.<List<Transaction>>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("Transactions retrieved successfully")
                                        .data(transactions)
                                        .build());
                            });
    }

    @GetMapping("/{transactionId}")
    public Mono<ResponseEntity<BaseResponse<Transaction>>> getTransactionById(
            @PathVariable String transactionId) {
        return transactionService.getTransactionById(transactionId)
                .map(transaction -> ResponseEntity.ok(BaseResponse.<Transaction>builder()
                        .status(HttpStatus.OK.value())
                        .message("Transaction retrieved successfully")
                        .data(transaction)
                        .build()))
                .defaultIfEmpty(ResponseEntity.ok(BaseResponse.<Transaction>builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .message("Transaction not found")
                        .build()))
                .onErrorResume(Exception.class, e -> {
                    log.error("Error retrieving transaction", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BaseResponse.<Transaction>builder()
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .message("Error retrieving transaction")
                                    .build()));
                })
                .doOnSuccess(response -> log.info("Retrieved transaction: {} with status: {}",
                        transactionId, response.getStatusCode()));
    }
}