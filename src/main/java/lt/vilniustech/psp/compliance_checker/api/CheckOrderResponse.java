package lt.vilniustech.psp.compliance_checker.api;

import lt.vilniustech.psp.compliance_checker.domain.Decision;

/** The JSON answer of POST /orders/check, for example {"decision":"WARN"}. */
public final class CheckOrderResponse {

    private final Decision decision;

    public CheckOrderResponse(Decision decision)
    {
        this.decision = decision;
    }

    public Decision getDecision()
    {
        return decision;
    }
}
