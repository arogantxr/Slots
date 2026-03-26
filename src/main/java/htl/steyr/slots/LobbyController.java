package htl.steyr.slots;

import htl.steyr.slots.gameLogik.serverlogik.GameServer;
import htl.steyr.slots.gameLogik.serverlogik.ServerConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

public class LobbyController {
    @FXML
    public ListView<String> playerListView;

    private final ObservableList<String> clientNames = FXCollections.observableArrayList();

    private ScheduledExecutorService executor;
    private static GameServer server = HomescreenController.getNewserver();


    public void initialize(){
        Thread updatelist = new Thread(() -> {

            try {
                for(ServerConnection connection : server.getClientList()){
                    if(!playerListView.getItems().contains(connection.getUsername())) {
                        clientNames.add(connection.getUsername());
                    }
                }
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            while (true) {

            }
        }, "lobby-updater");
        updatelist.start();
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
