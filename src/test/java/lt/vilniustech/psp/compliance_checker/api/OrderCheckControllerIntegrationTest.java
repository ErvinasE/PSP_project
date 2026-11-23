package lt.vilniustech.psp.compliance_checker.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lt.vilniustech.psp.compliance_checker.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * POST /orders/check through the whole app: controller, service, engine and a real PostgreSQL
 * (Testcontainers) with the V2 seed. MockMvc sends the request without opening a network port.
 * {@code @Transactional}: an accepted order is saved as pending, and every test is rolled back at the end.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderCheckControllerIntegrationTest {

    // A valid order for firm-a's portfolio P1: only the API key differs between the tests below
    private static final String ORDER_BODY = """
        {"portfolio_id": "P1", "instrument": "X-EQ", "trade_type": "BUY", "quantity": 100}
        """;

    // Scenario 4 from the design doc: 10,000 X-EQ at EUR 100 is EUR 1,000,000
    private static final String SCENARIO_4_ORDER_BODY = """
        {"portfolio_id": "P1", "instrument": "X-EQ", "trade_type": "BUY", "quantity": 10000}
        """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void wrongApiKeyIsUnauthorized() throws Exception
    {
        ResultActions response = mockMvc.perform(post("/orders/check")
            .header("X-API-Key", "not-a-real-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(ORDER_BODY));

        response.andExpect(status().isUnauthorized());
    }

    @Test
    void missingApiKeyIsUnauthorized() throws Exception
    {
        ResultActions response = mockMvc.perform(post("/orders/check")
            .contentType(MediaType.APPLICATION_JSON)
            .content(ORDER_BODY));

        response.andExpect(status().isUnauthorized());
    }

    @Test
    void firmBCannotCheckAnOrderOnFirmAsPortfolio() throws Exception
    {
        // The same request twice. P1 belongs to firm-a, so only firm-a's key may use it.
        ResultActions responseOfFirmA = mockMvc.perform(post("/orders/check")
            .header("X-API-Key", "demo-key-firm-a")
            .contentType(MediaType.APPLICATION_JSON)
            .content(ORDER_BODY));
        ResultActions responseOfFirmB = mockMvc.perform(post("/orders/check")
            .header("X-API-Key", "demo-key-firm-b")
            .contentType(MediaType.APPLICATION_JSON)
            .content(ORDER_BODY));

        responseOfFirmA.andExpect(status().isOk());
        responseOfFirmB.andExpect(status().isNotFound());
    }

    @Test
    void scenario4SecondIdenticalOrderIsBlockedOverHttp() throws Exception
    {
        // BUY 10,000 X-EQ (EUR 1,000,000) twice for P1.
        // First: 8.5M + 1M = 9.50% -> WARN, saved as pending.
        // Second: 8.5M + 1M (pending) + 1M = 10.50% -> BLOCK.
        ResultActions firstResponse = mockMvc.perform(post("/orders/check")
            .header("X-API-Key", "demo-key-firm-a")
            .contentType(MediaType.APPLICATION_JSON)
            .content(SCENARIO_4_ORDER_BODY));
        ResultActions secondResponse = mockMvc.perform(post("/orders/check")
            .header("X-API-Key", "demo-key-firm-a")
            .contentType(MediaType.APPLICATION_JSON)
            .content(SCENARIO_4_ORDER_BODY));

        firstResponse.andExpect(status().isOk()).andExpect(jsonPath("$.decision").value("WARN"));
        secondResponse.andExpect(status().isOk()).andExpect(jsonPath("$.decision").value("BLOCK"));
    }
}
