package lt.vilniustech.psp.compliance_checker.domain;

import java.util.Map;

public final class PortfolioSnapshot {
    private final Map<String, String> instrumentIssuers;
    private final Map<String, Money> prices;
    private final Map<String, Long> holdings;
    private final Money cash;

    public PortfolioSnapshot(Map<String, String> instrumentIssuers, Map<String, Money> prices,
                             Map<String, Long> holdings, Money cash)
    {
        this.instrumentIssuers = Map.copyOf(instrumentIssuers);
        this.prices = Map.copyOf(prices);
        this.holdings = Map.copyOf(holdings);
        this.cash = cash;
    }

    public boolean isKnown(String instrument)
    {
        return instrumentIssuers.containsKey(instrument);
    }

    public boolean hasPrice(String instrument)
    {
        return prices.containsKey(instrument);
    }

    public boolean allHoldingsHavePrices()
    {
        for (String held : holdings.keySet())
        {
            if (!hasPrice(held))
            {
                return false;
            }
        }
        return true;
    }

    public String issuerOf(String instrument)
    {
        String issuer = instrumentIssuers.get(instrument);
        if (issuer == null)
        {
            throw new IllegalStateException("unknown instrument " + instrument + "; run DataCompletenessCheck first");
        }
        return issuer;
    }

    public Money nav()
    {
        Money total = cash;
        for (Map.Entry<String, Long> holding : holdings.entrySet())
        {
            total = total.plus(valueAtSnapshotPrice(holding.getKey(), holding.getValue()));
        }
        return total;
    }

    public Money issuerExposure(String issuer)
    {
        Money total = new Money(0L);
        for (Map.Entry<String, Long> holding : holdings.entrySet())
        {
            if (issuerOf(holding.getKey()).equals(issuer))
            {
                total = total.plus(valueAtSnapshotPrice(holding.getKey(), holding.getValue()));
            }
        }
        return total;
    }

    public Money valueAtSnapshotPrice(String instrument, long quantity)
    {
        Money price = prices.get(instrument);
        if (price == null)
        {
            throw new IllegalStateException("no price for " + instrument + "; run DataCompletenessCheck first");
        }
        return price.times(quantity);
    }
}
