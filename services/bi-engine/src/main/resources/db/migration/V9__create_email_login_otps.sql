-- Migration para suporte a login e MFA via código temporário (OTP) por e-mail

CREATE TABLE email_login_otps (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_login_otps_user_id ON email_login_otps(user_id);
CREATE INDEX idx_email_login_otps_expires_at ON email_login_otps(expires_at);
