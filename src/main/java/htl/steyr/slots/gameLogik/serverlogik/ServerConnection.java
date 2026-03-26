package htl.steyr.slots.gameLogik.serverlogik;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ServerConnection {
    private final Socket socket;
    private final Scanner in;
    private final PrintWriter out;

    private volatile boolean running;
    private Thread receiveThread;

    private String username;

    public ServerConnection(Socket newconnection) throws IOException {
        this.socket = newconnection;

        this.in = new Scanner(newconnection.getInputStream());
        this.out = new PrintWriter(newconnection.getOutputStream(), true);

        running = true;
    }


    public void acceptnewConnections(){
        if (receiveThread != null && receiveThread.isAlive()) {
            return;
        }

        receiveThread = new Thread(() -> {
            try {
                while (running && !socket.isClosed() && in.hasNextLine()) {
                    String message = in.nextLine();
                    System.out.println("Received: " + message);
                    if(message.startsWith("set-username;")){
                        String[] parts = message.split(";", 2);
                        if(parts.length == 2){
                            this.username = parts[1];
                            System.out.println("Username set to: " + username);
                        }
                    }
                }
            } finally {
                close();
            }
        });

        receiveThread.start();
    }

    public void sendMessage(Object inputs){
        out.println(inputs);
    }

    public String getUsername() {
        return this.username;
    }

    public void close() {
        running = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
