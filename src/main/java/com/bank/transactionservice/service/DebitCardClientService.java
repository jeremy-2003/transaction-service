package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.BaseResponse;
import com.bank.transactionservice.model.debitcard.DebitCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class DebitCardClientService {
    private final WebClient webClient;
    @Autowired
    public DebitCardClientService(@Value("${account-service.base-url}") String accountServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
    }
    public Mono<DebitCard> getDebitCardById(String cardId) {
        return webClient.get()
                .uri("/debit-cards/{cardId}", cardId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<DebitCard>>() { })
                .flatMap(response -> {
                    if (response.getStatus() == 200 && response.getData() != null) {
                        return Mono.just(response.getData());
                    } else {
                        return Mono.error(new RuntimeException("Debit card not found or error: " + response.getMessage()));
                    }
                });
    }
}
