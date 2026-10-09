CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    url TEXT NOT NULL,
    location TEXT,
    country TEXT,
    industry TEXT,
    image TEXT,
    tags TEXT[]
);

CREATE INDEX idx_companies_tags_gin ON companies USING GIN (tags);

CREATE TABLE IF NOT EXISTS reviews
(
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL, -- REFERENCES companies(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL, -- REFERENCES users(id) ON DELETE CASCADE,
    management INT NOT NULL,
    culture INT NOT NULL,
    salary INT NOT NULL,
    benefits INT NOT NULL,
    would_recommend INT NOT NULL,
    review TEXT NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users
(
    id BIGSERIAL PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    hashed_password TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS recovery_tokens
(
    email TEXT PRIMARY KEY,
    token TEXT NOT NULL,
    expiration BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS invites
(
    id BIGSERIAL PRIMARY KEY,
    user_email TEXT NOT NULL,
    company_id BIGINT NOT NULL,
    n_invites INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS review_summaries
(
    company_id BIGINT NOT NULL PRIMARY KEY,
    contents TEXT NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE invites
ADD CONSTRAINT uq_company_user_email UNIQUE (company_id, user_email);
