package lt.vilniustech.psp.compliance_checker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void exposureOneEuroHundredOverTenPercentOfNavIsAboveTheLimit() {
        // Arrange: scenario 3, exposure EUR 10,000,100 against NAV EUR 100,000,000 (in cents)
        Money exposure = new Money(1_000_010_000L);
        Money nav = new Money(10_000_000_000L);
        BasisPoints limit = new BasisPoints(1000); // 10%

        // Act
        boolean above = exposure.isAbove(limit, nav);

        // Assert
        assertThat(above).isTrue();
    }
    @Test
    void exposureTenPercentOfNavIsNotAboveTheLimit()
    {
        Money exposure = new Money(1_000_000_000L); // EUR 1,000,000
        Money nav = new Money(10_000_000_000L); // EUR 10,000,000
        BasisPoints limit = new BasisPoints(1000); // 10%

        boolean above = exposure.isAbove(limit, nav);

        assertThat(above).isFalse();
    }

    @Test
    void plusAddsTheCents()
    {
        Money cash = new Money(1_000L);
        Money holding = new Money(1_500L);

        Money total = cash.plus(holding);

        assertThat(total.cents()).isEqualTo(2_500L);
    }

    @Test
    void timesMultipliesThePriceByAQuantity()
    {
        Money price = new Money(500L); // EUR 5.00 per unit

        Money value = price.times(3L);

        assertThat(value.cents()).isEqualTo(1_500L);
    }
}
