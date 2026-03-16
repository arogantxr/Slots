package htl.steyr.slots.gameLogik.clientlogik;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class GameClient {
    private String playerName;
    private Socket socket;

    private PrintWriter out;

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
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listen.start();

        Scanner consoleScanner = new Scanner(System.in);

        Thread sendmsg = new Thread(() -> {
            while(true) {
                String message = consoleScanner.nextLine();

                if (message.startsWith("@")) {
                    String[] parts = message.split(" ", 2);

                    String recipient = parts[0].substring(1); // Entfernt das '@' Zeichen
                    String privateMessage = parts[1];

                    this.sendMessage("private;" + recipient + ";" + privateMessage);
                } else {
                    this.sendMessage("broadcast;" + message);
                }
            }
        });
        sendmsg.start();

    }

    public void sendMessage(String message) {
        out.println(message);
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
