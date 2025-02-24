package com.bank.transactionservice.service;

import com.bank.transactionservice.model.account.Account;
import com.bank.transactionservice.model.account.AccountType;
import com.bank.transactionservice.model.credit.Credit;
import com.bank.transactionservice.model.credit.CreditType;
import com.bank.transactionservice.model.creditcard.CreditCard;
import com.bank.transactionservice.model.creditcard.CreditCardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.util.concurrent.TimeoutException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class TransactionCacheServiceTest {
    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOperations;
    private TransactionCacheService transactionCacheService;
    private ObjectMapper objectMapper;
    private Account testAccount;
    private Credit testCredit;
    private CreditCard testCreditCard;
    @BeforeEach
    void setUp() {
        transactionCacheService = new TransactionCacheService(redisTemplate);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Setup test Account
        testAccount = new Account();
        testAccount.setId("1");
        testAccount.setCustomerId("customer1");
        testAccount.setAccountType(AccountType.SAVINGS);
        testAccount.setBalance(1000.0);
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
    }
    @Test
    void saveAccount_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String expectedJson = objectMapper.writeValueAsString(testAccount);
        String expectedKey = "Account:1";
        when(valueOperations.set(anyString(), anyString()))
                .thenReturn(Mono.just(Boolean.TRUE));
        StepVerifier.create(transactionCacheService.saveAccount(testAccount.getId(), testAccount))
                .verifyComplete();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture());
        assert keyCaptor.getValue().equals(expectedKey);
        assert objectMapper.readTree(valueCaptor.getValue())
                .equals(objectMapper.readTree(expectedJson));
    }
    @Test
    void saveAccount_NullId_ReturnsError() {
        StepVerifier.create(transactionCacheService.saveAccount(null, testAccount))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
    @Test
    void getAccount_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String accountJson = objectMapper.writeValueAsString(testAccount);
        String expectedKey = "Account:1";
        when(valueOperations.get(eq(expectedKey)))
                .thenReturn(Mono.just(accountJson));
        StepVerifier.create(transactionCacheService.getAccount("1"))
                .expectNextMatches(account ->
                        account.getId().equals(testAccount.getId()) &&
                                account.getCustomerId().equals(testAccount.getCustomerId()) &&
                                account.getAccountType() == testAccount.getAccountType())
                .verifyComplete();
    }
    @Test
    void saveCredit_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String expectedJson = objectMapper.writeValueAsString(testCredit);
        String expectedKey = "Credit:1";
        when(valueOperations.set(anyString(), anyString()))
                .thenReturn(Mono.just(Boolean.TRUE));
        StepVerifier.create(transactionCacheService.saveCredit(testCredit.getId(), testCredit))
                .verifyComplete();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture());
        assert keyCaptor.getValue().equals(expectedKey);
        assert objectMapper.readTree(valueCaptor.getValue())
                .equals(objectMapper.readTree(expectedJson));
    }
    @Test
    void saveCreditCard_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String expectedJson = objectMapper.writeValueAsString(testCreditCard);
        String expectedKey = "CreditCard:1";
        when(valueOperations.set(anyString(), anyString()))
                .thenReturn(Mono.just(Boolean.TRUE));
        StepVerifier.create(transactionCacheService.saveCreditCard(testCreditCard.getId(), testCreditCard))
                .verifyComplete();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture());
        assert keyCaptor.getValue().equals(expectedKey);
        assert objectMapper.readTree(valueCaptor.getValue())
                .equals(objectMapper.readTree(expectedJson));
    }
    @Test
    void getCredit_NotFound_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(Mono.empty());
        StepVerifier.create(transactionCacheService.getCredit("1"))
                .verifyComplete();
    }
    @Test
    void getCreditCard_InvalidJson_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(Mono.just("invalid json"));
        StepVerifier.create(transactionCacheService.getCreditCard("1"))
                .verifyComplete();
    }
    @Test
    void getAccount_Timeout_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(Mono.error(new TimeoutException("Operation timed out")));
        StepVerifier.create(transactionCacheService.getAccount("1"))
                .verifyComplete();
    }
    @Test
    void getCredit_RedisError_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Redis connection error")));
        StepVerifier.create(transactionCacheService.getCredit("1"))
                .verifyComplete();
    }
    @Test
    void saveCreditCard_RedisError_PropagatesError() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Redis error")));
        StepVerifier.create(transactionCacheService.saveCreditCard("1", testCreditCard))
                .expectError(RuntimeException.class)
                .verify();
    }
    @Test
    void getAccount_TimeoutException_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenReturn(Mono.error(new TimeoutException("Operation timed out")));
        StepVerifier.create(transactionCacheService.getAccount("1"))
                .verifyComplete();
    }
    @Test
    void getCredit_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String creditJson = objectMapper.writeValueAsString(testCredit);
        String expectedKey = "Credit:1";
        when(valueOperations.get(eq(expectedKey)))
                .thenReturn(Mono.just(creditJson));
        StepVerifier.create(transactionCacheService.getCredit("1"))
                .expectNextMatches(credit ->
                        credit.getId().equals(testCredit.getId()) &&
                                credit.getCustomerId().equals(testCredit.getCustomerId()) &&
                                credit.getCreditType() == testCredit.getCreditType())
                .verifyComplete();
    }
    @Test
    void getCreditCard_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String creditCardJson = objectMapper.writeValueAsString(testCreditCard);
        String expectedKey = "CreditCard:1";
        when(valueOperations.get(eq(expectedKey)))
                .thenReturn(Mono.just(creditCardJson));
        StepVerifier.create(transactionCacheService.getCreditCard("1"))
                .expectNextMatches(creditCard ->
                        creditCard.getId().equals(testCreditCard.getId()) &&
                                creditCard.getCustomerId().equals(testCreditCard.getCustomerId()) &&
                                creditCard.getCardType() == testCreditCard.getCardType())
                .verifyComplete();
    }
    @Test
    void saveCredit_NullId_ReturnsError() {
        StepVerifier.create(transactionCacheService.saveCredit(null, testCredit))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
    @Test
    void saveCreditCard_NullId_ReturnsError() {
        StepVerifier.create(transactionCacheService.saveCreditCard(null, testCreditCard))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}