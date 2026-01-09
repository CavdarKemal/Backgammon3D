package com.backgammon3d.ai;

import com.backgammon3d.model.GameState;
import com.backgammon3d.model.Move;

import java.util.List;

/**
 * Interface for Backgammon players (human or AI).
 */
public interface Player {

    /**
     * Returns the name of this player.
     */
    String getName();

    /**
     * Selects moves for the current turn.
     *
     * @param state Current game state
     * @param dice Available dice values
     * @return List of moves to execute (may be empty if no valid moves)
     */
    List<Move> selectMoves(GameState state, int[] dice);

    /**
     * Returns true if this is a human player (requires UI input).
     */
    boolean isHuman();

    /**
     * Called when a game ends. Used for learning.
     *
     * @param won True if this player won
     */
    default void onGameEnd(boolean won) {
        // Default: do nothing
    }

    /**
     * Called after each move. Used for learning.
     *
     * @param state The state after the move
     */
    default void onMoveComplete(GameState state) {
        // Default: do nothing
    }
}
