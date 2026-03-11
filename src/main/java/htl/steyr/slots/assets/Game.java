package htl.steyr.slots.assets;

import java.util.ArrayList;
import java.util.List;

public class Game {

    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex;
    private Claim currentClaim;

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void startRound() {
        for (Player player : players) {
            if (player.isAlive()) {
                player.spin();
            }
        }
        currentClaim = null;
        currentPlayerIndex = 0;
    }


    public List<Player> getPlayers() {
        return players;
    }

    public Claim getCurrentClaim() {
        return currentClaim;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }
}