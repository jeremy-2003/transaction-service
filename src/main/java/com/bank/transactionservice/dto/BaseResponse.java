package com.bank.transactionservice.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BaseResponse<T> {
    private int status;
    private String message;
    private T data;
}
