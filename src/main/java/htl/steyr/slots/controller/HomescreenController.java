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

    private String playerName;

    private static GameServer newserver;
    private static GameClient newclient;

    /**
     * Validiert den Spielernamen
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

    @FXML
    private void handleHostGame() {
        if (validatePlayerName()) {
            mainButtonBox.setVisible(false);
            hostInputBox.setVisible(true);
            portField.clear();
        }
    }

    @FXML
    private void handleJoinGame() {
        if (validatePlayerName()) {
            mainButtonBox.setVisible(false);
            joinInputBox.setVisible(true);
            ipField.clear();
            portFieldJoin.clear();
        }
    }

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

    @FXML
    private void handleCancelHost() {
        hostInputBox.setVisible(false);
        mainButtonBox.setVisible(true);
        hideError();
    }

    @FXML
    private void handleCancelJoin() {
        joinInputBox.setVisible(false);
        mainButtonBox.setVisible(true);
        hideError();
    }

    @FXML
    private void handleExit() {
        Stage stage = (Stage) playerNameField.getScene().getWindow();
        stage.close();
    }


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

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }

    public static GameServer getNewserver() {
        return newserver;
    }
}
