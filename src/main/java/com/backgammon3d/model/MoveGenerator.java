package com.backgammon3d.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates all legal moves for a given game state and dice roll.
 */
public class MoveGenerator {

    /**
     * Generates all possible single moves for a single die value.
     */
    public static List<Move> generateSingleMoves(GameState state, int dieValue, boolean isWhite) {
        List<Move> moves = new ArrayList<>();

        // Must enter from bar first
        if (isWhite && state.getWhiteBar() > 0) {
            generateBarEntry(state, dieValue, isWhite, moves);
            return moves;
        }
        if (!isWhite && state.getBlackBar() > 0) {
            generateBarEntry(state, dieValue, isWhite, moves);
            return moves;
        }

        // Generate normal moves
        for (int from = 0; from < GameState.NUM_POINTS; from++) {
            if (!state.isOwnedBy(from, isWhite)) {
                continue;
            }

            int target = BackgammonRules.calculateTarget(from, dieValue, isWhite);

            if (target == -2) {
                // Invalid move
                continue;
            }

            if (target == Move.BEAR_OFF) {
                // Bear off move
                Move bearOff = Move.bearOff(from, dieValue);
                if (BackgammonRules.canBearOff(state, bearOff, isWhite)) {
                    moves.add(bearOff);
                }
            } else {
                // Normal move
                if (BackgammonRules.canMoveTo(state, target, isWhite)) {
                    boolean isHit = BackgammonRules.willHit(state, target, isWhite);
                    moves.add(new Move(from, target, dieValue, isHit));
                }
            }
        }

        return moves;
    }

    /**
     * Generates bar entry moves.
     */
    private static void generateBarEntry(GameState state, int dieValue, boolean isWhite, List<Move> moves) {
        int target = BackgammonRules.calculateTarget(Move.BAR, dieValue, isWhite);

        if (target >= 0 && target < 24 && BackgammonRules.canMoveTo(state, target, isWhite)) {
            boolean isHit = BackgammonRules.willHit(state, target, isWhite);
            moves.add(Move.fromBar(target, dieValue, isHit));
        }
    }

    /**
     * Generates all possible move sequences for a complete turn.
     * Each sequence represents one way to use all dice (or as many as possible).
     *
     * @param state Current game state
     * @param dice Available dice values
     * @param isWhite Current player
     * @return List of move sequences, where each sequence is a list of moves
     */
    public static List<List<Move>> generateAllMoveSequences(GameState state, int[] dice, boolean isWhite) {
        List<List<Move>> sequences = new ArrayList<>();
        List<Move> currentSequence = new ArrayList<>();
        List<Integer> remainingDice = new ArrayList<>();

        for (int d : dice) {
            remainingDice.add(d);
        }

        generateSequencesRecursive(state, remainingDice, isWhite, currentSequence, sequences);

        // Filter to keep only the longest sequences (must use as many dice as possible)
        int maxLength = 0;
        for (List<Move> seq : sequences) {
            maxLength = Math.max(maxLength, seq.size());
        }

        final int finalMax = maxLength;
        sequences.removeIf(seq -> seq.size() < finalMax);

        // If no moves possible, return empty list
        if (sequences.isEmpty()) {
            sequences.add(new ArrayList<>());
        }

        return sequences;
    }

    private static void generateSequencesRecursive(
            GameState state,
            List<Integer> remainingDice,
            boolean isWhite,
            List<Move> currentSequence,
            List<List<Move>> sequences) {

        if (remainingDice.isEmpty()) {
            sequences.add(new ArrayList<>(currentSequence));
            return;
        }

        boolean anyMoveFound = false;

        // Try each remaining die
        for (int i = 0; i < remainingDice.size(); i++) {
            int dieValue = remainingDice.get(i);

            List<Move> possibleMoves = generateSingleMoves(state, dieValue, isWhite);

            for (Move move : possibleMoves) {
                anyMoveFound = true;

                // Apply move
                GameState newState = state.copy();
                newState.applyMove(move);

                // Remove used die
                List<Integer> newRemainingDice = new ArrayList<>(remainingDice);
                newRemainingDice.remove(i);

                // Continue recursively
                currentSequence.add(move);
                generateSequencesRecursive(newState, newRemainingDice, isWhite, currentSequence, sequences);
                currentSequence.remove(currentSequence.size() - 1);
            }
        }

        // If no move was possible with any die, save current sequence
        if (!anyMoveFound) {
            sequences.add(new ArrayList<>(currentSequence));
        }
    }

    /**
     * Gets valid target points for a specific checker.
     *
     * @param state Current game state
     * @param fromPoint Source point
     * @param dice Available dice values
     * @param isWhite Current player
     * @return List of valid target points (-1 for bear off)
     */
    public static List<Integer> getValidTargets(GameState state, int fromPoint, int[] dice, boolean isWhite) {
        List<Integer> targets = new ArrayList<>();

        for (int dieValue : dice) {
            int target = BackgammonRules.calculateTarget(fromPoint, dieValue, isWhite);

            if (target == -2) {
                continue;
            }

            if (target == Move.BEAR_OFF) {
                Move bearOff = Move.bearOff(fromPoint, dieValue);
                if (BackgammonRules.canBearOff(state, bearOff, isWhite)) {
                    if (!targets.contains(Move.BEAR_OFF)) {
                        targets.add(Move.BEAR_OFF);
                    }
                }
            } else {
                if (BackgammonRules.canMoveTo(state, target, isWhite)) {
                    if (!targets.contains(target)) {
                        targets.add(target);
                    }
                }
            }
        }

        return targets;
    }

    /**
     * Checks if any move is possible.
     */
    public static boolean hasAnyMove(GameState state, int[] dice, boolean isWhite) {
        for (int dieValue : dice) {
            if (!generateSingleMoves(state, dieValue, isWhite).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
