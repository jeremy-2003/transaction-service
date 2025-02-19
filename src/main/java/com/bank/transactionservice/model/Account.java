package com.bank.transactionservice.model;

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
@Document(collection = "accounts")
public class Account {
    @Id
    private String id;
    private String customerId;
    private AccountType accountType;
    private double balance;
    private List<String> holders;
    private List<String> signers;
    private LocalDateTime createdAd;
    private LocalDateTime modifiedAd;
}
