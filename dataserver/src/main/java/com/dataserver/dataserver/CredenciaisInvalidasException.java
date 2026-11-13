package com.dataserver.dataserver;

// Lançada no login quando o e-mail não existe ou a senha não confere (vira 401 no TratadorDeErros)
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos");
    }
}
