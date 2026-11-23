package lt.vilniustech.psp.compliance_checker.persistence;

import java.util.List;
import java.util.Optional;
import lt.vilniustech.psp.compliance_checker.auth.ApiKeyHasher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Finds which firm (tenant) an API key belongs to. This is the one query without "WHERE tenant_id = ?":
 * it is the lookup that PRODUCES the tenant. Only the hash of the key is sent to the database.
 */
@Repository
public class FirmRepository {

    private final JdbcTemplate jdbc;

    public FirmRepository(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    /** Empty when the key is missing or no firm has it (the controller turns that into 401). */
    public Optional<String> findTenantId(String apiKey)
    {
        if (apiKey == null)
        {
            return Optional.empty();
        }
        String keyHash = ApiKeyHasher.hash(apiKey);
        List<String> tenantRows = jdbc.queryForList(
            "SELECT tenant_id FROM firms WHERE api_key_hash = ?",
            String.class, keyHash);
        if (tenantRows.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(tenantRows.get(0));
    }
}
