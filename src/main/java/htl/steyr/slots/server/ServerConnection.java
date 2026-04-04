package htl.steyr.slots.server;

import htl.steyr.slots.assets.Slotmachine;
import htl.steyr.slots.interfaces.Event;
import htl.steyr.slots.interfaces.Player;
import htl.steyr.slots.interfaces.PublisherInterface;
import htl.steyr.slots.interfaces.SubscriberInterface;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ServerConnection implements Player, PublisherInterface {
    Socket socket;
    private final Scanner in;
    private final PrintWriter out;

    private volatile boolean running;
    private Thread receiveThread;

    private String username;
    private int id;

    // Game state fields
    private final Slotmachine slotmachine;
    private boolean alive = true;
    private boolean usedRespin = false;
    private boolean submitted = false;
    private int claimedHearts = 0;
    private List<String> lastSpin = new ArrayList<>();

    private final List<SubscriberInterface> subscribers = new ArrayList<>();

    public ServerConnection(Socket newconnection) throws IOException {
        this.socket = newconnection;

        this.in = new Scanner(newconnection.getInputStream());
        this.out = new PrintWriter(newconnection.getOutputStream(), true);

        running = true;
        this.slotmachine = new Slotmachine();
    }

    @Override
    public void addSubscriber(SubscriberInterface subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void notifySubscribers(Object message) {
        for (SubscriberInterface subscriber : subscribers) {
            subscriber.notify(new Event(this, message));
        }
    }

    public void acceptnewConnections() {
        if (receiveThread != null && receiveThread.isAlive()) {
            return;
        }

        receiveThread = new Thread(() -> {
            try {
                while (running && !socket.isClosed() && in.hasNextLine()) {
                    String message = in.nextLine();
                    System.out.println("Received: " + message);

                    // Notify subscribers
                    notifySubscribers(message);
                }
            } finally {
                close();
            }
        });

        receiveThread.start();
    }

    public void sendMessage(Object inputs) {
        out.println(inputs);
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    // Game Logic Methods
    @Override
    public List<String> spin() {
        lastSpin = slotmachine.spin();
        return lastSpin;
    }

    @Override
    public List<String> deadSpin() {
        lastSpin = slotmachine.spin(1);
        return lastSpin;
    }

    @Override
    public int countHearts() {
        return slotmachine.countHearts(lastSpin);
    }

    @Override
    public void resetForRound() {
        submitted = false;
        claimedHearts = 0;
        lastSpin = new ArrayList<>();
    }

    @Override
    public void useRespin() {
        usedRespin = true;
    }

    @Override
    public void eliminate() {
        alive = false;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public boolean hasUsedRespin() {
        return usedRespin;
    }

    @Override
    public boolean hasSubmitted() {
        return submitted;
    }

    @Override
    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    @Override
    public int getClaimedHearts() {
        return claimedHearts;
    }

    @Override
    public void setClaimedHearts(int claimedHearts) {
        this.claimedHearts = claimedHearts;
    }

    @Override
    public List<String> getLastSpin() {
        return lastSpin;
    }
}
