package com.dataserver.dataserver;


import jakarta.persistence.*;

// Entidade JPA - mapeia esta classe para a tabela "produtos" no Supabase
@Entity
@Table(name = "produtos")
public class Produto {

    // Chave primária com geração automática de ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Coluna "nome" - não pode ser nula
    @Column(nullable = false)
    private String nome;

    // Coluna "preco" - não pode ser nula
    @Column(nullable = false)
    private Double preco;

    // Construtor padrão obrigatório para o JPA
    public Produto() {}

    // Construtor completo
    public Produto(Long id, String nome, Double preco) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Double getPreco() { return preco; }
    public void setPreco(Double preco) { this.preco = preco; }
}














