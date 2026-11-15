-- КР3 расширяет существующую схему КР1, не меняя числовые ключи КР1/КР2.

ALTER TABLE public.clients
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE public.transportation_requests
    ADD COLUMN public_id UUID DEFAULT gen_random_uuid(),
    ADD COLUMN request_code VARCHAR(40),
    ADD COLUMN fragile BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN updated_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE public.transportation_requests
SET public_id = gen_random_uuid()
WHERE public_id IS NULL;

UPDATE public.transportation_requests
SET request_code = 'TR-' || repeat('0', greatest(0, 6 - length(id::text))) || id::text,
    updated_at = created_at;

ALTER TABLE public.transportation_requests
    ALTER COLUMN public_id SET NOT NULL,
    ALTER COLUMN request_code SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT LOCALTIMESTAMP;

ALTER TABLE public.transportation_requests
    ADD CONSTRAINT chk_transportation_requests_code_not_blank
        CHECK (btrim(request_code) <> '');

CREATE UNIQUE INDEX uq_transportation_requests_public_id
    ON public.transportation_requests (public_id);
CREATE UNIQUE INDEX uq_transportation_requests_code
    ON public.transportation_requests (request_code);

-- JDBC-клиент КР2 не передаёт request_code и version. BEFORE INSERT видит
-- id, который уже выдал identity-счётчик, и назначает код для такой вставки.
CREATE FUNCTION public.kr3_request_insert_defaults()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.public_id IS NULL THEN
        NEW.public_id := gen_random_uuid();
    END IF;
    IF NEW.request_code IS NULL THEN
        NEW.request_code := 'TR-' || repeat('0', greatest(0, 6 - length(NEW.id::text))) || NEW.id::text;
    END IF;
    IF NEW.updated_at IS NULL THEN
        NEW.updated_at := COALESCE(NEW.created_at, LOCALTIMESTAMP);
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_kr3_request_insert_defaults
BEFORE INSERT ON public.transportation_requests
FOR EACH ROW EXECUTE FUNCTION public.kr3_request_insert_defaults();

-- Hibernate сам передаёт version + 1; прежний JDBC-клиент не передаёт version.
-- При JDBC UPDATE триггер повышает его ровно на один.
CREATE FUNCTION public.kr3_request_update_version()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.version = OLD.version THEN
        NEW.version := OLD.version + 1;
    ELSIF NEW.version <> OLD.version + 1 THEN
        RAISE EXCEPTION 'Invalid request version increment' USING ERRCODE = '23514';
    END IF;
    NEW.updated_at := LOCALTIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_kr3_request_update_version
BEFORE UPDATE ON public.transportation_requests
FOR EACH ROW EXECUTE FUNCTION public.kr3_request_update_version();

CREATE FUNCTION public.kr3_client_update_version()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.version = OLD.version THEN
        NEW.version := OLD.version + 1;
    ELSIF NEW.version <> OLD.version + 1 THEN
        RAISE EXCEPTION 'Invalid client version increment' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_kr3_client_update_version
BEFORE UPDATE ON public.clients
FOR EACH ROW EXECUTE FUNCTION public.kr3_client_update_version();

CREATE TABLE public.app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_app_users_email_not_blank CHECK (btrim(email) <> ''),
    CONSTRAINT chk_app_users_display_name_not_blank CHECK (btrim(display_name) <> ''),
    CONSTRAINT chk_app_users_failed_attempts CHECK (failed_login_attempts >= 0)
);

CREATE UNIQUE INDEX uq_app_users_email_lower ON public.app_users (lower(email));

CREATE TABLE public.app_user_roles (
    user_id UUID NOT NULL REFERENCES public.app_users (id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT chk_app_user_roles_role CHECK (role IN ('VIEWER', 'EDITOR', 'ADMIN'))
);

CREATE TABLE public.refresh_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES public.app_users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT chk_refresh_sessions_expiration CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_sessions_user_id ON public.refresh_sessions (user_id);
CREATE INDEX idx_refresh_sessions_expires_at ON public.refresh_sessions (expires_at);

CREATE TABLE public.transportation_request_tags (
    request_id BIGINT NOT NULL REFERENCES public.transportation_requests (id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (request_id, tag),
    CONSTRAINT chk_transportation_request_tags_not_blank CHECK (btrim(tag) <> '')
);

CREATE INDEX idx_transportation_request_tags_tag ON public.transportation_request_tags (tag);

CREATE TABLE public.transportation_request_notes (
    id UUID PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES public.transportation_requests (id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES public.app_users (id) ON DELETE RESTRICT,
    body VARCHAR(4000) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_transportation_request_notes_body_not_blank CHECK (btrim(body) <> '')
);

CREATE INDEX idx_transportation_request_notes_request_id
    ON public.transportation_request_notes (request_id, created_at DESC);
CREATE INDEX idx_transportation_request_notes_author_id
    ON public.transportation_request_notes (author_id);

CREATE TABLE public.audit_events (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES public.app_users (id) ON DELETE SET NULL,
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80),
    entity_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    description VARCHAR(500) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    CONSTRAINT chk_audit_events_type_not_blank CHECK (btrim(event_type) <> ''),
    CONSTRAINT chk_audit_events_correlation_not_blank CHECK (btrim(correlation_id) <> '')
);

CREATE INDEX idx_audit_events_occurred_at ON public.audit_events (occurred_at DESC);
CREATE INDEX idx_audit_events_user_id ON public.audit_events (user_id);
CREATE INDEX idx_audit_events_event_type ON public.audit_events (event_type);
CREATE INDEX idx_audit_events_entity ON public.audit_events (entity_type, entity_id);
CREATE INDEX idx_audit_events_correlation_id ON public.audit_events (correlation_id);

CREATE INDEX idx_transportation_requests_archived
    ON public.transportation_requests (archived);
CREATE INDEX idx_transportation_requests_fragile
    ON public.transportation_requests (fragile);
CREATE INDEX idx_transportation_requests_weight_kg
    ON public.transportation_requests (weight_kg);
CREATE INDEX idx_transportation_requests_price
    ON public.transportation_requests (price);
CREATE INDEX idx_transportation_requests_created_at
    ON public.transportation_requests (created_at DESC);

-- Поиск с ILIKE '%строка%' может использовать GIN-индексы trigram.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_transportation_requests_cargo_search
    ON public.transportation_requests USING gin (cargo_description gin_trgm_ops);
CREATE INDEX idx_transportation_requests_pickup_search
    ON public.transportation_requests USING gin (pickup_address gin_trgm_ops);
CREATE INDEX idx_transportation_requests_delivery_search
    ON public.transportation_requests USING gin (delivery_address gin_trgm_ops);
CREATE INDEX idx_clients_full_name_search
    ON public.clients USING gin (full_name gin_trgm_ops);
