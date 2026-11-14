package com.dataserver.dataserver;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// No Render, toda requisição chega de um servidor da Cloudflare (IP da conexão muda a
// cada vez); o IP real do visitante vem no CF-Connecting-IP. Aqui o IP da conexão é
// sempre diferente, como lá, e o limite precisa contar pelo cabeçalho.
@TesteDaApi
@TestPropertySource(properties = "CABECALHO_IP_CLIENTE=CF-Connecting-IP")
class IpDoClienteAtrasDoProxyTests {

    private static final String EMAIL = "admin@dataserver.com";
    private static final String SENHA = "senha-correta-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository; // simulado (ver TesteDaApi)

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LimiteDeTentativas limiteDeTentativas;

    // Os controllers dependem do repositório de produtos; aqui ele é simulado (sem banco)
    @MockitoBean
    private ProdutoRepository produtoRepository;

    private int conexao = 0;

    @BeforeEach
    void preparar() {
        limiteDeTentativas.limparTudo();
        when(usuarioRepository.findByEmail(EMAIL))
            .thenReturn(Optional.of(new Usuario(EMAIL, passwordEncoder.encode(SENHA))));
    }

    // Cada chamada sai de um "servidor da Cloudflare" diferente
    private ResultActions tentarLogin(String ipVisitante, String senha) throws Exception {
        String ipDaConexao = "172.70.0." + (++conexao);
        var requisicao = post("/auth/login")
            .with(r -> { r.setRemoteAddr(ipDaConexao); return r; })
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + EMAIL + "\",\"senha\":\"" + senha + "\"}");
        if (ipVisitante != null) {
            requisicao.header("CF-Connecting-IP", ipVisitante);
        }
        return mockMvc.perform(requisicao);
    }

    @Test
    void contaAsFalhasPeloIpDoCabecalhoMesmoComAConexaoMudando() throws Exception {
        for (int i = 0; i < LimiteDeTentativas.MAX_FALHAS; i++) {
            tentarLogin("203.0.113.7", "senha-errada").andExpect(status().isUnauthorized());
        }
        tentarLogin("203.0.113.7", SENHA).andExpect(status().isTooManyRequests());

        // Outro visitante, mesmo e-mail: continua entrando
        tentarLogin("198.51.100.9", SENHA).andExpect(status().isOk());
    }

    @Test
    void semOCabecalhoUsaOIpDaConexao() throws Exception {
        // Sem CF-Connecting-IP, cada conexão diferente conta separado (comportamento local)
        for (int i = 0; i < LimiteDeTentativas.MAX_FALHAS; i++) {
            tentarLogin(null, "senha-errada").andExpect(status().isUnauthorized());
        }
        tentarLogin(null, SENHA).andExpect(status().isOk());
    }
}
