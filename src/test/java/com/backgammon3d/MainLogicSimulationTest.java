package com.backgammon3d;

import com.backgammon3d.ai.Player;
import com.backgammon3d.ai.RandomPlayer;
import com.backgammon3d.ai.TDPlayer;
import com.backgammon3d.model.Dice;
import com.backgammon3d.model.GameState;
import com.backgammon3d.model.Move;
import com.backgammon3d.neural.TDNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that simulate the exact logic flow of Main.java without starting the GUI.
 * This helps identify issues in the game controller logic.
 */
@DisplayName("Main.java Logic Simulation Tests")
class MainLogicSimulationTest {

    // Simulated state from Main.java
    private GameState gameState;
    private int[] currentDice;
    private List<Integer> availableMoves;
    private boolean isWhiteTurn;

    @BeforeEach
    void setUp() {
        gameState = new GameState();
        currentDice = null;
        availableMoves = null;
        isWhiteTurn = true;
    }

    /**
     * Simulates Main.rollDice() logic
     */
    private void simulateRollDice() {
        currentDice = Dice.roll();
        int[] moves = Dice.getMovesFromRoll(currentDice[0], currentDice[1]);

        availableMoves = new ArrayList<>();
        for (int move : moves) {
            availableMoves.add(move);
        }
    }

    /**
     * Simulates Main.executeAITurn() logic
     */
    private List<Move> simulateExecuteAITurn(Player player) {
        // Roll dice
        currentDice = Dice.roll();
        int[] moves = Dice.getMovesFromRoll(currentDice[0], currentDice[1]);

        // Initialize availableMoves
        availableMoves = new ArrayList<>();
        for (int v : moves) availableMoves.add(v);

        // Get AI moves
        gameState.setWhiteTurn(isWhiteTurn);
        return player.selectMoves(gameState, moves);
    }

    /**
     * Simulates Main.executeAIMovesWithDelay() logic for a single move
     */
    private void simulateApplyAIMove(Move move) {
        // Set whose turn it is
        gameState.setWhiteTurn(isWhiteTurn);

        // Apply the move
        gameState.applyMove(move);

        // Update availableMoves (remove used die)
        if (availableMoves == null) {
            availableMoves = new ArrayList<>();
            int[] diceValues = Dice.getMovesFromRoll(currentDice[0], currentDice[1]);
            for (int v : diceValues) availableMoves.add(v);
        }
        availableMoves.remove(Integer.valueOf(move.getDieValue()));
    }

    /**
     * Simulates Main.onMoveExecuted() logic
     */
    private void simulateOnMoveExecuted(int fromPoint, int toPoint) {
        if (availableMoves == null || availableMoves.isEmpty()) {
            return;
        }

        // Calculate the die value used (lines 524-532)
        int dieUsed;
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
                fail("Could not find valid die for move " + fromPoint + " -> " + toPoint);
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
    }

    @Nested
    @DisplayName("executeAITurn Simulation Tests")
    class ExecuteAITurnTests {

        @Test
        @DisplayName("AI turn correctly uses dice values")
        void aiTurnUsesCorrectDiceValues() {
            Player player = new RandomPlayer();
            isWhiteTurn = true;

            List<Move> aiMoves = simulateExecuteAITurn(player);

            // Track which dice values were used
            List<Integer> usedDice = new ArrayList<>();
            for (Move move : aiMoves) {
                usedDice.add(move.getDieValue());
            }

            // Verify all used dice were from the roll
            int[] diceValues = Dice.getMovesFromRoll(currentDice[0], currentDice[1]);
            List<Integer> availableDice = new ArrayList<>();
            for (int d : diceValues) availableDice.add(d);

            for (int usedDie : usedDice) {
                assertTrue(availableDice.contains(usedDie),
                    "Used die " + usedDie + " should be in available dice " + availableDice);
                availableDice.remove(Integer.valueOf(usedDie));
            }
        }

        @RepeatedTest(50)
        @DisplayName("50 random AI turns all produce valid moves")
        void manyRandomAITurnsValid() {
            Player player = new RandomPlayer();
            isWhiteTurn = Math.random() > 0.5;

            List<Move> aiMoves = simulateExecuteAITurn(player);

            // Verify each move
            for (Move move : aiMoves) {
                // Die value should be 1-6
                assertTrue(move.getDieValue() >= 1 && move.getDieValue() <= 6,
                    "Die value should be 1-6: " + move.getDieValue());

                // Die value should be from the roll
                assertTrue(move.getDieValue() == currentDice[0] || move.getDieValue() == currentDice[1],
                    String.format("Die value %d should match roll [%d, %d]",
                        move.getDieValue(), currentDice[0], currentDice[1]));

                // Move distance should match die value (for normal moves)
                if (!move.isFromBar() && !move.isBearOff()) {
                    int distance = isWhiteTurn ?
                        (move.getFrom() - move.getTo()) :
                        (move.getTo() - move.getFrom());

                    assertEquals(move.getDieValue(), distance,
                        String.format("Move distance %d should match die %d for %s (%d -> %d)",
                            distance, move.getDieValue(),
                            isWhiteTurn ? "white" : "black",
                            move.getFrom(), move.getTo()));
                }
            }
        }
    }

    @Nested
    @DisplayName("executeAIMovesWithDelay Simulation Tests")
    class ExecuteAIMovesWithDelayTests {

        @Test
        @DisplayName("AI moves applied in sequence update state correctly")
        void aiMovesAppliedInSequence() {
            Player player = new RandomPlayer();
            isWhiteTurn = true;

            // Get AI moves
            List<Move> aiMoves = simulateExecuteAITurn(player);

            // Record initial state
            int[] initialPoints = Arrays.copyOf(gameState.getPoints(), 24);

            // Apply each move
            for (Move move : aiMoves) {
                simulateApplyAIMove(move);
            }

            // Verify state changed
            boolean stateChanged = false;
            int[] finalPoints = gameState.getPoints();
            for (int i = 0; i < 24; i++) {
                if (initialPoints[i] != finalPoints[i]) {
                    stateChanged = true;
                    break;
                }
            }

            if (!aiMoves.isEmpty()) {
                assertTrue(stateChanged, "Board state should change after AI moves");
            }
        }

        @Test
        @DisplayName("availableMoves correctly decremented after each AI move")
        void availableMovesCorrectlyDecremented() {
            isWhiteTurn = true;

            // Set up specific dice
            currentDice = new int[]{3, 5};
            int[] moves = Dice.getMovesFromRoll(currentDice[0], currentDice[1]);
            availableMoves = new ArrayList<>();
            for (int m : moves) availableMoves.add(m);

            assertEquals(2, availableMoves.size(), "Should start with 2 moves");

            // Get AI moves
            Player player = new RandomPlayer();
            gameState.setWhiteTurn(isWhiteTurn);
            List<Move> aiMoves = player.selectMoves(gameState, moves);

            // Apply each move and verify availableMoves
            for (Move move : aiMoves) {
                int sizeBefore = availableMoves.size();
                simulateApplyAIMove(move);
                int sizeAfter = availableMoves.size();

                assertEquals(sizeBefore - 1, sizeAfter,
                    "availableMoves should decrease by 1 after each move");
            }
        }
    }

    @Nested
    @DisplayName("onMoveExecuted Simulation Tests (Human Player)")
    class OnMoveExecutedTests {

        @Test
        @DisplayName("Human move 18->13 with dice [3,5] uses die 5")
        void humanMove18to13UsesDie5() {
            isWhiteTurn = true;
            currentDice = new int[]{3, 5};
            availableMoves = new ArrayList<>(Arrays.asList(3, 5));

            simulateOnMoveExecuted(18, 13);

            // Die 5 should be removed
            assertEquals(1, availableMoves.size());
            assertTrue(availableMoves.contains(3), "Die 3 should remain");
            assertFalse(availableMoves.contains(5), "Die 5 should be used");
        }

        @Test
        @DisplayName("Human move 18->15 with dice [3,5] uses die 3")
        void humanMove18to15UsesDie3() {
            isWhiteTurn = true;
            currentDice = new int[]{3, 5};
            availableMoves = new ArrayList<>(Arrays.asList(3, 5));

            simulateOnMoveExecuted(18, 15);

            assertEquals(1, availableMoves.size());
            assertTrue(availableMoves.contains(5), "Die 5 should remain");
            assertFalse(availableMoves.contains(3), "Die 3 should be used");
        }

        @Test
        @DisplayName("Black human move 5->10 with dice [3,5] uses die 5")
        void blackHumanMove5to10UsesDie5() {
            isWhiteTurn = false;
            currentDice = new int[]{3, 5};
            availableMoves = new ArrayList<>(Arrays.asList(3, 5));

            simulateOnMoveExecuted(5, 10);

            assertEquals(1, availableMoves.size());
            assertTrue(availableMoves.contains(3), "Die 3 should remain");
            assertFalse(availableMoves.contains(5), "Die 5 should be used");
        }

        @Test
        @DisplayName("Human move correctly calculates die from distance")
        void humanMoveCalculatesDieFromDistance() {
            isWhiteTurn = true;
            currentDice = new int[]{2, 6};
            availableMoves = new ArrayList<>(Arrays.asList(2, 6));

            // Move from 11 to 5 should use die 6
            // First verify this is a valid starting position
            assertTrue(gameState.getPoint(11) > 0, "White should have checkers on point 11");

            simulateOnMoveExecuted(11, 5);

            assertEquals(1, availableMoves.size());
            assertTrue(availableMoves.contains(2), "Die 2 should remain");
            assertFalse(availableMoves.contains(6), "Die 6 should be used");
        }

        @RepeatedTest(20)
        @DisplayName("Human moves with random dice always use correct die")
        void humanMovesWithRandomDice() {
            isWhiteTurn = true;
            currentDice = Dice.roll();
            int[] moves = Dice.getMovesFromRoll(currentDice[0], currentDice[1]);
            availableMoves = new ArrayList<>();
            for (int m : moves) availableMoves.add(m);

            // Find a valid move
            gameState.setWhiteTurn(true);
            Player player = new RandomPlayer();
            List<Move> validMoves = player.selectMoves(gameState, moves);

            if (!validMoves.isEmpty()) {
                Move firstMove = validMoves.get(0);
                int expectedDie = firstMove.getDieValue();

                assertTrue(availableMoves.contains(expectedDie),
                    "Expected die " + expectedDie + " should be available in " + availableMoves);

                // Simulate the human making this move
                if (!firstMove.isFromBar() && !firstMove.isBearOff()) {
                    int sizeBeforeMove = availableMoves.size();
                    simulateOnMoveExecuted(firstMove.getFrom(), firstMove.getTo());

                    assertEquals(sizeBeforeMove - 1, availableMoves.size(),
                        "Should have used one die");
                }
            }
        }
    }

    @Nested
    @DisplayName("Full Game Turn Simulation")
    class FullGameTurnSimulation {

        @Test
        @DisplayName("Complete AI vs AI game produces valid state")
        void completeAIvsAIGame() {
            Player whitePlayer = new RandomPlayer();
            Player blackPlayer = new RandomPlayer();

            int maxTurns = 200; // Limit to prevent infinite loops
            int turnCount = 0;

            while (!gameState.isGameOver() && turnCount < maxTurns) {
                isWhiteTurn = (turnCount % 2 == 0);
                Player currentPlayer = isWhiteTurn ? whitePlayer : blackPlayer;

                // Execute AI turn
                List<Move> aiMoves = simulateExecuteAITurn(currentPlayer);

                // Apply moves
                for (Move move : aiMoves) {
                    simulateApplyAIMove(move);
                }

                turnCount++;
            }

            // Verify game state is valid
            int whiteTotal = countTotalCheckers(true);
            int blackTotal = countTotalCheckers(false);

            assertEquals(15, whiteTotal, "White should have 15 checkers total");
            assertEquals(15, blackTotal, "Black should have 15 checkers total");
        }

        @Test
        @DisplayName("TD Player vs Random Player game")
        void tdPlayerVsRandomPlayer() {
            TDNetwork network = new TDNetwork();
            Player whitePlayer = new TDPlayer(network, true);
            Player blackPlayer = new RandomPlayer();

            int maxTurns = 100;
            int turnCount = 0;

            while (!gameState.isGameOver() && turnCount < maxTurns) {
                isWhiteTurn = (turnCount % 2 == 0);
                Player currentPlayer = isWhiteTurn ? whitePlayer : blackPlayer;

                List<Move> aiMoves = simulateExecuteAITurn(currentPlayer);

                for (Move move : aiMoves) {
                    // Verify move uses correct dice
                    assertTrue(move.getDieValue() == currentDice[0] || move.getDieValue() == currentDice[1],
                        "Move should use dice from roll");

                    simulateApplyAIMove(move);
                }

                turnCount++;
            }

            // Game should remain valid
            assertEquals(15, countTotalCheckers(true));
            assertEquals(15, countTotalCheckers(false));
        }

        private int countTotalCheckers(boolean isWhite) {
            int count = 0;
            for (int i = 0; i < 24; i++) {
                int val = gameState.getPoint(i);
                if (isWhite && val > 0) count += val;
                if (!isWhite && val < 0) count += Math.abs(val);
            }
            if (isWhite) {
                count += gameState.getWhiteBar();
                count += gameState.getWhiteBearOff();
            } else {
                count += gameState.getBlackBar();
                count += gameState.getBlackBearOff();
            }
            return count;
        }
    }

    @Nested
    @DisplayName("Specific Bug Scenarios")
    class SpecificBugScenarios {

        @Test
        @DisplayName("Verify die 6 moves correct distance for white")
        void verifyDie6MovesCorrectDistanceWhite() {
            isWhiteTurn = true;
            currentDice = new int[]{6, 1};
            availableMoves = new ArrayList<>(Arrays.asList(6, 1));

            // Point 11 has 5 white checkers
            // Move from 11 should go to 5 with die 6
            int fromPoint = 11;
            int expectedTo = 5; // 11 - 6 = 5

            simulateOnMoveExecuted(fromPoint, expectedTo);

            // Verify die 6 was used
            assertTrue(availableMoves.contains(1), "Die 1 should remain");
            assertFalse(availableMoves.contains(6), "Die 6 should be used");
        }

        @Test
        @DisplayName("Verify doubles (4-4) uses all four dice")
        void verifyDoublesUsesFourDice() {
            isWhiteTurn = true;
            currentDice = new int[]{4, 4};
            int[] moves = Dice.getMovesFromRoll(currentDice[0], currentDice[1]);
            availableMoves = new ArrayList<>();
            for (int m : moves) availableMoves.add(m);

            assertEquals(4, availableMoves.size(), "Should have 4 moves for doubles");

            // Get AI moves
            Player player = new RandomPlayer();
            gameState.setWhiteTurn(isWhiteTurn);
            List<Move> aiMoves = player.selectMoves(gameState, moves);

            // All moves should use die 4
            for (Move move : aiMoves) {
                assertEquals(4, move.getDieValue(), "All moves should use die 4");
            }
        }

        @Test
        @DisplayName("Hitting a blot updates bar correctly")
        void hittingBlotUpdatesBar() {
            // Set up a position where white can hit black
            gameState = new GameState();
            gameState.setPoint(13, -1); // Single black on point 14

            isWhiteTurn = true;
            currentDice = new int[]{5, 2};
            availableMoves = new ArrayList<>(Arrays.asList(5, 2));

            int initialBlackBar = gameState.getBlackBar();

            // Move from 18 to 13 (hit)
            simulateOnMoveExecuted(18, 13);

            assertEquals(initialBlackBar + 1, gameState.getBlackBar(),
                "Black bar should increase by 1 after hit");
        }

        @Test
        @DisplayName("Die 1 moves exactly 1 point")
        void die1MovesExactly1Point() {
            isWhiteTurn = true;
            currentDice = new int[]{1, 3};
            availableMoves = new ArrayList<>(Arrays.asList(1, 3));

            // Move from 0 to... wait, 0 is the lowest for white
            // Let's use point 18 -> 17 with die 1
            int fromPoint = 18;
            int toPoint = 17;

            int calculatedDie = fromPoint - toPoint; // Should be 1
            assertEquals(1, calculatedDie);

            simulateOnMoveExecuted(fromPoint, toPoint);

            // Die 1 should be used
            assertFalse(availableMoves.contains(1), "Die 1 should be used");
            assertTrue(availableMoves.contains(3), "Die 3 should remain");
        }
    }
}
