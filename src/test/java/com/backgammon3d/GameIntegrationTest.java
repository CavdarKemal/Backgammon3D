package com.backgammon3d;

import com.backgammon3d.ai.Player;
import com.backgammon3d.ai.RandomPlayer;
import com.backgammon3d.ai.TDPlayer;
import com.backgammon3d.model.Dice;
import com.backgammon3d.model.GameState;
import com.backgammon3d.model.Move;
import com.backgammon3d.model.MoveGenerator;
import com.backgammon3d.neural.TDNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for game logic between Main, GameState, and AI players.
 * These tests verify that dice values are correctly used and moves are properly executed.
 */
@DisplayName("Game Integration Tests")
class GameIntegrationTest {

    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameState = new GameState();
    }

    @Nested
    @DisplayName("Dice Value Tests (Critical Bug Area)")
    class DiceValueTests {

        @Test
        @DisplayName("Dice.roll() returns values 1-6")
        void diceRollReturnsValidValues() {
            for (int i = 0; i < 100; i++) {
                int[] roll = Dice.roll();
                assertEquals(2, roll.length, "Roll should return 2 values");
                assertTrue(roll[0] >= 1 && roll[0] <= 6, "Die 1 should be 1-6: " + roll[0]);
                assertTrue(roll[1] >= 1 && roll[1] <= 6, "Die 2 should be 1-6: " + roll[1]);
            }
        }

        @Test
        @DisplayName("Dice.getMovesFromRoll normal roll returns 2 moves")
        void normalRollReturnsTwoMoves() {
            int[] moves = Dice.getMovesFromRoll(3, 5);
            assertEquals(2, moves.length, "Normal roll should return 2 moves");
            assertEquals(3, moves[0]);
            assertEquals(5, moves[1]);
        }

        @Test
        @DisplayName("Dice.getMovesFromRoll doubles returns 4 moves")
        void doublesReturnsFourMoves() {
            int[] moves = Dice.getMovesFromRoll(4, 4);
            assertEquals(4, moves.length, "Doubles should return 4 moves");
            for (int move : moves) {
                assertEquals(4, move, "All moves should be 4");
            }
        }

        @Test
        @DisplayName("calculateDieUsed logic for white (Main.onMoveExecuted)")
        void calculateDieUsedForWhite() {
            // Simulating the logic from Main.onMoveExecuted
            // White moves from higher to lower indices
            int fromPoint = 18;
            int toPoint = 13;
            boolean isWhiteTurn = true;

            int dieUsed;
            if (isWhiteTurn) {
                dieUsed = fromPoint - toPoint;
            } else {
                dieUsed = toPoint - fromPoint;
            }

            assertEquals(5, dieUsed, "Die used should be 5 (18-13)");
        }

        @Test
        @DisplayName("calculateDieUsed logic for black (Main.onMoveExecuted)")
        void calculateDieUsedForBlack() {
            int fromPoint = 5;
            int toPoint = 10;
            boolean isWhiteTurn = false;

            int dieUsed;
            if (isWhiteTurn) {
                dieUsed = fromPoint - toPoint;
            } else {
                dieUsed = toPoint - fromPoint;
            }

            assertEquals(5, dieUsed, "Die used should be 5 (10-5)");
        }
    }

    @Nested
    @DisplayName("Move Execution Tests")
    class MoveExecutionTests {

        @Test
        @DisplayName("White move from point 18 with die 5 goes to 13")
        void whiteMoveCorrectDirection() {
            gameState.setWhiteTurn(true);

            // Generate moves with die 5
            List<Move> moves = MoveGenerator.generateSingleMoves(gameState, 5, true);

            // Find move from 18
            Move moveFrom18 = moves.stream()
                .filter(m -> m.getFrom() == 18)
                .findFirst()
                .orElse(null);

            assertNotNull(moveFrom18, "Should have move from point 18");
            assertEquals(13, moveFrom18.getTo(), "Target should be 13 (18-5)");
            assertEquals(5, moveFrom18.getDieValue(), "Die value should be 5");
        }

        @Test
        @DisplayName("Black move from point 5 with die 5 goes to 10")
        void blackMoveCorrectDirection() {
            gameState.setWhiteTurn(false);

            List<Move> moves = MoveGenerator.generateSingleMoves(gameState, 5, false);

            Move moveFrom5 = moves.stream()
                .filter(m -> m.getFrom() == 5)
                .findFirst()
                .orElse(null);

            assertNotNull(moveFrom5, "Should have move from point 5");
            assertEquals(10, moveFrom5.getTo(), "Target should be 10 (5+5)");
            assertEquals(5, moveFrom5.getDieValue(), "Die value should be 5");
        }

        @Test
        @DisplayName("Move applied to GameState correctly updates board")
        void moveAppliedCorrectly() {
            gameState.setWhiteTurn(true);
            int initialWhiteOn18 = gameState.getPoint(18);
            int initialWhiteOn13 = gameState.getPoint(13);

            // Apply a move: 18 -> 13 (die 5)
            Move move = new Move(18, 13, 5, false);
            gameState.applyMove(move);

            assertEquals(initialWhiteOn18 - 1, gameState.getPoint(18),
                "Source point should have one less checker");
            assertEquals(initialWhiteOn13 + 1, gameState.getPoint(13),
                "Target point should have one more checker");
        }

        @Test
        @DisplayName("availableMoves correctly tracks used dice")
        void availableMovesTracksUsedDice() {
            // Simulating Main.java dice tracking
            int[] currentDice = {3, 5};
            List<Integer> availableMoves = new ArrayList<>();
            for (int d : Dice.getMovesFromRoll(currentDice[0], currentDice[1])) {
                availableMoves.add(d);
            }

            assertEquals(2, availableMoves.size());
            assertTrue(availableMoves.contains(3));
            assertTrue(availableMoves.contains(5));

            // Use die 5
            availableMoves.remove(Integer.valueOf(5));

            assertEquals(1, availableMoves.size());
            assertTrue(availableMoves.contains(3));
            assertFalse(availableMoves.contains(5));
        }
    }

    @Nested
    @DisplayName("AI Move Selection Tests")
    class AIMoveSelectionTests {

        @Test
        @DisplayName("RandomPlayer selects valid moves")
        void randomPlayerSelectsValidMoves() {
            Player randomPlayer = new RandomPlayer();

            int[] dice = {3, 5};
            int[] moves = Dice.getMovesFromRoll(dice[0], dice[1]);

            gameState.setWhiteTurn(true);
            List<Move> selectedMoves = randomPlayer.selectMoves(gameState, moves);

            // Verify all selected moves are valid
            for (Move move : selectedMoves) {
                assertTrue(move.getDieValue() == 3 || move.getDieValue() == 5,
                    "Move should use dice value 3 or 5, got: " + move.getDieValue());

                // Verify direction for white
                if (!move.isFromBar() && !move.isBearOff()) {
                    assertTrue(move.getTo() < move.getFrom(),
                        "White should move to lower index: " + move);
                }
            }
        }

        @Test
        @DisplayName("RandomPlayer selects valid moves for black")
        void randomPlayerSelectsValidMovesForBlack() {
            Player randomPlayer = new RandomPlayer();

            int[] dice = {3, 5};
            int[] moves = Dice.getMovesFromRoll(dice[0], dice[1]);

            gameState.setWhiteTurn(false);
            List<Move> selectedMoves = randomPlayer.selectMoves(gameState, moves);

            // Verify direction for black
            for (Move move : selectedMoves) {
                if (!move.isFromBar() && !move.isBearOff()) {
                    assertTrue(move.getTo() > move.getFrom(),
                        "Black should move to higher index: " + move);
                }
            }
        }

        @Test
        @DisplayName("TDPlayer selects valid moves")
        void tdPlayerSelectsValidMoves() {
            TDNetwork network = new TDNetwork();
            Player tdPlayer = new TDPlayer(network, true);

            int[] dice = {3, 5};
            int[] moves = Dice.getMovesFromRoll(dice[0], dice[1]);

            gameState.setWhiteTurn(true);
            List<Move> selectedMoves = tdPlayer.selectMoves(gameState, moves);

            // Verify all selected moves are valid
            for (Move move : selectedMoves) {
                assertTrue(move.getDieValue() == 3 || move.getDieValue() == 5,
                    "Move should use dice value 3 or 5, got: " + move.getDieValue());
            }
        }

        @RepeatedTest(10)
        @DisplayName("AI moves use correct dice values (repeated test)")
        void aiMovesUseCorrectDiceValues() {
            Player randomPlayer = new RandomPlayer();

            int[] diceRoll = Dice.roll();
            int[] moves = Dice.getMovesFromRoll(diceRoll[0], diceRoll[1]);

            gameState.setWhiteTurn(true);
            List<Move> selectedMoves = randomPlayer.selectMoves(gameState, moves);

            // Create list of expected dice values
            List<Integer> expectedDice = new ArrayList<>();
            for (int d : moves) {
                expectedDice.add(d);
            }

            // Verify each move uses a valid die
            for (Move move : selectedMoves) {
                assertTrue(expectedDice.contains(move.getDieValue()),
                    "Move die value " + move.getDieValue() + " not in dice " + expectedDice);
                expectedDice.remove(Integer.valueOf(move.getDieValue()));
            }
        }
    }

    @Nested
    @DisplayName("Full Turn Simulation Tests")
    class FullTurnSimulationTests {

        @Test
        @DisplayName("Complete white turn with dice 3-5")
        void completeWhiteTurnWith35() {
            gameState.setWhiteTurn(true);
            int[] dice = {3, 5};
            int[] moves = Dice.getMovesFromRoll(dice[0], dice[1]);

            // Get all possible move sequences
            List<List<Move>> sequences = MoveGenerator.generateAllMoveSequences(gameState, moves, true);

            assertFalse(sequences.isEmpty(), "Should have at least one sequence");

            // Execute first sequence
            List<Move> sequence = sequences.get(0);
            GameState testState = gameState.copy();
            testState.setWhiteTurn(true);

            List<Integer> remainingDice = new ArrayList<>();
            for (int d : moves) remainingDice.add(d);

            for (Move move : sequence) {
                // Verify die is available
                assertTrue(remainingDice.contains(move.getDieValue()),
                    "Die " + move.getDieValue() + " should be available");

                // Apply move
                testState.applyMove(move);

                // Remove used die
                remainingDice.remove(Integer.valueOf(move.getDieValue()));
            }

            // All dice should be used (or no more moves possible)
            if (!remainingDice.isEmpty()) {
                // Check that no more moves are possible
                assertFalse(MoveGenerator.hasAnyMove(testState,
                    remainingDice.stream().mapToInt(i -> i).toArray(), true),
                    "If dice remain, no more moves should be possible");
            }
        }

        @Test
        @DisplayName("Complete black turn with dice 3-5")
        void completeBlackTurnWith35() {
            gameState.setWhiteTurn(false);
            int[] dice = {3, 5};
            int[] moves = Dice.getMovesFromRoll(dice[0], dice[1]);

            List<List<Move>> sequences = MoveGenerator.generateAllMoveSequences(gameState, moves, false);

            assertFalse(sequences.isEmpty(), "Should have at least one sequence");

            List<Move> sequence = sequences.get(0);
            GameState testState = gameState.copy();
            testState.setWhiteTurn(false);

            for (Move move : sequence) {
                // Verify direction for black (from -> to should be increasing)
                if (!move.isFromBar() && !move.isBearOff()) {
                    assertTrue(move.getTo() > move.getFrom(),
                        "Black move should go to higher index: " + move);
                }
                testState.applyMove(move);
            }
        }

        @RepeatedTest(20)
        @DisplayName("Random dice roll produces valid game progression")
        void randomDiceRollProducesValidProgression() {
            int[] diceRoll = Dice.roll();
            int[] moves = Dice.getMovesFromRoll(diceRoll[0], diceRoll[1]);

            boolean isWhite = true;
            gameState.setWhiteTurn(isWhite);

            Player player = new RandomPlayer();
            List<Move> selectedMoves = player.selectMoves(gameState, moves);

            // Verify moves and apply them
            GameState testState = gameState.copy();
            testState.setWhiteTurn(isWhite);

            for (Move move : selectedMoves) {
                // Verify move is within bounds
                if (!move.isFromBar()) {
                    assertTrue(move.getFrom() >= 0 && move.getFrom() < 24,
                        "From point should be valid: " + move.getFrom());
                }
                if (!move.isBearOff()) {
                    assertTrue(move.getTo() >= 0 && move.getTo() < 24,
                        "To point should be valid: " + move.getTo());
                }

                // Verify die value matches roll
                boolean validDie = false;
                for (int d : moves) {
                    if (d == move.getDieValue()) {
                        validDie = true;
                        break;
                    }
                }
                assertTrue(validDie, "Die value " + move.getDieValue() +
                    " should be from roll [" + diceRoll[0] + "," + diceRoll[1] + "]");

                testState.applyMove(move);
            }
        }
    }

    @Nested
    @DisplayName("Bug Reproduction Tests")
    class BugReproductionTests {

        @Test
        @DisplayName("Verify dice values match move distances for white")
        void verifyDiceMatchMoveDistanceWhite() {
            // This test specifically checks the suspected bug area
            Player randomPlayer = new RandomPlayer();

            for (int trial = 0; trial < 50; trial++) {
                GameState testState = new GameState();
                testState.setWhiteTurn(true);

                int[] diceRoll = Dice.roll();
                int[] moves = Dice.getMovesFromRoll(diceRoll[0], diceRoll[1]);

                List<Move> selectedMoves = randomPlayer.selectMoves(testState, moves);

                for (Move move : selectedMoves) {
                    if (!move.isFromBar() && !move.isBearOff()) {
                        int actualDistance = move.getFrom() - move.getTo();
                        assertEquals(move.getDieValue(), actualDistance,
                            String.format("White move distance mismatch! " +
                                "From: %d, To: %d, Die: %d, Distance: %d, " +
                                "Dice roll: [%d, %d]",
                                move.getFrom(), move.getTo(), move.getDieValue(),
                                actualDistance, diceRoll[0], diceRoll[1]));
                    }
                }
            }
        }

        @Test
        @DisplayName("Verify dice values match move distances for black")
        void verifyDiceMatchMoveDistanceBlack() {
            Player randomPlayer = new RandomPlayer();

            for (int trial = 0; trial < 50; trial++) {
                GameState testState = new GameState();
                testState.setWhiteTurn(false);

                int[] diceRoll = Dice.roll();
                int[] moves = Dice.getMovesFromRoll(diceRoll[0], diceRoll[1]);

                List<Move> selectedMoves = randomPlayer.selectMoves(testState, moves);

                for (Move move : selectedMoves) {
                    if (!move.isFromBar() && !move.isBearOff()) {
                        int actualDistance = move.getTo() - move.getFrom();
                        assertEquals(move.getDieValue(), actualDistance,
                            String.format("Black move distance mismatch! " +
                                "From: %d, To: %d, Die: %d, Distance: %d, " +
                                "Dice roll: [%d, %d]",
                                move.getFrom(), move.getTo(), move.getDieValue(),
                                actualDistance, diceRoll[0], diceRoll[1]));
                    }
                }
            }
        }

        @Test
        @DisplayName("Test Main.onMoveExecuted die calculation logic")
        void testMainOnMoveExecutedLogic() {
            // Simulate the exact logic from Main.onMoveExecuted

            // Test case: White at 18, dice [3, 5], user drags to 13
            int fromPoint = 18;
            int toPoint = 13;
            boolean isWhiteTurn = true;
            List<Integer> availableMoves = new ArrayList<>();
            availableMoves.add(3);
            availableMoves.add(5);

            // Calculate die used (from Main.java lines 524-532)
            int dieUsed;
            if (isWhiteTurn) {
                dieUsed = fromPoint - toPoint;  // 18 - 13 = 5
            } else {
                dieUsed = toPoint - fromPoint;
            }

            assertEquals(5, dieUsed, "Die should be 5 for move 18->13");
            assertTrue(availableMoves.contains(dieUsed), "Die 5 should be available");


            // Test case: White at 18, dice [3, 5], user drags to 15
            toPoint = 15;
            if (isWhiteTurn) {
                dieUsed = fromPoint - toPoint;  // 18 - 15 = 3
            } else {
                dieUsed = toPoint - fromPoint;
            }

            assertEquals(3, dieUsed, "Die should be 3 for move 18->15");
            assertTrue(availableMoves.contains(dieUsed), "Die 3 should be available");
        }

        @Test
        @DisplayName("Test doubles handling in AI turn")
        void testDoublesHandlingInAITurn() {
            // Test that doubles (4 moves) are handled correctly
            int[] diceRoll = {4, 4};
            int[] moves = Dice.getMovesFromRoll(diceRoll[0], diceRoll[1]);

            assertEquals(4, moves.length, "Doubles should give 4 moves");

            Player randomPlayer = new RandomPlayer();
            gameState.setWhiteTurn(true);
            List<Move> selectedMoves = randomPlayer.selectMoves(gameState, moves);

            // All selected moves should use die value 4
            for (Move move : selectedMoves) {
                assertEquals(4, move.getDieValue(), "All moves should use die value 4");
            }

            // Should use up to 4 moves
            assertTrue(selectedMoves.size() <= 4, "Should use at most 4 moves for doubles");
        }
    }

    @Nested
    @DisplayName("Board State Consistency Tests")
    class BoardStateConsistencyTests {

        @Test
        @DisplayName("Total checker count remains constant after moves")
        void totalCheckerCountConstant() {
            int initialWhiteCount = countWhiteCheckers(gameState);
            int initialBlackCount = countBlackCheckers(gameState);

            assertEquals(15, initialWhiteCount, "Should start with 15 white checkers");
            assertEquals(15, initialBlackCount, "Should start with 15 black checkers");

            // Play several turns
            for (int turn = 0; turn < 10; turn++) {
                boolean isWhite = (turn % 2 == 0);
                gameState.setWhiteTurn(isWhite);

                int[] diceRoll = Dice.roll();
                int[] moves = Dice.getMovesFromRoll(diceRoll[0], diceRoll[1]);

                Player player = new RandomPlayer();
                List<Move> selectedMoves = player.selectMoves(gameState, moves);

                for (Move move : selectedMoves) {
                    gameState.applyMove(move);
                }

                // Verify counts after each turn
                int whiteCount = countWhiteCheckers(gameState);
                int blackCount = countBlackCheckers(gameState);

                assertEquals(15, whiteCount,
                    "White checker count should remain 15 after turn " + turn);
                assertEquals(15, blackCount,
                    "Black checker count should remain 15 after turn " + turn);
            }
        }

        private int countWhiteCheckers(GameState state) {
            int count = state.getWhiteBar();
            count += state.getWhiteBearOff();
            for (int i = 0; i < 24; i++) {
                int val = state.getPoint(i);
                if (val > 0) count += val;
            }
            return count;
        }

        private int countBlackCheckers(GameState state) {
            int count = state.getBlackBar();
            count += state.getBlackBearOff();
            for (int i = 0; i < 24; i++) {
                int val = state.getPoint(i);
                if (val < 0) count += Math.abs(val);
            }
            return count;
        }
    }
}
