package com.bank.transactionservice.dto.bootcoinbank;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class BootCoinBankPurchaseCompleted {
    private String transactionId;
    private boolean accepted;
}
