package com.bank.transactionservice.model.creditcard;

import lombok.*;
import nonapi.io.github.classgraph.json.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CreditCard {
    @Id
    private String id;
    private String customerId;
    private CreditCardType cardType;
    private BigDecimal creditLimit;
    private BigDecimal availableBalance;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private PaymentStatus paymentStatus;
    private LocalDateTime cutoffDate;
    private LocalDateTime paymentDueDate;
    private BigDecimal minimumPayment;
}
