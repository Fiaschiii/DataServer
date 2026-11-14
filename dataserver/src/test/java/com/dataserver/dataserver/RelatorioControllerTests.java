package com.dataserver.dataserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpServer;

// Testa o /relatorio sem depender do serviço Python real nem do banco:
// um servidor HTTP falso simula o Python (respondendo como texto puro ou com erro).
class RelatorioControllerTests {

    private static final String ANALISE_FALSA = """
        {"total_produtos":2,"preco_total":30.0,"preco_medio":15.0,\
        "produto_mais_caro":{"id":2,"nome":"Caro","preco":20.0},\
        "produto_mais_barato":{"id":1,"nome":"Barato","preco":10.0}}""";

    // Status que o Python falso devolve; 200 = responde a análise normalmente
    private static final AtomicInteger statusDoPython = new AtomicInteger(200);

    private static final HttpServer pythonFalso = iniciarPythonFalso();

    private static HttpServer iniciarPythonFalso() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/analise", exchange -> {
                int status = statusDoPython.get();
                if (status != 200) {
                    exchange.sendResponseHeaders(status, -1);
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

    @AfterAll
    static void pararPythonFalso() {
        pythonFalso.stop(0);
    }

    @Nested
    @TesteDaApi
    @WithMockUser // requisições já autenticadas: aqui o foco são as regras da API, não o login
    class ComOPythonRodando {

        @DynamicPropertySource
        static void apontarParaPythonFalso(DynamicPropertyRegistry registry) {
            registry.add("python.service.url", () -> "http://localhost:" + pythonFalso.getAddress().getPort());
        }

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ProdutoRepository produtoRepository;

        @BeforeEach
        void pythonNormal() {
            statusDoPython.set(200);
        }

        @Test
        void relatorioRespondeComoJson() throws Exception {
            mockMvc.perform(get("/relatorio"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total_produtos").value(2))
                .andExpect(jsonPath("$.preco_medio").value(15.0))
                .andExpect(jsonPath("$.produto_mais_caro.nome").value("Caro"));
        }

        // Python com erro (ex.: banco fora): 502 em JSON, não 500
        @Test
        void relatorioResponde502QuandoOPythonDaErro() throws Exception {
            statusDoPython.set(500);

            mockMvc.perform(get("/relatorio"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.erro").exists());
        }
    }

    @Nested
    @TesteDaApi
    @WithMockUser
    class ComOPythonParado {

        // Porta livre em que nada está escutando (como o Python desligado)
        @DynamicPropertySource
        static void apontarParaPortaSemPython(DynamicPropertyRegistry registry) throws IOException {
            int portaLivre;
            try (ServerSocket socket = new ServerSocket(0)) {
                portaLivre = socket.getLocalPort();
            }
            registry.add("python.service.url", () -> "http://localhost:" + portaLivre);
        }

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ProdutoRepository produtoRepository;

        @Test
        void relatorioResponde503ComMensagemClara() throws Exception {
            mockMvc.perform(get("/relatorio"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.erro").value(
                    "O serviço de análise (Python) não está rodando. Inicie-o na porta 8001."));
        }
    }
}
