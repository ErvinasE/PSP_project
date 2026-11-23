package lt.vilniustech.psp.compliance_checker.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lt.vilniustech.psp.compliance_checker.domain.Money;
import lt.vilniustech.psp.compliance_checker.domain.PortfolioSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
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
        List<Long> cashRows = jdbc.queryForList(
            "SELECT cash_cents FROM portfolios WHERE tenant_id = ? AND portfolio_id = ?",
            Long.class, tenantId, portfolioId);
        if (cashRows.isEmpty())
        {
            return Optional.empty();
        }

        // 2. The tenant's reference data: issuer of every known instrument, and its price if it has one.
        Map<String, String> instrumentIssuers = new HashMap<>();
        Map<String, Money> prices = new HashMap<>();
        SqlRowSet instrumentRows = jdbc.queryForRowSet(
            "SELECT code, issuer, price_cents FROM instruments WHERE tenant_id = ?",
            tenantId);
        while (instrumentRows.next())
        {
            String code = instrumentRows.getString("code");
            instrumentIssuers.put(code, instrumentRows.getString("issuer"));
            long priceCents = instrumentRows.getLong("price_cents");
            if (!instrumentRows.wasNull())
            {
                prices.put(code, new Money(priceCents));
            }
        }

        // 3. What this portfolio holds.
        Map<String, Long> holdings = loadHoldings(tenantId, portfolioId);

        // 4. Build the snapshot from the four things we collected
        long cashCents = cashRows.get(0);
        Money cashMoney = new Money(cashCents);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(instrumentIssuers, prices, holdings, cashMoney);
        return Optional.of(snapshot);
    }

    private Map<String, Long> loadHoldings(String tenantId, String portfolioId)
    {
        Map<String, Long> holdings = new HashMap<>();
        SqlRowSet holdingRows = jdbc.queryForRowSet(
            "SELECT instrument, quantity FROM holdings WHERE tenant_id = ? AND portfolio_id = ?",
            tenantId, portfolioId);
        while (holdingRows.next())
        {
            holdings.put(holdingRows.getString("instrument"), holdingRows.getLong("quantity"));
        }
        return holdings;
    }
}
