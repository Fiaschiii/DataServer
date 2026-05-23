package com.dataserver.dataserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    @Autowired
    private ProdutoRepository produtoRepository;

    // GET /produtos?page=0&size=50 - busca 50 produtos por vez
    @GetMapping
    public Page<Produto> getProdutos(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "") String busca
    ) {
        if (busca.isEmpty()) {
            return produtoRepository.findAll(PageRequest.of(page, size));
        }            
        return produtoRepository.findByNomeContainingIgnoreCase(busca, PageRequest.of(page, size));
    }

    // POST /produtos - salva um novo produto
    @PostMapping
    public Produto criarProduto(@RequestBody Produto produto) {
        return produtoRepository.save(produto);
    }

    // DELETE /produtos/{id}
    @DeleteMapping("/{id}")
    public void deletarProduto(@PathVariable Long id) {
        produtoRepository.deleteById(id);
    }
}