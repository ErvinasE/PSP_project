package lt.vilniustech.psp.compliance_checker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import java.util.Map;

class DataCompletenessCheckTest {

    @Test
    void unknownInstrumentIsBlockedAndNotOverridable()
    {
        // Arrange: scenario 7, ZZ-EQ is not in the firm's reference data
        Set<String> knownInstruments = Set.of("X-EQ", "X-BOND", "R-EQ");
        Set<String> heldInstruments = Set.of("X-EQ");
        DataCompletenessCheck check = new DataCompletenessCheck();
        Map<String, Money> prices = Map.of(
        "X-EQ", new Money(10_000L),       
        "X-BOND", new Money(100_000L)); 

        // Act
        PortfolioSnapshot snapshot = new PortfolioSnapshot(knownInstruments, prices, heldInstruments);
        RuleResult result = check.evaluate("ZZ-EQ", snapshot);

        // Assert
        assertThat(result.outcome()).isEqualTo(RuleOutcome.BLOCK);
        assertThat(result.overridable()).isFalse();
    }
    @Test 
    void knownInstrumentWithPriceIsPass()
    {
        Set<String> knownInstruments = Set.of("X-EQ", "X-BOND", "R-EQ");
        Set<String> heldInstruments = Set.of("X-EQ");
        DataCompletenessCheck check = new DataCompletenessCheck();
        Map<String, Money> prices = Map.of(
        "X-EQ", new Money(10_000L),       
        "X-BOND", new Money(100_000L)); 

        // Act
        PortfolioSnapshot snapshot = new PortfolioSnapshot(knownInstruments, prices, heldInstruments);
        RuleResult result = check.evaluate("X-EQ", snapshot);

        // Assert
        assertThat(result.outcome()).isEqualTo(RuleOutcome.PASS);
        assertThat(result.overridable()).isFalse();
    }
    @Test 
    void knownInstrumentWithoutPricesIsBlocked()
    {
        Set<String> knownInstruments = Set.of("X-EQ", "X-BOND", "R-EQ");
        Set<String> heldInstruments = Set.of("X-BOND");
        DataCompletenessCheck check = new DataCompletenessCheck();
        Map<String, Money> prices = Map.of(
        "X-BOND", new Money(100_000L)); 

        // Act
        PortfolioSnapshot snapshot = new PortfolioSnapshot(knownInstruments, prices, heldInstruments);
        RuleResult result = check.evaluate("X-EQ", snapshot);

        // Assert
        assertThat(result.outcome()).isEqualTo(RuleOutcome.BLOCK);
        assertThat(result.overridable()).isFalse();
    }
    @Test
    void heldInstrumentWithoutPriceIsBlocked()
    {
        Set<String> knownInstruments = Set.of("X-EQ", "X-BOND", "R-EQ");
        Set<String> heldInstruments = Set.of("X-EQ", "X-BOND");
        Map<String, Money> price = Map.of(
        "X-EQ", new Money(10_000L));
        DataCompletenessCheck check = new DataCompletenessCheck();

        // Act
        PortfolioSnapshot snapshot = new PortfolioSnapshot(knownInstruments, price, heldInstruments);
        RuleResult result = check.evaluate("X-EQ", snapshot);

        // Assert
        assertThat(result.outcome()).isEqualTo(RuleOutcome.BLOCK);
        assertThat(result.overridable()).isFalse();
    }
}
