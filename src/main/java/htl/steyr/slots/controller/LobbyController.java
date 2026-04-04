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

public class LobbyController {
    @FXML
    public ListView<String> playerListView;
    @FXML
    public Button startgameButton;
    public List<String> playerNames = new ArrayList<>();

    private GameServer server;
    private GameClient client;
    private int myId;
    private boolean isHost = false;


    
    public void setGameServer(GameServer gameServer) {
        this.server = gameServer;
    }

    public void setGameClient(GameClient gameClient) {
        this.client = gameClient;
        // Set this controller as the message handler for the client
        this.client.setMessageHandler(this::handleServerMessage);
        
        // Send username to server after handler is set to ensure broadcasts are received
        this.client.sendMessage("set-username;" + this.client.getPlayerName());
    }

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

    public void leaveButtonClicked(ActionEvent actionEvent) {
        // Schließe die Lobby-Stage
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        // Stoppe ggf. den Updater
        stage.close();
    }
}
