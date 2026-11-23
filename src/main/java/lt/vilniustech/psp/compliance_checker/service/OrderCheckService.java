package lt.vilniustech.psp.compliance_checker.service;

import lt.vilniustech.psp.compliance_checker.domain.ComplianceEngine;
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
}
