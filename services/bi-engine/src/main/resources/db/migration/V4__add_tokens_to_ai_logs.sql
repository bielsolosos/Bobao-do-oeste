-- ==============================================================================
-- Migration V4: Adicionando colunas de rastreamento de Tokens (Billing)
-- ==============================================================================

ALTER TABLE ai_analysis_logs
ADD COLUMN prompt_tokens INT,
ADD COLUMN generation_tokens INT,
ADD COLUMN total_tokens INT;
