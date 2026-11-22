package lt.vilniustech.psp.compliance_checker.persistence;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lt.vilniustech.psp.compliance_checker.domain.Money;
import lt.vilniustech.psp.compliance_checker.domain.PortfolioSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads a portfolio from the database and turns it into the domain's PortfolioSnapshot.
 * Plain SQL; every query is filtered by tenant_id, so one firm can never read another firm's rows.
 */
@Repository
public class PortfolioRepository {

    private final JdbcTemplate jdbc;

    public PortfolioRepository(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    /** Empty when this tenant has no such portfolio (also when another tenant has one with that id). */
    public Optional<PortfolioSnapshot> loadSnapshot(String tenantId, String portfolioId)
    {
        // 1. The portfolio itself (cash). No row = not this tenant's portfolio.
        List<Long> cash = jdbc.queryForList(
            "SELECT cash_cents FROM portfolios WHERE tenant_id = ? AND portfolio_id = ?",
            Long.class, tenantId, portfolioId);
        if (cash.isEmpty())
        {
            return Optional.empty();
        }

        // 2. The tenant's reference data: issuer of every known instrument, and its price if it has one.
        Map<String, String> instrumentIssuers = new HashMap<>();
        Map<String, Money> prices = new HashMap<>();
        jdbc.query(
            "SELECT code, issuer, price_cents FROM instruments WHERE tenant_id = ?",
            (ResultSet row) -> {
                String code = row.getString("code");
                instrumentIssuers.put(code, row.getString("issuer"));
                long priceCents = row.getLong("price_cents");
                if (!row.wasNull())
                {
                    prices.put(code, new Money(priceCents));
                }
            },
            tenantId);

        // 3. What this portfolio holds.
        Map<String, Long> holdings = new HashMap<>();
        jdbc.query(
            "SELECT instrument, quantity FROM holdings WHERE tenant_id = ? AND portfolio_id = ?",
            (ResultSet row) -> {
                holdings.put(row.getString("instrument"), row.getLong("quantity"));
            },
            tenantId, portfolioId);

        return Optional.of(new PortfolioSnapshot(instrumentIssuers, prices, holdings, new Money(cash.get(0))));
    }
}
