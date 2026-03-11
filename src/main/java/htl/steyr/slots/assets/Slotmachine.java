package htl.steyr.slots.assets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Slotmachine {

    private final List<String> symbols = List.of("Hearts", "Diamonds", "Clubs", "Spades");

    public List<String> spin() {
        return spin(4);
    }

    public List<String> spin(int reels) {
        List<String> result = new ArrayList<>(reels);

        for (int i = 0; i < reels; i++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(symbols.size());
            result.add(symbols.get(randomIndex));
        }

        return result;
    }

    public int countHearts(List<String> spin) {
        int hearts = 0;

        for (String symbol : spin) {
            if (symbol.equals("Hearts")) {
                hearts++;
            }
        }

        return hearts;
    }
}