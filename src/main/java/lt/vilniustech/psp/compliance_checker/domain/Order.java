package lt.vilniustech.psp.compliance_checker.domain;

public final class Order {
    private final String instrument;
    private final TradeType tradeType;
    private final long quantity;


    public Order(String instrument, TradeType tradeType, long quantity) {
        if (quantity <= 0)
        {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.instrument = instrument;
        this.tradeType = tradeType;
        this.quantity = quantity;
    }
    public String instrument() {
        return instrument;
    }
    public TradeType tradeType() {
        return tradeType;
    }
    public long quantity() {
        return quantity;
    }

}
