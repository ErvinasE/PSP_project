package lt.vilniustech.psp.compliance_checker.domain;

public interface ComplianceRule {
    RuleResult evaluate(PostTradeState state);
}
