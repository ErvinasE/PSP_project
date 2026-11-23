package lt.vilniustech.psp.compliance_checker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ComplianceEngineTest {

    // Rule-set from the prototype scope: issuer limit 10%, warning from 9%
    private static final BasisPoints LIMIT = new BasisPoints(1000);
    private static final BasisPoints WARNING = new BasisPoints(900);

    // Portfolio P1 from the design doc, in cents:
    // cash EUR 10,000,000 + issuer X EUR 8,500,000 (60,000 X-EQ at EUR 100 + 2,500 X-BOND at EUR 1,000)
    // + other holdings EUR 81,500,000 (815,000 O-EQ at EUR 100) = NAV EUR 100,000,000.
    // Y-EQ is known and priced but not held (scenario 6).
    private static PortfolioSnapshot portfolioP1()
    {
        Map<String, String> instrumentIssuers = Map.of(
            "X-EQ", "X",
            "X-BOND", "X",
            "O-EQ", "O",
            "Y-EQ", "Y");
        Map<String, Money> prices = Map.of(
            "X-EQ", new Money(10_000L),
            "X-BOND", new Money(100_000L),
            "O-EQ", new Money(10_000L),
            "Y-EQ", new Money(10_000L));
        Map<String, Long> holdings = Map.of(
            "X-EQ", 60_000L,
            "X-BOND", 2_500L,
            "O-EQ", 815_000L);
        Money cash = new Money(1_000_000_000L);
        return new PortfolioSnapshot(instrumentIssuers, prices, holdings, cash);
    }

    private static ComplianceEngine engineWith(ComplianceRule... rules)
    {
        return new ComplianceEngine(new DataCompletenessCheck(), List.of(rules), new DecisionCombiner());
    }

    private static ComplianceRule issuerRule()
    {
        return new IssuerConcentrationRule(LIMIT, WARNING);
    }

    @Test
    void scenario1NormalBuyIsAllowed()
    {
        // BUY 3,000 X-EQ (EUR 300,000): X exposure 8.80%
        Order order = new Order("X-EQ", TradeType.BUY, 3_000L);

        Decision decision = engineWith(issuerRule()).check(order, portfolioP1(), List.of());

        assertThat(decision).isEqualTo(Decision.ALLOW);
    }

    @Test
    void scenario2ExactlyOnTheLimitIsWarn()
    {
        // BUY 15,000 X-EQ (EUR 1,500,000): X exposure exactly 10.00%
        Order order = new Order("X-EQ", TradeType.BUY, 15_000L);

        Decision decision = engineWith(issuerRule()).check(order, portfolioP1(), List.of());

        assertThat(decision).isEqualTo(Decision.WARN);
    }

    @Test
    void scenario3OneShareOverTheLimitIsBlock()
    {
        // BUY 15,001 X-EQ (EUR 1,500,100): X exposure 10.0001%
        Order order = new Order("X-EQ", TradeType.BUY, 15_001L);

        Decision decision = engineWith(issuerRule()).check(order, portfolioP1(), List.of());

        assertThat(decision).isEqualTo(Decision.BLOCK);
    }

    @Test
    void scenario4PendingOrderTurnsTheSecondIdenticalOrderIntoBlock()
    {
        // Order A: BUY 10,000 X-EQ (EUR 1,000,000), checked first: 9.50% -> WARN, becomes pending.
        // Order B: identical, arrives next: 8.5M + 1M (pending A) + 1M = 10.50% -> BLOCK.
        Order orderA = new Order("X-EQ", TradeType.BUY, 10_000L);
        Order orderB = new Order("X-EQ", TradeType.BUY, 10_000L);
        ComplianceEngine engine = engineWith(issuerRule());

        Decision decisionA = engine.check(orderA, portfolioP1(), List.of());
        Decision decisionB = engine.check(orderB, portfolioP1(), List.of(orderA));

        assertThat(decisionA).isEqualTo(Decision.WARN);
        assertThat(decisionB).isEqualTo(Decision.BLOCK);
    }

    @Test
    void pendingSellReducesTheExposure()
    {
        // Pending SELL 10,000 X-EQ (-EUR 1,000,000), then BUY 20,000 X-EQ (+EUR 2,000,000):
        // 8.5M - 1M + 2M = 9.50% -> WARN. Without the pending sell it would be 10.50% -> BLOCK.
        Order pendingSell = new Order("X-EQ", TradeType.SELL, 10_000L);
        Order order = new Order("X-EQ", TradeType.BUY, 20_000L);

        Decision decision = engineWith(issuerRule()).check(order, portfolioP1(), List.of(pendingSell));

        assertThat(decision).isEqualTo(Decision.WARN);
    }

    @Test
    void pendingOrderOfAnotherIssuerDoesNotCount()
    {
        // Pending BUY 50,000 Y-EQ (EUR 5,000,000) is issuer Y, not X.
        // BUY 15,000 X-EQ stays exactly 10.00% -> WARN. If Y were counted: 15.00% -> BLOCK.
        Order pendingOtherIssuer = new Order("Y-EQ", TradeType.BUY, 50_000L);
        Order order = new Order("X-EQ", TradeType.BUY, 15_000L);

        Decision decision = engineWith(issuerRule()).check(order, portfolioP1(), List.of(pendingOtherIssuer));

        assertThat(decision).isEqualTo(Decision.WARN);
    }

    @Test
    void scenario7UnknownInstrumentIsBlockAndNoRuleRuns()
    {
        // BUY 100 ZZ-EQ: not in the reference data. The fake rule fails the test if the engine calls it.
        Order order = new Order("ZZ-EQ", TradeType.BUY, 100L);
        ComplianceRule mustNotRun = state -> {
            throw new AssertionError("no rule may run after a data completeness BLOCK");
        };

        Decision decision = engineWith(mustNotRun).check(order, portfolioP1(), List.of());

        assertThat(decision).isEqualTo(Decision.BLOCK);
    }
}
