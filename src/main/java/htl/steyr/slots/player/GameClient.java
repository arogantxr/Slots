package htl.steyr.slots.player;

import htl.steyr.slots.GameApplication;
import htl.steyr.slots.controller.GameTableController;
import htl.steyr.slots.server.GameServer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * Represents the client side of a multiplayer game session.
 *
 * <p>Each player (including the host) owns a {@code GameClient} instance that
 * maintains the TCP connection to the {@link GameServer}, forwards incoming
 * messages to a registered {@link Consumer} handler, and provides a helper
 * to open the game-table window on the JavaFX application thread.</p>
 */
public class GameClient {

    private String playerName;
    private Socket socket;
    private PrintWriter out;
    private Consumer<String> messageHandler;

    /**
     * Creates a new client, opens a TCP connection to the server, and starts
     * the background listener thread.
     *
     * @param playerName the display name chosen by this player
     * @param serverIP   the IP address or hostname of the game server
     * @param serverPort the port the game server is listening on
     * @throws IOException if the connection cannot be established
     */
    public GameClient(String playerName, String serverIP, int serverPort) throws IOException {
        this.playerName = playerName;
        socket = new Socket(serverIP, serverPort);

        out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("acknowledged connection to server: " + socket);

        connect();
    }

    /**
     * Starts the background thread that continuously reads lines from the server
     * and dispatches them to the registered {@link #setMessageHandler message handler}.
     */
    public void connect() {
        System.out.println("Spielername: " + playerName);

        Thread listen = new Thread(() -> {
            try (Scanner in = new Scanner(socket.getInputStream())) {
                while (in.hasNextLine()) {
                    String message = in.nextLine();
                    System.out.println("Nachricht vom Server: " + message);

                    if (messageHandler != null) {
                        messageHandler.accept(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listen.start();
    }

    /**
     * Sends a line of text to the server.
     *
     * @param message the message to send
     */
    public void sendMessage(String message) {
        out.println(message);
    }

    /**
     * Registers a handler that receives every message sent by the server.
     * Replaces any previously registered handler.
     *
     * @param messageHandler the consumer that will process incoming server messages
     */
    public void setMessageHandler(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Opens the game-table window on the JavaFX application thread.
     *
     * @param currentPlayerName the name of the player who takes the first turn
     * @param server            the {@link GameServer} instance for the host, or
     *                          {@code null} for non-host clients
     * @throws IOException if the Game-view FXML resource cannot be loaded
     */
    public void startGameWindow(String currentPlayerName, GameServer server) throws IOException {
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(GameApplication.class.getResource("stages/Game-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load());

                GameTableController gameController = fxmlLoader.getController();
                gameController.setCurrentPlayerName(playerName);
                gameController.setGameClient(this);
                if (server != null) {
                    gameController.setGameServer(server);
                }

                stage.setTitle("Casino Slots - Multiplayer");
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Returns the display name of this player.
     *
     * @return the player name
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Closes the underlying TCP socket and releases all associated resources.
     */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Standalone test entry point — creates a client on localhost and reads
     * console input to send broadcast or private messages.
     *
     * @throws IOException if the connection cannot be established
     */
    public static void main() throws IOException {
        GameClient client = new GameClient("Player1", "localhost", 22222);

        Scanner consoleScanner = new Scanner(System.in);

        while (true) {
            String message = consoleScanner.nextLine();

            if (message.startsWith("@")) {
                String[] parts = message.split(" ", 2);

                String recipient = parts[0].substring(1);
                String privateMessage = parts[1];

                client.sendMessage("private;" + recipient + ";" + privateMessage);
            } else {
                client.sendMessage("broadcast;" + message);
            }
        }
    }
}
