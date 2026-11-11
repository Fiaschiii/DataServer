package com.dataserver.dataserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class RelatorioController {

    private final RestClient restClient;

    // URL do serviço Python (configurável em application.properties / variável PYTHON_SERVICE_URL)
    public RelatorioController(@Value("${python.service.url}") String pythonServiceUrl) {
        this.restClient = RestClient.create(pythonServiceUrl);
    }

    // GET /relatorio - repassa a análise do serviço Python.
    // "produces" garante o Content-Type application/json (antes saía como text/plain).
    @GetMapping(value = "/relatorio", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getRelatorio() {
        return restClient.get()
            .uri("/analise")
            .retrieve()
            .body(String.class);
    }
}
