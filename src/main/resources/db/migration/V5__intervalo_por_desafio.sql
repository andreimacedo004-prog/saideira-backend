-- ---------------------------------------------------------------
-- V5 — intervalo entre check-ins por desafio
--
-- Vazio = vale o padrao do app (CHECKIN_INTERVALO_MINUTOS, 2h).
-- O admin pode encurtar ou alongar so num desafio, ex.: 30 min num show.
-- ---------------------------------------------------------------
ALTER TABLE challenges
    ADD COLUMN intervalo_minimo_minutos INTEGER
        CHECK (intervalo_minimo_minutos BETWEEN 15 AND 1440);
