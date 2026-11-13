package lt.vilniustech.psp.compliance_checker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ComplianceCheckerApplicationTests {

	@Test
	void contextLoads() {
	}

}
