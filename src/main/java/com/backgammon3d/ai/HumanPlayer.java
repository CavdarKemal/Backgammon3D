package com.backgammon3d.ai;

import com.backgammon3d.model.GameState;
import com.backgammon3d.model.Move;

import java.util.Collections;
import java.util.List;

/**
 * Represents a human player who provides moves through the UI.
 * The selectMoves method returns empty as moves are handled by UI callbacks.
 */
public class HumanPlayer implements Player {

    private final String name;

    public HumanPlayer() {
        this("Mensch");
    }

    public HumanPlayer(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Move> selectMoves(GameState state, int[] dice) {
        // Human moves are handled through UI - this method is not used
        return Collections.emptyList();
    }

    @Override
    public boolean isHuman() {
        return true;
    }
}
