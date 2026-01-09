package com.backgammon3d.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GameState class.
 */
@DisplayName("GameState Tests")
class GameStateTest {

    private GameState state;

    @BeforeEach
    void setUp() {
        state = new GameState();
    }

    @Nested
    @DisplayName("Initial Position Tests")
    class InitialPositionTests {

        @Test
        @DisplayName("White has 2 checkers on point 1 (index 0)")
        void whiteHasTwoOnPoint1() {
            assertEquals(2, state.getPoint(0), "Point 1 should have 2 white checkers");
        }

        @Test
        @DisplayName("White has 5 checkers on point 12 (index 11)")
        void whiteHasFiveOnPoint12() {
            assertEquals(5, state.getPoint(11), "Point 12 should have 5 white checkers");
        }

        @Test
        @DisplayName("White has 3 checkers on point 17 (index 16)")
        void whiteHasThreeOnPoint17() {
            assertEquals(3, state.getPoint(16), "Point 17 should have 3 white checkers");
        }

        @Test
        @DisplayName("White has 5 checkers on point 19 (index 18)")
        void whiteHasFiveOnPoint19() {
            assertEquals(5, state.getPoint(18), "Point 19 should have 5 white checkers");
        }

        @Test
        @DisplayName("Black has 2 checkers on point 24 (index 23)")
        void blackHasTwoOnPoint24() {
            assertEquals(-2, state.getPoint(23), "Point 24 should have 2 black checkers");
        }

        @Test
        @DisplayName("Black has 5 checkers on point 13 (index 12)")
        void blackHasFiveOnPoint13() {
            assertEquals(-5, state.getPoint(12), "Point 13 should have 5 black checkers");
        }

        @Test
        @DisplayName("Black has 3 checkers on point 8 (index 7)")
        void blackHasThreeOnPoint8() {
            assertEquals(-3, state.getPoint(7), "Point 8 should have 3 black checkers");
        }

        @Test
        @DisplayName("Black has 5 checkers on point 6 (index 5)")
        void blackHasFiveOnPoint6() {
            assertEquals(-5, state.getPoint(5), "Point 6 should have 5 black checkers");
        }

        @Test
        @DisplayName("White starts the game")
        void whiteStarts() {
            assertTrue(state.isWhiteTurn(), "White should start the game");
        }

        @Test
        @DisplayName("Bar is empty at start")
        void barIsEmpty() {
            assertEquals(0, state.getWhiteBar(), "White bar should be empty");
            assertEquals(0, state.getBlackBar(), "Black bar should be empty");
        }

        @Test
        @DisplayName("Bear off is empty at start")
        void bearOffIsEmpty() {
            assertEquals(0, state.getWhiteBearOff(), "White bear off should be empty");
            assertEquals(0, state.getBlackBearOff(), "Black bear off should be empty");
        }

        @Test
        @DisplayName("Total white checkers is 15")
        void totalWhiteCheckers() {
            int total = 0;
            for (int i = 0; i < 24; i++) {
                int val = state.getPoint(i);
                if (val > 0) total += val;
            }
            assertEquals(15, total, "White should have 15 checkers total");
        }

        @Test
        @DisplayName("Total black checkers is 15")
        void totalBlackCheckers() {
            int total = 0;
            for (int i = 0; i < 24; i++) {
                int val = state.getPoint(i);
                if (val < 0) total += -val;
            }
            assertEquals(15, total, "Black should have 15 checkers total");
        }
    }

    @Nested
    @DisplayName("Copy Tests")
    class CopyTests {

        @Test
        @DisplayName("Copy creates independent state")
        void copyIsIndependent() {
            GameState copy = state.copy();
            copy.setPoint(0, 10);
            assertEquals(2, state.getPoint(0), "Original should be unchanged");
            assertEquals(10, copy.getPoint(0), "Copy should have new value");
        }

        @Test
        @DisplayName("Copy preserves all values")
        void copyPreservesValues() {
            state.setWhiteBar(2);
            state.setBlackBar(1);
            state.setWhiteBearOff(3);
            state.setBlackBearOff(4);
            state.setWhiteTurn(false);

            GameState copy = state.copy();

            assertEquals(2, copy.getWhiteBar());
            assertEquals(1, copy.getBlackBar());
            assertEquals(3, copy.getWhiteBearOff());
            assertEquals(4, copy.getBlackBearOff());
            assertFalse(copy.isWhiteTurn());
        }
    }

    @Nested
    @DisplayName("Apply Move Tests")
    class ApplyMoveTests {

        @Test
        @DisplayName("White simple move reduces source")
        void whiteSimpleMoveReducesSource() {
            state.setWhiteTurn(true);
            Move move = new Move(18, 13, 5); // Point 19 to Point 14

            int before = state.getPoint(18);
            state.applyMove(move);

            assertEquals(before - 1, state.getPoint(18), "Source should have one less checker");
        }

        @Test
        @DisplayName("White simple move increases destination")
        void whiteSimpleMoveIncreasesDestination() {
            state.setWhiteTurn(true);
            Move move = new Move(18, 13, 5); // Point 19 to Point 14

            int before = state.getPoint(13);
            state.applyMove(move);

            assertEquals(before + 1, state.getPoint(13), "Destination should have one more checker");
        }

        @Test
        @DisplayName("Black simple move reduces source")
        void blackSimpleMoveReducesSource() {
            state.setWhiteTurn(false);
            Move move = new Move(5, 10, 5); // Point 6 to Point 11

            int before = state.getPoint(5);
            state.applyMove(move);

            assertEquals(before + 1, state.getPoint(5), "Source should have one less black checker (value closer to 0)");
        }

        @Test
        @DisplayName("White hit move sends opponent to bar")
        void whiteHitMove() {
            // Set up a blot
            state.setPoint(13, -1); // Single black checker on point 14
            state.setWhiteTurn(true);
            Move move = new Move(18, 13, 5, true); // Hit!

            state.applyMove(move);

            assertEquals(1, state.getPoint(13), "Point should now have white checker");
            assertEquals(1, state.getBlackBar(), "Black should have one on bar");
        }

        @Test
        @DisplayName("Black hit move sends opponent to bar")
        void blackHitMove() {
            // Set up a blot
            state.setPoint(10, 1); // Single white checker on point 11
            state.setWhiteTurn(false);
            Move move = new Move(5, 10, 5, true); // Hit!

            state.applyMove(move);

            assertEquals(-1, state.getPoint(10), "Point should now have black checker");
            assertEquals(1, state.getWhiteBar(), "White should have one on bar");
        }

        @Test
        @DisplayName("White bar entry move")
        void whiteBarEntry() {
            state.setWhiteBar(1);
            state.setPoint(20, 0); // Clear point 21 for entry
            state.setWhiteTurn(true);
            Move move = Move.fromBar(20, 4, false); // Enter on point 21 (index 20) with die 4

            state.applyMove(move);

            assertEquals(0, state.getWhiteBar(), "Bar should be empty");
            assertEquals(1, state.getPoint(20), "Point 21 should have checker");
        }

        @Test
        @DisplayName("Black bar entry move")
        void blackBarEntry() {
            state.setBlackBar(1);
            state.setPoint(3, 0); // Clear point 4 for entry
            state.setWhiteTurn(false);
            Move move = Move.fromBar(3, 4, false); // Enter on point 4 (index 3) with die 4

            state.applyMove(move);

            assertEquals(0, state.getBlackBar(), "Bar should be empty");
            assertEquals(-1, state.getPoint(3), "Point 4 should have black checker");
        }

        @Test
        @DisplayName("White bear off move")
        void whiteBearOff() {
            // Set up bear off position
            setupWhiteBearOffPosition();
            state.setWhiteTurn(true);
            Move move = Move.bearOff(0, 1); // Bear off from point 1 with die 1

            state.applyMove(move);

            assertEquals(1, state.getWhiteBearOff(), "White should have 1 borne off");
        }

        @Test
        @DisplayName("Black bear off move")
        void blackBearOff() {
            // Set up bear off position
            setupBlackBearOffPosition();
            state.setWhiteTurn(false);
            Move move = Move.bearOff(23, 1); // Bear off from point 24 with die 1

            state.applyMove(move);

            assertEquals(1, state.getBlackBearOff(), "Black should have 1 borne off");
        }
    }

    @Nested
    @DisplayName("Can Bear Off Tests")
    class CanBearOffTests {

        @Test
        @DisplayName("White cannot bear off at start")
        void whiteCannotBearOffAtStart() {
            assertFalse(state.canBearOff(true), "White cannot bear off at start");
        }

        @Test
        @DisplayName("Black cannot bear off at start")
        void blackCannotBearOffAtStart() {
            assertFalse(state.canBearOff(false), "Black cannot bear off at start");
        }

        @Test
        @DisplayName("White can bear off when all in home board")
        void whiteCanBearOff() {
            setupWhiteBearOffPosition();
            assertTrue(state.canBearOff(true), "White should be able to bear off");
        }

        @Test
        @DisplayName("Black can bear off when all in home board")
        void blackCanBearOff() {
            setupBlackBearOffPosition();
            assertTrue(state.canBearOff(false), "Black should be able to bear off");
        }

        @Test
        @DisplayName("White cannot bear off with checker on bar")
        void whiteCannotBearOffWithBar() {
            setupWhiteBearOffPosition();
            state.setWhiteBar(1);
            assertFalse(state.canBearOff(true), "White cannot bear off with checker on bar");
        }
    }

    @Nested
    @DisplayName("Game Over Tests")
    class GameOverTests {

        @Test
        @DisplayName("Game not over at start")
        void gameNotOverAtStart() {
            assertFalse(state.isGameOver(), "Game should not be over at start");
        }

        @Test
        @DisplayName("Game over when white bears off all")
        void gameOverWhiteWins() {
            state.setWhiteBearOff(15);
            assertTrue(state.isGameOver(), "Game should be over");
            assertTrue(state.getWinner(), "White should win");
        }

        @Test
        @DisplayName("Game over when black bears off all")
        void gameOverBlackWins() {
            state.setBlackBearOff(15);
            assertTrue(state.isGameOver(), "Game should be over");
            assertFalse(state.getWinner(), "Black should win");
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("getCheckerCount returns correct count for white")
        void getCheckerCountWhite() {
            assertEquals(5, state.getCheckerCount(18, true), "Point 19 should have 5 white");
            assertEquals(0, state.getCheckerCount(5, true), "Point 6 should have 0 white");
        }

        @Test
        @DisplayName("getCheckerCount returns correct count for black")
        void getCheckerCountBlack() {
            assertEquals(5, state.getCheckerCount(5, false), "Point 6 should have 5 black");
            assertEquals(0, state.getCheckerCount(18, false), "Point 19 should have 0 black");
        }

        @Test
        @DisplayName("isOwnedBy returns correct ownership")
        void isOwnedBy() {
            assertTrue(state.isOwnedBy(0, true), "Point 1 owned by white");
            assertFalse(state.isOwnedBy(0, false), "Point 1 not owned by black");
            assertTrue(state.isOwnedBy(5, false), "Point 6 owned by black");
            assertFalse(state.isOwnedBy(5, true), "Point 6 not owned by white");
        }

        @Test
        @DisplayName("isBlocked returns true for 2+ opponent checkers")
        void isBlocked() {
            assertTrue(state.isBlocked(5, true), "Point 6 blocked for white (5 black)");
            assertTrue(state.isBlocked(0, false), "Point 1 blocked for black (2 white)");
            assertFalse(state.isBlocked(0, true), "Point 1 not blocked for white");
        }

        @Test
        @DisplayName("hasBlot returns true for single opponent checker")
        void hasBlot() {
            state.setPoint(10, -1); // Single black checker
            assertTrue(state.hasBlot(10, true), "Point 11 has black blot for white");
            assertFalse(state.hasBlot(10, false), "Point 11 has no white blot for black");

            state.setPoint(15, 1); // Single white checker
            assertTrue(state.hasBlot(15, false), "Point 16 has white blot for black");
        }
    }

    // Helper methods to set up test positions

    private void setupWhiteBearOffPosition() {
        for (int i = 0; i < 24; i++) {
            state.setPoint(i, 0);
        }
        state.setPoint(0, 3);  // 3 on point 1
        state.setPoint(1, 3);  // 3 on point 2
        state.setPoint(2, 3);  // 3 on point 3
        state.setPoint(3, 3);  // 3 on point 4
        state.setPoint(4, 3);  // 3 on point 5
        state.setWhiteBar(0);
    }

    private void setupBlackBearOffPosition() {
        for (int i = 0; i < 24; i++) {
            state.setPoint(i, 0);
        }
        state.setPoint(23, -3); // 3 on point 24
        state.setPoint(22, -3); // 3 on point 23
        state.setPoint(21, -3); // 3 on point 22
        state.setPoint(20, -3); // 3 on point 21
        state.setPoint(19, -3); // 3 on point 20
        state.setBlackBar(0);
    }
}
