package com.dataserver.dataserver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repositório JPA - fornece automaticamente métodos como findAll(), save(), deleteById()
@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}