package lt.vilniustech.psp.compliance_checker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
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

    // ---- Learning aid: prints the numbers behind a decision. Remove before committing. ----

    private static void printTrace(String title, Order order, List<Order> pendingOrders, Decision decision)
    {
        PortfolioSnapshot snapshot = portfolioP1();
        System.out.println();
        System.out.println("=== " + title + " ===");
        System.out.println("Order: " + describe(order));
        if (!snapshot.isKnown(order.instrument()))
        {
            System.out.println("Instrument known?          no -> data check BLOCKs, no rule runs");
            System.out.println("Decision:                  " + decision);
            return;
        }
        String issuer = snapshot.issuerOf(order.instrument());
        Money nav = snapshot.nav();
        Money exposure = snapshot.issuerExposure(issuer);
        printCents("NAV", nav);
        printCents(issuer + " exposure in snapshot", exposure);
        for (Order pending : pendingOrders)
        {
            String pendingIssuer = snapshot.issuerOf(pending.instrument());
            if (!pendingIssuer.equals(issuer))
            {
                System.out.println("  skipped pending " + describe(pending) + " (issuer " + pendingIssuer + ")");
                continue;
            }
            exposure = withOrder(exposure, pending, snapshot);
            printCents(sign(pending) + " pending " + describe(pending),
                snapshot.valueAtSnapshotPrice(pending.instrument(), pending.quantity()));
        }
        exposure = withOrder(exposure, order, snapshot);
        printCents(sign(order) + " this order", snapshot.valueAtSnapshotPrice(order.instrument(), order.quantity()));
        printCents("= " + issuer + " exposure after", exposure);
        System.out.println("Comparison (no division, like Money.isAbove / isAtLeast):");
        printNumber("  exposure x 10,000", exposure.cents() * BasisPoints.FULL);
        printNumber("  limit   1,000 x NAV", LIMIT.value() * nav.cents());
        printNumber("  warning   900 x NAV", WARNING.value() * nav.cents());
        System.out.println("Decision:                  " + decision);
    }

    private static Money withOrder(Money exposure, Order order, PortfolioSnapshot snapshot)
    {
        Money value = snapshot.valueAtSnapshotPrice(order.instrument(), order.quantity());
        if (order.tradeType() == TradeType.BUY)
        {
            return exposure.plus(value);
        }
        return exposure.minus(value);
    }

    private static String describe(Order order)
    {
        return order.tradeType() + " " + order.quantity() + " " + order.instrument();
    }

    private static String sign(Order order)
    {
        if (order.tradeType() == TradeType.BUY)
        {
            return "+";
        }
        return "-";
    }

    private static void printCents(String label, Money money)
    {
        System.out.printf(Locale.ROOT, "%-30s %,22d cents%n", label, money.cents());
    }

    private static void printNumber(String label, long number)
    {
        System.out.printf(Locale.ROOT, "%-30s %,22d%n", label, number);
    }

    // ---- Tests ----

    @Test
    void scenario1NormalBuyIsAllowed()
    {
        // BUY 3,000 X-EQ (EUR 300,000): X exposure 8.80%
        Order order = new Order("X-EQ", TradeType.BUY, 3_000L);

        Decision decision = engineWith(issuerRule()).check(order, portfolioP1(), List.of());

        printTrace("Scenario 1: normal buy", order, List.of(), decision);
        assertThat(decision).isEqualTo(Decision.ALLOW);
    }

    @Test
    void scenario2ExactlyOnTheLimitIsWarn()
    {
        // BUY 15,000 X-EQ (EUR 1,500,000): X exposure exactly 10.00%
        Order order = new Order("X-EQ", TradeType.BUY, 15_000L);

        Decision decision = engineWith(issuerRule()).check(order, portfolioP1(), List.of());

        printTrace("Scenario 2: exactly on the limit", order, List.of(), decision);
        assertThat(decision).isEqualTo(Decision.WARN);
    }

    @Test
    void scenario3OneShareOverTheLimitIsBlock()
    {
        // BUY 15,001 X-EQ (EUR 1,500,100): X exposure 10.0001%
        Order order = new Order("X-EQ", TradeType.BUY, 15_001L);

        Decision decision = engineWith(issuerRule()).check(order, portfolioP1(), List.of());

        printTrace("Scenario 3: one share over the limit", order, List.of(), decision);
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

        printTrace("Scenario 4, order A (nothing pending)", orderA, List.of(), decisionA);
        printTrace("Scenario 4, order B (A is pending)", orderB, List.of(orderA), decisionB);
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

        printTrace("Pending sell reduces the exposure", order, List.of(pendingSell), decision);
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

        printTrace("Pending order of another issuer", order, List.of(pendingOtherIssuer), decision);
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

        printTrace("Scenario 7: unknown instrument", order, List.of(), decision);
        assertThat(decision).isEqualTo(Decision.BLOCK);
    }
}
