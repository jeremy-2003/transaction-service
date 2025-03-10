package com.bank.transactionservice.event;

import com.bank.transactionservice.client.AccountClientService;
import com.bank.transactionservice.client.DebitCardClientService;
import com.bank.transactionservice.dto.YankiTransactionEvent;
import com.bank.transactionservice.dto.YankiTransactionProcessedEvent;
import com.bank.transactionservice.model.account.Account;
import com.bank.transactionservice.model.debitcard.DebitCard;
import com.bank.transactionservice.model.transaction.Transaction;
import com.bank.transactionservice.model.transaction.TransactionType;
import com.bank.transactionservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.ListenableFuture;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YankiTransactionEventConsumerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private DebitCardClientService debitCardClientService;

    @Mock
    private AccountClientService accountClientService;

    @InjectMocks
    private YankiTransactionEventConsumer consumer;

    @Test
    void shouldProcessInternalTransactionWhenBothUsersAreYankiOnly() {
        YankiTransactionEvent event = new YankiTransactionEvent();
        event.setSenderCard(null);
        event.setReceiverCard(null);

        when(kafkaTemplate.send(anyString(), any())).thenReturn(mock(ListenableFuture.class));
        consumer.processYankiTransaction(event);
        verify(kafkaTemplate, times(1)).send(eq("yanki.transaction.processed"), any());
    }

    @Test
    void shouldDebitSenderAccountWhenSenderHasCardAndReceiverDoesNot() {
        YankiTransactionEvent event = new YankiTransactionEvent();
        event.setSenderCard("123456789");
        event.setReceiverCard(null);
        event.setAmount(BigDecimal.valueOf(100));

        DebitCard debitCard = new DebitCard();
        debitCard.setPrimaryAccountId("account123");
        debitCard.setCustomerId("customer123");

        Account account = new Account();
        account.setId("account123");

        Transaction transaction = new Transaction();
        transaction.setId("txn123");

        when(debitCardClientService.getDebitCardByCardNumber("123456789"))
                .thenReturn(Mono.just(debitCard));
        when(accountClientService.getAccountById("account123"))
                .thenReturn(Mono.just(account));
        when(transactionService.createTransaction(any()))
                .thenReturn(Mono.just(transaction));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(mock(ListenableFuture.class));
        consumer.processYankiTransaction(event);
        verify(transactionService, times(1)).createTransaction(any(Transaction.class));
        verify(kafkaTemplate, times(1)).send(eq("yanki.transaction.processed"), any());
    }

    @Test
    void shouldHandleErrorWhenDebitFails() {
        YankiTransactionEvent event = new YankiTransactionEvent();
        event.setSenderCard("123456789");
        event.setReceiverCard(null);

        DebitCard debitCard = new DebitCard();
        debitCard.setPrimaryAccountId("account123");

        when(debitCardClientService.getDebitCardByCardNumber("123456789"))
                .thenReturn(Mono.just(debitCard));
        when(accountClientService.getAccountById("account123"))
                .thenReturn(Mono.error(new RuntimeException("Account not found")));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(mock(ListenableFuture.class));

        consumer.processYankiTransaction(event);
        verify(kafkaTemplate, times(1)).send(anyString(), argThat(argument ->
                argument instanceof YankiTransactionProcessedEvent &&
                        ((YankiTransactionProcessedEvent) argument).getStatus().equals("FAILED")
        ));
    }
    @Test
    void shouldCreditReceiverAccountWhenReceiverHasCardAndSenderDoesNot() {
        YankiTransactionEvent event = new YankiTransactionEvent();
        event.setSenderCard(null);
        event.setReceiverCard("987654321");
        event.setAmount(BigDecimal.valueOf(100));
        DebitCard receiverDebitCard = new DebitCard();
        receiverDebitCard.setPrimaryAccountId("receiverAccount123");
        receiverDebitCard.setCustomerId("receiverCustomer123");
        Account receiverAccount = new Account();
        receiverAccount.setId("receiverAccount123");
        Transaction transaction = new Transaction();
        transaction.setId("txn456");

        when(debitCardClientService.getDebitCardByCardNumber("987654321"))
                .thenReturn(Mono.just(receiverDebitCard));
        when(accountClientService.getAccountById("receiverAccount123"))
                .thenReturn(Mono.just(receiverAccount));
        when(transactionService.createTransaction(any()))
                .thenReturn(Mono.just(transaction));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(mock(ListenableFuture.class));
        consumer.processYankiTransaction(event);
        verify(transactionService, times(1)).createTransaction(argThat(tx ->
                tx.getTransactionType() == TransactionType.DEPOSIT &&
                        tx.getProductId().equals("receiverAccount123") &&
                        tx.getAmount().equals(BigDecimal.valueOf(100))
        ));
        verify(kafkaTemplate, times(1)).send(eq("yanki.transaction.processed"), argThat(arg ->
                arg instanceof YankiTransactionProcessedEvent &&
                        ((YankiTransactionProcessedEvent) arg).getStatus().equals("SUCCESS")
        ));
    }
    @Test
    void shouldHandleErrorWhenCreditToReceiverFails() {
        YankiTransactionEvent event = new YankiTransactionEvent();
        event.setSenderCard(null);
        event.setReceiverCard("987654321");
        DebitCard receiverDebitCard = new DebitCard();
        receiverDebitCard.setPrimaryAccountId("receiverAccount123");

        when(debitCardClientService.getDebitCardByCardNumber("987654321"))
                .thenReturn(Mono.just(receiverDebitCard));
        when(accountClientService.getAccountById("receiverAccount123"))
                .thenReturn(Mono.error(new RuntimeException("Receiver account not found")));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(mock(ListenableFuture.class));
        consumer.processYankiTransaction(event);
        verify(kafkaTemplate, times(1)).send(anyString(), argThat(argument ->
                argument instanceof YankiTransactionProcessedEvent &&
                        ((YankiTransactionProcessedEvent) argument).getStatus().equals("FAILED") &&
                        ((YankiTransactionProcessedEvent) argument)
                            .getReason().contains("Could not credit receiver's account")
        ));
    }
    @Test
    void shouldProcessTransferWhenBothUsersHaveCards() {
        YankiTransactionEvent event = new YankiTransactionEvent();
        event.setSenderCard("123456789");
        event.setReceiverCard("987654321");
        event.setAmount(BigDecimal.valueOf(100));
        event.setTransactionId("txnId123");
        event.setSenderPhoneNumber("1234567890");
        event.setReceiverPhoneNumber("0987654321");

        DebitCard senderDebitCard = new DebitCard();
        senderDebitCard.setPrimaryAccountId("senderAccount123");
        senderDebitCard.setCustomerId("senderCustomer123");
        Account senderAccount = new Account();
        senderAccount.setId("senderAccount123");
        senderAccount.setCustomerId("senderCustomer123");

        DebitCard receiverDebitCard = new DebitCard();
        receiverDebitCard.setPrimaryAccountId("receiverAccount123");
        receiverDebitCard.setCustomerId("receiverCustomer123");
        Account receiverAccount = new Account();
        receiverAccount.setId("receiverAccount123");
        receiverAccount.setCustomerId("receiverCustomer123");

        Transaction transaction = new Transaction();
        transaction.setId("txn789");

        when(debitCardClientService.getDebitCardByCardNumber("123456789"))
                .thenReturn(Mono.just(senderDebitCard));
        when(debitCardClientService.getDebitCardByCardNumber("987654321"))
                .thenReturn(Mono.just(receiverDebitCard));
        when(accountClientService.getAccountById("senderAccount123"))
                .thenReturn(Mono.just(senderAccount));
        when(accountClientService.getAccountById("receiverAccount123"))
                .thenReturn(Mono.just(receiverAccount));
        when(transactionService.createTransaction(any()))
                .thenReturn(Mono.just(transaction));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(mock(ListenableFuture.class));
        consumer.processYankiTransaction(event);
        verify(transactionService, times(1)).createTransaction(argThat(tx ->
                tx.getTransactionType() == TransactionType.TRANSFER &&
                        tx.getProductId().equals("senderAccount123") &&
                        tx.getCustomerId().equals("senderCustomer123") &&
                        tx.getDestinationAccountId().equals("receiverAccount123") &&
                        tx.getAmount().equals(BigDecimal.valueOf(100))
        ));
        verify(kafkaTemplate, times(1)).send(eq("yanki.transaction.processed"), argThat(arg ->
                arg instanceof YankiTransactionProcessedEvent &&
                        ((YankiTransactionProcessedEvent) arg).getStatus().equals("SUCCESS") &&
                        ((YankiTransactionProcessedEvent) arg).getTransactionId().equals("txnId123") &&
                        ((YankiTransactionProcessedEvent) arg).getSenderPhoneNumber().equals("1234567890") &&
                        ((YankiTransactionProcessedEvent) arg).getReceiverPhoneNumber().equals("0987654321") &&
                        ((YankiTransactionProcessedEvent) arg).getAmount().equals(BigDecimal.valueOf(100))
        ));
    }
    @Test
    void shouldHandleErrorWhenTransferBetweenAccountsFails() {
        YankiTransactionEvent event = new YankiTransactionEvent();
        event.setSenderCard("123456789");
        event.setReceiverCard("987654321");
        event.setAmount(BigDecimal.valueOf(100));

        DebitCard senderDebitCard = new DebitCard();
        senderDebitCard.setPrimaryAccountId("senderAccount123");

        DebitCard receiverDebitCard = new DebitCard();
        receiverDebitCard.setPrimaryAccountId("receiverAccount123");

        when(debitCardClientService.getDebitCardByCardNumber("123456789"))
                .thenReturn(Mono.just(senderDebitCard));
        when(debitCardClientService.getDebitCardByCardNumber("987654321"))
                .thenReturn(Mono.just(receiverDebitCard));
        when(accountClientService.getAccountById("senderAccount123"))
                .thenReturn(Mono.error(new RuntimeException("Sender account not found")));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(mock(ListenableFuture.class));
        consumer.processYankiTransaction(event);
        verify(kafkaTemplate, times(1)).send(anyString(), argThat(argument ->
                argument instanceof YankiTransactionProcessedEvent &&
                        ((YankiTransactionProcessedEvent) argument).getStatus().equals("FAILED") &&
                        ((YankiTransactionProcessedEvent) argument)
                            .getReason().contains("Error in transfer of bank accounts")
        ));
    }
    @Test
    void shouldHandleExceptionDuringProcessing() {
        YankiTransactionEvent event = new YankiTransactionEvent();
        event.setSenderCard("123456789");
        event.setReceiverCard("987654321");

        when(debitCardClientService.getDebitCardByCardNumber(anyString()))
                .thenReturn(null);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(mock(ListenableFuture.class));
        consumer.processYankiTransaction(event);
        verify(kafkaTemplate, times(1)).send(anyString(), argThat(argument ->
                argument instanceof YankiTransactionProcessedEvent &&
                        ((YankiTransactionProcessedEvent) argument).getStatus().equals("FAILED")
        ));
    }
}

