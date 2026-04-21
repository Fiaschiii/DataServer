package com.dataserver.dataserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class ProdutoController {

    @GetMapping("/produtos")
    public List<Produto> getProdutos() {
        return List.of(
            new Produto(1, "Notebook", 3500.00),
            new Produto(2, "Mouse", 150.00),
            new Produto(3, "Teclado", 250.00)
        );
    }
}