package com.backgammon3d.neural;

import com.backgammon3d.model.Dice;
import com.backgammon3d.model.GameState;
import com.backgammon3d.model.Move;
import com.backgammon3d.model.MoveGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * TD-Gammon Self-Play Trainer.
 *
 * Trains the neural network by having it play against itself,
 * learning from the outcomes using TD(λ) learning.
 */
public class TDTrainer {

    private final TDNetwork network;
    private final Random random = new Random();
    private double alpha = 0.1;      // Learning rate
    private double lambda = 0.7;     // Eligibility trace decay
    private int maxMovesPerGame = 200; // Prevent infinite games

    // Statistics
    private int gamesPlayed = 0;
    private int whiteWins = 0;
    private int blackWins = 0;
    private long totalMoves = 0;

    // Callback for progress updates
    private Consumer<TrainingProgress> progressCallback;
    private volatile boolean stopRequested = false;

    public TDTrainer(TDNetwork network) {
        this.network = network;
        this.network.setLambda(lambda);
    }

    /**
     * Trains the network through self-play.
     *
     * @param numGames Number of games to play
     */
    public void train(int numGames) {
        stopRequested = false;

        for (int game = 0; game < numGames && !stopRequested; game++) {
            TrainingResult result = playOneGame();

            gamesPlayed++;
            totalMoves += result.numMoves;
            if (result.whiteWon) {
                whiteWins++;
            } else {
                blackWins++;
            }

            // Report progress dynamically based on total games
            int reportInterval = Math.max(1, Math.min(100, numGames / 20));
            if ((game + 1) % reportInterval == 0 || game == numGames - 1) {
                reportProgress(game + 1, numGames);
            }
        }
    }

    /**
     * Plays one self-play game and learns from it.
     * Uses random move selection for speed (like original TD-Gammon).
     */
    private TrainingResult playOneGame() {
        GameState state = new GameState();

        // Track states for learning (sample every few moves to save memory)
        List<float[]> states = new ArrayList<>();
        int moveCount = 0;
        int turnCount = 0;

        while (!state.isGameOver() && turnCount < maxMovesPerGame) {
            boolean isWhiteTurn = state.isWhiteTurn();

            // Roll dice
            int[] diceRoll = Dice.roll();
            int[] dice = Dice.getMovesFromRoll(diceRoll[0], diceRoll[1]);

            // Store state occasionally for learning
            if (turnCount % 3 == 0) {
                states.add(BoardEncoder.encode(state, true)); // Always from white's perspective
            }

            // Get all possible move sequences
            List<List<Move>> allSequences = MoveGenerator.generateAllMoveSequences(state, dice, isWhiteTurn);

            if (!allSequences.isEmpty()) {
                // Pick random move sequence (fast exploration)
                List<Move> moves = allSequences.get(random.nextInt(allSequences.size()));

                // Apply moves
                for (Move move : moves) {
                    state.applyMove(move);
                    moveCount++;
                }
            } else {
                state.switchTurn();
            }

            turnCount++;
        }

        // Determine winner
        boolean whiteWon = state.isGameOver() ? state.getWinner() : (state.getWhiteBearOff() > state.getBlackBearOff());

        // Simple TD update: adjust all states toward final outcome
        double reward = whiteWon ? 1.0 : 0.0;
        for (int i = 0; i < states.size(); i++) {
            double discount = Math.pow(lambda, states.size() - 1 - i);
            double target = reward * discount + (1 - discount) * 0.5;
            network.tdUpdate(states.get(i), target, alpha * 0.1);
        }

        return new TrainingResult(whiteWon, moveCount);
    }

    /**
     * Updates the network using TD(λ) learning.
     */
    private void updateNetwork(List<float[]> states, double finalReward) {
        if (states.isEmpty()) return;

        int n = states.size();
        double[] values = new double[n + 1];

        // Compute values for all states
        for (int i = 0; i < n; i++) {
            values[i] = network.evaluate(states.get(i));
        }
        values[n] = finalReward;

        // TD(λ) updates going backwards
        double[] eligibility = new double[n];

        for (int t = n - 1; t >= 0; t--) {
            // TD error at time t
            double tdError = values[t + 1] - values[t];

            // Update eligibility traces
            for (int i = 0; i <= t; i++) {
                double decay = Math.pow(lambda, t - i);
                eligibility[i] += decay;
            }

            // Compute λ-return target
            double target = values[t] + alpha * tdError * eligibility[t];
            target = Math.max(0.0, Math.min(1.0, target)); // Clip to [0,1]

            // Simple gradient update (approximation)
            network.tdUpdate(states.get(t), target, alpha);
        }
    }

    /**
     * Reports training progress.
     */
    private void reportProgress(int currentGame, int totalGames) {
        TrainingProgress progress = new TrainingProgress(
            currentGame,
            totalGames,
            gamesPlayed,
            whiteWins,
            blackWins,
            totalMoves
        );

        System.out.printf("Training: %d/%d games | White: %d (%.1f%%) | Black: %d (%.1f%%) | Avg moves: %.1f%n",
            currentGame, totalGames,
            whiteWins, (100.0 * whiteWins / gamesPlayed),
            blackWins, (100.0 * blackWins / gamesPlayed),
            (double) totalMoves / gamesPlayed);

        if (progressCallback != null) {
            progressCallback.accept(progress);
        }
    }

    /**
     * Requests the training to stop after the current game.
     */
    public void requestStop() {
        stopRequested = true;
    }

    /**
     * Saves the trained model.
     */
    public void saveModel(String path) throws IOException {
        network.save(path);
    }

    /**
     * Resets training statistics.
     */
    public void resetStats() {
        gamesPlayed = 0;
        whiteWins = 0;
        blackWins = 0;
        totalMoves = 0;
    }

    // Getters and setters

    public void setProgressCallback(Consumer<TrainingProgress> callback) {
        this.progressCallback = callback;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
        this.network.setLambda(lambda);
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getWhiteWins() {
        return whiteWins;
    }

    public int getBlackWins() {
        return blackWins;
    }

    public double getWhiteWinRate() {
        return gamesPlayed > 0 ? (double) whiteWins / gamesPlayed : 0.5;
    }

    /**
     * Training result for a single game.
     */
    private static class TrainingResult {
        final boolean whiteWon;
        final int numMoves;

        TrainingResult(boolean whiteWon, int numMoves) {
            this.whiteWon = whiteWon;
            this.numMoves = numMoves;
        }
    }

    /**
     * Training progress information.
     */
    public static class TrainingProgress {
        public final int currentGame;
        public final int totalGames;
        public final int totalGamesPlayed;
        public final int whiteWins;
        public final int blackWins;
        public final long totalMoves;

        public TrainingProgress(int currentGame, int totalGames, int totalGamesPlayed,
                               int whiteWins, int blackWins, long totalMoves) {
            this.currentGame = currentGame;
            this.totalGames = totalGames;
            this.totalGamesPlayed = totalGamesPlayed;
            this.whiteWins = whiteWins;
            this.blackWins = blackWins;
            this.totalMoves = totalMoves;
        }

        public double getProgress() {
            return (double) currentGame / totalGames;
        }

        public double getWhiteWinRate() {
            return totalGamesPlayed > 0 ? (double) whiteWins / totalGamesPlayed : 0.5;
        }
    }

    /**
     * Main method for command-line training.
     */
    public static void main(String[] args) {
        int numGames = 1000;
        String modelPath = "td-gammon-model.zip";

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-n") && i + 1 < args.length) {
                numGames = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-o") && i + 1 < args.length) {
                modelPath = args[++i];
            }
        }

        System.out.println("=== TD-Gammon Self-Play Trainer ===");
        System.out.println("Games to play: " + numGames);
        System.out.println("Output model: " + modelPath);
        System.out.println();

        TDNetwork network = new TDNetwork();
        TDTrainer trainer = new TDTrainer(network);

        long startTime = System.currentTimeMillis();
        trainer.train(numGames);
        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println();
        System.out.println("=== Training Complete ===");
        System.out.printf("Time: %.1f seconds%n", elapsed / 1000.0);
        System.out.printf("Speed: %.1f games/second%n", numGames / (elapsed / 1000.0));

        try {
            trainer.saveModel(modelPath);
            System.out.println("Model saved to: " + modelPath);
        } catch (IOException e) {
            System.err.println("Failed to save model: " + e.getMessage());
        }
    }
}
