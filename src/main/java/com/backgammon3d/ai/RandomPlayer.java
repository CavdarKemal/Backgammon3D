package com.backgammon3d.ai;

import com.backgammon3d.model.GameState;
import com.backgammon3d.model.Move;
import com.backgammon3d.model.MoveGenerator;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * AI player that selects moves randomly.
 * Used as a baseline for comparison with TD-Gammon.
 */
public class RandomPlayer implements Player {

    private final Random random = new Random();
    private final String name;

    public RandomPlayer() {
        this("Random");
    }

    public RandomPlayer(String name) {
        this.name = name;
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

        // Pick a random sequence
        int index = random.nextInt(allSequences.size());
        return allSequences.get(index);
    }

    @Override
    public boolean isHuman() {
        return false;
    }
}
