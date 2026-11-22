package lt.vilniustech.psp.compliance_checker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import lt.vilniustech.psp.compliance_checker.TestcontainersConfiguration;
import lt.vilniustech.psp.compliance_checker.domain.PortfolioSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Reads the V2 seed through a real PostgreSQL (Testcontainers), connected as psp_app. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PortfolioRepositoryIntegrationTest {

    @Autowired
    private PortfolioRepository repository;

    @Test
    void portfolioP1FromTheSeedHasTheDesignDocNumbers()
    {
        PortfolioSnapshot snapshot = repository.loadSnapshot("firm-a", "P1").orElseThrow();

        // NAV EUR 100,000,000 and issuer X EUR 8,500,000, same as portfolioP1() in ComplianceEngineTest
        assertThat(snapshot.nav().cents()).isEqualTo(10_000_000_000L);
        assertThat(snapshot.issuerExposure("X").cents()).isEqualTo(850_000_000L);
    }

    @Test
    void referenceDataKeepsUnknownUnpricedAndNotHeldApart()
    {
        PortfolioSnapshot snapshot = repository.loadSnapshot("firm-a", "P1").orElseThrow();

        assertThat(snapshot.isKnown("Y-EQ")).isTrue();      // known and priced, not held
        assertThat(snapshot.hasPrice("Y-EQ")).isTrue();
        assertThat(snapshot.isKnown("U-EQ")).isTrue();      // known, price NULL
        assertThat(snapshot.hasPrice("U-EQ")).isFalse();
        assertThat(snapshot.isKnown("ZZ-EQ")).isFalse();    // no row at all
    }

    @Test
    void unknownPortfolioIsEmpty()
    {
        Optional<PortfolioSnapshot> snapshot = repository.loadSnapshot("firm-a", "NO-SUCH-PORTFOLIO");

        assertThat(snapshot).isEmpty();
    }

    @Test
    void anotherFirmCannotLoadFirmAsPortfolio()
    {
        // P1 exists, but it belongs to firm-a
        Optional<PortfolioSnapshot> snapshot = repository.loadSnapshot("firm-b", "P1");

        assertThat(snapshot).isEmpty();
    }

    @Test
    void eachFirmSeesItsOwnPrices()
    {
        // Firm B's X-EQ costs EUR 120 (firm A's: EUR 100): 500,000 cash + 1,000 x 120 = EUR 620,000
        PortfolioSnapshot snapshot = repository.loadSnapshot("firm-b", "B1").orElseThrow();

        assertThat(snapshot.nav().cents()).isEqualTo(62_000_000L);
    }
}
