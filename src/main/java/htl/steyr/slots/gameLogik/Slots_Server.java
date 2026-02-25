package htl.steyr.slots.gameLogik;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Slots_Server {
    private final ServerSocket server;
    private boolean running;
    private final List<Connection_Handling> clients = Collections.synchronizedList(new ArrayList<>());
    private Thread acceptnewConnections;

    public Slots_Server() throws IOException {
        server = new ServerSocket(12345);
        running = true;
    }

    public Slots_Server(int port) throws IOException {
        server = new ServerSocket(port);
        running = true;
    }

    public void acceptConnections() {

        acceptnewConnections = new Thread(() -> {
            while (running) {
                try {
                    Connection_Handling cl = new Connection_Handling(server.accept());
                    clients.add(cl);
                    cl.acceptnewConnections();
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        }, "slots-server-accept");

        acceptnewConnections.start();
    }


    public void stop() throws IOException {
        running = false;
        if (acceptnewConnections != null) acceptnewConnections.interrupt();
        synchronized (clients) {
            for (Connection_Handling client : clients) {
                client.close();
            }
        }
        server.close();
    }

    public static void main(String[] args) {

        System.out.println("===SLOTS-SERVER===");
        int portposition = 55555;

        try {
            Slots_Server newserver = new Slots_Server(portposition);
            newserver.acceptConnections();

            System.out.println("Server is running on Port: " + portposition);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
