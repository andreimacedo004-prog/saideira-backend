-- ---------------------------------------------------------------
-- V3 — formato e quantidade de cada cerveja no check-in
--
-- Nao muda pontuacao (o bonus continua sendo por cerveja nova).
-- Serve so para a retrospectiva: formato x quantidade = litros.
--
-- Check-ins que ja existiam ficam como 1 lata.
-- ---------------------------------------------------------------

ALTER TABLE check_in_cervejas
    ADD COLUMN formato    VARCHAR(32) NOT NULL DEFAULT 'LATA',
    ADD COLUMN quantidade INTEGER     NOT NULL DEFAULT 1;

ALTER TABLE check_in_cervejas
    ADD CONSTRAINT ck_check_in_cervejas_quantidade CHECK (quantidade BETWEEN 1 AND 20);
