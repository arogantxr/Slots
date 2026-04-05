package htl.steyr.slots.assets;

import htl.steyr.slots.interfaces.Player;
import htl.steyr.slots.server.ServerConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the authoritative game state for a multiplayer Slots session.
 *
 * <p>This class runs exclusively on the host side.  It tracks all connected
 * players, advances the turn order, evaluates Liar calls, and notifies the
 * host controller via callbacks whenever the current player or the overall
 * game state changes.</p>
 */
public class Game {

    private final List<ServerConnection> players = new ArrayList<>();
    private int currentPlayerIndex;
    private Consumer<String> turnUpdateCallback;
    private Consumer<List<String>> gameStateCallback;

    /**
     * Adds a player to the game.
     *
     * @param player the {@link ServerConnection} representing the player to add
     */
    public void addPlayer(ServerConnection player) {
        players.add(player);
    }

    /**
     * Starts a new round by resetting every living player and setting the
     * turn pointer to the first alive player.
     */
    public void startRound() {
        for (Player player : players) {
            if (player.isAlive()) {
                player.resetForRound();
            }
        }

        currentPlayerIndex = findNextAlivePlayer(0);
    }

    /**
     * Executes a regular spin for the current player.
     *
     * @return the list of symbols produced by the spin
     */
    public List<String> spinCurrentPlayer() {
        return getCurrentPlayer().spin();
    }

    /**
     * Executes a respin (Double) for the current player if the respin has not
     * already been used this round.  If it has been used the last spin result
     * is returned unchanged.
     *
     * @return the new spin symbols, or the previous spin if respin was already used
     */
    public List<String> respinCurrentPlayer() {
        Player player = getCurrentPlayer();

        if (!player.hasUsedRespin()) {
            player.useRespin();
            return player.spin();
        }

        return player.getLastSpin();
    }

    /**
     * Records the current player's claimed hearts, marks them as submitted, and
     * advances the turn to the next alive player.
     *
     * @param hearts the number of hearts the player claims (0–4)
     */
    public void submitCurrentPlayer(int hearts) {
        Player player = getCurrentPlayer();
        player.setClaimedHearts(hearts);
        player.setSubmitted(true);
        nextPlayer();
    }

    /**
     * Resolves a Liar call: the player whose claim was wrong must perform a
     * dead spin.
     *
     * @param caller the player raising the Liar call
     * @param target the player being accused
     * @return the player who had to perform the dead spin
     */
    public Player callPlayer(Player caller, Player target) {
        int realHearts = target.countHearts();
        Player deadSpinPlayer;

        if (target.getClaimedHearts() > realHearts) {
            deadSpinPlayer = target;
        } else {
            deadSpinPlayer = caller;
        }

        deadSpin(deadSpinPlayer);
        return deadSpinPlayer;
    }

    /**
     * Performs a dead spin for the given player.  If no Heart is drawn the
     * player is eliminated.
     *
     * @param player the player who must perform the dead spin
     * @return {@code true} if the player survived (Heart drawn), {@code false}
     *         if eliminated
     */
    public boolean deadSpin(Player player) {
        List<String> spin = player.deadSpin();

        if (spin.contains("Hearts")) {
            return true;
        }

        player.eliminate();
        return false;
    }

    /**
     * Checks whether every living player has already submitted their claim for
     * this round.
     *
     * @return {@code true} if all alive players have submitted
     */
    public boolean allAlivePlayersSubmitted() {
        for (Player player : players) {
            if (player.isAlive() && !player.hasSubmitted()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Finds the most-recently submitted alive player who comes directly before
     * the given player in turn order.
     *
     * @param player the reference player
     * @return the previous submitted alive player, or {@code null} if none exists
     */
    public Player getPreviousSubmittedAlivePlayer(Player player) {
        int index = players.indexOf(player);

        index--;
        if (index < 0) {
            index = players.size() - 1;
        }

        while (players.get(index) != player) {
            Player previousPlayer = players.get(index);

            if (previousPlayer.isAlive() && previousPlayer.hasSubmitted()) {
                return previousPlayer;
            }

            index--;
            if (index < 0) {
                index = players.size() - 1;
            }
        }

        return null;
    }

    /**
     * Returns the alive player with the highest number of claimed hearts.
     *
     * @return the leading player, or {@code null} if there are no alive players
     */
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

    /**
     * Returns {@code true} if there is exactly one player in first place (no tie).
     *
     * @return {@code true} when a unique leader exists
     */
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

    /**
     * Returns all alive players who are currently tied for last place.
     *
     * @return a list of last-place players; may contain multiple entries on a tie
     */
    public List<Player> getLastPlacePlayers() {
        List<Player> lastPlayers = new ArrayList<>();
        Player lastPlace = getLastPlace();

        if (lastPlace == null) {
            return lastPlayers;
        }

        for (Player player : players) {
            if (player.isAlive() && player.getClaimedHearts() == lastPlace.getClaimedHearts()) {
                lastPlayers.add(player);
            }
        }

        return lastPlayers;
    }

    /**
     * Returns {@code true} if the leader may eliminate a last-place player this
     * round (unique leader with more than 5 claimed hearts).
     *
     * @return {@code true} when an elimination is allowed
     */
    public boolean canLeaderEliminate() {
        Player leader = getLeader();

        if (leader == null) {
            return false;
        }

        return hasUniqueLeader() && leader.getClaimedHearts() > 5;
    }

    /**
     * Eliminates the specified player from the game.
     *
     * @param player the player to eliminate; does nothing if {@code null}
     */
    public void eliminatePlayer(Player player) {
        if (player != null) {
            player.eliminate();
        }
    }

    /**
     * Advances the turn pointer to the next alive player and fires the
     * turn-update callback.
     */
    public void nextPlayer() {
        currentPlayerIndex = findNextAlivePlayer(currentPlayerIndex + 1);
        notifyTurnUpdate(getCurrentPlayer().getName());
    }

    /**
     * Returns {@code true} if one or fewer players remain alive.
     *
     * @return {@code true} when the game is over
     */
    public boolean isGameOver() {
        int alive = 0;

        for (Player player : players) {
            if (player.isAlive()) {
                alive++;
            }
        }

        return alive <= 1;
    }

    /**
     * Returns the last surviving player, or {@code null} if nobody is alive.
     *
     * @return the winner, or {@code null}
     */
    public Player getWinner() {
        for (Player player : players) {
            if (player.isAlive()) {
                return player;
            }
        }

        return null;
    }

    /**
     * Returns the player whose turn it currently is.
     *
     * @return the current player
     */
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    /**
     * Returns the full list of players (alive and eliminated).
     *
     * @return all players in join order
     */
    public List<ServerConnection> getPlayers() {
        return players;
    }

    /**
     * Returns the alive player with the fewest claimed hearts (last place).
     *
     * @return the last-place player, or {@code null} if there are no alive players
     */
    private Player getLastPlace() {
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

    /**
     * Searches forward from {@code startIndex} (wrapping around) for the first
     * alive player.
     *
     * @param startIndex the index to start searching from
     * @return the index of the next alive player
     */
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

    /**
     * Registers a callback that is invoked whenever the current player changes.
     * The argument passed to the callback is the new current player's name.
     *
     * @param callback the callback to register
     */
    public void setTurnUpdateCallback(Consumer<String> callback) {
        this.turnUpdateCallback = callback;
    }

    /**
     * Registers a callback that is invoked whenever the game state should be
     * broadcast.  The argument is a list of formatted player-state strings.
     *
     * @param callback the callback to register
     */
    public void setGameStateCallback(Consumer<List<String>> callback) {
        this.gameStateCallback = callback;
    }

    /**
     * Fires the turn-update callback if one is registered.
     *
     * @param update the name of the player who is now active
     */
    private void notifyTurnUpdate(String update) {
        if (turnUpdateCallback != null) {
            turnUpdateCallback.accept(update);
        }
    }

    /**
     * Fires the game-state callback if one is registered.
     *
     * @param gameState the list of formatted player-state strings to broadcast
     */
    private void notifyGameStateUpdate(List<String> gameState) {
        if (gameStateCallback != null) {
            gameStateCallback.accept(gameState);
        }
    }
}
