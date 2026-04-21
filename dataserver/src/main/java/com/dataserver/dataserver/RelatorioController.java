package com.dataserver.dataserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
public class RelatorioController {

    private final WebClient webClient = WebClient.create("http://localhost:8001");

    @GetMapping("/relatorio")
    public String getRelatorio() {
        return webClient.get()
                .uri("/analise")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
