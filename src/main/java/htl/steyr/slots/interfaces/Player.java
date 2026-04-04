package htl.steyr.slots.interfaces;

import java.util.List;

public interface Player {
    // Server Communication Methods
    /**
     * Sends a message to the client
     */
    void sendMessage(Object message);

    /**
     * Gets the username of the player
     */
    String getUsername();

    /**
     * Closes the connection
     */
    void close();

    /**
     * Accepts new connections from the client
     */
    void acceptnewConnections();

    // Game Logic Methods
    /**
     * Performs a regular spin
     */
    List<String> spin();

    /**
     * Performs a spin when the player is dead
     */
    List<String> deadSpin();

    /**
     * Counts the hearts in the last spin
     */
    int countHearts();

    /**
     * Resets the player for a new round
     */
    void resetForRound();

    /**
     * Uses the respin for the player
     */
    void useRespin();

    /**
     * Eliminates the player
     */
    void eliminate();

    /**
     * Gets the name of the player
     */
    String getName();

    /**
     * Checks if the player is alive
     */
    boolean isAlive();

    /**
     * Checks if the player has used respin
     */
    boolean hasUsedRespin();

    /**
     * Checks if the player has submitted
     */
    boolean hasSubmitted();

    /**
     * Sets the submitted status
     */
    void setSubmitted(boolean submitted);

    /**
     * Gets the claimed hearts
     */
    int getClaimedHearts();

    /**
     * Sets the claimed hearts
     */
    void setClaimedHearts(int claimedHearts);

    /**
     * Gets the last spin result
     */
    List<String> getLastSpin();
}
