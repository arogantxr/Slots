package htl.steyr.slots.server;

import htl.steyr.slots.GameApplication;
import htl.steyr.slots.interfaces.Event;
import htl.steyr.slots.interfaces.SubscriberInterface;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Central multiplayer server for the Casino Slots game.
 *
 * <p>This server accepts TCP connections from up to {@code MAX_PLAYERS} clients simultaneously.
 * Each accepted connection is wrapped in a {@link ServerConnection} and assigned a unique integer
 * ID. The first client that connects from the loopback address is designated the host.
 *
 * <p>{@code GameServer} implements {@link SubscriberInterface} so that it can receive events
 * published by individual {@link ServerConnection} instances. Incoming messages are dispatched
 * according to their prefix:
 * <ul>
 *   <li>{@code set-username;…} — registers the player's display name and broadcasts updated
 *       lobby state to all clients.</li>
 *   <li>{@code action-…} — routed to an externally injected {@link BiConsumer} action handler,
 *       allowing game-logic components to react to player actions without coupling them to the
 *       server directly.</li>
 * </ul>
 */
public class GameServer implements SubscriberInterface {
    private final ServerSocket server;
    private boolean running;
    private final List<ServerConnection> clients = Collections.synchronizedList(new ArrayList<>());
    private Thread acceptnewConnections;
    private static final int MAX_PLAYERS = 4;
    private int nextId = 0;
    private ServerConnection hostClient;
    private int hostId;
    private BiConsumer<ServerConnection, String> actionHandler;

    /**
     * Creates a new {@code GameServer} listening on the default port {@code 12345}.
     *
     * @throws IOException if the server socket cannot be opened on port 12345
     */
    public GameServer() throws IOException {
        server = new ServerSocket(12345);
        running = true;
    }

    /**
     * Returns the thread-safe list of currently connected clients.
     *
     * @return a synchronized {@link List} of {@link ServerConnection} objects representing
     *         all clients that are currently connected to this server
     */
    public List<ServerConnection> getClientList() {
        return clients;
    }

    /**
     * Returns the maximum number of players allowed on this server.
     *
     * @return the value of the {@code MAX_PLAYERS} constant
     */
    public int getMaxPlayers() {
        return MAX_PLAYERS;
    }

    /**
     * Indicates whether the server has reached its maximum player capacity.
     *
     * @return {@code true} if the number of connected clients equals or exceeds
     *         {@code MAX_PLAYERS}; {@code false} otherwise
     */
    public boolean isFull() {
        return clients.size() >= MAX_PLAYERS;
    }

    /**
     * Creates a new {@code GameServer} listening on the specified port.
     *
     * <p>Use this constructor when a port other than the default ({@code 12345}) is required,
     * for example when multiple server instances must run on the same host.
     *
     * @param port the TCP port number on which the server socket will be opened
     * @throws IOException if the server socket cannot be opened on the given port
     */
    public GameServer(int port) throws IOException {
        server = new ServerSocket(port);
        running = true;
    }

    /**
     * Starts a background thread that continuously accepts incoming client connections.
     *
     * <p>The thread runs until {@link #stop()} is called. For each accepted socket:
     * <ol>
     *   <li>If the server is already full (i.e., {@code clients.size() >= MAX_PLAYERS}), the
     *       connection is immediately closed and the loop continues.</li>
     *   <li>Otherwise the socket is wrapped in a {@link ServerConnection}, subscribed to this
     *       server via {@link SubscriberInterface}, assigned the next available ID, and added to
     *       the client list.</li>
     *   <li>If the new connection is the first client and originates from the loopback address it
     *       is designated as the host.</li>
     * </ol>
     *
     * @throws IOException not thrown directly by this method; declared for API consistency
     */
    public void acceptConnections() throws IOException {

        acceptnewConnections = new Thread(() -> {
            while (running) {
                try {
                    ServerConnection cl = new ServerConnection(server.accept());

                    // Überprüfe ob maximal 4 Spieler erreicht wurden
                    if (clients.size() >= MAX_PLAYERS) {
                        System.out.println("Server full! Maximum of " + MAX_PLAYERS + " players reached. Rejecting connection.");
                        cl.close();
                        continue;
                    }

                    // Subscribe to client messages
                    cl.addSubscriber(this);

                    // Assign ID
                    cl.setId(nextId++);

                    // Check if this is the host (first client from localhost)
                    if (clients.isEmpty() && cl.socket.getInetAddress().isLoopbackAddress()) {
                        hostClient = cl;
                        hostId = cl.getId();
                        System.out.println("Host client connected: " + cl + " (ID: " + hostId + ")");
                    }

                    clients.add(cl);
                    cl.acceptnewConnections();
                    System.out.println("New client connected (Server): " + cl + " (" + clients.size() + "/" + MAX_PLAYERS + ")");

                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        }, "slots-server-accept");

        acceptnewConnections.start();

    }

    /**
     * Registers the handler that will be invoked for every {@code action-…} message received
     * from any client.
     *
     * <p>The {@link BiConsumer} receives the originating {@link ServerConnection} as its first
     * argument and the raw message string as its second argument, allowing the caller to
     * identify the player and parse the action payload.
     *
     * @param handler the {@link BiConsumer} to call when an {@code action-…} message arrives;
     *                may be {@code null} to disable action routing
     */
    public void setActionHandler(BiConsumer<ServerConnection, String> handler) {
        this.actionHandler = handler;
    }

    /**
     * Handles events published by connected {@link ServerConnection} instances.
     *
     * <p>Two message types are recognised:
     * <ul>
     *   <li><b>{@code set-username;&lt;name&gt;}</b> — stores the given username on the client
     *       object, replies with {@code your-id;&lt;id&gt;} so the client knows its own ID,
     *       and then broadcasts the current host ID and the updated player list to all
     *       clients.</li>
     *   <li><b>{@code action-…}</b> — delegates the raw message string to the injected
     *       {@link #setActionHandler(BiConsumer) action handler} together with the originating
     *       {@link ServerConnection}, if a handler has been set.</li>
     * </ul>
     *
     * @param event the event published by a {@link ServerConnection}; {@code event.source()}
     *              must be a {@link ServerConnection} and {@code event.message()} must be a
     *              {@link String}
     */
    @Override
    public void notify(Event event) {
        String message = (String) event.message();
        ServerConnection client = (ServerConnection) event.source();
        if (message.startsWith("set-username;")) {
            String[] parts = message.split(";", 2);
            if (parts.length == 2) {
                client.setUsername(parts[1]);
                System.out.println("Username set to: " + client.getUsername());
                // Send ID to client
                client.sendMessage("your-id;" + client.getId());
                // Broadcast host ID
                broadcastHostId();
                // Broadcast updated player list
                broadcastPlayerList();
            }
        } else if (message.startsWith("action-")) {
            if (actionHandler != null) {
                actionHandler.accept(client, message);
            }
        }
    }

    /**
     * Opens the JavaFX game view in a new window, effectively starting a new multiplayer game
     * session for all currently connected clients.
     *
     * <p>This method must be called on the JavaFX Application Thread.
     *
     * @throws IOException if the FXML resource {@code stages/Game-view.fxml} cannot be loaded
     */
    public void startNewGame() throws IOException {
        Stage stage = new Stage();

        System.out.println("Starting a new game with " + clients.size() + " players.");
        //a new GameTable is created and the game starts

        FXMLLoader fxmlLoader = new FXMLLoader(GameApplication.class.getResource("stages/Game-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Casino Slots - Multiplayer");
        stage.setScene(scene);
        stage.show();
    }


    /**
     * Stops the server gracefully.
     *
     * <p>Sets the {@code running} flag to {@code false}, interrupts the connection-accept thread,
     * closes every active {@link ServerConnection}, and finally closes the underlying
     * {@link ServerSocket}.
     *
     * @throws IOException if closing the server socket or any client connection fails
     */
    public void stop() throws IOException {
        running = false;
        if (acceptnewConnections != null) acceptnewConnections.interrupt();
        synchronized (clients) {
            for (ServerConnection client : clients) {
                client.close();
            }
        }
        server.close();
    }

    /**
     * Broadcasts the current list of connected player usernames to every client.
     *
     * <p>Wire format: {@code player-list;&lt;name1&gt;,&lt;name2&gt;,…}
     * <br>Only clients whose username has already been set (i.e., is non-{@code null}) are
     * included in the list. The names are separated by commas with no trailing comma.
     */
    public void broadcastPlayerList() {
        synchronized (clients) {
            // Build player list message
            StringBuilder playerListMessage = new StringBuilder("player-list;");
            for (int i = 0; i < clients.size(); i++) {
                ServerConnection client = clients.get(i);
                if (client.getUsername() != null) {
                    playerListMessage.append(client.getUsername());
                    if (i < clients.size() - 1) {
                        playerListMessage.append(",");
                    }
                }
            }

            // Send to all clients
            for (ServerConnection client : clients) {
                client.sendMessage(playerListMessage.toString());
            }
        }
    }

    /**
     * Broadcasts the host player's ID to every connected client.
     *
     * <p>Wire format: {@code host-id;&lt;hostId&gt;}
     * <br>Clients use this information to determine who is allowed to start the game.
     */
    public void broadcastHostId() {
        synchronized (clients) {
            String hostIdMessage = "host-id;" + hostId;
            for (ServerConnection client : clients) {
                client.sendMessage(hostIdMessage);
            }
        }
    }

    /**
     * Broadcasts a game-start notification to every connected client, including the name of the
     * player whose turn it is first.
     *
     * <p>Wire format: {@code game-start;&lt;currentPlayerName&gt;}
     *
     * @param currentPlayerName the username of the player who takes the first turn
     */
    public void broadcastGameStart(String currentPlayerName) {
        synchronized (clients) {
            String gameStartMessage = "game-start;" + currentPlayerName;
            for (ServerConnection client : clients) {
                client.sendMessage(gameStartMessage);
            }
        }
    }

    /**
     * Broadcasts a turn-update notification to every connected client, indicating whose turn it
     * currently is.
     *
     * <p>Wire format: {@code turn-update;&lt;currentPlayerName&gt;}
     *
     * @param currentPlayerName the username of the player whose turn has just begun
     */
    public void broadcastTurnUpdate(String currentPlayerName) {
        synchronized (clients) {
            String turnMessage = "turn-update;" + currentPlayerName;
            for (ServerConnection client : clients) {
                client.sendMessage(turnMessage);
            }
        }
    }

    /**
     * Sends an arbitrary pre-formatted message to every connected client.
     *
     * <p>Use this method when none of the specialised broadcast methods match the required wire
     * format.
     *
     * @param message the raw message string to deliver to all clients
     */
    public void broadcastMessage(String message) {
        synchronized (clients) {
            for (ServerConnection client : clients) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Broadcasts the current game state, containing one state token per player, to every
     * connected client.
     *
     * <p>Wire format: {@code game-state;&lt;state0&gt;|&lt;state1&gt;|…}
     * <br>Individual player state tokens are separated by a pipe character ({@code |}). The order
     * of tokens corresponds to the order of elements in the supplied list.
     *
     * @param playerStates a {@link List} of state strings, one per player; must not be
     *                     {@code null}
     */
    public void broadcastGameState(List<String> playerStates) {
        synchronized (clients) {
            StringBuilder stateMessage = new StringBuilder("game-state;");
            for (int i = 0; i < playerStates.size(); i++) {
                stateMessage.append(playerStates.get(i));
                if (i < playerStates.size() - 1) {
                    stateMessage.append("|");
                }
            }
            String message = stateMessage.toString();
            for (ServerConnection client : clients) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Application entry point for running the server as a standalone process.
     *
     * <p>Creates a {@link GameServer} on port {@code 22222}, starts accepting connections, and
     * prints a confirmation message. Any {@link IOException} during startup is printed to the
     * standard error stream.
     *
     * @param args command-line arguments (not evaluated)
     */
    public static void main(String[] args) {

        System.out.println("===SLOTS-SERVER===");
        int portposition = 22222;

        try {
            GameServer newserver = new GameServer(portposition);
            newserver.acceptConnections();

            System.out.println("Server is running on Port: " + portposition);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
