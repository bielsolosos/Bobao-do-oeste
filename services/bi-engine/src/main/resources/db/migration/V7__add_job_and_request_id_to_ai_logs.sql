-- ==============================================================================
-- Migration V7: Adicionando colunas de rastreamento de Job ID e Request ID
-- ==============================================================================

ALTER TABLE ai_analysis_logs
ADD COLUMN request_id VARCHAR(255),
ADD COLUMN job_id VARCHAR(255);

CREATE INDEX idx_ai_logs_request_id ON ai_analysis_logs(request_id);
CREATE INDEX idx_ai_logs_job_id ON ai_analysis_logs(job_id);
