package htl.steyr.slots.server;

import htl.steyr.slots.GameApplication;
import htl.steyr.slots.interfaces.Event;
import htl.steyr.slots.interfaces.SubscriberInterface;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class GameServer implements SubscriberInterface{
    private final ServerSocket server;
    private boolean running;
    private final List<ServerConnection> clients = Collections.synchronizedList(new ArrayList<>());
    private Thread acceptnewConnections;
    private static final int MAX_PLAYERS = 4;
    private int nextId = 0;
    private ServerConnection hostClient;
    private int hostId;
    private BiConsumer<ServerConnection, String> actionHandler;

    public GameServer() throws IOException {
        server = new ServerSocket(12345);
        running = true;
    }

    public List<ServerConnection> getClientList() {
        return clients;
    }

    public int getMaxPlayers() {
        return MAX_PLAYERS;
    }

    public boolean isFull() {
        return clients.size() >= MAX_PLAYERS;
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
                    
                    // Überprüfe ob maximal 4 Spieler erreicht wurden
                    if (clients.size() >= MAX_PLAYERS) {
                        System.out.println("Server full! Maximum of " + MAX_PLAYERS + " players reached. Rejecting connection.");
                        cl.close();
                        continue;
                    }
                    
                    // Subscribe to client messages
                    cl.addSubscriber(this);
                    
                    // Assign ID
                    cl.setId(nextId++);
                    
                    // Check if this is the host (first client from localhost)
                    if (clients.isEmpty() && cl.socket.getInetAddress().isLoopbackAddress()) {
                        hostClient = cl;
                        hostId = cl.getId();
                        System.out.println("Host client connected: " + cl + " (ID: " + hostId + ")");
                    }
                    
                    clients.add(cl);
                    cl.acceptnewConnections();
                    System.out.println("New client connected (Server): " + cl + " (" + clients.size() + "/" + MAX_PLAYERS + ")");
                    
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        }, "slots-server-accept");

        acceptnewConnections.start();

    }

    public void setActionHandler(BiConsumer<ServerConnection, String> handler) {
        this.actionHandler = handler;
    }

    @Override
    public void notify(Event event) {
        String message = (String) event.message();
        ServerConnection client = (ServerConnection) event.source();
        if (message.startsWith("set-username;")) {
            String[] parts = message.split(";", 2);
            if (parts.length == 2) {
                client.setUsername(parts[1]);
                System.out.println("Username set to: " + client.getUsername());
                // Send ID to client
                client.sendMessage("your-id;" + client.getId());
                // Broadcast host ID
                broadcastHostId();
                // Broadcast updated player list
                broadcastPlayerList();
            }
        } else if (message.startsWith("action-")) {
            if (actionHandler != null) {
                actionHandler.accept(client, message);
            }
        }
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

    public void broadcastPlayerList() {
        synchronized (clients) {
            // Build player list message
            StringBuilder playerListMessage = new StringBuilder("player-list;");
            for (int i = 0; i < clients.size(); i++) {
                ServerConnection client = clients.get(i);
                if (client.getUsername() != null) {
                    playerListMessage.append(client.getUsername());
                    if (i < clients.size() - 1) {
                        playerListMessage.append(",");
                    }
                }
            }
            
            // Send to all clients
            for (ServerConnection client : clients) {
                client.sendMessage(playerListMessage.toString());
            }
        }
    }

    public void broadcastHostId() {
        synchronized (clients) {
            String hostIdMessage = "host-id;" + hostId;
            for (ServerConnection client : clients) {
                client.sendMessage(hostIdMessage);
            }
        }
    }

    public void broadcastGameStart(String currentPlayerName) {
        synchronized (clients) {
            String gameStartMessage = "game-start;" + currentPlayerName;
            for (ServerConnection client : clients) {
                client.sendMessage(gameStartMessage);
            }
        }
    }

    public void broadcastTurnUpdate(String currentPlayerName) {
        synchronized (clients) {
            String turnMessage = "turn-update;" + currentPlayerName;
            for (ServerConnection client : clients) {
                client.sendMessage(turnMessage);
            }
        }
    }

    public void broadcastGameState(List<String> playerStates) {
        synchronized (clients) {
            StringBuilder stateMessage = new StringBuilder("game-state;");
            for (int i = 0; i < playerStates.size(); i++) {
                stateMessage.append(playerStates.get(i));
                if (i < playerStates.size() - 1) {
                    stateMessage.append("|");
                }
            }
            String message = stateMessage.toString();
            for (ServerConnection client : clients) {
                client.sendMessage(message);
            }
        }
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
