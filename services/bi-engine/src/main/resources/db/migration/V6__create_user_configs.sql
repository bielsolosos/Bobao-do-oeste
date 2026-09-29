-- Tabela de Configurações de Usuário
CREATE TABLE user_configs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    ai_vendor VARCHAR(50) NOT NULL DEFAULT 'GEMINI',
    cheap_model VARCHAR(100) NOT NULL DEFAULT 'gemini-2.5-flash',
    strong_model VARCHAR(100) NOT NULL DEFAULT 'gemini-2.5-pro',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Índice de busca rápida por usuário
CREATE INDEX idx_user_configs_user_id ON user_configs(user_id);

-- Inserção de configurações padrão para usuários já existentes
INSERT INTO user_configs (id, user_id, ai_vendor, cheap_model, strong_model, created_at, updated_at)
SELECT gen_random_uuid(), id, 'GEMINI', 'gemini-2.5-flash', 'gemini-2.5-pro', NOW(), NOW()
FROM users;
