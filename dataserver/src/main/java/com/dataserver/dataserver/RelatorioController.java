package com.dataserver.dataserver;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@RestController
public class RelatorioController {

    private static final Logger log = LoggerFactory.getLogger(RelatorioController.class);

    // Cabeçalho que prova ao Python que a chamada veio do Java (no Render o Python é público)
    static final String CABECALHO_CHAVE = "X-Chave-Interna";

    private final RestClient restClient;
    private final int tentativas;
    private final long esperaEntreTentativasMs;
    private final boolean manterAcordado;

    // URL do serviço Python (PYTHON_SERVICE_URL) e a chave combinada com ele (CHAVE_INTERNA).
    // No plano free do Render o Python dorme após 15 min sem uso e leva de 20 a 60 s para
    // acordar: por isso o tempo de resposta é generoso e há novas tentativas.
    public RelatorioController(@Value("${python.service.url}") String pythonServiceUrl,
                               @Value("${python.service.chave}") String chaveInterna,
                               @Value("${python.service.timeout-segundos:70}") int timeoutSegundos,
                               @Value("${python.service.tentativas:3}") int tentativas,
                               @Value("${python.service.espera-entre-tentativas-ms:5000}") long esperaMs,
                               @Value("${python.service.manter-acordado:true}") boolean manterAcordado) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));

        RestClient.Builder builder = RestClient.builder()
            .baseUrl(pythonServiceUrl)
            .requestFactory(fabrica);
        if (!chaveInterna.isEmpty()) {
            builder.defaultHeader(CABECALHO_CHAVE, chaveInterna);
        }
        this.restClient = builder.build();
        this.tentativas = Math.max(1, tentativas);
        this.esperaEntreTentativasMs = esperaMs;
        this.manterAcordado = manterAcordado;
    }

    // GET /relatorio - repassa a análise do serviço Python.
    // "produces" garante o Content-Type application/json (antes saía como text/plain).
    // Falhas viram 503 (Python fora/acordando) ou 502 (Python recusou), nunca um 500 genérico.
    @GetMapping(value = "/relatorio", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getRelatorio() {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return restClient.get()
                    .uri("/analise")
                    .retrieve()
                    .body(String.class);
            } catch (HttpClientErrorException e) {
                // 4xx: não adianta repetir (ex.: 401 = CHAVE_INTERNA diferente nos dois serviços)
                log.error("Serviço de análise recusou a chamada: HTTP {}", e.getStatusCode().value());
                throw new ServicoDeAnaliseException(HttpStatus.BAD_GATEWAY,
                    "O serviço de análise recusou a chamada (HTTP " + e.getStatusCode().value()
                        + "). Confira a CHAVE_INTERNA da API e do serviço de análise.");
            } catch (HttpServerErrorException | ResourceAccessException e) {
                // 5xx ou sem conexão/tempo esgotado: o Python pode estar acordando
                log.warn("Serviço de análise indisponível (tentativa {} de {}): {}",
                    tentativa, tentativas, e.getMessage());
                if (tentativa >= tentativas) {
                    throw new ServicoDeAnaliseException(HttpStatus.SERVICE_UNAVAILABLE,
                        "O serviço de análise está indisponível ou iniciando. "
                            + "Tente novamente em alguns segundos.");
                }
                esperar();
            }
        }
    }

    // Enquanto a API está no ar, cutuca o Python a cada 10 min para ele não dormir
    // (o Render só desliga um serviço free após 15 min sem requisições). Também roda
    // logo na subida, para o Python já ir acordando junto com a API.
    @Scheduled(initialDelay = 0, fixedDelay = 10 * 60 * 1000)
    public void manterServicoDeAnaliseAcordado() {
        if (!manterAcordado) {
            return;
        }
        try {
            restClient.get().uri("/saude").retrieve().toBodilessEntity();
        } catch (RuntimeException e) {
            log.debug("Não foi possível acordar o serviço de análise: {}", e.getMessage());
        }
    }

    private void esperar() {
        try {
            Thread.sleep(esperaEntreTentativasMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicoDeAnaliseException(HttpStatus.SERVICE_UNAVAILABLE,
                "O serviço de análise está indisponível. Tente novamente em alguns segundos.");
        }
    }
}
