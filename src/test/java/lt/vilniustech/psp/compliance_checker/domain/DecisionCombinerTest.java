package lt.vilniustech.psp.compliance_checker.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class DecisionCombinerTest {
    @Test 
    void anyBlockMakesTheDecisionBlock()
    {
        List <RuleResult> results = List.of(
            new RuleResult(RuleOutcome.PASS, false),
            new RuleResult(RuleOutcome.WARN, false),
            new RuleResult(RuleOutcome.BLOCK, true)
        );
        DecisionCombiner combiner = new DecisionCombiner();

        Decision decision = combiner.combine(results);


        assertThat(decision).isEqualTo(Decision.BLOCK);
    }
    @Test
    void allPassMakesTheDecisionAllow()
    {
        List <RuleResult> results = List.of(
            new RuleResult(RuleOutcome.PASS, false),
            new RuleResult(RuleOutcome.PASS, false)
        );
        DecisionCombiner combiner = new DecisionCombiner();

        Decision decision = combiner.combine(results);

        assertThat(decision).isEqualTo(Decision.ALLOW);
    }
    @Test 
    void noResultsMakesTheDecisionAllow()
    {
        List <RuleResult> results = List.of();
        DecisionCombiner combiner = new DecisionCombiner();

        Decision decision = combiner.combine(results);

        assertThat(decision).isEqualTo(Decision.ALLOW);
    }
    @Test
    void anyWarnWithoutBlockMakesTheDecisionWarn()
    {
        List <RuleResult> results = List.of(
            new RuleResult(RuleOutcome.PASS, false),
            new RuleResult(RuleOutcome.WARN, false)
        );
        DecisionCombiner combiner = new DecisionCombiner();

        Decision decision = combiner.combine(results);

        assertThat(decision).isEqualTo(Decision.WARN);
    }
    @Test
    void combinerKeepsNoStateBetweenCalls()
    {
        List <RuleResult> firstOrderResults = List.of(
            new RuleResult(RuleOutcome.PASS, false),
            new RuleResult(RuleOutcome.WARN, false)
        );
        List <RuleResult> secondOrderResults = List.of(
            new RuleResult(RuleOutcome.PASS, false),
            new RuleResult(RuleOutcome.PASS, false)
        );
        DecisionCombiner combiner = new DecisionCombiner();

        Decision firstDecision = combiner.combine(firstOrderResults);
        Decision secondDecision = combiner.combine(secondOrderResults);

        assertThat(firstDecision).isEqualTo(Decision.WARN);
        assertThat(secondDecision).isEqualTo(Decision.ALLOW);
    }

}
