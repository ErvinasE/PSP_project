package lt.vilniustech.psp.compliance_checker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
public class IssuerConcentrationRuleTest {
    @Test
    void exposureOverTenPercentOfNavIsBlockedAndOverridable()
    {
        Money exposure = new Money(1_000_010_000L);
        Money nav = new Money (10_000_000_000L);
        IssuerConcentrationRule rule = new IssuerConcentrationRule(new BasisPoints(1000), new BasisPoints(900));
        PostTradeState state = new PostTradeState(nav, exposure, TradeType.BUY);

        RuleResult result = rule.evaluate(state);

        assertThat(result.outcome()).isEqualTo(RuleOutcome.BLOCK);
        assertThat(result.overridable()).isTrue();
    }
    @Test 
    void exposureAtTenPercentOfNavIsWarning()
    {
        Money exposure = new Money(1_000_000_000L);
        Money nav = new Money (10_000_000_000L);
        IssuerConcentrationRule rule = new IssuerConcentrationRule(new BasisPoints(1000), new BasisPoints(900));
        PostTradeState state = new PostTradeState(nav, exposure, TradeType.BUY);

        RuleResult result = rule.evaluate(state);

        assertThat(result.outcome()).isEqualTo(RuleOutcome.WARN);
        assertThat(result.overridable()).isFalse();
    }
    @Test 
    void exposureAtNinePercentIsWarning()
    {
        Money exposure = new Money(900_000_000L);
        Money nav = new Money (10_000_000_000L);
        IssuerConcentrationRule rule = new IssuerConcentrationRule(new BasisPoints(1000), new BasisPoints(900));
        PostTradeState state = new PostTradeState(nav, exposure, TradeType.BUY);

        RuleResult result = rule.evaluate(state);

        assertThat(result.outcome()).isEqualTo(RuleOutcome.WARN);
        assertThat(result.overridable()).isFalse();
    }
    @Test 
    void exposureBelowNinePercentIsPass()
    {
        Money exposure = new Money(880_000_000L);
        Money nav = new Money (10_000_000_000L);
        IssuerConcentrationRule rule = new IssuerConcentrationRule(new BasisPoints(1000), new BasisPoints(900));
        PostTradeState state = new PostTradeState(nav, exposure, TradeType.BUY);
        RuleResult result = rule.evaluate(state);

        assertThat(result.outcome()).isEqualTo(RuleOutcome.PASS);
        assertThat(result.overridable()).isFalse();

    }
    @Test
    void sellOrderAtTwelvePercentIsPass()
    {
        Money exposure = new Money(1_200_000_000L);
        Money nav = new Money (10_000_000_000L);
        IssuerConcentrationRule rule = new IssuerConcentrationRule(new BasisPoints(1000), new BasisPoints(900));
        PostTradeState state = new PostTradeState(nav, exposure, TradeType.SELL);
        RuleResult result = rule.evaluate(state);

        assertThat(result.outcome()).isEqualTo(RuleOutcome.PASS);
        assertThat(result.overridable()).isFalse();
    }
}
