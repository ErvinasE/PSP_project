package lt.vilniustech.psp.compliance_checker.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
public class OrderTest {
    @Test
    void zeroOrNegativeQuantityIsRejected()
    {
    assertThatThrownBy(() -> new Order("X-EQ", TradeType.BUY, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Order("X-EQ", TradeType.BUY, -1))
        .isInstanceOf(IllegalArgumentException.class);

    }
    @Test 
    void quantityOfOneIsAccepted()
    {
        assertThatCode(() -> new Order("X-EQ", TradeType.BUY, 1))
            .doesNotThrowAnyException();
    }

}
