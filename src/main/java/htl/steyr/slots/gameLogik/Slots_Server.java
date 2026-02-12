package htl.steyr.slots.gameLogik;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Slots_Server {
    private final ServerSocket server;
    private final boolean running;
    private final List<Connection_Handling> clients = Collections.synchronizedList(new ArrayList<>());


    public Slots_Server() throws IOException {
        server = new ServerSocket(12345);
        running = true;
    }

    public Slots_Server(int port) throws IOException {
        server = new ServerSocket(port);
        running = true;
    }

    public void acceptConnections(){

        Thread acceptnewConnections = new Thread(() -> {
            while (running) {
                try (Socket newconnection = server.accept()) {

                    Connection_Handling cl = new Connection_Handling(newconnection);

                    clients.add(cl);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        acceptnewConnections.start();
    }



    public List getSlotSymbols(){
        List symbols = new ArrayList();

        for(int i=0;i<4;++i){
            int rand = ThreadLocalRandom.current().nextInt(4);
            symbols.add(rand+1);
        }
        return symbols;
    }

    static void main() {

        System.out.println("===SLOTS-SERVER===");
        int portposition=55555;

        try {
            Slots_Server newserver = new Slots_Server(portposition);
            newserver.acceptConnections();
            System.out.println("Player1: "+ newserver.getSlotSymbols());
            System.out.println("Player2: "+ newserver.getSlotSymbols());
            System.out.println("Player3: "+ newserver.getSlotSymbols());
            System.out.println("Player4: "+ newserver.getSlotSymbols());
            System.out.println("Server is running on Port: "+portposition);
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
