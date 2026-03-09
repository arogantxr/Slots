package htl.steyr.slots.assets;

public class Claim {

    private final String symbol;
    private final int amount;

    public Claim(String symbol, int amount) {
        this.symbol = symbol;
        this.amount = amount;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getAmount() {
        return amount;
    }
}