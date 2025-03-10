package com.bank.transactionservice.event;
import com.bank.transactionservice.client.AccountClientService;
import com.bank.transactionservice.client.CustomerClientService;
import com.bank.transactionservice.dto.bootcoinbank.BootCoinBankPurchaseCompleted;
import com.bank.transactionservice.dto.bootcoinbank.BootCoinBankPurchaseRequested;
import com.bank.transactionservice.model.account.Account;
import com.bank.transactionservice.model.customer.Customer;
import com.bank.transactionservice.model.transaction.ProductCategory;
import com.bank.transactionservice.model.transaction.ProductSubType;
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
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
@ExtendWith(MockitoExtension.class)
public class BootCoinBankPurchaseTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private TransactionService transactionService;
    @Mock
    private AccountClientService accountClientService;
    @Mock
    private CustomerClientService customerClientService;
    @Mock
    private ListenableFuture<SendResult<String, Object>> listenableFuture;
    @InjectMocks
    private BootCoinBankPurchase bootCoinBankPurchase;
    @Captor
    private ArgumentCaptor<BootCoinBankPurchaseCompleted> eventCaptor;
    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;
    private BootCoinBankPurchaseRequested requestEvent;
    private Customer customer;
    private Account account;
    private Transaction transaction;
    @BeforeEach
    void setUp() {
        requestEvent = BootCoinBankPurchaseRequested.builder()
                .transactionId("transaction-123")
                .buyerDocumentNumber("12345678")
                .buyerAccountNumber("ACC-001")
                .amount(new BigDecimal("2.5"))
                .totalAmountInPEN(new BigDecimal("250"))
                .build();
        customer = new Customer();
        customer.setId("customer-001");
        customer.setDocumentNumber("12345678");
        account = new Account();
        account.setId("ACC-001");
        account.setCustomerId("customer-001");
        transaction = new Transaction(
                "tx-123",
                "12345678",
                "ACC-001",
                ProductCategory.ACCOUNT,
                ProductSubType.BOOT_COIN,
                TransactionType.WITHDRAWAL,
                new BigDecimal("250"),
                LocalDateTime.now(),
                null,
                null,
                null,
                null
        );
        when(kafkaTemplate.send(anyString(), any())).thenReturn(listenableFuture);
    }
    @Test
    void processBootCoinPurchase_Success() {
        when(customerClientService.getCustomerByDocumentNumber(requestEvent.getBuyerDocumentNumber()))
                .thenReturn(Mono.just(customer));
        when(accountClientService.getAccountById(requestEvent.getBuyerAccountNumber()))
                .thenReturn(Mono.just(account));
        when(transactionService.createTransaction(any(Transaction.class)))
                .thenReturn(Mono.just(transaction));

        bootCoinBankPurchase.processBootCoinPurchase(requestEvent);

        verify(customerClientService).getCustomerByDocumentNumber(requestEvent.getBuyerDocumentNumber());
        verify(accountClientService).getAccountById(requestEvent.getBuyerAccountNumber());
        verify(transactionService).createTransaction(transactionCaptor.capture());
        verify(kafkaTemplate).send(eq("bootcoin.bank.purchase.procesed"), eventCaptor.capture());
        Transaction capturedTransaction = transactionCaptor.getValue();
        assertEquals(requestEvent.getBuyerDocumentNumber(), capturedTransaction.getCustomerId());
        assertEquals(requestEvent.getBuyerAccountNumber(), capturedTransaction.getProductId());
        assertEquals(ProductCategory.ACCOUNT, capturedTransaction.getProductCategory());
        assertEquals(ProductSubType.BOOT_COIN, capturedTransaction.getProductSubType());
        assertEquals(TransactionType.WITHDRAWAL, capturedTransaction.getTransactionType());
        assertEquals(requestEvent.getTotalAmountInPEN(), capturedTransaction.getAmount());
        BootCoinBankPurchaseCompleted resultEvent = eventCaptor.getValue();
        assertEquals(requestEvent.getTransactionId(), resultEvent.getTransactionId());
        assertTrue(resultEvent.isAccepted());
    }
    @Test
    void processBootCoinPurchase_AccountDoesNotBelongToCustomer() {
        Account differentAccount = new Account();
        differentAccount.setId("ACC-001");
        differentAccount.setCustomerId("different-customer-id");
        when(customerClientService.getCustomerByDocumentNumber(requestEvent.getBuyerDocumentNumber()))
                .thenReturn(Mono.just(customer));
        when(accountClientService.getAccountById(requestEvent.getBuyerAccountNumber()))
                .thenReturn(Mono.just(differentAccount));
        bootCoinBankPurchase.processBootCoinPurchase(requestEvent);
        verify(customerClientService).getCustomerByDocumentNumber(requestEvent.getBuyerDocumentNumber());
        verify(accountClientService).getAccountById(requestEvent.getBuyerAccountNumber());
        verify(transactionService, times(0)).createTransaction(any());
        verify(kafkaTemplate).send(eq("bootcoin.bank.purchase.procesed"), eventCaptor.capture());
        BootCoinBankPurchaseCompleted resultEvent = eventCaptor.getValue();
        assertEquals(requestEvent.getTransactionId(), resultEvent.getTransactionId());
        assertFalse(resultEvent.isAccepted());
    }
    @Test
    void processBootCoinPurchase_CustomerNotFound() {
        when(customerClientService.getCustomerByDocumentNumber(requestEvent.getBuyerDocumentNumber()))
                .thenReturn(Mono.error(new RuntimeException("Customer not found")));
        bootCoinBankPurchase.processBootCoinPurchase(requestEvent);
        verify(customerClientService).getCustomerByDocumentNumber(requestEvent.getBuyerDocumentNumber());
        verify(accountClientService, times(0)).getAccountById(anyString());
        verify(transactionService, times(0)).createTransaction(any());
        verify(kafkaTemplate).send(eq("bootcoin.bank.purchase.procesed"), eventCaptor.capture());
        BootCoinBankPurchaseCompleted resultEvent = eventCaptor.getValue();
        assertEquals(requestEvent.getTransactionId(), resultEvent.getTransactionId());
        assertFalse(resultEvent.isAccepted());
    }
    @Test
    void processBootCoinPurchase_AccountNotFound() {
        when(customerClientService.getCustomerByDocumentNumber(requestEvent.getBuyerDocumentNumber()))
                .thenReturn(Mono.just(customer));
        when(accountClientService.getAccountById(requestEvent.getBuyerAccountNumber()))
                .thenReturn(Mono.error(new RuntimeException("Account not found")));
        bootCoinBankPurchase.processBootCoinPurchase(requestEvent);
        verify(customerClientService).getCustomerByDocumentNumber(requestEvent.getBuyerDocumentNumber());
        verify(accountClientService).getAccountById(requestEvent.getBuyerAccountNumber());
        verify(transactionService, times(0)).createTransaction(any());
        verify(kafkaTemplate).send(eq("bootcoin.bank.purchase.procesed"), eventCaptor.capture());
        BootCoinBankPurchaseCompleted resultEvent = eventCaptor.getValue();
        assertEquals(requestEvent.getTransactionId(), resultEvent.getTransactionId());
        assertFalse(resultEvent.isAccepted());
    }
    @Test
    void processBootCoinPurchase_TransactionCreationFails() {
        when(customerClientService.getCustomerByDocumentNumber(requestEvent.getBuyerDocumentNumber()))
                .thenReturn(Mono.just(customer));
        when(accountClientService.getAccountById(requestEvent.getBuyerAccountNumber()))
                .thenReturn(Mono.just(account));
        when(transactionService.createTransaction(any(Transaction.class)))
                .thenReturn(Mono.error(new RuntimeException("Failed to create transaction")));
        bootCoinBankPurchase.processBootCoinPurchase(requestEvent);
        verify(customerClientService).getCustomerByDocumentNumber(requestEvent.getBuyerDocumentNumber());
        verify(accountClientService).getAccountById(requestEvent.getBuyerAccountNumber());
        verify(transactionService).createTransaction(any());
        verify(kafkaTemplate).send(eq("bootcoin.bank.purchase.procesed"), eventCaptor.capture());
        BootCoinBankPurchaseCompleted resultEvent = eventCaptor.getValue();
        assertEquals(requestEvent.getTransactionId(), resultEvent.getTransactionId());
        assertFalse(resultEvent.isAccepted());
    }
}