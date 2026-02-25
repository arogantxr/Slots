package htl.steyr.slots.assets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Slotmachine {

    private final int REELS = 4;

    private final List<String> symbols = List.of("Hearts", "Diamonds", "Clubs", "Spades");

    public List<String> spin() {
        List<String> result = new ArrayList<>(REELS);

        for (int i = 0; i < REELS; i++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(symbols.size());
            result.add(symbols.get(randomIndex));
        }

        return result;
    }
}