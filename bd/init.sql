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

-- Historique de prix (timeseries) pour les cryptos
CREATE TABLE IF NOT EXISTS crypto_price_history (
    id            BIGSERIAL PRIMARY KEY,
    asset_id      BIGINT NOT NULL,
    vs_currency   VARCHAR(10) NOT NULL DEFAULT 'usd',
    ts            TIMESTAMPTZ NOT NULL,
    price         DOUBLE PRECISION NOT NULL,
    market_cap    DOUBLE PRECISION,
    total_volume  DOUBLE PRECISION,

    CONSTRAINT fk_price_history_asset
        FOREIGN KEY (asset_id)
        REFERENCES crypto_assets(id)
        ON DELETE CASCADE,

    -- évite d’insérer deux fois le même point temporel pour un asset/devise
    CONSTRAINT uq_price_history_point UNIQUE (asset_id, vs_currency, ts)
);

-- Index utiles pour requêter une période rapidement
CREATE INDEX IF NOT EXISTS idx_price_history_asset_ts
    ON crypto_price_history (asset_id, ts DESC);

CREATE INDEX IF NOT EXISTS idx_price_history_ts
    ON crypto_price_history (ts DESC);

-- ✅ Solde utilisateur
ALTER TABLE users
ADD COLUMN IF NOT EXISTS balance DOUBLE PRECISION NOT NULL DEFAULT 0;

-- ✅ Favoris : relation user <-> crypto_assets
CREATE TABLE IF NOT EXISTS user_favorites (
  user_id  BIGINT NOT NULL,
  asset_id BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, asset_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (asset_id) REFERENCES crypto_assets(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_favorites_user ON user_favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_user_favorites_asset ON user_favorites(asset_id);

CREATE TABLE IF NOT EXISTS price_alerts (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  asset_id BIGINT NOT NULL REFERENCES crypto_assets(id) ON DELETE CASCADE,
  threshold_high NUMERIC NULL,
  threshold_low  NUMERIC NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  last_triggered_high_at TIMESTAMPTZ NULL,
  last_triggered_low_at  TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(user_id, asset_id)
);

-- Table des positions (holdings)
CREATE TABLE IF NOT EXISTS user_holdings (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  asset_id BIGINT NOT NULL,
  quantity DOUBLE PRECISION NOT NULL DEFAULT 0,

  CONSTRAINT fk_user_holdings_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

  CONSTRAINT fk_user_holdings_asset
    FOREIGN KEY (asset_id) REFERENCES crypto_assets(id) ON DELETE CASCADE,

  CONSTRAINT uq_user_asset UNIQUE (user_id, asset_id)
);

CREATE INDEX IF NOT EXISTS idx_user_holdings_user_id ON user_holdings(user_id);
CREATE INDEX IF NOT EXISTS idx_user_holdings_asset_id ON user_holdings(asset_id);
