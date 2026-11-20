package lt.vilniustech.psp.compliance_checker.domain;

public class PostTradeState {

    private final Money nav;
    private final Money issuerExposure;
    private final TradeType tradeType;

    public PostTradeState(Money nav, Money issuerExposure, TradeType tradeType)
    {
        this.nav = nav;
        this.issuerExposure = issuerExposure;
        this.tradeType = tradeType;

    }
    public Money nav()
    {
        return nav;
    }
    public Money issuerExposure()
    {
        return issuerExposure;
    }
    public TradeType tradeType()
    {
        return tradeType;
    }

}
