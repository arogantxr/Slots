package htl.steyr.slots.controller;


import htl.steyr.slots.assets.Game;
import htl.steyr.slots.interfaces.Player;
import htl.steyr.slots.player.GameClient;
import htl.steyr.slots.server.GameServer;
import htl.steyr.slots.server.ServerConnection;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * JavaFX controller for the game-table view ({@code Game-view.fxml}).
 *
 * <p>This controller runs on every participant's machine.  Its behaviour differs
 * depending on the role of the player:</p>
 * <ul>
 *   <li><strong>Host</strong> ({@link #gameServer} is set) — owns the authoritative
 *       {@link Game} instance, processes all player actions locally, and broadcasts
 *       state changes to all clients via {@link GameServer}.</li>
 *   <li><strong>Client</strong> ({@link #gameServer} is {@code null}) — has no local
 *       game state; instead it sends {@code action-*} messages to the host and reacts
 *       to {@code turn-update}, {@code game-state}, {@code spin-result},
 *       {@code liar-info}, and {@code liar-result} messages received from the
 *       server.</li>
 * </ul>
 */
public class GameTableController {

    @FXML
    public Button btnCard1;
    @FXML
    public Button btnCard2;
    @FXML
    public Button btnCard3;
    @FXML
    public Button btnCard4;
    @FXML
    public Button spinButton;
    @FXML
    public Button voteButton;
    @FXML
    public Label labelInfoText;
    @FXML
    public Label leaderboardLabel;
    @FXML
    public ListView<String> leaderboardListView;
    @FXML
    public Button tutorialButton;

    private final Game game = new Game();
    private GameServer gameServer;
    private GameClient gameClient;
    private String currentPlayerName;
    private MediaPlayer backgroundMusicPlayer;

    private boolean hasSpunThisTurn = false;
    private boolean isMyTurn = false;
    private int myTotalHearts = 0;


    /**
     * Called automatically by the JavaFX runtime after the FXML fields have been
     * injected.  Sets up background music and the tutorial button handler.
     */
    @FXML
    public void initialize() {
        initBackgroundMusic();

        tutorialButton.setOnAction(e -> showInfo(
                "Regeln",
                "Ablauf:\n1) Spin\n2) Optional Double (Respin)\n3) Submit (Claim 0-4 Herzen)\n4) Alle stimmen ab ob gelogen wurde\n\n" +
                        "Herzen sammeln sich über Runden an.\n" +
                        "Rundenende: Spieler mit >= 5 Herzen können 5 Herzen ausgeben um abzustimmen, wer zum Deathspin muss."
        ));
    }

    /**
     * Handles the Vote button. Spends 5 hearts to send another player to a deathspin.
     * Only available during the player's turn after spinning, if they have >= 5 total hearts.
     */
    public void onVoteClick(ActionEvent actionEvent) {
        if (!isMyTurn || !hasSpunThisTurn) return;

        if (gameServer == null) {
            // client mode
            if (gameClient != null) gameClient.sendMessage("action-vote;" + currentPlayerName);
            return;
        }

        if (game.isGameOver()) return;

        Player current = game.getCurrentPlayer();
        if (current.getTotalHearts() < 5) {
            setInfo("Nicht genug Herzen (mind. 5 benötigt).");
            return;
        }

        List<Player> targets = new ArrayList<>();
        for (Player t : game.getPlayers()) {
            if (t.isAlive() && t != current) targets.add(t);
        }
        if (targets.isEmpty()) return;

        Player target = chooseDeathspinTarget(targets, current);
        if (target == null) return;

        current.addTotalHearts(-5);
        boolean survived = game.deadSpin(target);
        showInfo("Deathspin",
                current.getName() + " schickt " + target.getName() + " zum Deathspin. (-5 Herzen)\n" +
                "Ergebnis: " + String.join(", ", target.getLastSpin()) + "\n" +
                (survived ? target.getName() + " hat überlebt!" : target.getName() + " ist raus!"));

        refreshLeaderboard();

        if (game.isGameOver()) {
            Player winner = game.getWinner();
            setInfo("GEWINNER: " + (winner != null ? winner.getName() : "Niemand"));
            disableAllGameButtons();
            return;
        }

        updateControls();
    }

    /**
     * Handles the Double (respin) button.
     *
     * <p>May only be used after the initial spin and only once per turn.
     * Clients send an {@code action-respin;} message to the host.</p>
     *
     * @param actionEvent the button action event (unused)
     */
    public void onDoubleClick(ActionEvent actionEvent) {
        if (!hasSpunThisTurn) {
            setInfo("Zuerst Spin, dann Double.");
            return;
        }

        if (gameServer == null) {
            // client mode: send respin request to server
            if (gameClient != null) gameClient.sendMessage("action-respin;" + currentPlayerName);
            return;
        }

        if (game.isGameOver()) return;

        Player current = game.getCurrentPlayer();
        if (current.hasUsedRespin()) {
            setInfo("Respin bereits benutzt.");
            return;
        }

        List<String> spin = game.respinCurrentPlayer();
        renderSpin(spin);
        setInfo(current.getName() + " hat Double/Respin verwendet.");
        refreshLeaderboard();
        updateControls();
    }

    /**
     * Handles the Submit button.
     *
     * <p>The player must have spun before submitting.  A dialog asks how many
     * hearts to claim (0–4).  Clients send an {@code action-submit;<hearts>}
     * message to the host; the host updates the game state directly.</p>
     *
     * @param actionEvent the button action event (unused)
     */
    public void onSubmitClick(ActionEvent actionEvent) {
        if (!hasSpunThisTurn) {
            setInfo("Du musst zuerst spinnen.");
            return;
        }

        if (gameServer == null) {
            // client mode: ask locally, send to server
            Integer hearts = askInt("Submit", "Wie viele Herzen claimst du? (0-4)", 0, 4);
            if (hearts == null) return;
            if (gameClient != null) gameClient.sendMessage("action-submit;" + hearts);
            return;
        }

        if (game.isGameOver()) return;

        Player current = game.getCurrentPlayer();
        if (!current.isAlive()) {
            game.nextPlayer();
            prepareNextTurn();
            return;
        }

        Integer hearts = askInt("Submit", "Wie viele Herzen claimst du? (0-4)", 0, 4);
        if (hearts == null) return;

        game.submitCurrentPlayer(hearts);
        conductLiarVote(current, hearts);
        refreshLeaderboard();

        if (game.isGameOver()) {
            Player winner = game.getWinner();
            setInfo("GEWINNER: " + (winner != null ? winner.getName() : "Niemand"));
            disableAllGameButtons();
            return;
        }

        game.nextPlayer();
        if (game.allAlivePlayersSubmitted()) {
            startNewRound();
        } else {
            prepareNextTurn();
        }
    }

    /**
     * Handles the Spin button.
     *
     * <p>Only allowed once per turn and only when it is this player's turn.
     * Clients send an {@code action-spin;} message to the host, then wait for a
     * {@code spin-result;} response.  The host executes the spin locally.</p>
     *
     * @param actionEvent the button action event (unused)
     */
    public void onSpinClick(ActionEvent actionEvent) {
        if (!isMyTurn) {
            setInfo("Du bist nicht dran.");
            return;
        }

        if (hasSpunThisTurn) {
            setInfo("Du hast bereits gespinnt.");
            return;
        }

        if (gameServer == null) {
            // client mode: send spin request to server
            if (gameClient != null) gameClient.sendMessage("action-spin;" + currentPlayerName);
            return;
        }

        if (game.isGameOver()) return;

        Player current = game.getCurrentPlayer();
        if (!current.isAlive()) {
            game.nextPlayer();
            prepareNextTurn();
            return;
        }

        List<String> spin = game.spinCurrentPlayer();
        hasSpunThisTurn = true;

        renderSpin(spin);
        setInfo(current.getName() + " ist dran. Spin: " + spin);
        refreshLeaderboard();
        updateControls();
    }

    /**
     * Conducts a liar vote after a player submits. Every other alive player
     * votes on whether the submitter lied. If the submitter lied, they do a
     * deathspin. If they didn't, each accuser does a deathspin.
     */
    private void conductLiarVote(Player submitter, int claimedHearts) {
        List<Player> accusers = new ArrayList<>();

        for (Player voter : game.getPlayers()) {
            if (!voter.isAlive() || voter == submitter) continue;

            boolean votesLiar = confirm("Liar Abstimmung",
                    voter.getName() + ": Hat " + submitter.getName() + " gelogen? (Claim: " + claimedHearts + " Herzen)");
            if (votesLiar) {
                accusers.add(voter);
            }
        }

        if (accusers.isEmpty()) return;

        int actualHearts = submitter.countHearts();
        boolean didLie = claimedHearts > actualHearts;

        if (didLie) {
            boolean survived = game.deadSpin(submitter);
            showInfo("Liar Ergebnis",
                    submitter.getName() + " hat gelogen! (Claim: " + claimedHearts + ", Tatsächlich: " + actualHearts + ")\n" +
                    "Deathspin: " + String.join(", ", submitter.getLastSpin()) + "\n" +
                    (survived ? "Überlebt!" : submitter.getName() + " ist raus!"));
        } else {
            StringBuilder result = new StringBuilder();
            result.append(submitter.getName() + " hat NICHT gelogen! (Claim: " + claimedHearts + ", Tatsächlich: " + actualHearts + ")\n\n");

            for (Player accuser : accusers) {
                if (!accuser.isAlive() || game.isGameOver()) continue;
                boolean survived = game.deadSpin(accuser);
                result.append(accuser.getName() + " Deathspin: " + String.join(", ", accuser.getLastSpin()) + " - ");
                result.append(survived ? "Überlebt!\n" : "Ist raus!\n");
            }

            showInfo("Liar Ergebnis", result.toString());
        }
    }

    /**
     * Lädt die Spieler aus der ServerConnection-Liste vom GameServer
     */
    /**
     * Loads all connected {@link ServerConnection} players from the
     * {@link GameServer} into the local {@link Game} instance.
     * Falls back gracefully if the server is unavailable or has no clients yet.
     */
    private void setupPlayersFromGameServer() {
        // Versuche den GameServer zu finden (muss als Singleton oder über Kontext verfügbar sein)
        try {
            // Wenn GameServer bereits läuft, hole die Clients
            if (gameServer != null) {
                List<ServerConnection> clients = gameServer.getClientList();
                System.out.println("Clients im game: " + clients);

                if (!clients.isEmpty()) {
                    for (ServerConnection client : clients) {
                        game.addPlayer(client);
                    }
                    System.out.println("Loaded " + clients.size() + " players from GameServer");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading players from GameServer: " + e.getMessage());
        }

        // Fallback: Erstelle Test-Spieler (wenn kein GameServer verfügbar ist)
        System.out.println("No GameServer found, using test players");
        // Hinweis: Hier müssten Test-Player erstellt werden, falls nötig
    }

    /**
     * Setzt den GameServer für diesen Controller
     */
    /**
     * Injects the {@link GameServer} and configures the host-side game.
     *
     * <p>Populates the local {@link Game} with connected players, registers
     * turn-update and game-state broadcast callbacks, installs the client-action
     * handler, starts the first round, and broadcasts the initial turn to all
     * clients.</p>
     *
     * @param server the running {@link GameServer}; must not be {@code null}
     */
    public void setGameServer(GameServer server) {
        this.gameServer = server;
        setupPlayersFromGameServer();

        game.setTurnUpdateCallback(currentPlayerName -> {
            if (gameServer != null) {
                gameServer.broadcastTurnUpdate(currentPlayerName);
            }
        });

        game.setGameStateCallback(playerStates -> {
            if (gameServer != null) {
                gameServer.broadcastGameState(playerStates);
            }
        });

        gameServer.setActionHandler((client, message) ->
                Platform.runLater(() -> handleClientAction(client, message)));

        startNewRound();

        // Broadcast who goes first so all clients set isMyTurn correctly
        gameServer.broadcastTurnUpdate(game.getCurrentPlayer().getName());
    }

    /**
     * Processes an {@code action-*} message received from a non-host client.
     *
     * <p>Only executes the action if the sending client is actually the current
     * player.  Handles {@code action-spin}, {@code action-respin},
     * {@code action-liar}, and {@code action-submit}.</p>
     *
     * <p>Must be called on the JavaFX application thread.</p>
     *
     * @param client  the {@link ServerConnection} that sent the message
     * @param message the raw protocol string (e.g. {@code "action-spin;Alice"})
     */
    private void handleClientAction(ServerConnection client, String message) {
        Player current = game.getCurrentPlayer();
        if (!current.getName().equals(client.getUsername())) return;

        if (message.startsWith("action-spin;")) {
            List<String> spin = game.spinCurrentPlayer();
            client.sendMessage("spin-result;" + String.join(",", spin));
            refreshLeaderboard();
        } else if (message.startsWith("action-respin;")) {
            List<String> spin = game.respinCurrentPlayer();
            client.sendMessage("spin-result;" + String.join(",", spin));
            refreshLeaderboard();
        } else if (message.startsWith("action-vote;")) {
            if (current.getTotalHearts() < 5) return;
            List<Player> targets = new ArrayList<>();
            for (Player t : game.getPlayers()) {
                if (t.isAlive() && t != current) targets.add(t);
            }
            if (targets.isEmpty()) return;
            Player target = chooseDeathspinTarget(targets, current);
            if (target == null) return;
            current.addTotalHearts(-5);
            boolean survived = game.deadSpin(target);
            String voteResult = "vote-result;" + current.getName() + ";" + target.getName() + ";" +
                    String.join(",", target.getLastSpin()) + ";" + survived + ";" + current.getTotalHearts();
            gameServer.broadcastMessage(voteResult);
            refreshLeaderboard();
            if (game.isGameOver()) {
                Player winner = game.getWinner();
                setInfo("GEWINNER: " + (winner != null ? winner.getName() : "Niemand"));
                disableAllGameButtons();
            } else {
                // Send updated hearts to the voter client
                client.sendMessage("your-hearts;" + current.getTotalHearts());
            }
        } else if (message.startsWith("action-submit;")) {
            String[] parts = message.split(";", 2);
            try {
                int hearts = Integer.parseInt(parts[1]);
                game.submitCurrentPlayer(hearts);
                conductLiarVote(current, hearts);
                refreshLeaderboard();

                if (game.isGameOver()) {
                    Player winner = game.getWinner();
                    setInfo("GEWINNER: " + (winner != null ? winner.getName() : "Niemand"));
                    disableAllGameButtons();
                    return;
                }

                game.nextPlayer();
                if (game.allAlivePlayersSubmitted()) {
                    startNewRound();
                } else {
                    prepareNextTurn();
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /**
     * Broadcasts the outcome of a Liar call to all connected clients.
     *
     * <p>Wire format:
     * {@code liar-result;<prevName>;<prevSpinCSV>;<realHearts>;<deadSpinName>;<deadSpinCSV>;<survived>}</p>
     *
     * @param previous       the accused player whose last spin is revealed
     * @param deadSpinPlayer the player who had to perform the dead spin
     */
    private void broadcastLiarResult(Player previous, Player deadSpinPlayer) {
        if (gameServer == null) return;
        String result = "liar-result;" +
                previous.getName() + ";" +
                String.join(",", previous.getLastSpin()) + ";" +
                previous.countHearts() + ";" +
                deadSpinPlayer.getName() + ";" +
                String.join(",", deadSpinPlayer.getLastSpin()) + ";" +
                deadSpinPlayer.isAlive();
        gameServer.broadcastMessage(result);
    }

    /**
     * Injects the {@link GameClient} and registers this controller as its
     * message handler so that server messages are dispatched to
     * {@link #handleServerMessage(String)}.
     *
     * @param client the {@link GameClient} for this player; must not be {@code null}
     */
    public void setGameClient(GameClient client) {
        this.gameClient = client;
        // Set this controller as the message handler for the client
        if (this.gameClient != null) {
            this.gameClient.setMessageHandler(this::handleServerMessage);
        }
    }

    /**
     * Dispatches an incoming server message to the appropriate UI update.
     *
     * <p>Handled message prefixes and their effects:</p>
     * <ul>
     *   <li>{@code turn-update;<name>} — sets {@link #isMyTurn}, resets per-turn
     *       flags, updates controls.</li>
     *   <li>{@code game-state;<states>} — refreshes the leaderboard list.</li>
     *   <li>{@code spin-result;<symbols>} — renders the spin result and enables
     *       submit.</li>
     *   <li>{@code liar-info;<name>;<claims>} — stores the callable target for the
     *       Liar dialog.</li>
     *   <li>{@code liar-result;<...>} — shows the Liar outcome dialog.</li>
     * </ul>
     *
     * <p>This method may be called from any thread; all UI updates are wrapped in
     * {@link javafx.application.Platform#runLater}.</p>
     *
     * @param message the raw protocol string received from the server
     */
    public void handleServerMessage(String message) {
        if (message.startsWith("turn-update;")) {
            String[] parts = message.split(";", 2);
            if (parts.length == 2) {
                String currentPlayer = parts[1];
                Platform.runLater(() -> {
                    isMyTurn = currentPlayer.equals(currentPlayerName);
                    hasSpunThisTurn = false;
                    myTotalHearts = 0;
                    updateControls();
                    setInfo(currentPlayer + " ist dran.");
                });
            }
        } else if (message.startsWith("game-state;")) {
            String[] parts = message.split(";", 2);
            if (parts.length == 2) {
                String[] playerStates = parts[1].split("\\|");
                Platform.runLater(() -> {
                    updateLeaderboardFromServer(playerStates);
                });
            }
        } else if (message.startsWith("spin-result;")) {
            String[] parts = message.split(";", 2);
            if (parts.length == 2) {
                List<String> spin = Arrays.asList(parts[1].split(","));
                Platform.runLater(() -> {
                    hasSpunThisTurn = true;
                    renderSpin(spin);
                    setInfo("Spin: " + spin);
                    updateControls();
                });
            }
        } else if (message.startsWith("your-hearts;")) {
            String[] parts = message.split(";", 2);
            if (parts.length == 2) {
                int hearts = Integer.parseInt(parts[1]);
                Platform.runLater(() -> {
                    myTotalHearts = hearts;
                    updateControls();
                });
            }
        } else if (message.startsWith("vote-result;")) {
            // vote-result;<voterName>;<targetName>;<spinCSV>;<survived>;<voterHearts>
            String[] parts = message.split(";", 6);
            if (parts.length == 6) {
                String voterName = parts[1];
                String targetName = parts[2];
                String spinResult = parts[3];
                boolean survived = Boolean.parseBoolean(parts[4]);
                Platform.runLater(() -> {
                    showInfo("Deathspin",
                            voterName + " schickt " + targetName + " zum Deathspin. (-5 Herzen)\n" +
                            "Ergebnis: [" + spinResult + "]\n" +
                            (survived ? targetName + " hat überlebt!" : targetName + " ist raus!"));
                });
            }
        } else if (message.startsWith("liar-result;")) {
            // liar-result;<prevName>;<prevSpinCSV>;<realHearts>;<deadSpinName>;<deadSpinCSV>;<survived>
            String[] parts = message.split(";", 7);
            if (parts.length == 7) {
                String prevName = parts[1];
                String prevSpin = parts[2];
                int realHearts = Integer.parseInt(parts[3]);
                String deadName = parts[4];
                String deadSpin = parts[5];
                boolean survived = Boolean.parseBoolean(parts[6]);
                Platform.runLater(() -> {
                    showInfo(
                            "Liar Ergebnis",
                            prevName + " hatte: [" + prevSpin + "]\n" +
                                    "Echte Herzen: " + realHearts + "\n\n" +
                                    deadName + " musste Deadspin machen: [" + deadSpin + "]\n" +
                                    (survived ? "Überlebt." : "Ist raus.")
                    );
                });
            }
        }
    }

    private void updateLeaderboardFromServer(String[] playerStates) {
        List<String> lines = new ArrayList<>();
        for (String state : playerStates) {
            lines.add(state);
        }
        leaderboardListView.getItems().setAll(lines);
    }

    public void setCurrentPlayerName(String name) {
        this.currentPlayerName = name;
    }

    public String getCurrentPlayerName() {
        // Return current player name from game if set, otherwise return player's own name
        if (game.getPlayers().size() > 0) {
            return game.getCurrentPlayer().getName();
        }
        return currentPlayerName;
    }

    private void startNewRound() {
        game.startRound();
        prepareNextTurn();
        setInfo("=== NEUE RUNDE === " + game.getCurrentPlayer().getName() + " startet.");
    }

    private void prepareNextTurn() {
        hasSpunThisTurn = false;
        clearCards();
        refreshLeaderboard();
        updateControls();

        if (!game.isGameOver()) {
            Player current = game.getCurrentPlayer();
            setInfo(current.getName() + " ist dran.");
            // Tell the current client their total hearts so they can enable the Vote button
            if (gameServer != null) {
                current.sendMessage("your-hearts;" + current.getTotalHearts());
            }
        }
    }

    private Player chooseDeathspinTarget(List<Player> candidates, Player voter) {
        List<String> names = new ArrayList<>();
        for (Player p : candidates) names.add(p.getName());

        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Deathspin Abstimmung");
        dialog.setHeaderText(voter.getName() + " stimmt ab (kostet 5 Herzen).");
        dialog.setContentText("Wen zum Deathspin schicken?");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) return null;

        String chosen = result.get();
        for (Player p : candidates) {
            if (p.getName().equals(chosen)) return p;
        }
        return null;
    }

    private void renderSpin(List<String> spin) {
        Button[] cards = {btnCard1, btnCard2, btnCard3, btnCard4};
        for (int i = 0; i < cards.length; i++) {
            cards[i].setText(i < spin.size() ? toSymbol(spin.get(i)) : "?");
        }
    }

    private void clearCards() {
        btnCard1.setText("?");
        btnCard2.setText("?");
        btnCard3.setText("?");
        btnCard4.setText("?");
    }

    private String toSymbol(String value) {
        if ("Hearts".equals(value)) {
            return "❤";
        } else if ("Diamonds".equals(value)) {
            return "♢";
        } else if ("Clubs".equals(value)) {
            return "♣";
        } else if ("Spades".equals(value)) {
            return "♠";
        } else {
            return "?";
        }
    }

    private void refreshLeaderboard() {
        List<String> lines = new ArrayList<>();
        Player current = game.isGameOver() ? null : game.getCurrentPlayer();

        for (Player p : game.getPlayers()) {
            String marker = (current == p) ? " ◀" : "";
            String alive = p.isAlive() ? "alive" : "out";
            String claim = p.hasSubmitted() ? String.valueOf(p.getClaimedHearts()) : "-";
            lines.add(p.getName() + " | " + alive + " | claim: " + claim + " | total: " + p.getTotalHearts() + marker);
        }

        leaderboardListView.getItems().setAll(lines);

        // Broadcast game state update
        if (gameServer != null) {
            gameServer.broadcastGameState(lines);
        }
    }

    private void updateControls() {
        if (gameServer != null && game.isGameOver()) {
            disableAllGameButtons();
            return;
        }

        spinButton.setDisable(!isMyTurn || hasSpunThisTurn);

        // Vote enabled when it's your turn, you've spun, and you have >= 5 hearts
        boolean canVote = isMyTurn && hasSpunThisTurn;
        if (canVote) {
            if (gameServer != null && !game.getPlayers().isEmpty()) {
                canVote = game.getCurrentPlayer().getTotalHearts() >= 5;
            } else {
                canVote = myTotalHearts >= 5;
            }
        }
        voteButton.setDisable(!canVote);

        btnCard1.setDisable(true);
        btnCard2.setDisable(true);
        btnCard3.setDisable(true);
        btnCard4.setDisable(true);
    }

    private void disableAllGameButtons() {
        spinButton.setDisable(true);
        voteButton.setDisable(true);
    }

    private Integer askInt(String title, String text, int min, int max) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(text);

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) return null;

        try {
            int value = Integer.parseInt(result.get().trim());
            if (value < min || value > max) {
                showInfo("Ungültige Eingabe", "Bitte Zahl zwischen " + min + " und " + max + " eingeben.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            showInfo("Ungültige Eingabe", "Bitte eine gültige Zahl eingeben.");
            return null;
        }
    }

    private boolean confirm(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private void showInfo(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void setInfo(String text) {
        labelInfoText.setText(text);
    }

    private void initBackgroundMusic() {
        try {
            URL musicUrl = getClass().getResource("/htl/steyr/slots/Sounds/backgroundMusic.mp3");
            if (musicUrl == null) musicUrl = getClass().getResource("/Sounds/backgroundMusic.mp3");
            if (musicUrl == null) return;

            Media media = new Media(musicUrl.toExternalForm());
            backgroundMusicPlayer = new MediaPlayer(media);
            backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundMusicPlayer.setVolume(0.35);
            backgroundMusicPlayer.play();

            // is stopping smoothly when window gets closed
            labelInfoText.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.windowProperty().addListener((wObs, oldWindow, newWindow) -> {
                        if (newWindow != null) {
                            newWindow.setOnHidden(e -> stopBackgroundMusic());
                        }
                    });
                }
            });
        } catch (Exception ignored) {
            // Music errors wont crash the game, just get skipped
        }
    }

    private void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
            backgroundMusicPlayer.dispose();
            backgroundMusicPlayer = null;
        }
    }
}
