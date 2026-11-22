package lt.vilniustech.psp.compliance_checker.domain;

import java.util.Map;
import java.util.Set;

public final class PortfolioSnapshot {
    private final Set<String> knownInstruments;
    private final Map<String, Money> prices;
    private final Money cash;
    private final Map<String, Long> holdings;
    public PortfolioSnapshot(Set<String> knownInstruments, Map<String, Money> prices, Map<String, Long> holdings, Money cash)
    {
        this.knownInstruments = Set.copyOf(knownInstruments);
        this.prices = Map.copyOf(prices);
        this.holdings = Map.copyOf(holdings);
        this.cash = cash;
    }

    public boolean isKnown(String instrument)
    {
        return knownInstruments.contains(instrument);
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

    public Money nav()
    {
        Money total = cash;
        for (Map.Entry<String, Long> holding : holdings.entrySet())
        {
            total = total.plus(valueAtSnapshotPrice(holding.getKey(), holding.getValue()));
        }
        return total;
    }

    private Money valueAtSnapshotPrice(String instrument, long quantity)
    {
        Money price = prices.get(instrument);
        if (price == null)
        {
            throw new IllegalStateException("no price for " + instrument + "; run DataCompletenessCheck first");
        }
        return price.times(quantity);
    }
}
