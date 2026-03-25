package htl.steyr.slots.assets;

import java.util.ArrayList;
import java.util.List;

/**
 * Verwaltet die Spiellogik für das Slots-Spiel.
 * Steuert Spieler, Runden, Liar-Calls und Eliminierungen.
 */
public class Game {

    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex;

    /**
     * Fügt einen Spieler zum Spiel hinzu.
     * @param player der hinzuzufügende Spieler
     */
    public void addPlayer(Player player) {
        players.add(player);
    }

    /**
     * Startet eine neue Runde und setzt alle lebenden Spieler zurück.
     */
    public void startRound() {
        for (Player player : players) {
            if (player.isAlive()) {
                player.resetForRound();
            }
        }

        currentPlayerIndex = findNextAlivePlayer(0);
    }

    /**
     * Führt einen Spin für den aktuellen Spieler aus.
     * @return Liste der gespinnten Symbole
     */
    public List<String> spinCurrentPlayer() {
        return getCurrentPlayer().spin();
    }

    /**
     * Führt einen Respin für den aktuellen Spieler aus (falls noch nicht verwendet).
     * @return Liste der gespinnten Symbole oder letzter Spin falls bereits verwendet
     */
    public List<String> respinCurrentPlayer() {
        Player player = getCurrentPlayer();

        if (!player.hasUsedRespin()) {
            player.useRespin();
            return player.spin();
        }

        return player.getLastSpin();
    }

    /**
     * Schließt den Zug des aktuellen Spielers ab und wechselt zum nächsten.
     * @param hearts Anzahl der geclaimten Herzen (0-4)
     */
    public void submitCurrentPlayer(int hearts) {
        Player player = getCurrentPlayer();
        player.setClaimedHearts(hearts);
        player.setSubmitted(true);
        nextPlayer();
    }

    /**
     * Ruft "Liar" gegen einen anderen Spieler. Der Verlierer muss Dead-Spin machen.
     * @param caller der aufrufende Spieler
     * @param target der beschuldigte Spieler
     * @return der Spieler, der den Dead-Spin machen musste
     */
    public Player callPlayer(Player caller, Player target) {
        int realHearts = target.countHearts();
        Player deadSpinPlayer;

        if (target.getClaimedHearts() > realHearts) {
            deadSpinPlayer = target;
        } else {
            deadSpinPlayer = caller;
        }

        deadSpin(deadSpinPlayer);
        return deadSpinPlayer;
    }

    /**
     * Führt einen Dead-Spin aus. Bei keinem Herz wird der Spieler eliminiert.
     * @param player der Spieler, der spinnen muss
     * @return true wenn überlebt (Herz gezogen), false wenn eliminiert
     */
    public boolean deadSpin(Player player) {
        List<String> spin = player.deadSpin();

        if (spin.contains("Hearts")) {
            return true;
        }

        player.eliminate();
        return false;
    }

    /**
     * Prüft ob alle lebenden Spieler ihren Zug abgegeben haben.
     * @return true wenn alle fertig sind
     */
    public boolean allAlivePlayersSubmitted() {
        for (Player player : players) {
            if (player.isAlive() && !player.hasSubmitted()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Findet den vorherigen lebenden Spieler, der bereits submitted hat.
     * @param player aktueller Spieler als Referenz
     * @return vorheriger Spieler oder null wenn keiner gefunden
     */
    public Player getPreviousSubmittedAlivePlayer(Player player) {
        int index = players.indexOf(player);

        index--;
        if (index < 0) {
            index = players.size() - 1;
        }

        while (players.get(index) != player) {
            Player previousPlayer = players.get(index);

            if (previousPlayer.isAlive() && previousPlayer.hasSubmitted()) {
                return previousPlayer;
            }

            index--;
            if (index < 0) {
                index = players.size() - 1;
            }
        }

        return null;
    }

    /**
     * Ermittelt den Spieler mit den meisten geclaimten Herzen.
     * @return der führende Spieler oder null wenn keine Spieler vorhanden
     */
    public Player getLeader() {
        Player leader = null;

        for (Player player : players) {
            if (!player.isAlive()) {
                continue;
            }

            if (leader == null || player.getClaimedHearts() > leader.getClaimedHearts()) {
                leader = player;
            }
        }

        return leader;
    }

    /**
     * Prüft ob es einen eindeutigen Leader gibt (kein Gleichstand).
     * @return true wenn genau ein Leader existiert
     */
    public boolean hasUniqueLeader() {
        Player leader = getLeader();

        if (leader == null) {
            return false;
        }

        int count = 0;

        for (Player player : players) {
            if (player.isAlive() && player.getClaimedHearts() == leader.getClaimedHearts()) {
                count++;
            }
        }

        return count == 1;
    }

    /**
     * Gibt alle Spieler auf dem letzten Platz zurück.
     * @return Liste der Letztplatzierten (kann mehrere bei Gleichstand enthalten)
     */
    public List<Player> getLastPlacePlayers() {
        List<Player> lastPlayers = new ArrayList<>();
        Player lastPlace = getLastPlace();

        if (lastPlace == null) {
            return lastPlayers;
        }

        for (Player player : players) {
            if (player.isAlive() && player.getClaimedHearts() == lastPlace.getClaimedHearts()) {
                lastPlayers.add(player);
            }
        }

        return lastPlayers;
    }

    /**
     * Prüft ob der Leader jemanden eliminieren darf (eindeutig und mehr als 5 Herzen).
     * @return true wenn Eliminierung möglich
     */
    public boolean canLeaderEliminate() {
        Player leader = getLeader();

        if (leader == null) {
            return false;
        }

        return hasUniqueLeader() && leader.getClaimedHearts() > 5;
    }

    /**
     * Eliminiert einen Spieler aus dem Spiel.
     * @param player der zu eliminierende Spieler
     */
    public void eliminatePlayer(Player player) {
        if (player != null) {
            player.eliminate();
        }
    }

    /**
     * Wechselt zum nächsten lebenden Spieler.
     */
    public void nextPlayer() {
        currentPlayerIndex = findNextAlivePlayer(currentPlayerIndex + 1);
    }

    /**
     * Prüft ob das Spiel vorbei ist (maximal 1 Spieler übrig).
     * @return true wenn Spiel beendet
     */
    public boolean isGameOver() {
        int alive = 0;

        for (Player player : players) {
            if (player.isAlive()) {
                alive++;
            }
        }

        return alive <= 1;
    }

    /**
     * Gibt den Gewinner zurück (letzter lebender Spieler).
     * @return der Gewinner oder null wenn keiner übrig
     */
    public Player getWinner() {
        for (Player player : players) {
            if (player.isAlive()) {
                return player;
            }
        }

        return null;
    }

    /**
     * Gibt den aktuellen Spieler zurück.
     * @return der Spieler, der gerade am Zug ist
     */
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    /**
     * Gibt die Liste aller Spieler zurück.
     * @return Liste aller Spieler
     */
    public List<Player> getPlayers() {
        return players;
    }

    /**
     * Ermittelt den Spieler mit den wenigsten Herzen (letzter Platz).
     * @return der letztplatzierte Spieler oder null
     */
    private Player getLastPlace() {
        Player lastPlace = null;

        for (Player player : players) {
            if (!player.isAlive()) {
                continue;
            }

            if (lastPlace == null || player.getClaimedHearts() < lastPlace.getClaimedHearts()) {
                lastPlace = player;
            }
        }

        return lastPlace;
    }

    /**
     * Findet den nächsten lebenden Spieler ab einem bestimmten Index.
     * @param startIndex Startindex für die Suche
     * @return Index des nächsten lebenden Spielers
     */
    private int findNextAlivePlayer(int startIndex) {
        if (players.isEmpty()) {
            return 0;
        }

        int index = startIndex;

        if (index >= players.size()) {
            index = 0;
        }

        while (!players.get(index).isAlive()) {
            index++;
            if (index >= players.size()) {
                index = 0;
            }
        }

        return index;
    }
}