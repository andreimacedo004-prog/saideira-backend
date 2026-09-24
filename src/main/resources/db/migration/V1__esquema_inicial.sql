-- ---------------------------------------------------------------
-- V1 — esquema inicial do Saideira
--
-- O Hibernate NAO cria tabelas (ddl-auto=validate). Toda mudanca de
-- estrutura vira um novo arquivo Vx__descricao.sql nesta pasta, e
-- nunca se edita um arquivo que ja foi aplicado.
-- ---------------------------------------------------------------

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    senha_hash  VARCHAR(255) NOT NULL,
    nome        VARCHAR(255) NOT NULL,
    foto_url    VARCHAR(1024),
    bio         VARCHAR(255),
    criado_em   TIMESTAMP(6)
);

-- ---------------------------------------------------------------
-- Grupos de amigos
-- ---------------------------------------------------------------
CREATE TABLE friend_groups (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(255) NOT NULL,
    criado_por_id   BIGINT NOT NULL REFERENCES users (id),
    codigo_convite  VARCHAR(32) NOT NULL UNIQUE
);

CREATE TABLE friend_group_members (
    group_id  BIGINT NOT NULL REFERENCES friend_groups (id) ON DELETE CASCADE,
    user_id   BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, user_id)
);

CREATE INDEX idx_friend_group_members_user ON friend_group_members (user_id);

-- ---------------------------------------------------------------
-- Desafios (periodo com ranking proprio dentro de um grupo)
-- ---------------------------------------------------------------
CREATE TABLE challenges (
    id             BIGSERIAL PRIMARY KEY,
    group_id       BIGINT NOT NULL REFERENCES friend_groups (id) ON DELETE CASCADE,
    nome           VARCHAR(255) NOT NULL,
    data_inicio    DATE NOT NULL,
    data_fim       DATE NOT NULL,
    criado_por_id  BIGINT NOT NULL REFERENCES users (id),
    criado_em      TIMESTAMP(6),

    CONSTRAINT ck_challenges_periodo CHECK (data_fim >= data_inicio)
);

CREATE INDEX idx_challenges_group ON challenges (group_id, data_inicio DESC);

-- ---------------------------------------------------------------
-- Catalogo de cervejas (compartilhado entre todos os grupos)
-- ---------------------------------------------------------------
CREATE TABLE beers (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(255) NOT NULL,
    estilo         VARCHAR(255),
    cervejaria     VARCHAR(255),
    -- nome|cervejaria normalizados: impede duplicata e serve de indice de busca
    chave_busca    VARCHAR(255) NOT NULL UNIQUE,
    criado_por_id  BIGINT REFERENCES users (id) ON DELETE SET NULL,
    criado_em      TIMESTAMP(6)
);

-- ---------------------------------------------------------------
-- Check-ins
-- ---------------------------------------------------------------
CREATE TABLE check_ins (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL REFERENCES users (id),
    challenge_id       BIGINT NOT NULL REFERENCES challenges (id) ON DELETE CASCADE,
    tipo               VARCHAR(32) NOT NULL,
    local              VARCHAR(255) NOT NULL,
    local_normalizado  VARCHAR(255) NOT NULL,
    foto_url           VARCHAR(1024),
    legenda            VARCHAR(500),
    feito_em           TIMESTAMP(6) NOT NULL,
    criado_em          TIMESTAMP(6)
);

-- Feed e ranking: todos os check-ins de um desafio em ordem de horario
CREATE INDEX idx_check_ins_challenge_feito_em ON check_ins (challenge_id, feito_em DESC);
-- Regra do intervalo minimo: check-ins de um usuario num desafio perto de um horario
CREATE INDEX idx_check_ins_user_challenge_feito_em ON check_ins (user_id, challenge_id, feito_em);

CREATE TABLE check_in_amigos (
    check_in_id  BIGINT NOT NULL REFERENCES check_ins (id) ON DELETE CASCADE,
    user_id      BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (check_in_id, user_id)
);

CREATE TABLE check_in_cervejas (
    check_in_id  BIGINT NOT NULL REFERENCES check_ins (id) ON DELETE CASCADE,
    beer_id      BIGINT NOT NULL REFERENCES beers (id),
    PRIMARY KEY (check_in_id, beer_id)
);

-- ---------------------------------------------------------------
-- Interacoes no feed
-- ---------------------------------------------------------------
CREATE TABLE reactions (
    id           BIGSERIAL PRIMARY KEY,
    check_in_id  BIGINT NOT NULL REFERENCES check_ins (id) ON DELETE CASCADE,
    user_id      BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    tipo         VARCHAR(32) NOT NULL,
    criado_em    TIMESTAMP(6),

    -- Uma reacao de cada tipo por pessoa por check-in
    CONSTRAINT uk_reactions_checkin_user_tipo UNIQUE (check_in_id, user_id, tipo)
);

CREATE TABLE comments (
    id           BIGSERIAL PRIMARY KEY,
    check_in_id  BIGINT NOT NULL REFERENCES check_ins (id) ON DELETE CASCADE,
    user_id      BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    texto        VARCHAR(500) NOT NULL,
    criado_em    TIMESTAMP(6)
);

CREATE INDEX idx_comments_check_in ON comments (check_in_id, criado_em);
