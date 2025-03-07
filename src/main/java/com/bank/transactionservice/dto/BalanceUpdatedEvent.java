package com.bank.transactionservice.dto;

import lombok.*;

import java.math.BigDecimal;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class BalanceUpdatedEvent {
    private String accountId;
    private BigDecimal newBalance;
    private String cardNumber;
}
