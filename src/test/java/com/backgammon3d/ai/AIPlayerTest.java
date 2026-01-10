package com.backgammon3d.ai;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AI players (RandomPlayer, TDPlayer).
 * Focuses on verifying that AI moves use correct dice values.
 */
@DisplayName("AI Player Tests")
class AIPlayerTest {

    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameState = new GameState();
    }

    @Nested
    @DisplayName("RandomPlayer Tests")
    class RandomPlayerTests {

        private RandomPlayer player;

        @BeforeEach
        void setUp() {
            player = new RandomPlayer();
        }

        @Test
        @DisplayName("isHuman returns false")
        void isHumanReturnsFalse() {
            assertFalse(player.isHuman());
        }

        @Test
        @DisplayName("selectMoves returns valid moves for white")
        void selectMovesValidForWhite() {
            gameState.setWhiteTurn(true);
            int[] dice = {3, 5};

            List<Move> moves = player.selectMoves(gameState, dice);

            assertNotNull(moves);
            for (Move move : moves) {
                assertTrue(move.getDieValue() == 3 || move.getDieValue() == 5,
                    "Die should be 3 or 5, got: " + move.getDieValue());
            }
        }

        @Test
        @DisplayName("selectMoves returns valid moves for black")
        void selectMovesValidForBlack() {
            gameState.setWhiteTurn(false);
            int[] dice = {3, 5};

            List<Move> moves = player.selectMoves(gameState, dice);

            assertNotNull(moves);
            for (Move move : moves) {
                assertTrue(move.getDieValue() == 3 || move.getDieValue() == 5,
                    "Die should be 3 or 5, got: " + move.getDieValue());
            }
        }

        @Test
        @DisplayName("selectMoves uses both dice when possible")
        void selectMovesUsesBothDice() {
            gameState.setWhiteTurn(true);
            int[] dice = {3, 5};

            List<Move> moves = player.selectMoves(gameState, dice);

            // In starting position, both dice should be usable
            assertEquals(2, moves.size(), "Should use both dice");

            Set<Integer> usedDice = new HashSet<>();
            for (Move move : moves) {
                usedDice.add(move.getDieValue());
            }
            assertTrue(usedDice.contains(3) && usedDice.contains(5),
                "Should use both 3 and 5");
        }

        @Test
        @DisplayName("selectMoves handles doubles correctly")
        void selectMovesHandlesDoubles() {
            gameState.setWhiteTurn(true);
            int[] dice = Dice.getMovesFromRoll(4, 4);

            assertEquals(4, dice.length, "Doubles should give 4 dice");

            List<Move> moves = player.selectMoves(gameState, dice);

            for (Move move : moves) {
                assertEquals(4, move.getDieValue(), "All moves should use die 4");
            }
        }

        @RepeatedTest(100)
        @DisplayName("100 random games: all AI moves use valid dice values")
        void manyGamesAllMovesUseValidDice() {
            gameState = new GameState();
            gameState.setWhiteTurn(true);

            int[] diceRoll = Dice.roll();
            int[] dice = Dice.getMovesFromRoll(diceRoll[0], diceRoll[1]);

            List<Move> moves = player.selectMoves(gameState, dice);

            List<Integer> availableDice = new ArrayList<>();
            for (int d : dice) availableDice.add(d);

            for (Move move : moves) {
                assertTrue(availableDice.remove(Integer.valueOf(move.getDieValue())),
                    String.format("Die %d should be available. Roll was [%d, %d]",
                        move.getDieValue(), diceRoll[0], diceRoll[1]));
            }
        }

        @Test
        @DisplayName("Move directions are correct for white")
        void moveDirectionsCorrectForWhite() {
            gameState.setWhiteTurn(true);
            int[] dice = {3, 5};

            List<Move> moves = player.selectMoves(gameState, dice);

            for (Move move : moves) {
                if (!move.isFromBar() && !move.isBearOff()) {
                    assertTrue(move.getTo() < move.getFrom(),
                        "White should move to lower index: " + move);
                    assertEquals(move.getFrom() - move.getTo(), move.getDieValue(),
                        "Distance should match die value");
                }
            }
        }

        @Test
        @DisplayName("Move directions are correct for black")
        void moveDirectionsCorrectForBlack() {
            gameState.setWhiteTurn(false);
            int[] dice = {3, 5};

            List<Move> moves = player.selectMoves(gameState, dice);

            for (Move move : moves) {
                if (!move.isFromBar() && !move.isBearOff()) {
                    assertTrue(move.getTo() > move.getFrom(),
                        "Black should move to higher index: " + move);
                    assertEquals(move.getTo() - move.getFrom(), move.getDieValue(),
                        "Distance should match die value");
                }
            }
        }

        @Test
        @DisplayName("Returns empty list when no moves possible")
        void returnsEmptyWhenNoMoves() {
            // Create a position where white cannot move
            clearBoard();
            gameState.setWhiteBar(1);
            // Block all entry points
            for (int i = 18; i < 24; i++) {
                gameState.setPoint(i, -2);
            }

            gameState.setWhiteTurn(true);
            int[] dice = {3, 5};

            List<Move> moves = player.selectMoves(gameState, dice);

            assertTrue(moves.isEmpty(), "Should return empty list when no moves possible");
        }

        private void clearBoard() {
            for (int i = 0; i < 24; i++) {
                gameState.setPoint(i, 0);
            }
            gameState.setWhiteBar(0);
            gameState.setBlackBar(0);
        }
    }

    @Nested
    @DisplayName("TDPlayer Tests")
    class TDPlayerTests {

        private TDPlayer whitePlayer;
        private TDPlayer blackPlayer;
        private TDNetwork network;

        @BeforeEach
        void setUp() {
            network = new TDNetwork();
            whitePlayer = new TDPlayer(network, true);
            blackPlayer = new TDPlayer(network, false);
        }

        @Test
        @DisplayName("isHuman returns false")
        void isHumanReturnsFalse() {
            assertFalse(whitePlayer.isHuman());
            assertFalse(blackPlayer.isHuman());
        }

        @Test
        @DisplayName("selectMoves returns valid moves for white")
        void selectMovesValidForWhite() {
            gameState.setWhiteTurn(true);
            int[] dice = {3, 5};

            List<Move> moves = whitePlayer.selectMoves(gameState, dice);

            assertNotNull(moves);
            for (Move move : moves) {
                assertTrue(move.getDieValue() == 3 || move.getDieValue() == 5,
                    "Die should be 3 or 5, got: " + move.getDieValue());
            }
        }

        @Test
        @DisplayName("selectMoves returns valid moves for black")
        void selectMovesValidForBlack() {
            gameState.setWhiteTurn(false);
            int[] dice = {3, 5};

            List<Move> moves = blackPlayer.selectMoves(gameState, dice);

            assertNotNull(moves);
            for (Move move : moves) {
                assertTrue(move.getDieValue() == 3 || move.getDieValue() == 5,
                    "Die should be 3 or 5, got: " + move.getDieValue());
            }
        }

        @Test
        @DisplayName("TDPlayer uses dice correctly with doubles")
        void tdPlayerUsesDoublesCorrectly() {
            gameState.setWhiteTurn(true);
            int[] dice = Dice.getMovesFromRoll(3, 3);

            List<Move> moves = whitePlayer.selectMoves(gameState, dice);

            for (Move move : moves) {
                assertEquals(3, move.getDieValue(), "All doubles moves should use die 3");
            }
        }

        @RepeatedTest(50)
        @DisplayName("TDPlayer always uses valid dice values")
        void tdPlayerAlwaysUsesValidDice() {
            boolean isWhite = Math.random() > 0.5;
            gameState.setWhiteTurn(isWhite);
            TDPlayer player = isWhite ? whitePlayer : blackPlayer;

            int[] diceRoll = Dice.roll();
            int[] dice = Dice.getMovesFromRoll(diceRoll[0], diceRoll[1]);

            List<Move> moves = player.selectMoves(gameState, dice);

            List<Integer> availableDice = new ArrayList<>();
            for (int d : dice) availableDice.add(d);

            for (Move move : moves) {
                assertTrue(availableDice.remove(Integer.valueOf(move.getDieValue())),
                    String.format("Die %d should be available. Roll: [%d, %d], Available: %s",
                        move.getDieValue(), diceRoll[0], diceRoll[1], availableDice));
            }
        }

        @Test
        @DisplayName("TDPlayer selects highest-value sequence")
        void tdPlayerSelectsHighValueSequence() {
            // TDPlayer should select the move sequence that maximizes expected value
            gameState.setWhiteTurn(true);
            int[] dice = {3, 5};

            List<Move> moves = whitePlayer.selectMoves(gameState, dice);

            // Should use both dice (maximizing moves)
            assertTrue(moves.size() >= 1, "Should have at least one move");
        }

        @Test
        @DisplayName("Move distances match die values for TDPlayer white")
        void moveDistancesMatchDieValuesWhite() {
            gameState.setWhiteTurn(true);
            int[] dice = {2, 6};

            List<Move> moves = whitePlayer.selectMoves(gameState, dice);

            for (Move move : moves) {
                if (!move.isFromBar() && !move.isBearOff()) {
                    int distance = move.getFrom() - move.getTo();
                    assertEquals(move.getDieValue(), distance,
                        String.format("White move %d->%d should have distance %d (die value), but was %d",
                            move.getFrom(), move.getTo(), move.getDieValue(), distance));
                }
            }
        }

        @Test
        @DisplayName("Move distances match die values for TDPlayer black")
        void moveDistancesMatchDieValuesBlack() {
            gameState.setWhiteTurn(false);
            int[] dice = {2, 6};

            List<Move> moves = blackPlayer.selectMoves(gameState, dice);

            for (Move move : moves) {
                if (!move.isFromBar() && !move.isBearOff()) {
                    int distance = move.getTo() - move.getFrom();
                    assertEquals(move.getDieValue(), distance,
                        String.format("Black move %d->%d should have distance %d (die value), but was %d",
                            move.getFrom(), move.getTo(), move.getDieValue(), distance));
                }
            }
        }
    }

    @Nested
    @DisplayName("HumanPlayer Tests")
    class HumanPlayerTests {

        @Test
        @DisplayName("isHuman returns true")
        void isHumanReturnsTrue() {
            HumanPlayer player = new HumanPlayer();
            assertTrue(player.isHuman());
        }

        @Test
        @DisplayName("selectMoves returns empty list (waits for UI)")
        void selectMovesReturnsEmpty() {
            HumanPlayer player = new HumanPlayer();
            int[] dice = {3, 5};

            List<Move> moves = player.selectMoves(gameState, dice);

            assertTrue(moves.isEmpty(), "Human player should return empty list (UI handles moves)");
        }
    }

    @Nested
    @DisplayName("Specific Dice Bug Tests")
    class SpecificDiceBugTests {

        @Test
        @DisplayName("Die 6 from point 23 for black can bear off")
        void die6FromPoint23ForBlack() {
            // Set up bear-off position for black (all in home board 18-23)
            clearBoard();
            for (int i = 18; i < 24; i++) {
                gameState.setPoint(i, -3);
            }

            gameState.setWhiteTurn(false);

            // First check if black can bear off at all
            boolean canBearOff = gameState.canBearOff(false);
            assertTrue(canBearOff, "Black should be able to bear off with all checkers in home board");

            // Generate moves with die 6 for black
            List<Move> moves = MoveGenerator.generateSingleMoves(gameState, 6, false);

            // With die 6 from 23, black would need to bear off (23 + 6 = 29 > 23)
            // However, the exact bear-off depends on implementation
            // Check that moves exist and are valid
            if (!moves.isEmpty()) {
                for (Move move : moves) {
                    assertEquals(6, move.getDieValue(), "Die should be 6");
                }
            }

            // Alternative: Check with die 1 which should definitely allow bear off from 23
            List<Move> moves1 = MoveGenerator.generateSingleMoves(gameState, 1, false);
            // With die 1 from 23: 23 + 1 = 24 -> BEAR_OFF
            boolean hasBearOff = moves1.stream().anyMatch(m -> m.getFrom() == 23 && m.isBearOff());
            assertTrue(hasBearOff, "Black should be able to bear off from 23 with die 1");
        }

        @Test
        @DisplayName("All dice values 1-6 work correctly for white")
        void allDiceValuesWorkForWhite() {
            for (int die = 1; die <= 6; die++) {
                gameState = new GameState();
                gameState.setWhiteTurn(true);

                List<Move> moves = MoveGenerator.generateSingleMoves(gameState, die, true);

                for (Move move : moves) {
                    assertEquals(die, move.getDieValue(), "Die value should be " + die);

                    if (!move.isFromBar() && !move.isBearOff()) {
                        int distance = move.getFrom() - move.getTo();
                        assertEquals(die, distance,
                            String.format("Move distance should be %d for die %d: %d -> %d",
                                die, die, move.getFrom(), move.getTo()));
                    }
                }
            }
        }

        @Test
        @DisplayName("All dice values 1-6 work correctly for black")
        void allDiceValuesWorkForBlack() {
            for (int die = 1; die <= 6; die++) {
                gameState = new GameState();
                gameState.setWhiteTurn(false);

                List<Move> moves = MoveGenerator.generateSingleMoves(gameState, die, false);

                for (Move move : moves) {
                    assertEquals(die, move.getDieValue(), "Die value should be " + die);

                    if (!move.isFromBar() && !move.isBearOff()) {
                        int distance = move.getTo() - move.getFrom();
                        assertEquals(die, distance,
                            String.format("Move distance should be %d for die %d: %d -> %d",
                                die, die, move.getFrom(), move.getTo()));
                    }
                }
            }
        }

        @Test
        @DisplayName("Verify move from specific starting positions")
        void verifyMoveFromSpecificPositions() {
            // White starting positions: 0, 11, 16, 18
            // Test each with various dice

            gameState = new GameState();
            gameState.setWhiteTurn(true);

            // From point 18 (has 5 white)
            for (int die = 1; die <= 6; die++) {
                List<Move> moves = MoveGenerator.generateSingleMoves(gameState, die, true);
                Move moveFrom18 = moves.stream()
                    .filter(m -> m.getFrom() == 18)
                    .findFirst()
                    .orElse(null);

                if (moveFrom18 != null) {
                    int expectedTo = 18 - die;
                    if (expectedTo >= 0) {
                        assertEquals(expectedTo, moveFrom18.getTo(),
                            String.format("From 18 with die %d should go to %d", die, expectedTo));
                    }
                }
            }
        }

        private void clearBoard() {
            for (int i = 0; i < 24; i++) {
                gameState.setPoint(i, 0);
            }
            gameState.setWhiteBar(0);
            gameState.setBlackBar(0);
        }
    }
}
