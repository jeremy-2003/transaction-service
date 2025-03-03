package com.bank.transactionservice.service;

import com.bank.transactionservice.client.AccountClientService;
import com.bank.transactionservice.client.CreditClientService;
import com.bank.transactionservice.client.DebitCardClientService;
import com.bank.transactionservice.model.account.Account;
import com.bank.transactionservice.model.credit.Credit;
import com.bank.transactionservice.model.credit.CreditStatus;
import com.bank.transactionservice.model.creditcard.PaymentStatus;
import com.bank.transactionservice.model.debitcard.DebitCard;
import com.bank.transactionservice.model.transaction.Transaction;
import com.bank.transactionservice.model.transaction.TransactionType;
import com.bank.transactionservice.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionCacheService transactionCacheService;
    private final AccountClientService accountClientService;
    private final CreditClientService creditClientService;
    private  final DebitCardClientService debitCardClientService;
    public TransactionService(TransactionRepository transactionRepository,
                              TransactionCacheService transactionCacheService,
                              AccountClientService accountClientService,
                              CreditClientService creditClientService,
                              DebitCardClientService debitCardClientService) {
        this.transactionRepository = transactionRepository;
        this.transactionCacheService = transactionCacheService;
        this.accountClientService = accountClientService;
        this.creditClientService = creditClientService;
        this.debitCardClientService = debitCardClientService;
    }
    public Mono<Transaction> createTransaction(Transaction transaction) {
        return validateAndProcessTransaction(transaction)
                .flatMap(result -> {
                    result.setTransactionDate(LocalDateTime.now());
                    return transactionRepository.save(result);
                })
                .doOnSuccess(saved -> log.info("Transaction created successfully: {}", saved.getId()))
                .doOnError(error -> log.error("Error creating transaction: {}", error.getMessage()));
    }
    private Mono<Transaction> validateAndProcessTransaction(Transaction transaction) {
        switch (transaction.getProductCategory()) {
            case ACCOUNT:
                return processAccountTransaction(transaction);
            case CREDIT:
                return processCreditTransaction(transaction);
            case CREDIT_CARD:
                return processCreditCardTransaction(transaction);
            case DEBIT_CARD:
                return processDebitCardTransaction(transaction);
            default:
                return Mono.error(new IllegalArgumentException("Invalid product category"));
        }
    }
    private Mono<Transaction> processDebitCardTransaction(Transaction transaction) {
        String debitCardId = transaction.getProductId();

        return debitCardClientService.getDebitCardById(debitCardId)
                .flatMap( debitCard -> {
                    if (!"ACTIVE".equals(debitCard.getStatus())) {
                        return Mono.error(new IllegalArgumentException("The debit card is not active"));
                    }

                    transaction.setCustomerId(debitCard.getCustomerId());

                    switch (transaction.getTransactionType()) {
                        case DEBIT_CARD_PAYMENT:
                        case DEBIT_CARD_WITHDRAWAL:
                            return processDebitCardPaymentOrWithdrawal(transaction, debitCard);
                        default:
                            return Mono.error(new IllegalArgumentException("Invalid transaction type for debit card"));
                    }
                });
    }
    private Mono<Transaction> processDebitCardPaymentOrWithdrawal(Transaction transaction, DebitCard debitCard) {
        String primaryAccountId = debitCard.getPrimaryAccountId();
        List<String> accountsToTry = new ArrayList<>(debitCard.getAssociatedAccountIds());

        accountsToTry.remove(primaryAccountId);
        accountsToTry.add(0, primaryAccountId);

        BigDecimal amountToProcess = transaction.getAmount();

        return processWithAvailableAccount(transaction, accountsToTry, 0, amountToProcess);
    }
    private Mono<Transaction> processWithAvailableAccount(Transaction transaction,
                                                          List<String> accountIds,
                                                          int currentIndex,
                                                          BigDecimal amount) {
        if (currentIndex >= accountIds.size()) {
            return Mono.error(new IllegalArgumentException("Insufficient balance in all associated accounts"));
        }
        String currentAccountId = accountIds.get(currentIndex);
        return accountClientService.getAccountById(currentAccountId)
                .flatMap( currentAccount -> {
                    BigDecimal accountBalance = BigDecimal.valueOf(currentAccount.getBalance());

                    if (accountBalance.compareTo(amount) >= 0) {
                        BigDecimal newBalance = accountBalance.subtract(amount);

                        return accountClientService.updateAccountBalance(currentAccountId, newBalance)
                                .then(Mono.defer(() -> {
                                    transaction.setSourceAccountId(currentAccountId);
                                    return Mono.just(transaction);
                                }));
                    } else {
                        return processWithAvailableAccount(transaction, accountIds, currentIndex + 1, amount);
                    }
                })
                .onErrorResume( e -> {
                    log.error("Error processing with account {}: {}", currentAccountId, e.getMessage());
                    return processWithAvailableAccount(transaction, accountIds, currentIndex + 1, amount);
                });
    }
    public Mono<Transaction> processAccountTransaction(Transaction transaction) {
        return transactionCacheService.getAccount(transaction.getProductId())
                .switchIfEmpty(accountClientService.getAccountById(transaction.getProductId())
                        .flatMap(account -> transactionCacheService.saveAccount(transaction.getProductId(), account)
                                .thenReturn(account)))
                .flatMap(account -> transactionRepository.findByProductId(transaction.getProductId())
                        .filter(e -> e.getTransactionType().equals(TransactionType.WITHDRAWAL) ||
                                e.getTransactionType().equals(TransactionType.DEPOSIT))
                        .count()
                        .flatMap(transactionCount -> {
                            BigDecimal newBalance = calculateNewBalance(account.getBalance(), transaction);
                            if (transactionCount >= account.getMaxFreeTransaction()
                                    && (transaction.getTransactionType() == TransactionType.WITHDRAWAL
                                    || transaction.getTransactionType() == TransactionType.DEPOSIT)) {
                                newBalance = newBalance.add(account.getTransactionCost());
                                transaction.setAmount(transaction.getAmount().add(account.getTransactionCost()));
                                transaction.setCommissions(account.getTransactionCost());
                            }

                            Mono<Account> updateAccountBalanceMono = accountClientService
                                    .updateAccountBalance(transaction.getProductId(), newBalance);

                            if (transaction.getTransactionType() == TransactionType.TRANSFER) {
                                if (transaction.getDestinationAccountId() == null) {
                                    return Mono.error(new IllegalArgumentException("A destination account " +
                                            "is required for a transfer"));
                                } else {
                                    updateAccountBalanceMono = updateAccountBalanceMono.then(
                                            accountClientService.getAccountById(transaction.getDestinationAccountId())
                                                    .flatMap(destinationAccount -> {
                                                        BigDecimal destinationNewBalance = BigDecimal
                                                                .valueOf(destinationAccount.getBalance())
                                                                .add(transaction.getAmount());
                                                        return accountClientService.updateAccountBalance(transaction
                                                                .getDestinationAccountId(),
                                                                destinationNewBalance);
                                                    })
                                    );
                                }
                            }

                            return updateAccountBalanceMono.thenReturn(transaction);
                        }));
    }
    private Mono<Transaction> processCreditTransaction(Transaction transaction) {
        return transactionCacheService.getCredit(transaction.getProductId())
                .switchIfEmpty(creditClientService.getCreditById(transaction.getProductId())
                        .flatMap(credit -> transactionCacheService.saveCredit(transaction.getProductId(), credit)
                                .thenReturn(credit)))
                .flatMap(credit -> {
                    if (transaction.getTransactionType() == TransactionType.CREDIT_PAYMENT) {
                        BigDecimal newBalance = calculateNewCreditBalance(credit.getRemainingBalance(), transaction);
                        Credit updatedCredit = credit;

                        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
                            updatedCredit.setCreditStatus(CreditStatus.FINISHED);
                            updatedCredit.setPaymentStatus(PaymentStatus.FINISHED);
                        } else {
                            if (transaction.getAmount().compareTo(credit.getMinimumPayment()) >= 0) {
                                updatedCredit.setPaymentStatus(PaymentStatus.PAID);
                                updatedCredit.setNextPaymentDate(credit.getNextPaymentDate().plusDays(30));

                                BigDecimal newMinimumPayment = newBalance.multiply(new BigDecimal("0.10"));
                                updatedCredit.setMinimumPayment(newMinimumPayment);
                            } else {
                                updatedCredit.setPaymentStatus(PaymentStatus.PENDING);
                            }
                        }
                        updatedCredit.setRemainingBalance(newBalance);
                        updatedCredit.setModifiedAt(LocalDateTime.now());
                        return creditClientService
                                .updateCredit(updatedCredit)
                                .thenReturn(transaction);
                    }
                    return null;
                });
    }
    private Mono<Transaction> processCreditCardTransaction(Transaction transaction) {
        return transactionCacheService.getCreditCard(transaction.getProductId())
                .switchIfEmpty(creditClientService.getCreditCardById(transaction.getProductId())
                        .flatMap(creditCard ->
                                transactionCacheService
                                        .saveCreditCard(transaction.getProductId(), creditCard)
                                .thenReturn(creditCard)))
                .flatMap(creditCard -> {
                    BigDecimal newBalance = calculateNewCreditCardBalance(creditCard.getAvailableBalance(),
                            transaction);
                    return creditClientService.updateCreditCardBalance(transaction.getProductId(), newBalance)
                            .thenReturn(transaction);
                });
    }
    private BigDecimal calculateNewBalance(Double currentBalance, Transaction transaction) {
        BigDecimal balance = BigDecimal.valueOf(currentBalance);
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transaction amount cannot be negative");
        }
        switch (transaction.getTransactionType()) {
            case DEPOSIT:
                return balance.add(transaction.getAmount());
            case WITHDRAWAL:
                if (transaction.getAmount().compareTo(balance) > 0) {
                    throw new IllegalArgumentException("Insufficient balance for withdrawal");
                }
                return balance.subtract(transaction.getAmount());
            case TRANSFER:
                if (transaction.getAmount().compareTo(balance) > 0) {
                    throw new IllegalArgumentException("Insufficient balance for transfer");
                }
                return balance.subtract(transaction.getAmount());
            default:
                throw new IllegalArgumentException("Invalid transaction type for account");
        }
    }

    private BigDecimal calculateNewCreditBalance(BigDecimal currentBalance, Transaction transaction) {
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transaction amount cannot be negative");
        }
        switch (transaction.getTransactionType()) {
            case CREDIT_PAYMENT:
                return currentBalance.subtract(transaction.getAmount());
            default:
                throw new IllegalArgumentException("Invalid transaction type for credit");
        }
    }

    private BigDecimal calculateNewCreditCardBalance(BigDecimal currentBalance, Transaction transaction) {
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transaction amount cannot be negative");
        }
        switch (transaction.getTransactionType()) {
            case CREDIT_CARD_PURCHASE:
                if (transaction.getAmount().compareTo(currentBalance) > 0) {
                    throw new IllegalArgumentException("Insufficient balance for purchase");
                }
                return currentBalance.subtract(transaction.getAmount());
            case CREDIT_PAYMENT:
                return currentBalance.add(transaction.getAmount());
            default:
                throw new IllegalArgumentException("Invalid transaction type for credit card");
        }
    }
    public Flux<Transaction> getTransactionsByCustomerId(String customerId) {
        return transactionRepository.findByCustomerId(customerId)
                .doOnComplete(() -> log.info("Retrieved transactions for customer: {}", customerId))
                .doOnError(error -> log.error("Error retrieving transactions for customer {}: {}",
                        customerId, error.getMessage()));
    }
    public Flux<Transaction> getTransactionsByProductId(String productId) {
        return transactionRepository.findByProductId(productId)
                .doOnComplete(() -> log.info("Retrieved transactions for product: {}", productId))
                .doOnError(error -> log.error("Error retrieving transactions for product {}: {}",
                        productId, error.getMessage()));
    }
    public Mono<Transaction> getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId)
                .switchIfEmpty(Mono.error(new RuntimeException("This transaction doesn exist")));
    }
    public Flux<Transaction> getTransactionsByCustomerIdAndProductId(String customerId, String productId) {
        return validateOwnership(customerId, productId)
                .thenMany(transactionRepository.findByCustomerIdAndProductId(customerId, productId))
                .doOnComplete(() -> log.info("Retrieved " +
                        "transactions for product: {}", productId))
                .doOnError(e -> log.error("Error retrieving " +
                        "transactions for product: {}: {}", productId, e.getMessage()));
    }
    public Mono<Boolean> validateOwnership(String customerId, String id) {
        return validateAccountOwnership(customerId, id)
                .switchIfEmpty(validateCreditOwnership(customerId, id))
                .switchIfEmpty(validateCreditCardOwnership(customerId, id))
                .onErrorResume(e -> Mono.just(false))
                .defaultIfEmpty(false);
    }

    private Mono<Boolean> validateAccountOwnership(String customerId, String accountId) {
        return transactionCacheService.getAccount(accountId)
                .flatMap(account -> {
                    if (account.getCustomerId().equals(customerId)) {
                        return Mono.just(true);
                    }
                    return Mono.error(new IllegalArgumentException("Account does not belong to customer"));
                })
                .switchIfEmpty(
                        accountClientService.getAccountById(accountId)
                                .flatMap(account -> {
                                    if (account.getCustomerId().equals(customerId)) {
                                        return transactionCacheService.saveAccount(accountId, account)
                                                .thenReturn(true);
                                    }
                                    return Mono.error(new IllegalArgumentException("Account does not " +
                                            "belong to customer"));
                                })
                                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                                    return Mono.error(new IllegalArgumentException("Account not found"));
                                })
                );
    }

    private Mono<Boolean> validateCreditOwnership(String customerId, String creditId) {
        return transactionCacheService.getCredit(creditId)
                .flatMap(credit -> {
                    if (credit.getCustomerId().equals(customerId)) {
                        return Mono.just(true);
                    }
                    return Mono.error(new IllegalArgumentException("Credit does not belong to customer"));
                })
                .switchIfEmpty(
                        creditClientService.getCreditById(creditId)
                                .flatMap(credit -> {
                                    if (credit.getCustomerId().equals(customerId)) {
                                        return transactionCacheService.saveCredit(creditId, credit)
                                                .thenReturn(true);
                                    }
                                    return Mono.error(new IllegalArgumentException("Credit does not " +
                                            "belong to customer"));
                                })
                                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                                    return Mono.error(new IllegalArgumentException("Credit not found"));
                                })
                );
    }
    private Mono<Boolean> validateCreditCardOwnership(String customerId, String creditCardId) {
        return transactionCacheService.getCreditCard(creditCardId)
                .flatMap(creditCard -> {
                    if (creditCard.getCustomerId().equals(customerId)) {
                        return Mono.just(true);
                    }
                    return Mono.error(new IllegalArgumentException("CreditCard does not belong to customer"));
                })
                .switchIfEmpty(
                        creditClientService.getCreditCardById(creditCardId)
                                .flatMap(creditCard -> {
                                    if (creditCard.getCustomerId().equals(customerId)) {
                                        return transactionCacheService
                                                .saveCreditCard(creditCardId, creditCard)
                                                .thenReturn(true);
                                    }
                                    return Mono.error(new IllegalArgumentException("CreditCard does not " +
                                            "belong to customer"));
                                })
                                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                                    return Mono.error(new IllegalArgumentException("CreditCard not found"));
                                })
                );
    }
    public Flux<Transaction> getTrasactionsByDate(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByTransactionDateBetween(startDate, endDate);
    }
}
