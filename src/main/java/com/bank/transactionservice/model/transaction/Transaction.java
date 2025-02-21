package com.bank.transactionservice.model.transaction;

import lombok.*;
import nonapi.io.github.classgraph.json.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    private String id;
    private String customerId;
    private String productId;
    private ProductCategory productCategory;
    private ProductSubType productSubType;
    private TransactionType transactionType;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String destinationAccountId; //Only for transfer
}
