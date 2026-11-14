package com.dataserver.dataserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// No servidor, o site tem outra URL: CORS_ORIGENS troca as origens aceitas
// (lista separada por vírgula, espaços ignorados) e o localhost deixa de valer.
@TesteDaApi
@TestPropertySource(properties = "CORS_ORIGENS=https://dataserver-web.onrender.com, https://www.meusite.com.br")
class CorsOrigensConfiguraveisTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProdutoRepository produtoRepository;

    @ParameterizedTest
    @ValueSource(strings = {"https://dataserver-web.onrender.com", "https://www.meusite.com.br"})
    void origensConfiguradasSaoAceitas(String origem) throws Exception {
        mockMvc.perform(options("/produtos")
                .header("Origin", origem)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", origem));
    }

    @Test
    void localhostDeixaDeSerAceito() throws Exception {
        mockMvc.perform(options("/produtos")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }
}
