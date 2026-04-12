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
    public Label labelInfoText;
    @FXML
    public Label leaderboardLabel;
    @FXML
    public ListView<String> leaderboardListView;
    @FXML
    public Button tutorialButton;

    private final Game game = new Game();
    public Button doubleButton;
    private GameServer gameServer;
    private GameClient gameClient;
    private String currentPlayerName;
    private MediaPlayer backgroundMusicPlayer;

    private boolean hasSpunThisTurn = false;
    private boolean hasCalledThisTurn = false;
    private boolean isMyTurn = false;
    private String liarTargetName = null;
    private int liarTargetClaims = 0;


    /**
     * Called automatically by the JavaFX runtime after the FXML fields have been
     * injected.  Sets up background music and the tutorial button handler.
     */
    @FXML
    public void initialize() {
        initBackgroundMusic();

        tutorialButton.setOnAction(e -> showInfo(
                "Regeln",
                "Ablauf:\n1) Spin\n2) Optional Double (Respin)\n3) Optional Liar\n4) Submit (Claim 0-4 Herzen)\n\n" +
                        "Rundenende: Eindeutiger Leader mit >5 Herzen darf letzten Platz eliminieren."
        ));
    }

    /**
     * Handles the Liar button.
     *
     * <p>The current player may call Liar on the most-recently submitted player
     * once per turn, but only after having spun.  Clients send an
     * {@code action-liar;} message to the host; the host processes the call
     * directly.  The outcome is broadcast to all players as a
     * {@code liar-result;} message.</p>
     *
     * @param actionEvent the button action event (unused)
     */
    public void onLiarClick(ActionEvent actionEvent) {
        if (!hasSpunThisTurn) {
            setInfo("Du musst zuerst spinnen.");
            return;
        }
        if (hasCalledThisTurn) {
            setInfo("Liar wurde in diesem Zug bereits verwendet.");
            return;
        }

        if (gameServer == null) {
            // client mode: use liar target info sent by server
            if (liarTargetName == null) {
                setInfo("Kein vorheriger abgegebener Spieler zum Callen.");
                return;
            }
            boolean confirm = confirm(
                    "Liar Call",
                    "Willst du " + liarTargetName + " callen?\nClaim: " + liarTargetClaims
            );
            if (!confirm) return;
            if (gameClient != null) gameClient.sendMessage("action-liar;" + currentPlayerName);
            hasCalledThisTurn = true;
            return;
        }

        if (game.isGameOver()) return;

        Player current = game.getCurrentPlayer();
        Player previous = game.getPreviousSubmittedAlivePlayer(current);

        if (previous == null) {
            setInfo("Kein vorheriger abgegebener Spieler zum Callen.");
            return;
        }

        boolean confirm = confirm(
                "Liar Call",
                "Willst du " + previous.getName() + " callen?\nClaim: " + previous.getClaimedHearts()
        );
        if (!confirm) return;

        Player deadSpinPlayer = game.callPlayer(current, previous);
        hasCalledThisTurn = true;

        broadcastLiarResult(previous, deadSpinPlayer);

        refreshLeaderboard();

        if (!current.isAlive()) {
            game.nextPlayer();
            prepareNextTurn();
        } else {
            updateControls();
        }
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
        refreshLeaderboard();

        if (game.allAlivePlayersSubmitted()) {
            finishRound();
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
        } else if (message.startsWith("action-liar;")) {
            Player previous = game.getPreviousSubmittedAlivePlayer(current);
            if (previous == null) return;
            Player deadSpinPlayer = game.callPlayer(current, previous);
            broadcastLiarResult(previous, deadSpinPlayer);
            refreshLeaderboard();
            if (!current.isAlive()) {
                game.nextPlayer();
                prepareNextTurn();
            } else {
                updateControls();
            }
        } else if (message.startsWith("action-submit;")) {
            String[] parts = message.split(";", 2);
            try {
                int hearts = Integer.parseInt(parts[1]);
                game.submitCurrentPlayer(hearts);
                refreshLeaderboard();
                if (game.allAlivePlayersSubmitted()) {
                    finishRound();
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
                    hasCalledThisTurn = false;
                    liarTargetName = null;
                    liarTargetClaims = 0;
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
        } else if (message.startsWith("liar-info;")) {
            // liar-info;<prevName>;<prevClaims>
            String[] parts = message.split(";", 3);
            if (parts.length == 3) {
                String targetName = parts[1];
                int targetClaims = Integer.parseInt(parts[2]);
                Platform.runLater(() -> {
                    liarTargetName = targetName;
                    liarTargetClaims = targetClaims;
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
                    hasCalledThisTurn = true;
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
        hasCalledThisTurn = false;
        clearCards();
        refreshLeaderboard();
        updateControls();

        if (!game.isGameOver()) {
            setInfo(game.getCurrentPlayer().getName() + " ist dran.");

            // Tell all clients who they can call Liar on
            if (gameServer != null) {
                Player prev = game.getPreviousSubmittedAlivePlayer(game.getCurrentPlayer());
                if (prev != null) {
                    gameServer.broadcastMessage("liar-info;" + prev.getName() + ";" + prev.getClaimedHearts());
                }
            }
        }
    }

    private void finishRound() {
        if (game.canLeaderEliminate()) {
            Player leader = game.getLeader();
            List<Player> lastPlayers = game.getLastPlacePlayers();

            if (leader != null) {
                if (lastPlayers.size() == 1) {
                    Player eliminated = lastPlayers.get(0);
                    game.eliminatePlayer(eliminated);
                    showInfo("Eliminierung", leader.getName() + " eliminiert " + eliminated.getName() + ".");
                } else if (!lastPlayers.isEmpty()) {
                    Player choice = choosePlayer(lastPlayers, leader);
                    if (choice != null) {
                        game.eliminatePlayer(choice);
                        showInfo("Eliminierung", leader.getName() + " eliminiert " + choice.getName() + ".");
                    }
                }
            }
        }

        refreshLeaderboard();

        if (game.isGameOver()) {
            Player winner = game.getWinner();
            setInfo("GEWINNER: " + (winner != null ? winner.getName() : "Niemand"));
            disableAllGameButtons();
            return;
        }

        startNewRound();
    }

    private Player choosePlayer(List<Player> candidates, Player leader) {
        List<String> names = new ArrayList<>();
        for (Player p : candidates) names.add(p.getName());

        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Eliminierung");
        dialog.setHeaderText(leader.getName() + " ist eindeutiger Leader (>5).");
        dialog.setContentText("Wen eliminieren?");

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
            String claim = String.valueOf(p.getClaimedHearts());
            lines.add(p.getName() + " | " + alive + " | claim: " + claim + marker);
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

        // For clients, only enable buttons if it's their turn
        spinButton.setDisable(!isMyTurn || hasSpunThisTurn);
        doubleButton.setDisable(!isMyTurn && !spinButton.isDisable());

    }

    private void disableAllGameButtons() {
        spinButton.setDisable(true);
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
