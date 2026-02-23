package htl.steyr.slots.assets;

import java.util.ArrayList;
import java.util.List;

public class Slotmachine {
    private final List<String> symbols;
    private int reels;

    public Slotmachine(List<String> symbols, int reels) {
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("Symbol list must not be null or empty.");
        }
        if (reels <= 0) {
            throw new IllegalArgumentException("Reels must be greater than 0.");
        }

        this.symbols = new ArrayList<>(symbols);
        this.reels = reels;
    }

}
