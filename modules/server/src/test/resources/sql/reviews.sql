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
