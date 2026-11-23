package lt.vilniustech.psp.compliance_checker.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import lt.vilniustech.psp.compliance_checker.TestcontainersConfiguration;
import lt.vilniustech.psp.compliance_checker.domain.BasisPoints;
import lt.vilniustech.psp.compliance_checker.domain.ComplianceEngine;
import lt.vilniustech.psp.compliance_checker.domain.DataCompletenessCheck;
import lt.vilniustech.psp.compliance_checker.domain.Decision;
import lt.vilniustech.psp.compliance_checker.domain.DecisionCombiner;
import lt.vilniustech.psp.compliance_checker.domain.IssuerConcentrationRule;
import lt.vilniustech.psp.compliance_checker.domain.Order;
import lt.vilniustech.psp.compliance_checker.domain.PortfolioSnapshot;
import lt.vilniustech.psp.compliance_checker.domain.TradeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real PostgreSQL (Testcontainers), connected as psp_app.
 * {@code @Transactional}: every test is rolled back at the end, so saved orders never leak into other tests.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PendingOrderRepositoryIntegrationTest {

    @Autowired
    private PendingOrderRepository pendingOrders;

    @Autowired
    private PortfolioRepository portfolios;

    @Test
    void noPendingOrdersInTheSeed()
    {
        List<Order> orders = pendingOrders.loadPendingOrders("firm-a", "P1");

        assertThat(orders).isEmpty();
    }

    @Test
    void savedOrderIsLoadedBack()
    {
        pendingOrders.save("firm-a", "P1", new Order("X-EQ", TradeType.BUY, 10_000L));

        List<Order> orders = pendingOrders.loadPendingOrders("firm-a", "P1");

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).instrument()).isEqualTo("X-EQ");
        assertThat(orders.get(0).tradeType()).isEqualTo(TradeType.BUY);
        assertThat(orders.get(0).quantity()).isEqualTo(10_000L);
    }

    @Test
    void anotherFirmDoesNotSeeThePendingOrders()
    {
        pendingOrders.save("firm-a", "P1", new Order("X-EQ", TradeType.BUY, 10_000L));

        List<Order> orders = pendingOrders.loadPendingOrders("firm-b", "P1");

        assertThat(orders).isEmpty();
    }

    @Test
    void databaseRejectsAPendingOrderForAnotherFirmsPortfolio()
    {
        // Composite foreign key (tenant_id, portfolio_id): firm-b has no portfolio P1
        Order order = new Order("X-EQ", TradeType.BUY, 10_000L);

        assertThatThrownBy(() -> pendingOrders.save("firm-b", "P1", order))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void scenario4ThroughTheDatabase()
    {
        // Order A was accepted (WARN, 9.50%) and saved as pending. Order B, identical, arrives:
        // 8.5M + 1M (pending A, read from the DB) + 1M = 10.50% -> BLOCK.
        ComplianceEngine engine = new ComplianceEngine(
            new DataCompletenessCheck(),
            List.of(new IssuerConcentrationRule(new BasisPoints(1000), new BasisPoints(900))),
            new DecisionCombiner());
        Order orderA = new Order("X-EQ", TradeType.BUY, 10_000L);
        Order orderB = new Order("X-EQ", TradeType.BUY, 10_000L);
        pendingOrders.save("firm-a", "P1", orderA);

        PortfolioSnapshot snapshot = portfolios.loadSnapshot("firm-a", "P1").orElseThrow();
        Decision decisionB = engine.check(orderB, snapshot, pendingOrders.loadPendingOrders("firm-a", "P1"));

        assertThat(decisionB).isEqualTo(Decision.BLOCK);
    }
}
