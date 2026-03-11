package htl.steyr.slots.assets;

public class Claim {

    private final String symbol;
    private final int amount;
    private final Player player;

    public Claim(String symbol, int amount, Player player) {
        this.symbol = symbol;
        this.amount = amount;
        this.player = player;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getAmount() {
        return amount;
    }

    public Player getPlayer() {
        return player;
    }
}