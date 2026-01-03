CREATE TABLE IF NOT EXISTS crypto_assets (
    id           BIGSERIAL PRIMARY KEY,
    external_id  VARCHAR(255) NOT NULL UNIQUE,
    symbol       VARCHAR(50),
    name         VARCHAR(255),
    current_price DOUBLE PRECISION,
    market_cap    DOUBLE PRECISION,
    total_volume  DOUBLE PRECISION,
    price_change_24h DOUBLE PRECISION,
    image_url     TEXT,
    market_cap_rank INTEGER
);

-- Table pour les utilisateurs
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE
);

-- Table pour les rôles
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Table de jointure pour les rôles des utilisateurs
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);