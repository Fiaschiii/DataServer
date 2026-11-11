package com.dataserver.dataserver;

// Lançada quando /produtos/{id} aponta para um produto que não existe (vira 404 no TratadorDeErros)
public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(Long id) {
        super("Produto " + id + " não encontrado");
    }
}
