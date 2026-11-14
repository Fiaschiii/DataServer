package com.dataserver.dataserver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

// Regras do limite de login com um relógio controlado (sem esperar 15 minutos de verdade)
class LimiteDeTentativasTests {

    // Relógio que o teste avança à mão
    private static class RelogioManual extends Clock {
        private Instant agora = Instant.parse("2026-01-01T12:00:00Z");

        void avancar(Duration d) { agora = agora.plus(d); }

        @Override public Instant instant() { return agora; }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    private final RelogioManual relogio = new RelogioManual();
    private final LimiteDeTentativas limite = new LimiteDeTentativas(relogio);

    private void errar(String chave, int vezes) {
        for (int i = 0; i < vezes; i++) limite.registrarFalha(chave);
    }

    @Test
    void bloqueiaSoNaQuintaFalha() {
        errar("ip|email", LimiteDeTentativas.MAX_FALHAS - 1);
        assertFalse(limite.bloqueado("ip|email"));

        errar("ip|email", 1);
        assertTrue(limite.bloqueado("ip|email"));
        assertFalse(limite.bloqueado("outro-ip|email"));
    }

    @Test
    void desbloqueiaQuandoAJanelaPassa() {
        errar("ip|email", LimiteDeTentativas.MAX_FALHAS);

        relogio.avancar(LimiteDeTentativas.BLOQUEIO.minusSeconds(1));
        assertTrue(limite.bloqueado("ip|email"));

        relogio.avancar(Duration.ofSeconds(1));
        assertFalse(limite.bloqueado("ip|email"));
    }

    @Test
    void falhasEspalhadasAlemDaJanelaNaoSeSomam() {
        errar("ip|email", LimiteDeTentativas.MAX_FALHAS - 1);
        relogio.avancar(LimiteDeTentativas.BLOQUEIO);

        errar("ip|email", 1);
        assertFalse(limite.bloqueado("ip|email"));
    }

    @Test
    void limparZeraAContagem() {
        errar("ip|email", LimiteDeTentativas.MAX_FALHAS);
        limite.limpar("ip|email");
        assertFalse(limite.bloqueado("ip|email"));
    }
}
