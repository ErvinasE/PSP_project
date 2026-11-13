package lt.vilniustech.psp.compliance_checker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Protects the row-level-security claim (SEC-1): RLS only works if the app connects as a role
 * that is neither superuser nor table owner.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DatabaseRolesIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void appConnectsAsPspApp() {
		String user = jdbc.queryForObject("SELECT current_user", String.class);

		assertThat(user).isEqualTo("psp_app");
	}

	@Test
	void appConnectsAsNonSuperuserWithoutBypassRls() {
		Map<String, Object> role = jdbc.queryForMap(
				"SELECT rolsuper, rolbypassrls FROM pg_roles WHERE rolname = 'psp_app'");

		assertThat(role).containsEntry("rolsuper", false).containsEntry("rolbypassrls", false);
	}

	@Test
	void flywayHistoryTableIsOwnedBySchemaOwner() {
		String owner = jdbc.queryForObject(
				"SELECT tableowner FROM pg_tables WHERE schemaname = 'psp' AND tablename = 'flyway_schema_history'",
				String.class);

		assertThat(owner).isEqualTo("psp_owner");
	}

}
