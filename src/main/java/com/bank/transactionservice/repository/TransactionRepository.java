package com.bank.transactionservice.repository;

import com.bank.transactionservice.model.Transaction.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {
    Flux<Transaction> findByCustomerId(String customerId);
    Flux<Transaction> findByProductId(String productId);
    Flux<Transaction> findByCustomerIdAndProductId(String customerId, String productId);
}
