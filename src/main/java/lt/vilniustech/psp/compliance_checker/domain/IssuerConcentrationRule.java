package lt.vilniustech.psp.compliance_checker.domain;

public class IssuerConcentrationRule implements ComplianceRule {
    private final BasisPoints limit;
    private final BasisPoints warning;

    public IssuerConcentrationRule(BasisPoints limit, BasisPoints warning)
    {
        this.limit = limit;
        this.warning = warning;

    }
    @Override
    public RuleResult evaluate(PostTradeState state)
    {
        if (state.tradeType() == TradeType.SELL)
        {
            return new RuleResult(RuleOutcome.PASS, false);
        }
        if (state.issuerExposure().isAbove(limit, state.nav()))
        {
            return new RuleResult(RuleOutcome.BLOCK, true);
        }
        if (state.issuerExposure().isAtLeast(warning, state.nav()))
        {
            return new RuleResult(RuleOutcome.WARN, false);
            
        }
        return new RuleResult(RuleOutcome.PASS, false);
    }
}
