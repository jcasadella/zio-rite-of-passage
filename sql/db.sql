CREATE DATABASE reviewboard;
\c reviewboard;

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
    created TIMESTAMP NOT NULL DEFAULT now(),
    updated TIMESTAMP NOT NULL DEFAULT now()
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
