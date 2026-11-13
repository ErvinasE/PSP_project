package lt.vilniustech.psp.compliance_checker;

import org.springframework.boot.SpringApplication;

public class TestComplianceCheckerApplication {

	public static void main(String[] args) {
		SpringApplication.from(ComplianceCheckerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
