package com.backgammon3d.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Move class.
 */
@DisplayName("Move Tests")
class MoveTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Create normal move")
        void createNormalMove() {
            Move move = new Move(11, 5, 6);

            assertEquals(11, move.getFrom());
            assertEquals(5, move.getTo());
            assertEquals(6, move.getDieValue());
            assertFalse(move.isHit());
        }

        @Test
        @DisplayName("Create hit move")
        void createHitMove() {
            Move move = new Move(11, 5, 6, true);

            assertTrue(move.isHit());
        }

        @Test
        @DisplayName("Create bar entry move")
        void createBarEntryMove() {
            Move move = Move.fromBar(20, 4, false);

            assertEquals(Move.BAR, move.getFrom());
            assertEquals(20, move.getTo());
            assertEquals(4, move.getDieValue());
            assertTrue(move.isFromBar());
            assertFalse(move.isBearOff());
        }

        @Test
        @DisplayName("Create bear off move")
        void createBearOffMove() {
            Move move = Move.bearOff(3, 4);

            assertEquals(3, move.getFrom());
            assertEquals(Move.BEAR_OFF, move.getTo());
            assertEquals(4, move.getDieValue());
            assertFalse(move.isFromBar());
            assertTrue(move.isBearOff());
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Equal moves are equal")
        void equalMoves() {
            Move move1 = new Move(5, 3, 2);
            Move move2 = new Move(5, 3, 2);

            assertEquals(move1, move2);
            assertEquals(move1.hashCode(), move2.hashCode());
        }

        @Test
        @DisplayName("Different moves are not equal")
        void differentMoves() {
            Move move1 = new Move(5, 3, 2);
            Move move2 = new Move(5, 2, 3);

            assertNotEquals(move1, move2);
        }

        @Test
        @DisplayName("Hit flag not considered in equality")
        void hitFlagNotInEquality() {
            Move move1 = new Move(5, 3, 2, false);
            Move move2 = new Move(5, 3, 2, true);

            assertEquals(move1, move2, "Hit flag should not affect equality");
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Normal move toString")
        void normalMoveToString() {
            Move move = new Move(11, 5, 6);
            String str = move.toString();

            assertTrue(str.contains("12"), "Should show point 12 (index 11)");
            assertTrue(str.contains("6"), "Should show point 6 (index 5)");
        }

        @Test
        @DisplayName("Hit move has asterisk")
        void hitMoveToString() {
            Move move = new Move(11, 5, 6, true);
            String str = move.toString();

            assertTrue(str.contains("*"), "Hit move should have asterisk");
        }

        @Test
        @DisplayName("Bar entry shows Bar")
        void barEntryToString() {
            Move move = Move.fromBar(20, 4, false);
            String str = move.toString();

            assertTrue(str.contains("Bar"), "Should show Bar");
        }

        @Test
        @DisplayName("Bear off shows Off")
        void bearOffToString() {
            Move move = Move.bearOff(3, 4);
            String str = move.toString();

            assertTrue(str.contains("Off"), "Should show Off");
        }
    }

    @Nested
    @DisplayName("Special Values Tests")
    class SpecialValuesTests {

        @Test
        @DisplayName("BAR constant is -1")
        void barConstant() {
            assertEquals(-1, Move.BAR);
        }

        @Test
        @DisplayName("BEAR_OFF constant is -1")
        void bearOffConstant() {
            assertEquals(-1, Move.BEAR_OFF);
        }

        @Test
        @DisplayName("isFromBar works correctly")
        void isFromBar() {
            Move barMove = Move.fromBar(20, 4, false);
            Move normalMove = new Move(11, 5, 6);

            assertTrue(barMove.isFromBar());
            assertFalse(normalMove.isFromBar());
        }

        @Test
        @DisplayName("isBearOff works correctly")
        void isBearOff() {
            Move bearOffMove = Move.bearOff(3, 4);
            Move normalMove = new Move(11, 5, 6);

            assertTrue(bearOffMove.isBearOff());
            assertFalse(normalMove.isBearOff());
        }
    }
}
