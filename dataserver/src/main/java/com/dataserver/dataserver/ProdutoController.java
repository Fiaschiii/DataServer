package com.dataserver.dataserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;


@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    // Limite de itens por página (evita carregar a tabela inteira de uma vez)
    static final int TAMANHO_MAXIMO_PAGINA = 100;

    @Autowired
    private ProdutoRepository produtoRepository;

    // GET /produtos?page=0&size=20&busca=texto - busca paginada (size entre 1 e 100)
    @GetMapping
    public Page<Produto> getProdutos(
        @RequestParam(defaultValue = "0")
        @Min(value = 0, message = "A página deve ser maior ou igual a 0") int page,
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "O tamanho da página deve ser no mínimo 1")
        @Max(value = TAMANHO_MAXIMO_PAGINA, message = "O tamanho da página deve ser no máximo 100") int size,
        @RequestParam(defaultValue = "") String busca
    ) {
        if (busca.isEmpty()) {
            return produtoRepository.findAll(PageRequest.of(page, size));
        }
        return produtoRepository.findByNomeContainingIgnoreCase(busca, PageRequest.of(page, size));
    }

    // POST /produtos - salva um novo produto (nome obrigatório, preço > 0)
    @PostMapping
    public Produto criarProduto(@Valid @RequestBody Produto produto) {
        return produtoRepository.save(produto);
    }

    // DELETE /produtos/{id}
    @DeleteMapping("/{id}")
    public void deletarProduto(@PathVariable Long id) {
        produtoRepository.deleteById(id);
    }
}
