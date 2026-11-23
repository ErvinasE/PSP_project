package lt.vilniustech.psp.compliance_checker.service;

import java.util.List;
import java.util.Optional;
import lt.vilniustech.psp.compliance_checker.domain.ComplianceEngine;
import lt.vilniustech.psp.compliance_checker.domain.Decision;
import lt.vilniustech.psp.compliance_checker.domain.Order;
import lt.vilniustech.psp.compliance_checker.domain.PortfolioSnapshot;
import lt.vilniustech.psp.compliance_checker.persistence.PendingOrderRepository;
import lt.vilniustech.psp.compliance_checker.persistence.PortfolioRepository;
import org.springframework.stereotype.Service;

/**
 * Runs one order check from start to finish: load the portfolio and its pending orders,
 * ask the engine for a decision, and remember the order as pending when it was accepted.
 * No HTTP in here (that is the controller's job) and no rules (that is the engine's job).
 */
@Service
public class OrderCheckService {

    private final PortfolioRepository portfolios;
    private final PendingOrderRepository pendingOrders;
    private final ComplianceEngine engine;

    public OrderCheckService(
        PortfolioRepository portfolios,
        PendingOrderRepository pendingOrders,
        ComplianceEngine engine)
    {
        this.portfolios = portfolios;
        this.pendingOrders = pendingOrders;
        this.engine = engine;
    }

    /** Empty when this tenant has no such portfolio (the controller turns that into 404). */
    public Optional<Decision> check(String tenantId, String portfolioId, Order order)
    {
        // 1. The portfolio as it is now. No portfolio for this tenant = nothing to check.
        Optional<PortfolioSnapshot> snapshot = portfolios.loadSnapshot(tenantId, portfolioId);
        if (snapshot.isEmpty())
        {
            return Optional.empty();
        }

        // 2. Orders accepted earlier but not executed yet
        List<Order> pending = pendingOrders.loadPendingOrders(tenantId, portfolioId);

        // 3. The decision: snapshot + pending + this order
        Decision decision = engine.check(order, snapshot.get(), pending);

        // 4. Only an accepted order becomes pending. A BLOCKed order will never execute.
        if (decision == Decision.ALLOW || decision == Decision.WARN)
        {
            pendingOrders.save(tenantId, portfolioId, order);
        }
        return Optional.of(decision);
    }
}
