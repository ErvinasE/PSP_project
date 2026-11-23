package lt.vilniustech.psp.compliance_checker.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import lt.vilniustech.psp.compliance_checker.domain.Decision;
import lt.vilniustech.psp.compliance_checker.persistence.FirmRepository;
import lt.vilniustech.psp.compliance_checker.service.OrderCheckService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP layer: who is calling (API key), then the service, then a status code.
 * No business rules and no SQL in here.
 */
@RestController
public class OrderCheckController {

    private final FirmRepository firms;
    private final OrderCheckService service;

    public OrderCheckController(FirmRepository firms, OrderCheckService service)
    {
        this.firms = firms;
        this.service = service;
    }

    @PostMapping("/orders/check")
    public ResponseEntity<CheckOrderResponse> check(
        @Valid @RequestBody CheckOrderRequest request,
        HttpServletRequest http)
    {
        // Who is calling? The tenant comes only from the API key, never from the body.
        // A missing or unknown key finds no firm.
        String apiKey = http.getHeader("X-API-Key");
        Optional<String> tenantId = firms.findTenantId(apiKey);
        if (tenantId.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Empty = this tenant has no such portfolio (also another tenant's portfolio).
        Optional<Decision> decision = service.check(tenantId.get(), request.portfolioId(), request.toOrder());
        if (decision.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(new CheckOrderResponse(decision.get()));
    }
}
