package com.bank.transactionservice.model.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    private String id;
    private String fullName;
    private String documentNumber;
    private CustomerType customerType;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private String status;
    //Only for special profiles
    private boolean isVip;
    private boolean isPym;
}
