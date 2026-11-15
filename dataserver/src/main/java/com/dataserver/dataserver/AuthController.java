package com.dataserver.dataserver;

import java.time.Duration;
import java.time.Instant;
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

import jakarta.servlet.http.HttpServletRequest;
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

    // nome pode vir null (usuário sem nome cadastrado): o painel mostra o e-mail
    public record LoginResponse(String token, String tipo, String email, String nome, Instant expiraEm) {}

    public record UsuarioLogado(String email, String nome) {}

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final LimiteDeTentativas limiteDeTentativas;
    private final Duration validadeDoToken;
    // Hash de uma senha qualquer, comparado quando o e-mail não existe: assim a resposta
    // demora o mesmo tanto e não dá para descobrir pelo tempo quais e-mails têm conta.
    private final String hashFalso;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtEncoder jwtEncoder,
                          LimiteDeTentativas limiteDeTentativas,
                          @Value("${seguranca.jwt.expiracao-horas}") long horasDeValidade) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.limiteDeTentativas = limiteDeTentativas;
        this.validadeDoToken = Duration.ofHours(horasDeValidade);
        this.hashFalso = passwordEncoder.encode("senha-que-nunca-confere");
    }

    // POST /auth/login - confere e-mail e senha e devolve o token
    // (401 se não conferir; 429 depois de erros demais seguidos, ver LimiteDeTentativas)
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest dados, HttpServletRequest request) {
        String emailInformado = Usuario.normalizarEmail(dados.email());
        String chave = request.getRemoteAddr() + "|" + emailInformado;
        if (limiteDeTentativas.bloqueado(chave)) {
            throw new MuitasTentativasException();
        }

        Optional<Usuario> usuario = usuarioRepository.findByEmail(emailInformado);
        String hash = usuario.map(Usuario::getSenhaHash).orElse(hashFalso);
        boolean senhaConfere = passwordEncoder.matches(dados.senha(), hash);
        if (usuario.isEmpty() || !senhaConfere) {
            // Mesma mensagem para e-mail inexistente e senha errada (não revela quem tem conta)
            limiteDeTentativas.registrarFalha(chave);
            throw new CredenciaisInvalidasException();
        }
        limiteDeTentativas.limpar(chave);

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

        return new LoginResponse(token, "Bearer", email, usuario.get().getNome(), expiraEm);
    }

    // GET /auth/eu - quem está logado (e-mail e nome)
    @GetMapping("/eu")
    public UsuarioLogado eu(Authentication autenticacao) {
        String email = autenticacao.getName();
        String nome = usuarioRepository.findByEmail(email).map(Usuario::getNome).orElse(null);
        return new UsuarioLogado(email, nome);
    }
}
