package com.dataserver.dataserver;

// Lançada no login quando a chave (IP + e-mail) errou a senha vezes demais (vira 429 no TratadorDeErros)
public class MuitasTentativasException extends RuntimeException {

    public MuitasTentativasException() {
        super("Muitas tentativas de login. Aguarde " + LimiteDeTentativas.BLOQUEIO.toMinutes()
            + " minutos e tente novamente.");
    }
}
