package com.dataserver.dataserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpServer;

// Testa o /relatorio sem depender do serviço Python real nem do banco:
// um servidor HTTP falso simula o Python respondendo como texto puro.
@WebMvcTest
class RelatorioControllerTests {

    private static final String ANALISE_FALSA = """
        {"total_produtos":2,"preco_total":30.0,"preco_medio":15.0,\
        "produto_mais_caro":{"id":2,"nome":"Caro","preco":20.0},\
        "produto_mais_barato":{"id":1,"nome":"Barato","preco":10.0}}""";

    private static final HttpServer pythonFalso = iniciarPythonFalso();

    private static HttpServer iniciarPythonFalso() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/analise", exchange -> {
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
    void relatorioRespondeComoJson() throws Exception {
        mockMvc.perform(get("/relatorio"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.total_produtos").value(2))
            .andExpect(jsonPath("$.preco_medio").value(15.0))
            .andExpect(jsonPath("$.produto_mais_caro.nome").value("Caro"));
    }
}
