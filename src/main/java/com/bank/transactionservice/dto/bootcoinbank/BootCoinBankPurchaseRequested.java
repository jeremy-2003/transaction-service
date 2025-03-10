package com.bank.transactionservice.dto.bootcoinbank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BootCoinBankPurchaseRequested {
    private String transactionId;
    private String buyerDocumentNumber;
    private String buyerAccountNumber;
    private BigDecimal amount; //Quantity of bootcoin
    private BigDecimal totalAmountInPEN;
}
