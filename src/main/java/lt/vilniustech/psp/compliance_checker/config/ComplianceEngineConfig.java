package lt.vilniustech.psp.compliance_checker.config;

import java.util.List;
import lt.vilniustech.psp.compliance_checker.domain.BasisPoints;
import lt.vilniustech.psp.compliance_checker.domain.ComplianceEngine;
import lt.vilniustech.psp.compliance_checker.domain.ComplianceRule;
import lt.vilniustech.psp.compliance_checker.domain.DataCompletenessCheck;
import lt.vilniustech.psp.compliance_checker.domain.DecisionCombiner;
import lt.vilniustech.psp.compliance_checker.domain.IssuerConcentrationRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tells Spring how to build the ComplianceEngine. The domain package stays free of Spring:
 * the engine is a plain class, and the only place that knows the fixed prototype rule-set is here.
 */
@Configuration
public class ComplianceEngineConfig {

    // Prototype rule-set (stage III makes this a versioned, per-firm rule-set)
    private static final BasisPoints ISSUER_LIMIT = new BasisPoints(1000);
    private static final BasisPoints ISSUER_WARNING = new BasisPoints(900);

    @Bean
    public ComplianceEngine complianceEngine()
    {
        ComplianceRule issuerRule = new IssuerConcentrationRule(ISSUER_LIMIT, ISSUER_WARNING);
        return new ComplianceEngine(new DataCompletenessCheck(), List.of(issuerRule), new DecisionCombiner());
    }
}
