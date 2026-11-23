package lt.vilniustech.psp.compliance_checker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import lt.vilniustech.psp.compliance_checker.TestcontainersConfiguration;
import lt.vilniustech.psp.compliance_checker.domain.Decision;
import lt.vilniustech.psp.compliance_checker.domain.Order;
import lt.vilniustech.psp.compliance_checker.domain.TradeType;
import lt.vilniustech.psp.compliance_checker.persistence.PendingOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * The whole flow with a real PostgreSQL (Testcontainers) and the seed from V2.
 * {@code @Transactional}: every test is rolled back at the end, so saved orders never leak into other tests.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class OrderCheckServiceIntegrationTest {

    @Autowired
    private OrderCheckService service;

    @Autowired
    private PendingOrderRepository pendingOrders;

    @Test
    void scenario4SecondIdenticalOrderIsBlockedBecauseTheFirstIsPending()
    {
        // Order A: BUY 10,000 X-EQ (EUR 1,000,000): 8.5M + 1M = 9.50% -> WARN, saved as pending.
        // Order B, identical: 8.5M + 1M (pending A, from the DB) + 1M = 10.50% -> BLOCK.
        Order orderA = new Order("X-EQ", TradeType.BUY, 10_000L);
        Order orderB = new Order("X-EQ", TradeType.BUY, 10_000L);

        Optional<Decision> decisionA = service.check("firm-a", "P1", orderA);
        Optional<Decision> decisionB = service.check("firm-a", "P1", orderB);

        assertThat(decisionA).contains(Decision.WARN);
        assertThat(decisionB).contains(Decision.BLOCK);
    }

    @Test
    void anotherFirmsPortfolioIsNotFound()
    {
        // P1 belongs to firm-a. For firm-b it must look exactly like a portfolio that does not exist.
        Order order = new Order("X-EQ", TradeType.BUY, 100L);

        Optional<Decision> decision = service.check("firm-b", "P1", order);

        assertThat(decision).isEmpty();
    }

    @Test
    void blockedOrderIsNotSavedAsPending()
    {
        // Scenario 3: BUY 15,001 X-EQ is 10.0001% -> BLOCK. A blocked order will never execute,
        // so it must not take room under the limit for later orders.
        Order order = new Order("X-EQ", TradeType.BUY, 15_001L);

        Optional<Decision> decision = service.check("firm-a", "P1", order);

        assertThat(decision).contains(Decision.BLOCK);
        assertThat(pendingOrders.loadPendingOrders("firm-a", "P1")).isEmpty();
    }
}
