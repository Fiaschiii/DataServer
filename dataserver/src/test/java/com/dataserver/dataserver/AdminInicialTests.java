package com.dataserver.dataserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// ADMIN_SENHA é a fonte da verdade da senha do administrador (sem banco: repositório simulado)
class AdminInicialTests {

    private static final String EMAIL = "admin@teste.com";

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // custo baixo: teste rápido

    private void subir(String senha) {
        new AdminInicial(repositorio, encoder, EMAIL, senha).run(null);
    }

    @Test
    void criaOAdministradorQuandoOEmailNaoExiste() {
        when(repositorio.findByEmail(EMAIL)).thenReturn(Optional.empty());

        subir("senha-nova-123");

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).save(salvo.capture());
        assertEquals(EMAIL, salvo.getValue().getEmail());
        assertTrue(encoder.matches("senha-nova-123", salvo.getValue().getSenhaHash()));
    }

    @Test
    void atualizaASenhaQuandoADMIN_SENHAMudou() {
        Usuario existente = new Usuario(EMAIL, encoder.encode("senha-antiga-123"));
        when(repositorio.findByEmail(EMAIL)).thenReturn(Optional.of(existente));

        subir("senha-nova-123");

        verify(repositorio).save(existente);
        assertTrue(encoder.matches("senha-nova-123", existente.getSenhaHash()));
    }

    @Test
    void naoMexeNoBancoQuandoASenhaJaEhAMesma() {
        Usuario existente = new Usuario(EMAIL, encoder.encode("senha-atual-123"));
        when(repositorio.findByEmail(EMAIL)).thenReturn(Optional.of(existente));

        subir("senha-atual-123");

        verify(repositorio, never()).save(any());
    }

    @Test
    void semADMIN_SENHANaoFazNada() {
        subir("");

        verify(repositorio, never()).findByEmail(any());
        verify(repositorio, never()).save(any());
    }
}
