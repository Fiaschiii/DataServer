package com.dataserver.dataserver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Configuração comum dos testes da camada web (sem banco):
// sobe os controllers com as regras de segurança reais (SegurancaConfig),
// um segredo JWT de teste e o repositório de usuários simulado.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@Import(SegurancaConfig.class)
@TestPropertySource(properties = {
    "JWT_SECRET=segredo-usado-somente-nos-testes-0123456789",
    "JWT_EXPIRACAO_HORAS=8",
})
@MockitoBean(types = UsuarioRepository.class)
@interface TesteDaApi {
}
