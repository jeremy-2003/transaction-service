package com.bank.transactionservice.dto;


import lombok.*;
import org.springframework.data.repository.NoRepositoryBean;

@Data
@Builder
public class BaseResponse<T> {
    private int status;
    private String message;
    private T data;
}
