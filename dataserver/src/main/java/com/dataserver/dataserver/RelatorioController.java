package com.dataserver.dataserver;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RestController
public class RelatorioController {

    private static final Logger log = LoggerFactory.getLogger(RelatorioController.class);

    private final RestClient restClient;

    // URL do serviço Python (PYTHON_SERVICE_URL, padrão http://localhost:8001).
    // Tempo curto: localmente o Python responde na hora; se não responder, está parado.
    public RelatorioController(@Value("${python.service.url}") String pythonServiceUrl,
                               @Value("${python.service.timeout-segundos:5}") int timeoutSegundos) {
        Duration timeout = Duration.ofSeconds(timeoutSegundos);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
            .baseUrl(pythonServiceUrl)
            .requestFactory(fabrica)
            .build();
    }

    // GET /relatorio - repassa a análise do serviço Python.
    // "produces" garante o Content-Type application/json (antes saía como text/plain).
    // Falhas viram 503 (Python parado) ou 502 (Python respondeu com erro), nunca um 500 genérico.
    @GetMapping(value = "/relatorio", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getRelatorio() {
        try {
            return restClient.get()
                .uri("/analise")
                .retrieve()
                .body(String.class);
        } catch (ResourceAccessException e) {
            log.warn("Serviço de análise (Python) não respondeu: {}", e.getMessage());
            throw new ServicoDeAnaliseException(HttpStatus.SERVICE_UNAVAILABLE,
                "O serviço de análise (Python) não está rodando. Inicie-o na porta 8001.");
        } catch (RestClientResponseException e) {
            log.error("Serviço de análise (Python) respondeu com erro: HTTP {}", e.getStatusCode().value());
            throw new ServicoDeAnaliseException(HttpStatus.BAD_GATEWAY,
                "O serviço de análise (Python) respondeu com erro (HTTP "
                    + e.getStatusCode().value() + "). Veja o terminal do Python.");
        }
    }
}
