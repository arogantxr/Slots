package htl.steyr.slots.assets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Models a slot machine that randomly draws symbols from a fixed set.
 *
 * <p>The four available symbols are {@code Hearts}, {@code Diamonds},
 * {@code Clubs}, and {@code Spades}.  A jackpot (all four reels showing
 * the same symbol) always counts as four hearts regardless of the actual
 * symbol drawn.</p>
 */
public class Slotmachine {

    private final List<String> symbols = List.of("Hearts", "Diamonds", "Clubs", "Spades");

    /**
     * Spins all four reels and returns the resulting symbols.
     *
     * @return a list of four randomly chosen symbol strings
     */
    public List<String> spin() {
        return spin(4);
    }

    /**
     * Spins the given number of reels and returns the resulting symbols.
     *
     * @param reels the number of reels to spin (must be &gt; 0)
     * @return a list of {@code reels} randomly chosen symbol strings
     */
    public List<String> spin(int reels) {
        List<String> result = new ArrayList<>(reels);

        for (int i = 0; i < reels; i++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(symbols.size());
            result.add(symbols.get(randomIndex));
        }

        return result;
    }

    /**
     * Counts the number of hearts in the given spin result.
     *
     * <p>A jackpot (all four reels showing the same symbol) always yields 4,
     * regardless of the actual symbol.</p>
     *
     * @param spin the spin result to evaluate
     * @return the number of hearts (0–4)
     */
    public int countHearts(List<String> spin) {
        if (isJackpot(spin)) {
            return 4;
        }

        int hearts = 0;

        for (String symbol : spin) {
            if (symbol.equals("Hearts")) {
                hearts++;
            }
        }

        return hearts;
    }

    /**
     * Returns {@code true} if all reels in the spin show the same symbol.
     *
     * @param spin the spin result to check (must contain exactly 4 symbols)
     * @return {@code true} if this is a jackpot, {@code false} otherwise
     */
    private boolean isJackpot(List<String> spin) {
        if (spin.size() != 4) {
            return false;
        }

        String firstSymbol = spin.get(0);

        for (String symbol : spin) {
            if (!firstSymbol.equals(symbol)) {
                return false;
            }
        }

        return true;
    }
}
