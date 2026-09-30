-- Migration para suporte a notificações e canais de disparo (Discord / Email)

-- 1. Criação da tabela de logs de notificações enviadas
CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel VARCHAR(25) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_logs_recipient_id ON notification_logs(recipient_id);

-- 2. Configurações de canais de notificação na tabela user_configs
ALTER TABLE user_configs ADD COLUMN IF NOT EXISTS discord_webhook_url VARCHAR(500);
ALTER TABLE user_configs ADD COLUMN IF NOT EXISTS discord_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE user_configs ADD COLUMN IF NOT EXISTS email_enabled BOOLEAN NOT NULL DEFAULT FALSE;
