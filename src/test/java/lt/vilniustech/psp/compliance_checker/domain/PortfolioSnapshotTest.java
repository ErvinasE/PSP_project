package lt.vilniustech.psp.compliance_checker.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PortfolioSnapshotTest {

    @Test
    void navIsCashPlusHoldingsAtSnapshotPrices()
    {
        // Arrange: EUR 10 cash + 3 X-EQ at EUR 5 + 2 X-BOND at EUR 20
        Map<String, String> instrumentIssuers = Map.of("X-EQ", "X", "X-BOND", "X");
        Map<String, Long> holdings = Map.of(
            "X-EQ", 3L,
            "X-BOND", 2L);
        Map<String, Money> prices = Map.of(
            "X-EQ", new Money(500L),
            "X-BOND", new Money(2_000L));
        Money cash = new Money(1_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(instrumentIssuers, prices, holdings, cash);

        // Act
        Money nav = snapshot.nav();

        // Assert: 1,000 + 3 x 500 + 2 x 2,000 = 6,500 cents
        assertThat(nav.cents()).isEqualTo(6_500L);
    }

    @Test
    void navOfPortfolioWithOnlyCashIsTheCash()
    {
        // Arrange: boundary, nothing held; X-EQ has a price but is not held, so it must not count
        Map<String, String> instrumentIssuers = Map.of("X-EQ", "X");
        Map<String, Money> prices = Map.of("X-EQ", new Money(500L));
        Map<String, Long> holdings = Map.of();
        Money cash = new Money(1_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(instrumentIssuers, prices, holdings, cash);

        // Act
        Money nav = snapshot.nav();

        // Assert
        assertThat(nav.cents()).isEqualTo(1_000L);
    }

    @Test
    void navFailsWhenAHoldingHasNoPrice()
    {
        // Arrange: X-BOND is held but has no price, so NAV cannot be known
        Map<String, String> instrumentIssuers = Map.of("X-EQ", "X", "X-BOND", "X");
        Map<String, Money> prices = Map.of("X-EQ", new Money(500L));
        Map<String, Long> holdings = Map.of(
            "X-EQ", 3L,
            "X-BOND", 2L);
        Money cash = new Money(1_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(instrumentIssuers, prices, holdings, cash);

        // Act + Assert: fail closed, never treat a missing price as zero
        assertThatThrownBy(snapshot::nav)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("X-BOND");
    }

    @Test
    void issuerExposureSumsAllInstrumentsOfThatIssuer()
    {
        // Arrange: P1 from the design doc. Issuer X = 60,000 X-EQ at EUR 100 + 2,500 X-BOND at EUR 1,000.
        // Y-EQ belongs to another issuer and must not count.
        Map<String, String> instrumentIssuers = Map.of("X-EQ", "X", "X-BOND", "X", "Y-EQ", "Y");
        Map<String, Money> prices = Map.of(
            "X-EQ", new Money(10_000L),
            "X-BOND", new Money(100_000L),
            "Y-EQ", new Money(10_000L));
        Map<String, Long> holdings = Map.of(
            "X-EQ", 60_000L,
            "X-BOND", 2_500L,
            "Y-EQ", 1_000L);
        Money cash = new Money(1_000_000_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(instrumentIssuers, prices, holdings, cash);

        // Act
        Money exposure = snapshot.issuerExposure("X");

        // Assert: EUR 6,000,000 + EUR 2,500,000 = EUR 8,500,000 = 850,000,000 cents
        assertThat(exposure.cents()).isEqualTo(850_000_000L);
    }

    @Test
    void issuerExposureIsZeroWhenNothingOfThatIssuerIsHeld()
    {
        // Arrange: scenario 6, P1 holds nothing of issuer Y
        Map<String, String> instrumentIssuers = Map.of("X-EQ", "X", "Y-EQ", "Y");
        Map<String, Money> prices = Map.of(
            "X-EQ", new Money(10_000L),
            "Y-EQ", new Money(10_000L));
        Map<String, Long> holdings = Map.of("X-EQ", 60_000L);
        Money cash = new Money(1_000_000_000L);
        PortfolioSnapshot snapshot = new PortfolioSnapshot(instrumentIssuers, prices, holdings, cash);

        // Act
        Money exposure = snapshot.issuerExposure("Y");

        // Assert
        assertThat(exposure.cents()).isZero();
    }
}
