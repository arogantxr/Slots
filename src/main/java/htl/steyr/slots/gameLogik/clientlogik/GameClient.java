package htl.steyr.slots.gameLogik.clientlogik;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;


public class GameClient {
    private String playerName;
    private String serverIP;
    private int serverPort;
    private Socket socket;

    private PrintWriter out;

    public GameClient(String playerName, String serverIP, int serverPort) throws IOException {
        this.playerName = playerName;
        socket = new Socket(serverIP, serverPort);

        this.serverIP = serverIP;
        this.serverPort = serverPort;

        out = new PrintWriter(socket.getOutputStream(), true);
        connect();
    }

    public void connect() {
        System.out.println("Verbunden mit Server: " + serverIP + ":" + serverPort);
        System.out.println("Spielername: " + playerName);

        Thread listen = new Thread(() -> {
            try (Scanner in = new Scanner(socket.getInputStream())) {
                while (in.hasNextLine()) {
                    String message = in.nextLine();
                    System.out.println("Nachricht vom Server: " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        listen.setDaemon(true);
        listen.start();
    }

    public void sendMessage(String message) {
        out.println(playerName + ": " + message);
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

    static void main() throws IOException {
        GameClient client = new GameClient("Player1", "localhost", 22222);
        // Hier weitere Aktionen durchführen, z.B. Nachrichten senden/empfangen

    }
}
