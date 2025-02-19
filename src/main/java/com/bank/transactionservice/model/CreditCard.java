package com.bank.transactionservice.model;

import lombok.*;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
}
