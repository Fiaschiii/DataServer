package com.dataserver.dataserver;

import org.springframework.http.HttpStatus;

// Lançada pelo /relatorio quando o serviço Python falha: 503 (não está rodando)
// ou 502 (respondeu com erro). Vira resposta JSON no TratadorDeErros.
public class ServicoDeAnaliseException extends RuntimeException {

    private final HttpStatus status;

    public ServicoDeAnaliseException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
