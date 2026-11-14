package com.dataserver.dataserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpServer;

// Testa o /relatorio sem depender do serviço Python real nem do banco:
// um servidor HTTP falso simula o Python respondendo como texto puro
// e, como o Python de verdade no Render, recusando chamadas sem a chave interna.
@TesteDaApi
@WithMockUser // requisições já autenticadas: aqui o foco são as regras da API, não o login
class RelatorioControllerTests {

    private static final String ANALISE_FALSA = """
        {"total_produtos":2,"preco_total":30.0,"preco_medio":15.0,\
        "produto_mais_caro":{"id":2,"nome":"Caro","preco":20.0},\
        "produto_mais_barato":{"id":1,"nome":"Barato","preco":10.0}}""";

    private static final String CHAVE = "chave-interna-de-teste";

    // Status que o Python falso devolve antes de responder normalmente (simula ele acordando)
    private static final ConcurrentLinkedQueue<Integer> falhasProgramadas = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger chamadas = new AtomicInteger();

    private static final HttpServer pythonFalso = iniciarPythonFalso();

    private static HttpServer iniciarPythonFalso() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/analise", exchange -> {
                chamadas.incrementAndGet();
                Integer falha = falhasProgramadas.poll();
                if (falha != null) {
                    exchange.sendResponseHeaders(falha, -1);
                    exchange.close();
                    return;
                }
                if (!CHAVE.equals(exchange.getRequestHeaders().getFirst("X-Chave-Interna"))) {
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                    return;
                }
                byte[] corpo = ANALISE_FALSA.getBytes(StandardCharsets.UTF_8);
                // Content-Type propositalmente "errado" para provar que o Java normaliza para JSON
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, corpo.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(corpo);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void apontarParaPythonFalso(DynamicPropertyRegistry registry) {
        registry.add("python.service.url", () -> "http://localhost:" + pythonFalso.getAddress().getPort());
        registry.add("CHAVE_INTERNA", () -> CHAVE);
        registry.add("python.service.espera-entre-tentativas-ms", () -> "10");
        registry.add("python.service.manter-acordado", () -> "false");
    }

    @BeforeEach
    void limparFalhas() {
        falhasProgramadas.clear();
        chamadas.set(0);
    }

    @AfterAll
    static void pararPythonFalso() {
        pythonFalso.stop(0);
    }

    @Autowired
    private MockMvc mockMvc;

    // Os controllers dependem do repositório; aqui ele é simulado (sem banco).
    @MockitoBean
    private ProdutoRepository produtoRepository;

    @Test
    void relatorioEnviaAChaveInternaERespondeComoJson() throws Exception {
        mockMvc.perform(get("/relatorio"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.total_produtos").value(2))
            .andExpect(jsonPath("$.preco_medio").value(15.0))
            .andExpect(jsonPath("$.produto_mais_caro.nome").value("Caro"));
    }

    // Render acordando o Python: as primeiras respostas são 502/503, depois funciona
    @Test
    void relatorioTentaDeNovoEnquantoOPythonAcorda() throws Exception {
        falhasProgramadas.add(503);
        falhasProgramadas.add(502);

        mockMvc.perform(get("/relatorio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_produtos").value(2));
        org.junit.jupiter.api.Assertions.assertEquals(3, chamadas.get());
    }

    // Python continua fora depois de todas as tentativas: 503 em JSON, não 500
    @Test
    void relatorioResponde503QuandoOPythonNaoVolta() throws Exception {
        for (int i = 0; i < 5; i++) {
            falhasProgramadas.add(500);
        }

        mockMvc.perform(get("/relatorio"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503))
            .andExpect(jsonPath("$.erro").exists());
        org.junit.jupiter.api.Assertions.assertEquals(3, chamadas.get());
    }

    // Python recusou (ex.: CHAVE_INTERNA diferente): 502 sem repetir a chamada
    @Test
    void relatorioResponde502SemRepetirQuandoOPythonRecusa() throws Exception {
        falhasProgramadas.add(401);

        mockMvc.perform(get("/relatorio"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.status").value(502));
        org.junit.jupiter.api.Assertions.assertEquals(1, chamadas.get());
    }
}
