package htl.steyr.slots.controller;

import htl.steyr.slots.player.GameClient;
import htl.steyr.slots.server.GameServer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the pre-game lobby screen of the Casino Slots application.
 *
 * <p>This controller manages the lobby view that is shown after a player has
 * successfully hosted or joined a game session. It displays the list of players
 * currently connected to the server and listens for messages broadcast by the
 * server to keep that list up to date. Only the host player sees the "Start
 * Game" button; all other players wait until the host initiates the game.
 * When the game starts the lobby window is closed and each player's game window
 * is opened automatically.</p>
 */
public class LobbyController {

    /** ListView that displays the names of all players currently in the lobby. */
    @FXML
    public ListView<String> playerListView;

    /** Button that allows the host to start the game. Invisible for non-host players. */
    @FXML
    public Button startgameButton;

    /** Mutable list of player names that mirrors the items shown in {@link #playerListView}. */
    public List<String> playerNames = new ArrayList<>();

    /** The {@link GameServer} reference, set only for the host player; {@code null} for clients. */
    private GameServer server;

    /** The local player's {@link GameClient} used for sending and receiving messages. */
    private GameClient client;

    /** The unique identifier assigned to this client by the server. */
    private int myId;

    /** Whether the local player is the host of the current game session. */
    private boolean isHost = false;


    /**
     * Sets the {@link GameServer} for this lobby session.
     *
     * <p>This method is called only for the player who is hosting the game. Clients
     * that join a remote server will have this field remain {@code null}.</p>
     *
     * @param gameServer the server instance to associate with this controller
     */
    public void setGameServer(GameServer gameServer) {
        this.server = gameServer;
    }

    /**
     * Sets the {@link GameClient} for this lobby session and registers this
     * controller as the client's message handler.
     *
     * <p>After the handler is registered, the client's username is sent to the
     * server so that the server can broadcast an updated player list to all
     * connected clients.</p>
     *
     * @param gameClient the client instance to associate with this controller
     */
    public void setGameClient(GameClient gameClient) {
        this.client = gameClient;
        // Set this controller as the message handler for the client
        this.client.setMessageHandler(this::handleServerMessage);

        // Send username to server after handler is set to ensure broadcasts are received
        this.client.sendMessage("set-username;" + this.client.getPlayerName());
    }

    /**
     * Processes a message received from the server and updates the lobby UI
     * accordingly.
     *
     * <p>The following message types are handled:</p>
     * <ul>
     *   <li>{@code player-list;&lt;names&gt;} – a comma-separated list of player
     *       names; refreshes the {@link #playerListView}.</li>
     *   <li>{@code your-id;&lt;id&gt;} – the unique integer ID assigned to this
     *       client by the server; stored in {@link #myId}.</li>
     *   <li>{@code host-id;&lt;id&gt;} – the integer ID of the host player; sets
     *       {@link #isHost} and toggles the visibility of the start-game button.</li>
     *   <li>{@code game-start;&lt;currentPlayer&gt;} – signals that the host has
     *       started the game; closes the lobby window and opens the game window for
     *       non-host clients.</li>
     * </ul>
     *
     * <p>All UI updates are dispatched on the JavaFX Application Thread via
     * {@link Platform#runLater(Runnable)}.</p>
     *
     * @param message the raw message string received from the server
     */
    public void handleServerMessage(String message) {
        if (message.startsWith("player-list;")) {
            String[] parts = message.split(";", 2);
            if (parts.length == 2) {
                String[] players = parts[1].split(",");
                Platform.runLater(() -> {
                    playerNames.clear();
                    for (String player : players) {
                        if (!player.trim().isEmpty()) {
                            playerNames.add(player.trim());
                        }
                    }
                    playerListView.getItems().clear();
                    playerListView.getItems().addAll(playerNames);
                });
            }
        } else if (message.startsWith("your-id;")) {
            String[] parts = message.split(";", 2);
            if (parts.length == 2) {
                myId = Integer.parseInt(parts[1]);
            }
        } else if (message.startsWith("host-id;")) {
            String[] parts = message.split(";", 2);
            if (parts.length == 2) {
                int hostId = Integer.parseInt(parts[1]);
                isHost = (myId == hostId);
                Platform.runLater(() -> {
                    startgameButton.setVisible(isHost);
                });
            }
        } else if (message.startsWith("game-start;")) {
            String[] parts = message.split(";", 2);
            String currentPlayer = parts.length == 2 ? parts[1] : "Unknown";

            // Close lobby and open game window
            Platform.runLater(() -> {
                try {
                    ((Stage) playerListView.getScene().getWindow()).close();
                    if (!isHost) {
                        client.startGameWindow(currentPlayer, null);
                    }
                } catch (Exception e) {
                    System.err.println("Error starting game window: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Handles the "Start Game" button click (host only).
     *
     * <p>Closes the lobby window, broadcasts a game-start message to all connected
     * clients via the {@link GameServer}, and opens the game window for the host
     * player. Non-host clients open their game windows upon receiving the
     * broadcast.</p>
     *
     * @param actionEvent the JavaFX {@link ActionEvent} fired by the button
     * @throws IOException if the game window cannot be opened
     */
    public void startgameButtonClicked(ActionEvent actionEvent) throws IOException {
        // Close the lobby window
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();

        // Only the host broadcasts game start
        if (server != null) {
            // Broadcast game start message to all clients
            String currentPlayerName = client.getPlayerName();
            server.broadcastGameStart(currentPlayerName);

            // Start game window for host
            client.startGameWindow(currentPlayerName, server);
        }
        // For clients, the game window is opened when they receive the broadcast message
    }

    /**
     * Handles the "Leave" button click.
     *
     * <p>Closes the lobby window. If the local player is the host, the server
     * broadcasts an updated player list to inform the remaining clients.</p>
     *
     * @param actionEvent the JavaFX {@link ActionEvent} fired by the button
     */
    public void leaveButtonClicked(ActionEvent actionEvent) {
        // Close the Lobby-Stage
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        // update the playerlist
        if(server != null) this.server.broadcastPlayerList();
        stage.close();
    }
}
