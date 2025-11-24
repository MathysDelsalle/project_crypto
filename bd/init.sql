CREATE TABLE IF NOT EXISTS crypto_assets (
    id           BIGSERIAL PRIMARY KEY,
    external_id  VARCHAR(255) NOT NULL UNIQUE,
    symbol       VARCHAR(50),
    name         VARCHAR(255),
    current_price DOUBLE PRECISION,
    market_cap    DOUBLE PRECISION,
    total_volume  DOUBLE PRECISION,
    image_url     TEXT
);

