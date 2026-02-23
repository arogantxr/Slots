package htl.steyr.slots.gameLogik;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Connection_Handling {
    private final Socket socket;
    private final Scanner in;
    private final PrintWriter out;

    private final boolean running;

    public Connection_Handling(Socket newconnection) throws IOException {
        this.socket = newconnection;

        this.in = new Scanner(newconnection.getInputStream());
        this.out = new PrintWriter(newconnection.getOutputStream(), true);

        running = true;
    }
}
