package com.dataserver.dataserver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Testa as validações da API de produtos sem banco (repositório simulado).
@WebMvcTest
class ProdutoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProdutoRepository produtoRepository;

    // ---------- POST /produtos ----------

    @Test
    void cadastraProdutoValido() throws Exception {
        when(produtoRepository.save(any(Produto.class)))
            .thenReturn(new Produto(1L, "Teclado", 250.0));

        mockMvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Teclado\",\"preco\":250.0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nome").value("Teclado"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "sem preço            | {\"nome\":\"X\"}                  | preco | O preço é obrigatório",
        "preço negativo       | {\"nome\":\"X\",\"preco\":-5}     | preco | O preço deve ser maior que zero",
        "preço zero           | {\"nome\":\"X\",\"preco\":0}      | preco | O preço deve ser maior que zero",
        "sem nome             | {\"preco\":10}                    | nome  | O nome é obrigatório",
        "nome só com espaços  | {\"nome\":\"   \",\"preco\":10}   | nome  | O nome é obrigatório",
    })
    void rejeitaProdutoInvalidoCom400(String caso, String json, String campo, String mensagem) throws Exception {
        mockMvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Dados inválidos"))
            .andExpect(jsonPath("$.campos." + campo).value(mensagem));

        verify(produtoRepository, never()).save(any());
    }

    @Test
    void rejeitaNomeMuitoLongo() throws Exception {
        String nome = "a".repeat(256);
        mockMvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"preco\":10}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.nome").value("O nome deve ter no máximo 255 caracteres"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "JSON malformado      | {\"nome\":",
        "preço não numérico   | {\"nome\":\"X\",\"preco\":\"abc\"}",
    })
    void rejeitaJsonInvalidoCom400(String caso, String json) throws Exception {
        mockMvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("JSON inválido ou malformado"));

        verify(produtoRepository, never()).save(any());
    }

    // ---------- GET /produtos ----------

    @Test
    void listaComPaginacaoPadrao() throws Exception {
        when(produtoRepository.findAll(PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(List.of(new Produto(1L, "Mouse", 150.0))));

        mockMvc.perform(get("/produtos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].nome").value("Mouse"));
    }

    @Test
    void aceitaTamanhoMaximoDePagina() throws Exception {
        when(produtoRepository.findByNomeContainingIgnoreCase("mouse", PageRequest.of(2, 100)))
            .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/produtos").param("page", "2").param("size", "100").param("busca", "mouse"))
            .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "page={0}, size={1}")
    @CsvSource(delimiter = '|', value = {
        "-1  | 20  | page | A página deve ser maior ou igual a 0",
        "0   | 0   | size | O tamanho da página deve ser no mínimo 1",
        "0   | 101 | size | O tamanho da página deve ser no máximo 100",
    })
    void rejeitaPaginacaoForaDosLimitesCom400(String page, String size, String campo, String mensagem) throws Exception {
        mockMvc.perform(get("/produtos").param("page", page).param("size", size))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Parâmetros inválidos"))
            .andExpect(jsonPath("$.campos." + campo).value(mensagem));

        verifyNoInteractions(produtoRepository);
    }

    @Test
    void rejeitaParametroNaoNumericoCom400() throws Exception {
        mockMvc.perform(get("/produtos").param("page", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.page").value("Valor inválido: 'abc'"));

        verify(produtoRepository, never()).findAll(any(Pageable.class));
        verify(produtoRepository, never()).findByNomeContainingIgnoreCase(anyString(), any());
    }
}
