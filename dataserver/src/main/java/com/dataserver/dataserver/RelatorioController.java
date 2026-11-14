package com.dataserver.dataserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class RelatorioController {

    private final RestClient restClient;

    // Cabeçalho que prova ao Python que a chamada veio do Java (no Render o Python é público)
    static final String CABECALHO_CHAVE = "X-Chave-Interna";

    // URL do serviço Python (PYTHON_SERVICE_URL) e a chave combinada com ele (CHAVE_INTERNA)
    public RelatorioController(@Value("${python.service.url}") String pythonServiceUrl,
                               @Value("${python.service.chave}") String chaveInterna) {
        RestClient.Builder builder = RestClient.builder().baseUrl(pythonServiceUrl);
        if (!chaveInterna.isEmpty()) {
            builder.defaultHeader(CABECALHO_CHAVE, chaveInterna);
        }
        this.restClient = builder.build();
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
