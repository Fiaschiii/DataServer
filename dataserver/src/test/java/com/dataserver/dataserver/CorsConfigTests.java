package com.dataserver.dataserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Garante que o navegador (frontend em localhost:5173) consegue chamar a API.
// O preflight (OPTIONS) é o que o navegador envia antes de DELETE/PUT e de POST com JSON.
// @WebMvcTest sobe só a camada web: o teste não depende do banco estar acessível.
@WebMvcTest
class CorsConfigTests {

    private static final String FRONTEND = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    // Os controllers dependem do repositório; aqui ele é simulado (sem banco).
    @MockitoBean
    private ProdutoRepository produtoRepository;

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE"})
    void preflightDoFrontendEhPermitido(String metodo) throws Exception {
        mockMvc.perform(options("/produtos/1")
                .header("Origin", FRONTEND)
                .header("Access-Control-Request-Method", metodo))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND));
    }

    @Test
    void preflightDeOrigemDesconhecidaEhBloqueado() throws Exception {
        mockMvc.perform(options("/produtos/1")
                .header("Origin", "http://site-malicioso.com")
                .header("Access-Control-Request-Method", "DELETE"))
            .andExpect(status().isForbidden());
    }
}
