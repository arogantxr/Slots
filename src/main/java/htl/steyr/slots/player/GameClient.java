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


public class GameClient {
    private String playerName;
    private Socket socket;
    private PrintWriter out;
    private Consumer<String> messageHandler;

    public GameClient(String playerName, String serverIP, int serverPort) throws IOException {
        this.playerName = playerName;
        socket = new Socket(serverIP, serverPort);



        out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("acknowledged connection to server: " + socket);

        connect();
    }

    public void connect() {
        System.out.println("Spielername: " + playerName);

        Thread listen = new Thread(() -> {
            try (Scanner in = new Scanner(socket.getInputStream())) {
                while (in.hasNextLine()) {
                    String message = in.nextLine();
                    System.out.println("Nachricht vom Server: " + message);
                    
                    // Call message handler if set
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

    public void sendMessage(String message) {
        out.println(message);
    }

    public void setMessageHandler(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

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

    public String getPlayerName() {
        return playerName;
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main() throws IOException {
        GameClient client = new GameClient("Player1", "localhost", 22222);

        Scanner consoleScanner = new Scanner(System.in);

        while (true) {
            String message = consoleScanner.nextLine();

            if(message.startsWith("@")){
                String[] parts = message.split(" ", 2);

                String recipient = parts[0].substring(1); // Entfernt das '@' Zeichen
                String privateMessage = parts[1];

                client.sendMessage("private;"+ recipient + ";" + privateMessage);
            }else{
                client.sendMessage( "broadcast;" + message);
            }
        }
        // Hier weitere Aktionen durchführen, z.B. Nachrichten senden/empfangen

    }
}
