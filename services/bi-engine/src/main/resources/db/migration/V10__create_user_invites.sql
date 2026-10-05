CREATE TABLE IF NOT EXISTS user_invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(150) NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    invited_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    accepted_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_user_invites_token ON user_invites(token);
CREATE INDEX IF NOT EXISTS idx_user_invites_email ON user_invites(email);
CREATE INDEX IF NOT EXISTS idx_user_invites_status ON user_invites(status);
