package lt.vilniustech.psp.compliance_checker.domain;

public final class BasisPoints {

    public static final int FULL = 10_000;

    private final int value;


    public BasisPoints(int value)
    {
        if (value < 0)
        {
            throw new IllegalArgumentException("basis points must not be negative");
        }
        this.value = value;
    }
    public int value()
    {
        return value;
    }
}

