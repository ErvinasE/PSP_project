package lt.vilniustech.psp.compliance_checker;

import java.nio.file.Path;
import java.util.TimeZone;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Starts PostgreSQL once per test run with the same roles as docker-compose (docker/db/init).
 * Deliberately no @ServiceConnection: that would wire the app to the superuser, and the
 * tenant-isolation tests would pass even with broken row-level security.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	private static final String SCHEMA_PARAM = "currentSchema=psp";

	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"))
			.withDatabaseName("psp")
			.withUsername("psp_admin")
			.withPassword("psp_admin")
			.withCopyFileToContainer(
					MountableFile.forHostPath(Path.of("docker", "db", "init", "01-roles.sql").toAbsolutePath()),
					"/docker-entrypoint-initdb.d/01-roles.sql");

	static {
		// The JDBC driver sends the JVM time zone to PostgreSQL. Some hosts report the legacy ID
		// "Europe/Kiev", which the postgres image rejects ("invalid value for parameter TimeZone").
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		POSTGRES.start();
	}

	@Bean
	DynamicPropertyRegistrar databaseProperties() {
		return registry -> {
			String baseUrl = POSTGRES.getJdbcUrl();
			String url = baseUrl + (baseUrl.contains("?") ? "&" : "?") + SCHEMA_PARAM;
			registry.add("spring.datasource.url", () -> url);
			registry.add("spring.datasource.username", () -> "psp_app");
			registry.add("spring.datasource.password", () -> "psp_app");
			registry.add("spring.flyway.url", () -> url);
			registry.add("spring.flyway.user", () -> "psp_owner");
			registry.add("spring.flyway.password", () -> "psp_owner");
		};
	}

}
