package com.dataserver.dataserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;


// Controller REST - expõe os endpoints da API de produtos
@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    // Injeta o repositório JPA automaticamente
    @Autowired
    private ProdutoRepository produtoRepository;

    // GET /produtos - busca todos os produtos do Supabase
    @GetMapping
    public List<Produto> getProdutos() {
        return produtoRepository.findAll();
    }

    // POST /produtos - salva um novo produto no Supabase
    @PostMapping
    public Produto criarProduto(@RequestBody Produto produto) {
        return produtoRepository.save(produto);
    }

    // DELETE /produtos/{id} - remove um produto pelo ID
    @DeleteMapping("/{id}")
    public void deletarProduto(@PathVariable Long id) {
        produtoRepository.deleteById(id);
    }
}