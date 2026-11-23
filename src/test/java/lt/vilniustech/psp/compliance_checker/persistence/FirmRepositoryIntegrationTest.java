package lt.vilniustech.psp.compliance_checker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import lt.vilniustech.psp.compliance_checker.TestcontainersConfiguration;
import lt.vilniustech.psp.compliance_checker.auth.ApiKeyHasher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Looks up the seeded firms (V2) through a real PostgreSQL. Their keys are in the seed comments and README. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FirmRepositoryIntegrationTest {

    @Autowired
    private FirmRepository firms;

    @Test
    void keyOfFirmAFindsFirmA()
    {
        Optional<String> tenantId = firms.findTenantId("demo-key-firm-a");

        assertThat(tenantId).contains("firm-a");
    }

    @Test
    void keyOfFirmBFindsFirmB()
    {
        Optional<String> tenantId = firms.findTenantId("demo-key-firm-b");

        assertThat(tenantId).contains("firm-b");
    }

    @Test
    void unknownKeyFindsNoFirm()
    {
        Optional<String> tenantId = firms.findTenantId("not-a-real-key");

        assertThat(tenantId).isEmpty();
    }

    @Test
    void theStoredHashIsNotAValidKey()
    {
        // A stolen copy of the firms table contains this hash. Sent as a key, it gets hashed again
        // and matches nothing, so the stolen table is useless for logging in.
        String storedHash = ApiKeyHasher.hash("demo-key-firm-a");

        Optional<String> tenantId = firms.findTenantId(storedHash);

        assertThat(tenantId).isEmpty();
    }
}
