package lt.vilniustech.psp.compliance_checker.domain;

public class RuleResult {
    private final RuleOutcome outcome;
    private final boolean overridable;

    public RuleResult(RuleOutcome outcome, boolean overridable)
    {
        this.outcome = outcome;
        this.overridable = overridable;

    }
    public RuleOutcome outcome ()
    {
        return outcome;
    }
    public boolean overridable()
    {
        return overridable;
    }

}
