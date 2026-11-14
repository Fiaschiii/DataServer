package com.dataserver.dataserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Não existe cadastro aberto de usuários: o administrador é criado aqui, na subida
// do Java, a partir de ADMIN_EMAIL e ADMIN_SENHA do .env.
// ADMIN_SENHA é a fonte da verdade: se o e-mail já existe com outra senha, o hash do
// banco é atualizado. Assim trocar a variável e reiniciar basta para trocar a senha.
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
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalStateException(
                "ADMIN_SENHA deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        Usuario existente = usuarioRepository.findByEmail(email).orElse(null);
        if (existente == null) {
            usuarioRepository.save(new Usuario(email, passwordEncoder.encode(senha)));
            log.info("Usuário administrador {} criado.", email);
            return;
        }
        if (!passwordEncoder.matches(senha, existente.getSenhaHash())) {
            existente.setSenhaHash(passwordEncoder.encode(senha));
            usuarioRepository.save(existente);
            log.info("Senha do administrador {} atualizada a partir de ADMIN_SENHA.", email);
        }
    }
}
