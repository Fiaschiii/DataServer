package com.dataserver.dataserver;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Freia quem tenta adivinhar senhas: depois de MAX_FALHAS logins errados seguidos,
// a mesma chave (IP + e-mail) fica bloqueada até a janela de BLOQUEIO passar.
// A chave inclui o IP para que um estranho não consiga trancar a conta de outra
// pessoa só errando a senha dela de propósito.
// Fica em memória: reiniciar o Java zera os contadores (suficiente para uma instância só).
@Component
public class LimiteDeTentativas {

    static final int MAX_FALHAS = 5;
    static final Duration BLOQUEIO = Duration.ofMinutes(15);
    // Acima disso, as chaves vencidas são descartadas (evita crescer sem limite)
    private static final int LIMPAR_ACIMA_DE = 10_000;

    private record Falhas(int quantidade, Instant primeira) {}

    private final Map<String, Falhas> falhas = new ConcurrentHashMap<>();
    private final Clock relogio;

    @Autowired
    public LimiteDeTentativas() {
        this(Clock.systemUTC());
    }

    // Usado nos testes, com um relógio controlado
    LimiteDeTentativas(Clock relogio) {
        this.relogio = relogio;
    }

    public boolean bloqueado(String chave) {
        Falhas f = falhas.get(chave);
        if (f == null) return false;
        if (vencido(f)) {
            falhas.remove(chave, f);
            return false;
        }
        return f.quantidade() >= MAX_FALHAS;
    }

    public void registrarFalha(String chave) {
        Instant agora = relogio.instant();
        falhas.compute(chave, (k, f) -> f == null || vencido(f)
            ? new Falhas(1, agora)
            : new Falhas(f.quantidade() + 1, f.primeira()));
        if (falhas.size() > LIMPAR_ACIMA_DE) {
            falhas.values().removeIf(this::vencido);
        }
    }

    // Login certo zera o contador daquela chave
    public void limpar(String chave) {
        falhas.remove(chave);
    }

    // Usado nos testes: cada teste começa sem bloqueios
    void limparTudo() {
        falhas.clear();
    }

    private boolean vencido(Falhas f) {
        return !relogio.instant().isBefore(f.primeira().plus(BLOQUEIO));
    }
}
