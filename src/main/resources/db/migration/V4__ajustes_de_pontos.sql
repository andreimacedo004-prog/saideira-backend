-- ---------------------------------------------------------------
-- V4 — ajustes de pontos feitos pelo admin
--
-- Um ajuste soma (ou tira) pontos de uma pessoa num desafio, sempre
-- com um motivo. Aparece no ranking para a galera ver.
-- Sai junto se o desafio ou a pessoa forem apagados.
-- ---------------------------------------------------------------

CREATE TABLE ajustes_pontos (
    id             BIGSERIAL PRIMARY KEY,
    challenge_id   BIGINT NOT NULL REFERENCES challenges (id) ON DELETE CASCADE,
    user_id        BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    pontos         INTEGER NOT NULL,
    motivo         VARCHAR(200) NOT NULL,
    criado_por_id  BIGINT REFERENCES users (id) ON DELETE SET NULL,
    criado_em      TIMESTAMP(6) NOT NULL,

    CONSTRAINT ck_ajustes_pontos_valor CHECK (pontos <> 0 AND pontos BETWEEN -1000 AND 1000)
);

CREATE INDEX idx_ajustes_pontos_challenge ON ajustes_pontos (challenge_id);
