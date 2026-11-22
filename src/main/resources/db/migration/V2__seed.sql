-- Stage II prototype seed: two firms (tenants) with the data from the design doc.
-- Firm A holds portfolio P1 (design doc section 3); Firm B exists for the tenant isolation test.
-- Money in cents: EUR 100 = 10000.

-- Demo API keys (documented in the README): only their SHA-256 hash is stored.
-- sha256() is built into PostgreSQL; encode(..., 'hex') turns the bytes into text.
INSERT INTO firms (tenant_id, name, api_key_hash) VALUES
    ('firm-a', 'Firm A', encode(sha256('demo-key-firm-a'::bytea), 'hex')),
    ('firm-b', 'Firm B', encode(sha256('demo-key-firm-b'::bytea), 'hex'));

-- ---------- Firm A ----------

-- Reference data + snapshot prices. ZZ-EQ is deliberately missing (scenario 7: unknown instrument).
INSERT INTO instruments (tenant_id, code, issuer, price_cents) VALUES
    ('firm-a', 'X-EQ',   'X', 10000),     -- EUR 100
    ('firm-a', 'X-BOND', 'X', 100000),    -- EUR 1,000
    ('firm-a', 'O-EQ',   'O', 10000),     -- EUR 100, the "other holdings" of P1
    ('firm-a', 'Y-EQ',   'Y', 10000),     -- EUR 100, known but not held (scenario 6)
    ('firm-a', 'R-EQ',   'R', 10000),     -- EUR 100, restricted list in stage III (scenario 5)
    ('firm-a', 'U-EQ',   'U', NULL);      -- known but unpriced: rule 1 blocks it

-- P1: cash EUR 10,000,000
INSERT INTO portfolios (tenant_id, portfolio_id, cash_cents) VALUES
    ('firm-a', 'P1', 1000000000);

-- NAV = 10,000,000 cash + 6,000,000 X-EQ + 2,500,000 X-BOND + 81,500,000 O-EQ = EUR 100,000,000
-- Issuer X exposure = 6,000,000 + 2,500,000 = EUR 8,500,000 = 8.50%
INSERT INTO holdings (tenant_id, portfolio_id, instrument, quantity) VALUES
    ('firm-a', 'P1', 'X-EQ',   60000),
    ('firm-a', 'P1', 'X-BOND', 2500),
    ('firm-a', 'P1', 'O-EQ',   815000);

-- ---------- Firm B ----------
-- Same instrument code X-EQ, but its own row with its own price: tenants never share data.
-- Firm B has no portfolio P1, so asking for P1 with Firm B's key must give 404.
INSERT INTO instruments (tenant_id, code, issuer, price_cents) VALUES
    ('firm-b', 'X-EQ', 'X', 12000);       -- EUR 120

INSERT INTO portfolios (tenant_id, portfolio_id, cash_cents) VALUES
    ('firm-b', 'B1', 50000000);           -- EUR 500,000

INSERT INTO holdings (tenant_id, portfolio_id, instrument, quantity) VALUES
    ('firm-b', 'B1', 'X-EQ', 1000);
