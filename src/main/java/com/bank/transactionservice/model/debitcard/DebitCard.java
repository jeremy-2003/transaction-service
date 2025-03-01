package com.bank.transactionservice.model.debitcard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "debit_cards")
public class DebitCard {
    @Id
    private String id;
    private String cardNumber;
    private String customerId;
    private String status;
    private String primaryAccountId;
    private List<String> associatedAccountIds;
    private LocalDateTime expirationDate;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}