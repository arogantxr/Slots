package htl.steyr.slots;

import htl.steyr.slots.gameLogik.clientlogik.GameClient;
import htl.steyr.slots.gameLogik.serverlogik.GameServer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class HomescreenController {

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


            // Server starten (hier muss die Slots_Server Klasse aufgerufen werden)
            // Slots_Server server = new Slots_Server(playerName, port);
            // server.start();


            System.out.printf("\n\n\n" +
                    "===SLOTS-SERVER===");

            try {
                GameServer newserver = new GameServer(port);
                newserver.acceptConnections();

                System.out.println("Server is running on Port: " + port);
            } catch (IOException e) {
                e.printStackTrace();
            }


            // Client für den Host starten (Verbindung zu localhost)
            GameClient hostClient = new GameClient(playerName, "localhost", port);


            System.out.println("Host-Client started... " + hostClient.getPlayerName());


        } catch (IOException e) {
            e.printStackTrace();
        }

        // Connection zum Server herstellen nachdem server gestartet wurde

        /**
         * String ip = ipField.getText().trim();
         *
         *          if (ip.isEmpty() || portStr.isEmpty()) {
         *          showError("Bitte IP-Adresse und Portnummer eingeben!");
         *          return;
         *          }
         *
         *          if (!isValidIP(ip)) {
         *          showError("Ungültige IP-Adresse!");
         *          return;
         *          }
         *
         *
         *         try {
         *             int port = Integer.parseInt(portStr);
         *             if (port < 1024 || port > 65535) {
         *                 showError("Port muss zwischen 1024 und 65535 liegen!");
         *                 return;
         *             }
         *
         *             System.out.println("Verbindung zu " + ip + ":" + port + " als " + playerName);
         *             //Client Klasse Aufrufen
         *             hideError();
         *
         *         } catch (NumberFormatException e) {
         *             showError("Ungültige Portnummer!");
         *
         *         }
         */
    }


    @FXML
    private void handleConnect() {

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


            GameClient newclient = new GameClient(playerName, ip, port);


            System.out.println("name of player...." + newclient.getPlayerName());




        } catch (NumberFormatException e) {
            showError("Ungültige Portnummer!");
        } catch (IOException e) {
            showError("Verbindung fehlgeschlagen! Prüfe IP-Adresse und Port.");
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
}
