-- Tabela de usuários do login (rodar uma vez no SQL Editor do Supabase).
-- O Java não cria tabelas sozinho (ddl-auto=validate): sem ela, ele não sobe.
-- A senha fica só como hash BCrypt; o primeiro usuário é criado pelo Java a
-- partir de ADMIN_EMAIL / ADMIN_SENHA / ADMIN_NOME do .env.
CREATE TABLE IF NOT EXISTS usuarios (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    nome       VARCHAR(100),
    senha_hash VARCHAR(100) NOT NULL,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- O Supabase expõe as tabelas do schema public pela API REST dele (PostgREST).
-- Com RLS ligado e nenhuma policy, ninguém lê os hashes por esse caminho; o Java
-- conecta como dono da tabela e não é afetado.
ALTER TABLE usuarios ENABLE ROW LEVEL SECURITY;

-- Para quem já tinha a tabela (criada antes do campo nome): acrescenta a coluna.
-- Pode rodar mais de uma vez sem problema.
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS nome VARCHAR(100);
