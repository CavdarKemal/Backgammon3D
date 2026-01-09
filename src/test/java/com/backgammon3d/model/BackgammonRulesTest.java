package com.backgammon3d.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BackgammonRules class.
 */
@DisplayName("BackgammonRules Tests")
class BackgammonRulesTest {

    private GameState state;

    @BeforeEach
    void setUp() {
        state = new GameState();
    }

    @Nested
    @DisplayName("Calculate Target Tests")
    class CalculateTargetTests {

        @Nested
        @DisplayName("White Movement")
        class WhiteMovementTests {

            @Test
            @DisplayName("White moves from high to low index")
            void whiteMoveDirection() {
                // White moves from point 19 (index 18) with die 5
                // Should move to point 14 (index 13)
                int target = BackgammonRules.calculateTarget(18, 5, true);
                assertEquals(13, target, "White should move from 18 to 13");
            }

            @Test
            @DisplayName("White moves to bear off from home board")
            void whiteBearOff() {
                // White on point 3 (index 2) with die 3 should bear off
                int target = BackgammonRules.calculateTarget(2, 3, true);
                assertEquals(Move.BEAR_OFF, target, "Should be bear off");
            }

            @Test
            @DisplayName("White moves to bear off with higher die")
            void whiteBearOffHigherDie() {
                // White on point 2 (index 1) with die 6 should bear off
                int target = BackgammonRules.calculateTarget(1, 6, true);
                assertEquals(Move.BEAR_OFF, target, "Should be bear off with higher die");
            }

            @Test
            @DisplayName("White bar entry")
            void whiteBarEntry() {
                // White enters from bar with die 4 -> point 21 (index 20)
                int target = BackgammonRules.calculateTarget(Move.BAR, 4, true);
                assertEquals(20, target, "White should enter on point 21 (index 20)");
            }

            @Test
            @DisplayName("White bar entry with die 1")
            void whiteBarEntryDie1() {
                // White enters from bar with die 1 -> point 24 (index 23)
                int target = BackgammonRules.calculateTarget(Move.BAR, 1, true);
                assertEquals(23, target, "White should enter on point 24 (index 23)");
            }

            @Test
            @DisplayName("White bar entry with die 6")
            void whiteBarEntryDie6() {
                // White enters from bar with die 6 -> point 19 (index 18)
                int target = BackgammonRules.calculateTarget(Move.BAR, 6, true);
                assertEquals(18, target, "White should enter on point 19 (index 18)");
            }
        }

        @Nested
        @DisplayName("Black Movement")
        class BlackMovementTests {

            @Test
            @DisplayName("Black moves from low to high index")
            void blackMoveDirection() {
                // Black moves from point 6 (index 5) with die 5
                // Should move to point 11 (index 10)
                int target = BackgammonRules.calculateTarget(5, 5, false);
                assertEquals(10, target, "Black should move from 5 to 10");
            }

            @Test
            @DisplayName("Black moves to bear off from home board")
            void blackBearOff() {
                // Black on point 22 (index 21) with die 3 should bear off
                int target = BackgammonRules.calculateTarget(21, 3, false);
                assertEquals(Move.BEAR_OFF, target, "Should be bear off");
            }

            @Test
            @DisplayName("Black bar entry")
            void blackBarEntry() {
                // Black enters from bar with die 4 -> point 4 (index 3)
                int target = BackgammonRules.calculateTarget(Move.BAR, 4, false);
                assertEquals(3, target, "Black should enter on point 4 (index 3)");
            }

            @Test
            @DisplayName("Black bar entry with die 1")
            void blackBarEntryDie1() {
                // Black enters from bar with die 1 -> point 1 (index 0)
                int target = BackgammonRules.calculateTarget(Move.BAR, 1, false);
                assertEquals(0, target, "Black should enter on point 1 (index 0)");
            }

            @Test
            @DisplayName("Black bar entry with die 6")
            void blackBarEntryDie6() {
                // Black enters from bar with die 6 -> point 6 (index 5)
                int target = BackgammonRules.calculateTarget(Move.BAR, 6, false);
                assertEquals(5, target, "Black should enter on point 6 (index 5)");
            }
        }
    }

    @Nested
    @DisplayName("canMoveTo Tests")
    class CanMoveToTests {

        @Test
        @DisplayName("Can move to empty point")
        void canMoveToEmpty() {
            state.setPoint(10, 0);
            assertTrue(BackgammonRules.canMoveTo(state, 10, true));
            assertTrue(BackgammonRules.canMoveTo(state, 10, false));
        }

        @Test
        @DisplayName("Can move to own point")
        void canMoveToOwn() {
            state.setPoint(10, 3);  // White checkers
            assertTrue(BackgammonRules.canMoveTo(state, 10, true), "White can move to white point");

            state.setPoint(10, -3); // Black checkers
            assertTrue(BackgammonRules.canMoveTo(state, 10, false), "Black can move to black point");
        }

        @Test
        @DisplayName("Can move to single opponent (blot)")
        void canMoveToBlot() {
            state.setPoint(10, 1);  // Single white
            assertTrue(BackgammonRules.canMoveTo(state, 10, false), "Black can hit white blot");

            state.setPoint(10, -1); // Single black
            assertTrue(BackgammonRules.canMoveTo(state, 10, true), "White can hit black blot");
        }

        @Test
        @DisplayName("Cannot move to blocked point")
        void cannotMoveToBlocked() {
            state.setPoint(10, 2);  // 2+ white = blocked for black
            assertFalse(BackgammonRules.canMoveTo(state, 10, false), "Black cannot move to white blocked");

            state.setPoint(10, -2); // 2+ black = blocked for white
            assertFalse(BackgammonRules.canMoveTo(state, 10, true), "White cannot move to black blocked");
        }

        @Test
        @DisplayName("Cannot move outside board")
        void cannotMoveOutsideBoard() {
            assertFalse(BackgammonRules.canMoveTo(state, -1, true));
            assertFalse(BackgammonRules.canMoveTo(state, 24, true));
        }
    }

    @Nested
    @DisplayName("canBearOff Tests")
    class CanBearOffTests {

        @Test
        @DisplayName("Cannot bear off with checker outside home board")
        void cannotBearOffOutsideHomeBoard() {
            // Default setup has checkers everywhere
            Move move = Move.bearOff(0, 1);
            assertFalse(BackgammonRules.canBearOff(state, move, true));
        }

        @Test
        @DisplayName("White can bear off exact die")
        void whiteBearOffExact() {
            setupWhiteBearOffPosition();
            // Checker on point 3 (index 2), die 3 = exact
            Move move = Move.bearOff(2, 3);
            assertTrue(BackgammonRules.canBearOff(state, move, true));
        }

        @Test
        @DisplayName("White cannot bear off with lower die value")
        void whiteBearOffLowerDie() {
            setupWhiteBearOffPosition();
            // Checker on point 4 (index 3), die 2 = too short
            Move move = Move.bearOff(3, 2);
            assertFalse(BackgammonRules.canBearOff(state, move, true));
        }

        @Test
        @DisplayName("White can bear off with higher die when no checkers behind")
        void whiteBearOffHigherDieNoCheckersBehind() {
            clearBoard();
            state.setPoint(2, 3);  // 3 checkers on point 3 (highest)
            state.setPoint(0, 3);  // 3 checkers on point 1

            // Checker on point 3 (index 2), die 6 - allowed since it's highest
            Move move = Move.bearOff(2, 6);
            assertTrue(BackgammonRules.canBearOff(state, move, true),
                "Can bear off highest checker with higher die");
        }

        @Test
        @DisplayName("White cannot bear off lower checker with higher die when checkers behind")
        void whiteBearOffHigherDieWithCheckersBehind() {
            clearBoard();
            state.setPoint(0, 3);  // 3 checkers on point 1
            state.setPoint(5, 3);  // 3 checkers on point 6 (higher)

            // Checker on point 1 (index 0), die 6 - NOT allowed since checker on point 6
            Move move = Move.bearOff(0, 6);
            assertFalse(BackgammonRules.canBearOff(state, move, true),
                "Cannot bear off with higher die when checkers behind");
        }

        @Test
        @DisplayName("Black can bear off exact die")
        void blackBearOffExact() {
            setupBlackBearOffPosition();
            // Checker on point 22 (index 21), die 3 = exact (24-21=3)
            Move move = Move.bearOff(21, 3);
            assertTrue(BackgammonRules.canBearOff(state, move, false));
        }
    }

    @Nested
    @DisplayName("isValidMove Tests")
    class IsValidMoveTests {

        @Test
        @DisplayName("Must enter from bar first")
        void mustEnterFromBar() {
            state.setWhiteBar(1);
            state.setWhiteTurn(true);

            // Try to move a normal checker while having one on bar
            Move move = new Move(18, 13, 5);
            assertFalse(BackgammonRules.isValidMove(state, move, true),
                "Cannot move normal checker while checker on bar");
        }

        @Test
        @DisplayName("Bar entry is valid when have checker on bar")
        void barEntryValid() {
            state.setWhiteBar(1);
            state.setPoint(20, 0); // Clear entry point

            Move move = Move.fromBar(20, 4, false);
            assertTrue(BackgammonRules.isValidMove(state, move, true));
        }

        @Test
        @DisplayName("Cannot move from point without own checker")
        void cannotMoveFromEmpty() {
            state.setPoint(10, 0); // Empty
            Move move = new Move(10, 5, 5);
            assertFalse(BackgammonRules.isValidMove(state, move, true));
        }

        @Test
        @DisplayName("Cannot move from opponent's point")
        void cannotMoveFromOpponent() {
            state.setPoint(10, -3); // Black checkers
            Move move = new Move(10, 5, 5);
            assertFalse(BackgammonRules.isValidMove(state, move, true),
                "White cannot move black checkers");
        }
    }

    @Nested
    @DisplayName("willHit Tests")
    class WillHitTests {

        @Test
        @DisplayName("Hit single opponent")
        void hitSingleOpponent() {
            state.setPoint(10, -1); // Single black
            assertTrue(BackgammonRules.willHit(state, 10, true), "White hits black blot");

            state.setPoint(10, 1); // Single white
            assertTrue(BackgammonRules.willHit(state, 10, false), "Black hits white blot");
        }

        @Test
        @DisplayName("No hit on empty point")
        void noHitEmpty() {
            state.setPoint(10, 0);
            assertFalse(BackgammonRules.willHit(state, 10, true));
            assertFalse(BackgammonRules.willHit(state, 10, false));
        }

        @Test
        @DisplayName("No hit on own point")
        void noHitOwn() {
            state.setPoint(10, 3); // White
            assertFalse(BackgammonRules.willHit(state, 10, true));

            state.setPoint(10, -3); // Black
            assertFalse(BackgammonRules.willHit(state, 10, false));
        }

        @Test
        @DisplayName("No hit on blocked point")
        void noHitBlocked() {
            state.setPoint(10, -2); // 2 black
            assertFalse(BackgammonRules.willHit(state, 10, true),
                "Cannot hit blocked point");
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
