package lt.vilniustech.psp.compliance_checker.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lt.vilniustech.psp.compliance_checker.domain.Order;
import lt.vilniustech.psp.compliance_checker.domain.TradeType;

/**
 * The JSON body of POST /orders/check. There is deliberately no tenant here:
 * the tenant comes only from the API key, never from the request body.
 */
public record CheckOrderRequest(
    @JsonProperty("portfolio_id") @NotBlank String portfolioId,
    @NotBlank String instrument,
    @JsonProperty("trade_type") @NotNull TradeType tradeType,
    @Positive long quantity) {

    /** The domain Order the engine understands (the portfolio id travels separately). */
    public Order toOrder()
    {
        return new Order(instrument, tradeType, quantity);
    }
}
