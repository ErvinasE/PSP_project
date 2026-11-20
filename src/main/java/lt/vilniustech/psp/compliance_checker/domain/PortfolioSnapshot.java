package lt.vilniustech.psp.compliance_checker.domain;

import java.util.Map;
import java.util.Set;

public final class PortfolioSnapshot {
    private final Set<String> knownInstruments;
    private final Map<String, Money> prices;
    private final Set<String> heldInstruments;

    public PortfolioSnapshot(Set<String> knownInstruments, Map<String, Money> prices, Set<String> heldInstruments)
    {
        this.knownInstruments = Set.copyOf(knownInstruments);
        this.prices = Map.copyOf(prices);
        this.heldInstruments = Set.copyOf(heldInstruments);
    }

    public boolean isKnown(String instrument)
    {
        return knownInstruments.contains(instrument);
    }

    public boolean hasPrice(String instrument)
    {
        return prices.containsKey(instrument);
    }

    public boolean allHeldInstrumentsHavePrices()
    {
        for (String held : heldInstruments)
        {
            if (!hasPrice(held))
            {
                return false;
            }
        }
        return true;
    }
}
