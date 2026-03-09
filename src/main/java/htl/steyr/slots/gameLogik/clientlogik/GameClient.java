package htl.steyr.slots.gameLogik.clientlogik;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;


public class GameClient {

    //for DEMO

    static void main() throws IOException {
        try (Socket connection = new Socket("localhost", 55555);
            Scanner commandLineScanner = new Scanner(System.in);
            PrintWriter out = new PrintWriter(connection.getOutputStream(), true)){



            Thread readingthread = new Thread(()-> {


                try(Scanner s = new Scanner(connection.getInputStream());) {

                    while(s.hasNextLine()){
                        String message = s.nextLine();
                        System.out.println("Received: " + message);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            });
            readingthread.start();

            while(true){
                out.println(commandLineScanner.nextLine());
            }

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
