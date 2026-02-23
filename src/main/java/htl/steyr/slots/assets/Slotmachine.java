package htl.steyr.slots.assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    public Slotmachine() {
        this(List.of("Cherry", "Lemon", "Orange", "Bell", "Seven"), 3);
    }

    public List<String> getSymbols() {
        return Collections.unmodifiableList(symbols);
    }

    public void addSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol must not be null or blank.");
        }
        symbols.add(symbol);
    }

    public boolean removeSymbol(String symbol) {
        if (symbols.size() <= 1) {
            throw new IllegalStateException("At least one symbol must remain.");
        }
        return symbols.remove(symbol);
    }

    public int getReels() {
        return reels;
    }

    public void setReels(int reels) {
        if (reels <= 0) {
            throw new IllegalArgumentException("Reels must be greater than 0.");
        }
        this.reels = reels;
    }

    public List<String> spin() {
        List<String> result = new ArrayList<>(reels);
        for (int i = 0; i < reels; i++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(symbols.size());
            result.add(symbols.get(randomIndex));
        }
        return result;
    }

}
