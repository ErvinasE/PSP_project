-- Stage II prototype schema. Every table holds tenant data and has tenant_id;
-- every query filters by it. Row-level security is deferred to stage III.
-- Money is stored as BIGINT cents, never as floating point.

-- A firm is a tenant. Only a SHA-256 hash of its API key is stored.
CREATE TABLE firms (
    tenant_id     TEXT PRIMARY KEY,
    name          TEXT NOT NULL,
    api_key_hash  TEXT NOT NULL UNIQUE
);

CREATE TABLE portfolios (
    tenant_id     TEXT   NOT NULL REFERENCES firms (tenant_id),
    portfolio_id  TEXT   NOT NULL,
    cash_cents    BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, portfolio_id)
);

-- Reference data + snapshot price. A missing row = unknown instrument;
-- NULL price = known but unpriced (both make rule 1 block).
CREATE TABLE instruments (
    tenant_id     TEXT   NOT NULL REFERENCES firms (tenant_id),
    code          TEXT   NOT NULL,
    issuer        TEXT   NOT NULL,
    price_cents   BIGINT CHECK (price_cents > 0),
    PRIMARY KEY (tenant_id, code)
);

CREATE TABLE holdings (
    tenant_id     TEXT   NOT NULL,
    portfolio_id  TEXT   NOT NULL,
    instrument    TEXT   NOT NULL,
    quantity      BIGINT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (tenant_id, portfolio_id, instrument),
    FOREIGN KEY (tenant_id, portfolio_id) REFERENCES portfolios (tenant_id, portfolio_id),
    FOREIGN KEY (tenant_id, instrument)   REFERENCES instruments (tenant_id, code)
);

-- Orders that were checked and accepted (ALLOW or WARN) but not executed yet.
CREATE TABLE pending_orders (
    order_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     TEXT   NOT NULL,
    portfolio_id  TEXT   NOT NULL,
    instrument    TEXT   NOT NULL,
    trade_type    TEXT   NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
    quantity      BIGINT NOT NULL CHECK (quantity > 0),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (tenant_id, portfolio_id) REFERENCES portfolios (tenant_id, portfolio_id),
    FOREIGN KEY (tenant_id, instrument)   REFERENCES instruments (tenant_id, code)
);
