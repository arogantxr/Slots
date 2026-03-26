package htl.steyr.slots.gameLogik.serverlogik;

import htl.steyr.slots.GameApplication;
import htl.steyr.slots.LobbyController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameServer{
    private final ServerSocket server;
    private boolean running;
    private final List<ServerConnection> clients = Collections.synchronizedList(new ArrayList<>());
    private Thread acceptnewConnections;

    public GameServer() throws IOException {
        server = new ServerSocket(12345);
        running = true;
    }

    public List<ServerConnection> getClientList() {
        return clients;
    }

    public GameServer(int port) throws IOException {
        server = new ServerSocket(port);
        running = true;
    }

    public void acceptConnections() throws IOException {

        acceptnewConnections = new Thread(() -> {
            while (running) {
                try {
                    ServerConnection cl = new ServerConnection(server.accept());
                    clients.add(cl);
                    cl.acceptnewConnections();
                    System.out.println("New client connected (Server): " + cl);
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        }, "slots-server-accept");

        acceptnewConnections.start();

    }

    public void startNewGame() throws IOException {
        Stage stage = new Stage();

                System.out.println("Starting a new game with " + clients.size() + " players.");
                //hier wird ein neuer Gametable generiert

        FXMLLoader fxmlLoader = new FXMLLoader(GameApplication.class.getResource("stages/Game-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Casino Slots - Multiplayer");
        stage.setScene(scene);
        stage.show();
    }


    public void stop() throws IOException {
        running = false;
        if (acceptnewConnections != null) acceptnewConnections.interrupt();
        synchronized (clients) {
            for (ServerConnection client : clients) {
                client.close();
            }
        }
        server.close();
    }

    public static void main(String[] args) {

        System.out.println("===SLOTS-SERVER===");
        int portposition = 22222;

        try {
            GameServer newserver = new GameServer(portposition);
            newserver.acceptConnections();

            System.out.println("Server is running on Port: " + portposition);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
