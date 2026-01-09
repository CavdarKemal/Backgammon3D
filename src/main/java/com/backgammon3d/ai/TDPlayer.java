package com.backgammon3d.ai;

import com.backgammon3d.model.GameState;
import com.backgammon3d.model.Move;
import com.backgammon3d.model.MoveGenerator;
import com.backgammon3d.neural.BoardEncoder;
import com.backgammon3d.neural.TDNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TD-Gammon AI player using temporal difference learning.
 *
 * This player:
 * - Evaluates all possible move sequences
 * - Selects the move leading to the highest estimated win probability
 * - Can learn from self-play using TD(λ)
 */
public class TDPlayer implements Player {

    private final TDNetwork network;
    private final String name;
    private final boolean isWhite;
    private final boolean learning;

    // For TD learning during games
    private List<float[]> gameStates = new ArrayList<>();

    public TDPlayer(TDNetwork network, boolean isWhite) {
        this(network, isWhite, false);
    }

    public TDPlayer(TDNetwork network, boolean isWhite, boolean learning) {
        this.network = network;
        this.isWhite = isWhite;
        this.learning = learning;
        this.name = "TD-Gammon" + (learning ? " (Learning)" : "");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Move> selectMoves(GameState state, int[] dice) {
        List<List<Move>> allSequences = MoveGenerator.generateAllMoveSequences(state, dice, state.isWhiteTurn());

        if (allSequences.isEmpty()) {
            return Collections.emptyList();
        }

        // Store current state for learning
        if (learning) {
            gameStates.add(BoardEncoder.encode(state, isWhite));
        }

        // Evaluate all possible resulting positions
        List<Move> bestSequence = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (List<Move> sequence : allSequences) {
            // Create a copy of the state and apply the moves
            GameState resultState = new GameState(state);
            for (Move move : sequence) {
                resultState.applyMove(move);
            }

            // Evaluate from our perspective
            float[] encoding = BoardEncoder.encode(resultState, isWhite);
            double value = network.evaluate(encoding);

            // We want to maximize our win probability
            if (value > bestValue) {
                bestValue = value;
                bestSequence = sequence;
            }
        }

        return bestSequence != null ? bestSequence : Collections.emptyList();
    }

    @Override
    public boolean isHuman() {
        return false;
    }

    @Override
    public void onMoveComplete(GameState state) {
        if (learning) {
            gameStates.add(BoardEncoder.encode(state, isWhite));
        }
    }

    @Override
    public void onGameEnd(boolean won) {
        if (learning && !gameStates.isEmpty()) {
            // TD(λ) update
            double reward = won ? 1.0 : 0.0;
            float[][] states = gameStates.toArray(new float[0][]);
            network.tdLambdaUpdate(states, reward);

            // Clear for next game
            gameStates.clear();
        }
    }

    /**
     * Evaluates the current position.
     *
     * @param state The game state
     * @return Win probability for this player [0, 1]
     */
    public double evaluate(GameState state) {
        float[] encoding = BoardEncoder.encode(state, isWhite);
        return network.evaluate(encoding);
    }

    /**
     * Returns the underlying network.
     */
    public TDNetwork getNetwork() {
        return network;
    }

    /**
     * Returns whether this player is in learning mode.
     */
    public boolean isLearning() {
        return learning;
    }

    /**
     * Resets learning state for a new game.
     */
    public void resetLearning() {
        gameStates.clear();
    }
}
