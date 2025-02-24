package com.bank.transactionservice.service;

import com.bank.transactionservice.model.account.Account;
import com.bank.transactionservice.model.account.AccountType;
import com.bank.transactionservice.model.credit.Credit;
import com.bank.transactionservice.model.credit.CreditType;
import com.bank.transactionservice.model.creditcard.CreditCard;
import com.bank.transactionservice.model.creditcard.CreditCardType;
import com.bank.transactionservice.model.transaction.ProductCategory;
import com.bank.transactionservice.model.transaction.Transaction;
import com.bank.transactionservice.model.transaction.TransactionType;
import com.bank.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionCacheService transactionCacheService;
    @Mock
    private AccountClientService accountClientService;
    @Mock
    private CreditClientService creditClientService;
    @InjectMocks
    private TransactionService transactionService;
    private Transaction testTransaction;
    private Account testAccount;
    private Credit testCredit;
    private CreditCard testCreditCard;
    @BeforeEach
    void setUp() {
        // Setup test Account
        testAccount = new Account();
        testAccount.setId("1");
        testAccount.setCustomerId("customer1");
        testAccount.setAccountType(AccountType.SAVINGS);
        testAccount.setBalance(1000.0);
        testAccount.setMaxFreeTransaction(3);
        testAccount.setTransactionCost(new BigDecimal("1.00"));
        // Setup test Credit
        testCredit = new Credit();
        testCredit.setId("1");
        testCredit.setCustomerId("customer1");
        testCredit.setCreditType(CreditType.PERSONAL);
        testCredit.setAmount(new BigDecimal("5000.00"));
        testCredit.setRemainingBalance(new BigDecimal("5000.00"));
        // Setup test CreditCard
        testCreditCard = new CreditCard();
        testCreditCard.setId("1");
        testCreditCard.setCustomerId("customer1");
        testCreditCard.setCardType(CreditCardType.PERSONAL_CREDIT_CARD);
        testCreditCard.setCreditLimit(new BigDecimal("10000.00"));
        testCreditCard.setAvailableBalance(new BigDecimal("10000.00"));
        // Setup base transaction
        testTransaction = new Transaction();
        testTransaction.setId("1");
        testTransaction.setCustomerId("customer1");
        testTransaction.setProductId("1");
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setTransactionDate(LocalDateTime.now());
    }
    @Test
    void createTransaction_AccountDeposit_Success() {
        testTransaction.setProductCategory(ProductCategory.ACCOUNT);
        testTransaction.setProductId("1");
        testTransaction.setTransactionType(TransactionType.DEPOSIT);
        when(transactionCacheService.getAccount(anyString())).thenReturn(Mono.just(testAccount));
        when(accountClientService.getAccountById(anyString())).thenReturn(Mono.just(testAccount));
        when(transactionRepository.findByProductId(anyString())).thenReturn(Flux.empty());
        when(accountClientService.updateAccountBalance(anyString(), any(BigDecimal.class)))
                .thenReturn(Mono.just(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(testTransaction));
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectNextMatches(transaction ->
                        transaction.getId().equals("1") &&
                                transaction.getProductCategory() == ProductCategory.ACCOUNT &&
                                transaction.getTransactionType() == TransactionType.DEPOSIT)
                .verifyComplete();
    }
    @Test
    void createTransaction_AccountWithdrawal_Success() {
        testTransaction.setProductCategory(ProductCategory.ACCOUNT);
        testTransaction.setProductId("1");
        testTransaction.setTransactionType(TransactionType.WITHDRAWAL);
        // Mock account service responses
        when(transactionCacheService.getAccount(testTransaction.getProductId())).thenReturn(Mono.just(testAccount));
        when(accountClientService.getAccountById(testTransaction.getProductId())).thenReturn(Mono.just(testAccount));
        when(accountClientService.updateAccountBalance(anyString(), any(BigDecimal.class)))
                .thenReturn(Mono.just(testAccount));
        // Mock repository responses
        when(transactionRepository.findByProductId(anyString()))
                .thenReturn(Flux.empty());
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(Mono.just(testTransaction));
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectNextMatches(transaction ->
                        transaction.getId().equals("1") &&
                                transaction.getProductCategory() == ProductCategory.ACCOUNT &&
                                transaction.getTransactionType() == TransactionType.WITHDRAWAL)
                .verifyComplete();
    }
    @Test
    void createTransaction_AccountTransfer_Success() {
        testTransaction.setProductCategory(ProductCategory.ACCOUNT);
        testTransaction.setProductId("1");
        testTransaction.setTransactionType(TransactionType.TRANSFER);
        testTransaction.setDestinationAccountId("2");
        Account destinationAccount = new Account();
        destinationAccount.setId("2");
        destinationAccount.setBalance(500.0);
        when(transactionCacheService.getAccount(anyString())).thenReturn(Mono.just(testAccount));
        when(transactionRepository.findByProductId(anyString())).thenReturn(Flux.empty());
        when(accountClientService.getAccountById(anyString())).thenReturn(Mono.just(destinationAccount));
        when(accountClientService.updateAccountBalance(anyString(), any(BigDecimal.class)))
                .thenReturn(Mono.just(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(testTransaction));
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectNextMatches(transaction ->
                        transaction.getId().equals("1") &&
                                transaction.getTransactionType() == TransactionType.TRANSFER &&
                                transaction.getDestinationAccountId().equals("2"))
                .verifyComplete();
    }
    @Test
    void createTransaction_CreditPayment_Success() {
        testTransaction.setProductCategory(ProductCategory.CREDIT);
        testTransaction.setProductId("1");
        testTransaction.setTransactionType(TransactionType.CREDIT_PAYMENT);
        when(transactionCacheService.getCredit(anyString())).thenReturn(Mono.just(testCredit));
        when(creditClientService.getCreditById(anyString())).thenReturn(Mono.just(testCredit));
        when(creditClientService.updateCreditBalance(anyString(), any(BigDecimal.class)))
                .thenReturn(Mono.just(testCredit));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(testTransaction));
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectNextMatches(transaction ->
                        transaction.getId().equals("1") &&
                                transaction.getProductCategory() == ProductCategory.CREDIT &&
                                transaction.getTransactionType() == TransactionType.CREDIT_PAYMENT)
                .verifyComplete();
    }

    @Test
    void createTransaction_CreditCardPurchase_Success() {
        testTransaction.setProductCategory(ProductCategory.CREDIT_CARD);
        testTransaction.setProductId("1");
        testTransaction.setTransactionType(TransactionType.CREDIT_CARD_PURCHASE);
        when(transactionCacheService.getCreditCard(anyString())).thenReturn(Mono.just(testCreditCard));
        when(creditClientService.getCreditCardById(anyString())).thenReturn(Mono.just(testCreditCard));
        when(creditClientService.updateCreditCardBalance(anyString(), any(BigDecimal.class)))
                .thenReturn(Mono.just(testCreditCard));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(testTransaction));
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectNextMatches(transaction ->
                        transaction.getId().equals("1") &&
                                transaction.getProductCategory() == ProductCategory.CREDIT_CARD &&
                                transaction.getTransactionType() == TransactionType.CREDIT_CARD_PURCHASE)
                .verifyComplete();
    }
    @Test
    void getTransactionsByCustomerId_Success() {
        String customerId = "customer1";
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(transactionRepository.findByCustomerId(customerId))
                .thenReturn(Flux.fromIterable(transactions));
        StepVerifier.create(transactionService.getTransactionsByCustomerId(customerId))
                .expectNextSequence(transactions)
                .verifyComplete();
    }
    @Test
    void getTransactionsByProductId_Success() {
        String productId = "1";
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(transactionRepository.findByProductId(productId))
                .thenReturn(Flux.fromIterable(transactions));
        StepVerifier.create(transactionService.getTransactionsByProductId(productId))
                .expectNextSequence(transactions)
                .verifyComplete();
    }
    @Test
    void getTransactionById_Success() {
        String transactionId = "1";
        when(transactionRepository.findById(transactionId))
                .thenReturn(Mono.just(testTransaction));
        StepVerifier.create(transactionService.getTransactionById(transactionId))
                .expectNext(testTransaction)
                .verifyComplete();
    }
    @Test
    void getTransactionById_NotFound() {
        String transactionId = "nonexistent";
        when(transactionRepository.findById(transactionId))
                .thenReturn(Mono.empty());
        StepVerifier.create(transactionService.getTransactionById(transactionId))
                .expectError(RuntimeException.class)
                .verify();
    }
    @Test
    void getTransactionsByDate_Success() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(transactionRepository.findByTransactionDateBetween(startDate, endDate))
                .thenReturn(Flux.fromIterable(transactions));
        StepVerifier.create(transactionService.getTrasactionsByDate(startDate, endDate))
                .expectNextSequence(transactions)
                .verifyComplete();
    }
    @Test
    void validateOwnership_Account_Success() {
        String customerId = "customer1";
        String accountId = "1";
        // Mock account service responses
        when(transactionCacheService.getAccount(accountId)).thenReturn(Mono.just(testAccount));
        when(accountClientService.getAccountById(accountId)).thenReturn(Mono.just(testAccount));
        // Mock credit service responses with empty
        when(transactionCacheService.getCredit(accountId)).thenReturn(Mono.empty());
        when(creditClientService.getCreditById(accountId)).thenReturn(Mono.empty());
        // Mock credit card service responses with empty
        when(transactionCacheService.getCreditCard(accountId)).thenReturn(Mono.empty());
        when(creditClientService.getCreditCardById(accountId)).thenReturn(Mono.empty());
        StepVerifier.create(transactionService.validateOwnership(customerId, accountId))
                .expectNext(true)
                .verifyComplete();
    }
    @Test
    void validateOwnership_Credit_Success() {
        String customerId = "customer1";
        String creditId = "1";
        // Mock account service responses with empty
        when(transactionCacheService.getAccount(creditId)).thenReturn(Mono.empty());
        when(accountClientService.getAccountById(creditId)).thenReturn(Mono.empty());
        // Mock credit service responses
        when(transactionCacheService.getCredit(creditId)).thenReturn(Mono.just(testCredit));
        when(creditClientService.getCreditById(creditId)).thenReturn(Mono.just(testCredit));
        // Mock credit card service responses with empty
        when(transactionCacheService.getCreditCard(creditId)).thenReturn(Mono.empty());
        when(creditClientService.getCreditCardById(creditId)).thenReturn(Mono.empty());
        StepVerifier.create(transactionService.validateOwnership(customerId, creditId))
                .expectNext(true)
                .verifyComplete();
    }
    @Test
    void validateOwnership_CreditCard_Success() {
        String customerId = "customer1";
        String creditCardId = "1";
        when(transactionCacheService.getAccount(creditCardId)).thenReturn(Mono.empty());
        when(accountClientService.getAccountById(creditCardId)).thenReturn(Mono.empty());
        when(transactionCacheService.getCredit(creditCardId)).thenReturn(Mono.empty());
        when(creditClientService.getCreditById(creditCardId)).thenReturn(Mono.empty());
        when(transactionCacheService.getCreditCard(creditCardId)).thenReturn(Mono.just(testCreditCard));
        when(creditClientService.getCreditCardById(creditCardId)).thenReturn(Mono.just(testCreditCard));
        StepVerifier.create(transactionService.validateOwnership(customerId, creditCardId))
                .expectNext(true)
                .verifyComplete();
    }
    @Test
    void validateOwnership_NotFound_ReturnsFalse() {
        String customerId = "customer1";
        String productId = "nonexistent";
        when(transactionCacheService.getAccount(productId)).thenReturn(Mono.empty());
        when(accountClientService.getAccountById(productId)).thenReturn(Mono.empty());
        when(transactionCacheService.getCredit(productId)).thenReturn(Mono.empty());
        when(creditClientService.getCreditById(productId)).thenReturn(Mono.empty());
        when(transactionCacheService.getCreditCard(productId)).thenReturn(Mono.empty());
        when(creditClientService.getCreditCardById(productId)).thenReturn(Mono.empty());
        StepVerifier.create(transactionService.validateOwnership(customerId, productId))
                .expectNext(false)
                .verifyComplete();
    }
    @Test
    void processAccountTransaction_TransferWithoutDestination_Error() {
        testTransaction.setProductCategory(ProductCategory.ACCOUNT);
        testTransaction.setProductId("1");
        testTransaction.setTransactionType(TransactionType.TRANSFER);
        testTransaction.setDestinationAccountId(null);
        when(transactionCacheService.getAccount(anyString())).thenReturn(Mono.empty());
        when(accountClientService.getAccountById(anyString())).thenReturn(Mono.just(testAccount));
        when(transactionCacheService.saveAccount(anyString(), any(Account.class))).thenReturn(Mono.empty());
        when(transactionRepository.findByProductId(anyString())).thenReturn(Flux.empty());
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException &&
                                error.getMessage().equals("A destination account is required for a transfer"))
                .verify();
    }
    @Test
    void calculateNewBalance_NegativeAmount_Error() {
        testTransaction.setProductCategory(ProductCategory.ACCOUNT);
        testTransaction.setAmount(new BigDecimal("-100.00"));
        testTransaction.setTransactionType(TransactionType.WITHDRAWAL);
        when(transactionCacheService.getAccount(anyString())).thenReturn(Mono.empty());
        when(accountClientService.getAccountById(anyString())).thenReturn(Mono.just(testAccount));
        when(transactionCacheService.saveAccount(anyString(), any(Account.class))).thenReturn(Mono.empty());
        when(transactionRepository.findByProductId(anyString())).thenReturn(Flux.empty());
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException &&
                                error.getMessage().equals("Transaction amount cannot be negative"))
                .verify();
    }
    @Test
    void calculateNewBalance_InsufficientBalance_WithdrawalError() {
        testTransaction.setAmount(new BigDecimal("2000.00")); // Amount greater than balance
        testTransaction.setProductCategory(ProductCategory.ACCOUNT);
        testTransaction.setTransactionType(TransactionType.WITHDRAWAL);
        when(transactionCacheService.getAccount(anyString())).thenReturn(Mono.empty());
        when(accountClientService.getAccountById(anyString())).thenReturn(Mono.just(testAccount));
        when(transactionCacheService.saveAccount(anyString(), any(Account.class))).thenReturn(Mono.empty());
        when(transactionRepository.findByProductId(anyString())).thenReturn(Flux.empty());
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException &&
                                error.getMessage().equals("Insufficient balance for withdrawal"))
                .verify();
    }
    @Test
    void calculateNewBalance_InsufficientBalance_TransferError() {
        testTransaction.setAmount(new BigDecimal("2000.00"));
        testTransaction.setProductCategory(ProductCategory.ACCOUNT);
        testTransaction.setTransactionType(TransactionType.TRANSFER);
        testTransaction.setDestinationAccountId("2");
        when(transactionCacheService.getAccount(anyString())).thenReturn(Mono.empty());
        when(accountClientService.getAccountById(anyString())).thenReturn(Mono.just(testAccount));
        when(transactionCacheService.saveAccount(anyString(), any(Account.class))).thenReturn(Mono.empty());
        when(transactionRepository.findByProductId(anyString())).thenReturn(Flux.empty());
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException &&
                                error.getMessage().equals("Insufficient balance for transfer"))
                .verify();
    }
    @Test
    void calculateNewBalance_InvalidTransactionType_Error() {
        testTransaction.setProductCategory(ProductCategory.ACCOUNT);
        testTransaction.setTransactionType(TransactionType.CREDIT_PAYMENT);
        when(transactionCacheService.getAccount(anyString())).thenReturn(Mono.empty());
        when(accountClientService.getAccountById(anyString())).thenReturn(Mono.just(testAccount));
        when(transactionCacheService.saveAccount(anyString(), any(Account.class))).thenReturn(Mono.empty());
        when(transactionRepository.findByProductId(anyString())).thenReturn(Flux.empty());
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException &&
                                error.getMessage().equals("Invalid transaction type for account"))
                .verify();
    }
    @Test
    void calculateNewCreditBalance_NegativeAmount_Error() {
        testTransaction.setProductCategory(ProductCategory.CREDIT);
        testTransaction.setTransactionType(TransactionType.CREDIT_PAYMENT);
        testTransaction.setAmount(new BigDecimal("-100.00"));
        when(transactionCacheService.getCredit(anyString())).thenReturn(Mono.empty());
        when(creditClientService.getCreditById(anyString())).thenReturn(Mono.just(testCredit));
        when(transactionCacheService.saveCredit(anyString(), any(Credit.class))).thenReturn(Mono.empty());
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException &&
                                error.getMessage().equals("Transaction amount cannot be negative"))
                .verify();
    }
    @Test
    void calculateNewCreditBalance_InvalidTransactionType_Error() {
        testTransaction.setProductCategory(ProductCategory.CREDIT);
        testTransaction.setTransactionType(TransactionType.WITHDRAWAL);
        when(transactionCacheService.getCredit(anyString())).thenReturn(Mono.empty());
        when(creditClientService.getCreditById(anyString())).thenReturn(Mono.just(testCredit));
        when(transactionCacheService.saveCredit(anyString(), any(Credit.class))).thenReturn(Mono.empty());
        StepVerifier.create(transactionService.createTransaction(testTransaction))
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException &&
                                error.getMessage().equals("Invalid transaction type for credit"))
                .verify();
    }
    @Test
    void validateCreditCardOwnership_CacheEmpty_Success() {
        String customerId = "customer1";
        String creditCardId = "1";
        when(transactionCacheService.getAccount(creditCardId)).thenReturn(Mono.empty());
        when(transactionCacheService.getCredit(creditCardId)).thenReturn(Mono.empty());
        when(transactionCacheService.getCreditCard(creditCardId)).thenReturn(Mono.empty());
        when(accountClientService.getAccountById(creditCardId)).thenReturn(Mono.empty());
        when(creditClientService.getCreditById(creditCardId)).thenReturn(Mono.empty());
        when(creditClientService.getCreditCardById(creditCardId)).thenReturn(Mono.just(testCreditCard));
        when(transactionCacheService.saveCreditCard(anyString(), any(CreditCard.class))).thenReturn(Mono.empty());
        StepVerifier.create(transactionService.validateOwnership(customerId, creditCardId))
                .expectNext(true)
                .verifyComplete();
        verify(transactionCacheService).saveCreditCard(eq(creditCardId), any(CreditCard.class));
    }
}