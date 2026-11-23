package lt.vilniustech.psp.compliance_checker.persistence;

import java.util.ArrayList;
import java.util.List;
import lt.vilniustech.psp.compliance_checker.domain.Order;
import lt.vilniustech.psp.compliance_checker.domain.TradeType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

/**
 * Orders that were accepted (ALLOW or WARN) but not executed yet.
 * Deciding WHEN to save one is not this class's job; it only stores and reads them, filtered by tenant_id.
 */
@Repository
public class PendingOrderRepository {

    private final JdbcTemplate jdbc;

    public PendingOrderRepository(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    public void save(String tenantId, String portfolioId, Order order)
    {
        jdbc.update(
            "INSERT INTO pending_orders (tenant_id, portfolio_id, instrument, trade_type, quantity)"
                + " VALUES (?, ?, ?, ?, ?)",
            tenantId, portfolioId, order.instrument(), order.tradeType().name(), order.quantity());
    }

    public List<Order> loadPendingOrders(String tenantId, String portfolioId)
    {
        SqlRowSet rows = jdbc.queryForRowSet(
            "SELECT instrument, trade_type, quantity FROM pending_orders WHERE tenant_id = ? AND portfolio_id = ?",
            tenantId, portfolioId);

        // One row = one Order
        List<Order> orders = new ArrayList<>();
        while (rows.next())
        {
            String instrument = rows.getString("instrument");
            TradeType tradeType = TradeType.valueOf(rows.getString("trade_type"));
            long quantity = rows.getLong("quantity");
            orders.add(new Order(instrument, tradeType, quantity));
        }
        return orders;
    }
}
