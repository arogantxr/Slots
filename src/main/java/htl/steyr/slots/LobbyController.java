package htl.steyr.slots;

import htl.steyr.slots.gameLogik.serverlogik.GameServer;
import htl.steyr.slots.gameLogik.serverlogik.ServerConnection;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LobbyController {
    @FXML
    public ListView<String> playerListView;
    public List<String> playerNames = new ArrayList<>();

    private static GameServer server = HomescreenController.getNewserver();


    public void initialize(){

            updatePlayerList();
    }

    public void updatePlayerList() {

        new Thread (()-> {
            while(true){
                for(ServerConnection client : server.getClientList()){
                    if(client.getUsername() != null && !playerNames.contains(client.getUsername())){
                        playerNames.add(client.getUsername());
                    }
                }

                // UI-Updates müssen auf dem FX-Thread ausgeführt werden
                Platform.runLater(() -> {
                    playerListView.getItems().clear();
                    playerListView.getItems().addAll(playerNames);
                });

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();


    }


    public void startgameButtonClicked(ActionEvent actionEvent) throws IOException {
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();


        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(GameApplication.class.getResource("stages/Game-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Casino Slots - Multiplayer");
        stage.setScene(scene);
        stage.show();
    }

    public void leaveButtonClicked(ActionEvent actionEvent) {
        // Schließe die Lobby-Stage
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        // Stoppe ggf. den Updater
        stage.close();
    }
}
