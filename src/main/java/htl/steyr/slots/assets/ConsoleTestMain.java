package htl.steyr.slots.assets;

import java.util.List;
import java.util.Scanner;

public class ConsoleTestMain {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Game game = new Game();
        Slotmachine slotmachine = new Slotmachine();

        game.addPlayer(new Player("Player 1", slotmachine));
        game.addPlayer(new Player("Player 2", slotmachine));
        game.addPlayer(new Player("Player 3", slotmachine));
        game.addPlayer(new Player("Player 4", slotmachine));

        while (!game.isGameOver()) {
            game.startRound();

            System.out.println("\n=== NEW ROUND ===");

            while (!game.allAlivePlayersSubmitted()) {
                Player currentPlayer = game.getCurrentPlayer();

                System.out.println("\n" + currentPlayer.getName() + " is dran.");

                List<String> spin = game.spinCurrentPlayer();
                System.out.println("Spin: " + spin);

                if (!currentPlayer.hasUsedRespin()) {
                    System.out.println("Respin? 1 = Ja, 2 = Nein");
                    int respinChoice = scanner.nextInt();

                    if (respinChoice == 1) {
                        spin = game.respinCurrentPlayer();
                        System.out.println("Neuer Spin: " + spin);
                    }
                }

                Player previousPlayer = game.getPreviousSubmittedAlivePlayer(currentPlayer);

                if (previousPlayer != null) {
                    System.out.println("Willst du " + previousPlayer.getName() + " callen?");
                    System.out.println("1 = Ja, 2 = Nein");
                    int callChoice = scanner.nextInt();

                    if (callChoice == 1) {
                        System.out.println(previousPlayer.getName() + " hatte: " + previousPlayer.getLastSpin());
                        System.out.println("Echte Herzen: " + previousPlayer.countHearts());

                        Player deadSpinPlayer = game.callPlayer(currentPlayer, previousPlayer);

                        System.out.println(deadSpinPlayer.getName() + " muss Deadspin machen.");
                        System.out.println("Deadspin: " + deadSpinPlayer.getLastSpin());

                        if (deadSpinPlayer.isAlive()) {
                            System.out.println(deadSpinPlayer.getName() + " ueberlebt.");
                        } else {
                            System.out.println(deadSpinPlayer.getName() + " ist raus.");
                        }
                    }
                }

                if (currentPlayer.isAlive()) {
                    System.out.print("Wie viele Herzen claimst du? ");
                    int hearts = scanner.nextInt();
                    game.submitCurrentPlayer(hearts);
                } else {
                    game.nextPlayer();
                }
            }

            System.out.println("\n=== CLAIMS ===");
            for (Player player : game.getPlayers()) {
                if (player.isAlive()) {
                    System.out.println(player.getName() + ": " + player.getClaimedHearts());
                }
            }

            if (game.canLeaderEliminate()) {
                Player leader = game.getLeader();
                List<Player> lastPlayers = game.getLastPlacePlayers();

                System.out.println("\n" + leader.getName() + " ist eindeutiger Erster mit mehr als 5 Herzen.");

                if (lastPlayers.size() == 1) {
                    Player eliminated = lastPlayers.get(0);
                    game.eliminatePlayer(eliminated);
                    System.out.println(eliminated.getName() + " wird eliminiert.");
                } else {
                    System.out.println("Geteilter letzter Platz. Wen willst du eliminieren?");

                    for (int i = 0; i < lastPlayers.size(); i++) {
                        System.out.println(i + " = " + lastPlayers.get(i).getName());
                    }

                    int choice = scanner.nextInt();
                    Player eliminated = lastPlayers.get(choice);
                    game.eliminatePlayer(eliminated);
                    System.out.println(eliminated.getName() + " wird eliminiert.");
                }
            }

            System.out.println("\n=== STATUS ===");
            for (Player player : game.getPlayers()) {
                System.out.println(player.getName() + " | alive: " + player.isAlive());
            }
        }

        Player winner = game.getWinner();
        if (winner != null) {
            System.out.println("\nGEWINNER: " + winner.getName());
        }
    }
}