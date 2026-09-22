-- ==============================================================================
-- Migration V5: Adicionando coluna required_delivery na tabela product_monitors
-- ==============================================================================

ALTER TABLE product_monitors
ADD COLUMN required_delivery BOOLEAN NOT NULL DEFAULT FALSE;
