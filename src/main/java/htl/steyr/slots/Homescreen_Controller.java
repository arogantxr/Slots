package htl.steyr.slots;

import htl.steyr.slots.gameLogik.clientlogik.GameClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class Homescreen_Controller {

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
    private void handleStartServer() {
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

            System.out.println("Server wird gestartet auf Port: " + port + " von Spieler: " + playerName);

            // Server starten (hier muss die Slots_Server Klasse aufgerufen werden)
            // Slots_Server server = new Slots_Server(playerName, port);
            // server.start();

            // Client für den Host starten (Verbindung zu localhost)
            GameClient hostClient = new GameClient(playerName, "localhost", port);

            try {
                hostClient.connect();
                hideError();

                //zu game wechseln

            } catch (Exception e) {
                showError("Server-Verbindung fehlgeschlagen: " + e.getMessage());
            }

        } catch (NumberFormatException e) {
            showError("Ungültige Portnummer!");
        }

        // Connection zum Server herstellen nachdem server gestartet wurde

        String ip = ipField.getText().trim();

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

            System.out.println("Verbindung zu " + ip + ":" + port + " als " + playerName);
            //Client Klasse Aufrufen
            hideError();

        } catch (NumberFormatException e) {
            showError("Ungültige Portnummer!");
        }
    }




    @FXML
    private void handleConnect() {
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

            // Client instanziieren und Daten übergeben
            GameClient client = new GameClient(playerName, ip, port);

            // Verbindung herstellen
            try {
                client.connect();
                hideError();

                // zu game wechseln

            } catch (Exception e) {
                showError("Verbindung fehlgeschlagen: " + e.getMessage());
            }

        } catch (NumberFormatException e) {
            showError("Ungültige Portnummer!");
        }
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
}
