package com.backgammon3d;

import com.backgammon3d.ai.HumanPlayer;
import com.backgammon3d.ai.Player;
import com.backgammon3d.ai.RandomPlayer;
import com.backgammon3d.ai.TDPlayer;
import com.backgammon3d.model.Dice;
import com.backgammon3d.model.GameState;
import com.backgammon3d.model.Move;
import com.backgammon3d.neural.TDNetwork;
import com.backgammon3d.neural.TDTrainer;
import com.backgammon3d.view.BoardView;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

/**
 * Main application class for Backgammon3D.
 * Orchestrates the game flow and UI components.
 */
public class Main extends Application {

    private GameState gameState;
    private BoardView boardView;
    private Label statusLabel;
    private Label diceLabel;
    private Button rollButton;
    private Button newGameButton;
    private ComboBox<String> whitePlayerCombo;
    private ComboBox<String> blackPlayerCombo;

    private int[] currentDice;
    private List<Integer> availableMoves; // Remaining moves from dice
    private boolean isWhiteTurn = true;
    private int selectedFromPoint = -1;

    // AI components
    private TDNetwork tdNetwork;
    private Player whitePlayer;
    private Player blackPlayer;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Backgammon 3D - TD-Gammon AI");

        // Initialize neural network and try to load saved model
        tdNetwork = new TDNetwork();
        tryLoadSavedModel();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2c3e50;");

        // Top toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);

        // Center: 3D Board
        gameState = new GameState();
        boardView = new BoardView(800, 500, gameState);
        boardView.setOnCheckerSelected(this::onCheckerSelected);
        boardView.setOnMoveExecuted(this::onMoveExecuted);
        root.setCenter(boardView);

        // Right: Controls
        VBox controls = createControlPanel();
        root.setRight(controls);

        // Bottom: Status
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1100, 700);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        updateStatus();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #34495e;");

        newGameButton = new Button("Neues Spiel");
        newGameButton.setOnAction(e -> newGame());

        Button trainButton = new Button("KI trainieren");
        trainButton.setOnAction(e -> showTrainingDialog());

        Button loadModelButton = new Button("Modell laden");
        loadModelButton.setOnAction(e -> loadModel());

        Label whiteLabel = new Label("Weiß:");
        whiteLabel.setStyle("-fx-text-fill: white;");
        whitePlayerCombo = new ComboBox<>();
        whitePlayerCombo.getItems().addAll("Mensch", "KI (Random)", "KI (TD-Gammon)");
        whitePlayerCombo.setValue("Mensch");

        Label blackLabel = new Label("Schwarz:");
        blackLabel.setStyle("-fx-text-fill: white;");
        blackPlayerCombo = new ComboBox<>();
        blackPlayerCombo.getItems().addAll("Mensch", "KI (Random)", "KI (TD-Gammon)");
        blackPlayerCombo.setValue("KI (TD-Gammon)");

        toolbar.getChildren().addAll(
            newGameButton,
            trainButton,
            loadModelButton,
            new Separator(),
            whiteLabel, whitePlayerCombo,
            blackLabel, blackPlayerCombo
        );

        return toolbar;
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPrefWidth(200);
        panel.setStyle("-fx-background-color: #34495e;");

        Label titleLabel = new Label("Steuerung");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        diceLabel = new Label("Würfel: -");
        diceLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        rollButton = new Button("Würfeln");
        rollButton.setPrefWidth(150);
        rollButton.setOnAction(e -> rollDice());

        Button undoButton = new Button("Zug zurück");
        undoButton.setPrefWidth(150);
        undoButton.setOnAction(e -> undoMove());

        Button confirmButton = new Button("Zug bestätigen");
        confirmButton.setPrefWidth(150);
        confirmButton.setOnAction(e -> confirmMove());

        // Score display
        Label scoreTitle = new Label("Punkte");
        scoreTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label whiteScore = new Label("Weiß ausgewürfelt: 0");
        whiteScore.setStyle("-fx-text-fill: white;");

        Label blackScore = new Label("Schwarz ausgewürfelt: 0");
        blackScore.setStyle("-fx-text-fill: white;");

        panel.getChildren().addAll(
            titleLabel,
            new Separator(),
            diceLabel,
            rollButton,
            undoButton,
            confirmButton,
            new Separator(),
            scoreTitle,
            whiteScore,
            blackScore
        );

        return panel;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #34495e;");

        statusLabel = new Label("Weiß ist am Zug");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        statusBar.getChildren().add(statusLabel);
        return statusBar;
    }

    private void newGame() {
        gameState = new GameState();
        boardView.setGameState(gameState);
        isWhiteTurn = true;
        currentDice = null;
        availableMoves = null;
        selectedFromPoint = -1;
        diceLabel.setText("Würfel: -");

        // Create players based on selection
        whitePlayer = createPlayer(whitePlayerCombo.getValue(), true);
        blackPlayer = createPlayer(blackPlayerCombo.getValue(), false);

        updateStatus();

        // If first player is AI, start their turn
        if (!getCurrentPlayer().isHuman()) {
            startAITurn();
        }
    }

    private Player createPlayer(String type, boolean isWhite) {
        switch (type) {
            case "KI (Random)":
                return new RandomPlayer();
            case "KI (TD-Gammon)":
                return new TDPlayer(tdNetwork, isWhite);
            default:
                return new HumanPlayer();
        }
    }

    private Player getCurrentPlayer() {
        return isWhiteTurn ? whitePlayer : blackPlayer;
    }

    private void startAITurn() {
        // Delay to make it visible
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(this::executeAITurn);
        }).start();
    }

    private void executeAITurn() {
        Player player = getCurrentPlayer();
        if (player.isHuman()) return;

        // Roll dice
        currentDice = Dice.roll();
        int[] moves = Dice.getMovesFromRoll(currentDice[0], currentDice[1]);

        String diceText = String.format("Würfel: %d, %d", currentDice[0], currentDice[1]);
        if (currentDice[0] == currentDice[1]) {
            diceText += " (Pasch!)";
        }
        diceLabel.setText(diceText);
        boardView.setAvailableMoves(moves);

        // Get AI moves
        gameState.setWhiteTurn(isWhiteTurn);
        List<Move> aiMoves = player.selectMoves(gameState, moves);

        if (aiMoves.isEmpty()) {
            // No valid moves - skip turn
            confirmMove();
            return;
        }

        // Execute moves with delay for visibility
        executeAIMovesWithDelay(aiMoves, 0);
    }

    private void executeAIMovesWithDelay(List<Move> moves, int index) {
        if (index >= moves.size()) {
            // All moves done
            new Thread(() -> {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(this::confirmMove);
            }).start();
            return;
        }

        Move move = moves.get(index);
        gameState.setWhiteTurn(isWhiteTurn);
        gameState.applyMove(move);
        boardView.updateBoard();

        // Update dice display
        if (availableMoves == null) {
            availableMoves = new ArrayList<>();
            int[] diceValues = Dice.getMovesFromRoll(currentDice[0], currentDice[1]);
            for (int v : diceValues) availableMoves.add(v);
        }
        availableMoves.remove(Integer.valueOf(move.getDieValue()));
        updateDiceLabel();

        // Next move after delay
        new Thread(() -> {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(() -> executeAIMovesWithDelay(moves, index + 1));
        }).start();
    }

    private void rollDice() {
        // Check if current player is AI
        if (getCurrentPlayer() != null && !getCurrentPlayer().isHuman()) {
            showAlert("KI am Zug", "Warte bis die KI ihren Zug beendet hat.");
            return;
        }

        if (availableMoves != null && !availableMoves.isEmpty()) {
            showAlert("Würfel bereits geworfen", "Du musst erst deine Züge machen.");
            return;
        }

        currentDice = Dice.roll();
        int[] moves = Dice.getMovesFromRoll(currentDice[0], currentDice[1]);

        // Convert to list for easy removal
        availableMoves = new ArrayList<>();
        for (int move : moves) {
            availableMoves.add(move);
        }

        String diceText = String.format("Würfel: %d, %d", currentDice[0], currentDice[1]);
        if (currentDice[0] == currentDice[1]) {
            diceText += " (Pasch!)";
        }
        diceLabel.setText(diceText);

        boardView.setAvailableMoves(moves);
        updateStatus();
    }

    private void undoMove() {
        // TODO: Implement undo within current turn
        showAlert("Nicht implementiert", "Zug zurück ist noch nicht implementiert.");
    }

    private void confirmMove() {
        // Switch player
        isWhiteTurn = !isWhiteTurn;
        currentDice = null;
        availableMoves = null;
        selectedFromPoint = -1;
        diceLabel.setText("Würfel: -");
        boardView.clearSelection();
        updateStatus();

        // Check for game over
        if (gameState.isGameOver()) {
            String winner = gameState.getWinner() ? "Weiß" : "Schwarz";
            showAlert("Spiel beendet", winner + " hat gewonnen!");
            return;
        }

        // If next player is AI, start their turn
        if (!getCurrentPlayer().isHuman()) {
            startAITurn();
        }
    }

    private void onCheckerSelected(int point, boolean isWhite) {
        if (isWhite != isWhiteTurn) {
            showAlert("Falscher Spieler", "Du kannst nur deine eigenen Steine bewegen.");
            boardView.clearSelection();
            return;
        }
        if (availableMoves == null || availableMoves.isEmpty()) {
            showAlert("Würfel zuerst", "Du musst zuerst würfeln!");
            boardView.clearSelection();
            return;
        }
        selectedFromPoint = point;
        int[] movesArray = availableMoves.stream().mapToInt(i -> i).toArray();
        boardView.highlightValidTargets(point, movesArray, isWhiteTurn);
    }

    private void onMoveExecuted(int fromPoint, int toPoint) {
        if (availableMoves == null || availableMoves.isEmpty()) {
            return;
        }

        // Calculate the die value used
        int dieUsed = Math.abs(toPoint - fromPoint);

        // For white moving from higher to lower points
        // For black moving from lower to higher points
        if (isWhiteTurn) {
            dieUsed = fromPoint - toPoint;
        } else {
            dieUsed = toPoint - fromPoint;
        }

        // Check if this die value is available
        if (!availableMoves.contains(dieUsed)) {
            // Maybe it's a bear-off with a higher die
            boolean found = false;
            for (int i = 0; i < availableMoves.size(); i++) {
                if (availableMoves.get(i) >= dieUsed) {
                    dieUsed = availableMoves.get(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return;
            }
        }

        // Ensure game state knows whose turn it is
        gameState.setWhiteTurn(isWhiteTurn);

        // Apply the move to game state
        boolean isHit = gameState.getPoint(toPoint) != 0 &&
                       (isWhiteTurn ? gameState.getPoint(toPoint) < 0 : gameState.getPoint(toPoint) > 0);
        Move move = new Move(fromPoint, toPoint, dieUsed, isHit);
        gameState.applyMove(move);

        // Remove the used die
        availableMoves.remove(Integer.valueOf(dieUsed));

        // Update dice label
        updateDiceLabel();

        // Update board display
        boardView.updateBoard();

        // Check if turn is over
        if (availableMoves.isEmpty()) {
            confirmMove();
        }
    }

    private void updateDiceLabel() {
        if (availableMoves == null || availableMoves.isEmpty()) {
            diceLabel.setText("Würfel: -");
        } else {
            StringBuilder sb = new StringBuilder("Würfel: ");
            for (int i = 0; i < availableMoves.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(availableMoves.get(i));
            }
            diceLabel.setText(sb.toString());
        }
    }

    private void updateStatus() {
        String player = isWhiteTurn ? "Weiß" : "Schwarz";
        String playerType = isWhiteTurn ? whitePlayerCombo.getValue() : blackPlayerCombo.getValue();
        statusLabel.setText(player + " ist am Zug (" + playerType + ")");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showTrainingDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("TD-Gammon Training");
        dialog.setHeaderText("Self-Play Training");

        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Number of games
        Spinner<Integer> gamesSpinner = new Spinner<>(100, 100000, 1000, 100);
        gamesSpinner.setEditable(true);
        grid.add(new Label("Anzahl Spiele:"), 0, 0);
        grid.add(gamesSpinner, 1, 0);

        // Progress
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        grid.add(new Label("Fortschritt:"), 0, 1);
        grid.add(progressBar, 1, 1);

        // Stats
        Label statsLabel = new Label("Bereit zum Training");
        statsLabel.setWrapText(true);
        grid.add(statsLabel, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Buttons
        ButtonType startButton = new ButtonType("Training starten", ButtonBar.ButtonData.OK_DONE);
        ButtonType stopButton = new ButtonType("Stopp", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType saveButton = new ButtonType("Modell speichern", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(startButton, stopButton, saveButton);

        // Get button references
        Button startBtn = (Button) dialog.getDialogPane().lookupButton(startButton);
        Button stopBtn = (Button) dialog.getDialogPane().lookupButton(stopButton);
        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveButton);

        // Initially disable stop button
        stopBtn.setDisable(true);

        // Training state
        final TDTrainer[] trainerHolder = new TDTrainer[1];
        final Thread[] trainingThread = new Thread[1];
        final boolean[] isTraining = {false};

        // Button actions
        startBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume(); // Don't close dialog

            int numGames = gamesSpinner.getValue();
            statsLabel.setText("Training läuft...");
            progressBar.setProgress(0);

            // Disable/enable buttons
            startBtn.setDisable(true);
            saveBtn.setDisable(true);
            stopBtn.setDisable(false);
            gamesSpinner.setDisable(true);
            isTraining[0] = true;

            trainerHolder[0] = new TDTrainer(tdNetwork);
            trainerHolder[0].setProgressCallback(progress -> {
                Platform.runLater(() -> {
                    progressBar.setProgress(progress.getProgress());
                    statsLabel.setText(String.format(
                        "Spiel %d/%d | Weiß: %d (%.1f%%) | Schwarz: %d (%.1f%%)",
                        progress.currentGame, progress.totalGames,
                        progress.whiteWins, progress.getWhiteWinRate() * 100,
                        progress.blackWins, (1 - progress.getWhiteWinRate()) * 100
                    ));
                });
            });

            trainingThread[0] = new Thread(() -> {
                long start = System.currentTimeMillis();
                trainerHolder[0].train(numGames);
                long elapsed = System.currentTimeMillis() - start;

                Platform.runLater(() -> {
                    double speed = elapsed > 0 ? numGames / (elapsed / 1000.0) : 0;
                    statsLabel.setText(String.format(
                        "Fertig! %d Spiele in %.1f Sek (%.1f Spiele/Sek)",
                        trainerHolder[0].getGamesPlayed(), elapsed / 1000.0, speed
                    ));
                    progressBar.setProgress(1.0);

                    // Re-enable buttons
                    startBtn.setDisable(false);
                    saveBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    gamesSpinner.setDisable(false);
                    isTraining[0] = false;
                });
            });
            trainingThread[0].start();
        });

        stopBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume(); // Don't close dialog
            if (trainerHolder[0] != null) {
                trainerHolder[0].requestStop();
                statsLabel.setText("Training wird gestoppt...");
                stopBtn.setDisable(true);
            }
        });

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume(); // Don't close dialog

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Modell speichern");
            fileChooser.setInitialFileName("td-gammon-model.zip");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Model Files", "*.zip")
            );

            File file = fileChooser.showSaveDialog(dialog.getOwner());
            if (file != null) {
                try {
                    tdNetwork.save(file.getAbsolutePath());
                    statsLabel.setText("Modell gespeichert: " + file.getName());
                } catch (IOException e) {
                    statsLabel.setText("Fehler beim Speichern: " + e.getMessage());
                }
            }
        });

        dialog.showAndWait();

        // Stop training if still running
        if (trainerHolder[0] != null) {
            trainerHolder[0].requestStop();
        }
    }

    private void loadModel() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Modell laden");
        fileChooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("Model Files", "*.zip")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                tdNetwork.load(file.getAbsolutePath());
                showAlert("Modell geladen", "TD-Gammon Modell wurde geladen:\n" + file.getName());
            } catch (IOException e) {
                showAlert("Fehler", "Konnte Modell nicht laden:\n" + e.getMessage());
            }
        }
    }

    private void tryLoadSavedModel() {
        // Try to find and load a saved model from common locations
        String[] possiblePaths = {
            "td-gammon-model.zip",
            "E:/Projekte/ClaudeCode/Backgammon3D/td-gammon-model.zip",
            System.getProperty("user.dir") + "/td-gammon-model.zip"
        };

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    tdNetwork.load(file.getAbsolutePath());
                    System.out.println("Trainiertes Modell geladen: " + file.getAbsolutePath());
                    return;
                } catch (IOException e) {
                    System.err.println("Fehler beim Laden: " + e.getMessage());
                }
            }
        }
        System.out.println("Kein gespeichertes Modell gefunden - verwende untrainiertes Netzwerk");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
