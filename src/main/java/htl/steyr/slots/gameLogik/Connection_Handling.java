package htl.steyr.slots.gameLogik;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Connection_Handling {
    private final Socket socket;
    private final Scanner in;
    private final PrintWriter out;

    private volatile boolean running;
    private Thread receiveThread;

    public Connection_Handling(Socket newconnection) throws IOException {
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
                }
            } finally {
                close();
            }
        }, "slots-connection-recv");

        receiveThread.start();
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
