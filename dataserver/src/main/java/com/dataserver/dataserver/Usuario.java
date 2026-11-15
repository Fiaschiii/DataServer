package com.dataserver.dataserver;

import jakarta.persistence.*;

// Entidade JPA - mapeia a tabela "usuarios" (criada pelo script database/usuarios.sql).
// A senha nunca é guardada em texto puro: só o hash BCrypt.
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sempre salvo em minúsculas e sem espaços (ver normalizarEmail)
    @Column(nullable = false, unique = true)
    private String email;

    // Nome exibido no painel (opcional: sem ele, o painel mostra o e-mail)
    @Column(length = 100)
    private String nome;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    // Construtor padrão obrigatório para o JPA
    public Usuario() {}

    public Usuario(String email, String senhaHash) {
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
    }

    // "  Fulano@Email.com " e "fulano@email.com" são o mesmo usuário
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSenhaHash() { return senhaHash; }

    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
}
