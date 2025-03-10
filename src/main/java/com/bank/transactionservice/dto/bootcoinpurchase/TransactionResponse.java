package com.bank.transactionservice.dto.bootcoinpurchase;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TransactionResponse {
    private String transactionId;
    private boolean success;
    private String message;
}
