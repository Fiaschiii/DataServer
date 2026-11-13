package com.dataserver.dataserver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

// Login e proteção das rotas, com tokens JWT reais (sem banco: repositórios simulados).
@TesteDaApi
class AuthControllerTests {

    private static final String EMAIL = "admin@dataserver.com";
    private static final String SENHA = "senha-correta-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository; // simulado (ver TesteDaApi)

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private ProdutoRepository produtoRepository;

    @BeforeEach
    void cadastrarAdmin() {
        Usuario admin = new Usuario(EMAIL, passwordEncoder.encode(SENHA));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));
        when(produtoRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
    }

    private String fazerLogin(String email, String senha) throws Exception {
        String resposta = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    // ---------- POST /auth/login ----------

    @Test
    void loginCorretoDevolveTokenQueAbreAApi() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\",\"senha\":\"" + SENHA + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("Bearer"))
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.expiraEm").isNotEmpty());

        String token = fazerLogin(EMAIL, SENHA);

        mockMvc.perform(get("/produtos").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        mockMvc.perform(get("/auth/eu").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void emailComMaiusculasEEspacosEhOMesmoUsuario() throws Exception {
        fazerLogin("  Admin@DataServer.com ", SENHA);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "senha errada       | admin@dataserver.com  | senha-errada",
        "e-mail inexistente | ninguem@dataserver.com | senha-correta-123",
    })
    void credenciaisErradasDevolvem401SemDizerQualEstaErrado(String caso, String email, String senha) throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.erro").value("E-mail ou senha inválidos"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "sem e-mail       | {\"senha\":\"x\"}                        | email | O e-mail é obrigatório",
        "e-mail inválido  | {\"email\":\"abc\",\"senha\":\"x\"}       | email | E-mail inválido",
        "sem senha        | {\"email\":\"a@b.com\"}                  | senha | A senha é obrigatória",
    })
    void loginComDadosInvalidosDevolve400(String caso, String json, String campo, String mensagem) throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Dados inválidos"))
            .andExpect(jsonPath("$.campos." + campo).value(mensagem));
    }

    // ---------- Rotas protegidas ----------

    @ParameterizedTest
    @ValueSource(strings = {"/produtos", "/produtos/1", "/relatorio", "/auth/eu"})
    void semTokenDevolve401EmJson(String rota) throws Exception {
        mockMvc.perform(get(rota))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.erro").value("Login necessário para acessar este recurso."));
    }

    @Test
    void cadastroSemTokenEhBloqueado() throws Exception {
        mockMvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"X\",\"preco\":10}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAdulteradoDevolve401() throws Exception {
        String token = fazerLogin(EMAIL, SENHA);
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";

        mockMvc.perform(get("/produtos").header("Authorization", "Bearer " + adulterado))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.erro").value("Sessão inválida ou expirada. Faça login novamente."));
    }

    @Test
    void tokenExpiradoDevolve401() throws Exception {
        Instant ontem = Instant.now().minus(1, ChronoUnit.DAYS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(EMAIL)
            .issuedAt(ontem.minus(8, ChronoUnit.HOURS))
            .expiresAt(ontem)
            .build();
        String expirado = jwtEncoder.encode(
            JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/produtos").header("Authorization", "Bearer " + expirado))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.erro").value("Sessão inválida ou expirada. Faça login novamente."));
    }

    @Test
    void helloContinuaPublico() throws Exception {
        mockMvc.perform(get("/hello")).andExpect(status().isOk());
    }
}
