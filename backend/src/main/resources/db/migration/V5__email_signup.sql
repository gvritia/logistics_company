ALTER TABLE public.app_users ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE public.email_login_challenges ADD COLUMN email VARCHAR(254);

UPDATE public.email_login_challenges AS challenge
SET email = lower(account.email)
FROM public.app_users AS account
WHERE account.id = challenge.user_id;

ALTER TABLE public.email_login_challenges DROP CONSTRAINT email_login_challenges_pkey;

ALTER TABLE public.email_login_challenges
    ALTER COLUMN user_id DROP NOT NULL,
    ADD PRIMARY KEY (email);
