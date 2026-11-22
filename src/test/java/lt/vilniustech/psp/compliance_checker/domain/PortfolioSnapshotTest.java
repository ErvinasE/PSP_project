package lt.vilniustech.psp.compliance_checker.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PortfolioSnapshotTest {

    @Test
    void navIsCashPlusHoldingsAtSnapshotPrices()
    {
        // Arrange: EUR 10 cash + 3 X-EQ at EUR 5 + 2 X-BOND at EUR 20
        Set<String> knownInstruments = Set.of("X-EQ", "X-BOND");
        Map<String, Long> holdings = Map.of(
            "X-EQ", 3L,
            "X-BOND", 2L);
        Map<String, Money> prices = Map.of(
            "X-EQ", new Money(500L),
            "X-BOND", new Money(2_000L));
        Money cash = new Money(1_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(knownInstruments, prices, holdings, cash);

        // Act
        Money nav = snapshot.nav();

        // Assert: 1,000 + 3 x 500 + 2 x 2,000 = 6,500 cents
        assertThat(nav.cents()).isEqualTo(6_500L);
    }

    @Test
    void navOfPortfolioWithOnlyCashIsTheCash()
    {
        // Arrange: boundary, nothing held; X-EQ has a price but is not held, so it must not count
        Set<String> knownInstruments = Set.of("X-EQ");
        Map<String, Money> prices = Map.of("X-EQ", new Money(500L));
        Map<String, Long> holdings = Map.of();
        Money cash = new Money(1_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(knownInstruments, prices, holdings, cash);

        // Act
        Money nav = snapshot.nav();

        // Assert
        assertThat(nav.cents()).isEqualTo(1_000L);
    }

    @Test
    void navFailsWhenAHoldingHasNoPrice()
    {
        // Arrange: X-BOND is held but has no price, so NAV cannot be known
        Set<String> knownInstruments = Set.of("X-EQ", "X-BOND");
        Map<String, Money> prices = Map.of("X-EQ", new Money(500L));
        Map<String, Long> holdings = Map.of(
            "X-EQ", 3L,
            "X-BOND", 2L);
        Money cash = new Money(1_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(knownInstruments, prices, holdings, cash);

        // Act + Assert: fail closed, never treat a missing price as zero
        assertThatThrownBy(snapshot::nav)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("X-BOND");
    }
}
