package lt.vilniustech.psp.compliance_checker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DataCompletenessCheckTest {

    @Test
    void unknownInstrumentIsBlockedAndNotOverridable()
    {
        // Arrange: scenario 7, ZZ-EQ is not in the firm's reference data
        Map<String, String> instrumentIssuers = Map.of("X-EQ", "X", "X-BOND", "X", "R-EQ", "R");
        Map<String, Long> holdings = Map.of("X-EQ", 100L);
        Map<String, Money> prices = Map.of(
            "X-EQ", new Money(10_000L),
            "X-BOND", new Money(100_000L));
        Money cash = new Money(1_000_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(instrumentIssuers, prices, holdings, cash);
        DataCompletenessCheck check = new DataCompletenessCheck();

        // Act
        RuleResult result = check.evaluate("ZZ-EQ", snapshot);

        // Assert
        assertThat(result.outcome()).isEqualTo(RuleOutcome.BLOCK);
        assertThat(result.overridable()).isFalse();
    }

    @Test
    void knownInstrumentWithPriceIsPass()
    {
        // Arrange: X-EQ is known and priced, and every holding is priced
        Map<String, String> instrumentIssuers = Map.of("X-EQ", "X", "X-BOND", "X", "R-EQ", "R");
        Map<String, Long> holdings = Map.of("X-EQ", 100L);
        Map<String, Money> prices = Map.of(
            "X-EQ", new Money(10_000L),
            "X-BOND", new Money(100_000L));
        Money cash = new Money(1_000_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(instrumentIssuers, prices, holdings, cash);
        DataCompletenessCheck check = new DataCompletenessCheck();

        // Act
        RuleResult result = check.evaluate("X-EQ", snapshot);

        // Assert
        assertThat(result.outcome()).isEqualTo(RuleOutcome.PASS);
        assertThat(result.overridable()).isFalse();
    }

    @Test
    void knownInstrumentWithoutPricesIsBlocked()
    {
        // Arrange: X-EQ is known but has no price; the holding X-BOND is priced
        Map<String, String> instrumentIssuers = Map.of("X-EQ", "X", "X-BOND", "X", "R-EQ", "R");
        Map<String, Long> holdings = Map.of("X-BOND", 50L);
        Map<String, Money> prices = Map.of(
            "X-BOND", new Money(100_000L));
        Money cash = new Money(1_000_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(instrumentIssuers, prices, holdings, cash);
        DataCompletenessCheck check = new DataCompletenessCheck();

        // Act
        RuleResult result = check.evaluate("X-EQ", snapshot);

        // Assert
        assertThat(result.outcome()).isEqualTo(RuleOutcome.BLOCK);
        assertThat(result.overridable()).isFalse();
    }

    @Test
    void heldInstrumentWithoutPriceIsBlocked()
    {
        // Arrange: X-EQ is known and priced, but the holding X-BOND has no price
        Map<String, String> instrumentIssuers = Map.of("X-EQ", "X", "X-BOND", "X", "R-EQ", "R");
        Map<String, Long> holdings = Map.of(
            "X-EQ", 100L,
            "X-BOND", 50L);
        Map<String, Money> prices = Map.of(
            "X-EQ", new Money(10_000L));
        Money cash = new Money(1_000_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(instrumentIssuers, prices, holdings, cash);
        DataCompletenessCheck check = new DataCompletenessCheck();

        // Act
        RuleResult result = check.evaluate("X-EQ", snapshot);

        // Assert
        assertThat(result.outcome()).isEqualTo(RuleOutcome.BLOCK);
        assertThat(result.overridable()).isFalse();
    }
}
