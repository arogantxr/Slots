package htl.steyr.slots.assets;

import java.util.ArrayList;
import java.util.List;

public class Game {

    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex;

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void startRound() {
        for (Player player : players) {
            if (player.isAlive()) {
                player.spin();
            }
        }
        currentPlayerIndex = 0;
    }



    public void nextPlayer() {
        currentPlayerIndex++;
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
        }
    }

    public boolean isGameOver() {
        int alive = 0;

        for (Player player : players) {
            if (player.isAlive()) {
                alive++;
            }
        }

        return alive <= 1;
    }

    public Player getWinner() {
        for (Player player : players) {
            if (player.isAlive()) {
                return player;
            }
        }
        return null;
    }

    public List<Player> getPlayers() {
        return players;
    }


    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }
}