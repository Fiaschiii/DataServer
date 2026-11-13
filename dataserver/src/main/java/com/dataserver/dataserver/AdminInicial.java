package com.dataserver.dataserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Não existe cadastro aberto de usuários: o primeiro acesso é criado aqui, na subida
// do Java, a partir de ADMIN_EMAIL e ADMIN_SENHA do .env. Se o e-mail já existe,
// nada muda (a senha do banco continua valendo, mesmo que ADMIN_SENHA seja trocada).
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);
    static final int TAMANHO_MINIMO_SENHA = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdminInicial(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${seguranca.admin.email}") String email,
                        @Value("${seguranca.admin.senha}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = Usuario.normalizarEmail(email);
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isEmpty() || senha.isEmpty()) {
            if (usuarioRepository.count() == 0) {
                log.warn("Nenhum usuário cadastrado: defina ADMIN_EMAIL e ADMIN_SENHA no .env "
                    + "e reinicie para criar o primeiro acesso.");
            }
            return;
        }
        if (usuarioRepository.existsByEmail(email)) {
            return;
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalStateException(
                "ADMIN_SENHA deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        usuarioRepository.save(new Usuario(email, passwordEncoder.encode(senha)));
        log.info("Usuário administrador {} criado.", email);
    }
}
