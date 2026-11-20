package lt.vilniustech.psp.compliance_checker.domain;

public class DataCompletenessCheck {
    public RuleResult evaluate(String instrument, PortfolioSnapshot snapshot)
    {
        if (!snapshot.isKnown(instrument))
        {
            return new RuleResult(RuleOutcome.BLOCK, false);
        }
        if (!snapshot.hasPrice(instrument))
        {
            return new RuleResult(RuleOutcome.BLOCK, false);
        }
        if (!snapshot.allHeldInstrumentsHavePrices())
        {
            return new RuleResult(RuleOutcome.BLOCK, false);
        }
        return new RuleResult(RuleOutcome.PASS, false);
    }
    
}