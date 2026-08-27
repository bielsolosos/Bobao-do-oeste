-- ==============================================================================
-- TABELA DE LOGS / HISTÓRICO DE CHAMADAS DE IA (AUDITORIA E CUSTO)
-- ==============================================================================
CREATE TABLE ai_analysis_logs (
    id UUID PRIMARY KEY,
    product_monitor_id UUID REFERENCES product_monitors(id) ON DELETE SET NULL,
    scraping_execution_id UUID REFERENCES scraping_executions(id) ON DELETE SET NULL,
    model_name VARCHAR(100) NOT NULL,
    vendor VARCHAR(50) NOT NULL DEFAULT 'GEMINI',
    items_count INT NOT NULL DEFAULT 0,
    system_prompt TEXT,
    user_prompt TEXT,
    raw_response TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    duration_ms INT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_logs_monitor_id ON ai_analysis_logs(product_monitor_id);
CREATE INDEX idx_ai_logs_execution_id ON ai_analysis_logs(scraping_execution_id);
CREATE INDEX idx_ai_logs_created_at ON ai_analysis_logs(created_at);
