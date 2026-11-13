package com.dataserver.dataserver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Segurança da API: só /auth/login e /hello são públicos; o resto exige o token
// devolvido pelo login, enviado no cabeçalho "Authorization: Bearer <token>".
// Não há sessão no servidor (stateless): cada requisição se autentica pelo token.
@Configuration
public class SegurancaConfig {

    // HS256 exige uma chave de pelo menos 256 bits
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private final SecretKey chave;

    public SegurancaConfig(@Value("${seguranca.jwt.segredo}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                "JWT_SECRET ausente ou curto demais: defina no .env um valor aleatório com pelo menos "
                + TAMANHO_MINIMO_SEGREDO + " caracteres (veja o .env.example).");
        }
        this.chave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint naoAutenticado = this::responderNaoAutenticado;
        http
            // CSRF protege sessões por cookie; aqui o token vai num cabeçalho, então não se aplica
            .csrf(csrf -> csrf.disable())
            // Usa a configuração de CORS do Spring MVC (DataserverApplication.corsConfigurer)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(rotas -> rotas
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // preflight do navegador
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers("/hello", "/error").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(rs -> rs
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(naoAutenticado))
            .exceptionHandling(e -> e.authenticationEntryPoint(naoAutenticado));
        return http.build();
    }

    // 401 no mesmo formato JSON dos outros erros da API (ver TratadorDeErros)
    private void responderNaoAutenticado(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException ex) throws IOException {
        String erro = ex instanceof OAuth2AuthenticationException
            ? "Sessão inválida ou expirada. Faça login novamente."
            : "Login necessário para acessar este recurso.";
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":401,\"erro\":\"" + erro + "\",\"campos\":{}}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Assina os tokens gerados no login
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    // Confere assinatura e validade (exp) do token de cada requisição
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
