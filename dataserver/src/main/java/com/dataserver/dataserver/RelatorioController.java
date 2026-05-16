package com.dataserver.dataserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class RelatorioController {

    private final RestClient restClient = RestClient.create();

    @GetMapping("/relatorio")
    public String getRelatorio() {
        return restClient.get()
            .uri("http://localhost:8001/analise")
            .retrieve()
            .body(String.class);
    }
}
