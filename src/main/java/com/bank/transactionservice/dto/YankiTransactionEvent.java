package com.bank.transactionservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class YankiTransactionEvent {
    private String transactionId;
    private String senderPhoneNumber;
    private String receiverPhoneNumber;
    private String senderCard;
    private String receiverCard;
    private BigDecimal amount;
}
