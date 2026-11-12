package com.dataserver.dataserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zaxxer.hikari.HikariDataSource;

// Garante que o Java não ocupa mais conexões do que o Supabase permite
// (limite de 15 no total no modo sessão, compartilhado com o Python).
@SpringBootTest
class PoolDeConexoesTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void poolLimitadoA5ConexoesEMantem1Ociosa() {
        HikariDataSource hikari = assertInstanceOf(HikariDataSource.class, dataSource);
        assertEquals(5, hikari.getMaximumPoolSize());
        assertEquals(1, hikari.getMinimumIdle());
    }
}
