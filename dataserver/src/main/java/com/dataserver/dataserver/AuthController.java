package com.dataserver.dataserver;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record LoginRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido") String email,
        @NotBlank(message = "A senha é obrigatória") String senha
    ) {
        // Espaços colados junto com o e-mail não devem reprovar o @Email
        public LoginRequest {
            email = email == null ? null : email.trim();
        }
    }

    public record LoginResponse(String token, String tipo, String email, Instant expiraEm) {}

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration validadeDoToken;
    // Hash de uma senha qualquer, comparado quando o e-mail não existe: assim a resposta
    // demora o mesmo tanto e não dá para descobrir pelo tempo quais e-mails têm conta.
    private final String hashFalso;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtEncoder jwtEncoder,
                          @Value("${seguranca.jwt.expiracao-horas}") long horasDeValidade) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.validadeDoToken = Duration.ofHours(horasDeValidade);
        this.hashFalso = passwordEncoder.encode("senha-que-nunca-confere");
    }

    // POST /auth/login - confere e-mail e senha e devolve o token (401 se não conferir)
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest dados) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(Usuario.normalizarEmail(dados.email()));
        String hash = usuario.map(Usuario::getSenhaHash).orElse(hashFalso);
        boolean senhaConfere = passwordEncoder.matches(dados.senha(), hash);
        if (usuario.isEmpty() || !senhaConfere) {
            // Mesma mensagem para e-mail inexistente e senha errada (não revela quem tem conta)
            throw new CredenciaisInvalidasException();
        }

        String email = usuario.get().getEmail();
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(validadeDoToken);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("dataserver")
            .subject(email)
            .issuedAt(agora)
            .expiresAt(expiraEm)
            .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();

        return new LoginResponse(token, "Bearer", email, expiraEm);
    }

    // GET /auth/eu - quem está logado (o frontend usa para conferir se o token ainda vale)
    @GetMapping("/eu")
    public Map<String, String> eu(Authentication autenticacao) {
        return Map.of("email", autenticacao.getName());
    }
}
