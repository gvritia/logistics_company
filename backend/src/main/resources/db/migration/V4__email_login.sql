ALTER TABLE public.app_users ADD COLUMN email_verified_at TIMESTAMPTZ;

CREATE TABLE public.email_login_challenges (
    user_id UUID PRIMARY KEY REFERENCES public.app_users (id) ON DELETE CASCADE,
    auth_version BIGINT NOT NULL,
    enrollment BOOLEAN NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    attempts_left INTEGER NOT NULL CHECK (attempts_left BETWEEN 0 AND 5)
);
