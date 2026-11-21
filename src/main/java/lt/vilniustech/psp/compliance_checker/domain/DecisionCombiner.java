package lt.vilniustech.psp.compliance_checker.domain;

import java.util.List;

public class DecisionCombiner {
    public Decision combine( List<RuleResult> results)
    {
        boolean sawWarning = false;
        for (RuleResult result : results)
        {
            if (result.outcome() == RuleOutcome.BLOCK)
            {
                return Decision.BLOCK;
            }
            if (result.outcome() == RuleOutcome.WARN)
            {
                sawWarning = true;
            }
        }
        if (sawWarning)
        {
            return Decision.WARN;
        }
        return Decision.ALLOW;
    }
    
}
