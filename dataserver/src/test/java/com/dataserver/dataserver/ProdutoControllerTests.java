package com.dataserver.dataserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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

    @Test
    void ignoraIdEnviadoNoCorpoParaNaoSobrescreverProdutoExistente() throws Exception {
        when(produtoRepository.save(any(Produto.class)))
            .thenReturn(new Produto(42L, "Teclado", 250.0));

        mockMvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":3,\"nome\":\"Teclado\",\"preco\":250.0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42));

        ArgumentCaptor<Produto> salvo = ArgumentCaptor.forClass(Produto.class);
        verify(produtoRepository).save(salvo.capture());
        assertNull(salvo.getValue().getId());
        assertEquals("Teclado", salvo.getValue().getNome());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "sem preço          | {\"nome\":\"X\"}                  | preco | O preço é obrigatório",
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

    // ---------- GET /produtos/{id} ----------

    @Test
    void buscaProdutoPorId() throws Exception {
        when(produtoRepository.findById(5L)).thenReturn(Optional.of(new Produto(5L, "Monitor", 900.0)));

        mockMvc.perform(get("/produtos/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.nome").value("Monitor"));
    }

    @Test
    void buscaProdutoInexistenteRetorna404() throws Exception {
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/produtos/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.erro").value("Produto 99 não encontrado"));
    }

    @Test
    void buscaComIdNaoNumericoRetorna400() throws Exception {
        mockMvc.perform(get("/produtos/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.id").value("Valor inválido: 'abc'"));

        verifyNoInteractions(produtoRepository);
    }

    // ---------- PUT /produtos/{id} ----------

    @Test
    void atualizaProdutoUsandoIdDaUrl() throws Exception {
        when(produtoRepository.findById(5L)).thenReturn(Optional.of(new Produto(5L, "Monitor", 900.0)));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/produtos/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":3,\"nome\":\"Monitor 27\",\"preco\":1200.0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.nome").value("Monitor 27"))
            .andExpect(jsonPath("$.preco").value(1200.0));

        ArgumentCaptor<Produto> salvo = ArgumentCaptor.forClass(Produto.class);
        verify(produtoRepository).save(salvo.capture());
        assertEquals(5L, salvo.getValue().getId());
    }

    @Test
    void atualizaProdutoInexistenteRetorna404() throws Exception {
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/produtos/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"X\",\"preco\":10}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Produto 99 não encontrado"));

        verify(produtoRepository, never()).save(any());
    }

    @Test
    void atualizaComDadosInvalidosRetorna400() throws Exception {
        mockMvc.perform(put("/produtos/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"X\",\"preco\":-1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.preco").value("O preço deve ser maior que zero"));

        verifyNoInteractions(produtoRepository);
    }

    // ---------- DELETE /produtos/{id} ----------

    @Test
    void deletaProdutoExistente() throws Exception {
        when(produtoRepository.existsById(5L)).thenReturn(true);

        mockMvc.perform(delete("/produtos/5"))
            .andExpect(status().isOk());

        verify(produtoRepository).deleteById(5L);
    }

    @Test
    void deletaProdutoInexistenteRetorna404() throws Exception {
        when(produtoRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(delete("/produtos/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Produto 99 não encontrado"));

        verify(produtoRepository, never()).deleteById(any());
    }
}
