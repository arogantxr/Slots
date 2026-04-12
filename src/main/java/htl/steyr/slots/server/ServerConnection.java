package htl.steyr.slots.server;

import htl.steyr.slots.assets.Slotmachine;
import htl.steyr.slots.interfaces.Event;
import htl.steyr.slots.interfaces.Player;
import htl.steyr.slots.interfaces.PublisherInterface;
import htl.steyr.slots.interfaces.SubscriberInterface;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Server-side handle for a single connected client that also acts as the
 * {@link Player} representation used by the game logic.
 *
 * <p>{@code ServerConnection} serves a dual role:
 * <ul>
 *   <li><b>Network layer</b> — wraps a {@link Socket} and owns the input/output
 *       streams used to communicate with the remote client. It starts a
 *       background thread that continuously reads incoming messages and
 *       forwards them to registered subscribers via the
 *       {@link PublisherInterface} contract.</li>
 *   <li><b>Game entity</b> — implements {@link Player} so the game engine can
 *       treat every connected client as a first-class participant without
 *       knowing anything about the underlying network transport. A dedicated
 *       {@link Slotmachine} instance handles all spin logic for this
 *       player.</li>
 * </ul>
 *
 * <p>This class implements {@link Player} and {@link PublisherInterface}.
 */
public class ServerConnection implements Player, PublisherInterface {
    Socket socket;
    private final Scanner in;
    private final PrintWriter out;

    private volatile boolean running;
    private Thread receiveThread;

    private String username;
    private int id;

    // Game state fields
    private final Slotmachine slotmachine;
    private boolean alive = true;
    private boolean usedRespin = false;
    private boolean submitted = false;
    private int claimedHearts = 0;
    private int totalHearts = 0;
    private List<String> lastSpin = new ArrayList<>();

    private final List<SubscriberInterface> subscribers = new ArrayList<>();

    /**
     * Creates a new {@code ServerConnection} that wraps the given {@link Socket}.
     *
     * <p>The constructor sets up a line-oriented {@link Scanner} for reading
     * from the socket's input stream and an auto-flushing {@link PrintWriter}
     * for writing to its output stream. A fresh {@link Slotmachine} is created
     * for this player and the receive loop is marked as ready to start.
     *
     * @param newconnection the accepted client socket; must not be {@code null}
     *                      and must already be connected
     * @throws IOException if an I/O error occurs while obtaining the input or
     *                     output stream from the socket
     */
    public ServerConnection(Socket newconnection) throws IOException {
        this.socket = newconnection;

        this.in = new Scanner(newconnection.getInputStream());
        this.out = new PrintWriter(newconnection.getOutputStream(), true);

        running = true;
        this.slotmachine = new Slotmachine();
    }

    /**
     * Registers a subscriber that will be notified whenever a message is
     * received from the remote client.
     *
     * @param subscriber the subscriber to add; must not be {@code null}
     */
    @Override
    public void addSubscriber(SubscriberInterface subscriber) {
        subscribers.add(subscriber);
    }

    /**
     * Notifies all registered subscribers about an incoming message by
     * wrapping it in an {@link Event} that carries this connection as the
     * source.
     *
     * @param message the message payload to broadcast; typically a
     *                {@link String} line received from the client
     */
    @Override
    public void notifySubscribers(Object message) {
        for (SubscriberInterface subscriber : subscribers) {
            subscriber.notify(new Event(this, message));
        }
    }

    /**
     * Starts the background receive thread that continuously reads lines from
     * the client socket and forwards them to all registered subscribers.
     *
     * <p>If a receive thread is already running, this method returns
     * immediately without starting a second thread. The thread terminates
     * automatically when the connection is closed, the socket is no longer
     * readable, or {@link #close()} is called, and it calls {@link #close()}
     * itself in a {@code finally} block to ensure resources are released.
     */
    public void acceptnewConnections() {
        if (receiveThread != null && receiveThread.isAlive()) {
            return;
        }

        receiveThread = new Thread(() -> {
            try {
                while (running && !socket.isClosed() && in.hasNextLine()) {
                    String message = in.nextLine();
                    System.out.println("Received: " + message);

                    // Notify subscribers
                    notifySubscribers(message);
                }
            } finally {
                close();
            }
        });

        receiveThread.start();
    }

    /**
     * Sends a message to the remote client by writing its string
     * representation followed by a newline to the output stream.
     *
     * @param inputs the object to send; {@link Object#toString()} is called
     *               to convert it to text before transmission
     */
    public void sendMessage(Object inputs) {
        out.println(inputs);
    }

    /**
     * Returns the username that was assigned to this connection after the
     * client authenticated or registered.
     *
     * @return the player's username, or {@code null} if not yet set
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Sets the username for this connection.
     *
     * @param username the username to assign; should not be {@code null}
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the numeric identifier assigned to this connection by the
     * server.
     *
     * @return the connection/player ID
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the numeric identifier for this connection.
     *
     * @param id the ID to assign
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Shuts down this connection gracefully.
     *
     * <p>Sets the running flag to {@code false}, interrupts the receive
     * thread if it is still alive, and closes the underlying socket. Any
     * {@link IOException} thrown while closing the socket is silently
     * ignored.
     */
    public void close() {
        running = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    // Game Logic Methods

    /**
     * Performs a full spin on this player's slot machine and stores the
     * result as the last spin.
     *
     * @return an unmodifiable-friendly list of symbol strings representing
     *         the outcome of the spin
     */
    @Override
    public List<String> spin() {
        lastSpin = slotmachine.spin();
        return lastSpin;
    }

    /**
     * Performs a reduced "dead spin" (a single-reel spin intended for
     * eliminated players) and stores the result as the last spin.
     *
     * @return a list of symbol strings representing the outcome of the dead
     *         spin
     */
    @Override
    public List<String> deadSpin() {
        lastSpin = slotmachine.spin(1);
        return lastSpin;
    }

    /**
     * Counts the number of heart symbols in the most recent spin result.
     *
     * @return the number of hearts found in {@link #getLastSpin()}
     */
    @Override
    public int countHearts() {
        return slotmachine.countHearts(lastSpin);
    }

    /**
     * Resets all per-round state so this player is ready for a new round.
     *
     * <p>Specifically, clears the submitted flag, resets the claimed-hearts
     * counter to zero, and replaces the last-spin list with a new empty list.
     */
    @Override
    public void resetForRound() {
        submitted = false;
        claimedHearts = 0;
        lastSpin = new ArrayList<>();
    }

    /**
     * Records that this player has consumed their one-time respin for the
     * current game.
     */
    @Override
    public void useRespin() {
        usedRespin = true;
    }

    /**
     * Eliminates this player from the current game by setting their alive
     * status to {@code false}.
     */
    @Override
    public void eliminate() {
        alive = false;
    }

    /**
     * Returns the display name of this player, which is identical to the
     * username assigned to this connection.
     *
     * @return the player's username
     */
    @Override
    public String getName() {
        return username;
    }

    /**
     * Indicates whether this player is still active in the current game.
     *
     * @return {@code true} if the player has not been eliminated;
     *         {@code false} otherwise
     */
    @Override
    public boolean isAlive() {
        return alive;
    }

    /**
     * Indicates whether this player has already used their respin in the
     * current game.
     *
     * @return {@code true} if {@link #useRespin()} has been called;
     *         {@code false} otherwise
     */
    @Override
    public boolean hasUsedRespin() {
        return usedRespin;
    }

    /**
     * Indicates whether this player has submitted their result for the
     * current round.
     *
     * @return {@code true} if the player has submitted; {@code false}
     *         otherwise
     */
    @Override
    public boolean hasSubmitted() {
        return submitted;
    }

    /**
     * Sets the submitted flag for the current round.
     *
     * @param submitted {@code true} to mark the player as having submitted
     *                  their result; {@code false} to clear the flag
     */
    @Override
    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    /**
     * Returns the number of hearts this player has claimed for the current
     * round.
     *
     * @return the claimed heart count
     */
    @Override
    public int getClaimedHearts() {
        return claimedHearts;
    }

    /**
     * Sets the number of hearts this player claims for the current round.
     *
     * @param claimedHearts the heart count to record
     */
    @Override
    public void setClaimedHearts(int claimedHearts) {
        this.claimedHearts = claimedHearts;
    }

    /**
     * Returns the list of symbols produced by the most recent spin (or dead
     * spin) performed by this player.
     *
     * @return the last spin result; an empty list if no spin has been
     *         performed yet this round
     */
    @Override
    public List<String> getLastSpin() {
        return lastSpin;
    }

    @Override
    public int getTotalHearts() {
        return totalHearts;
    }

    @Override
    public void addTotalHearts(int hearts) {
        totalHearts += hearts;
    }
}
