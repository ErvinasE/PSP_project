package lt.vilniustech.psp.compliance_checker.domain;

public class IssuerConcentrationRule {
    private final BasisPoints limit;
    private final BasisPoints warning;

    public IssuerConcentrationRule(BasisPoints limit, BasisPoints warning)
    {
        this.limit = limit;
        this.warning = warning;

    }
    public RuleResult evaluate(Money exposure, Money nav)
    {
        if (exposure.isAbove(limit, nav))
        {
            return new RuleResult(RuleOutcome.BLOCK, true);
        }
        if (exposure.isAtLeast(warning,nav))
        {
            return new RuleResult(RuleOutcome.WARN, false);
            
        }
        return new RuleResult(RuleOutcome.PASS, false);


    }
}
