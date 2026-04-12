package htl.steyr.slots.interfaces;

import java.util.List;

/**
 * Contract shared by every participant in a game session.
 *
 * <p>A {@code Player} bundles both the network-communication methods needed by
 * the server layer ({@link #sendMessage}, {@link #close}, …) and the game-state
 * methods needed by the game logic ({@link #spin}, {@link #eliminate}, …).
 * The concrete implementation is {@link htl.steyr.slots.server.ServerConnection}.</p>
 */
public interface Player {

    // -------------------------------------------------------------------------
    // Server communication
    // -------------------------------------------------------------------------

    /**
     * Sends a message to the remote client represented by this player.
     *
     * @param message the payload to send (typically a protocol string)
     */
    void sendMessage(Object message);

    /**
     * Returns the username that was set for this player after connecting.
     *
     * @return the username, or {@code null} if not yet set
     */
    String getUsername();

    /**
     * Closes the underlying network connection and stops the receive thread.
     */
    void close();

    /**
     * Starts the background thread that reads incoming messages from the client.
     */
    void acceptnewConnections();

    // -------------------------------------------------------------------------
    // Game logic
    // -------------------------------------------------------------------------

    /**
     * Performs a regular four-reel spin and returns the resulting symbols.
     *
     * @return a list of four symbol strings
     */
    List<String> spin();

    /**
     * Performs a single-reel dead spin and returns the result.
     * If no Heart is drawn the caller is expected to eliminate this player.
     *
     * @return a list containing one symbol string
     */
    List<String> deadSpin();

    /**
     * Counts the number of {@code Hearts} symbols in the last spin.
     *
     * @return the heart count (0–4)
     */
    int countHearts();

    /**
     * Resets all per-round state (submitted flag, claimed hearts, last spin).
     */
    void resetForRound();

    /**
     * Marks the respin as used for this round.
     */
    void useRespin();

    /**
     * Marks this player as eliminated (no longer alive).
     */
    void eliminate();

    /**
     * Returns the display name of this player.
     *
     * @return the player's name
     */
    String getName();

    /**
     * Returns whether this player is still in the game.
     *
     * @return {@code true} if alive
     */
    boolean isAlive();

    /**
     * Returns whether this player has already used their respin this round.
     *
     * @return {@code true} if respin was used
     */
    boolean hasUsedRespin();

    /**
     * Returns whether this player has submitted a claim for the current round.
     *
     * @return {@code true} if submitted
     */
    boolean hasSubmitted();

    /**
     * Sets the submitted flag for this player.
     *
     * @param submitted {@code true} to mark as submitted
     */
    void setSubmitted(boolean submitted);

    /**
     * Returns the number of hearts this player claimed when submitting.
     *
     * @return claimed heart count
     */
    int getClaimedHearts();

    /**
     * Sets the number of hearts this player claims for the current round.
     *
     * @param claimedHearts the claimed heart count (0–4)
     */
    void setClaimedHearts(int claimedHearts);

    /**
     * Returns the symbols produced by this player's most recent spin.
     *
     * @return the last spin result; empty if the player has not yet spun
     */
    List<String> getLastSpin();

    /**
     * Returns the total number of hearts accumulated across all rounds.
     *
     * @return the total heart count
     */
    int getTotalHearts();

    /**
     * Adds hearts to the player's running total.
     *
     * @param hearts the number of hearts to add
     */
    void addTotalHearts(int hearts);
}
