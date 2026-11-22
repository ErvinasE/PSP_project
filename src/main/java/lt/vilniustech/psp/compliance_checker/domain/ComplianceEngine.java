package lt.vilniustech.psp.compliance_checker.domain;

import java.util.ArrayList;
import java.util.List;

public class ComplianceEngine {
    private final DataCompletenessCheck dataCheck;
    private final List<ComplianceRule> rules;
    private final DecisionCombiner combiner;

    public ComplianceEngine(DataCompletenessCheck dataCheck, List<ComplianceRule> rules, DecisionCombiner combiner)
    {
        this.dataCheck = dataCheck;
        this.rules = List.copyOf(rules);
        this.combiner = combiner;
    }

    public Decision check(Order order, PortfolioSnapshot snapshot, List<Order> pendingOrders)
    {
        // 1. Data completeness: fail closed, no other rule runs
        RuleResult dataResult = dataCheck.evaluate(order.instrument(), snapshot);
        if (dataResult.outcome() == RuleOutcome.BLOCK)
        {
            return Decision.BLOCK;
        }

        // 2. Post-trade state: snapshot + pending orders + this order (NAV does not change on a trade)
        String issuer = snapshot.issuerOf(order.instrument());
        Money exposure = snapshot.issuerExposure(issuer);
        for (Order pending : pendingOrders)
        {
            if (snapshot.issuerOf(pending.instrument()).equals(issuer))
            {
                exposure = applyOrder(exposure, pending, snapshot);
            }
        }
        exposure = applyOrder(exposure, order, snapshot);
        PostTradeState state = new PostTradeState(snapshot.nav(), exposure, order.tradeType());

        // 3. Every rule, then combine
        List<RuleResult> results = new ArrayList<>();
        for (ComplianceRule rule : rules)
        {
            results.add(rule.evaluate(state));
        }
        return combiner.combine(results);
    }

    private Money applyOrder(Money exposure, Order order, PortfolioSnapshot snapshot)
    {
        Money value = snapshot.valueAtSnapshotPrice(order.instrument(), order.quantity());
        if (order.tradeType() == TradeType.BUY)
        {
            return exposure.plus(value);
        }
        return exposure.minus(value);
    }
}
