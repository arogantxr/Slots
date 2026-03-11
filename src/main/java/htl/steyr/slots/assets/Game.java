package htl.steyr.slots.assets;

import java.util.ArrayList;
import java.util.List;

public class Game {

    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex;
    private boolean callingPhase;

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void startRound() {
        for (Player player : players) {
            if (player.isAlive()) {
                player.resetForRound();
            }
        }

        currentPlayerIndex = findNextAlivePlayer(0);
        callingPhase = false;
    }

    public List<String> spinCurrentPlayer() {
        return getCurrentPlayer().spin();
    }

    public List<String> respinCurrentPlayer() {
        Player player = getCurrentPlayer();

        if (!player.hasUsedRespin()) {
            player.useRespin();
            return player.spin();
        }

        return player.getLastSpin();
    }

    public void submitCurrentPlayer(int hearts) {
        Player player = getCurrentPlayer();
        player.setClaimedHearts(hearts);
        player.setSubmitted(true);
        nextPlayer();

        if (allAlivePlayersSubmitted()) {
            callingPhase = true;
        }
    }

    public Player callPlayer(Player caller, Player target) {
        int realHearts = target.countHearts();
        Player deadSpinPlayer;

        if (target.getClaimedHearts() > realHearts) {
            deadSpinPlayer = target;
        } else {
            deadSpinPlayer = caller;
        }

        deadSpin(deadSpinPlayer);
        callingPhase = false;
        return deadSpinPlayer;
    }

    public boolean deadSpin(Player player) {
        List<String> spin = player.deadSpin();

        if (spin.contains("Hearts")) {
            return true;
        }

        player.eliminate();
        return false;
    }

    public boolean hasUniqueLeader() {
        Player leader = getLeader();

        if (leader == null) {
            return false;
        }

        int count = 0;

        for (Player player : players) {
            if (player.isAlive() && player.getClaimedHearts() == leader.getClaimedHearts()) {
                count++;
            }
        }

        return count == 1;
    }

    public Player eliminateLastPlaceIfLeaderCan() {
        Player leader = getLeader();
        Player lastPlace = getLastPlace();

        if (leader == null || lastPlace == null) {
            return null;
        }

        if (leader == lastPlace) {
            return null;
        }

        if (!hasUniqueLeader()) {
            return null;
        }

        if (leader.getClaimedHearts() > 5) {
            lastPlace.eliminate();
            return lastPlace;
        }

        return null;
    }

    public void nextPlayer() {
        currentPlayerIndex = findNextAlivePlayer(currentPlayerIndex + 1);
    }

    public boolean allAlivePlayersSubmitted() {
        for (Player player : players) {
            if (player.isAlive() && !player.hasSubmitted()) {
                return false;
            }
        }
        return true;
    }

    public Player getLeader() {
        Player leader = null;

        for (Player player : players) {
            if (!player.isAlive()) {
                continue;
            }

            if (leader == null || player.getClaimedHearts() > leader.getClaimedHearts()) {
                leader = player;
            }
        }

        return leader;
    }

    public Player getLastPlace() {
        Player lastPlace = null;

        for (Player player : players) {
            if (!player.isAlive()) {
                continue;
            }

            if (lastPlace == null || player.getClaimedHearts() < lastPlace.getClaimedHearts()) {
                lastPlace = player;
            }
        }

        return lastPlace;
    }

    public boolean isCallingPhase() {
        return callingPhase;
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

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public List<Player> getPlayers() {
        return players;
    }

    private int findNextAlivePlayer(int startIndex) {
        if (players.isEmpty()) {
            return 0;
        }

        int index = startIndex;

        if (index >= players.size()) {
            index = 0;
        }

        while (!players.get(index).isAlive()) {
            index++;
            if (index >= players.size()) {
                index = 0;
            }
        }

        return index;
    }
}