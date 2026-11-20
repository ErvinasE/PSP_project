package lt.vilniustech.psp.compliance_checker.domain;

public final class Money {

    private final long cents;
    
    public Money (long cents)
    {
        this.cents = cents;
    }
    public long cents()
    {
        return cents;

    }
    public boolean isAbove(BasisPoints share, Money total)
    {
        return Math.multiplyExact(cents, BasisPoints.FULL) > Math.multiplyExact(share.value(), total.cents());
    }
    public boolean isAtLeast(BasisPoints share, Money total)
    {
        return Math.multiplyExact(cents, BasisPoints.FULL) >= Math.multiplyExact(share.value(), total.cents());
    }
}