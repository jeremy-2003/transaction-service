package com.bank.transactionservice.model;

import lombok.*;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credit {
    @Id
    private String id;
    private String customerId;
    private CreditType creditType;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private BigDecimal interestRate;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
