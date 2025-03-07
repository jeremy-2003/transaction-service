package com.bank.transactionservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class YankiTransactionProcessedEvent {
    private String transactionId;
    private String senderPhoneNumber;
    private String receiverPhoneNumber;
    private BigDecimal amount;
    private String status;
    private String reason;
    private Instant processedAt;
}

