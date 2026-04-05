package htl.steyr.slots.controller;

import htl.steyr.slots.GameApplication;
import htl.steyr.slots.player.GameClient;
import htl.steyr.slots.server.GameServer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Controller for the home screen of the Casino Slots application.
 *
 * <p>This controller manages the initial view where the player enters their name
 * and chooses whether to host a new game session or join an existing one.
 * Hosting creates a {@link GameServer} and automatically connects the host as the
 * first {@link GameClient}. Joining connects an existing {@link GameClient} to a
 * remote server identified by an IP address and port number.
 * After a successful connection is established the controller transitions the UI
 * to the pre-game lobby screen.</p>
 */
public class HomescreenController {

    private static final Logger LOGGER = Logger.getLogger(HomescreenController.class.getName());

    @FXML
    private TextField playerNameField;

    @FXML
    private Label errorLabel;

    @FXML
    private VBox hostInputBox;

    @FXML
    private VBox joinInputBox;

    @FXML
    private VBox mainButtonBox;

    @FXML
    private TextField portField;

    @FXML
    private TextField ipField;

    @FXML
    private TextField portFieldJoin;

    /** The validated player name entered by the user. */
    private String playerName;

    /** The {@link GameServer} instance created when the local player acts as host. */
    private static GameServer newserver;

    /** The {@link GameClient} instance used by the local player (host or joining client). */
    private static GameClient newclient;

    /**
     * Validates the player name entered in {@code playerNameField}.
     *
     * <p>If the field is empty an error message is displayed and {@code false} is
     * returned. Otherwise the trimmed name is stored in {@link #playerName}, the
     * error label is hidden, and {@code true} is returned.</p>
     *
     * @return {@code true} if the player name is non-empty; {@code false} otherwise
     */
    private boolean validatePlayerName() {
        String name = playerNameField.getText().trim();
        if (name.isEmpty()) {
            showError("Bitte einen Spielernamen eingeben!");
            return false;
        }
        this.playerName = name;
        hideError();
        return true;
    }

    /**
     * Handles the "Host Game" button click.
     *
     * <p>Validates the player name and, if valid, hides the main button panel and
     * reveals the host-configuration input box where the user can enter a port
     * number for the server.</p>
     */
    @FXML
    private void handleHostGame() {
        if (validatePlayerName()) {
            mainButtonBox.setVisible(false);
            hostInputBox.setVisible(true);
            portField.clear();
        }
    }

    /**
     * Handles the "Join Game" button click.
     *
     * <p>Validates the player name and, if valid, hides the main button panel and
     * reveals the join-configuration input box where the user can enter the server
     * IP address and port number.</p>
     */
    @FXML
    private void handleJoinGame() {
        if (validatePlayerName()) {
            mainButtonBox.setVisible(false);
            joinInputBox.setVisible(true);
            ipField.clear();
            portFieldJoin.clear();
        }
    }

    /**
     * Handles the "Start Server" button click in the host-configuration panel.
     *
     * <p>Reads and validates the port number entered by the user. If the port is
     * valid (1024–65535) a new {@link GameServer} is created and started, a local
     * {@link GameClient} is connected to it on {@code localhost}, and the lobby
     * screen is shown. Any IO or number-format errors are reported to the user via
     * the error label.</p>
     *
     * @param actionEvent the JavaFX {@link ActionEvent} fired by the button
     */
    @FXML
    private void handleStartServer(ActionEvent actionEvent) {
        String portStr = portField.getText().trim();

        if (portStr.isEmpty()) {
            showError("Bitte Portnummer eingeben!");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                showError("Port muss zwischen 1024 und 65535 liegen!");
                return;
            }

            System.out.printf("\n\n\n" +
                    "===SLOTS-SERVER===");

            try {
                newserver = new GameServer(port);
                newserver.acceptConnections();

                // Client für den Host starten (Verbindung zu localhost)
                newclient = new GameClient(playerName, "localhost", port);

                // Übergabe des Servers an die Lobby
                viewLobby(actionEvent);

                System.out.println("Server is running on Port: " + port);
            } catch (IOException e) {
                e.printStackTrace();
            }


        } catch (NumberFormatException e) {
            showError("Ungültige Portnummer!");
        }

        // Connection zum Server herstellen nachdem server gestartet wurde


    }


    /**
     * Handles the "Connect" button click in the join-configuration panel.
     *
     * <p>Validates the player name, IP address and port number. If all inputs are
     * valid a new {@link GameClient} is created and connected to the specified
     * server, then the lobby screen is shown. IP addresses that fail the
     * four-octet validation or the special value {@code "localhost"} are rejected
     * or accepted respectively. IO and number-format errors are shown via the error
     * label.</p>
     *
     * @param actionEvent the JavaFX {@link ActionEvent} fired by the button
     */
    @FXML
    private void handleConnect(ActionEvent actionEvent) {

        if (!validatePlayerName()) {
            return;
        }

        String ip = ipField.getText().trim();
        String portStr = portFieldJoin.getText().trim();

        if (ip.isEmpty() || portStr.isEmpty()) {
            showError("Bitte IP-Adresse und Portnummer eingeben!");
            return;
        }

        if (!isValidIP(ip)) {
            showError("Ungültige IP-Adresse!");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                showError("Port muss zwischen 1024 und 65535 liegen!");
                return;
            }

            // Create client first
            newclient = new GameClient(playerName, ip, port);
            LOGGER.info("name of player: " + newclient.getPlayerName());

            // Then show lobby
            viewLobby(actionEvent);

        } catch (NumberFormatException e) {
            showError("Ungültige Portnummer!");
        } catch (IOException e) {
            showError("Verbindung fehlgeschlagen! Prüfe IP-Adresse und Port.");
        }
    }


    /**
     * Loads and displays the lobby screen, passing the current {@link GameServer}
     * and {@link GameClient} to the {@link LobbyController}.
     *
     * <p>The FXML resource {@code stages/Lobby-view.fxml} is loaded, its controller
     * is configured with the active server and client references, a new
     * {@link Stage} is shown, and the current home-screen stage is closed.</p>
     *
     * @param actionEvent the JavaFX {@link ActionEvent} whose source node provides
     *                    a reference to the current stage so it can be closed
     * @throws IOException if the FXML resource cannot be loaded
     */
    // Bestehende viewLobby für Joins ohne Server
    public void viewLobby(ActionEvent actionEvent) throws IOException {


        FXMLLoader fxmlLoader = new FXMLLoader(
                GameApplication.class.getResource("stages/Lobby-view.fxml")
        );

        Scene newscene = new Scene(fxmlLoader.load());

        // Pass the server to the LobbyController (can be null for clients)
        LobbyController lobbyController = fxmlLoader.getController();
        if (newserver != null) {
            lobbyController.setGameServer(newserver);
        }
        if (newclient != null) {
            lobbyController.setGameClient(newclient);
        }

        Stage newstage = new Stage();
        newstage.setScene(newscene);
        newstage.setTitle("Casino Slots - Multiplayer");
        newstage.show();

        // altes Fenster schließen
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();


    }

    /**
     * Handles the "Cancel" button click in the host-configuration panel.
     *
     * <p>Hides the host-input box, shows the main button panel again, and clears
     * any visible error message.</p>
     */
    @FXML
    private void handleCancelHost() {
        hostInputBox.setVisible(false);
        mainButtonBox.setVisible(true);
        hideError();
    }

    /**
     * Handles the "Cancel" button click in the join-configuration panel.
     *
     * <p>Hides the join-input box, shows the main button panel again, and clears
     * any visible error message.</p>
     */
    @FXML
    private void handleCancelJoin() {
        joinInputBox.setVisible(false);
        mainButtonBox.setVisible(true);
        hideError();
    }

    /**
     * Handles the "Exit" button click.
     *
     * <p>Closes the application window that contains the player-name field.</p>
     */
    @FXML
    private void handleExit() {
        Stage stage = (Stage) playerNameField.getScene().getWindow();
        stage.close();
    }


    /**
     * Validates whether the given string is a valid IP address or the literal
     * {@code "localhost"}.
     *
     * <p>A valid IPv4 address must consist of exactly four dot-separated segments,
     * each being an integer in the range 0–255. The string {@code "localhost"} is
     * always considered valid regardless of case.</p>
     *
     * @param ip the IP address string to validate
     * @return {@code true} if {@code ip} is {@code "localhost"} or a valid IPv4
     *         address; {@code false} otherwise
     */
    private boolean isValidIP(String ip) {
        if (ip.equalsIgnoreCase("localhost")) {
            return true;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Displays an error message in the error label.
     *
     * @param message the error text to display
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Hides the error label from the user interface.
     */
    private void hideError() {
        errorLabel.setVisible(false);
    }

    /**
     * Returns the static {@link GameServer} instance that was created when the
     * local player chose to host a game session.
     *
     * @return the current {@link GameServer}, or {@code null} if no server has
     *         been started in this session
     */
    public static GameServer getNewserver() {
        return newserver;
    }
}
