package com.dataserver.dataserver;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // O e-mail deve chegar já normalizado (Usuario.normalizarEmail)
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
