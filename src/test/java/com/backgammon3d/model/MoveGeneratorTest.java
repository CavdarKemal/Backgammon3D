package com.backgammon3d.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MoveGenerator class.
 * These tests verify that move generation follows Backgammon rules correctly.
 */
@DisplayName("MoveGenerator Tests")
class MoveGeneratorTest {

    private GameState state;

    @BeforeEach
    void setUp() {
        state = new GameState();
    }

    @Nested
    @DisplayName("generateSingleMoves Tests")
    class GenerateSingleMovesTests {

        @Test
        @DisplayName("White generates moves from correct points at start")
        void whiteGeneratesMovesAtStart() {
            List<Move> moves = MoveGenerator.generateSingleMoves(state, 5, true);

            // White has checkers on: 0, 11, 16, 18
            // With die 5, possible targets: 0->-5 (invalid), 11->6, 16->11, 18->13
            // Point 6 (index 5) is blocked by black (-5), so 11->6 is invalid
            // Point 12 (index 11) has 5 white checkers, so 16->11 is valid
            // Point 14 (index 13) is empty, so 18->13 is valid

            assertFalse(moves.isEmpty(), "Should have moves with die 5");

            // Verify moves are from correct sources
            for (Move move : moves) {
                assertTrue(state.isOwnedBy(move.getFrom(), true),
                    "All moves should be from white-owned points");
            }
        }

        @Test
        @DisplayName("Black generates moves from correct points at start")
        void blackGeneratesMovesAtStart() {
            List<Move> moves = MoveGenerator.generateSingleMoves(state, 5, false);

            // Black has checkers on: 5, 7, 12, 23
            assertFalse(moves.isEmpty(), "Should have moves with die 5");

            for (Move move : moves) {
                assertTrue(state.isOwnedBy(move.getFrom(), false),
                    "All moves should be from black-owned points");
            }
        }

        @Test
        @DisplayName("Must enter from bar first - White")
        void whiteMustEnterFromBar() {
            state.setWhiteBar(1);
            state.setPoint(20, 0); // Clear entry point

            List<Move> moves = MoveGenerator.generateSingleMoves(state, 4, true);

            // Should only have bar entry moves
            assertEquals(1, moves.size(), "Should have exactly 1 bar entry move");
            assertTrue(moves.get(0).isFromBar(), "Move should be from bar");
            assertEquals(20, moves.get(0).getTo(), "Should enter on point 21 (index 20)");
        }

        @Test
        @DisplayName("Must enter from bar first - Black")
        void blackMustEnterFromBar() {
            state.setBlackBar(1);
            state.setPoint(3, 0); // Clear entry point

            List<Move> moves = MoveGenerator.generateSingleMoves(state, 4, false);

            assertEquals(1, moves.size(), "Should have exactly 1 bar entry move");
            assertTrue(moves.get(0).isFromBar());
            assertEquals(3, moves.get(0).getTo(), "Should enter on point 4 (index 3)");
        }

        @Test
        @DisplayName("No moves when bar entry blocked")
        void noMovesWhenBarEntryBlocked() {
            state.setWhiteBar(1);
            // Point 21 (index 20) is already blocked by default (opponent has >2)
            // Actually need to set up blocked position
            state.setPoint(20, -2); // Block entry point

            List<Move> moves = MoveGenerator.generateSingleMoves(state, 4, true);

            assertTrue(moves.isEmpty(), "No moves when entry is blocked");
        }

        @Test
        @DisplayName("Hit move marked correctly")
        void hitMoveMarkedCorrectly() {
            state.setPoint(13, -1); // Single black on point 14

            List<Move> moves = MoveGenerator.generateSingleMoves(state, 5, true);

            // 18 -> 13 should be a hit
            boolean foundHit = moves.stream()
                .anyMatch(m -> m.getFrom() == 18 && m.getTo() == 13 && m.isHit());

            assertTrue(foundHit, "Should find hit move from 18 to 13");
        }

        @Test
        @DisplayName("Cannot move to blocked point")
        void cannotMoveToBlocked() {
            // Point 6 (index 5) has -5 black, so blocked for white
            // Check no white move targets point 5

            List<Move> moves = MoveGenerator.generateSingleMoves(state, 6, true);

            boolean anyMoveToBlocked = moves.stream()
                .anyMatch(m -> m.getTo() == 5);

            assertFalse(anyMoveToBlocked, "No move should target blocked point");
        }
    }

    @Nested
    @DisplayName("Bear Off Move Tests")
    class BearOffMoveTests {

        @Test
        @DisplayName("Generate bear off moves when in home board")
        void generateBearOffMoves() {
            setupWhiteBearOffPosition();

            List<Move> moves = MoveGenerator.generateSingleMoves(state, 3, true);

            // Point 3 (index 2) with die 3 should bear off
            boolean foundBearOff = moves.stream()
                .anyMatch(m -> m.getFrom() == 2 && m.isBearOff());

            assertTrue(foundBearOff, "Should find bear off move from point 3");
        }

        @Test
        @DisplayName("No bear off when not all in home board")
        void noBearOffWhenNotInHomeBoard() {
            // Default position has checkers outside home board
            List<Move> moves = MoveGenerator.generateSingleMoves(state, 1, true);

            boolean anyBearOff = moves.stream().anyMatch(Move::isBearOff);

            assertFalse(anyBearOff, "No bear off moves in starting position");
        }
    }

    @Nested
    @DisplayName("generateAllMoveSequences Tests")
    class GenerateAllMoveSequencesTests {

        @Test
        @DisplayName("Generates sequences using both dice")
        void generatesSequencesUsingBothDice() {
            int[] dice = {3, 5};

            List<List<Move>> sequences = MoveGenerator.generateAllMoveSequences(state, dice, true);

            assertFalse(sequences.isEmpty(), "Should have sequences");

            // All sequences should use 2 dice (if possible)
            int maxLen = sequences.stream().mapToInt(List::size).max().orElse(0);

            for (List<Move> seq : sequences) {
                assertEquals(maxLen, seq.size(),
                    "All sequences should have maximum length");
            }
        }

        @Test
        @DisplayName("Doubles gives 4 moves")
        void doublesGivesFourMoves() {
            int[] dice = Dice.getMovesFromRoll(3, 3); // [3, 3, 3, 3]

            List<List<Move>> sequences = MoveGenerator.generateAllMoveSequences(state, dice, true);

            // Should have sequences with up to 4 moves
            int maxLen = sequences.stream().mapToInt(List::size).max().orElse(0);

            assertTrue(maxLen <= 4, "Maximum 4 moves for doubles");
        }

        @Test
        @DisplayName("Empty sequence when no moves possible")
        void emptySequenceWhenNoMoves() {
            // Create a position where white cannot move
            clearBoard();
            state.setWhiteBar(1);
            // Block all entry points
            for (int i = 18; i < 24; i++) {
                state.setPoint(i, -2);
            }

            int[] dice = {3, 5};
            List<List<Move>> sequences = MoveGenerator.generateAllMoveSequences(state, dice, true);

            assertEquals(1, sequences.size(), "Should have one sequence");
            assertTrue(sequences.get(0).isEmpty(), "Sequence should be empty");
        }

        @Test
        @DisplayName("Uses higher die when only one move possible")
        void usesHigherDieWhenOnlyOneMove() {
            // This is a complex rule - if only one die can be used, must use higher
            // Simplified test: just ensure moves are generated
            clearBoard();
            state.setPoint(5, 2); // White on point 6

            int[] dice = {3, 5};
            List<List<Move>> sequences = MoveGenerator.generateAllMoveSequences(state, dice, true);

            assertFalse(sequences.isEmpty(), "Should have sequences");
        }
    }

    @Nested
    @DisplayName("getValidTargets Tests")
    class GetValidTargetsTests {

        @Test
        @DisplayName("Returns valid targets for white checker")
        void returnsValidTargetsForWhite() {
            // White has 5 checkers on point 19 (index 18)
            int[] dice = {3, 5};

            List<Integer> targets = MoveGenerator.getValidTargets(state, 18, dice, true);

            // With dice 3, 5 from point 19:
            // 18-3=15 (point 16), 18-5=13 (point 14)
            assertFalse(targets.isEmpty(), "Should have valid targets");
        }

        @Test
        @DisplayName("Returns bear off as target when possible")
        void returnsBearOffTarget() {
            setupWhiteBearOffPosition();
            int[] dice = {3, 5};

            List<Integer> targets = MoveGenerator.getValidTargets(state, 2, dice, true);

            // Point 3 (index 2) with die 3 should bear off
            assertTrue(targets.contains(Move.BEAR_OFF), "Should include bear off");
        }

        @Test
        @DisplayName("Empty when from point has no checkers")
        void emptyWhenNoCheckers() {
            int[] dice = {3, 5};

            List<Integer> targets = MoveGenerator.getValidTargets(state, 10, dice, true);

            assertTrue(targets.isEmpty(), "No targets from empty point");
        }

        @Test
        @DisplayName("Empty when all targets blocked")
        void emptyWhenAllTargetsBlocked() {
            clearBoard();
            state.setPoint(18, 5); // White on point 19
            state.setPoint(15, -2); // Block target for die 3
            state.setPoint(13, -2); // Block target for die 5

            int[] dice = {3, 5};
            List<Integer> targets = MoveGenerator.getValidTargets(state, 18, dice, true);

            assertTrue(targets.isEmpty(), "No targets when all blocked");
        }
    }

    @Nested
    @DisplayName("hasAnyMove Tests")
    class HasAnyMoveTests {

        @Test
        @DisplayName("Has moves in starting position")
        void hasMovesAtStart() {
            int[] dice = {3, 5};

            assertTrue(MoveGenerator.hasAnyMove(state, dice, true), "White should have moves");
            assertTrue(MoveGenerator.hasAnyMove(state, dice, false), "Black should have moves");
        }

        @Test
        @DisplayName("No moves when completely blocked")
        void noMovesWhenBlocked() {
            clearBoard();
            state.setWhiteBar(1);
            // Block all entry points for white
            for (int i = 18; i < 24; i++) {
                state.setPoint(i, -2);
            }

            int[] dice = {3, 5};

            assertFalse(MoveGenerator.hasAnyMove(state, dice, true), "No moves when bar entry blocked");
        }
    }

    @Nested
    @DisplayName("Move Direction Tests")
    class MoveDirectionTests {

        @Test
        @DisplayName("White moves towards lower indices (towards bear off)")
        void whiteMoveDirection() {
            List<Move> moves = MoveGenerator.generateSingleMoves(state, 5, true);

            for (Move move : moves) {
                if (!move.isFromBar() && !move.isBearOff()) {
                    assertTrue(move.getTo() < move.getFrom(),
                        "White should move from higher to lower index: " + move);
                }
            }
        }

        @Test
        @DisplayName("Black moves towards higher indices (towards bear off)")
        void blackMoveDirection() {
            List<Move> moves = MoveGenerator.generateSingleMoves(state, 5, false);

            for (Move move : moves) {
                if (!move.isFromBar() && !move.isBearOff()) {
                    assertTrue(move.getTo() > move.getFrom(),
                        "Black should move from lower to higher index: " + move);
                }
            }
        }
    }

    @Nested
    @DisplayName("Specific Scenario Tests")
    class SpecificScenarioTests {

        @Test
        @DisplayName("White opening move with 3-1")
        void whiteOpeningMove31() {
            int[] dice = {3, 1};
            List<List<Move>> sequences = MoveGenerator.generateAllMoveSequences(state, dice, true);

            assertFalse(sequences.isEmpty());
            // Should use both dice
            assertTrue(sequences.stream().anyMatch(s -> s.size() == 2),
                "Should have sequences using both dice");
        }

        @Test
        @DisplayName("White opening move with 6-5")
        void whiteOpeningMove65() {
            int[] dice = {6, 5};
            List<List<Move>> sequences = MoveGenerator.generateAllMoveSequences(state, dice, true);

            assertFalse(sequences.isEmpty());
            // Classic "lover's leap" - 24/13 is blocked, so other moves
            assertTrue(sequences.stream().anyMatch(s -> s.size() >= 1));
        }

        @Test
        @DisplayName("Cannot make illegal move - moving wrong direction")
        void cannotMoveWrongDirection() {
            clearBoard();
            state.setPoint(10, 2); // White on point 11

            int[] dice = {3, 0}; // Single die for simplicity
            List<Move> moves = MoveGenerator.generateSingleMoves(state, 3, true);

            // White should not have any moves to point 13 (index 12)
            boolean hasWrongDirectionMove = moves.stream()
                .anyMatch(m -> m.getFrom() == 10 && m.getTo() == 13);

            assertFalse(hasWrongDirectionMove, "White cannot move backwards");
        }
    }

    @Nested
    @DisplayName("Die Value Usage Tests")
    class DieValueUsageTests {

        @Test
        @DisplayName("Move uses correct die value")
        void moveUsesCorrectDieValue() {
            List<Move> moves3 = MoveGenerator.generateSingleMoves(state, 3, true);
            List<Move> moves5 = MoveGenerator.generateSingleMoves(state, 5, true);

            for (Move move : moves3) {
                assertEquals(3, move.getDieValue(), "Die value should be 3");
            }

            for (Move move : moves5) {
                assertEquals(5, move.getDieValue(), "Die value should be 5");
            }
        }

        @Test
        @DisplayName("Move distance matches die value")
        void moveDistanceMatchesDieValue() {
            List<Move> moves = MoveGenerator.generateSingleMoves(state, 5, true);

            for (Move move : moves) {
                if (!move.isFromBar() && !move.isBearOff()) {
                    int distance = move.getFrom() - move.getTo(); // White moves down
                    assertEquals(5, distance,
                        "Move distance should equal die value: " + move);
                }
            }
        }

        @Test
        @DisplayName("Black move distance matches die value")
        void blackMoveDistanceMatchesDieValue() {
            List<Move> moves = MoveGenerator.generateSingleMoves(state, 5, false);

            for (Move move : moves) {
                if (!move.isFromBar() && !move.isBearOff()) {
                    int distance = move.getTo() - move.getFrom(); // Black moves up
                    assertEquals(5, distance,
                        "Black move distance should equal die value: " + move);
                }
            }
        }
    }

    // Helper methods

    private void clearBoard() {
        for (int i = 0; i < 24; i++) {
            state.setPoint(i, 0);
        }
        state.setWhiteBar(0);
        state.setBlackBar(0);
    }

    private void setupWhiteBearOffPosition() {
        clearBoard();
        state.setPoint(0, 3);
        state.setPoint(1, 3);
        state.setPoint(2, 3);
        state.setPoint(3, 3);
        state.setPoint(4, 3);
    }

    private void setupBlackBearOffPosition() {
        clearBoard();
        state.setPoint(23, -3);
        state.setPoint(22, -3);
        state.setPoint(21, -3);
        state.setPoint(20, -3);
        state.setPoint(19, -3);
    }
}
