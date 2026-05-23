package com.dataserver.dataserver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


// Repositório JPA - fornece automaticamente métodos como findAll(), save(), deleteById()
@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
Page<Produto> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

}