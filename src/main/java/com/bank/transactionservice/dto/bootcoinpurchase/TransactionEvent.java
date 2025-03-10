package com.bank.transactionservice.dto.bootcoinpurchase;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TransactionEvent {
    private String purchaseId;
    private String buyerDocumentNumber;
    private String sellerDocumentNumber;
    private BigDecimal amount;
    private BigDecimal totalAmountInPEN;
    private String sellerAccountNumber;
    private String buyerAccountNumber;
    private String transactionType;
}