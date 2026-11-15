-- Пароль меняется — ранее выданные access JWT должны стать недействительными.
ALTER TABLE public.app_users
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
