package htl.steyr.slots.gameLogik;

import java.io.IOException;
import java.net.Socket;

public class Slots_Client {
    private String playerName;
    private String serverIP;
    private int serverPort;
    private Socket socket;

    public Slots_Client(String playerName, String serverIP, int serverPort) {
        this.playerName = playerName;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void connect() throws IOException {
        // Verbindung zum Server aufbauen
        socket = new Socket(serverIP, serverPort);
        System.out.println("Verbunden mit Server: " + serverIP + ":" + serverPort);
        System.out.println("Spielername: " + playerName);

        // Hier die weitere Verbindungslogik implementieren
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
}
